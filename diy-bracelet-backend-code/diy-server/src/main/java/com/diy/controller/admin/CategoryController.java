package com.diy.controller.admin;

import com.diy.dto.CategoryDTO;
import com.diy.dto.CategoryPageQueryDTO;
import com.diy.entity.Category;
import com.diy.result.PageResult;
import com.diy.result.Result;
import com.diy.service.CategoryService;
import com.diy.vo.CategoryAddVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Api(tags = "分类相关接口")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类
     * @param categoryDTO
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("新增分类")
    public Result<CategoryAddVO> save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增分类：{}", categoryDTO);
        categoryService.save(categoryDTO);
        
        // 查询刚插入的分类ID
        List<Category> categories = categoryService.list();
        Category category = categories.stream()
                .filter(c -> c.getName().equals(categoryDTO.getName()))
                .findFirst()
                .orElse(null);
        
        if (category != null) {
            CategoryAddVO categoryAddVO = CategoryAddVO.builder()
                    .categoryId(category.getId())
                    .build();
            return Result.success(categoryAddVO);
        }
        
        return Result.success();
    }

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分类分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页查询：{}", categoryPageQueryDTO);
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 删除分类
     * @param categoryDTO
     * @return
     */
    @PostMapping("/delete")
    @ApiOperation("删除分类")
    public Result<String> deleteById(@RequestBody CategoryDTO categoryDTO){
        log.info("删除分类：{}", categoryDTO.getId());
        categoryService.deleteById(categoryDTO.getId());
        return Result.success();
    }

    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @PostMapping("/update")
    @ApiOperation("修改分类")
    public Result<String> update(@RequestBody CategoryDTO categoryDTO){
        categoryService.update(categoryDTO);
        return Result.success();
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用分类")
    public Result<String> startOrStop(@PathVariable("status") Integer status, Long id){
        categoryService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * 查询所有分类
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询所有分类")
    public Result<List<Category>> list(){
        List<Category> list = categoryService.list();
        return Result.success(list);
    }
}
