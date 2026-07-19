package com.diy.service.impl;

import com.diy.context.BaseContext;
import com.diy.dto.DiyOrderCreateDTO;
import com.diy.entity.ColorSeries;
import com.diy.entity.DiyCategory;
import com.diy.entity.DiyMaterial;
import com.diy.entity.OrderItem;
import com.diy.entity.Orders;
import com.diy.exception.OrderBusinessException;
import com.diy.mapper.ColorSeriesMapper;
import com.diy.mapper.DiyCategoryMapper;
import com.diy.mapper.DiyMaterialMapper;
import com.diy.mapper.OrderDetailMapper;
import com.diy.mapper.OrderMapper;
import com.diy.service.DesignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DesignServiceImpl implements DesignService {

    @Autowired
    private DiyCategoryMapper diyCategoryMapper;

    @Autowired
    private ColorSeriesMapper colorSeriesMapper;

    @Autowired
    private DiyMaterialMapper diyMaterialMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    /**
     * 查询所有DIY分类
     * 
     * @return
     */
    @Override
    public List<DiyCategory> getCategoryList() {
        log.info("查询DIY分类列表");
        return diyCategoryMapper.list();
    }

    /**
     * 查询所有色系
     * 
     * @return
     */
    @Override
    public List<ColorSeries> getColorSeriesList() {
        log.info("查询色系列表");
        return colorSeriesMapper.list();
    }

    /**
     * 查询DIY材料列表（支持分类和色系筛选）
     * 
     * @param categories  分类键列表
     * @param colorSeries 色系键列表
     * @return
     */
    @Override
    public List<DiyMaterial> getMaterialList(List<String> categories, List<String> colorSeries) {
        log.info("查询DIY材料列表，分类：{}，色系：{}", categories, colorSeries);
        // 用户端需要同名聚合且按尺寸排序
        return diyMaterialMapper.listForUser(categories, colorSeries);
    }

    /**
     * 创建DIY订单
     * 
     * @param diyOrderCreateDTO 订单数据
     * @return
     */
    @Transactional
    @Override
    public Orders createDiyOrder(DiyOrderCreateDTO diyOrderCreateDTO) {
        // 获取当前用户ID
        Long userId = BaseContext.getCurrentId();
        log.info("创建DIY订单，用户ID: {}, 订单项: {}", userId, diyOrderCreateDTO.getItems());

        // 计算总金额并构建订单项列表
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        if (diyOrderCreateDTO.getItems() != null) {
            for (DiyOrderCreateDTO.DiyOrderItem item : diyOrderCreateDTO.getItems()) {
                Long materialId = item.getProductId(); // productId 实际存储的是 materialId
                Integer quantity = item.getQuantity() != null ? item.getQuantity() : 1;

                // 检查库存并扣减（原子操作）
                log.info("扣减材料库存: materialId={}, quantity={}", materialId, quantity);
                int affectedRows = diyMaterialMapper.deductStock(materialId, quantity);

                if (affectedRows == 0) {
                    // 库存不足，查询材料信息用于错误提示
                    DiyMaterial material = diyMaterialMapper.getById(materialId);
                    String materialName = material != null ? material.getTitle() : "材料ID:" + materialId;
                    int currentStock = material != null && material.getStock() != null ? material.getStock() : 0;
                    throw new OrderBusinessException("材料【" + materialName + "】库存不足，当前库存：" + currentStock);
                }

                log.info("材料库存扣减成功: materialId={}, quantity={}", materialId, quantity);

                BigDecimal itemPrice = BigDecimal.valueOf(item.getPrice() != null ? item.getPrice() : 0);
                BigDecimal itemQuantity = BigDecimal.valueOf(quantity);
                totalAmount = totalAmount.add(itemPrice.multiply(itemQuantity));

                // 根据materialId获取材料图片
                DiyMaterial material = diyMaterialMapper.getById(materialId);
                String materialImage = material != null ? material.getImageUrl() : null;
                
                // 构建订单项（productId 存储 materialId）
                OrderItem orderItem = OrderItem.builder()
                        .productId(materialId) // 复用 productId 字段存储 materialId
                        .title(item.getTitle())
                        .price(itemPrice)
                        .quantity(quantity)
                        .productImage(materialImage) // 设置材料图片
                        .build();
                orderItems.add(orderItem);
            }
        }

        // 生成订单号：时间戳 + 用户ID
        String orderNo = "DIY" + System.currentTimeMillis();

        // 创建订单对象
        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setAmount(totalAmount);
        order.setStatus(Orders.PENDING_PAYMENT); // 0-待支付
        order.setCreateTime(LocalDateTime.now());
        order.setIsOverseas(diyOrderCreateDTO.getIsOverseas() != null ? diyOrderCreateDTO.getIsOverseas() : 0);

        // DIY 专属字段
        order.setDiyName(diyOrderCreateDTO.getDiyName());
        order.setProductImage(diyOrderCreateDTO.getDiyImage());
        order.setDescription(diyOrderCreateDTO.getDescription());

        // 收货人信息
        order.setReceiverName(diyOrderCreateDTO.getReceiverName());
        order.setReceiverPhone(diyOrderCreateDTO.getReceiverPhone());
        order.setReceiverProvince(diyOrderCreateDTO.getReceiverProvince());
        order.setReceiverCity(diyOrderCreateDTO.getReceiverCity());
        order.setReceiverDistrict(diyOrderCreateDTO.getReceiverDistrict());
        order.setReceiverDetail(diyOrderCreateDTO.getReceiverDetail());

        // 备注（可选）
        order.setRemark(diyOrderCreateDTO.getRemark());

        // 插入订单
        orderMapper.insert(order);
        Long orderId = order.getId();
        log.info("DIY订单创建成功，订单ID: {}, 订单号: {}, 金额: {}", orderId, orderNo, totalAmount);

        // 插入订单详情
        if (!orderItems.isEmpty()) {
            log.info("准备插入DIY订单详情，订单项数量: {}", orderItems.size());
            for (OrderItem orderItem : orderItems) {
                orderItem.setOrderId(orderId);
                log.info("设置订单项orderId: {}, materialId: {}, title: {}",
                        orderId, orderItem.getProductId(), orderItem.getTitle());
            }

            try {
                log.info("开始执行 insertBatch，参数数量: {}", orderItems.size());
                orderDetailMapper.insertBatch(orderItems);
                log.info("DIY订单详情插入成功");
            } catch (Exception e) {
                log.error("DIY订单详情插入失败", e);
                throw e;
            }
        }

        return order;
    }
}
