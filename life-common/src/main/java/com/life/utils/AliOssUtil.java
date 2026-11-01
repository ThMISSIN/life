package com.life.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes      文件字节
     * @param objectName OSS 对象名（UUID+后缀）
     * @return 文件访问 URL（https://{bucket}.{endpoint}/{objectName}）
     * @throws RuntimeException 上传失败时抛出, 由调用方统一返回业务错误（不静默返回无效 URL）
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            log.error("OSS 服务端拒绝请求, ErrorCode: {}, ErrorMessage: {}, RequestId: {}",
                    oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
            throw new RuntimeException("文件上传到OSS失败: " + oe.getErrorMessage(), oe);
        } catch (ClientException ce) {
            log.error("OSS 客户端异常(网络/配置问题): {}", ce.getMessage());
            throw new RuntimeException("文件上传到OSS失败: " + ce.getMessage(), ce);
        } finally {
            ossClient.shutdown();
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        String url = "https://" + bucketName + "." + endpoint + "/" + objectName;

        log.info("文件上传到:{}", url);

        return url;
    }
}
