<template>
  <view class="page">
    <!-- 顶部黄色背景区 -->
    <view class="user-card">
      <view class="user-info">
        <image v-if="user && user.avatarUrl" class="avatar" :src="user.avatarUrl" mode="aspectFill" />
        <image v-else class="avatar" src="/static/tabbar/mine.png" mode="aspectFill" />
        
        <view class="text-info">
          <view class="nickname">{{ user ? user.nickName : '微信用户' }}</view>
          <view class="welcome">{{ user ? '欢迎回来' : '点击登录' }}</view>
        </view>
      </view>
      
      <button v-if="user" class="edit-btn" @click="openEditProfile">编辑资料</button>
      <button v-else class="edit-btn" @click="handleLogin">去登录</button>
    </view>

    <!-- 订单区域 -->
    <view class="panel order-panel">
      <view class="panel-header" @click="goAllOrders">
        <text class="panel-title">我的订单</text>
        <view class="panel-more">
          <text>全部订单</text>
          <text class="arrow"> → </text>
        </view>
      </view>
      
      <view class="order-grid">
        <view class="order-item" @click.stop="goOrders(0)">
          <view class="icon-box bg-orange">
            <Icon name="orderPending" size="40rpx" color="#FF9800" />
            <view class="badge" v-if="count.s0">{{ count.s0 }}</view>
          </view>
          <text class="order-text">待支付</text>
        </view>

        <view class="order-item" @click.stop="goOrders(1)">
          <view class="icon-box bg-green">
            <Icon name="orderPaid" size="40rpx" color="#4CAF50" />
            <view class="badge" v-if="count.s1">{{ count.s1 }}</view>
          </view>
          <text class="order-text">已支付</text>
        </view>

        <view class="order-item" @click.stop="goOrders(2)">
          <view class="icon-box bg-blue">
            <Icon name="orderShipped" size="40rpx" color="#2196F3" />
            <view class="badge" v-if="count.s2">{{ count.s2 }}</view>
          </view>
          <text class="order-text">已发货</text>
        </view>

        <view class="order-item" @click.stop="goOrders(3)">
          <view class="icon-box bg-yellow">
            <Icon name="orderCompleted" size="40rpx" color="#FFC107" />
            <view class="badge" v-if="count.s3">{{ count.s3 }}</view>
          </view>
          <text class="order-text">已完成</text>
        </view>
      </view>
    </view>

    <!-- 功能卡片区域 -->
    <view class="func-grid">
      <!-- 地址管理 - 粉色 -->
      <view class="func-card card-pink" @click="goAddress">
        <view class="card-icon-box">
          <Icon name="location" size="32rpx" color="#666" />
        </view>
        <text class="card-name">地址管理</text>
      </view>

      <!-- 联系客服 - 紫色 -->
      <view class="func-card card-purple" @click="contact">
        <view class="card-icon-box">
          <Icon name="service" size="32rpx" color="#666" />
        </view>
        <text class="card-name">联系客服</text>
      </view>

      <!-- 设置 - 蓝色 -->
      <view class="func-card card-blue" @click="goSetting">
        <view class="card-icon-box">
          <Icon name="settings" size="32rpx" color="#666" />
        </view>
        <text class="card-name">设置</text>
      </view>

      <!-- 关于我们 - 绿色 -->
      <view class="func-card card-green" @click="about">
        <view class="card-icon-box">
          <Icon name="info" size="32rpx" color="#666" />
        </view>
        <text class="card-name">关于我们</text>
      </view>
    </view>

    <!-- 退出登录按钮 -->
    <view class="logout-section" v-if="user">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>

    <view v-if="showQR" class="mask" @click="showQR=false">
      <view class="qr-modal" @click.stop>
        <view class="qr-title">客服微信</view>
        <image class="qr" :src="qrUrl" mode="widthFix" show-menu-by-longpress="true" @click="previewQR" />
        <view class="qr-tips">长按识别二维码添加</view>
        <button class="qr-close" @click="showQR=false">关闭</button>
      </view>
    </view>

    <!-- 编辑资料弹窗 -->
    <view v-if="showEditProfile" class="mask" @click="showEditProfile=false">
      <view class="edit-modal" @click.stop>
        <view class="modal-title">编辑资料</view>
        
        <view class="form-item">
          <view class="label">头像</view>
          <button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
            <image class="avatar-preview" :src="tempAvatarUrl || user.avatarUrl || '/static/tabbar/mine.png'" mode="aspectFill" />
            <view class="avatar-tip">点击选择头像</view>
          </button>
        </view>
        
        <view class="form-item">
          <view class="label">昵称</view>
          <input 
            class="nickname-input" 
            type="nickname" 
            v-model="tempNickName" 
            placeholder="请输入昵称" 
            :maxlength="20"
          />
        </view>
        
        <view class="modal-btns">
          <button class="cancel-btn" @click="cancelEdit">取消</button>
          <button class="save-btn" @click="saveProfile">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { orderList, userGet, logout, isLoggedIn } from '../../api/index.js'
