package com.ruoyi.life.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.life.constant.LifeConstants;
import com.ruoyi.life.domain.LifeBenefitCategory;
import com.ruoyi.life.mapper.LifeBenefitCategoryMapper;
import com.ruoyi.life.mapper.LifeBenefitMapper;
import com.ruoyi.life.service.ILifeBenefitCategoryService;
import com.ruoyi.life.util.LifeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权益分类管理 服务实现（业务口径与C端 LifeBenefitCategoryServiceImpl 管理端方法一致）
 *
 * @author life
 */
@Service
public class LifeBenefitCategoryServiceImpl implements ILifeBenefitCategoryService
{
    @Autowired
    private LifeBenefitCategoryMapper lifeBenefitCategoryMapper;

    @Autowired
    private LifeBenefitMapper lifeBenefitMapper;

    @Override
    public List<LifeBenefitCategory> selectLifeBenefitCategoryList()
    {
        return lifeBenefitCategoryMapper.selectLifeBenefitCategoryList(null);
    }

    @Override
    public LifeBenefitCategory selectLifeBenefitCategoryById(Long id)
    {
        return lifeBenefitCategoryMapper.selectLifeBenefitCategoryById(id);
    }

    /**
     * 管理端新增分类: 校验名称、上级分类与层级一致性
     */
    @Override
    public int insertLifeBenefitCategory(LifeBenefitCategory lifeBenefitCategory)
    {
        checkName(lifeBenefitCategory.getName(), null);
        checkParent(lifeBenefitCategory);
        lifeBenefitCategory.setCreateTime(LifeUtils.now());
        lifeBenefitCategory.setUpdateTime(LifeUtils.now());
        lifeBenefitCategory.setCreater(LifeUtils.currentOperator());
        lifeBenefitCategory.setUpdater(LifeUtils.currentOperator());
        lifeBenefitCategory.setDeleted(0);
        return lifeBenefitCategoryMapper.insertLifeBenefitCategory(lifeBenefitCategory);
    }

    /**
     * 管理端修改分类: 名称与层级仅在传值时校验
     */
    @Override
    public int updateLifeBenefitCategory(LifeBenefitCategory lifeBenefitCategory)
    {
        if (lifeBenefitCategory.getId() == null)
        {
            throw new ServiceException("分类ID不能为空");
        }
        String name = lifeBenefitCategory.getName();
        if (name != null)
        {
            checkName(name, lifeBenefitCategory.getId());
        }
        if (lifeBenefitCategory.getParentId() != null)
        {
            checkParent(lifeBenefitCategory);
        }
        lifeBenefitCategory.setUpdateTime(LifeUtils.now());
        lifeBenefitCategory.setUpdater(LifeUtils.currentOperator());
        return lifeBenefitCategoryMapper.updateLifeBenefitCategory(lifeBenefitCategory);
    }

    /**
     * 管理端删除分类: 存在子分类或挂载权益时禁止删除, 避免出现孤儿数据
     */
    @Override
    public int deleteLifeBenefitCategoryById(Long id)
    {
        int childCount = lifeBenefitCategoryMapper.countByParentId(id);
        if (childCount > 0)
        {
            throw new ServiceException("存在子分类, 不允许删除");
        }
        int benefitCount = lifeBenefitMapper.countByCategoryId(id);
        if (benefitCount > 0)
        {
            throw new ServiceException("分类下存在权益, 不允许删除");
        }
        return lifeBenefitCategoryMapper.deleteLifeBenefitCategoryById(id);
    }

    /**
     * 校验分类名称: 非空且不重复(排除自身)
     */
    private void checkName(String name, Long excludeId)
    {
        if (name == null || name.isBlank())
        {
            throw new ServiceException("分类名称不能为空");
        }
        if (lifeBenefitCategoryMapper.countByName(name, excludeId) > 0)
        {
            throw new ServiceException("分类名称已存在");
        }
    }

    /**
     * 校验上级分类与层级一致性: parentId=0为一级分类, 其余必须挂载在一级分类下并固定为二级
     */
    private void checkParent(LifeBenefitCategory lifeBenefitCategory)
    {
        Long parentId = lifeBenefitCategory.getParentId();
        if (parentId == null || parentId == LifeConstants.ROOT_PARENT_ID)
        {
            lifeBenefitCategory.setParentId(LifeConstants.ROOT_PARENT_ID);
            lifeBenefitCategory.setLevel(LifeConstants.LEVEL_FIRST);
            return;
        }
        // 不能把自己挂到自己的子级
        if (parentId.equals(lifeBenefitCategory.getId()))
        {
            throw new ServiceException("上级分类不能是自身");
        }
        LifeBenefitCategory parent = lifeBenefitCategoryMapper.selectLifeBenefitCategoryById(parentId);
        if (parent == null || parent.getParentId() != LifeConstants.ROOT_PARENT_ID)
        {
            throw new ServiceException("上级分类不存在或不是一级分类");
        }
        lifeBenefitCategory.setLevel(LifeConstants.LEVEL_SECOND);
    }
}
