package com.ruoyi.life.service;

import com.ruoyi.life.domain.LifeNoticeTemplate;
import com.ruoyi.life.domain.query.LifeNoticeTemplateQuery;

import java.util.List;

/**
 * 通知模板管理 服务接口
 */
public interface ILifeNoticeTemplateService
{
    /** 分页查询(status/type不传查全部); 分页由Controller startPage()接管 */
    public List<LifeNoticeTemplate> selectLifeNoticeTemplateList(LifeNoticeTemplateQuery query);

    /** 按ID查询 */
    public LifeNoticeTemplate selectLifeNoticeTemplateById(Long id);

    /** 新增模板: 名称/编码/类型/标题/内容必填, 编码全局唯一 */
    public int insertLifeNoticeTemplate(LifeNoticeTemplate lifeNoticeTemplate);

    /** 修改模板: 部分更新, 编码变更时重做唯一校验 */
    public int updateLifeNoticeTemplate(LifeNoticeTemplate lifeNoticeTemplate);

    /** 启用/禁用 */
    public int changeStatus(Long id, Integer status);

    /** 删除模板(软删除) */
    public int deleteLifeNoticeTemplateById(Long id);
}