import Icon from '../../components/Icon.vue'

const count = ref({ s0: 0, s1: 0, s2: 0, s3: 0 })
const showQR = ref(false)
const qrUrl = '/static/CustomerService/714966e4f87775b79a26b9002c0606d1.jpg'
const user = ref(null)
const showEditProfile = ref(false)
const tempAvatarUrl = ref('')
const tempNickName = ref('')

async function loadOrders() {
  if (!isLoggedIn()) {
    count.value = { s0: 0, s1: 0, s2: 0, s3: 0 }
    return
  }

  try {
    console.log('🔄 开始加载订单数据...')
    
    // 查询所有状态的订单
    const res = await orderList({ page: 1, size: 100 })
    console.log('📦 订单API返回:', res)
    
    // 处理后端返回的数据格式
    let orders = []
    if (res && res.orders) {
      // {orders: Array, page: number, size: number, total: number} 格式
      orders = res.orders
    } else if (res && res.records) {
      // PageHelper分页格式
      orders = res.records
    } else if (Array.isArray(res)) {
      orders = res
    } else if (res && res.data) {
      if (res.data.orders) {
        orders = res.data.orders
      } else {
        orders = Array.isArray(res.data) ? res.data : (res.data.records || [])
      }
    }
    
    console.log('📋 订单列表总数:', orders.length)
    
    // 统计各状态订单数量
    const newCount = {
      s0: orders.filter(o => o.status === 0).length,
      s1: orders.filter(o => o.status === 1 || o.status === 4).length, // 已支付 + 退款审核中
      s2: orders.filter(o => o.status === 2).length,
      s3: orders.filter(o => o.status === 3).length,
    }
    
    console.log('📊 订单统计 - 待支付:', newCount.s0, '已支付:', newCount.s1, '已发货:', newCount.s2, '已完成:', newCount.s3)
    
    count.value = newCount
    
    console.log('✅ 订单数据加载完成')
  } catch (e) {
    console.error('❌ 加载订单失败:', e)
  }
}

onMounted(() => { 
  user.value = userGet()
  loadOrders() 
})

// 每次显示页面时重新加载订单
onShow(() => {
  console.log('我的页面显示，重新加载订单')
  user.value = userGet() // 确保用户信息也是最新的
  // 增加小延迟，确保后端订单状态已更新
  setTimeout(() => {
    loadOrders()
  }, 300)
})

function handleLogin() {
  // 跳转到首页进行登录（首页有完整的微信登录流程）
  uni.reLaunch({ url: '/pages/index/index?login=1' })
}

function goAllOrders() {
  if (!isLoggedIn()) return handleLogin()
  uni.navigateTo({ url: '/pages/order/list' })
}
function goOrders(status) {
  if (!isLoggedIn()) return handleLogin()
  uni.navigateTo({ url: '/pages/order/list?status=' + status })
}
function goAddress() {
  if (!isLoggedIn()) return handleLogin()
  uni.navigateTo({ url: '/pages/address/list' }) 
}
function contact() { uni.navigateTo({ url: '/pages/customer-service/index' }) }
function goSetting() { uni.navigateTo({ url: '/pages/setting/index' }) }
function about() { uni.navigateTo({ url: '/pages/about/index' }) }

async function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    confirmText: '退出',
    confirmColor: '#ff6600',
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '正在退出...' })
        try {
          await logout()
          user.value = null
          uni.hideLoading()
          uni.showToast({ title: '已退出登录', icon: 'success' })
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/index/index' })
          }, 1500)
        } catch (e) {
          uni.hideLoading()
          uni.showToast({ title: '退出失败', icon: 'none' })
        }
      }
    }
  })
}
function previewQR() {
  try { uni.previewImage({ urls: [qrUrl] }) } catch (e) { uni.showToast({ title: '请先在 /static 放入二维码图片', icon: 'none' }) }
}

