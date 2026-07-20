package com.diy.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * DIY 设计图后端渲染请求。
 * 前端已完成全部几何计算(缩放/居中)，这里的坐标均为“最终成图像素坐标”，后端只负责按坐标合成。
 */
@Data
public class DesignRenderDTO implements Serializable {

    /** 成图边长(正方形)，如 1000 */
    private Integer size;

    private Rope rope;
    private Logo logo;
    private List<Bead> beads;

    /** 绳子(圆环) */
    @Data
    public static class Rope implements Serializable {
        private Double cx;
        private Double cy;
        private Double r;
        private String color;
        private Double width;
    }

    /** 中心 Logo */
    @Data
    public static class Logo implements Serializable {
        private Double cx;
        private Double cy;
        private Double size;
        private String url;
    }

    /** 单颗珠子 */
    @Data
    public static class Bead implements Serializable {
        /** 图片地址(MinIO 路径 /admin/common/image/... 或完整 URL) */
        private String url;
        /** 中心坐标(成图像素) */
        private Double cx;
        private Double cy;
        /** 方形边长(图片按 aspectFit 装入其中) */
        private Double size;
        /** 旋转弧度 */
        private Double rot;
        /** 是否水平镜像 */
        private Boolean mirror;
        /** 图片缺失时的兜底填充色 */
        private String color;
        /** 是否画在绳子下层(大珠子) */
        private Boolean below;
    }
}
