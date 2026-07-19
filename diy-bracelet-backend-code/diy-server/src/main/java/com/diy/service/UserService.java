package com.diy.service;

import com.diy.dto.UserLoginDTO;
import com.diy.entity.User;

public interface UserService {
    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
