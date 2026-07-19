import request from './request'

// 管理员登录
export function adminLogin (data) {
  return request({
    url: '/admin/login',
    method: 'post',
    data
  })
}

// 获取轮播图列表
export function getBannerList () {
  return request({
    url: '/admin/banner',
    method: 'get'
  })
}

// 添加轮播图
export function addBanner (data) {
  return request({
    url: '/admin/banner/add',
    method: 'post',
    data
  })
}

// 更新轮播图
export function updateBanner (data) {
  return request({
    url: '/admin/banner/update',
    method: 'post',
    data
  })
}

// 删除轮播图
export function deleteBanner (id) {
  return request({
    url: `/admin/banner/delete/${id}`,
    method: 'delete'
  })
}

// 获取商品分类列表（分页）
export function getCategoryList (params) {
  return request({
    url: '/admin/category/page',
    method: 'get',
    params
  })
}

// 获取所有商品分类列表
export function getAllCategories () {
  return request({
    url: '/admin/category/list',
    method: 'get'
  })
}

// 添加商品分类
export function addCategory (data) {
  return request({
    url: '/admin/category/add',
    method: 'post',
    data
  })
}

// 更新商品分类
export function updateCategory (data) {
  return request({
    url: '/admin/category/update',
    method: 'post',
    data
  })
}

// 删除商品分类
export function deleteCategory (id) {
  return request({
    url: '/admin/category/delete',
    method: 'post',
    data: { id }
  })
}

// 获取商品列表
export function getProductList (params) {
  return request({
    url: '/admin/product/list',
    method: 'get',
    params
  })
}

// 获取商品详情
export function getProductDetail (id) {
  return request({
    url: '/admin/product/detail',
    method: 'get',
    params: { id }
  })
}

// 添加商品
export function addProduct (data) {
  return request({
    url: '/admin/product/add',
    method: 'post',
    data
  })
}

// 更新商品
export function updateProduct (data) {
  return request({
    url: '/admin/product/update',
    method: 'post',
    data
  })
}

// 删除商品
export function deleteProduct (id) {
  return request({
    url: '/admin/product/delete',
    method: 'post',
    params: { id }
  })
}

// 获取订单列表（分页，支持按状态/订单号筛选）
export function getOrderList (params = {}) {
  return request({
    url: '/admin/order/list',
    method: 'get',
    params
  })
}

// 获取订单详情
export function getOrderDetail (orderId) {
  return request({
    url: '/admin/order/detail',
    method: 'get',
    params: { orderId }
  })
}

// 更新订单状态
export function updateOrderStatus (data) {
  return request({
    url: '/admin/order/updateStatus',
    method: 'post',
    data
  })
}

// 管理员审核退款（执行微信退款）
export function adminRefund (orderId, adminPhone) {
  return request({
    url: '/admin/order/adminRefund',
    method: 'post',
    params: { orderId, adminPhone }
  })
}

// 获取退款配置（是否允许已完成订单退款）
export function getRefundConfig () {
  return request({
    url: '/admin/order/refundConfig',
    method: 'get'
  })
}

// 上传客服二维码
export function uploadCustomerServiceQR (file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/admin/common/upload/customer-service-qr',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取客服二维码
export function getCustomerServiceQR () {
  return request({
    url: '/admin/common/customer-service-qr',
    method: 'get'
  })
}

// 删除客服二维码
export function deleteCustomerServiceQR () {
  return request({
    url: '/admin/common/customer-service-qr',
    method: 'delete'
  })
}

// ============ DIY材料管理 ============

// 获取DIY材料列表
export function getDiyMaterialList (params) {
  return request({
    url: '/admin/diy/material/list',
    method: 'get',
    params
  })
}

// 获取DIY材料详情
export function getDiyMaterialDetail (id) {
  return request({
    url: `/admin/diy/material/${id}`,
    method: 'get'
  })
}

// 添加DIY材料
export function addDiyMaterial (data) {
  return request({
    url: '/admin/diy/material',
    method: 'post',
    data
  })
}

// 更新DIY材料
export function updateDiyMaterial (data) {
  return request({
    url: '/admin/diy/material',
    method: 'put',
    data
  })
}

// 删除DIY材料
export function deleteDiyMaterial (id) {
  return request({
    url: `/admin/diy/material/${id}`,
    method: 'delete'
  })
}

// 批量更新DIY材料状态
export function updateDiyMaterialStatus (status, ids) {
  return request({
    url: `/admin/diy/material/status/${status}`,
    method: 'post',
    data: ids
  })
}

// 获取色系列表
export function getColorSeriesList () {
  return request({
    url: '/admin/diy/material/colorSeries/list',
    method: 'get'
  })
}

// ============ 海外价格管理 ============

export function saveOverseasPrice (data) {
  return request({
    url: '/admin/product/overseas-price/save',
    method: 'post',
    data
  })
}

export function getOverseasPrice (productId) {
  return request({
    url: '/admin/product/overseas-price',
    method: 'get',
    params: { productId }
  })
}

export function getOverseasOrderList (params) {
  return request({
    url: '/admin/order/overseas/list',
    method: 'get',
    params
  })
}

export function updateOverseasOrderStatus (data) {
  return request({
    url: '/admin/order/overseas/updateStatus',
    method: 'post',
    data
  })
}
