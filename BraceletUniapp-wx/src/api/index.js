/**
 * API接口统一导出文件
 * - 真实后端API从 ./api.js 导入
 * - 设计台mock功能保留在此文件
 * - 地址管理本地存储功能保留在此文件
 */

// ==================== 导入真实后端API ====================
import {
  getAddressList,
  addAddress,
  deleteAddress,
  setDefaultAddress,
  getDefaultAddress
} from './api.js'

export {
  // 登录
  wechatLogin,
  loginWithWeixinCode,

  // 分类
  getCategoryList,
  categoryList,

  // 商品
  getProductList,
  getProductDetail,
  productList,
  productDetail,

  // 购物车
  addToCart,
  getCartList,
  deleteCartItem,
  clearCart,
  subCartItem,
  cartAdd,
  cartList,
  cartDelete,
  cartUpdate,

  // 订单
  submitOrder,
  createOrderFromCart,
  payOrder,
  checkPaymentStatus,
  getOrderList,
  getOrderDetail,
  getHistoryOrders,
  getOrderDetailById,
  cancelOrder,
  completeOrder,
  repetitionOrder,
  reminderOrder,
  orderCreate,
  orderList,
  orderDetail,
  orderPay,
  refundOrder, // Refund API

  // 管理员
  deliveryOrder,

  // 轮播图
  getBannerList,
  bannerList,

  // 店铺
  getShopStatus,

  // AI推荐
  aiRecommend,
  aiFortuneRecommend,

  // 地址管理
  addAddress,
  getAddressList,
  deleteAddress,
  setDefaultAddress,
  getDefaultAddress
} from './api.js'

import { STORAGE_TOKEN_KEY, STORAGE_USER_KEY } from '../config.js'

// ==================== 本地存储工具 ====================
const promise = (data) => Promise.resolve(data)

// ==================== 用户相关工具函数 ====================
export const userGet = () => {
  try {
    return uni.getStorageSync(STORAGE_USER_KEY) || null
  } catch (e) {
    return null
  }
}

export const userSet = (u) => {
  try {
    uni.setStorageSync(STORAGE_USER_KEY, u || null)
  } catch (e) {
    console.error('保存用户信息失败:', e)
  }
}

export const tokenGet = () => {
  try {
    return uni.getStorageSync(STORAGE_TOKEN_KEY) || ''
  } catch (e) {
    return ''
  }
}

// 检查是否已登录
export const isLoggedIn = () => {
  const user = userGet()
  const token = tokenGet()
  return !!(user && token)
}

// 要求登录（如果未登录则跳转到首页）
export const requireLogin = () => {
  if (!isLoggedIn()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index?login=1' })
    }, 1500)
    return false
  }
  return true
}

// 退出登录
export const logout = async () => {
  try {
    uni.removeStorageSync(STORAGE_USER_KEY)
    uni.removeStorageSync(STORAGE_TOKEN_KEY)
    return promise({ ok: true, message: '退出登录成功' })
  } catch (e) {
    console.error('退出登录失败', e)
    return promise({ ok: false, message: '退出登录失败' })
  }
}

// ==================== DIY设计模块（从后端API获取）====================
// 已迁移到 api.js，这里直接从 api.js 导出
export { 
  designCategoryList, 
  designProductList,
  uploadFile,
  createDiyOrder,
  designOrderCreate
} from './api.js'

// ==================== 地址管理适配层 ====================

// 后端 -> 前端 字段映射
const fromApiAddress = (addr) => {
  if (!addr) return null
  return {
    id: addr.id,
    name: addr.consignee || '',
    phone: addr.phone || '',
    province: addr.province || '',
    city: addr.city || '',
    district: addr.district || '',
    detail: addr.detailAddress || '',
    isDefault: addr.isDefault // 0 or 1
  }
}

// 前端 -> 后端 字段映射
const toApiAddress = (data) => {
  return {
    id: data.id, // 如果有id则是修改
    consignee: data.name,
    phone: data.phone,
    province: data.province,
    city: data.city,
    district: data.district,
    detailAddress: data.detail,
    isDefault: data.isDefault ? 1 : 0
  }
}

export const addressList = () => {
  return getAddressList().then(list => {
    return list.map(fromApiAddress)
  })
}

export const addressDetail = (id) => {
  // 由于没有详情接口，先获取列表再查找
  return getAddressList().then(list => {
    const addr = list.find(a => a.id === Number(id))
    return fromApiAddress(addr) || null
  })
}

export const addressAdd = (data) => {
  const apiData = toApiAddress(data)
  // 如果是添加，id可能不需要，或者由后端生成，但如果data里没有id则没问题
  return addAddress(apiData).then(res => ({ ok: true, data: res }))
}

export const addressUpdate = (id, data) => {
  // 复用 addAddress，确保 id 存在
  const apiData = toApiAddress({ ...data, id })
  return addAddress(apiData).then(res => ({ ok: true, data: res }))
}

export const addressDelete = (id) => {
  return deleteAddress(id).then(() => ({ ok: true }))
}

export const addressSetDefault = (id) => {
  return setDefaultAddress(id).then(() => ({ ok: true }))
}

export const addressGetDefault = () => {
  return getDefaultAddress().then(res => fromApiAddress(res))
}

