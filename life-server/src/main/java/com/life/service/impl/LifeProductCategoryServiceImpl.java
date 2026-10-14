package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.CategoryConstant;
import com.life.constant.MessageConstant;
import com.life.constant.StatusConstant;
import com.life.entity.LifeProduct;
import com.life.entity.LifeProductCategory;
import com.life.exception.ValidationException;
import com.life.mapper.LifeProductCategoryMapper;
import com.life.mapper.LifeProductMapper;
import com.life.service.LifeProductCategoryService;
import com.life.vo.LifeProductCategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LifeProductCategoryServiceImpl extends ServiceImpl<LifeProductCategoryMapper, LifeProductCategory> implements LifeProductCategoryService {

    @Autowired
    private LifeProductMapper lifeProductMapper;

    @Override
    public List<LifeProductCategoryVO> getCategoryTree() {
        // 查询所有 分类(deleted由MP逻辑删除自动过滤)
        LambdaQueryWrapper<LifeProductCategory> eq = new LambdaQueryWrapper<LifeProductCategory>()
                .eq(LifeProductCategory::getStatus, StatusConstant.ENABLE)
                .orderByAsc(LifeProductCategory::getSort);

        List<LifeProductCategory> categories = baseMapper.selectList(eq);

        // 过滤出所有一级分类
        List<LifeProductCategory> rootList = categories.stream()
                .filter(c -> c.getParentId() == CategoryConstant.ROOT_PARENT_ID)
                .toList();

        // 组装两级分类树
        List<LifeProductCategoryVO> vos = new ArrayList<>();

        for (LifeProductCategory root : rootList) {
            LifeProductCategoryVO vo = new LifeProductCategoryVO();
            List<LifeProductCategoryVO> sonList = categories.stream()
                    .filter(c -> c.getParentId().equals(root.getId()))
                    .map(c -> LifeProductCategoryVO
                            .builder()
                            .id(c.getId())
                            .name(c.getName())
                            .icon(c.getIcon())
                            .build())
                    .toList();
            vo.setId(root.getId());
            vo.setName(root.getName());
            vo.setIcon(root.getIcon());
            vo.setChildren(sonList);
            vos.add(vo);
        }

        return vos;
    }

    /**
     * 管理端新增分类: 校验名称、上级分类与层级一致性
     */
    @Override
    public void addCategory(LifeProductCategory lifeProductCategory) {
        checkName(lifeProductCategory.getName(), null);
        checkParent(lifeProductCategory);
        save(lifeProductCategory);
    }

    /**
     * 管理端修改分类: 名称与层级仅在传值时校验
     */
    @Override
    public void updateCategory(LifeProductCategory lifeProductCategory) {
        if (lifeProductCategory.getId() == null) {
            throw new ValidationException(MessageConstant.CATEGORY_ID_REQUIRED);
        }
        String name = lifeProductCategory.getName();
        if (name != null) {
            checkName(name, lifeProductCategory.getId());
        }
        if (lifeProductCategory.getParentId() != null) {
            checkParent(lifeProductCategory);
        }
        updateById(lifeProductCategory);
    }

    /**
     * 管理端删除分类: 存在子分类或挂载商品时禁止删除, 避免出现孤儿数据
     */
    @Override
    public void removeCategory(Long id) {
        Long childCount = baseMapper.selectCount(new LambdaQueryWrapper<LifeProductCategory>()
                .eq(LifeProductCategory::getParentId, id));
        if (childCount > 0) {
            throw new ValidationException(MessageConstant.CATEGORY_BE_RELATED_PRODUCT);
        }
        Long productCount = lifeProductMapper.selectCount(new LambdaQueryWrapper<LifeProduct>()
                .eq(LifeProduct::getCategoryId, id));
        if (productCount > 0) {
            throw new ValidationException(MessageConstant.CATEGORY_BE_RELATED_PRODUCT);
        }
        removeById(id);
    }

    /**
     * 校验分类名称: 非空且不重复(排除自身)
     */
    private void checkName(String name, Long excludeId) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(MessageConstant.CATEGORY_NAME_REQUIRED);
        }
        LambdaQueryWrapper<LifeProductCategory> wrapper = new LambdaQueryWrapper<LifeProductCategory>()
                .eq(LifeProductCategory::getName, name);
        if (excludeId != null) {
            wrapper.ne(LifeProductCategory::getId, excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ValidationException(MessageConstant.CATEGORY_NAME_EXISTS);
        }
    }

    /**
     * 校验上级分类与层级一致性: parentId=0为一级分类, 其余必须挂载在一级分类下并固定为二级
     */
    private void checkParent(LifeProductCategory lifeProductCategory) {
        Long parentId = lifeProductCategory.getParentId();
        if (parentId == null || parentId == CategoryConstant.ROOT_PARENT_ID) {
            lifeProductCategory.setParentId(CategoryConstant.ROOT_PARENT_ID);
            lifeProductCategory.setLevel(CategoryConstant.LEVEL_FIRST);
            return;
        }
        // 不能把自己挂到自己的子级
        if (parentId.equals(lifeProductCategory.getId())) {
            throw new ValidationException(MessageConstant.CATEGORY_PARENT_ERROR);
        }
        LifeProductCategory parent = baseMapper.selectById(parentId);
        if (parent == null || parent.getParentId() != CategoryConstant.ROOT_PARENT_ID) {
            throw new ValidationException(MessageConstant.CATEGORY_PARENT_ERROR);
        }
        lifeProductCategory.setLevel(CategoryConstant.LEVEL_SECOND);
    }
}
