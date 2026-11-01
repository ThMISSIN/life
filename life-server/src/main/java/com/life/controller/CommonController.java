package com.life.controller;

import com.life.constant.MessageConstant;
import com.life.entity.LifeFile;
import com.life.mapper.LifeFileMapper;
import com.life.result.Result;
import com.life.util.LocalStorageUtil;
import com.life.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Slf4j
public class CommonController {

    /**
     * 扩展名 -> life_file.file_type(1-图片 2-视频 3-文档 4-其他)
     */
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "mov", "avi", "webm", "mkv");
    private static final Set<String> DOC_EXT = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt");

    @Autowired
    private AliOssUtil aliOssUtil;

    @Autowired
    private LocalStorageUtil localStorageUtil;

    @Autowired
    private LifeFileMapper lifeFileMapper;

    /**
     * 存储通道: local-本地磁盘(默认, 项目自带 AK 已被云端禁用) / oss-阿里云 OSS
     */
    @Value("${life.storage.type:local}")
    private String storageType;

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file,
                                 @RequestParam(required = false) String bizType) {
        log.info("文件上传：{}, 存储通道: {}", file, storageType);

        // 基础校验: 空文件直接拒绝
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().contains(".")) {
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }

        try {
            // 取原文件后缀, UUID+后缀作为对象名, 避免重名覆盖; 按年月分目录
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String monthPath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String objectName = monthPath + "/" + UUID.randomUUID().toString() + extension;

            boolean useOss = "oss".equalsIgnoreCase(storageType);
            String filePath = useOss
                    ? aliOssUtil.upload(file.getBytes(), objectName)
                    : localStorageUtil.upload(file.getBytes(), objectName);

            // 上传留痕(life_file): 原名/URL/大小/平台/业务类型/上传人(creater自动填充);
            // 留痕失败不影响上传结果返回, 仅告警
            try {
                LifeFile record = LifeFile.builder()
                        .fileKey(filePath)
                        .fileName(originalFilename)
                        .fileType(inferFileType(extension))
                        .fileSize(file.getSize())
                        .platform(useOss ? "阿里云" : "本地")
                        .bizType(bizType == null || bizType.isBlank() ? "OTHER" : bizType.toUpperCase(Locale.ROOT))
                        .status(1)
                        .build();
                lifeFileMapper.insert(record);
            } catch (Exception e) {
                log.warn("上传留痕写 life_file 失败: {}", filePath, e);
            }

            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件读取失败：{}", e);
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

    /**
     * 按扩展名推断文件类型: 1-图片, 2-视频, 3-文档, 4-其他
     */
    private Integer inferFileType(String extension) {
        String ext = extension.toLowerCase(Locale.ROOT).replaceFirst("^\\.", "");
        if (IMAGE_EXT.contains(ext)) {
            return 1;
        }
        if (VIDEO_EXT.contains(ext)) {
            return 2;
        }
        if (DOC_EXT.contains(ext)) {
            return 3;
        }
        return 4;
    }
}
