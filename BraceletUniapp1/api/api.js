/**
 * API接口定义 - 根据后端接口文档
 * 基于 api-documentation.json v1.0.0
 */

import { get, post, put, del } from './request.js'
import { API_PATHS, STORAGE_TOKEN_KEY, STORAGE_USER_KEY, API_BASE_URL, TOKEN_HEADER, RESPONSE_CODE } from '../config.js'

// ==================== 用户登录模块 ====================

/**
 * 微信登录
 * @param {String} code 微信授权code
 * @param {Object} profile { nickName, avatarUrl }
 * @returns {Promise} { id, openid, token, nickname, avatar }
 */
export function wechatLogin(code, profile = {}) {
  const payload = {
    code,
    nickName: profile.nickName,
    avatarUrl: profile.avatarUrl
  }
  console.log('🔑 开始微信登录，code:', code, 'profile:', JSON.stringify(profile))
  return post(API_PATHS.WECHAT_LOGIN, payload, false)
    .then(res => {
      console.log('🔑 登录API返回:', JSON.stringify(res))
      // 登录成功后保存token和用户信息
      if (res && res.token) {
        try {
          uni.setStorageSync(STORAGE_TOKEN_KEY, res.token)
          uni.setStorageSync(STORAGE_USER_KEY, {
            id: res.id,
            openid: res.openid,
            nickName: res.nickname,
            avatarUrl: res.avatar
          })
          console.log('✅ Token已保存:', res.token)
        } catch (e) {
          console.error('保存登录信息失败:', e)
        }
      } else {
        console.warn('⚠️ 登录响应中无token字段，完整响应:', JSON.stringify(res))
      }
      return res
    })
    .catch(err => {
      console.error('❌ 登录请求失败:', JSON.stringify(err))
      throw err
    })
}

// ==================== 分类模块 ====================

/**
 * 查询分类列表
 * @returns {Promise} { categories: [] }
 */
export function getCategoryList() {
  // TODO: Set to true in production
  return get(API_PATHS.CATEGORY_LIST, {}, false)
    .then(res => res.categories || res || [])
}

// ==================== 商品模块 ====================

/**
 * 根据分类查询商品列表
 * @param {Number} categoryId 分类ID
 * @returns {Promise} { products: [] }
 */
export function getProductList(categoryId) {
  // 商品列表无需登录
  return get(API_PATHS.PRODUCT_LIST, { categoryId }, false)
    .then(res => res.products || res || [])
}

/**
 * 查询商品详情
 * @param {Number} id 商品ID
 * @returns {Promise} { product: {} }
 */
export function getProductDetail(id) {
  // 商品详情无需登录
  return get(API_PATHS.PRODUCT_DETAIL, { id }, false)
    .then(res => res.product || res || null)
}

// ==================== 购物车模块 ====================

/**
 * 添加商品到购物车
 * @param {Number} productId 商品ID
 * @param {Number} quantity 数量
 * @returns {Promise}
 */
export function addToCart(productId, quantity = 1) {
  return post(API_PATHS.CART_ADD, { productId, quantity })
}

/**
 * 查看购物车
 * @returns {Promise} { items: [], totalAmount: 0 }
 */
export function getCartList() {
  return get(API_PATHS.CART_LIST, {})
    .then(res => {
      return {
        items: res.items || res || [],
        totalAmount: res.totalAmount || 0
      }
    })
}

/**
 * 删除购物车商品
 * @param {Number} productId 商品ID
 * @returns {Promise}
 */
export function deleteCartItem(productId) {
  return post(API_PATHS.CART_DELETE, { productId })
}

/**
 * 清空购物车
 * @returns {Promise}
 */
export function clearCart() {
  return del(API_PATHS.CART_CLEAN, {})
}

/**
 * 减少商品数量（旧接口）
 * @param {Object} params { dishId, setmealId, dishFlavor }
 * @returns {Promise}
 */
export function subCartItem(params) {
  return post(API_PATHS.CART_SUB, params)
}

// ==================== 订单模块 ====================

