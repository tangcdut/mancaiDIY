package com.diy.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "AI推荐请求参数")
public class AiRecommendDTO implements Serializable {

    @ApiModelProperty(value = "用户自由输入的内容，可包含出生日期、喜好、需求等任何信息", required = true, example = "我1990年5月15日出生，男生，喜欢蓝色系，想要一条招财的手链")
    private String prompt;

    @ApiModelProperty("是否启用塔罗牌分析（默认 false）")
    private Boolean useTarot = false;
}
