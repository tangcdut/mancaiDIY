package com.diy.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProductPageQueryDTO implements Serializable {

    //页码
    private int page = 1;

    //每页记录数
    private int pageSize = 20;

    //分类ID
    private Long categoryId;

    //状态：1上架 0下架
    private Integer status;

    //商品标题
    private String title;
}