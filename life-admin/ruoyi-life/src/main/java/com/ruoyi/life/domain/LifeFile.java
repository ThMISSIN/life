package com.ruoyi.life.domain;

/**
 * 文件留痕对象 life_file(上传成功即记录, 与C端共表共库)
 *
 * @author life
 */
public class LifeFile extends LifeBaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文件ID */
    private Long id;

    /** 文件key(存储对象路径/访问URL) */
    private String fileKey;

    /** 原始文件名 */
    private String fileName;

    /** 文件类型: 1-图片, 2-视频, 3-文档, 4-其他 */
    private Integer fileType;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 平台: 腾讯云 / 阿里云 / 本地 */
    private String platform;

    /** 业务类型: AVATAR/POST/GOODS/BENEFIT/BANNER/OTHER */
    private String bizType;

    /** 状态: 0-上传中, 1-已上传, 2-已删除 */
    private Integer status;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getFileKey()
    {
        return fileKey;
    }

    public void setFileKey(String fileKey)
    {
        this.fileKey = fileKey;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public Integer getFileType()
    {
        return fileType;
    }

    public void setFileType(Integer fileType)
    {
        this.fileType = fileType;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public String getPlatform()
    {
        return platform;
    }

    public void setPlatform(String platform)
    {
        this.platform = platform;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }
}
