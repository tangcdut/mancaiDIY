package com.diy.task;

import com.diy.entity.Orders;
import com.diy.mapper.OrderMapper;
import com.diy.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private OrderService orderService;
    /**
     * 处理支付超时的订单
     */
    @Scheduled(cron = "0 * * * * ? ")//每分钟触发一次
    public void processTimeoutOrder(){
        LocalDateTime time = LocalDateTime.now().minusMinutes(30);
        List<Orders> ordersList = orderMapper.getByStatusAndOrdertimeLT(Orders.PENDING_PAYMENT, time);
        if (ordersList == null || ordersList.isEmpty()) {
            return;
        }
        log.info("定时处理超时未支付订单，超时时间点: {}，待处理数量: {}", time, ordersList.size());
        for (Orders order : ordersList) {
            try {
                // 复用用户端的取消逻辑：会校验状态并回滚库存、清空 prepayId
                orderService.update(order.getId());
            } catch (Exception e) {
                log.error("自动取消超时订单失败，订单ID: {}", order.getId(), e);
            }
        }
    }
    @Scheduled(cron = "0 0 1 * * ?")//每天凌晨一点处理前一天处于派送中的订单
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单:{}",LocalDateTime.now());
        LocalDateTime time=LocalDateTime.now().plusMinutes(-60);

    }
}
