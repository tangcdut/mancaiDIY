package com.diy.controller.user;

import com.diy.dto.AddToCartDTO;
import com.diy.dto.DeleteFromCartDTO;
import com.diy.dto.ShoppingCartDTO;
import com.diy.entity.CartItem;
import com.diy.result.Result;
import com.diy.service.CartItemService;
import com.diy.vo.AddToCartVO;
import com.diy.vo.CartItemListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/cart")
@Slf4j
@Api(tags = "C端购物车")
public class ShoppingCartController {
    @Autowired
    private CartItemService cartItemService;

//    /**
//     * 添加购物车（旧接口）
//     * @param shoppingCartDTO
//     * @return
//     */
//    @PostMapping("/add")
//    @ApiOperation("添加购物车")
//    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
//        log.info("添加购物车,商品信息:{}", shoppingCartDTO);
//        cartItemService.add(shoppingCartDTO);
//        return Result.success();
//    }
    
    /**
     * 添加商品进购物车（新接口）
     * @param addToCartDTO
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("添加商品进购物车")
    public Result<AddToCartVO> addToCart(
            @ApiParam("添加购物车参数") @RequestBody AddToCartDTO addToCartDTO) {
        log.info("添加商品进购物车,商品信息:{}", addToCartDTO);
        
        CartItem cartItem = cartItemService.addToCart(addToCartDTO);
        
        // 转换为VO
        AddToCartVO.CartItem item = new AddToCartVO.CartItem();
        item.setProductId(cartItem.getProductId());
        item.setQuantity(cartItem.getQuantity());
        
        AddToCartVO addToCartVO = AddToCartVO.builder()
                .cartItem(item)
                .build();
        
        return Result.success(addToCartVO);
    }

    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查看购物车")
    public Result<CartItemListVO> list() {
        CartItemListVO cartItemListVO = cartItemService.listWithProductInfo();
        return Result.success(cartItemListVO);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    @ApiOperation("清空购物车")
    public Result clean() {
        cartItemService.clean();
        return Result.success();
    }

    /**
     * 删除购物车中的一个商品
     * @return
     */
    @PostMapping("/sub")
    @ApiOperation("删除购物车中的一个商品")
    public Result sub(@RequestBody ShoppingCartDTO shoppingCartDTO){
        cartItemService.sub(shoppingCartDTO);
        return Result.success();
    }
    
    /**
     * 从购物车删除商品（新接口）
     * @param deleteFromCartDTO
     * @return
     */
    @PostMapping("/delete")
    @ApiOperation("从购物车删除商品")
    public Result deleteFromCart(
            @ApiParam("删除购物车商品参数") @RequestBody DeleteFromCartDTO deleteFromCartDTO) {
        log.info("从购物车删除商品,商品ID:{}", deleteFromCartDTO.getProductId());
        
        cartItemService.deleteFromCart(deleteFromCartDTO);
        
        return Result.success();
    }
}
