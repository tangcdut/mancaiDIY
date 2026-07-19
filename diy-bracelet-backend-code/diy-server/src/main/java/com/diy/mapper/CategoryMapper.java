package com.diy.mapper;

import com.github.pagehelper.Page;
import com.diy.annotation.AutoFill;
import com.diy.enumeration.OperationType;
import com.diy.dto.CategoryPageQueryDTO;
import com.diy.entity.Category;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {

    /**
     * 插入数据
     * @param category
     */
    @Insert("insert into category(name, sort, status, create_time)" +
            " VALUES" +
            " (#{name}, #{sort}, #{status}, #{createTime})")
    void insert(Category category);

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据id删除分类
     * @param id
     */
    @Delete("delete from category where id = #{id}")
    void deleteById(Long id);

    /**
     * 根据id修改分类
     * @param category
     */
    void update(Category category);

    /**
     * 查询所有启用的分类
     * @return
     */
    List<Category> list();
    
    /**
     * 根据名称和ID查询分类（用于检查名称是否重复）
     * @param category 查询条件
     * @param id 分类ID（更新时排除自身）
     * @return 分类列表
     */
    List<Category> listByNameAndId(Category category, Long id);
@Select("select name from category where sort=#{categoryId}")
    String getCategoryById(Long categoryId);
}
