package com.diy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiyCategoryWithChildrenVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 父分类英文键（diy_category.key_code，如 main_bead）
     */
    private String keyCode;

    /**
     * 父分类中文名（diy_category.name，如 主珠）
     */
    private String name;

    /**
     * 子分类列表（color_series.key_code 为组合键，如 main_bead__white）
     */
    private List<DiyChildCategoryVO> children;
}
