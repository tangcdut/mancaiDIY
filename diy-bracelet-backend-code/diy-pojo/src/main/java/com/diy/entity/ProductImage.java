package com.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品图片明细
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    private String imageUrl;

    private Integer sort;

    /**
     * 是否封面：1是 0否
     */
    private Integer isCover;

    private LocalDateTime createTime;
}
