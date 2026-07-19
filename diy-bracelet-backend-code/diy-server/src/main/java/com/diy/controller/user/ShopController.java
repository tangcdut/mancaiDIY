package com.diy.controller.user;

import com.diy.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@Slf4j
@RequestMapping("/user/shop")
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;
    private static final String KEY="SHOP_STATUS";
    /**
     * 获取店铺状态
     * @return
     */
//    @GetMapping("/status")
//    public Result<Integer> getStatus(){
//        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
//        log.info("获取到的店铺营业状态为:{}",status==1?"营业中":"已打样");
//        return Result.success(status);
//    }
}
