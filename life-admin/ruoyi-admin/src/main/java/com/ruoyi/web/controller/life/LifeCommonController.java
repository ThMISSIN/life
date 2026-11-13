package com.ruoyi.web.controller.life;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.file.InvalidExtensionException;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.life.domain.LifeFile;
import com.ruoyi.life.mapper.LifeFileMapper;
import com.ruoyi.life.util.LifeUtils;

/**
 * life业务通用请求处理
 * 图片上传写入与C端 life-java 共用的本地存储目录({life.local-storage.base-path}),
 * 返回与C端一致的相对路径 /uploads/{yyyy}/{MM}/{uuid}.ext,
 * 图片统一由C端后端(默认 http://localhost:8080)/uploads/** 提供, 保证两端数据互通
 *
 * @author life
 */
@RestController
@RequestMapping("/life/common")
public class LifeCommonController
{
    private static final Logger log = LoggerFactory.getLogger(LifeCommonController.class);

    @Value("${life.local-storage.base-path:uploads}")
    private String basePath;

    @Autowired
    private LifeFileMapper lifeFileMapper;

    /**
     * 图片上传(单个), 与C端 CommonController /admin/common/upload 数据口径一致;
     * 上传成功后写 life_file 留痕(1.5), 留痕失败仅告警不影响上传结果
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(MultipartFile file,
                                 @RequestParam(required = false) String bizType)
    {
        try
        {
            // 仅允许图片类型, 复用若依扩展名校验
            FileUploadUtils.assertAllowed(file, MimeTypeUtils.IMAGE_EXTENSION);
            String originalFilename = file.getOriginalFilename();
            String extension = "." + originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            String datePath = com.ruoyi.common.utils.DateUtils.datePath();
            String objectName = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

            Path root = Paths.get(basePath).toAbsolutePath();
            Path target = root.resolve(objectName).normalize();
            // 防目录穿越: 目标必须落在根目录内
            if (!target.startsWith(root))
            {
                return AjaxResult.error("文件路径非法");
            }
            Files.createDirectories(target.getParent());
            file.transferTo(new File(target.toUri()));
            String url = "/uploads/" + objectName;
            log.info("life图片保存到共享本地存储: {}", target);

            recordLifeFile(url, originalFilename, file.getSize(),
                    bizType == null || bizType.trim().isEmpty() ? "OTHER" : bizType.trim().toUpperCase());

            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", url);
            ajax.put("newFileName", FileUtils.getName(objectName));
            ajax.put("originalFilename", originalFilename);
            return ajax;
        }
        catch (InvalidExtensionException e)
        {
            return AjaxResult.error("仅支持图片文件: " + e.getMessage());
        }
        catch (Exception e)
        {
            log.error("life图片上传失败", e);
            return AjaxResult.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传留痕: 本服务固定图片类(file_type=1)与本地平台, creater 记当前管理端账号
     */
    private void recordLifeFile(String fileKey, String fileName, Long fileSize, String bizType)
    {
        try
        {
            LifeFile record = new LifeFile();
            record.setFileKey(fileKey);
            record.setFileName(fileName);
            record.setFileType(1);
            record.setFileSize(fileSize);
            record.setPlatform("本地");
            record.setBizType(bizType);
            record.setStatus(1);
            record.setCreateTime(LifeUtils.now());
            record.setUpdateTime(LifeUtils.now());
            record.setCreater(LifeUtils.currentOperator());
            record.setUpdater(LifeUtils.currentOperator());
            record.setDeleted(0);
            lifeFileMapper.insertLifeFile(record);
        }
        catch (Exception e)
        {
            log.warn("上传留痕写 life_file 失败: {}", fileKey, e);
        }
    }
}
