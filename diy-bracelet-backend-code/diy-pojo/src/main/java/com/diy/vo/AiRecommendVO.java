package com.diy.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "AI推荐结果")
public class AiRecommendVO implements Serializable {

    @ApiModelProperty("生辰八字信息")
    private String bazi;

    @ApiModelProperty("五行分析结果")
    private Map<String, Integer> wuxing;

    @ApiModelProperty("塔罗牌信息（如果启用）")
    private String tarotInfo;

    @ApiModelProperty("命理/塔罗综合分析建议")
    private String analysis;

    @ApiModelProperty("推荐的材料列表")
    private List<MaterialItem> materials;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialItem implements Serializable {
        @ApiModelProperty("材料ID")
        private Long materialId;

        @ApiModelProperty("名称")
        private String title;

        @ApiModelProperty("图片地址")
        private String imageUrl;

        @ApiModelProperty("分类键")
        private String categoryKey;

        @ApiModelProperty("数量")
        private Integer quantity;

        @ApiModelProperty("位置建议（如：主珠、配珠、绳子）")
        private String position;

        @ApiModelProperty("推荐理由")
        private String reason;
    }
}
