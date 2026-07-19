package com.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DIY材料实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiyMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 分类键（关联diy_category.key_code）
    private String categoryKey;

    // 色系键（关联color_series.key_code）
    private String colorSeriesKey;

    // 材料名称
    private String title;

    // 材料描述
    private String description;

    // 尺寸（单位：mm）
    private BigDecimal size;

    // 颜色值（CSS颜色或渐变）
    private String color;

    // 单价
    private BigDecimal price;

    // 海外版单价（美元）
    private BigDecimal priceOverseas;

    // 库存数量
    private Integer stock;

    // 单个重量（单位：克）
    private BigDecimal weight;

    // 图片地址
    private String imageUrl;

    // 实拍展示图1
    private String realImageUrl1;

    // 实拍展示图2
    private String realImageUrl2;

    // 排序
    private Integer sort;

    // 状态：1上架 0下架
    private Integer status;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