/**
 * 提交订单
 * @param {Object} orderData 订单数据
 * @returns {Promise} { id, orderNo, amount, orderTime }
 */
export function submitOrder(orderData) {
  return post(API_PATHS.ORDER_SUBMIT, orderData)
}

/**
 * 从购物车创建订单
 * @returns {Promise} { orderId, orderNo, amount }
 */
export function createOrderFromCart(data = {}) {
  return post(API_PATHS.ORDER_CREATE, data)
}

/**
 * 订单支付
 * @param {String} orderNo 订单号
 * @param {Number} payMethod 付款方式
 * @returns {Promise} 微信支付参数
 */
export function payOrder(orderNo, payMethod) {
  // 根据接口文档，参数名为 orderNumber
  return put(API_PATHS.ORDER_PAYMENT, { orderNumber: orderNo, payMethod })
}

/**
 * 查询订单支付状态
 * @param {String} orderNo 订单号
 * @returns {Promise} { isPaid, status, statusText }
 */
export function checkPaymentStatus(orderNo) {
  return get(API_PATHS.ORDER_PAYMENT_STATUS, { orderNo })
}

/**
 * 分页查询订单列表
 * @param {Object} params { page, size, status, orderNo }
 * @returns {Promise} { orders: [], page, size, total }
 */
export function getOrderList(params = {}) {
  const { page = 1, size = 20, status, orderNo } = params
  const queryParams = { page, size }
  if (status !== undefined) {
    queryParams.status = status
  }
  if (orderNo) {
    queryParams.orderNo = orderNo
  }
  return get(API_PATHS.ORDER_LIST, queryParams)
}

/**
 * 查询订单详情
 * @param {Number} id 订单ID
 * @returns {Promise} { order: {} }
 */
export function getOrderDetail(id) {
  // 接口期望参数名为 orderId
  return get(API_PATHS.ORDER_DETAIL, { orderId: id })
}

/**
 * 历史订单查询（旧接口）
 * @param {Object} params { page, pageSize, status }
 * @returns {Promise}
 */
export function getHistoryOrders(params) {
  return get(API_PATHS.ORDER_HISTORY, params)
}

/**
 * 查询订单详情（旧接口 - 路径参数）
 * @param {Number} id 订单ID
 * @returns {Promise}
 */
export function getOrderDetailById(id) {
  return get(`${API_PATHS.ORDER_DETAIL_BY_ID}/${id}`, {})
}

/**
 * 取消订单
 * @param {Number} id 订单ID
 * @returns {Promise}
 */
export function cancelOrder(id) {
  return put(`${API_PATHS.ORDER_CANCEL}/${id}`, {})
}

/**
 * 确认收货
 * @param {Number} id 订单ID
 * @returns {Promise}
 */
export function completeOrder(id) {
  return put(`${API_PATHS.ORDER_COMPLETE}/${id}`, {})
}

/**
 * 再来一单
 * @param {Number} id 订单ID
 * @returns {Promise}
 */
export function repetitionOrder(id) {
  return post(`${API_PATHS.ORDER_REPETITION}/${id}`, {})
}

/**
 * 催单
 * @param {Number} id 订单ID
 * @returns {Promise}
 */
export function reminderOrder(id) {
  return get(`${API_PATHS.ORDER_REMINDER}/${id}`, {})
}

/**
 * 申请退款
 * @param {Number} id 订单ID
 * @returns {Promise}
 */
export function refundOrder(id) {
  return post(`/user/order/refund/${id}`, {})
}

// ==================== 管理员模块 ====================

/**
 * 订单发货
 * @param {Object} data { orderId, status, trackingNumber }
 * @returns {Promise}
 */
export function deliveryOrder(data) {
  return post(API_PATHS.ADMIN_ORDER_DELIVERY, data)
}

// ==================== 轮播图模块 ====================

/**
 * 查询轮播图列表
 * @returns {Promise} { banners: [] }
 */
export function getBannerList() {
  return get(API_PATHS.BANNER_LIST, {}, false)
    .then(res => res.banners || res || [])
}

// ==================== 店铺模块 ====================

