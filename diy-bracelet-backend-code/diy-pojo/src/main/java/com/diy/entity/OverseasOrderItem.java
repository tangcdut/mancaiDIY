package com.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 海外版订单商品明细
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverseasOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 商品标题（下单时快照） */
    private String title;

    /** 单价 */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 商品图片URL */
    private String productImage;
}
