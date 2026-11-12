package com.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 风控记录(2.10/3.3): 高频操作命中规则留痕, 管理端排查与拦截率统计(4.1)数据源
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("life_risk_record")
public class LifeRiskRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String ip;

    /** 设备指纹(前端生成, 随 X-Device-Fingerprint 请求头上送) */
    private String deviceFingerprint;

    /** 动作类型: SIGN/POST/COMMENT/LIKE/VIEW/CLAIM/ORDER */
    private String actionType;

    /** 风险级别: 1-高频预警, 2-标记限制 */
    private Integer riskLevel;

    /** 命中规则描述 */
    private String hitRule;
}
