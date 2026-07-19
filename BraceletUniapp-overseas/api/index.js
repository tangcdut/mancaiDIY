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
  updateAddress,
  deleteAddress,
  setDefaultAddress,
  getDefaultAddress
} from './api.js'

export {
  // 登录 (overseas: no-op stub)
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

  // 地址管理
  addAddress,
  updateAddress,
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

// 海外版：永远返回 true，无需微信登录
export const isLoggedIn = () => {
  return true
}

// 海外版：无需登录检查
export const requireLogin = () => {
  return true
}

// 退出登录（海外版：清除本地访客信息）
export const logout = async () => {
  try {
    uni.removeStorageSync(STORAGE_USER_KEY)
    uni.removeStorageSync(STORAGE_TOKEN_KEY)
    uni.removeStorageSync('guest_info')
    return promise({ ok: true, message: '退出登录成功' })
  } catch (e) {
    console.error('退出登录失败', e)
    return promise({ ok: false, message: '退出登录失败' })
  }
}

// 海外版：设置访客信息
export const setGuestInfo = (email, name) => {
  try {
    uni.setStorageSync('guest_info', { email, name })
  } catch (e) {
    console.error('保存访客信息失败:', e)
  }
}

// 海外版：获取访客信息
export const getGuestInfo = () => {
  try {
    return uni.getStorageSync('guest_info') || null
  } catch (e) {
    return null
  }
}

// ==================== DIY设计模块（从后端API获取）====================
// 已迁移到 api.js，这里直接从 api.js 导出
export {
  designCategoryList,
  designProductList,
  uploadFile,
  createDiyOrder,
  designOrderCreate,
  // 海外版
  createGuestOrder,
  paypalPayment,
  checkGuestPaymentStatus,
  guestOrderDetail,
  guestOrderList
} from './api.js'

// ==================== 地址管理适配层 ====================

// 后端 -> 前端 字段映射 (overseas international format)
const fromApiAddress = (addr) => {
  if (!addr) return null

  // Parse zip from detailAddress if stored with delimiter '|'
  let zip = ''
  let address = addr.detailAddress || addr.receiverAddress || addr.address || ''
  if (address.includes('|')) {
    const idx = address.indexOf('|')
    zip = address.substring(0, idx)
    address = address.substring(idx + 1)
  }

  return {
    id: addr.id,
    name: addr.consignee || addr.receiverName || '',
    phone: addr.phone || addr.receiverPhone || '',
    country: addr.district || addr.receiverCountry || addr.country || '',
    state: addr.province || addr.receiverState || addr.state || '',
    city: addr.city || addr.receiverCity || '',
    zip: zip || addr.receiverZip || addr.zip || '',
    address: address,
    detail: address,
    province: addr.province || '',
    district: addr.district || '',
    isDefault: addr.isDefault // 0 or 1
  }
}

// 前端 -> 后端 字段映射 (overseas international format)
const toApiAddress = (data) => {
  // Pack zip and detailed address using '|' delimiter
  const packedAddress = (data.zip ? data.zip.trim() : '') + '|' + (data.address || data.detail || '').trim()

  return {
    id: data.id,
    consignee: data.name,
    receiverName: data.name,
    phone: data.phone,
    receiverPhone: data.phone,
    country: data.country,
    state: data.state,
    city: data.city,
    zip: data.zip,
    detailAddress: packedAddress,
    receiverAddress: packedAddress,
    province: data.state || '',   // State maps to province
    district: data.country || '', // Country maps to district
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
  const apiData = toApiAddress({ ...data, id })
  return updateAddress(apiData).then(res => ({ ok: true, data: res }))
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

