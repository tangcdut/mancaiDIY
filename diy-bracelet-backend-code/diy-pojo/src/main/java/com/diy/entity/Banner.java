package com.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 首页轮播图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Banner implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //图片地址
    private String imageUrl;

    //跳转链接
    private String link;

    //排序
    private Integer sort;
}
