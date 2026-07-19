package com.diy.controller.admin;

import com.diy.result.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@Slf4j
@Api(tags = "（废弃）管理端-店铺管理")
@RequestMapping("/admin/shop")
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;
    private static final String KEY="SHOP_STATUS";

    /**
     * 设置店铺状态
     * @param status
     * @return
     */
//    @PutMapping("/{status}")
//    public Result setStatus(@PathVariable Integer status){
//        redisTemplate.opsForValue().set(KEY,status);
//        log.info("设置店铺的状态为:{}",status==1?"营业中":"已打样");
//        return Result.success();
//    }

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
