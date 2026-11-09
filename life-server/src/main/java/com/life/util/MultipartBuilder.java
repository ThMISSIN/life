package com.life.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 极简 multipart/form-data 构造器：Java 收到 UploadFile 后原样转发给 Python Agent。
 * 不依赖第三方库，支持单个文件字段。
 */
public final class MultipartBuilder {

    private final String boundary = "----LifeAgentBoundary" + System.nanoTime();
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public static MultipartBuilder create() {
        return new MultipartBuilder();
    }

    public MultipartBuilder addFile(String field, String filename, String contentType, byte[] data) {
        try {
            String header = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + field + "\"; filename=\""
                    + sanitize(filename) + "\"\r\n"
                    + "Content-Type: " + (contentType == null ? "application/octet-stream" : contentType) + "\r\n\r\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(data);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("构造 multipart 失败", e);
        }
        return this;
    }

    public byte[] bodyBytes() {
        try {
            ByteArrayOutputStream full = new ByteArrayOutputStream();
            full.write(out.toByteArray());
            full.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return full.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("构造 multipart 失败", e);
        }
    }

    public String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        return name.replaceAll("[\"\\r\\n]", "");
    }
}
