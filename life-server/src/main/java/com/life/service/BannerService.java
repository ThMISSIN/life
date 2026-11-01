package com.life.service;

import com.life.vo.BannerVO;

import java.util.List;

/**
 * Banner服务(1.5): C端只读列表
 */
public interface BannerService {

    /**
     * 按位置查询上架Banner(生效时间窗内, sort升序)
     *
     * @param position 位置标识: home/benefit/mall 或 1/2/3; 空查全部
     */
    List<BannerVO> queryByPosition(String position);
}
