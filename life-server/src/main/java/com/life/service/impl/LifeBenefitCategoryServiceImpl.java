package com.life.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.life.constant.MessageConstant;
import com.life.entity.LifeBenefit;
import com.life.entity.LifeBenefitCategory;
import com.life.exception.ValidationException;
import com.life.mapper.LifeBenefitCategoryMapper;
import com.life.mapper.LifeBenefitMapper;
import com.life.service.LifeBenefitCategoryService;
import com.life.vo.LifeBenefitCategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LifeBenefitCategoryServiceImpl extends ServiceImpl<LifeBenefitCategoryMapper, LifeBenefitCategory> implements LifeBenefitCategoryService {

    @Autowired
    private LifeBenefitMapper lifeBenefitMapper;

    @Override
    public List<LifeBenefitCategoryVO> getCategoryTree() {
        // 查询所有 分类(deleted由MP逻辑删除自动过滤)
        LambdaQueryWrapper<LifeBenefitCategory> eq = new LambdaQueryWrapper<LifeBenefitCategory>()
                .eq(LifeBenefitCategory::getStatus, 1)
                .orderByAsc(LifeBenefitCategory::getSort);

        List<LifeBenefitCategory> categories = baseMapper.selectList(eq);

        // 过滤出所有一级分类
        List<LifeBenefitCategory> rootList = categories.stream()
                .filter(c -> c.getParentId() == 0)
                .toList();

        // 找出一级分类下的所有二级分类
        List<LifeBenefitCategoryVO> vos = new ArrayList<>();

        for (LifeBenefitCategory root : rootList) {
            LifeBenefitCategoryVO vo = new LifeBenefitCategoryVO();
            // 过滤出二级分类
            List<LifeBenefitCategoryVO> sonList = categories.stream()
                    .filter(c -> c.getParentId().equals(root.getId()))
                    .map(c -> LifeBenefitCategoryVO
                            .builder()
                            .id(c.getId())
                            .name(c.getName())
                            .icon(c.getIcon())
                            .build())
                    .toList();
            // 为父级分类 赋值 以及 设置子级分类
            vo.setId(root.getId());
            vo.setName(root.getName());
            vo.setIcon(root.getIcon());
            vo.setChildren(sonList);
            // 将父级分类vo添加到结果中
            vos.add(vo);
        }

        return vos;
    }

    /**
     * 管理端新增分类: 校验名称、上级分类与层级一致性
     */
    @Override
    public void addCategory(LifeBenefitCategory lifeBenefitCategory) {
        checkName(lifeBenefitCategory.getName(), null);
        checkParent(lifeBenefitCategory);
        save(lifeBenefitCategory);
    }

    /**
     * 管理端修改分类: 名称与层级仅在传值时校验
     */
    @Override
    public void updateCategory(LifeBenefitCategory lifeBenefitCategory) {
        if (lifeBenefitCategory.getId() == null) {
            throw new ValidationException(MessageConstant.CATEGORY_ID_REQUIRED);
        }
        String name = lifeBenefitCategory.getName();
        if (name != null) {
            checkName(name, lifeBenefitCategory.getId());
        }
        if (lifeBenefitCategory.getParentId() != null) {
            checkParent(lifeBenefitCategory);
        }
        updateById(lifeBenefitCategory);
    }

    /**
     * 管理端删除分类: 存在子分类或挂载权益时禁止删除, 避免出现孤儿数据
     */
    @Override
    public void removeCategory(Long id) {
        Long childCount = baseMapper.selectCount(new LambdaQueryWrapper<LifeBenefitCategory>()
                .eq(LifeBenefitCategory::getParentId, id));
        if (childCount > 0) {
            throw new ValidationException(MessageConstant.CATEGORY_BE_RELATED);
        }
        Long benefitCount = lifeBenefitMapper.selectCount(new LambdaQueryWrapper<LifeBenefit>()
                .eq(LifeBenefit::getCategoryId, id));
        if (benefitCount > 0) {
            throw new ValidationException(MessageConstant.CATEGORY_BE_RELATED);
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
        LambdaQueryWrapper<LifeBenefitCategory> wrapper = new LambdaQueryWrapper<LifeBenefitCategory>()
                .eq(LifeBenefitCategory::getName, name);
        if (excludeId != null) {
            wrapper.ne(LifeBenefitCategory::getId, excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ValidationException(MessageConstant.CATEGORY_NAME_EXISTS);
        }
    }

    /**
     * 校验上级分类与层级一致性: parentId=0为一级分类, 其余必须挂载在一级分类下并固定为二级
     */
    private void checkParent(LifeBenefitCategory lifeBenefitCategory) {
        Long parentId = lifeBenefitCategory.getParentId();
        if (parentId == null || parentId == 0) {
            lifeBenefitCategory.setParentId(0L);
            lifeBenefitCategory.setLevel(1);
            return;
        }
        // 不能把自己挂到自己的子级
        if (parentId.equals(lifeBenefitCategory.getId())) {
            throw new ValidationException(MessageConstant.CATEGORY_PARENT_ERROR);
        }
        LifeBenefitCategory parent = baseMapper.selectById(parentId);
        if (parent == null || parent.getParentId() != 0) {
            throw new ValidationException(MessageConstant.CATEGORY_PARENT_ERROR);
        }
        lifeBenefitCategory.setLevel(2);
    }
}
