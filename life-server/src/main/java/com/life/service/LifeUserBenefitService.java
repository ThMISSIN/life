package com.life.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.life.entity.LifeUserBenefit;
import com.life.query.UserBenefitQueryDTO;
import com.life.result.PageResult;
import com.life.vo.UserBenefitVO;

public interface LifeUserBenefitService extends IService<LifeUserBenefit> {

    /**
     * C端「我的权益」分页(仅本人数据, 列表不下发券码)
     */
    PageResult queryMyPage(UserBenefitQueryDTO query);

    /**
     * C端「我的权益」详情(含券码)
     */
    UserBenefitVO getMyDetail(Long id);

    /**
     * C端核销(使用): 仅未使用且未过期的券可核销
     */
    void verifyMyBenefit(Long id);

    /**
     * 管理端发放记录分页(可按用户/权益/订单号/券码筛选, 含券码)
     */
    PageResult queryAdminPage(UserBenefitQueryDTO query);

    /**
     * 管理端作废: 作废凭证 + 退积分(写流水) + 回库存(减销量)
     */
    void voidUserBenefit(Long id);
}
