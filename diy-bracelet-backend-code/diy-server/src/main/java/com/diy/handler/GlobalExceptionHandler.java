package com.diy.handler;

import com.diy.constant.MessageConstant;
import com.diy.exception.BaseException;
import com.diy.exception.CategoryBusinessException;
import com.diy.exception.LoginFailedException;
import com.diy.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }
    
    /**
     * 捕获登录失败异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(LoginFailedException ex){
        log.error("登录失败异常信息：{}", ex.getMessage());
        return Result.error("登录失败：" + ex.getMessage());
    }
    
    /**
     * 捕获分类业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(CategoryBusinessException ex){
        log.error("分类业务异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }
    
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        //Duplicate entry 'zhangsan' for key 'employee.idx_username'
        String message=ex.getMessage();
        if(message.contains("Duplicate entry")){
            String[] split = message.split(" ");
            String username=split[2];
            String msg=username+ MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        }
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
    
    /**
     * 捕获参数校验异常（@Validated）
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(MethodArgumentNotValidException ex){
        log.error("参数校验异常：{}", ex.getMessage());
        String errorMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        return Result.error(errorMessage);
    }
    
    /**
     * 捕获参数绑定异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BindException ex){
        log.error("参数绑定异常：{}", ex.getMessage());
        String errorMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        return Result.error(errorMessage);
    }
    
    /**
     * 捕获约束违反异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(ConstraintViolationException ex){
        log.error("约束违反异常：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }
    
    /**
     * 捕获运行时异常（兜底处理）
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(RuntimeException ex){
        log.error("运行时异常：{}", ex.getMessage(), ex);
        return Result.error(ex.getMessage());
    }
    
    /**
     * 捕获所有未处理的异常（最后的兜底）
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(Exception ex){
        log.error("系统异常：{}", ex.getMessage(), ex);
        return Result.error("系统繁忙，请稍后再试");
    }
}