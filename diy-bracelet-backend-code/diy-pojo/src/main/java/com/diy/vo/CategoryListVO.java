package com.diy.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(description = "分类列表返回数据")
public class CategoryListVO implements Serializable {

    @ApiModelProperty("分类列表")
    private List<CategoryItem> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel(description = "分类项")
    public static class CategoryItem implements Serializable {
        
        @ApiModelProperty("分类ID")
        private Long id;
        
        @ApiModelProperty("分类名称")
        private String name;
        
        @ApiModelProperty("排序")
        private Integer sort;
        
        @ApiModelProperty("状态：1启用 0禁用")
        private Integer status;
    }
}
