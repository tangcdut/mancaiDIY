package com.diy.controller.admin;

import com.diy.dto.ProductPageQueryDTO;
import com.diy.entity.Product;
import com.diy.result.PageResult;
import com.diy.result.Result;
import com.diy.service.ProductService;
import com.diy.vo.ProductDetailVO;
import com.diy.vo.ProductListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController("adminProductController")
@RequestMapping("/admin/product")
@Api(tags = "管理端-商品接口")
@Slf4j
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 商品分页查询
     * @param productPageQueryDTO 分页查询条件
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("商品分页查询")
    public Result<ProductListVO> list(ProductPageQueryDTO productPageQueryDTO) {
        log.info("商品分页查询: {}", productPageQueryDTO);
        
        // 分页查询
        PageResult pageResult = productService.pageQuery(productPageQueryDTO);
        
        // Service层已经返回了ProductItem列表，直接使用
        List<ProductListVO.ProductItem> productItems = (List<ProductListVO.ProductItem>) pageResult.getRecords();
        
        ProductListVO productListVO = ProductListVO.builder()
                .products(productItems)
                .page(productPageQueryDTO.getPage())
                .size(productPageQueryDTO.getPageSize())
                .total(pageResult.getTotal())
                .build();
        
        return Result.success(productListVO);
    }

    /**
     * 根据ID查询商品详情
     * @param id 商品ID
     * @return
     */
    @GetMapping("/detail")
    @ApiOperation("根据ID查询商品详情")
    public Result<ProductDetailVO> getDetail(@RequestParam Long id) {
        log.info("根据ID查询商品详情: id={}", id);
        
        Product product = productService.getById(id);
        
        // 转换为VO
        ProductDetailVO.ProductItem item = new ProductDetailVO.ProductItem();
        BeanUtils.copyProperties(product, item);
        item.setDetailImages(product.getImages());
        
        ProductDetailVO productDetailVO = ProductDetailVO.builder()
                .product(item)
                .build();
        
        return Result.success(productDetailVO);
    }

    /**
     * 新增商品
     * @param product 商品信息
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("新增商品")
    public Result<String> add(@RequestBody Product product) {
        log.info("新增商品: {}", product);
        // 兼容：部分前端“保存”可能一直调用 add，但会携带 id
        // 此时按更新处理，避免出现图片列表未落库/商品重复插入
        if (product.getId() != null) {
            log.warn("/admin/product/add 收到带id请求，按更新处理: id={}", product.getId());
            productService.update(product);
            return Result.success();
        }

        productService.add(product);
        return Result.success();
    }

    /**
     * 修改商品
     * @param product 商品信息
     * @return
     */
    @PostMapping("/update")
    @ApiOperation("修改商品")
    public Result<String> update(@RequestBody Product product) {
        log.info("修改商品: {}", product);
        productService.update(product);
        return Result.success();
    }

    /**
     * 商品上架下架
     * @param id 商品ID
     * @param status 状态：1上架 0下架
     * @return
     */
    @PostMapping("/changeStatus")
    @ApiOperation("商品上架下架")
    public Result<String> changeStatus(@RequestParam Long id, @RequestParam Integer status) {
        log.info("修改商品状态: id={}, status={}", id, status);
        productService.changeStatus(id, status);
        return Result.success();
    }

    /**
     * 删除商品
     * @param id 商品ID
     * @return
     */
    @PostMapping("/delete")
    @ApiOperation("删除商品")
    public Result<String> delete(@RequestParam Long id) {
        log.info("删除商品: id={}", id);
        productService.delete(id);
        return Result.success();
    }

    // ============ 海外价格管理 ============

    /**
     * 查询商品海外价格
     */
    @GetMapping("/overseas-price")
    @ApiOperation("查询商品海外价格")
    public Result<java.util.Map<String, Object>> getOverseasPrice(@RequestParam Long productId) {
        Product product = productService.getById(productId);
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("productId", productId);
        map.put("price", product != null ? product.getPriceOverseas() : null);
        map.put("currency", "USD");
        return Result.success(map);
    }

    /**
     * 保存商品海外价格（新增或更新）
     */
    @PostMapping("/overseas-price/save")
    @ApiOperation("保存商品海外价格")
    public Result<String> saveOverseasPrice(@RequestBody java.util.Map<String, Object> priceData) {
        log.info("保存海外价格: {}", priceData);

        Object productIdObj = priceData.get("productId");
        Object priceObj = priceData.get("price");

        if (productIdObj == null || priceObj == null) {
            return Result.error("productId and price are required");
        }

        Long productId = Long.valueOf(productIdObj.toString());
        BigDecimal price = new BigDecimal(priceObj.toString());

        Product product = Product.builder()
                .id(productId)
                .priceOverseas(price)
                .build();

        productService.update(product);
        return Result.success();
    }
}