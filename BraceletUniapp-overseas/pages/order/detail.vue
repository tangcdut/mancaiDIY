<template>
  <view class="page">
    <view v-if="loading" class="loading">加载中... / Loading...</view>
    <view v-else-if="!detail" class="empty">订单不存在 / Order not found</view>
    <view v-else>
    <!-- 头部状态 -->
    <view class="card head">
      <view class="status-chip" :class="'s'+(detail.status||0)">{{ statusText(detail.status) }}</view>
      <view class="order-no">Order No. / 订单号: {{ detail.order_no || detail.orderNo || detail.id }}</view>
    </view>

    <!-- 物流信息 -->
    <view class="card tracking-card" v-if="detail.status === 2 && (detail.trackingNumber || detail.tracking_number)">
       <view class="tracking-row">
          <text class="label">Tracking / 快递单号</text>
          <text class="value">{{ detail.trackingNumber || detail.tracking_number }}</text>
          <view class="copy-tag" @click="copyText(detail.trackingNumber || detail.tracking_number)">Copy / 复制</view>
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
            <view class="sub" v-if="detail.description">Specs / 规格: {{ detail.description }}</view>
            <view class="price-row">
              <text class="price">${{ detail.amount }}</text>
              <text class="qty">x1</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 2. 使用材料列表 -->
      <view class="card materials-card" v-if="displayMaterials.length > 0">
        <view class="card-title">Materials / 使用材料</view>
        <view class="row" v-for="i in displayMaterials" :key="i.product_id || i.id">
          <image v-if="getItemImage(i)" :src="getItemImage(i)" mode="aspectFill" class="material-thumb" />
          <view class="meta">
            <view class="title">{{ i.title || i.name || '未知材料 / Unknown material' }}</view>
            <view class="price-row">
              <text class="price">${{ i.price }}</text>
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
          <view class="sub">${{ i.price }} × {{ i.quantity }}</view>
        </view>
      </view>
    </view>

    <!-- 金额汇总 -->
    <view class="card summary">
      <view class="line">
        <text>金额 / Amount</text>
        <text class="amount">${{ detail.amount }}</text>
      </view>
    </view>

    <!-- 客服提示 -->
    <view class="card service-tip">
      <view class="tip-text" v-if="detail.status === 4" style="color: #ff9800;">Please contact customer service for help / 如等待时间过长请加客服了解</view>
      <view class="tip-text" v-else>Contact service for details / 联系客服了解商品详情</view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bar">
      <view class="total">Due / 应付：<text class="money">${{ detail.amount }}</text></view>
      <view class="btn-group">
        <button v-if="detail.status===0" class="btn cancel-btn" @click="handleCancelOrder">Cancel / 取消订单</button>
        <button v-if="detail.status===0" class="btn pay-btn" @click="pay">Pay with PayPal</button>
        <button v-else-if="detail.status===2" class="btn pay-btn" @click="confirmReceipt">Confirm / 确认收货</button>
        <button v-else class="btn done-btn" disabled>{{ statusText(detail.status) }}</button>
      </view>
    </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { guestOrderDetail, paypalPayment, checkGuestPaymentStatus, orderDetail } from '../../api/index.js'
import { onLoad } from '@dcloudio/uni-app'
import { resolveImageUrl } from '../../utils/imageHelper.js'
import Icon from '../../components/Icon.vue'

const detail = ref(null)
const loading = ref(true)
let queryOrderNo = ''

const displayMaterials = computed(() => {
  if (!detail.value) return []
  if (detail.value.materials && detail.value.materials.length > 0) return detail.value.materials
  if (detail.value.items && detail.value.items.length > 0) {
    if (detail.value.items.length > 1) return detail.value.items
    return []
  }
  return []
})

function statusText(s) {
  const m = { 0: 'Pending / 待支付', 1: 'Paid / 已支付', 2: 'Shipped / 已发货', 3: 'Completed / 已完成', 4: 'Cancelled / 已取消' }
  return m[s] ?? s
}

