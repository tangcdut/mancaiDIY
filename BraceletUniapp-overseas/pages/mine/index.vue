<template>
  <view class="page">
    <!-- 顶部用户信息卡片 -->
    <view class="user-card">
      <view class="user-info">
        <image class="avatar" src="/static/tabbar/mine.png" mode="aspectFill" />
        <view class="text-info">
          <view class="nickname">Guest / 游客</view>
          <view class="welcome">Track Your Order / 查询订单</view>
        </view>
      </view>
    </view>

    <!-- 订单查询区域 (Guest) -->
    <view class="panel order-panel">
      <view class="panel-header">
        <text class="panel-title">Order Lookup / 订单查询</text>
      </view>
      <view class="order-lookup-form">
        <input class="lookup-input" v-model="lookupEmail" placeholder="Email / 邮箱" type="text" />
        <input class="lookup-input" v-model="lookupOrderNo" placeholder="Order No. / 订单号" type="text" />
        <button class="lookup-btn" @click="lookupOrder">Search / 查询</button>
      </view>
    </view>

    <!-- 功能卡片区域 -->
    <view class="func-grid">
      <!-- 地址管理 / Address - 粉色 -->
      <view class="func-card card-pink" @click="goAddress">
        <view class="card-icon-box">
          <Icon name="location" size="32rpx" color="#666" />
        </view>
        <text class="card-name">Address / 地址管理</text>
      </view>

      <!-- 联系客服 / Customer Service - 紫色 -->
      <view class="func-card card-purple" @click="contact">
        <view class="card-icon-box">
          <Icon name="service" size="32rpx" color="#666" />
        </view>
        <text class="card-name">Service / 联系客服</text>
      </view>

      <!-- 设置 / Settings - 蓝色 -->
      <view class="func-card card-blue" @click="goSetting">
        <view class="card-icon-box">
          <Icon name="settings" size="32rpx" color="#666" />
        </view>
        <text class="card-name">Settings / 设置</text>
      </view>

      <!-- 关于我们 / About - 绿色 -->
      <view class="func-card card-green" @click="about">
        <view class="card-icon-box">
          <Icon name="info" size="32rpx" color="#666" />
        </view>
        <text class="card-name">About / 关于我们</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Icon from '../../components/Icon.vue'

const lookupEmail = ref('')
const lookupOrderNo = ref('')

onMounted(() => {
  lookupEmail.value = uni.getStorageSync('guest_email') || ''
})

function lookupOrder() {
  const email = lookupEmail.value.trim()
  const orderNo = lookupOrderNo.value.trim()

  if (!email && !orderNo) {
    uni.showToast({ title: 'Please enter email or order number / 请输入邮箱或订单号', icon: 'none' })
    return
  }

  // Navigate to order detail page with lookup params
  const params = []
  if (email) params.push('email=' + encodeURIComponent(email))
  if (orderNo) params.push('orderNo=' + encodeURIComponent(orderNo))

  uni.navigateTo({ url: '/pages/order/list?' + params.join('&') })
}

function goAddress() { uni.navigateTo({ url: '/pages/address/list' }) }
function contact() { uni.navigateTo({ url: '/pages/customer-service/index' }) }
function goSetting() { uni.navigateTo({ url: '/pages/setting/index' }) }
function about() { uni.navigateTo({ url: '/pages/about/index' }) }
</script>

<style>
.page {
  padding: 24rpx;
  background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%);
  min-height: 100vh;
  box-sizing: border-box;
}

/* 顶部用户信息卡片 */
.user-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #ffe082, #ffd54f, #ffca28);
  border-radius: 24rpx;
  padding: 30rpx 40rpx;
  box-shadow: 0 8rpx 28rpx rgba(255,193,7,0.25), 0 2rpx 8rpx rgba(0,0,0,0.04);
  margin-bottom: 24rpx;
  border: 1rpx solid rgba(255,255,255,0.4);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.avatar {
  width: 110rpx;
  height: 110rpx;
  background: #fff;
  border-radius: 50%;
  border: 4rpx solid rgba(255,255,255,0.5);
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.08);
}

.text-info {
  display: flex;
  flex-direction: column;
}

.nickname {
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.welcome {
  font-size: 26rpx;
  color: #5f4500;
}

.edit-btn {
  margin: 0;
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  background: #fff;
  color: #333;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 28rpx;
  border: none;
  box-shadow: 0 2rpx 6rpx rgba(0,0,0,0.05);
}

/* 面板通用样式 */
.panel {
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.04), 0 1rpx 4rpx rgba(0,0,0,0.02);
  margin-bottom: 24rpx;
  padding: 24rpx;
  border: 1rpx solid rgba(0,0,0,0.03);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
  padding: 0 8rpx;
}

.panel-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #333;
}

.panel-more {
  display: flex;
  align-items: center;
  color: #999;
  font-size: 24rpx;
  gap: 4rpx;
}

.arrow {
  font-size: 24rpx;
}

/* 订单网格 */
.order-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
}

.order-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.icon-box {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.order-icon {
  font-size: 40rpx;
}

.bg-orange { background: #FFF4E5; color: #FF9800; }
.bg-green { background: #E8F5E9; color: #4CAF50; }
.bg-blue { background: #E3F2FD; color: #2196F3; }
.bg-yellow { background: #FFF8E1; color: #FFC107; }

.order-text {
  font-size: 24rpx;
  color: #333;
}

.badge {
  position: absolute;
  right: -6rpx;
  top: -6rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
  background: #ff4d4f;
  color: #fff;
  border-radius: 16rpx;
  font-size: 20rpx;
  padding: 0 8rpx;
  border: 2rpx solid #fff;
}

/* 功能卡片网格 */
.func-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
  margin-bottom: 30rpx;
}

.func-card {
  height: 140rpx;
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.04), 0 1rpx 4rpx rgba(0,0,0,0.02);
  border: 1rpx solid rgba(0,0,0,0.02);
  transition: transform 0.2s;
}

.func-card:active {
  transform: scale(0.97);
}

.card-pink { background: linear-gradient(135deg, #fff0f5, #ffe6eb); }
.card-purple { background: linear-gradient(135deg, #f8f0ff, #efdfff); }
.card-blue { background: linear-gradient(135deg, #f0f7ff, #e0efff); }
.card-green { background: linear-gradient(135deg, #f0fff4, #dfffe7); }

.card-icon-box {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.7);
  border-radius: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}

.card-icon {
  font-size: 32rpx;
}

.card-name {
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
}

/* 订单查询表单 */
.order-lookup-form {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.lookup-input {
  width: 100%;
  height: 76rpx;
  background: #f7f7f7;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
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
</style>
