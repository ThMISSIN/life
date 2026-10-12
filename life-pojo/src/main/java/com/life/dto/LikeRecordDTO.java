package com.life.dto;

import lombok.Data;

@Data
public class LikeRecordDTO {

    private Long bizId;

    private String bizType;

    private Boolean liked;
}
