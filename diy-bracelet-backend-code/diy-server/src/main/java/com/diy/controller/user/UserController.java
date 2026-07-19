package com.diy.controller.user;

import com.diy.constant.JwtClaimsConstant;
import com.diy.dto.UserLoginDTO;
import com.diy.entity.User;
import com.diy.properties.JwtProperties;
import com.diy.result.Result;
import com.diy.service.UserService;
import com.diy.utils.JwtUtil;
import com.diy.vo.UserLoginVO;
import io.jsonwebtoken.Jwt;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/user")
@Api(tags = "C端用户相关接口")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("/login")
    @ApiOperation("微信登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("微信授权登录:{}", userLoginDTO.getCode());
        //微信登录
        User user = userService.wxLogin(userLoginDTO);
        //生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID,user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);
        
        // 输出token到控制台供测试使用
        log.info("=====================================");
        log.info("微信登录成功！");
        log.info("用户ID: {}", user.getId());
        log.info("用户OpenID: {}", user.getOpenid());
        log.info("用户昵称: {}", user.getNickname());
        log.info("Token: {}", token);
        log.info("=====================================");
        
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
        return Result.success(userLoginVO);


    }
}
