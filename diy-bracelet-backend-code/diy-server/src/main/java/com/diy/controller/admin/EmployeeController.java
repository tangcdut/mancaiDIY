package com.diy.controller.admin;

import com.diy.constant.JwtClaimsConstant;
import com.diy.constant.StatusConstant;
import com.diy.dto.EmployeeDTO;
import com.diy.dto.EmployeeLoginDTO;
import com.diy.dto.EmployeePageQueryDTO;
import com.diy.entity.Admin;
import com.diy.properties.JwtProperties;
import com.diy.result.Result;
import com.diy.service.AdminService;
import com.diy.utils.JwtUtil;
import com.diy.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员管理
 */
@RestController
@RequestMapping("/admin")
@Slf4j
@Api(tags = "管理员相关接口")
public class EmployeeController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @ApiOperation("管理员登录")
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("管理员登录：{}", employeeLoginDTO);

        Admin admin = adminService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, admin.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(admin.getId())
                .userName(admin.getUsername())
                .name(admin.getUsername())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation("管理员退出")
    public Result<String> logout() {
        return Result.success();
    }
}
