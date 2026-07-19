package com.diy.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.diy.constant.MessageConstant;
import com.diy.dto.UserLoginDTO;
import com.diy.entity.User;
import com.diy.exception.LoginFailedException;
import com.diy.mapper.UserMapper;
import com.diy.properties.WeChatProperties;
import com.diy.service.UserService;
import com.diy.utils.HttpClientUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

/**
 * 
 *
 * @author 
 * @since 2022-01-01
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    //微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        // 调用微信接口服务获取用户的openid
        // 开发工具中 wx.login() 返回的 code 是有效的，jscode2session 正常校验通过
        // 微信服务端自行区分测试/正式 openid，业务代码无需干预
        log.info("开始调用微信接口获取openid，code: {}", userLoginDTO.getCode());
        String openid = getOpenId(userLoginDTO.getCode());
        log.info("微信接口返回openid: {}", openid);
        
        if (openid == null) {
            log.error("获取openid失败，code可能已过期或无效");
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判断当前用户是否为新用户
        User user = userMapper.getByOpenId(openid);
        log.info("查询用户信息，openid: {}, user: {}", openid, user);
        //新用户,注册
        String nickname = userLoginDTO.getNickName();
        String avatar = userLoginDTO.getAvatarUrl();

        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .nickname(StringUtils.hasText(nickname) ? nickname : null)
                    .avatar(StringUtils.hasText(avatar) ? avatar : null)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
            log.info("新增用户，id: {}", user.getId());
        } else {
            boolean needUpdate = false;
            User update = new User();
            update.setId(user.getId());
            if (StringUtils.hasText(nickname) && !nickname.equals(user.getNickname())) {
                update.setNickname(nickname);
                needUpdate = true;
                user.setNickname(nickname);
            }
            if (StringUtils.hasText(avatar) && !avatar.equals(user.getAvatar())) {
                update.setAvatar(avatar);
                needUpdate = true;
                user.setAvatar(avatar);
            }
            if (needUpdate) {
                userMapper.update(update);
            }
        }
        //返回用户对象
        return user;
    }

    /**
     * 调用微信接口服务,获取微信用户的openid
     *
     * @param code
     * @return
     */
    private String getOpenId(String code) {
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        
        log.info("调用微信登录接口参数: appid={}, secret={}, js_code={}, grant_type={}", 
                weChatProperties.getAppid(), "******", code, "authorization_code");
        
        try {
            String json = HttpClientUtil.doGet(WX_LOGIN, map);
            log.info("微信登录接口返回结果: {}", json);
            
            //判断是否有错误信息
            JSONObject jsonObject = JSON.parseObject(json);
            
            // 检查是否有错误码
            if (jsonObject.containsKey("errcode")) {
                Integer errcode = jsonObject.getInteger("errcode");
                String errmsg = jsonObject.getString("errmsg");
                log.error("微信登录接口返回错误: errcode={}, errmsg={}", errcode, errmsg);
                return null;
            }
            
            String openid = jsonObject.getString("openid");
            return openid;
        } catch (Exception e) {
            log.error("调用微信登录接口异常: ", e);
            return null;
        }
    }
}