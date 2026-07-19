package com.diy.service;

import com.diy.dto.ProductPageQueryDTO;
import com.diy.entity.Product;
import com.diy.result.PageResult;
import java.util.List;

public interface ProductService {

    /**
     * 根据分类ID查询商品列表
     * @param categoryId 分类ID
     * @return 商品列表
     */
    List<Product> listByCategoryId(Long categoryId);
    
    /**
     * 根据ID查询商品详情
     * @param id 商品ID
     * @return 商品详情
     */
    Product getById(Long id);
    
    /**
     * 商品分页查询
     * @param productPageQueryDTO 分页查询条件
     * @return 分页结果
     */
    PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO);
    
    /**
     * 新增商品
     * @param product 商品信息
     */
    void add(Product product);
    
    /**
     * 修改商品
     * @param product 商品信息
     */
    void update(Product product);
    
    /**
     * 修改商品状态（上架/下架）
     * @param id 商品ID
     * @param status 状态：1上架 0下架
     */
    void changeStatus(Long id, Integer status);
    
    /**
     * 删除商品
     * @param id 商品ID
     */
    void delete(Long id);
}