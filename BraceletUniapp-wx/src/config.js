// API配置文件 - 根据后端接口文档配置
// 基于 api-documentation.json

// ==================== API 运行模式配置 ====================
// 是否使用本地开发环境接口 (true: 本地 8080 端口, false: 远端生产接口)
const USE_LOCAL_API = true 

const LOCAL_API_BASE_URL = 'http://127.0.0.1:8080'
const REMOTE_API_BASE_URL = 'https://diy.hk.cn/api'
// =========================================================

// 开发环境
const DEV_API_BASE_URL = USE_LOCAL_API ? LOCAL_API_BASE_URL : REMOTE_API_BASE_URL

// 生产环境
const PROD_API_BASE_URL = REMOTE_API_BASE_URL

// 判断当前环境
const isDevelopment = process.env.NODE_ENV === 'development'

// 导出当前使用的API地址
export const API_BASE_URL = isDevelopment ? DEV_API_BASE_URL : PROD_API_BASE_URL

// JWT Token认证配置
export const TOKEN_HEADER = 'authentication'  // 请求头中token的key
export const STORAGE_TOKEN_KEY = 'token'       // 本地存储token的key
export const STORAGE_USER_KEY = 'user'         // 本地存储用户信息的key

// API接口路径 - 根据接口文档
export const API_PATHS = {
  // ==================== 用户登录模块 ====================
  // 微信登录
  WECHAT_LOGIN: '/user/user/login',
  
  // ==================== 分类模块 ====================
  // 查询分类列表
  CATEGORY_LIST: '/user/category/list',
  
  // ==================== 商品模块 ====================
  // 根据分类查询商品列表
  PRODUCT_LIST: '/user/product/list',
  // 查询商品详情
  PRODUCT_DETAIL: '/user/product/detail',
  
  // ==================== 购物车模块 ====================
  // 添加商品到购物车
  CART_ADD: '/user/cart/add',
  // 查看购物车
  CART_LIST: '/user/cart/list',
  // 删除购物车商品
  CART_DELETE: '/user/cart/delete',
  // 清空购物车
  CART_CLEAN: '/user/cart/clean',
  // 减少商品数量
  CART_SUB: '/user/cart/sub',
  
  // ==================== 订单模块 ====================
  // 提交订单
  // 注意：原接口 /user/order/submit 返回404，暂时使用 /user/order/create 替代
  // 前端需确保先将商品加入购物车
  ORDER_SUBMIT: '/user/order/create',
  // 从购物车创建订单
  ORDER_CREATE: '/user/order/create',
  // 订单支付
  ORDER_PAYMENT: '/user/order/payment',
  // 查询订单支付状态
  ORDER_PAYMENT_STATUS: '/user/order/paymentStatus',
  // 分页查询订单列表
  ORDER_LIST: '/user/order/list',
  // 查询订单详情
  ORDER_DETAIL: '/user/order/detail',
  // 历史订单查询
  ORDER_HISTORY: '/user/order/historyOrders',
  // 查询订单详情（旧接口）
  ORDER_DETAIL_BY_ID: '/user/order/orderDetail',
  // 取消订单
  ORDER_CANCEL: '/user/order/cancel',
  // 确认收货
  ORDER_COMPLETE: '/user/order/complete',
  // 再来一单
  ORDER_REPETITION: '/user/order/repetition',
  // 催单
  ORDER_REMINDER: '/user/order/reminder',
  
  // ==================== 管理员模块 ====================
  // 订单发货
  ADMIN_ORDER_DELIVERY: '/admin/order/deliveryWithTrackingNumber',

  // ==================== 轮播图模块 ====================
  // 查询轮播图列表
  BANNER_LIST: '/user/banner/list',
  
  // ==================== 店铺模块 ====================
  // 获取店铺营业状态
  SHOP_STATUS: '/user/shop/status',
  
  // ==================== DIY设计模块 ====================
  // 查询DIY分类列表
  DIY_CATEGORY_LIST: '/user/design/category/list',
  // 查询色系列表
  DIY_COLOR_SERIES_LIST: '/user/design/colorSeries/list',
  // 查询DIY材料列表（支持分类和色系筛选）
  DIY_MATERIAL_LIST: '/user/design/material/list',
  // 从DIY设计创建订单
  DIY_ORDER_CREATE: '/user/design/order/create',

  // ==================== AI推荐模块 ====================
  // 获取AI搭配推荐
  AI_RECOMMEND: '/user/ai/recommend',
  // 获取AI算命搭配推荐（八卦命理）
  AI_FORTUNE_RECOMMEND: '/user/ai/fortune-recommend',

  // ==================== 地址管理模块 ====================
  // 添加/修改地址
  ADDRESS_ADD: '/user/address/add',
  // 查询地址列表
  ADDRESS_LIST: '/user/address/list',
  // 获取默认地址
  ADDRESS_DEFAULT: '/user/address/default',
  // 删除地址
  ADDRESS_DELETE: '/user/address/delete',
  // 设置默认地址
  ADDRESS_SET_DEFAULT: '/user/address/setDefault'
}

// 请求超时时间（毫秒）
export const REQUEST_TIMEOUT = 10000

// 请求头配置
export const REQUEST_HEADERS = {
  'Content-Type': 'application/json'
}

// 订单状态枚举
export const ORDER_STATUS = {
  PENDING: 0,      // 待支付
  PAID: 1,         // 已支付
  SHIPPED: 2,      // 已发货
  COMPLETED: 3     // 已完成
}

// 订单状态文本
export const ORDER_STATUS_TEXT = {
  0: '待支付',
  1: '已支付',
  2: '已发货',
  3: '已完成'
}

// 店铺状态枚举
export const SHOP_STATUS = {
  CLOSED: 0,       // 已打样
  OPEN: 1          // 营业中
}

// 商品状态枚举
export const PRODUCT_STATUS = {
  OFFLINE: 0,      // 下架
  ONLINE: 1        // 上架
}

// 响应码
export const RESPONSE_CODE = {
  SUCCESS: 1,      // 成功 (旧接口)
  FAIL: 0,         // 失败 (旧接口)
  SUCCESS_NEW: 0   // 成功 (新接口 /design/)
}
