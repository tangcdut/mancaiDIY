package com.diy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeNode implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    
    // 分类键（如main_bead、accessory、rope）
    private String keyCode;
    
    // 分类名称
    private String name;
    
    // 排序
    private Integer sort;
    
    // 状态：1启用 0禁用
    private Integer status;
    
    // 子分类列表
    @Builder.Default
    private List<CategoryTreeNode> children = new ArrayList<>();
    
    // 是否为叶子节点（无子分类）
    private boolean leaf;
    
    // 层级（0表示顶级分类）
    private int level;
}