const paying = ref(false)

function isDiyOrder(detail) {
  if (!detail) return false
  if (detail.diyImage || detail.designImage || detail.diy_image || detail.productImage) return true
  const orderNo = String(detail.orderNo || detail.order_no || detail.id || '')
  if (orderNo.toUpperCase().startsWith('DIY')) return true
  return false
}

function getDesignImage(detail) {
  if (!detail) return ''
  let img = detail.diyImage || detail.designImage || detail.diy_image || detail.productImage || detail.image || detail.img
  if (!img && detail.items && detail.items.length > 0) {
    img = detail.items[0].productImage || detail.items[0].image || detail.items[0].imageUrl
  }
  return resolveImageUrl(img ? String(img).trim() : '')
}

function getDisplayTitle(detail) {
  if (!detail) return 'DIY Bracelet'
  if (detail.items && detail.items.length > 0 && detail.items[0].title) return detail.items[0].title
  if (detail.diyName) return detail.diyName
  return 'DIY Bracelet'
}

function previewDesign(url) {
  if (url) uni.previewImage({ urls: [url] })
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
    success: () => uni.showToast({ title: 'Copied / 已复制', icon: 'none' })
  })
}

function getFullAddress(d) {
  if (!d) return ''
  if (d.receiverAddress) return d.receiverAddress
  if (d.address) return d.address
  const parts = [d.receiverProvince || d.province, d.receiverCity || d.city, d.receiverDistrict || d.district, d.receiverDetail || d.detailAddress]
  return parts.filter(p => p && p !== 'null' && p !== 'undefined').join(', ') || '-'
}

// PayPal Payment
async function pay() {
  if (paying.value) return
  paying.value = true

  try {
    uni.showLoading({ title: 'Creating PayPal payment...' })

    const orderNo = detail.value.orderNo || detail.value.order_no || queryOrderNo
    if (!orderNo) {
      uni.showToast({ title: 'Order not found / 订单号缺失', icon: 'none' })
      return
    }

    const res = await paypalPayment(orderNo, 'USD')
    console.log('PayPal payment response:', res)

    uni.hideLoading()

    if (res && res.approvalUrl) {
      // Redirect to PayPal
      window.location.href = res.approvalUrl
    } else {
      uni.showToast({ title: 'PayPal payment creation failed / 支付创建失败', icon: 'none' })
    }
  } catch (e) {
    uni.hideLoading()
    console.error('PayPal payment error:', e)
    uni.showToast({ title: 'Payment failed / 支付失败', icon: 'none' })
  } finally {
    paying.value = false
  }
}

onLoad(async (options) => {
  queryOrderNo = options?.orderNo || options?.id || ''

  if (!queryOrderNo) {
    loading.value = false
    uni.showToast({ title: 'No order specified / 未指定订单', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1500)
    return
  }

  loading.value = true

  setTimeout(async () => {
    try {
      // Try guest order API first, then fall back to authenticated API
      let res
      try {
        res = await guestOrderDetail(queryOrderNo)
      } catch (guestErr) {
        // Fallback: try authenticated API
        const numId = Number(queryOrderNo)
        if (!isNaN(numId) && numId > 0) {
          res = await orderDetail(numId)
        } else {
          throw guestErr
        }
      }

      if (res && res.order) {
        detail.value = res.order
        if (!detail.value.items && res.items) detail.value.items = res.items
      } else if (res && res.data) {
        detail.value = res.data
      } else if (res && !res.code && !res.msg) {
        detail.value = res
      } else {
        detail.value = null
      }

      if (detail.value && !detail.value.items && detail.value.orderItems) {
        detail.value.items = detail.value.orderItems
      }
    } catch (e) {
      console.error('Load order failed', e)
      detail.value = null
      uni.showToast({ title: 'Order not found / 订单未找到', icon: 'none' })
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
