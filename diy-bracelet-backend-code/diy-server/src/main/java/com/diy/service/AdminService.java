package com.diy.service;

import com.diy.dto.EmployeeLoginDTO;
import com.diy.entity.Admin;

public interface AdminService {

    /**
     * 管理员登录
     * @param employeeLoginDTO
     * @return
     */
    Admin login(EmployeeLoginDTO employeeLoginDTO);
}
