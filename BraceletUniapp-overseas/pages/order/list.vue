<template>
  <view class="page">
    <!-- Order Lookup Form -->
    <view class="lookup-card">
      <view class="lookup-title">Track Your Order / 订单查询</view>
      <input class="lookup-input" v-model="lookupEmail" placeholder="Email Address / 联系邮箱" type="text" />
      <input class="lookup-input" v-model="lookupOrderNo" placeholder="Order Number / 订单号" type="text" />
      <button class="lookup-btn" @click="lookupOrder" :disabled="isLoading">
        {{ isLoading ? 'Searching... / 查询中...' : 'Search / 查询' }}
      </button>
    </view>

    <view v-if="!orders.length && !isLoading" class="empty">Enter email or order number to search / 输入邮箱或订单号查询</view>
    <view class="list" v-else>
      <view class="card" v-for="o in orders" :key="o.id || o.orderId" @click="goDetail(o.id || o.orderId)">
        <view class="head">
          <view class="no">Order No. / 订单号: {{ o.orderNo || o.order_no || o.id || o.orderId }}</view>
          <view class="head-right">
            <view v-if="o.status === 0 && o.countdownStr" class="countdown-tag">
              <text>Expires / 剩余: {{ o.countdownStr }}</text>
            </view>
            <view class="chip" :class="'s'+(o.status||0)">{{ o.statusText || statusText(o.status) }}</view>
          </view>
        </view>
        <view class="body">
          <view class="thumb">
            <image v-if="getOrderImage(o)" :src="getOrderImage(o)" mode="aspectFill" style="width:100%;height:100%;border-radius:12rpx;" />
          </view>
          <view class="meta">
            <view class="amount">${{ o.amount }}</view>
            <!-- 接口未返回数量时，默认不显示或显示1 -->
            <view class="sub" v-if="o.totalNum || o.productCount">{{ o.totalNum || o.productCount }} Items / 共 {{ o.totalNum || o.productCount }} 件</view>
          </view>
        </view>
        <view class="footer">
          <view class="tracking-row" v-if="o.status === 2 && (o.trackingNumber || o.tracking_number)" @click.stop>
            <text class="label">Tracking / 快递单号：</text>
            <text class="value">{{ o.trackingNumber || o.tracking_number }}</text>
            <view class="copy-tag" @click.stop="copyText(o.trackingNumber || o.tracking_number)">Copy / 复制</view>
          </view>
          <view class="actions">
            <view class="tip" v-if="o.status === 4" style="color: #ff9800;">Please contact customer service for help / 如等待时间过长请加客服了解</view>
            <view class="tip" v-else>Contact service for details / 联系客服了解商品详情</view>
            <!-- 状态 0:待支付, 1:已支付(待发货), 2:已发货, 3:已完成, 4:退款审核中, 5:退款中, 6:已退款 -->
            <view class="btn-cancel" v-if="o.status === 0" @click.stop="handleCancel(o)">Cancel / 取消订单</view>
            <view class="btn-pay" v-if="o.status === 0" @click.stop="handlePay(o)">Pay / 去支付</view>
            <!-- 仅未发货(已支付状态)显示退款按钮 -->
            <view class="btn-refund" v-if="(o.status === 1)" @click.stop="handleRefund(o)">Refund / 申请退款</view>
          </view>
        </view>
      </view>
    </view>
    <!-- <view class="loading-more" v-if="orders.length > 0">
      {{ hasMore ? '加载中...' : '没有更多了' }}
    </view> -->
  </view>
  
</template>

<script setup>
import { ref } from 'vue'
import { guestOrderDetail, guestOrderList } from '../../api/index.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'
import { onLoad } from '@dcloudio/uni-app'

const orders = ref([])
const isLoading = ref(false)
const lookupEmail = ref('')
const lookupOrderNo = ref('')

function getOrderImage(order) {
  if (!order) return ''
  let diyImg = order.productImage || order.product_image || order.diyImage || order.designImage || order.image || order.img || order.pic
  if (!diyImg && order.items && order.items.length > 0) {
    diyImg = order.items[0].productImage || order.items[0].image || order.items[0].imageUrl
  }
  return diyImg ? resolveImageUrl(String(diyImg).trim()) : ''
}

function goDetail(orderNo) {
  if (!orderNo) return
  uni.navigateTo({ url: '/pages/order/detail?orderNo=' + orderNo })
}

function copyText(text) {
  if (!text) return
  uni.setClipboardData({ data: String(text), success: () => uni.showToast({ title: 'Copied / 已复制', icon: 'none' }) })
}

function statusText(s) {
  const m = { 0: 'Pending / 待支付', 1: 'Paid / 已支付', 2: 'Shipped / 已发货', 3: 'Completed / 已完成', 4: 'Cancelled / 已取消' }
  return m[s] ?? s
}

