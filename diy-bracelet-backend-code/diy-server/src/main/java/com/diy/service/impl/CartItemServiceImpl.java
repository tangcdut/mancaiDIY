package com.diy.service.impl;

import com.diy.context.BaseContext;
import com.diy.dto.AddToCartDTO;
import com.diy.dto.DeleteFromCartDTO;
import com.diy.dto.ShoppingCartDTO;
import com.diy.entity.CartItem;
import com.diy.mapper.ShoppingCartMapper;
import com.diy.service.CartItemService;
import com.diy.vo.CartItemListVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CartItemServiceImpl implements CartItemService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    private static final String IMAGE_URL_PREFEX="https://cloud.xiaotangstory.top";

    /**
     * 添加到购物车
     * @param shoppingCartDTO
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        //TODO: 实现添加到购物车逻辑
        // 1. 检查商品是否已在购物车中
        // 2. 如果存在，更新数量
        // 3. 如果不存在，插入新记录
    }
    
    /**
     * 添加商品到购物车（新接口）
     * @param addToCartDTO
     * @return
     */
    @Override
    public CartItem addToCart(AddToCartDTO addToCartDTO) {
        Long userId = BaseContext.getCurrentId();
        Long productId = addToCartDTO.getProductId();
        Integer quantity = addToCartDTO.getQuantity();
        
        // 构造查询条件
        CartItem cartItemQuery = CartItem.builder()
                .userId(userId)
                .productId(productId)
                .build();
        
        // 查询购物车中是否已存在该商品
        List<CartItem> cartItems = shoppingCartMapper.list(cartItemQuery);
        
        CartItem cartItemResult;
        if (cartItems != null && !cartItems.isEmpty()) {
            // 如果存在，更新数量
            CartItem existingItem = cartItems.get(0);
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            shoppingCartMapper.updateQuantityById(existingItem);
            cartItemResult = existingItem;
        } else {
            // 如果不存在，插入新记录
            CartItem newItem = CartItem.builder()
                    .userId(userId)
                    .productId(productId)
                    .quantity(quantity)
                    .createTime(LocalDateTime.now())
                    .build();
            shoppingCartMapper.insert(newItem);
            cartItemResult = newItem;
        }
        
        return cartItemResult;
    }

    /**
     * 查看购物车
     */
    @Override
    public List<CartItem> list() {
        Long userId = BaseContext.getCurrentId();
        CartItem cartItem = CartItem.builder()
                .userId(userId)
                .build();
        return shoppingCartMapper.list(cartItem);
    }
    
    /**
     * 查看购物车（包含商品信息）
     */
    @Override
    public CartItemListVO listWithProductInfo() {
        Long userId = BaseContext.getCurrentId();
        List<Map<String, Object>> cartItems = shoppingCartMapper.listWithProductInfo(userId);
        
        // 转换为VO
        List<CartItemListVO.CartItem> items = cartItems.stream().map(map -> {
            CartItemListVO.CartItem item = new CartItemListVO.CartItem();
            item.setProductId((Long) map.get("productId"));
            item.setTitle((String) map.get("title"));
            item.setPrice((BigDecimal) map.get("price"));
            item.setQuantity((Integer) map.get("quantity"));
            //拼接图片地址
            item.setCoverImage(IMAGE_URL_PREFEX+((String) map.get("coverImage")));
            return item;
        }).collect(Collectors.toList());
        
        return CartItemListVO.builder()
                .items(items)
                .build();
    }

    /**
     * 清空购物车
     */
    @Override
    public void clean() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 删除购物车中的一个商品
     */
    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        //TODO: 实现减少商品数量逻辑
    }
    
    /**
     * 从购物车删除商品（新接口）- 直接删除整个商品，不管数量多少
     * @param deleteFromCartDTO
     */
    @Override
    public void deleteFromCart(DeleteFromCartDTO deleteFromCartDTO) {
        Long userId = BaseContext.getCurrentId();
        Long productId = deleteFromCartDTO.getProductId();
        
        log.info("删除购物车商品，用户ID: {}, 商品ID: {}", userId, productId);
        
        // 构造查询条件
        CartItem cartItemQuery = CartItem.builder()
                .userId(userId)
                .productId(productId)
                .build();
        
        // 查询购物车中是否已存在该商品
        List<CartItem> cartItems = shoppingCartMapper.list(cartItemQuery);
        
        if (cartItems != null && !cartItems.isEmpty()) {
            CartItem existingItem = cartItems.get(0);
            // 直接删除整条记录，不管数量多少
            shoppingCartMapper.deleteById(existingItem.getId());
            log.info("成功删除购物车商品，购物车项ID: {}", existingItem.getId());
        }
    }
}
