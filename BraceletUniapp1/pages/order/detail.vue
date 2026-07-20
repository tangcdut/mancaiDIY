<template>
  <view class="page">
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="!detail" class="empty">订单不存在</view>
    <view v-else>
    <!-- 头部状态 -->
    <view class="card head">
      <view class="status-chip" :class="'s'+(detail.status||0)">{{ statusText(detail.status) }}</view>
      <view class="order-no">订单号 {{ detail.order_no || detail.orderNo || detail.id }}</view>
    </view>

    <!-- 物流信息 -->
    <view class="card tracking-card" v-if="detail.status === 2 && (detail.trackingNumber || detail.tracking_number)">
       <view class="tracking-row">
          <text class="label">快递单号</text>
          <text class="value">{{ detail.trackingNumber || detail.tracking_number }}</text>
          <view class="copy-tag" @click="copyText(detail.trackingNumber || detail.tracking_number)">复制</view>
       </view>
    </view>

    <!-- 地址信息 -->
    <view class="card address-card" v-if="detail.receiverName || detail.consignee || detail.name">
      <view class="icon-box"><Icon name="location" size="36rpx" color="#999" /></view>
      <view class="addr-info">
        <view class="addr-header">
          <text class="name">{{ detail.receiverName || detail.consignee || detail.name }}</text>
          <text class="phone">{{ detail.receiverPhone || detail.phone || detail.mobile }}</text>
        </view>
        <view class="addr-detail">
          {{ getFullAddress(detail) }}
        </view>
      </view>
    </view>

    <!-- DIY 订单展示逻辑 -->
    <template v-if="isDiyOrder(detail)">
      <!-- 1. DIY作品展示 (作为主商品) -->
      <view class="card product-card">
        <view class="row diy-main-row">
          <image :src="getDesignImage(detail)" mode="aspectFill" class="thumb" @click="previewDesign(getDesignImage(detail))" />
          <view class="meta">
            <view class="title">{{ getDisplayTitle(detail) }}</view>
            <view class="sub" v-if="detail.description">规格：{{ detail.description }}</view>
            <view class="price-row">
              <text class="price">¥{{ detail.amount }}</text>
              <text class="qty">x1</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 2. 使用材料列表 -->
      <view class="card materials-card" v-if="displayMaterials.length > 0">
        <view class="card-title">使用材料</view>
        <view class="row" v-for="i in displayMaterials" :key="i.product_id || i.id">
          <image v-if="getItemImage(i)" :src="getItemImage(i)" mode="aspectFill" class="material-thumb" />
          <view class="meta">
            <view class="title">{{ i.title || i.name || '未知材料' }}</view>
            <view class="price-row">
              <text class="price">¥{{ i.price }}</text>
              <text class="qty">x{{ i.quantity }}</text>
            </view>
          </view>
        </view>
      </view>
    </template>

    <!-- 普通订单展示商品列表 -->
    <view class="card items" v-else>
      <view class="row" v-for="i in (detail.items || [])" :key="i.product_id || i.id">
        <view class="thumb-box">
          <image v-if="getItemImage(i)" :src="getItemImage(i)" mode="aspectFill" class="thumb" />
        </view>
        <view class="meta">
          <view class="title">{{ i.title }}</view>
          <view class="sub">¥{{ i.price }} × {{ i.quantity }}</view>
        </view>
      </view>
    </view>

    <!-- 金额汇总 -->
    <view class="card summary">
      <view class="line">
        <text>金额</text>
        <text class="amount">¥{{ detail.amount }}</text>
      </view>
    </view>

    <!-- 客服提示 -->
    <view class="card service-tip">
      <view class="tip-text" v-if="detail.status === 4" style="color: #ff9800;">如等待时间过长请加客服了解</view>
      <view class="tip-text" v-else>联系客服具体了解商品详情</view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bar">
      <view class="total">应付：<text class="money">¥{{ detail.amount }}</text></view>
      <view class="btn-group">
        <button v-if="detail.status===0" class="btn cancel-btn" @click="handleCancelOrder">取消订单</button>
        <button v-if="detail.status===0" class="btn pay-btn" @click="pay">去支付</button>
        <button v-else-if="detail.status===2" class="btn pay-btn" @click="confirmReceipt">确认收货</button>
        <button v-else class="btn done-btn" disabled>{{ statusText(detail.status) }}</button>
      </view>
    </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { orderDetail, orderPay, completeOrder, checkPaymentStatus, cancelOrder } from '../../api/index.js'
import { STORAGE_TOKEN_KEY } from '../../config.js'
import { onLoad } from '@dcloudio/uni-app'
import { handleOrderPayment } from '../../utils/paymentHelper.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'
import Icon from '../../components/Icon.vue'

