package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

/**
 * 文件表(上传留痕: 原名/访问URL/大小/上传人/bizType)
 */
@Data
@Builder
@TableName("life_file")
public class LifeFile extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件key(存储对象路径, 本地通道即 /uploads 下的访问URL)
     */
    private String fileKey;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件类型: 1-图片, 2-视频, 3-文档, 4-其他
     */
    private Integer fileType;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 平台: 腾讯云 / 阿里云 / 本地
     */
    private String platform;

    /**
     * 业务类型: AVATAR/POST/GOODS/BENEFIT/BANNER/OTHER
     */
    private String bizType;

    /**
     * 状态: 0-上传中, 1-已上传, 2-已删除
     */
    private Integer status;
}
