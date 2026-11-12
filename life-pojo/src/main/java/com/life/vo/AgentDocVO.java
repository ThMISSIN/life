package com.life.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 知识文档入库结果（对应 Python /agent/knowledge/documents 返回）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentDocVO {

    @JsonAlias("document_id")
    private String documentId;

    private String filename;

    /** 解析分块成功后写入向量库的块数 */
    @JsonAlias("chunk_count")
    private int chunkCount;
}