const detail = ref(null)
const loading = ref(true)
let oid = ''

// 计算显示的材料列表
const displayMaterials = computed(() => {
  if (!detail.value) return []
  // 1. 优先使用 materials 字段
  if (detail.value.materials && detail.value.materials.length > 0) return detail.value.materials
  
  // 2. 只有当 items 数量大于1时，才认为是包含了材料详情（排除只有一个主商品的情况）
  //    或者当 items[0] 明显不是主商品（比如没有diyImage）
  if (detail.value.items && detail.value.items.length > 0) {
    if (detail.value.items.length > 1) return detail.value.items
    
    // 如果只有一个item，检查是否是主商品
    const item = detail.value.items[0]
    // 简单的判断：如果item的价格等于订单总价，那它可能是主商品，不应该在材料里显示
    // 但为了安全，如果没materials字段，还是暂且认为items可能是材料（除非明确知道结构）
    // 鉴于POST数据结构 items=[DIYProduct], materials=[Beads...]
    // 如果后端没返回 materials，那可能就无法显示材料了。
    // 这里我们保守一点：如果有materials用materials，没有则为空（避免把主商品当材料显示）
    return []
  }
  return []
})

function statusText(s) {
  const m = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '退款审核中', 5: '退款中', 6: '已退款' }
  return m[s] ?? s
}

const paying = ref(false)
const confirming = ref(false)

function isDiyOrder(detail) {
  if (!detail) return false
  // 1. 有图片字段 (兼容多种命名)
  if (detail.diyImage || detail.designImage || detail.diy_image || detail.design_image || detail.productImage || detail.product_image) return true
  // 2. 订单号以DIY开头
  const orderNo = String(detail.orderNo || detail.order_no || detail.id || '')
  if (orderNo.toUpperCase().startsWith('DIY')) return true
  return false
}

function getDesignImage(detail) {
  if (!detail) return ''
  // 按照列表页的逻辑查找图片
  let img = detail.diyImage || detail.designImage || detail.diy_image || detail.design_image || detail.productImage || detail.product_image || detail.image || detail.img || detail.pic
  
  // 如果都没有，尝试查找 items 里的图片
  if (!img && detail.items && detail.items.length > 0) {
      img = detail.items[0].productImage || detail.items[0].image || detail.items[0].imageUrl || detail.items[0].pic
  }
  
  return resolveImageUrl(img ? String(img).trim() : '')
}

function getDisplayTitle(detail) {
  if (!detail) return 'DIY作品'
  
  // 优先使用 items 中的 title (通常包含用户自定义名称或商品名称)
  if (detail.items && detail.items.length > 0 && detail.items[0].title) {
    return detail.items[0].title
  }
  
  // 其次使用 diyName
  if (detail.diyName) return detail.diyName
  
  return 'DIY作品'
}

function previewDesign(url) {
  if (url) {
    uni.previewImage({ urls: [url] })
  }
}

function getItemImage(item) {
  if (!item) return ''
  const img = item.imageUrl || item.image || item.productImage || item.product_image || item.pic || ''
  return resolveImageUrl(img ? String(img).trim() : '')
}

function copyText(text) {
  if (!text) return
  uni.setClipboardData({
    data: String(text),
    success: () => {
      uni.showToast({ title: '复制成功', icon: 'none' })
    }
  })
}

function getFullAddress(d) {
  if (!d) return ''
  if (d.receiverAddress) return d.receiverAddress
  if (d.address) return d.address
  
  // 拼接地址，过滤掉无效值
  const province = d.receiverProvince || d.province
  const city = d.receiverCity || d.city
  const district = d.receiverDistrict || d.district
  const detail = d.receiverDetail || d.detailAddress || d.detail_address || d.addressDetail
  
  const parts = [province, city, district, detail]
  const address = parts.filter(p => p && p !== 'null' && p !== 'undefined').join('')
  
  return address || '暂无详细地址'
}

async function handleCancelOrder() {
  const orderId = detail.value.id || detail.value.orderId
  if (!orderId) return

  const res = await uni.showModal({
    title: '提示',
    content: '确定要取消该订单吗？',
    confirmColor: '#e54d42'
  })

  if (res.confirm) {
    uni.showLoading({ title: '处理中' })
    try {
      await cancelOrder(orderId)
      uni.showToast({ title: '取消成功', icon: 'success' })
      // 刷新数据
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    } catch (e) {
      uni.showToast({ title: '取消失败', icon: 'none' })
    } finally {
      uni.hideLoading()
    }
  }
}

