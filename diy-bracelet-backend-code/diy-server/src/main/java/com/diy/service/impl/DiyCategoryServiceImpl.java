package com.diy.service.impl;

import com.diy.dto.CategoryTreeNode;
import com.diy.entity.DiyCategory;
import com.diy.mapper.DiyCategoryMapper;
import com.diy.service.DiyCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DiyCategoryServiceImpl implements DiyCategoryService {

    @Autowired
    private DiyCategoryMapper diyCategoryMapper;

    @Override
    public List<DiyCategory> list() {
        return diyCategoryMapper.list();
    }

    @Override
    public List<CategoryTreeNode> getCategoryTree() {
        // 查询所有分类
        List<DiyCategory> allCategories = diyCategoryMapper.listAll();
        
        // 构建分类树
        return buildCategoryTree(allCategories);
    }

    /**
     * 构建分类树结构
     * @param categories 所有分类
     * @return 分类树
     */
    private List<CategoryTreeNode> buildCategoryTree(List<DiyCategory> categories) {
        if (CollectionUtils.isEmpty(categories)) {
            return new ArrayList<>();
        }

        // 按keyCode分组，方便查找父子关系
        Map<String, DiyCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(DiyCategory::getKeyCode, c -> c));

        // 存储所有树节点
        Map<String, CategoryTreeNode> nodeMap = new HashMap<>();

        // 创建所有节点
        for (DiyCategory category : categories) {
            CategoryTreeNode node = CategoryTreeNode.builder()
                    .id(category.getId())
                    .keyCode(category.getKeyCode())
                    .name(category.getName())
                    .sort(category.getSort())
                    .status(category.getStatus())
                    .children(new ArrayList<>())
                    .build();
            
            // 计算层级
            String keyCode = category.getKeyCode();
            int level = StringUtils.countOccurrencesOf(keyCode, ".");
            node.setLevel(level);
            
            nodeMap.put(keyCode, node);
        }

        // 构建父子关系
        List<CategoryTreeNode> rootNodes = new ArrayList<>();
        for (Map.Entry<String, CategoryTreeNode> entry : nodeMap.entrySet()) {
            String keyCode = entry.getKey();
            CategoryTreeNode node = entry.getValue();
            
            // 查找父节点
            String parentKeyCode = getParentKeyCode(keyCode);
            if (parentKeyCode != null && nodeMap.containsKey(parentKeyCode)) {
                // 有父节点，添加为子节点
                CategoryTreeNode parentNode = nodeMap.get(parentKeyCode);
                parentNode.getChildren().add(node);
                node.setLeaf(true); // 默认设置为叶子节点，后面会更新
            } else {
                // 没有父节点，作为根节点
                rootNodes.add(node);
                node.setLeaf(false); // 根节点不是叶子节点
            }
        }

        // 更新叶子节点标记
        updateLeafFlag(rootNodes);

        // 按排序字段排序
        sortTreeNodes(rootNodes);

        return rootNodes;
    }

    /**
     * 获取父级keyCode
     * @param keyCode 当前keyCode
     * @return 父级keyCode，如果没有则返回null
     */
    private String getParentKeyCode(String keyCode) {
        int lastDotIndex = keyCode.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return keyCode.substring(0, lastDotIndex);
        }
        return null;
    }

    /**
     * 更新叶子节点标记
     * @param nodes 节点列表
     */
    private void updateLeafFlag(List<CategoryTreeNode> nodes) {
        for (CategoryTreeNode node : nodes) {
            if (CollectionUtils.isEmpty(node.getChildren())) {
                node.setLeaf(true);
            } else {
                node.setLeaf(false);
                updateLeafFlag(node.getChildren());
            }
        }
    }

    /**
     * 对树节点进行排序
     * @param nodes 节点列表
     */
    private void sortTreeNodes(List<CategoryTreeNode> nodes) {
        // 按sort字段排序
        nodes.sort(Comparator.comparing(CategoryTreeNode::getSort));
        
        // 递归排序子节点
        for (CategoryTreeNode node : nodes) {
            if (!CollectionUtils.isEmpty(node.getChildren())) {
                sortTreeNodes(node.getChildren());
            }
        }
    }
}