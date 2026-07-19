package com.diy.service;

import com.diy.dto.AiRecommendDTO;
import com.diy.vo.AiRecommendVO;

public interface AiRecommendService {
    /**
     * 根据用户输入（生辰八字/塔罗）获取材料推荐建议（仅限塔罗）
     * @param aiRecommendDTO 用户输入参数
     * @return 推荐结果
     */
    AiRecommendVO getRecommendation(AiRecommendDTO aiRecommendDTO);

    /**
     * 根据用户输入（生辰八字）获取算命搭配推荐（不含塔罗）
     * @param aiRecommendDTO 用户输入参数
     * @return 推荐结果
     */
    AiRecommendVO getFortuneRecommendation(AiRecommendDTO aiRecommendDTO);
}
