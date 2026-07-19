package com.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //分类ID
    private Long categoryId;

    //分类名称（非数据库字段，用于关联查询）
    private String categoryName;

    //商品标题
    private String title;

    //商品描述
    private String description;

    //商品封面图
    private String coverImage;

    //商品图片列表（非数据库字段）
    private List<String> images;

    //价格
    private BigDecimal price;

    //海外价格
    private BigDecimal priceOverseas;

    //克重（单位：克）
    private BigDecimal weight;

    //库存
    private Integer stock;

    //1上架 0下架
    private Integer status;

    //创建时间
    private LocalDateTime createTime;
}
