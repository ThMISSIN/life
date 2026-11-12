package com.life.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.life.entity.LifeSensitiveWord;
import com.life.exception.ValidationException;
import com.life.mapper.LifeSensitiveWordMapper;
import com.life.result.PageResult;
import com.life.result.Result;
import com.life.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2.9 敏感词库管理(/admin/**): CRUD 后即时 refresh 内存 DFA 词典(单机即时生效;
 * 多实例部署时其余节点靠 5 分钟定时刷新收敛)
 */
@RestController
@RequestMapping("/admin/sensitiveWord")
@RequiredArgsConstructor
public class AdminSensitiveWordController {

    private final LifeSensitiveWordMapper wordMapper;
    private final SensitiveWordService sensitiveWordService;

    /**
     * 分页: word 模糊 / level / status 可选筛选
     */
    @GetMapping("/page")
    public Result<PageResult> page(@RequestParam(defaultValue = "1") int pageNum,
                                   @RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(required = false) String word,
                                   @RequestParam(required = false) Integer level,
                                   @RequestParam(required = false) Integer status) {
        Page<LifeSensitiveWord> page = wordMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<LifeSensitiveWord>()
                        .like(word != null && !word.isBlank(), LifeSensitiveWord::getWord, word)
                        .eq(level != null, LifeSensitiveWord::getLevel, level)
                        .eq(status != null, LifeSensitiveWord::getStatus, status)
                        .orderByDesc(LifeSensitiveWord::getId));
        return Result.success(new PageResult(page.getTotal(), page.getRecords()));
    }

    /**
     * 新增: word 必填且唯一(uk_word 兜底), level 缺省 3-替换
     */
    @PostMapping
    public Result add(@RequestBody LifeSensitiveWord word) {
        if (word.getWord() == null || word.getWord().isBlank()) {
            throw new ValidationException("敏感词不能为空");
        }
        word.setId(null);
        if (word.getLevel() == null) {
            word.setLevel(3);
        }
        if (word.getStatus() == null) {
            word.setStatus(1);
        }
        wordMapper.insert(word);
        sensitiveWordService.refresh();
        return Result.success();
    }

    /**
     * 修改(词面/级别/分类/启停)
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody LifeSensitiveWord word) {
        word.setId(id);
        wordMapper.updateById(word);
        sensitiveWordService.refresh();
        return Result.success();
    }

    /**
     * 删除(逻辑删)
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        wordMapper.deleteById(id);
        sensitiveWordService.refresh();
        return Result.success();
    }
}
