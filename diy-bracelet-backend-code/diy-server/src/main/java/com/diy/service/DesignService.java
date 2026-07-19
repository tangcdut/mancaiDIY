package com.diy.service;

import com.diy.dto.DiyOrderCreateDTO;
import com.diy.entity.ColorSeries;
import com.diy.entity.DiyCategory;
import com.diy.entity.DiyMaterial;
import com.diy.entity.Orders;
import java.util.List;

public interface DesignService {

    /**
     * 查询所有DIY分类
     * @return
     */
    List<DiyCategory> getCategoryList();

    /**
     * 查询所有色系
     * @return
     */
    List<ColorSeries> getColorSeriesList();

    /**
     * 查询DIY材料列表（支持分类和色系筛选）
     * @param categories 分类键列表
     * @param colorSeries 色系键列表
     * @return
     */
    List<DiyMaterial> getMaterialList(List<String> categories, List<String> colorSeries);
    
    /**
     * 创建DIY订单
     * @param diyOrderCreateDTO 订单数据
     * @return
     */
    Orders createDiyOrder(DiyOrderCreateDTO diyOrderCreateDTO);
}
