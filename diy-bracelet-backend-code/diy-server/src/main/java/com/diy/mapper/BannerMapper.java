package com.diy.mapper;

import com.diy.entity.Banner;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BannerMapper {

    /**
     * 查询轮播图列表
     */
    @Select("select * from banner order by sort asc")
    List<Banner> list();

    @Select("select * from banner where id=#{id}")
    Banner getById(Long id);
    
    @Insert("insert into banner(image_url, link, sort) values(#{imageUrl}, #{link}, #{sort})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Banner banner);

    @Update("update banner set image_url=#{imageUrl}, link=#{link}, sort=#{sort} where id=#{id}")
    void update(Banner banner);

    @Delete("delete from banner where id=#{id}")
    void delete(Long id);
}
