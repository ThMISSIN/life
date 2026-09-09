package com.life.query;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

}
