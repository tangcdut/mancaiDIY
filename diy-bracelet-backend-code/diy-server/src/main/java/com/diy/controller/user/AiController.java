package com.diy.controller.user;

import com.diy.dto.AiRecommendDTO;
import com.diy.result.Result;
import com.diy.service.AiRecommendService;
import com.diy.vo.AiRecommendVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/ai")
@Api(tags = "用户端AI推荐接口")
@Slf4j
public class AiController {

    @Autowired
    private AiRecommendService aiRecommendService;

    @PostMapping("/recommend")
    @ApiOperation("获取AI搭配推荐（仅塔罗）")
    public Result<AiRecommendVO> getRecommendation(@RequestBody AiRecommendDTO aiRecommendDTO) {
        log.info("收到AI塔罗推荐请求: prompt={}", aiRecommendDTO.getPrompt());
        
        AiRecommendVO vo = aiRecommendService.getRecommendation(aiRecommendDTO);
        
        log.info("AI塔罗推荐完成，返回材料数: {}", vo.getMaterials().size());
        return Result.success(vo);
    }

    @PostMapping("/fortune-recommend")
    @ApiOperation("获取AI算命搭配推荐（仅算命八字五行）")
    public Result<AiRecommendVO> getFortuneRecommendation(@RequestBody AiRecommendDTO aiRecommendDTO) {
        log.info("收到AI算命推荐请求: prompt={}", aiRecommendDTO.getPrompt());
        
        AiRecommendVO vo = aiRecommendService.getFortuneRecommendation(aiRecommendDTO);
        
        log.info("AI算命推荐完成，返回材料数: {}", vo.getMaterials().size());
        return Result.success(vo);
    }
}
