package com.diy.mapper;

import com.diy.entity.ProductImage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductImageMapper {

    @Select("select image_url from product_image where product_id = #{productId} order by sort asc, id asc")
    List<String> listUrlsByProductId(@Param("productId") Long productId);

    @Delete("delete from product_image where product_id = #{productId}")
    void deleteByProductId(@Param("productId") Long productId);

    @Update("update product_image set image_url = #{imageUrl} where product_id = #{productId} and is_cover = 1")
    int updateCoverUrl(@Param("productId") Long productId, @Param("imageUrl") String imageUrl);

    @Insert("insert into product_image(product_id, image_url, sort, is_cover, create_time) values(#{productId}, #{imageUrl}, #{sort}, #{isCover}, #{createTime})")
    void insert(ProductImage productImage);

    @Insert({
            "<script>",
            "insert into product_image(product_id, image_url, sort, is_cover, create_time) values",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.productId}, #{item.imageUrl}, #{item.sort}, #{item.isCover}, #{item.createTime})",
            "</foreach>",
            "</script>"
    })
    void insertBatch(@Param("list") List<ProductImage> list);
}
