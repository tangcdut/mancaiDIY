package com.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品海外价格实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceOverseas implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 商品ID */
    private Long productId;

    /** 海外价格（USD） */
    private BigDecimal price;

    /** 货币代码 */
    private String currency;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
