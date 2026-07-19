package com.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 色系实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColorSeries implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 色系键（如white_series、red_series）
    private String keyCode;

    // 色系名称
    private String name;

    // 排序
    private Integer sort;

    // 状态：1启用 0禁用
    private Integer status;

    // 创建时间
    private LocalDateTime createTime;
}
