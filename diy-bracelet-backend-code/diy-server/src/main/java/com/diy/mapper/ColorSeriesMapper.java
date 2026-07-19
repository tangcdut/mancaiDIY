package com.diy.mapper;

import com.diy.entity.ColorSeries;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ColorSeriesMapper {

    /**
     * 查询所有启用的色系
     * @return
     */
    @Select("SELECT id, key_code, name, sort, status, create_time FROM color_series WHERE status = 1 ORDER BY sort ASC")
    List<ColorSeries> list();
}
