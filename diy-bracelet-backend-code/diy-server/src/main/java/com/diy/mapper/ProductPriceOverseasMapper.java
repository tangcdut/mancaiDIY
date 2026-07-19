package com.diy.mapper;

import com.diy.entity.ProductPriceOverseas;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品海外价格Mapper
 */
@Mapper
public interface ProductPriceOverseasMapper {

    /**
     * 根据商品ID和货币查询海外价格
     */
    @Select("select * from product_price_overseas where product_id = #{productId} and currency = #{currency}")
    ProductPriceOverseas getByProductIdAndCurrency(@Param("productId") Long productId, @Param("currency") String currency);

    /**
     * 根据商品ID查询海外价格（默认USD）
     */
    @Select("select * from product_price_overseas where product_id = #{productId} and currency = 'USD'")
    ProductPriceOverseas getByProductId(@Param("productId") Long productId);

    /**
     * 批量查询商品海外价格
     */
    @Select("<script>select * from product_price_overseas where product_id in " +
            "<foreach collection='productIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}</foreach> and currency = #{currency}</script>")
    List<ProductPriceOverseas> batchGetByProductIds(@Param("productIds") List<Long> productIds, @Param("currency") String currency);

    /**
     * 插入海外价格
     */
    @Insert("insert into product_price_overseas (product_id, price, currency, create_time) values (#{productId}, #{price}, #{currency}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ProductPriceOverseas price);

    /**
     * 更新海外价格
     */
    @Update("update product_price_overseas set price = #{price}, currency = #{currency}, update_time = now() where id = #{id}")
    void update(ProductPriceOverseas price);

    /**
     * 更新或插入海外价格（upsert）
     */
    @Update("insert into product_price_overseas (product_id, price, currency, create_time) values (#{productId}, #{price}, #{currency}, now()) " +
            "on duplicate key update price = values(price), update_time = now()")
    void upsert(ProductPriceOverseas price);

    /**
     * 删除海外价格
     */
    @Delete("delete from product_price_overseas where id = #{id}")
    void delete(@Param("id") Long id);

    /**
     * 根据商品ID删除所有海外价格
     */
    @Delete("delete from product_price_overseas where product_id = #{productId}")
    void deleteByProductId(@Param("productId") Long productId);
}
