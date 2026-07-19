package com.diy.service;

import com.diy.dto.CategoryDTO;
import com.diy.dto.CategoryPageQueryDTO;
import com.diy.entity.Category;
import com.diy.result.PageResult;
import java.util.List;

public interface CategoryService {

    /**
     * 新增分类
     * @param categoryDTO
     */
    void save(CategoryDTO categoryDTO);
    
    /**
     * 检查分类名称是否存在
     * @param name 分类名称
     * @param id 分类ID（更新时使用，新增时为null）
     * @return 是否存在
     */
    boolean isNameExists(String name, Long id);

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据id删除分类
     * @param id
     */
    void deleteById(Long id);

    /**
     * 修改分类
     * @param categoryDTO
     */
    void update(CategoryDTO categoryDTO);

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 查询所有分类
     * @return
     */
    List<Category> list();
}
