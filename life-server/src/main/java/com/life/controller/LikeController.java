package com.life.controller;

import com.life.dto.LikeRecordDTO;
import com.life.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/like")
public class LikeController {

    @Autowired
    private LikeService likeService;

    /**
     * 点赞
     */
    @RequestMapping
    public void addLikeRecord(LikeRecordDTO recordDto) {
        likeService.addLikeRecord(recordDto);
    }

    @GetMapping("/list")
    public Set<Long> isBizLiked(@RequestParam("bizType") String bizType,
                                @RequestParam("bizIds") List<Long> bizIds) {
        return likeService.isBizLiked(bizType, bizIds);
    }

}
