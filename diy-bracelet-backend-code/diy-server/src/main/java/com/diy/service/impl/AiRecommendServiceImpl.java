package com.diy.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.diy.dto.AiRecommendDTO;
import com.diy.entity.DiyMaterial;
import com.diy.exception.BaseException;
import com.diy.mapper.DiyMaterialMapper;
import com.diy.properties.DeepSeekProperties;
import com.diy.service.AiRecommendService;
import com.diy.utils.HttpClientUtil;
import com.diy.vo.AiRecommendVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiRecommendServiceImpl implements AiRecommendService {

    @Autowired
    private DeepSeekProperties deepSeekProperties;

    @Autowired
    private DiyMaterialMapper diyMaterialMapper;

    @Override
    public AiRecommendVO getRecommendation(AiRecommendDTO aiRecommendDTO) {
        log.info("收到AI塔罗推荐请求: {}", aiRecommendDTO.getPrompt());

        List<DiyMaterial> allMaterials = diyMaterialMapper.listForUser(null, null);
        if (allMaterials == null || allMaterials.isEmpty()) {
            throw new BaseException("当前没有可用的DIY材料");
        }

        String systemPrompt = buildTarotSystemPrompt(allMaterials);
        String userPrompt = aiRecommendDTO.getPrompt();

        String aiResponse = callDeepSeek(systemPrompt, userPrompt);

        return parseAiResponse(aiResponse, allMaterials);
    }

    @Override
    public AiRecommendVO getFortuneRecommendation(AiRecommendDTO aiRecommendDTO) {
        log.info("收到AI算命推荐请求: {}", aiRecommendDTO.getPrompt());

        List<DiyMaterial> allMaterials = diyMaterialMapper.listForUser(null, null);
        if (allMaterials == null || allMaterials.isEmpty()) {
            throw new BaseException("当前没有可用的DIY材料");
        }

        String systemPrompt = buildFortuneSystemPrompt(allMaterials);
        String userPrompt = aiRecommendDTO.getPrompt();

        String aiResponse = callDeepSeek(systemPrompt, userPrompt);

        return parseAiResponse(aiResponse, allMaterials);
    }

    private String buildTarotSystemPrompt(List<DiyMaterial> materials) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一位精通西方塔罗牌占卜的珠宝设计搭配大师。\n\n");

        sb.append("用户会提供他们的当前状态、情感、事业或生活需求等描述信息。");
        sb.append("你需要：\n");
        sb.append("1. 为用户随机抽取一张大阿尔卡那塔罗牌（如「愚人」、「女祭司」、「太阳」等）。\n");
        sb.append("2. 结合该塔罗牌的牌面意象、启示与核心能量，为用户提供手链材料的搭配建议。\n");
        sb.append("3. 从下方的材料库中挑选最合适的主珠、配珠、绳子等，组合成一条能够给予用户指引和能量的手链。\n\n");

        sb.append("搭配原则：\n");
        sb.append("1. 塔罗启示：详细描述抽中的塔罗牌核心寓意及它对用户当前境遇的启示。\n");
        sb.append("2. 意象契合：选用的珠子材质、颜色和能量需契合塔罗牌的牌面象征（例如：「太阳」牌搭配温暖活力色系如黄色主珠；「月亮」牌搭配直觉梦幻色系等）。\n");
        sb.append("3. 结构合理：手链需包含1颗主珠(main_bead)、若干配珠(accessory，建议6~12颗)以及1条绳子(rope)。\n\n");

        sb.append("约束条件：\n");
        sb.append("1. 只能从下方材料库中选择，必须使用正确的 materialId。\n");
        sb.append("2. 必须包含：1颗主珠(main_bead)、若干配珠(accessory)、1条绳子(rope)。\n");
        sb.append("3. 只返回纯 JSON，不要包含 markdown 代码块标记或其他文字。\n\n");

        sb.append("材料库：\n");
        List<Map<String, Object>> simplifiedMaterials = materials.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("name", m.getTitle());
            map.put("category", m.getCategoryKey());
            map.put("colorSeries", m.getColorSeriesKey());
            map.put("size", m.getSize());
            return map;
        }).collect(Collectors.toList());
        sb.append(JSON.toJSONString(simplifiedMaterials));
        sb.append("\n\n");

        sb.append("返回 JSON 结构（严格按此格式）：\n");
        sb.append("{\n");
        sb.append("  \"tarotInfo\": \"抽中「XX」牌，象征...，给您的启示是...\",\n");
        sb.append("  \"analysis\": \"结合塔罗牌的能量与您的需求，色彩搭配设计说明和选择理由...\",\n");
        sb.append("  \"recommendations\": [\n");
        sb.append("    {\"materialId\": 101, \"quantity\": 1, \"position\": \"main_bead\", \"reason\": \"选择理由\"},\n");
        sb.append("    {\"materialId\": 202, \"quantity\": 8, \"position\": \"accessory\", \"reason\": \"选择理由\"},\n");
        sb.append("    {\"materialId\": 303, \"quantity\": 1, \"position\": \"rope\", \"reason\": \"选择理由\"}\n");
        sb.append("  ]\n");
        sb.append("}");

        return sb.toString();
    }

    private String buildFortuneSystemPrompt(List<DiyMaterial> materials) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一位精通中国传统命理学（生辰八字、五行、风水）的珠宝搭配大师。\n\n");

        sb.append("用户会用自然语言描述自己的情况，可能包含：出生日期（阳历或阴历）、性别、性格特点、生活现状或想要的愿望寓意（招财、平安、姻缘、事业等）。\n");
        sb.append("你需要：\n");
        sb.append("1. 分析用户的八字五行情况。如果用户提供了出生时间，请推算其八字，并计算五行（金、木、水、火、土）分布。如果用户未提供出生日期，则根据其诉求或描述进行五行能量匹配分析。\n");
        sb.append("2. 遵循「损有余而补不足」或「生助喜用神」的原则，挑选最能平衡其五行能量、达成其愿望的珠子和配饰。\n");
        sb.append("3. 从下方的材料库中挑选最合适的主珠、配珠和绳子等，组合成一条助运避邪的手链。\n\n");

        sb.append("搭配原则：\n");
        sb.append("1. 五行平衡：八字五行中缺什么补什么，或者根据喜用神选定相应五行属性材质和颜色的材料。\n");
        sb.append("2. 寓意匹配：根据用户诉求（如招财、事业、姻缘等），选用有相应寓意的天然石/水晶/珠子。\n");
        sb.append("3. 结构合理：手链需包含1颗主珠(main_bead)、若干配珠(accessory，建议6~12颗)以及1条绳子(rope)。\n\n");

        sb.append("约束条件：\n");
        sb.append("1. 只能从下方材料库中选择，必须使用正确的 materialId。\n");
        sb.append("2. 必须包含：1颗主珠(main_bead)、若干配珠(accessory)、1条绳子(rope)。\n");
        sb.append("3. 只返回纯 JSON，不要包含 markdown 代码块标记或其他文字。\n\n");

        sb.append("材料库：\n");
        List<Map<String, Object>> simplifiedMaterials = materials.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("name", m.getTitle());
            map.put("category", m.getCategoryKey());
            map.put("colorSeries", m.getColorSeriesKey());
            map.put("size", m.getSize());
            return map;
        }).collect(Collectors.toList());
        sb.append(JSON.toJSONString(simplifiedMaterials));
        sb.append("\n\n");

        sb.append("返回 JSON 结构（严格按此格式）：\n");
        sb.append("{\n");
        sb.append("  \"bazi\": \"壬寅年 壬子月 丙午日（如果能推算八字）\",\n");
        sb.append("  \"wuxing\": {\"金\": 1, \"木\": 2, \"水\": 0, \"火\": 3, \"土\": 2},\n");
        sb.append("  \"analysis\": \"详细的八字五行命理分析、搭配的五行依据和选择理由...\",\n");
        sb.append("  \"recommendations\": [\n");
        sb.append("    {\"materialId\": 101, \"quantity\": 1, \"position\": \"main_bead\", \"reason\": \"选择理由\"},\n");
        sb.append("    {\"materialId\": 202, \"quantity\": 8, \"position\": \"accessory\", \"reason\": \"选择理由\"},\n");
        sb.append("    {\"materialId\": 303, \"quantity\": 1, \"position\": \"rope\", \"reason\": \"选择理由\"}\n");
        sb.append("  ]\n");
        sb.append("}");

        return sb.toString();
    }

    private String callDeepSeek(String systemPrompt, String userPrompt) {
        try {
            String url = deepSeekProperties.getApiUrl() + "/chat/completions";
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + deepSeekProperties.getApiKey());
            headers.put("Content-Type", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("model", deepSeekProperties.getModel());

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(createMessage("system", systemPrompt));
            messages.add(createMessage("user", userPrompt));
            body.put("messages", messages);

            Map<String, String> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            body.put("response_format", responseFormat);

            log.info("调用DeepSeek API, model: {}", deepSeekProperties.getModel());
            String response = HttpClientUtil.doPost4Json(url, body, headers,
                    deepSeekProperties.getConnectTimeout(), deepSeekProperties.getReadTimeout());

            log.debug("DeepSeek API 原始响应: {}", response);

            JSONObject jsonResponse = JSON.parseObject(response);
            if (jsonResponse.containsKey("error")) {
                throw new BaseException("AI服务调用失败: " + jsonResponse.getJSONObject("error").getString("message"));
            }

            return jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        } catch (Exception e) {
            log.error("调用DeepSeek失败", e);
            throw new BaseException("AI推荐服务暂时不可用，请稍后再试");
        }
    }

    private Map<String, String> createMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private AiRecommendVO parseAiResponse(String content, List<DiyMaterial> allMaterials) {
        try {
            JSONObject json = JSON.parseObject(content);

            AiRecommendVO vo = AiRecommendVO.builder()
                    .bazi(json.getString("bazi"))
                    .analysis(json.getString("analysis"))
                    .tarotInfo(json.getString("tarotInfo"))
                    .wuxing(new HashMap<>())
                    .materials(new ArrayList<>())
                    .build();

            JSONObject wuxingJson = json.getJSONObject("wuxing");
            if (wuxingJson != null) {
                for (String key : wuxingJson.keySet()) {
                    vo.getWuxing().put(key, wuxingJson.getInteger(key));
                }
            }

            JSONArray recs = json.getJSONArray("recommendations");
            Map<Long, DiyMaterial> materialMap = allMaterials.stream()
                    .collect(Collectors.toMap(DiyMaterial::getId, m -> m));

            if (recs != null) {
                for (int i = 0; i < recs.size(); i++) {
                    JSONObject rec = recs.getJSONObject(i);
                    Long mid = rec.getLong("materialId");
                    DiyMaterial m = materialMap.get(mid);

                    if (m != null) {
                        vo.getMaterials().add(AiRecommendVO.MaterialItem.builder()
                                .materialId(mid)
                                .title(m.getTitle())
                                .imageUrl(m.getImageUrl())
                                .categoryKey(m.getCategoryKey())
                                .quantity(rec.getInteger("quantity"))
                                .position(rec.getString("position"))
                                .reason(rec.getString("reason"))
                                .build());
                    }
                }
            }

            return vo;
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", content, e);
            throw new BaseException("AI返回数据解析失败");
        }
    }
}
