package com.diy.service.impl;

import com.diy.entity.Product;
import com.diy.exception.CategoryBusinessException;
import com.diy.mapper.ProductMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.diy.constant.MessageConstant;
import com.diy.constant.StatusConstant;
import com.diy.context.BaseContext;
import com.diy.dto.CategoryDTO;
import com.diy.dto.CategoryPageQueryDTO;
import com.diy.entity.Category;
import com.diy.exception.DeletionNotAllowedException;
import com.diy.mapper.CategoryMapper;
import com.diy.result.PageResult;
import com.diy.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类业务层
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private ProductMapper productMapper;

    /**
     * 新增分类
     * @param categoryDTO
     */
    public void save(CategoryDTO categoryDTO) {
        // 检查分类名称是否已存在
        if (isNameExists(categoryDTO.getName(), null)) {
            throw new CategoryBusinessException("分类名称已存在");
        }
        
        Category category = new Category();
        //属性拷贝
        BeanUtils.copyProperties(categoryDTO, category);

        //分类状态默认为启用状态1
        category.setStatus(StatusConstant.ENABLE);

        //设置创建时间
        category.setCreateTime(LocalDateTime.now());

        categoryMapper.insert(category);
    }

    /**
     * 检查分类名称是否存在
     * @param name 分类名称
     * @param id 分类ID（更新时使用，新增时为null）
     * @return 是否存在
     */
    @Override
    public boolean isNameExists(String name, Long id) {
        Category categoryQuery = Category.builder()
                .name(name)
                .build();
        List<Category> categories = categoryMapper.listByNameAndId(categoryQuery, id);
        return categories != null && !categories.isEmpty();
    }
    
    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        //下一条sql进行分页，自动加入limit关键字分页
        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据id删除分类
     * @param id
     */
    public void deleteById(Long id) {
        // 检查该分类下是否有商品
        List<Product> products = productMapper.listAllByCategoryId(id);
        if (products != null && !products.isEmpty()) {
            throw new CategoryBusinessException("该分类下有商品，不能删除");
        }
        
        //删除分类数据
        categoryMapper.deleteById(id);
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    public void update(CategoryDTO categoryDTO) {
        // 检查分类名称是否已存在
        if (isNameExists(categoryDTO.getName(), categoryDTO.getId())) {
            throw new CategoryBusinessException("分类名称已存在");
        }
        
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO,category);

        categoryMapper.update(category);
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder()
                .id(id)
                .status(status)
                .build();
        categoryMapper.update(category);
    }

    /**
     * 查询所有分类
     * @return
     */
    public List<Category> list() {
        return categoryMapper.list();
    }
}