async function confirmReceipt() {
  if (confirming.value) return
  
  try {
    const result = await uni.showModal({
      title: '确认收货',
      content: '确认已收到商品吗？',
      confirmText: '确认',
      cancelText: '取消'
    })
    
    if (!result.confirm) return
    
    confirming.value = true
    uni.showLoading({ title: '处理中...', mask: true })
    
    const orderId = detail.value.id || detail.value.orderId
    await completeOrder(orderId)
    
    uni.hideLoading()
    uni.showToast({ 
      title: '确认收货成功', 
      icon: 'success',
      duration: 2000
    })
    
    // 更新本地订单状态
    if (detail.value) {
      detail.value.status = 3
    }
    
    // 延迟跳转
    setTimeout(() => {
      uni.redirectTo({ 
        url: '/pages/order/list',
        fail: () => {
          uni.navigateBack()
        }
      })
    }, 2000)
    
  } catch (error) {
    uni.hideLoading()
    console.error('确认收货失败：', error)
    uni.showToast({ 
      title: error.message || '确认收货失败', 
      icon: 'none',
      duration: 2000
    })
  } finally {
    confirming.value = false
  }
}

async function pay() {
  if (paying.value) return
  
  paying.value = true
  
  await handleOrderPayment(detail.value, () => {
    // 支付成功回调
    
    // 更新本地订单状态
    if (detail.value) {
      detail.value.status = 1
      detail.value.payTime = new Date().toISOString()
    }
    
    // 延迟跳转，等待后端状态完全更新
    setTimeout(() => {
      // 跳转到订单列表页，会自动触发刷新
      uni.redirectTo({ 
        url: '/pages/order/list',
        fail: () => {
          // 如果跳转失败，尝试返回
          uni.navigateBack()
        }
      })
    }, 2000)
  }, () => {
    // 支付失败回调
  }).finally(() => {
    paying.value = false
  })
}

onLoad(async (options) => {
  const token = uni.getStorageSync(STORAGE_TOKEN_KEY)
  if (!token) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' })
    }, 800)
    return
  }

  // 获取订单ID，支持多种参数名
  oid = options?.id || options?.orderId || ''

  console.log('📋 接收到的参数:', options)
  console.log('📋 订单ID:', oid, '类型:', typeof oid)

  // 验证订单ID是否有效
  if (!oid && oid !== 0) {
    loading.value = false
    uni.showModal({
      title: '提示',
      content: '订单ID无效，请返回重试',
      showCancel: false,
      success: () => {
        uni.navigateBack()
      }
    })
    return
  }

  // 先显示加载状态，避免阻塞渲染
  loading.value = true

  // 延迟加载数据，让页面先渲染
  setTimeout(async () => {
    try {
      const orderIdNum = Number(oid)

      // 再次验证转换后的数字是否有效
      if (isNaN(orderIdNum) || orderIdNum <= 0) {
        throw new Error('订单ID格式错误')
      }

      const res = await orderDetail(orderIdNum)
      console.log('📦 订单详情API返回:', res)
      
      // 处理后端返回的数据格式
      if (res && res.order) {
        detail.value = res.order
        // 尝试从外层获取items补充到order中 (如果items在order外部)
        if (!detail.value.items && res.items) {
           detail.value.items = res.items
        }
      } else if (res && res.data) {
        detail.value = res.data
      } else if (res && !res.code && !res.msg) {
        // 假设res是数据本身
        detail.value = res
      } else {
        // 数据无效
        console.warn('收到无效的订单数据:', res)
        detail.value = null
        if (res.msg) {
          uni.showToast({ title: res.msg, icon: 'none' })
        }
      }
      
      // 兼容 orderItems (API文档中定义的名称) 到 items (页面使用的名称)
      if (detail.value && !detail.value.items && detail.value.orderItems) {
        detail.value.items = detail.value.orderItems
      }

      console.log('📋 订单详情:', detail.value)
    } catch (e) {
      console.error('❌ 加载订单失败', e)
      uni.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      loading.value = false
    }
  }, 50)
})
</script>