async function lookupOrder() {
  const email = lookupEmail.value.trim()
  const orderNo = lookupOrderNo.value.trim()
  
  if (!email && !orderNo) {
    uni.showToast({ title: 'Please enter email or order number / 请输入邮箱或订单号', icon: 'none' })
    return
  }

  isLoading.value = true
  try {
    const res = await guestOrderList({ email, orderNo })
    const list = Array.isArray(res) ? res : (res && res.data ? res.data : [])
    orders.value = list
    if (!list.length) {
      uni.showToast({ title: 'No orders found / 未找到相关订单', icon: 'none' })
    }
  } catch (e) {
    console.error('Lookup order failed:', e)
    orders.value = []
    uni.showToast({ title: 'Query failed / 查询失败', icon: 'none' })
  } finally {
    isLoading.value = false
  }
}

onLoad((options) => {
  let shouldLookup = false
  if (options) {
    if (options.email) {
      lookupEmail.value = decodeURIComponent(options.email)
      shouldLookup = true
    }
    if (options.orderNo) {
      lookupOrderNo.value = decodeURIComponent(options.orderNo)
      shouldLookup = true
    }
  }
  
  // 自动回显存储的邮箱并自动查询列表
  if (!shouldLookup) {
    const savedEmail = uni.getStorageSync('guest_email')
    if (savedEmail) {
      lookupEmail.value = savedEmail
      shouldLookup = true
    }
  }

  if (shouldLookup) {
    lookupOrder()
  }
})
</script>

<style>
.page { padding: 24rpx; background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%); min-height: 100vh; box-sizing: border-box; }

.lookup-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.04);
}
.lookup-title { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 16rpx; }
.lookup-input {
  width: 100%;
  height: 76rpx;
  background: #f7f7f7;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
  margin-bottom: 16rpx;
}
.lookup-btn {
  width: 100%;
  height: 76rpx;
  line-height: 76rpx;
  background: linear-gradient(135deg, #ffd84c, #ffca28);
  color: #333;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 38rpx;
  border: none;
}

.empty { padding: 120rpx 24rpx; text-align: center; color: #999; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.card { background: #ffffff; border-radius: 20rpx; padding: 16rpx; box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.04), 0 1rpx 4rpx rgba(0,0,0,0.02); border: 1rpx solid rgba(0,0,0,0.03); }
.head { display: flex; justify-content: space-between; align-items: center; }
.no { color: #666; font-size: 24rpx; }
.chip { padding: 4rpx 14rpx; border-radius: 999rpx; font-size: 22rpx; }
.chip.s0 { background: #fff3e0; color: #a66a00; }
.chip.s1 { background: #e8f5e9; color: #2e7d32; }
.chip.s2 { background: #e3f2fd; color: #1565c0; }
.chip.s3 { background: #ede7f6; color: #5e35b1; }
.body { display: flex; gap: 16rpx; margin-top: 12rpx; }
.thumb { width: 140rpx; height: 140rpx; background: #e9eef3; border-radius: 12rpx; }
.meta { display: flex; flex-direction: column; justify-content: center; }
.amount { color: #e54d42; font-size: 32rpx; font-weight: 700; }
.sub { color: #888; margin-top: 6rpx; }
.footer { margin-top: 12rpx; padding-top: 12rpx; border-top: 1rpx solid #f0f0f0; }
.actions { display: flex; justify-content: space-between; align-items: center; }
.tip { text-align: right; color: #999; font-size: 22rpx; }
.tracking-row {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #666;
  margin-bottom: 12rpx;
  padding-bottom: 12rpx;
  border-bottom: 1rpx dashed #f0f0f0;
}
.tracking-row .value {
  color: #333;
  margin-left: 8rpx;
  font-family: monospace;
  flex: 1;
}
.tracking-row .copy-tag {
  color: #1565c0;
  margin-left: 16rpx;
  padding: 4rpx 12rpx;
  background: #e3f2fd;
  border-radius: 8rpx;
  font-size: 20rpx;
}
.btn-cancel {
  font-size: 24rpx;
  color: #999;
  border: 1rpx solid #ddd;
  padding: 8rpx 20rpx;
  border-radius: 30rpx;
  background: #fff;
  margin-right: 16rpx;
}
.btn-refund { 
  font-size: 24rpx; 
  color: #666; 
  border: 1rpx solid #ddd; 
  padding: 8rpx 20rpx; 
  border-radius: 30rpx;
  background: #fff;
}
.btn-pay {
  font-size: 24rpx;
  color: #333;
  border: 1rpx solid #ffd84c;
  padding: 8rpx 28rpx;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #ffd84c, #ffca28);
  font-weight: 600;
  margin-left: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(255,193,7,0.25);
}
.head-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.countdown-tag {
  color: #ff4d4f;
  font-size: 22rpx;
  font-weight: bold;
  background: rgba(255, 77, 79, 0.1);
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
}
</style>
