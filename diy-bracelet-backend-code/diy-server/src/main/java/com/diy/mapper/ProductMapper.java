package com.diy.mapper;

import com.diy.dto.ProductPageQueryDTO;
import com.diy.entity.Product;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {

    /**
     * 根据分类ID查询商品列表
     * @param categoryId 分类ID，0表示查询所有分类
     * @return 商品列表
     */
    @Select("<script>" +
            "SELECT * FROM product WHERE status = 1 " +
            "<if test='categoryId != null and categoryId > 0'>" +
            "AND category_id = #{categoryId} " +
            "</if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<Product> listByCategoryId(Long categoryId);
    
    /**
     * 根据分类ID查询所有商品（包括下架的）
     * @param categoryId 分类ID，0表示查询所有分类
     * @return 商品列表
     */
    @Select("SELECT * FROM product WHERE category_id = #{categoryId}")
    List<Product> listAllByCategoryId(Long categoryId);
    
    /**
     * 根据ID查询商品详情
     * @param id 商品ID
     * @return 商品详情
     */
    @Select("SELECT p.id, p.category_id, c.name as categoryName, p.title, p.description, " +
            "p.cover_image, p.price, p.price_overseas, p.weight, p.stock, p.status, p.create_time " +
            "FROM product p LEFT JOIN category c ON p.category_id = c.id WHERE p.id = #{id}")
    Product getById(Long id);
    
    /**
     * 商品分页查询
     * @param productPageQueryDTO 分页查询条件
     * @return 分页结果
     */
    Page<Product> pageQuery(ProductPageQueryDTO productPageQueryDTO);
    
    /**
     * 插入商品
     * @param product 商品信息
     */
    @Insert("INSERT INTO product(category_id, title, description, cover_image, price, price_overseas, weight, stock, status, create_time) " +
            "VALUES(#{categoryId}, #{title}, #{description}, #{coverImage}, #{price}, #{priceOverseas}, #{weight}, #{stock}, #{status}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Product product);
    
    /**
     * 更新商品
     * @param product 商品信息
     */
    void update(Product product);
    
    /**
     * 根据ID删除商品
     * @param id 商品ID
     */
    @Delete("DELETE FROM product WHERE id = #{id}")
    void deleteById(Long id);
}