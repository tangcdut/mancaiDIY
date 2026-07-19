package com.diy.controller.user;

import com.diy.entity.Product;
import com.diy.result.Result;
import com.diy.service.ProductService;
import com.diy.vo.ProductDetailVO;
import com.diy.vo.ProductListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController("userProductController")
@RequestMapping("/user/product")
@Api(tags = "C端-商品接口")
@Slf4j
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 根据分类ID查询商品列表
     * 支持 region=overseas 参数，返回USD价格
     */
    @GetMapping("/list")
    @ApiOperation("根据分类ID查询商品列表")
    public Result<ProductListVO> listByCategoryId(
            @ApiParam("分类ID") @RequestParam Long categoryId,
            @ApiParam("区域标识(overseas返回USD价格)") @RequestParam(required = false, defaultValue = "") String region) {
        log.info("根据分类ID查询商品列表: categoryId={}, region={}", categoryId, region);

        List<Product> products = productService.listByCategoryId(categoryId);

        // 海外版：注入USD价格
        final boolean isOverseas = "overseas".equalsIgnoreCase(region);

        // 转换为VO
        List<ProductListVO.ProductItem> productItems = products.stream().map(product -> {
            ProductListVO.ProductItem item = new ProductListVO.ProductItem();
            BeanUtils.copyProperties(product, item);

            if (isOverseas && product.getPriceOverseas() != null) {
                item.setPrice(product.getPriceOverseas());
            }
            return item;
        }).collect(Collectors.toList());

        ProductListVO productListVO = ProductListVO.builder()
                .products(productItems)
                .build();

        return Result.success(productListVO);
    }

    /**
     * 根据ID查询商品详情
     * 支持 region=overseas 参数，返回USD价格
     */
    @GetMapping("/detail")
    @ApiOperation("根据ID查询商品详情")
    public Result<ProductDetailVO> getById(
            @ApiParam("商品ID") @RequestParam Long id,
            @ApiParam("区域标识(overseas返回USD价格)") @RequestParam(required = false, defaultValue = "") String region) {
        log.info("根据ID查询商品详情: id={}, region={}", id, region);

        Product product = productService.getById(id);

        final boolean isOverseas = "overseas".equalsIgnoreCase(region);

        // 转换为VO
        ProductDetailVO.ProductItem item = new ProductDetailVO.ProductItem();
        BeanUtils.copyProperties(product, item);
        item.setDetailImages(product.getImages());

        if (isOverseas && product.getPriceOverseas() != null) {
            item.setPrice(product.getPriceOverseas());
        }

        ProductDetailVO productDetailVO = ProductDetailVO.builder()
                .product(item)
                .build();

        return Result.success(productDetailVO);
    }
}