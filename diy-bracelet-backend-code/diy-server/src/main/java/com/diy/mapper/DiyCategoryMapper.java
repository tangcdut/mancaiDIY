package com.diy.mapper;

import com.diy.entity.DiyCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DiyCategoryMapper {

    /**
     * 查询所有启用的DIY分类
     * @return
     */
    @Select("SELECT id, key_code, name, sort, status, create_time FROM diy_category WHERE status = 1 ORDER BY sort ASC")
    List<DiyCategory> list();
    
    /**
     * 查询所有DIY分类（包括禁用的）
     * @return
     */
    @Select("SELECT id, key_code, name, sort, status, create_time FROM diy_category ORDER BY sort ASC")
    List<DiyCategory> listAll();
}
