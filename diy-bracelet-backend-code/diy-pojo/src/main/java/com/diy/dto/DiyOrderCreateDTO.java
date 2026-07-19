package com.diy.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class DiyOrderCreateDTO implements Serializable {
    
    /**
     * DIY作品名称
     */
    private String diyName;
    
    /**
     * DIY设计图片URL
     */
    private String diyImage;
    
    /**
     * 手链组成描述（如材料顺序等）
     */
    private String description;
    
    /**
     * 收货人姓名
     */
    private String receiverName;
    
    /**
     * 收货人电话
     */
    private String receiverPhone;
    
    /**
     * 收货省份
     */
    private String receiverProvince;
    
    /**
     * 收货城市
     */
    private String receiverCity;
    
    /**
     * 收货区县
     */
    private String receiverDistrict;
    
    /**
     * 详细地址
     */
    private String receiverDetail;
    
    /**
     * 设计ID（可选，如果用户保存了设计）
     */
    private Long designId;

    /**
     * 订单备注（可选）
     */
    private String remark;
    
    /**
     * 订单项列表
     */
    private List<DiyOrderItem> items;

    /**
     * 是否海外订单：0 否，1 是
     */
    private Integer isOverseas;
    
    @Data
    public static class DiyOrderItem implements Serializable {
        /**
         * 商品ID/材料ID
         */
        private Long productId;
        
        /**
         * 材料名称
         */
        private String title;
        
        /**
         * 单价
         */
        private Double price;
        
        /**
         * 数量
         */
        private Integer quantity;
    }
}