<style>
.page { padding: 24rpx; background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%); min-height: 100vh; box-sizing: border-box; padding-bottom: calc(140rpx + env(safe-area-inset-bottom)); }
.card { background: #ffffff; border-radius: 20rpx; padding: 24rpx; box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.04), 0 1rpx 4rpx rgba(0,0,0,0.02); border: 1rpx solid rgba(0,0,0,0.03); margin-bottom: 20rpx; }
.head { display: flex; justify-content: space-between; align-items: center; }
.status-chip { padding: 6rpx 20rpx; border-radius: 999rpx; font-size: 24rpx; font-weight: 500; }
.status-chip.s0 { background: #fff8e1; color: #ffb300; } /* 待支付 - 黄色 */
.status-chip.s1 { background: #e8f5e9; color: #2e7d32; } /* 已支付 - 绿色 */
.status-chip.s2 { background: #e3f2fd; color: #1565c0; } /* 已发货 - 蓝色 */
.status-chip.s3 { background: #f3e5f5; color: #7b1fa2; } /* 已完成 - 紫色 */

.tracking-card { padding: 20rpx 24rpx; }
.tracking-row { display: flex; align-items: center; font-size: 28rpx; color: #333; }
.tracking-row .label { color: #666; margin-right: 16rpx; }
.tracking-row .value { flex: 1; font-family: monospace; font-size: 30rpx; }
.tracking-row .copy-tag { color: #1565c0; margin-left: 16rpx; padding: 6rpx 20rpx; background: #e3f2fd; border-radius: 8rpx; font-size: 24rpx; }
.status-chip.s4 { background: #fff3e0; color: #a66a00; } /* 退款审核中 - 橙色 */
.order-no { color: #999; font-size: 24rpx; }

/* 地址卡片 */
.address-card { display: flex; align-items: flex-start; gap: 20rpx; }
.icon-box { margin-top: 4rpx; }
.icon { font-size: 36rpx; }
.addr-info { flex: 1; }
.addr-header { margin-bottom: 8rpx; display: flex; align-items: center; gap: 16rpx; }
.name { font-size: 30rpx; font-weight: 600; color: #333; }
.phone { font-size: 30rpx; font-weight: 600; color: #333; }
.addr-detail { font-size: 26rpx; color: #666; line-height: 1.4; }

/* 商品/DIY卡片 */
.product-card { padding: 20rpx; }
.materials-card { padding: 20rpx; }
.card-title { font-size: 28rpx; color: #666; margin-bottom: 16rpx; font-weight: 500; }
.row { display: flex; gap: 20rpx; align-items: flex-start; padding: 10rpx 0; }
.thumb { width: 140rpx; height: 140rpx; border-radius: 12rpx; background: #f5f5f5; flex-shrink: 0; }
.thumb-box { width: 140rpx; height: 140rpx; border-radius: 12rpx; background: #f5f5f5; flex-shrink: 0; overflow: hidden; }
.material-thumb { width: 100rpx; height: 100rpx; border-radius: 8rpx; background: #f5f5f5; flex-shrink: 0; }

.meta { flex: 1; display: flex; flex-direction: column; justify-content: space-between; min-height: 100rpx; }
.title { font-size: 28rpx; color: #333; line-height: 1.4; margin-bottom: 8rpx; }
.sub { font-size: 24rpx; color: #999; margin-bottom: 8rpx; }
.price-row { display: flex; justify-content: space-between; align-items: center; margin-top: auto; }
.price { font-size: 30rpx; color: #333; font-weight: 500; }
.qty { font-size: 26rpx; color: #999; }

/* 汇总信息 */
.summary { padding: 24rpx; }
.line { display: flex; justify-content: space-between; align-items: center; font-size: 28rpx; }
.amount { color: #e54d42; font-weight: 700; font-size: 32rpx; }

/* 客服提示 */
.service-tip { background: #fff; text-align: center; padding: 24rpx; display: flex; justify-content: center; align-items: center; }
.tip-text { color: #999; font-size: 24rpx; width: 100%; text-align: center; }

/* 底部栏 */
.bar { 
  position: fixed; left: 0; right: 0; bottom: 0; 
  background: #ffffff; 
  padding: 20rpx 30rpx calc(20rpx + env(safe-area-inset-bottom)); 
  display: flex; justify-content: space-between; align-items: center; 
  box-shadow: 0 -4rpx 16rpx rgba(0,0,0,0.06); 
  z-index: 100;
}
.total { color: #333; font-size: 28rpx; }
.money { color: #e54d42; font-weight: 700; font-size: 34rpx; margin-left: 8rpx; }
.btn-group { display: flex; gap: 20rpx; }
.btn {
  margin: 0;
  padding: 0 36rpx;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 36rpx;
  font-size: 28rpx;
  font-weight: 500;
  border: none;
}
.btn::after { border: none; }
.cancel-btn { background: #f5f5f5; color: #666; }
.pay-btn { background: #ffd84c; color: #333; font-weight: 600; }
.done-btn { background: #f5f5f5; color: #999; }

.loading, .empty { padding: 200rpx 0; text-align: center; color: #999; font-size: 28rpx; }
</style>
