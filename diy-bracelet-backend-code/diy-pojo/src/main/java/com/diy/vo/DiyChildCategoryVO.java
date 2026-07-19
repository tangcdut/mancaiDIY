package com.diy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiyChildCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 子分类英文键（如 main_bead__white）
     */
    private String keyCode;

    /**
     * 子分类中文名（如 白色系）
     */
    private String name;
}
