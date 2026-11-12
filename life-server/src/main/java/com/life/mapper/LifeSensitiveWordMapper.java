package com.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.life.entity.LifeSensitiveWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LifeSensitiveWordMapper extends BaseMapper<LifeSensitiveWord> {

    /**
     * 全量启用词(DFA 词典构建数据源, 定时刷新)
     */
    @Select("SELECT * FROM life_sensitive_word WHERE status = 1 AND deleted = 0")
    List<LifeSensitiveWord> selectEnabled();
}
