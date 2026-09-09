package com.life.controller;

import com.life.entity.SignResultVO;
import com.life.result.Result;
import com.life.service.SignRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/signRecord")
public class SignRecordController {

    @Autowired
    private SignRecordService recordService;

    @PostMapping
    public Result<SignResultVO> addSignRecords(){
        SignResultVO vo = recordService.addSignRecords();
        return Result.success(vo);
    }

    @GetMapping
    public Result<Byte[]> querySignRecords(){
        Byte[] bytes = recordService.querySignRecords();
        return Result.success(bytes);
    }

}