// 打开编辑资料弹窗
function openEditProfile() {
  // 预填充当前昵称
  tempNickName.value = user.value?.nickName || ''
  tempAvatarUrl.value = ''
  showEditProfile.value = true
}

// 选择头像
function onChooseAvatar(e) {
  console.log('选择头像:', e)
  const { avatarUrl } = e.detail
  if (avatarUrl) {
    tempAvatarUrl.value = avatarUrl
    console.log('临时头像已设置:', avatarUrl)
  }
}

// 取消编辑
function cancelEdit() {
  showEditProfile.value = false
  tempAvatarUrl.value = ''
  tempNickName.value = ''
}

// 保存资料
function saveProfile() {
  const newAvatar = tempAvatarUrl.value || user.value.avatarUrl
  const newNickName = tempNickName.value.trim() || user.value.nickName
  
  if (!newNickName || newNickName.length === 0) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }
  
  console.log('保存资料: 头像=', newAvatar, ', 昵称=', newNickName)
  
  // 更新用户信息
  const updatedUser = {
    ...user.value,
    avatarUrl: newAvatar,
    nickName: newNickName
  }
  
  try {
    uni.setStorageSync('user', updatedUser)
    user.value = updatedUser
    console.log('资料保存成功:', updatedUser)
    uni.showToast({ title: '保存成功', icon: 'success' })
    showEditProfile.value = false
    tempAvatarUrl.value = ''
    tempNickName.value = ''
  } catch (e) {
    console.error('保存失败:', e)
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}
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

/* 退出登录按钮 */
.logout-section {
  padding: 0 10rpx;
  margin-bottom: 40rpx;
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #fff;
  color: #e74c3c;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 16rpx;
  border: none;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
  border: 1rpx solid rgba(231,76,60,0.1);
}

/* 弹窗样式 */
.mask { position: fixed; left: 0; right: 0; top: 0; bottom: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.qr-modal { width: 80%; background: #fff; border-radius: 20rpx; padding: 24rpx; box-shadow: 0 10rpx 24rpx rgba(0,0,0,0.2); display: flex; flex-direction: column; align-items: center; }
.qr-title { font-size: 30rpx; font-weight: 700; color: #333; margin-bottom: 10rpx; }
.qr { width: 520rpx; height: 520rpx; background: #e9eef3; border-radius: 12rpx; }
.qr-tips { color: #666; margin: 12rpx 0 4rpx; }
.qr-close { margin-top: 6rpx; background: #ffd84c; color: #333; border-radius: 999rpx; padding: 0 28rpx; height: 72rpx; line-height: 72rpx; font-weight: 600; }

.edit-modal { width: 85%; background: #fff; border-radius: 20rpx; padding: 32rpx 28rpx; box-shadow: 0 10rpx 24rpx rgba(0,0,0,0.2); }
.modal-title { font-size: 32rpx; font-weight: 700; color: #333; text-align: center; margin-bottom: 28rpx; }
.form-item { margin-bottom: 24rpx; }
.form-item .label { font-size: 26rpx; color: #666; margin-bottom: 12rpx; }
.avatar-btn { width: 100%; background: #f7f7f7; border-radius: 12rpx; padding: 20rpx; display: flex; flex-direction: column; align-items: center; gap: 12rpx; border: 1px dashed #ddd; }
.avatar-preview { width: 120rpx; height: 120rpx; border-radius: 50%; background: #e0e0e0; }
.avatar-tip { font-size: 24rpx; color: #999; }
.nickname-input { width: 100%; height: 80rpx; line-height: 80rpx; background: #f7f7f7; border-radius: 12rpx; padding: 0 20rpx; font-size: 28rpx; color: #333; box-sizing: border-box; }
.modal-btns { display: flex; gap: 16rpx; margin-top: 32rpx; }
.cancel-btn, .save-btn { flex: 1; height: 80rpx; line-height: 80rpx; border-radius: 12rpx; font-size: 28rpx; font-weight: 600; border: none; }
.cancel-btn { background: #f0f0f0; color: #666; }
.save-btn { background: #ffd84c; color: #333; }
</style>
