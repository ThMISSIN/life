package com.life.service;

import com.life.entity.SignResultVO;

public interface SignRecordService {

    SignResultVO addSignRecords();

    Byte[] querySignRecords();
}
