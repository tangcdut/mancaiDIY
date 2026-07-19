package com.diy.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.diy.constant.MessageConstant;
import com.diy.context.BaseContext;
import com.diy.dto.*;
import com.diy.entity.*;
import com.diy.exception.OrderBusinessException;
import com.diy.exception.ShoppingCartBusinessException;
import com.diy.mapper.*;
import com.diy.result.PageResult;
import com.diy.service.CartItemService;
import com.diy.service.OrderService;
import com.diy.utils.WeChatPayUtil;
import com.diy.utils.MinioUtil;
import com.diy.utils.PayPalPayUtil;
import com.diy.properties.PayPalProperties;
import com.diy.vo.OrderCreateVO;
import com.diy.vo.OrderPaymentStatusVO;
import com.diy.vo.OrderPaymentVO;
import com.diy.vo.OrderSubmitVO;
import com.diy.vo.OrderVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
//todo 下单没有扣库存
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CartItemService cartItemService;
    @Autowired
    private DiyMaterialMapper diyMaterialMapper; // 新增：用于材料库存操作
    @Autowired
    private MinioUtil minioUtil;
    @Autowired
    private PayPalProperties payPalProperties;
    @Autowired
    private OverseasOrderMapper overseasOrderMapper;
    @Autowired
    private OverseasOrderItemMapper overseasOrderItemMapper;
    @Autowired
    private PayPalWebhookEventMapper paypalWebhookEventMapper;

    @Value("${dev.mock-payment:false}")
    private Boolean mockPayment;

    @Value("${diy.order.allow-completed-refund:true}")
    private boolean allowCompletedRefund;

    @Transactional
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        CartItem cartItemQuery = CartItem.builder()
                .userId(userId)
                .build();
        List<CartItem> cartItems = shoppingCartMapper.list(cartItemQuery);

        if (cartItems == null || cartItems.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Long productId = cartItem.getProductId();
            Integer quantity = cartItem.getQuantity();

            Product product = productMapper.getById(productId);
            if (product == null) {
                throw new OrderBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
            }

            // 检查库存是否充足
            if (product.getStock() == null || product.getStock() < quantity) {
                throw new OrderBusinessException("商品【" + product.getTitle() + "】库存不足，当前库存：" + 
                    (product.getStock() == null ? 0 : product.getStock()));
            }

            // 扣减库存
            product.setStock(product.getStock() - quantity);
            productMapper.update(product);

            BigDecimal itemAmount = product.getPrice().multiply(new BigDecimal(quantity));
            totalAmount = totalAmount.add(itemAmount);

            OrderItem orderItem = OrderItem.builder()
                    .productId(productId)
                    .title(product.getTitle())
                    .price(product.getPrice())
                    .quantity(quantity)
                    .productImage(product.getCoverImage())
                    .build();
            orderItems.add(orderItem);
        }

        String orderNo = "ORD" + System.currentTimeMillis();

        // 订单封面图：取第一个商品的封面图（用于订单列表展示）
        String orderCoverImage = null;
        if (orderItems != null && !orderItems.isEmpty()) {
            orderCoverImage = orderItems.get(0).getProductImage();
        }

        Orders order = Orders.builder()
                .userId(userId)
                .orderNo(orderNo)
                .amount(totalAmount)
                .status(Orders.PENDING_PAYMENT)
                .createTime(LocalDateTime.now())
                .productImage(orderCoverImage)
                .remark(ordersSubmitDTO != null ? ordersSubmitDTO.getRemark() : null)
                .isOverseas(ordersSubmitDTO != null && ordersSubmitDTO.getIsOverseas() != null ? ordersSubmitDTO.getIsOverseas() : 0)
                .build();

        orderMapper.insert(order);
        Long orderId = order.getId();

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(orderId);
        }
        orderDetailMapper.insertBatch(orderItems);

        shoppingCartMapper.deleteByUserId(userId);

        return OrderSubmitVO.builder()
                .id(orderId)
                .orderNumber(orderNo)
                .orderAmount(totalAmount)
                .orderTime(order.getCreateTime())
                .build();
    }

    @Transactional
    @Override
    public OrderCreateVO createOrderFromCart(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();
        log.info("开始从购物车创建订单，用户ID: {}", userId);

        CartItem cartItemQuery = CartItem.builder()
                .userId(userId)
                .build();
        List<CartItem> cartItems = shoppingCartMapper.list(cartItemQuery);
        log.info("购物车商品数量: {}", cartItems != null ? cartItems.size() : 0);

        if (cartItems == null || cartItems.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Long productId = cartItem.getProductId();
            Integer quantity = cartItem.getQuantity();
            log.info("处理购物车商品: productId={}, quantity={}", productId, quantity);

            Product product = productMapper.getById(productId);
            if (product == null) {
                throw new OrderBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
            }

            // 检查库存是否充足
            if (product.getStock() == null || product.getStock() < quantity) {
                throw new OrderBusinessException("商品【" + product.getTitle() + "】库存不足，当前库存：" + 
                    (product.getStock() == null ? 0 : product.getStock()));
            }

            // 扣减库存
            product.setStock(product.getStock() - quantity);
            productMapper.update(product);

            BigDecimal itemAmount = product.getPrice().multiply(new BigDecimal(quantity));
            totalAmount = totalAmount.add(itemAmount);

            OrderItem orderItem = OrderItem.builder()
                    .productId(productId)
                    .title(product.getTitle())
                    .price(product.getPrice())
                    .quantity(quantity)
                    .productImage(product.getCoverImage())
                    .build();
            orderItems.add(orderItem);
            log.info("添加订单项: title={}, price={}, quantity={}", product.getTitle(), product.getPrice(), quantity);
        }

        String orderNo = "ORD" + System.currentTimeMillis();
        log.info("生成订单号: {}, 总金额: {}", orderNo, totalAmount);

        // 订单封面图：取第一个商品的封面图（用于订单列表展示）
        String orderCoverImage = null;
        if (orderItems != null && !orderItems.isEmpty()) {
            orderCoverImage = orderItems.get(0).getProductImage();
        }

        Orders order = Orders.builder()
                .userId(userId)
                .orderNo(orderNo)
                .amount(totalAmount)
                .status(Orders.PENDING_PAYMENT)
                .createTime(LocalDateTime.now())
                .productImage(orderCoverImage)
                .remark(ordersSubmitDTO != null ? ordersSubmitDTO.getRemark() : null)
                // 添加收件人信息（如果直接传递）
                .receiverName(ordersSubmitDTO != null ? ordersSubmitDTO.getReceiverName() : null)
                .receiverPhone(ordersSubmitDTO != null ? ordersSubmitDTO.getReceiverPhone() : null)
                .receiverProvince(ordersSubmitDTO != null ? ordersSubmitDTO.getReceiverProvince() : null)
                .receiverCity(ordersSubmitDTO != null ? ordersSubmitDTO.getReceiverCity() : null)
                .receiverDistrict(ordersSubmitDTO != null ? ordersSubmitDTO.getReceiverDistrict() : null)
                .receiverDetail(ordersSubmitDTO != null ? ordersSubmitDTO.getReceiverDetail() : null)
                .isOverseas(ordersSubmitDTO != null && ordersSubmitDTO.getIsOverseas() != null ? ordersSubmitDTO.getIsOverseas() : 0)
                .build();

        orderMapper.insert(order);
        Long orderId = order.getId();
        log.info("订单插入成功，orderId: {}", orderId);

        // 设置订单ID
        log.info("准备插入订单详情，订单项数量: {}", orderItems.size());
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(orderId);
            log.info("设置订单项orderId: {}, productId: {}, title: {}", 
                orderId, orderItem.getProductId(), orderItem.getTitle());
        }
        
        // 批量插入订单详情
        try {
            log.info("开始执行 insertBatch，参数数量: {}", orderItems.size());
            orderDetailMapper.insertBatch(orderItems);
            log.info("订单详情插入成功");
        } catch (Exception e) {
            log.error("订单详情插入失败", e);
            throw e;
        }

        shoppingCartMapper.deleteByUserId(userId);
        log.info("清空购物车成功");

        OrderCreateVO.Order orderVO = OrderCreateVO.Order.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .amount(totalAmount)
                .status(Orders.PENDING_PAYMENT)
                .build();

        return OrderCreateVO.builder()
                .order(orderVO)
                .build();
    }

    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        Long userId = BaseContext.getCurrentId();
        log.info("开始处理支付，用户ID: {}, 订单号: {}", userId, ordersPaymentDTO.getOrderNumber());

        User user = userMapper.getById(userId);
        if (user == null) {
            log.error("用户不存在，用户ID: {}", userId);
            throw new RuntimeException("用户不存在");
        }

        log.info("用户信息: id={}, nickname={}, openid={}", user.getId(), user.getNickname(),
                user.getOpenid() != null ? user.getOpenid() : "null");

        Orders ordersDB = orderMapper.getByNumberAndUserId(ordersPaymentDTO.getOrderNumber(), userId);
        if (ordersDB == null) {
            log.error("订单不存在，订单号: {}, 用户ID: {}", ordersPaymentDTO.getOrderNumber(), userId);
            throw new RuntimeException("订单不存在");
        }

        log.info("订单信息: id={}, orderNo={}, amount={}, status={}", ordersDB.getId(), ordersDB.getOrderNo(),
                ordersDB.getAmount(), ordersDB.getStatus());
        
        // 检查订单状态，只有待支付的订单才能支付
        if (!Orders.PENDING_PAYMENT.equals(ordersDB.getStatus())) {
            String statusText = getStatusText(ordersDB.getStatus());
            log.error("订单状态不允许支付，订单号: {}, 当前状态: {} ({})", 
                ordersPaymentDTO.getOrderNumber(), ordersDB.getStatus(), statusText);
            throw new OrderBusinessException("订单状态为【" + statusText + "】，无法支付");
        }
        
        // 若已存在 prepayId：允许复用，并重新生成小程序调起支付参数（用户在微信支付页取消后再次支付的场景）
        if (ordersDB.getPrepayId() != null && !ordersDB.getPrepayId().isEmpty()) {
            log.info("订单已存在prepayId，复用并重新签名，订单号: {}, prepayId: {}",
                    ordersDB.getOrderNo(), ordersDB.getPrepayId());

            JSONObject payParams = weChatPayUtil.buildJsapiPayParamsFromPrepayId(ordersDB.getPrepayId());
            String packageStr = payParams.getString("package");

            return OrderPaymentVO.builder()
                    .nonceStr(payParams.getString("nonceStr"))
                    .paySign(payParams.getString("paySign"))
                    .timeStamp(payParams.getString("timeStamp"))
                    .signType(payParams.getString("signType"))
                    .packageStr(packageStr)
                    .build();
        }

        // 模拟支付模式（开发测试用）
        if (Boolean.TRUE.equals(mockPayment)) {
            log.info("【开发模式】使用模拟支付");

            OrderPaymentVO vo = OrderPaymentVO.builder()
                    .nonceStr("test_nonce_str")
                    .paySign("test_pay_sign")
                    .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                    .signType("RSA")
                    .packageStr("prepay_id=test_prepay_id")
                    .build();

            // 直接更新订单状态为已支付
            LocalDateTime now = LocalDateTime.now();
            Orders orders = Orders.builder()
                    .id(ordersDB.getId())
                    .status(Orders.PAID)
                    .payTime(now)
                    .updateTime(now)
                    .transactionId("MOCK-" + ordersDB.getOrderNo())
                    .build();
            orderMapper.update(orders);

            log.info("模拟支付完成");
            return vo;
        }

        // 真实微信支付模式
        if (user.getOpenid() == null || user.getOpenid().isEmpty()) {
            log.error("用户openid为空，无法发起微信支付");
            throw new RuntimeException("用户未绑定微信，请重新登录");
        }

        // 构建商品描述信息（符合微信要求）
        List<OrderItem> orderItems = orderDetailMapper.getByOrderId(ordersDB.getId());
        String description = "商品购买";
        if (orderItems != null && !orderItems.isEmpty()) {
            // 取第一个商品作为主要描述
            String firstItemTitle = orderItems.get(0).getTitle();
            if (orderItems.size() > 1) {
                description = firstItemTitle + "等" + orderItems.size() + "件商品";
            } else {
                description = firstItemTitle;
            }
            // 限制长度（微信要求description不超过127字符）
            if (description.length() > 127) {
                description = description.substring(0, 124) + "...";
            }
        }
        log.info("订单商品描述: {}", description);

        try {
            log.info("开始调用微信支付接口...");
            JSONObject jsonObject = weChatPayUtil.pay(
                    ordersPaymentDTO.getOrderNumber(),
                    ordersDB.getAmount(),
                    description,
                    user.getOpenid()
            );

            log.info("微信支付接口调用成功，返回数据: {}", jsonObject);
            
            // 提取 prepay_id 并保存到数据库（防止用户取消后重复调用）
            String packageStr = jsonObject.getString("package");
            if (packageStr != null && packageStr.startsWith("prepay_id=")) {
                String prepayId = packageStr.substring(10); // 提取 "prepay_id=" 后面的内容
                log.info("提取到 prepay_id: {}", prepayId);
                
                // 立即保存 prepay_id 到数据库
                Orders updateOrder = Orders.builder()
                        .id(ordersDB.getId())
                        .prepayId(prepayId)
                        .updateTime(LocalDateTime.now())
                        .build();
                orderMapper.update(updateOrder);
                log.info("已保存 prepay_id 到数据库，订单ID: {}", ordersDB.getId());
            }

            return OrderPaymentVO.builder()
                    .nonceStr(jsonObject.getString("nonceStr"))
                    .paySign(jsonObject.getString("paySign"))
                    .timeStamp(jsonObject.getString("timeStamp"))
                    .signType(jsonObject.getString("signType"))
                    .packageStr(packageStr)
                    .build();
        } catch (Exception e) {
            log.error("微信支付调用失败", e);
            throw new RuntimeException("微信支付调用失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public void paySuccess(String outTradeNo, String transactionId) {
        log.info("处理支付成功回调，订单号: {}", outTradeNo);

        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null) {
            log.error("订单不存在，订单号: {}", outTradeNo);
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (Orders.PAID.equals(ordersDB.getStatus()) ||
                Orders.SHIPPED.equals(ordersDB.getStatus()) ||
                Orders.COMPLETED.equals(ordersDB.getStatus())) {
            log.warn("订单已处于支付后状态，忽略本次回调，订单号: {}, 当前状态: {}", outTradeNo, ordersDB.getStatus());
            return;
        }

        if (!Orders.PENDING_PAYMENT.equals(ordersDB.getStatus())) {
            log.error("订单状态不正确，无法支付，订单号: {}, 当前状态: {}", outTradeNo, ordersDB.getStatus());
            throw new OrderBusinessException("订单状态异常，无法完成支付");
        }

        LocalDateTime now = LocalDateTime.now();
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.PAID)
                .payTime(now)
                .updateTime(now)
                .transactionId(transactionId)
                .prepayId(null) // 清空 prepayId
                .build();
        orderMapper.update(orders);

        log.info("支付成功回调处理完成，订单号: {}, 微信交易号: {}", outTradeNo, transactionId);
    }

    @Override
    public PageResult pageQuery4User(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();
                List<OrderItem> orderItems = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderItems(orderItems);
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    @Override
    public PageResult listOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = page.getResult();
        
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                
                List<OrderItem> orderItems = orderDetailMapper.getByOrderId(orders.getId());
                orderVO.setOrderItems(orderItems);
                orderVOList.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), orderVOList);
    }

    @Override
    public void delivery(Long id) {
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(Orders.SHIPPED);
        order.setDeliveryTime(LocalDateTime.now()); // 设置发货时间
        orderMapper.update(order);
    }

    @Override
    public void complete(Long id) {
        Orders order = new Orders();
        order.setStatus(Orders.COMPLETED);
        order.setId(id);
        orderMapper.update(order);
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Long id = ordersCancelDTO.getId();
        log.info("管理端取消订单，订单ID: {}", id);
        
        // 查询订单状态
        Orders order = orderMapper.getDetailsById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        
        // 只有待支付的订单才能取消
        if (!Orders.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new OrderBusinessException("订单状态不允许取消");
        }
        
        // 回滚库存
        rollbackOrderStock(id);
        
        // 更新订单状态为已取消
        Orders updateOrder = new Orders();
        updateOrder.setId(id);
        updateOrder.setStatus(Orders.CANCELLED); // 设置为已取消状态
        updateOrder.setPrepayId(null); // 清空 prepayId
        updateOrder.setUpdateTime(LocalDateTime.now());
        orderMapper.update(updateOrder);
        
        log.info("管理端取消订单成功，库存已回滚，状态已更新为已取消，订单ID: {}", id);
    }

    @Override
    public void repetition(Long id) {
        // 再来一单：将原订单的商品重新加入当前用户的购物车
        Long userId = BaseContext.getCurrentId();
        Orders order = orderMapper.getDetailsById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!userId.equals(order.getUserId())) {
            throw new OrderBusinessException("无权操作该订单");
        }
        List<OrderItem> orderItems = orderDetailMapper.getByOrderId(id);
        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }
        for (OrderItem orderItem : orderItems) {
            com.diy.dto.AddToCartDTO dto = new com.diy.dto.AddToCartDTO();
            dto.setProductId(orderItem.getProductId());
            dto.setQuantity(orderItem.getQuantity());
            cartItemService.addToCart(dto);
        }
    }

    @Override
    public void reminder(Long id) {
        Orders order = orderMapper.getDetailsById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 暂不做实际推送，仅记录日志，预留扩展点
        log.info("用户催单，订单ID: {}，订单号: {}", order.getId(), order.getOrderNo());
    }
    
    @Override
    public void updateStatus(OrderStatusUpdateDTO orderStatusUpdateDTO) {
        Long orderId = orderStatusUpdateDTO.getOrderId();
        Integer status = orderStatusUpdateDTO.getStatus();
        
        Orders order = orderMapper.getDetailsById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        
        Orders updateOrder = Orders.builder()
                .id(orderId)
                .status(status)
                .build();
                
        // 如果是发货状态，设置发货时间和运单号
        if (Orders.SHIPPED.equals(status)) {
            updateOrder.setDeliveryTime(LocalDateTime.now());
            // 如果提供了运单号，则更新运单号
            if (orderStatusUpdateDTO.getTrackingNumber() != null && 
                !orderStatusUpdateDTO.getTrackingNumber().isEmpty()) {
                updateOrder.setTrackingNumber(orderStatusUpdateDTO.getTrackingNumber());
            }
        }
        
        orderMapper.update(updateOrder);
    }

    @Override
    public OrderVO getDetails(Long id) {
        Orders orders = orderMapper.getDetailsById(id);
        List<OrderItem> orderItems = orderDetailMapper.getByOrderId(id);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderItems(orderItems);

        // 兼容历史数据：如果订单主图为空，则使用订单项中的第一张商品图作为主图
        if ((orderVO.getProductImage() == null || orderVO.getProductImage().isEmpty())
                && orderItems != null && !orderItems.isEmpty()) {
            for (OrderItem item : orderItems) {
                if (item != null && item.getProductImage() != null && !item.getProductImage().isEmpty()) {
                    orderVO.setProductImage(item.getProductImage());
                    break;
                }
            }
        }
        return orderVO;
    }

    @Transactional
    @Override
    public void update(Long id) {
        log.info("取消订单，订单ID: {}", id);
        
        // 查询订单状态
        Orders order = orderMapper.getDetailsById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        
        // 只有待支付的订单才能取消
        if (!Orders.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new OrderBusinessException("订单状态不允许取消");
        }
        
        // 回滚库存
        rollbackOrderStock(id);
        
        // 如果是DIY订单且有设计图片，则删除设计图片
        if (order.getDiyName() != null && !order.getDiyName().isEmpty() && 
            order.getProductImage() != null && !order.getProductImage().isEmpty()) {
            try {
                minioUtil.delete(order.getProductImage());
                log.info("DIY设计图片删除成功: {}", order.getProductImage());
            } catch (Exception e) {
                log.warn("DIY设计图片删除失败: {}", order.getProductImage(), e);
            }
        }
        
        // 更新订单状态为已取消
        Orders updateOrder = new Orders();
        updateOrder.setId(id);
        updateOrder.setStatus(Orders.CANCELLED); // 设置为已取消状态
        updateOrder.setPrepayId(null); // 清空 prepayId
        updateOrder.setUpdateTime(LocalDateTime.now());
        orderMapper.update(updateOrder);
        
        log.info("订单取消成功，库存已回滚，状态已更新为已取消，订单ID: {}", id);
    }
    
    /**
     * 获取订单状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        switch (status) {
            case 0: // PENDING_PAYMENT
                return "待支付";
            case 1: // PAID
                return "已支付";
            case 2: // SHIPPED
                return "已发货";
            case 3: // COMPLETED
                return "已完成";
            case 4: // REFUNDING
                return "退款中";
            case 5: // REFUNDED
                return "已退款";
            case 6: // CANCELLED
                return "已取消";
            default:
                return "未知状态";
        }
    }
    
    @Transactional
    @Override
    public void refund(Long orderId) throws Exception {
        log.info("用户申请退款，订单ID: {}", orderId);
        
        // 获取当前用户ID
        Long userId = BaseContext.getCurrentId();
        log.info("用户端申请退款，用户ID: {}", userId);
        
        // 查询订单信息
        Orders order = orderMapper.getDetailsById(orderId);
        if (order == null) {
            log.error("订单不存在，订单ID: {}", orderId);
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        
        // 验证订单归属
        if (!userId.equals(order.getUserId())) {
            log.error("订单不属于当前用户，订单ID: {}, 用户ID: {}, 订单所属用户: {}", 
                orderId, userId, order.getUserId());
            throw new OrderBusinessException("无权操作该订单");
        }
        
        // 限制只有已支付未发货的订单才能申请退款
        if (!Orders.PAID.equals(order.getStatus())) {
            log.error("订单状态不允许退款，订单ID: {}, 当前状态: {}", orderId, order.getStatus());
            throw new OrderBusinessException("只有已支付未发货的订单才能申请退款");
        }
        
        // 检查是否已经退款
        if (Orders.REFUNDING.equals(order.getStatus()) || Orders.REFUNDED.equals(order.getStatus())) {
            log.warn("订单已退款或退款中，订单ID: {}, 状态: {}", orderId, order.getStatus());
            throw new OrderBusinessException("订单已退款或退款中");
        }
        
        // 更新订单状态为退款中，等待管理员审核
        LocalDateTime now = LocalDateTime.now();
        Orders updateOrder = Orders.builder()
            .id(orderId)
            .status(Orders.REFUNDING)
            .refundAmount(order.getAmount())
            .refundTime(now)
            .updateTime(now)
            .build();
        orderMapper.update(updateOrder);
        
        log.info("用户退款申请成功，订单ID: {}，状态已更新为退款中，等待管理员审核", orderId);
    }
    
    @Transactional
    @Override
    public void refundSuccess(String outTradeNo) {
        log.info("处理退款成功回调，订单号: {}", outTradeNo);
        
        Orders order = orderMapper.getByNumber(outTradeNo);
        if (order == null) {
            log.error("订单不存在，订单号: {}", outTradeNo);
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        
        // 检查订单状态，避重复回调
        if (Orders.REFUNDED.equals(order.getStatus())) {
            log.warn("订单已退款，忽略本次回调，订单号: {}", outTradeNo);
            return;
        }
        
        // 回滚库存（事务中执行）
        rollbackOrderStock(order.getId());
        
        // 更新订单状态为已退款
        LocalDateTime now = LocalDateTime.now();
        Orders updateOrder = Orders.builder()
                .id(order.getId())
                .status(Orders.REFUNDED)
                .updateTime(now)
                .build();
        orderMapper.update(updateOrder);
        
        log.info("退款成功处理完成，订单ID: {}, 库存已回滚", order.getId());
    }
    
    @Transactional
    @Override
    public void adminRefund(Long orderId, String adminPhone) throws Exception {
        log.info("管理员审核通过退款，订单ID: {}，管理员手机号: {}", orderId, adminPhone);
        
        // 验证手机号
        if (!"13020695025".equals(adminPhone)) {
            log.error("管理员手机号验证失败，输入手机号: {}", adminPhone);
            throw new OrderBusinessException("管理员手机号验证失败");
        }
        
        // 查询订单信息并锁定（防止并发/重复退款）
        Orders order = orderMapper.getDetailsByIdForUpdate(orderId);
        if (order == null) {
            log.error("订单不存在，订单ID: {}", orderId);
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        
        // 验证订单状态（支持已支付、已发货、退款中，以及已完成的订单主动退款）
        if (!Orders.REFUNDING.equals(order.getStatus()) && 
            !Orders.PAID.equals(order.getStatus()) && 
            !Orders.SHIPPED.equals(order.getStatus()) && 
            !Orders.COMPLETED.equals(order.getStatus())) {
            log.error("订单状态不允许退款，订单ID: {}，当前状态: {}", orderId, order.getStatus());
            throw new OrderBusinessException("当前订单状态不允许退款");
        }
        
        // 如果是已完成订单，需验证系统配置是否允许已完成订单退款
        if (Orders.COMPLETED.equals(order.getStatus()) && !allowCompletedRefund) {
            log.error("系统配置不允许对已完成订单进行退款，订单ID: {}", orderId);
            throw new OrderBusinessException("系统配置不允许对已完成订单进行退款");
        }
        
        // 生成退款单号
        String outRefundNo = "RF" + System.currentTimeMillis();
        
        log.info("订单信息: orderNo={}, amount={}, transactionId={}", 
            order.getOrderNo(), order.getAmount(), order.getTransactionId());
        log.info("退款单号: {}", outRefundNo);
        
        try {
            if (order.getIsOverseas() != null && order.getIsOverseas() == 1) {
                // 海外订单，调用 PayPal 退款
                String captureId = order.getPaypalCaptureId();
                if (captureId == null || captureId.isEmpty()) {
                    log.error("海外订单没有 PayPal 捕获 ID，无法进行 PayPal 退款，订单ID: {}", orderId);
                    throw new OrderBusinessException("无法进行 PayPal 退款，因为缺少捕获 ID");
                }
                
                log.info("调用 PayPal 退款接口，captureId: {}, 金额: {}", captureId, order.getAmount());
                String refundId = payPalPayUtil.refund(captureId, order.getAmount(), "USD");
                log.info("PayPal 退款接口调用成功，结果 refundId: {}", refundId);
                
                // 回滚库存
                rollbackOrderStock(orderId);
                
                // 更新订单状态为已退款
                LocalDateTime now = LocalDateTime.now();
                Orders updateOrder = Orders.builder()
                    .id(orderId)
                    .status(Orders.REFUNDED)
                    .refundId(refundId)
                    .refundAmount(order.getAmount())
                    .refundTime(now)
                    .updateTime(now)
                    .build();
                orderMapper.update(updateOrder);
                
                log.info("管理员 PayPal 退款成功并更新订单状态为已退款，订单ID: {}, 退款单号: {}", orderId, refundId);
            } else {
                // 调用微信退款接口
                String refundResult = weChatPayUtil.refund(
                    order.getOrderNo(),      // 商户订单号
                    outRefundNo,             // 商户退款单号
                    order.getAmount(),       // 退款金额（全额退款）
                    order.getAmount()        // 原订单金额
                );
                
                log.info("微信退款接口调用成功，结果: {}", refundResult);
                
                // 解析退款结果
                JSONObject refundJson = JSON.parseObject(refundResult);
                String refundId = refundJson.getString("refund_id"); // 微信退款单号
                
                // 更新订单状态为退款中（保持退款中状态，等待微信回调完成退款）
                LocalDateTime now = LocalDateTime.now();
                Orders updateOrder = Orders.builder()
                    .id(orderId)
                    .status(Orders.REFUNDING) // 显式设置为退款中状态
                    .refundAmount(order.getAmount())
                    .refundTime(now)
                    .refundId(refundId)  // 更新微信退款单号
                    .updateTime(now)
                    .build();
                orderMapper.update(updateOrder);
                
                log.info("管理员退款审核成功，订单ID: {}，退款单号: {}，微信退款ID: {}", 
                    orderId, outRefundNo, refundId);
            }
        } catch (Exception e) {
            log.error("管理员退款失败，订单ID: {}", orderId, e);
            throw new RuntimeException("管理员退款失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 回滚订单库存（通用方法）
     * @param orderId 订单ID
     */
    private void rollbackOrderStock(Long orderId) {
        log.info("开始回滚订单库存，订单ID: {}", orderId);
        
        // 查询订单信息，根据订单号前缀判断订单类型
        Orders order = orderMapper.getDetailsById(orderId);
        if (order == null) {
            log.warn("订单不存在，无法回滚库存，订单ID: {}", orderId);
            return;
        }
        
        // 根据订单号前缀判断类型：以"ORD"开头的是普通商品订单，以"DIY"开头的是DIY订单
        String orderNo = order.getOrderNo();
        boolean isDiyOrder = orderNo.startsWith("DIY");
        log.info("订单类型: {}, 订单号: {}", isDiyOrder ? "DIY订单" : "普通商品订单", orderNo);
        
        // 查询订单项
        List<OrderItem> orderItems = orderDetailMapper.getByOrderId(orderId);
        if (orderItems == null || orderItems.isEmpty()) {
            log.warn("订单无订单项，无需回滚库存，订单ID: {}", orderId);
            return;
        }
        
        for (OrderItem orderItem : orderItems) {
            Long productId = orderItem.getProductId();
            Integer quantity = orderItem.getQuantity();
            
            try {
                if (isDiyOrder) {
                    // DIY订单：回滚 diy_material 表的库存
                    diyMaterialMapper.rollbackStock(productId, quantity);
                    log.info("成功回滚 DIY 材料库存: materialId={}, quantity={}", productId, quantity);
                } else {
                    // 普通商品订单：回滚 product 表的库存
                    Product product = productMapper.getById(productId);
                    if (product != null) {
                        product.setStock(product.getStock() + quantity);
                        productMapper.update(product);
                        log.info("成功回滚普通商品库存: productId={}, quantity={}", productId, quantity);
                    } else {
                        log.warn("未找到商品，无法回滚库存: productId={}", productId);
                    }
                }
            } catch (Exception e) {
                log.error("回滚库存失败: productId={}, quantity={}, 订单类型={}", 
                    productId, quantity, isDiyOrder ? "DIY" : "普通商品", e);
                throw new RuntimeException("回滚库存失败", e);
            }
        }
        
        log.info("订单库存回滚完成，订单ID: {}", orderId);
    }
    
    @Autowired
    private PayPalPayUtil payPalPayUtil;
    
    @Override
    public OrderPaymentVO paypalPayment(String orderNo, String currencyCode) throws Exception {
        log.info("开始处理PayPal支付，订单号: {}, 货币代码: {}", orderNo, currencyCode);
        
        Long userId = BaseContext.getCurrentId();
        Orders ordersDB = orderMapper.getByNumberAndUserId(orderNo, userId);
        if (ordersDB == null) {
            log.error("订单不存在，订单号: {}, 用户ID: {}", orderNo, userId);
            throw new RuntimeException("订单不存在");
        }
        
        if (!Orders.PENDING_PAYMENT.equals(ordersDB.getStatus())) {
            String statusText = getStatusText(ordersDB.getStatus());
            log.error("订单状态不允许支付，订单号: {}, 当前状态: {} ({})", 
                orderNo, ordersDB.getStatus(), statusText);
            throw new OrderBusinessException("订单状态为【" + statusText + "】，无法支付");
        }
        
        List<OrderItem> orderItems = orderDetailMapper.getByOrderId(ordersDB.getId());
        String description = "商品购买";
        if (orderItems != null && !orderItems.isEmpty()) {
            String firstItemTitle = orderItems.get(0).getTitle();
            if (orderItems.size() > 1) {
                description = firstItemTitle + "等" + orderItems.size() + "件商品";
            } else {
                description = firstItemTitle;
            }
            if (description.length() > 127) {
                description = description.substring(0, 124) + "...";
            }
        }
        
        String paypalOrderId;
        String approvalUrl;
        try {
            PayPalPayUtil.CreateOrderResult createResult = payPalPayUtil.createOrder(
                orderNo,
                ordersDB.getAmount(),
                description,
                currencyCode
            );
            paypalOrderId = createResult.getOrderId();
            approvalUrl = createResult.getApprovalUrl();
        } catch (IOException e) {
            log.error("[PayPal支付] 创建PayPal订单失败", e);
            throw new RuntimeException("PayPal订单创建失败: " + e.getMessage());
        }

        log.info("PayPal订单创建成功，PayPal订单ID: {}，审批URL: {}", paypalOrderId, approvalUrl);

        Orders updateOrder = Orders.builder()
                .id(ordersDB.getId())
                .transactionId(paypalOrderId)
                .updateTime(LocalDateTime.now())
                .build();
        orderMapper.update(updateOrder);

        return OrderPaymentVO.builder()
                .paypalOrderId(paypalOrderId)
                .paypalApprovalUrl(approvalUrl)
                .approvalUrl(approvalUrl)
                .paymentMethod("PAYPAL")
                .build();
    }
    
    @Override
    public void paypalPaySuccess(String paypalOrderId, String payerId) throws Exception {
        log.info("处理PayPal支付成功回调，PayPal订单ID: {}, 支付者ID: {}", paypalOrderId, payerId);

        // 1. 优先查询合流后的主表订单 (基于新增的 paypal_order_id)
        Orders mainOrder = orderMapper.getByPaypalOrderId(paypalOrderId);
        if (mainOrder != null) {
            if (!Orders.PENDING_PAYMENT.equals(mainOrder.getStatus())) {
                log.warn("主表海外订单状态已变更，跳过处理，订单ID: {}, 当前状态: {}",
                    mainOrder.getId(), mainOrder.getStatus());
                return;
            }

            String captureId = payPalPayUtil.captureOrder(paypalOrderId);

            LocalDateTime now = LocalDateTime.now();
            mainOrder.setStatus(Orders.PAID);
            mainOrder.setPaypalCaptureId(captureId);
            mainOrder.setPayTime(now);
            mainOrder.setUpdateTime(now);
            orderMapper.update(mainOrder);

            log.info("PayPal支付成功（主表海外订单），订单ID: {}，捕获ID: {}", mainOrder.getId(), captureId);
            return;
        }

        // 2. 查历史独立海外表
        OverseasOrder overseasOrder = overseasOrderMapper.getByPaypalOrderId(paypalOrderId);
        if (overseasOrder != null) {
            if (!OverseasOrder.PENDING_PAYMENT.equals(overseasOrder.getStatus())) {
                log.warn("历史海外订单状态已变更，跳过处理，订单ID: {}, 当前状态: {}",
                    overseasOrder.getId(), overseasOrder.getStatus());
                return;
            }

            String captureId = payPalPayUtil.captureOrder(paypalOrderId);

            LocalDateTime now = LocalDateTime.now();
            overseasOrder.setStatus(OverseasOrder.PAID);
            overseasOrder.setPaypalCaptureId(captureId);
            overseasOrder.setPayTime(now);
            overseasOrder.setUpdateTime(now);
            overseasOrderMapper.update(overseasOrder);

            log.info("PayPal支付成功（历史海外表），订单ID: {}，捕获ID: {}", overseasOrder.getId(), captureId);
            return;
        }

        // 3. 查国内订单（历史遗留使用 transaction_id 存 paypalOrderId 的情况）
        Orders ordersDB = orderMapper.getByTransactionId(paypalOrderId);
        if (ordersDB == null) {
            log.error("订单不存在，PayPal订单ID: {}", paypalOrderId);
            throw new RuntimeException("订单不存在");
        }

        if (!Orders.PENDING_PAYMENT.equals(ordersDB.getStatus())) {
            log.warn("订单状态已变更，跳过处理，订单ID: {}, 当前状态: {}",
                ordersDB.getId(), ordersDB.getStatus());
            return;
        }

        String captureId = payPalPayUtil.captureOrder(paypalOrderId);

        LocalDateTime now = LocalDateTime.now();
        Orders updateOrder = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.PAID)
                .payTime(now)
                .updateTime(now)
                .transactionId(paypalOrderId)
                .build();
        orderMapper.update(updateOrder);

        log.info("PayPal支付成功（国内），订单ID: {}，捕获ID: {}", ordersDB.getId(), captureId);
    }

    // ==================== 海外版免登录接口（使用独立的 overseas_order 表） ====================

    @Override
    @Transactional
    public com.diy.controller.guest.GuestOrderController.GuestOrderCreateVO createGuestOrder(GuestOrderCreateDTO dto) throws Exception {
        log.info("免登录创建订单（合流到主表）：items={}, amount={}", dto.getItems() != null ? dto.getItems().size() : 0, dto.getAmount());

        // Generate order number
        String orderNo = "OS" + System.currentTimeMillis();

        // Build address info
        GuestOrderCreateDTO.GuestAddressDTO addr = dto.getAddress();
        LocalDateTime now = LocalDateTime.now();

        String detailAddress = "";
        if (addr != null) {
            StringBuilder sb = new StringBuilder();
            if (addr.getAddress() != null) sb.append(addr.getAddress());
            if (addr.getZip() != null) sb.append(" (").append(addr.getZip()).append(")");
            if (addr.getCountry() != null) sb.append(", ").append(addr.getCountry());
            detailAddress = sb.toString();
        }

        // Insert into order table
        Orders order = Orders.builder()
                .userId(0L) // 0 表示免登录游客单
                .orderNo(orderNo)
                .amount(dto.getAmount())
                .receiverName(addr != null ? addr.getName() : "")
                .receiverPhone(addr != null ? addr.getPhone() : "")
                .receiverProvince(addr != null ? addr.getState() : "")
                .receiverCity(addr != null ? addr.getCity() : "")
                .receiverDistrict("")
                .receiverDetail(detailAddress)
                .status(Orders.PENDING_PAYMENT)
                .remark(dto.getRemark())
                .email(dto.getEmail()) // 游客下单联系邮箱
                .createTime(now)
                .updateTime(now)
                .isOverseas(1)
                .build();
        orderMapper.insert(order);

        // Insert order items into order_item table
        if (dto.getItems() != null) {
            List<OrderItem> items = new ArrayList<>();
            for (GuestOrderCreateDTO.GuestOrderItemDTO item : dto.getItems()) {
                OrderItem orderItem = OrderItem.builder()
                        .orderId(order.getId())
                        .productId(item.getProductId())
                        .title(item.getTitle())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .productImage(item.getImage())
                        .build();
                items.add(orderItem);
            }
            orderDetailMapper.insertBatch(items);
        }

        log.info("免登录订单创建成功（合流），订单ID: {}, 订单号: {}", order.getId(), orderNo);

        return com.diy.controller.guest.GuestOrderController.GuestOrderCreateVO.builder()
                .orderId(order.getId())
                .orderNo(orderNo)
                .amount(dto.getAmount())
                .build();
    }

    @Override
    public OrderPaymentVO guestPaypalPayment(String orderNo, String currencyCode) throws Exception {
        log.info("免登录PayPal支付，订单号: {}, 货币: {}", orderNo, currencyCode);

        // 优先从主表查询
        Orders mainOrder = orderMapper.getByNumber(orderNo);
        if (mainOrder != null) {
            if (!Orders.PENDING_PAYMENT.equals(mainOrder.getStatus())) {
                String statusText = mainOrder.getStatus() == 0 ? "Pending" :
                    mainOrder.getStatus() == 1 ? "Paid" : String.valueOf(mainOrder.getStatus());
                log.error("订单状态不允许支付，订单号: {}, 当前状态: {}", orderNo, statusText);
                throw new OrderBusinessException("Order status is [" + statusText + "], cannot pay");
            }

            // 防重复
            String existingPaypalOrderId = mainOrder.getPaypalOrderId();
            if (existingPaypalOrderId != null && !existingPaypalOrderId.isEmpty()) {
                try {
                    JSONObject existingOrder = payPalPayUtil.getOrderDetails(existingPaypalOrderId);
                    String existingStatus = existingOrder.getString("status");
                    if ("CREATED".equals(existingStatus) || "APPROVED".equals(existingStatus)) {
                        log.info("主表复用已有的PayPal订单: {}", existingPaypalOrderId);
                        String approvalUrl = null;
                        com.alibaba.fastjson.JSONArray links = existingOrder.getJSONArray("links");
                        if (links != null) {
                            for (int i = 0; i < links.size(); i++) {
                                JSONObject link = links.getJSONObject(i);
                                if ("approve".equals(link.getString("rel"))) {
                                    approvalUrl = link.getString("href");
                                    break;
                                }
                            }
                        }
                        return OrderPaymentVO.builder()
                                .paypalOrderId(existingPaypalOrderId)
                                .paypalApprovalUrl(approvalUrl)
                                .approvalUrl(approvalUrl)
                                .paymentMethod("PAYPAL")
                                .build();
                    }
                } catch (Exception e) {
                    log.warn("查询已有PayPal订单失败，将创建新订单: {}", e.getMessage());
                }
            }

            // Build description
            List<OrderItem> orderItems = orderDetailMapper.getByOrderId(mainOrder.getId());
            String description = "Bracelet Purchase";
            if (orderItems != null && !orderItems.isEmpty()) {
                String firstItemTitle = orderItems.get(0).getTitle();
                if (orderItems.size() > 1) {
                    description = firstItemTitle + " and " + (orderItems.size() - 1) + " more items";
                } else {
                    description = firstItemTitle;
                }
                if (description.length() > 127) {
                    description = description.substring(0, 124) + "...";
                }
            }

            PayPalPayUtil.CreateOrderResult createResult = payPalPayUtil.createOrder(
                orderNo, mainOrder.getAmount(), description, currencyCode);
            String paypalOrderId = createResult.getOrderId();
            String approvalUrl = createResult.getApprovalUrl();

            log.info("PayPal订单创建成功，PayPal订单ID: {}，审批URL: {}", paypalOrderId, approvalUrl);

            mainOrder.setPaypalOrderId(paypalOrderId);
            mainOrder.setUpdateTime(LocalDateTime.now());
            orderMapper.update(mainOrder);

            return OrderPaymentVO.builder()
                    .paypalOrderId(paypalOrderId)
                    .paypalApprovalUrl(approvalUrl)
                    .approvalUrl(approvalUrl)
                    .paymentMethod("PAYPAL")
                    .build();
        }

        // 兜底：从历史海外表查询
        OverseasOrder order = overseasOrderMapper.getByOrderNo(orderNo);
        if (order == null) {
            log.error("海外订单不存在，订单号: {}", orderNo);
            throw new RuntimeException("Order not found");
        }

        if (!OverseasOrder.PENDING_PAYMENT.equals(order.getStatus())) {
            String statusText = order.getStatus() == 0 ? "Pending" :
                order.getStatus() == 1 ? "Paid" : String.valueOf(order.getStatus());
            log.error("订单状态不允许支付，订单号: {}, 当前状态: {}", orderNo, statusText);
            throw new OrderBusinessException("Order status is [" + statusText + "], cannot pay");
        }

        String existingPaypalOrderId = order.getPaypalOrderId();
        if (existingPaypalOrderId != null && !existingPaypalOrderId.isEmpty()) {
            try {
                JSONObject existingOrder = payPalPayUtil.getOrderDetails(existingPaypalOrderId);
                String existingStatus = existingOrder.getString("status");
                if ("CREATED".equals(existingStatus) || "APPROVED".equals(existingStatus)) {
                    log.info("复用已有的PayPal订单: {}", existingPaypalOrderId);
                    String approvalUrl = null;
                    com.alibaba.fastjson.JSONArray links = existingOrder.getJSONArray("links");
                    if (links != null) {
                        for (int i = 0; i < links.size(); i++) {
                            JSONObject link = links.getJSONObject(i);
                            if ("approve".equals(link.getString("rel"))) {
                                approvalUrl = link.getString("href");
                                break;
                            }
                        }
                    }
                    return OrderPaymentVO.builder()
                            .paypalOrderId(existingPaypalOrderId)
                            .paypalApprovalUrl(approvalUrl)
                            .approvalUrl(approvalUrl)
                            .paymentMethod("PAYPAL")
                            .build();
                }
            } catch (Exception e) {
                log.warn("查询已有PayPal订单失败，将创建新订单: {}", e.getMessage());
            }
        }

        List<OverseasOrderItem> orderItems = overseasOrderItemMapper.getByOrderId(order.getId());
        String description = "Bracelet Purchase";
        if (orderItems != null && !orderItems.isEmpty()) {
            String firstItemTitle = orderItems.get(0).getTitle();
            if (orderItems.size() > 1) {
                description = firstItemTitle + " and " + (orderItems.size() - 1) + " more items";
            } else {
                description = firstItemTitle;
            }
            if (description.length() > 127) {
                description = description.substring(0, 124) + "...";
            }
        }

        PayPalPayUtil.CreateOrderResult createResult = payPalPayUtil.createOrder(
            orderNo, order.getAmount(), description, currencyCode);
        String paypalOrderId = createResult.getOrderId();
        String approvalUrl = createResult.getApprovalUrl();

        log.info("PayPal订单创建成功，PayPal订单ID: {}，审批URL: {}", paypalOrderId, approvalUrl);

        order.setPaypalOrderId(paypalOrderId);
        order.setUpdateTime(LocalDateTime.now());
        overseasOrderMapper.update(order);

        return OrderPaymentVO.builder()
                .paypalOrderId(paypalOrderId)
                .paypalApprovalUrl(approvalUrl)
                .approvalUrl(approvalUrl)
                .paymentMethod("PAYPAL")
                .build();
    }

    @Override
    public OrderVO getGuestOrderDetail(String orderNo) {
        log.info("免登录查询订单详情，订单号: {}", orderNo);

        // 优先从主表查询
        Orders mainOrder = orderMapper.getByNumber(orderNo);
        if (mainOrder != null) {
            List<OrderItem> orderItems = orderDetailMapper.getByOrderId(mainOrder.getId());
            OrderVO vo = new OrderVO();
            vo.setId(mainOrder.getId());
            vo.setOrderNo(mainOrder.getOrderNo());
            vo.setAmount(mainOrder.getAmount());
            vo.setStatus(mainOrder.getStatus());
            vo.setReceiverName(mainOrder.getReceiverName());
            vo.setReceiverPhone(mainOrder.getReceiverPhone());
            vo.setReceiverProvince(mainOrder.getReceiverProvince());
            vo.setReceiverCity(mainOrder.getReceiverCity());
            vo.setReceiverDetail(mainOrder.getReceiverDetail());
            vo.setRemark(mainOrder.getRemark());
            vo.setTransactionId(mainOrder.getPaypalOrderId());
            vo.setCreateTime(mainOrder.getCreateTime());
            vo.setPayTime(mainOrder.getPayTime());
            vo.setIsOverseas(mainOrder.getIsOverseas());
            vo.setOrderItems(orderItems);
            return vo;
        }

        // 历史海外订单表查询
        OverseasOrder order = overseasOrderMapper.getByOrderNo(orderNo);
        if (order == null) {
            log.error("海外订单不存在，订单号: {}", orderNo);
            throw new RuntimeException("Order not found");
        }

        List<OverseasOrderItem> orderItems = overseasOrderItemMapper.getByOrderId(order.getId());

        // 转换为通用 OrderVO（兼容前端）
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverProvince(order.getReceiverState());
        vo.setReceiverCity(order.getReceiverCity());
        vo.setReceiverDetail(order.getReceiverAddress());
        vo.setRemark(order.getRemark());
        vo.setTransactionId(order.getPaypalOrderId());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setIsOverseas(1);

        if (orderItems != null) {
            List<OrderItem> itemList = new ArrayList<>();
            for (OverseasOrderItem oi : orderItems) {
                OrderItem item = new OrderItem();
                item.setOrderId(oi.getOrderId());
                item.setProductId(oi.getProductId());
                item.setTitle(oi.getTitle());
                item.setPrice(oi.getPrice());
                item.setQuantity(oi.getQuantity());
                item.setProductImage(oi.getProductImage());
                itemList.add(item);
            }
            vo.setOrderItems(itemList);
        }

        return vo;
    }

    @Override
    public java.util.List<OrderVO> getGuestOrderList(String email, String orderNo) {
        log.info("免登录查询订单列表，email: {}, orderNo: {}", email, orderNo);
        java.util.List<OrderVO> result = new java.util.ArrayList<>();
        
        if ((email == null || email.trim().isEmpty()) && (orderNo == null || orderNo.trim().isEmpty())) {
            return result;
        }

        java.util.List<Orders> mainOrders = orderMapper.getByEmailOrOrderNo(
                email != null ? email.trim() : null, 
                orderNo != null ? orderNo.trim() : null
        );

        if (mainOrders != null && !mainOrders.isEmpty()) {
            for (Orders mainOrder : mainOrders) {
                java.util.List<OrderItem> orderItems = orderDetailMapper.getByOrderId(mainOrder.getId());
                OrderVO vo = new OrderVO();
                vo.setId(mainOrder.getId());
                vo.setOrderNo(mainOrder.getOrderNo());
                vo.setAmount(mainOrder.getAmount());
                vo.setStatus(mainOrder.getStatus());
                vo.setReceiverName(mainOrder.getReceiverName());
                vo.setReceiverPhone(mainOrder.getReceiverPhone());
                vo.setReceiverProvince(mainOrder.getReceiverProvince());
                vo.setReceiverCity(mainOrder.getReceiverCity());
                vo.setReceiverDetail(mainOrder.getReceiverDetail());
                vo.setRemark(mainOrder.getRemark());
                vo.setEmail(mainOrder.getEmail());
                vo.setTransactionId(mainOrder.getPaypalOrderId());
                vo.setCreateTime(mainOrder.getCreateTime());
                vo.setPayTime(mainOrder.getPayTime());
                vo.setIsOverseas(mainOrder.getIsOverseas());
                vo.setOrderItems(orderItems);
                result.add(vo);
            }
        }

        if (orderNo != null && !orderNo.trim().isEmpty() && result.isEmpty()) {
            OverseasOrder order = overseasOrderMapper.getByOrderNo(orderNo.trim());
            if (order != null) {
                java.util.List<OverseasOrderItem> orderItems = overseasOrderItemMapper.getByOrderId(order.getId());
                OrderVO vo = new OrderVO();
                vo.setId(order.getId());
                vo.setOrderNo(order.getOrderNo());
                vo.setAmount(order.getAmount());
                vo.setStatus(order.getStatus());
                vo.setReceiverName(order.getReceiverName());
                vo.setReceiverPhone(order.getReceiverPhone());
                vo.setReceiverProvince(order.getReceiverState());
                vo.setReceiverCity(order.getReceiverCity());
                vo.setReceiverDetail(order.getReceiverAddress());
                vo.setRemark(order.getRemark());
                vo.setTransactionId(order.getPaypalOrderId());
                vo.setCreateTime(order.getCreateTime());
                vo.setPayTime(order.getPayTime());
                vo.setIsOverseas(1);

                if (orderItems != null) {
                    java.util.List<OrderItem> itemList = new java.util.ArrayList<>();
                    for (OverseasOrderItem oi : orderItems) {
                        OrderItem item = new OrderItem();
                        item.setOrderId(oi.getOrderId());
                        item.setProductId(oi.getProductId());
                        item.setTitle(oi.getTitle());
                        item.setPrice(oi.getPrice());
                        item.setQuantity(oi.getQuantity());
                        item.setProductImage(oi.getProductImage());
                        itemList.add(item);
                    }
                    vo.setOrderItems(itemList);
                }
                result.add(vo);
            }
        }

        return result;
    }

    @Override
    public OrderPaymentStatusVO getPaymentStatus(String orderNo) {
        log.info("查询订单支付状态，订单号: {}", orderNo);

        // 1. 优先查合流主表
        Orders mainOrder = orderMapper.getByNumber(orderNo);
        if (mainOrder != null) {
            boolean isPaid = mainOrder.getStatus() != null && mainOrder.getStatus() >= 1;
            String paypalOrderId = mainOrder.getPaypalOrderId();

            if (isPaid) {
                return OrderPaymentStatusVO.builder()
                        .isPaid(true)
                        .orderNo(orderNo)
                        .status(mainOrder.getStatus())
                        .build();
            }

            if (paypalOrderId != null && !paypalOrderId.isEmpty()) {
                try {
                    JSONObject paypalOrder = payPalPayUtil.getOrderDetails(paypalOrderId);
                    String paypalStatus = paypalOrder.getString("status");

                    if ("APPROVED".equals(paypalStatus)) {
                        log.info("主表订单 {} PayPal状态为APPROVED，主动执行capture", orderNo);
                        String captureId = payPalPayUtil.captureOrder(paypalOrderId);

                        mainOrder.setStatus(Orders.PAID);
                        mainOrder.setPaypalCaptureId(captureId);
                        mainOrder.setPayTime(LocalDateTime.now());
                        mainOrder.setUpdateTime(LocalDateTime.now());
                        orderMapper.update(mainOrder);

                        return OrderPaymentStatusVO.builder()
                                .isPaid(true)
                                .orderNo(orderNo)
                                .status(Orders.PAID)
                                .build();
                    }
                    log.info("主表订单 {} PayPal状态: {}", orderNo, paypalStatus);
                } catch (Exception e) {
                    log.warn("查询PayPal订单状态失败（主表），订单号: {}，错误: {}", orderNo, e.getMessage());
                }
            }

            return OrderPaymentStatusVO.builder()
                    .isPaid(false)
                    .orderNo(orderNo)
                    .status(mainOrder.getStatus())
                    .build();
        }

        // 2. 查历史独立海外表
        OverseasOrder order = overseasOrderMapper.getByOrderNo(orderNo);
        if (order != null) {
            boolean isPaid = order.getStatus() != null && order.getStatus() >= 1;
            String paypalOrderId = order.getPaypalOrderId();

            // 如果本地已标记为已支付，直接返回
            if (isPaid) {
                return OrderPaymentStatusVO.builder()
                        .isPaid(true)
                        .orderNo(orderNo)
                        .status(order.getStatus())
                        .build();
            }

            // 如果有 PayPal 订单ID，查询 PayPal 状态
            if (paypalOrderId != null && !paypalOrderId.isEmpty()) {
                try {
                    JSONObject paypalOrder = payPalPayUtil.getOrderDetails(paypalOrderId);
                    String paypalStatus = paypalOrder.getString("status");

                    // 如果 PayPal 状态是 APPROVED，主动 capture
                    if ("APPROVED".equals(paypalStatus)) {
                        log.info("历史订单 {} PayPal状态为APPROVED，主动执行capture", orderNo);
                        String captureId = payPalPayUtil.captureOrder(paypalOrderId);

                        order.setStatus(OverseasOrder.PAID);
                        order.setPaypalCaptureId(captureId);
                        order.setPayTime(LocalDateTime.now());
                        order.setUpdateTime(LocalDateTime.now());
                        overseasOrderMapper.update(order);

                        return OrderPaymentStatusVO.builder()
                                .isPaid(true)
                                .orderNo(orderNo)
                                .status(OverseasOrder.PAID)
                                .build();
                    }

                    log.info("历史订单 {} PayPal状态: {}", orderNo, paypalStatus);
                } catch (Exception e) {
                    log.warn("查询PayPal订单状态失败（历史表），订单号: {}，错误: {}", orderNo, e.getMessage());
                }
            }

            return OrderPaymentStatusVO.builder()
                    .isPaid(false)
                    .orderNo(orderNo)
                    .status(order.getStatus())
                    .build();
        }

        // 3. 国内订单查询（原有逻辑）
        Orders ordersDB = orderMapper.getByNumber(orderNo);
        if (ordersDB == null) {
            throw new RuntimeException("订单不存在");
        }

        boolean isPaid = ordersDB.getStatus() != null && ordersDB.getStatus() >= 1;
        return OrderPaymentStatusVO.builder()
                .isPaid(isPaid)
                .orderNo(orderNo)
                .status(ordersDB.getStatus())
                .build();
    }
}