/**
 * 获取店铺营业状态
 * @returns {Promise} Number 1=营业中 0=已打样
 */
export function getShopStatus() {
  return get(API_PATHS.SHOP_STATUS, {}, false)
    .then(res => {
      // 返回的data直接就是状态数字
      return typeof res === 'number' ? res : (res.status || 0)
    })
}

// ==================== DIY设计模块 ====================

/**
 * 查询DIY分类列表（含子分类/色系）
 * 对应接口: /user/design/colorSeries/list
 * @returns {Promise} [{ keyCode, name, children: [{ keyCode, name }] }]
 */
export function getDiyCategoryList() {
  // 注意：根据最新需求，使用 colorSeries/list 作为主分类接口
  return get(API_PATHS.DIY_COLOR_SERIES_LIST, {}, false)
    .then(res => {
      // 接口返回 Result<List<DiyCategoryWithChildrenVO>>
      // res 可能是直接的数组 (data) 或者包含 data 字段的对象
      const list = Array.isArray(res) ? res : (res.data || [])
      return list
    })
}

// ==================== AI推荐模块 ====================

/**
 * 获取AI搭配推荐
 * @param {String} prompt 用户输入内容
 * @param {Boolean} useTarot 是否启用塔罗牌分析
 * @returns {Promise} { analysis, bazi, materials[], tarotInfo, wuxing }
 */
export function aiRecommend(prompt, useTarot = false) {
  return post(API_PATHS.AI_RECOMMEND, { prompt, useTarot })
}

// ==================== 地址管理模块 ====================

/**
 * 添加/修改地址
 * @param {Object} data 地址信息
 * @returns {Promise}
 */
export function addAddress(data) {
  return post(API_PATHS.ADDRESS_ADD, data)
}

/**
 * 查询地址列表
 * @returns {Promise} []
 */
export function getAddressList() {
  return get(API_PATHS.ADDRESS_LIST, {})
    .then(res => {
      // 兼容可能的返回结构
      return Array.isArray(res) ? res : (res.addresses || res.data || [])
    })
}

/**
 * 获取默认地址
 * @returns {Promise} {}
 */
export function getDefaultAddress() {
  return get(API_PATHS.ADDRESS_DEFAULT, {})
}

/**
 * 删除地址
 * @param {Number} id 地址ID
 * @returns {Promise}
 */
export function deleteAddress(id) {
  return del(`${API_PATHS.ADDRESS_DELETE}/${id}`, {})
}

/**
 * 设置默认地址
 * @param {Number} id 地址ID
 * @returns {Promise}
 */
export function setDefaultAddress(id) {
  return put(`${API_PATHS.ADDRESS_SET_DEFAULT}/${id}`, {})
}

/**
 * 上传文件
 * @param {string} filePath - 文件临时路径
 * @param {string} [scene] - 场景参数 (可选)
 * @returns {Promise} { url: string }
 */
export function uploadFile(filePath, scene) {
  return new Promise((resolve, reject) => {
    let url = API_BASE_URL + '/user/common/upload'
    if (scene) {
      url += `?scene=${scene}`
    }
    
    uni.uploadFile({
      url: url,
      filePath: filePath,
      name: 'file',
      header: {
        [TOKEN_HEADER]: uni.getStorageSync(STORAGE_TOKEN_KEY) || ''
      },
      success: (res) => {
        console.log('Upload response raw:', res.data)
        if (res.statusCode === 200) {
          try {
            const data = JSON.parse(res.data)
            console.log('Upload response parsed:', data)
            // 兼容多种成功状态码：0(新接口), 1(旧接口), 200(标准HTTP风格)
            if (data.code === RESPONSE_CODE.SUCCESS || 
                data.code === RESPONSE_CODE.SUCCESS_NEW || 
                data.code === 200) {
              resolve(data.data)
            } else {
              console.error('上传业务失败:', data)
              reject(new Error(data.msg || '上传失败'))
            }
          } catch (e) {
            console.error('解析上传响应失败:', e)
            reject(e)
          }
        } else {
          console.error('上传HTTP失败:', res.statusCode)
          reject(new Error(`上传失败: ${res.statusCode}`))
        }
      },
      fail: (err) => {
        console.error('上传请求失败:', err)
        reject(err)
      }
    })
  })
}

