package com.ruoyi.life.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.life.constant.LifeConstants;
import com.ruoyi.life.domain.LifeProductCategory;
import com.ruoyi.life.mapper.LifeProductCategoryMapper;
import com.ruoyi.life.mapper.LifeProductMapper;
import com.ruoyi.life.service.ILifeProductCategoryService;
import com.ruoyi.life.util.LifeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品分类管理 服务实现（业务口径与C端 LifeProductCategoryServiceImpl 管理端方法一致）
 *
 * @author life
 */
@Service
public class LifeProductCategoryServiceImpl implements ILifeProductCategoryService
{
    @Autowired
    private LifeProductCategoryMapper lifeProductCategoryMapper;

    @Autowired
    private LifeProductMapper lifeProductMapper;

    @Override
    public List<LifeProductCategory> selectLifeProductCategoryList()
    {
        return lifeProductCategoryMapper.selectLifeProductCategoryList(null);
    }

    @Override
    public LifeProductCategory selectLifeProductCategoryById(Long id)
    {
        return lifeProductCategoryMapper.selectLifeProductCategoryById(id);
    }

    /**
     * 管理端新增分类: 校验名称、上级分类与层级一致性
     */
    @Override
    public int insertLifeProductCategory(LifeProductCategory lifeProductCategory)
    {
        checkName(lifeProductCategory.getName(), null);
        checkParent(lifeProductCategory);
        lifeProductCategory.setCreateTime(LifeUtils.now());
        lifeProductCategory.setUpdateTime(LifeUtils.now());
        lifeProductCategory.setCreater(LifeUtils.currentOperator());
        lifeProductCategory.setUpdater(LifeUtils.currentOperator());
        lifeProductCategory.setDeleted(0);
        return lifeProductCategoryMapper.insertLifeProductCategory(lifeProductCategory);
    }

    /**
     * 管理端修改分类: 名称与层级仅在传值时校验
     */
    @Override
    public int updateLifeProductCategory(LifeProductCategory lifeProductCategory)
    {
        if (lifeProductCategory.getId() == null)
        {
            throw new ServiceException("分类ID不能为空");
        }
        String name = lifeProductCategory.getName();
        if (name != null)
        {
            checkName(name, lifeProductCategory.getId());
        }
        if (lifeProductCategory.getParentId() != null)
        {
            checkParent(lifeProductCategory);
        }
        lifeProductCategory.setUpdateTime(LifeUtils.now());
        lifeProductCategory.setUpdater(LifeUtils.currentOperator());
        return lifeProductCategoryMapper.updateLifeProductCategory(lifeProductCategory);
    }

    /**
     * 管理端删除分类: 存在子分类或挂载商品时禁止删除, 避免出现孤儿数据
     */
    @Override
    public int deleteLifeProductCategoryById(Long id)
    {
        int childCount = lifeProductCategoryMapper.countByParentId(id);
        if (childCount > 0)
        {
            throw new ServiceException("存在子分类, 不允许删除");
        }
        int productCount = lifeProductMapper.countByCategoryId(id);
        if (productCount > 0)
        {
            throw new ServiceException("分类下存在商品, 不允许删除");
        }
        return lifeProductCategoryMapper.deleteLifeProductCategoryById(id);
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
        if (lifeProductCategoryMapper.countByName(name, excludeId) > 0)
        {
            throw new ServiceException("分类名称已存在");
        }
    }

    /**
     * 校验上级分类与层级一致性: parentId=0为一级分类, 其余必须挂载在一级分类下并固定为二级
     */
    private void checkParent(LifeProductCategory lifeProductCategory)
    {
        Long parentId = lifeProductCategory.getParentId();
        if (parentId == null || parentId == LifeConstants.ROOT_PARENT_ID)
        {
            lifeProductCategory.setParentId(LifeConstants.ROOT_PARENT_ID);
            lifeProductCategory.setLevel(LifeConstants.LEVEL_FIRST);
            return;
        }
        // 不能把自己挂到自己的子级
        if (parentId.equals(lifeProductCategory.getId()))
        {
            throw new ServiceException("上级分类不能是自身");
        }
        LifeProductCategory parent = lifeProductCategoryMapper.selectLifeProductCategoryById(parentId);
        if (parent == null || parent.getParentId() != LifeConstants.ROOT_PARENT_ID)
        {
            throw new ServiceException("上级分类不存在或不是一级分类");
        }
        lifeProductCategory.setLevel(LifeConstants.LEVEL_SECOND);
    }
}
