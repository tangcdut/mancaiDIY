package com.diy.controller.admin;

import com.diy.entity.ColorSeries;
import com.diy.entity.DiyMaterial;
import com.diy.result.PageResult;
import com.diy.result.Result;
import com.diy.service.DesignService;
import com.diy.service.DiyMaterialService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/diy/material")
@Api(tags = "管理端-DIY材料管理")
@Slf4j
public class DiyMaterialController {
    @Autowired
    private DesignService designService;

    @Autowired
    private DiyMaterialService diyMaterialService;

    /**
     * 分页查询材料列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询材料列表")
    public Result<PageResult> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String categoryKey,
            @RequestParam(required = false) String colorSeriesKey) {
        log.info("分页查询材料列表，页码：{}，每页大小：{}，分类：{}，色系：{}", page, pageSize, categoryKey, colorSeriesKey);
        PageResult pageResult = diyMaterialService.page(page, pageSize, categoryKey, colorSeriesKey);
        return Result.success(pageResult);
    }

    /**
     * 查询材料列表（不分页）
     */
    @GetMapping("/list")
    @ApiOperation("查询材料列表")
    public Result<List<DiyMaterial>> list(
            @RequestParam(required = false) String categoryKey,
            @RequestParam(required = false) String colorSeriesKey) {
        log.info("查询材料列表，分类：{}，色系：{}", categoryKey, colorSeriesKey);
        List<DiyMaterial> materials = diyMaterialService.list(categoryKey, colorSeriesKey);
        return Result.success(materials);
    }

    /**
     * 根据ID查询材料详情
     */
    @GetMapping("/{id}")
    @ApiOperation("查询材料详情")
    public Result<DiyMaterial> getById(@PathVariable Long id) {
        log.info("查询材料详情，id: {}", id);
        DiyMaterial material = diyMaterialService.getById(id);
        return Result.success(material);
    }

    /**
     * 新增材料
     */
    @PostMapping
    @ApiOperation("新增材料")
    public Result<String> add(@RequestBody DiyMaterial material) {
        log.info("新增材料：{}", material);
        diyMaterialService.add(material);
        return Result.success();
    }

    /**
     * 更新材料
     */
    @PutMapping
    @ApiOperation("更新材料")
    public Result<String> update(@RequestBody DiyMaterial material) {
        log.info("更新材料：{}", material);
        diyMaterialService.update(material);
        return Result.success();
    }

    /**
     * 删除材料
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除材料")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除材料，id: {}", id);
        diyMaterialService.delete(id);
        return Result.success();
    }

    /**
     * 批量上下架
     */
    @PostMapping("/status/{status}")
    @ApiOperation("批量上下架")
    public Result<String> updateStatus(@PathVariable Integer status, @RequestBody List<Long> ids) {
        log.info("批量修改状态，status: {}, ids: {}", status, ids);
        diyMaterialService.updateStatus(status, ids);
        return Result.success();
    }

    @GetMapping("/colorSeries/list")
    @ApiOperation("查询色系列表")
    public Result<List<ColorSeries>> getColorSeriesList() {
        log.info("查询色系列表");
        List<ColorSeries> colorSeries = designService.getColorSeriesList();
        return Result.success(colorSeries);
    }
}
