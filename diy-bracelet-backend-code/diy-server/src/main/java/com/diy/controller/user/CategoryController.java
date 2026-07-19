package com.diy.controller.user;

import com.diy.entity.Category;
import com.diy.result.Result;
import com.diy.service.CategoryService;
import com.diy.vo.CategoryListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController("userCategoryController")
@RequestMapping("/user/category")
@Api(tags = "用户端分类接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 查询分类列表
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询分类列表")
    public Result<CategoryListVO> list() {
        List<Category> categories = categoryService.list();
        
        // 转换为VO
        List<CategoryListVO.CategoryItem> categoryItems = categories.stream().map(category -> {
            CategoryListVO.CategoryItem item = new CategoryListVO.CategoryItem();
            BeanUtils.copyProperties(category, item);
            return item;
        }).collect(Collectors.toList());
        
        CategoryListVO categoryListVO = CategoryListVO.builder()
                .categories(categoryItems)
                .build();
        
        return Result.success(categoryListVO);
    }
}
