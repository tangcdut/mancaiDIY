package com.diy.service;

import com.diy.dto.CategoryTreeNode;
import com.diy.entity.DiyCategory;
import java.util.List;

public interface DiyCategoryService {
    
    /**
     * 查询所有启用的DIY分类
     * @return
     */
    List<DiyCategory> list();
    
    /**
     * 获取分类树形结构
     * @return
     */
    List<CategoryTreeNode> getCategoryTree();
}