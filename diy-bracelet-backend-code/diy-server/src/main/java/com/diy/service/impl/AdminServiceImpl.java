package com.diy.service.impl;

import com.diy.constant.MessageConstant;
import com.diy.dto.EmployeeLoginDTO;
import com.diy.entity.Admin;
import com.diy.exception.AccountNotFoundException;
import com.diy.exception.PasswordErrorException;
import com.diy.mapper.EmployeeMapper;
import com.diy.service.AdminService;
import com.diy.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 管理员登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Admin login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Admin employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对 - 将输入的明文密码进行MD5加密后与数据库中的密文比对
        if (!PasswordUtil.matches(password, employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        //3、转换为Admin对象返回
        Admin admin = Admin.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .password(employee.getPassword())
                .createTime(employee.getCreateTime())
                .build();
        return admin;
    }
}