/**
 * 查询DIY色系列表 (已废弃，合并入 getDiyCategoryList)
 * 保留此函数为了兼容性，但指向同一接口
 */
export const getColorSeriesList = getDiyCategoryList

/**
 * 查询DIY材料列表
 * 对应接口: /user/design/material/list
 * @param {Object} params { categories: string, colorSeries: string, page, size }
 * @returns {Promise} { materials: [], totalPages: number }
 */
export function getDiyMaterialList(params = {}) {
  const { categories, colorSeries, page = 1, size = 20 } = params
  const queryParams = { page, size }
  
  // 处理分类参数 (支持数组或逗号分隔字符串)
  if (categories) {
    queryParams.categories = Array.isArray(categories) ? categories.join(',') : categories
  }
  
  // 处理色系/子分类参数
  if (colorSeries) {
    queryParams.colorSeries = Array.isArray(colorSeries) ? colorSeries.join(',') : colorSeries
  }
  
  // 保留对 1-8 级分类的兼容支持 (如果还需要的话，根据 optimize query 的指示，主要关注上述两个参数)
  // 但为了不破坏现有逻辑，如果传入了这些key，还是带上
  for (let i = 1; i <= 8; i++) {
    const key = `classificationDetailKey${i}`
    if (params[key]) {
      queryParams[key] = params[key]
    }
  }
  
  return get(API_PATHS.DIY_MATERIAL_LIST, queryParams, false)
    .then(res => {
      // 响应结构: Result<Map<string, object>>
      // 预期 data 中包含 materials 列表
      const data = res.data || res
      
      // 兼容处理：检查 materials 是否存在
      let materials = []
      if (Array.isArray(data)) {
        materials = data
      } else if (data && Array.isArray(data.materials)) {
        materials = data.materials
        materials.totalPages = data.totalPages
      } else if (data && Array.isArray(data.records)) { // 分页常见字段
        materials = data.records
        materials.totalPages = data.pages
      }
      
      // 处理附加的分类详情 (如果有)
      for (let i = 1; i <= 8; i++) {
        const detailKey = `classificationDetail${i}`
        if (data[detailKey]) {
          materials[detailKey] = data[detailKey]
        }
      }
      
      return materials
    })
}

/**
 * 从DIY设计创建订单
 * @param {Object} data - diyOrderCreateDTO
 * @returns {Promise}
 */
export function createDiyOrder(data) {
  return post(API_PATHS.DIY_ORDER_CREATE, data)
}

// ==================== 导出兼容旧代码的函数名 ====================

// 轮播图
export const bannerList = getBannerList

// 分类
export const categoryList = getCategoryList

// 商品
export const productList = getProductList
export const productDetail = getProductDetail

// 购物车
export const cartAdd = addToCart
export const cartList = getCartList
export const cartDelete = deleteCartItem
export const cartUpdate = (productId, quantity) => {
  // 如果数量为0，删除；否则更新（需要先删除再添加）
  if (quantity <= 0) {
    return deleteCartItem(productId)
  }
  // 暂时没有单独的更新接口，使用删除+添加实现
  return deleteCartItem(productId).then(() => addToCart(productId, quantity))
}

// 订单
export const orderCreate = createOrderFromCart
export const orderList = getOrderList
export const orderDetail = getOrderDetail
export const orderPay = payOrder

// 登录
export const loginWithWeixinCode = (code, profile) => wechatLogin(code, profile)

// DIY设计
export const designCategoryList = getDiyCategoryList
export const designProductList = getDiyMaterialList
export const designOrderCreate = createDiyOrder

// ==================== 客服模块 ====================

/**
 * 获取客服二维码
 * @returns {Promise} { data: string } 二维码路径
 */
export function getCustomerServiceQRCode() {
  return get('/user/common/customer-service-qr', {}, false)
}
