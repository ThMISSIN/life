package com.ruoyi.life.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.life.domain.LifeNoticeTemplate;
import com.ruoyi.life.domain.query.LifeNoticeTemplateQuery;
import com.ruoyi.life.mapper.LifeNoticeTemplateMapper;
import com.ruoyi.life.service.ILifeNoticeTemplateService;
import com.ruoyi.life.util.LifeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知模板管理 服务实现
 *
 * @author life
 */
@Service
public class LifeNoticeTemplateServiceImpl implements ILifeNoticeTemplateService
{
    @Autowired
    private LifeNoticeTemplateMapper lifeNoticeTemplateMapper;

    @Override
    public List<LifeNoticeTemplate> selectLifeNoticeTemplateList(LifeNoticeTemplateQuery query)
    {
        return lifeNoticeTemplateMapper.selectLifeNoticeTemplateList(query);
    }

    @Override
    public LifeNoticeTemplate selectLifeNoticeTemplateById(Long id)
    {
        return lifeNoticeTemplateMapper.selectLifeNoticeTemplateById(id);
    }

    /**
     * 新增模板: 必填校验 + 编码唯一(与表 uk_code 双保险)
     */
    @Override
    public int insertLifeNoticeTemplate(LifeNoticeTemplate lifeNoticeTemplate)
    {
        checkRequired(lifeNoticeTemplate);
        if (lifeNoticeTemplateMapper.countByCode(lifeNoticeTemplate.getCode(), null) > 0)
        {
            throw new ServiceException("模板编码已存在");
        }
        if (lifeNoticeTemplate.getStatus() == null)
        {
            lifeNoticeTemplate.setStatus(1);
        }
        lifeNoticeTemplate.setCreateTime(LifeUtils.now());
        lifeNoticeTemplate.setUpdateTime(LifeUtils.now());
        lifeNoticeTemplate.setCreater(LifeUtils.currentOperator());
        lifeNoticeTemplate.setUpdater(LifeUtils.currentOperator());
        lifeNoticeTemplate.setDeleted(0);
        return lifeNoticeTemplateMapper.insertLifeNoticeTemplate(lifeNoticeTemplate);
    }

    /**
     * 修改模板: 编码传值且变更时重做唯一校验
     */
    @Override
    public int updateLifeNoticeTemplate(LifeNoticeTemplate lifeNoticeTemplate)
    {
        if (lifeNoticeTemplate.getId() == null)
        {
            throw new ServiceException("模板ID不能为空");
        }
        LifeNoticeTemplate origin = lifeNoticeTemplateMapper.selectLifeNoticeTemplateById(lifeNoticeTemplate.getId());
        if (origin == null)
        {
            throw new ServiceException("模板不存在");
        }
        String code = lifeNoticeTemplate.getCode();
        if (code != null && !code.equals(origin.getCode())
                && lifeNoticeTemplateMapper.countByCode(code, origin.getId()) > 0)
        {
            throw new ServiceException("模板编码已存在");
        }
        lifeNoticeTemplate.setUpdateTime(LifeUtils.now());
        lifeNoticeTemplate.setUpdater(LifeUtils.currentOperator());
        return lifeNoticeTemplateMapper.updateLifeNoticeTemplate(lifeNoticeTemplate);
    }

    @Override
    public int changeStatus(Long id, Integer status)
    {
        if (status == null || (status != 0 && status != 1))
        {
            throw new ServiceException("状态不合法");
        }
        LifeNoticeTemplate template = new LifeNoticeTemplate();
        template.setId(id);
        template.setStatus(status);
        template.setUpdateTime(LifeUtils.now());
        template.setUpdater(LifeUtils.currentOperator());
        return lifeNoticeTemplateMapper.updateLifeNoticeTemplate(template);
    }

    @Override
    public int deleteLifeNoticeTemplateById(Long id)
    {
        LifeNoticeTemplate template = lifeNoticeTemplateMapper.selectLifeNoticeTemplateById(id);
        if (template == null)
        {
            throw new ServiceException("模板不存在");
        }
        return lifeNoticeTemplateMapper.deleteLifeNoticeTemplateById(id);
    }

    /**
     * 模板名称/编码/类型/标题/内容模板必填, 类型限1~5
     */
    private void checkRequired(LifeNoticeTemplate lifeNoticeTemplate)
    {
        if (lifeNoticeTemplate.getName() == null || lifeNoticeTemplate.getName().isBlank())
        {
            throw new ServiceException("模板名称不能为空");
        }
        if (lifeNoticeTemplate.getCode() == null || lifeNoticeTemplate.getCode().isBlank())
        {
            throw new ServiceException("模板编码不能为空");
        }
        if (lifeNoticeTemplate.getType() == null || lifeNoticeTemplate.getType() < 1 || lifeNoticeTemplate.getType() > 5)
        {
            throw new ServiceException("模板类型不合法(1-系统 2-订单 3-活动 4-权益 5-积分)");
        }
        if (lifeNoticeTemplate.getTitleTemplate() == null || lifeNoticeTemplate.getTitleTemplate().isBlank())
        {
            throw new ServiceException("标题模板不能为空");
        }
        if (lifeNoticeTemplate.getContentTemplate() == null || lifeNoticeTemplate.getContentTemplate().isBlank())
        {
            throw new ServiceException("内容模板不能为空");
        }
    }
}
