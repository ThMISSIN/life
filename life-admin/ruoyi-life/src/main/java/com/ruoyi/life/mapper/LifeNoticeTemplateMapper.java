package com.ruoyi.life.mapper;

import com.ruoyi.life.domain.LifeNoticeTemplate;
import com.ruoyi.life.domain.query.LifeNoticeTemplateQuery;

import java.util.List;

/**
 * 通知模板 数据层
 */
public interface LifeNoticeTemplateMapper
{
    /** 分页查询模板列表(不分页由Controller startPage接管) */
    public List<LifeNoticeTemplate> selectLifeNoticeTemplateList(LifeNoticeTemplateQuery query);

    /** 按ID查询 */
    public LifeNoticeTemplate selectLifeNoticeTemplateById(Long id);

    /** 按编码查询(业务方渲染取模板用) */
    public LifeNoticeTemplate selectByCode(String code);

    /** 按编码计数(编码唯一校验, excludeId排除自身) */
    public int countByCode(String code, Long excludeId);

    /** 新增 */
    public int insertLifeNoticeTemplate(LifeNoticeTemplate lifeNoticeTemplate);

    /** 修改 */
    public int updateLifeNoticeTemplate(LifeNoticeTemplate lifeNoticeTemplate);

    /** 逻辑删除 */
    public int deleteLifeNoticeTemplateById(Long id);
}
