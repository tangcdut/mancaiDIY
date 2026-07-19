<template>
  <view class="page">
    <!-- 账号信息 -->
    <view class="section">
      <view class="section-title">Account / 账号信息</view>
      <view class="item" v-if="user">
        <view class="item-label">Nickname / 用户昵称</view>
        <view class="item-value">{{ user.nickName || 'Not Set / 未设置' }}</view>
      </view>
      <view class="item" v-if="user">
        <view class="item-label">User ID / 用户ID</view>
        <view class="item-value">{{ user.id }}</view>
      </view>
    </view>

    <!-- 通用设置 -->
    <view class="section">
      <view class="section-title">General Settings / 通用设置</view>
      <view class="item arrow" @click="clearCache">
        <view class="item-label">Clear Cache / 清除缓存</view>
        <view class="item-value">{{ cacheSize }}</view>
      </view>
      <view class="item arrow" @click="checkUpdate">
        <view class="item-label">Check for Updates / 检查更新</view>
        <view class="item-value">v1.0.0</view>
      </view>
    </view>

    <!-- 隐私与安全 -->
    <view class="section">
      <view class="section-title">Privacy & Security / 隐私与安全</view>
      <view class="item arrow" @click="showPrivacy">
        <view class="item-label">Privacy Policy / 隐私政策</view>
        <view class="item-value"></view>
      </view>
      <view class="item arrow" @click="showUserAgreement">
        <view class="item-label">User Agreement / 用户协议</view>
        <view class="item-value"></view>
      </view>
    </view>

    <!-- 其他 -->
    <view class="section">
      <view class="section-title">Others / 其他</view>
      <view class="item arrow" @click="goAbout">
        <view class="item-label">About Us / 关于我们</view>
        <view class="item-value"></view>
      </view>
      <view class="item arrow" @click="feedback">
        <view class="item-label">Feedback / 意见反馈</view>
        <view class="item-value"></view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section" v-if="user">
      <button class="logout-btn" @click="handleLogout">Logout / 退出登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userGet, logout } from '../../api/index.js'

const user = ref(null)
const cacheSize = ref('0KB')

onMounted(() => {
  user.value = userGet()
  calculateCacheSize()
})

// 计算缓存大小（模拟）
function calculateCacheSize() {
  try {
    const storage = uni.getStorageInfoSync()
    const size = storage.currentSize || 0
    if (size < 1024) {
      cacheSize.value = `${size}KB`
    } else {
      cacheSize.value = `${(size / 1024).toFixed(2)}MB`
    }
  } catch (e) {
    cacheSize.value = 'Unknown / 未知'
  }
}

// 清除缓存
function clearCache() {
  uni.showModal({
    title: '清除缓存 / Clear Cache',
    content: '确定要清除缓存吗？这不会清除您的登录信息 / Are you sure you want to clear the cache? This will not clear your login session.',
    confirmColor: '#ffd84c',
    success: (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '清除中... / Clearing...' })
        // 这里可以清除一些不重要的缓存数据
        setTimeout(() => {
          uni.hideLoading()
          uni.showToast({ title: '清除成功 / Cleared', icon: 'success' })
          calculateCacheSize()
        }, 1000)
      }
    }
  })
}

// 检查更新
function checkUpdate() {
  uni.showLoading({ title: '检查中... / Checking...' })
  setTimeout(() => {
    uni.hideLoading()
    uni.showModal({
      title: '当前已是最新版本 / Already the latest version',
      content: '版本号 / Version: v1.0.0',
      showCancel: false,
      confirmColor: '#ffd84c'
    })
  }, 1000)
}

// 隐私政策
function showPrivacy() {
  uni.showModal({
    title: '隐私政策 / Privacy Policy',
    content: '我们非常重视您的隐私保护，详细政策请访问官网查看。 / We value your privacy. Please visit our website for the detailed privacy policy.',
    showCancel: false,
    confirmColor: '#ffd84c'
  })
}

// 用户协议
function showUserAgreement() {
  uni.showModal({
    title: '用户协议 / User Agreement',
    content: '使用本应用即表示您同意我们的用户协议，详细内容请访问官网查看。 / By using this app, you agree to our User Agreement. Please visit our website for details.',
    showCancel: false,
    confirmColor: '#ffd84c'
  })
}

// 关于我们
function goAbout() {
  uni.navigateTo({ url: '/pages/about/index' })
}

// 意见反馈
function feedback() {
  uni.showModal({
    title: '意见反馈 / Feedback',
    content: '感谢您的反馈！请通过客服微信联系我们。 / Thank you for your feedback! Please contact us via customer service WeChat.',
    confirmText: '联系客服 / Contact Service',
    confirmColor: '#ffd84c',
    success: (res) => {
      if (res.confirm) {
        uni.navigateBack()
      }
    }
  })
}

// 退出登录
async function handleLogout() {
  uni.showModal({
    title: '提示 / Hint',
    content: '确定要退出登录吗？ / Are you sure you want to log out?',
    confirmText: '退出 / Logout',
    confirmColor: '#ff6600',
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '正在退出... / Logging out...' })
        try {
          await logout()
          user.value = null
          uni.hideLoading()
          uni.showToast({ title: '已退出登录 / Logged out', icon: 'success' })
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/index/index' })
          }, 1500)
        } catch (e) {
          uni.hideLoading()
          uni.showToast({ title: '退出失败 / Failed', icon: 'none' })
        }
      }
    }
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%);
  padding-bottom: 24rpx;
}

.section {
  background: #fff;
  margin: 24rpx 24rpx 0;
  border-radius: 16rpx;
  overflow: hidden;
}

.section-title {
  padding: 24rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #333;
  border-bottom: 1rpx solid #f5f5f5;
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.item:last-child {
  border-bottom: none;
}

.item.arrow {
  position: relative;
}

.item.arrow::after {
  content: '>';
  position: absolute;
  right: 24rpx;
  font-size: 28rpx;
  color: #ccc;
}

.item-label {
  font-size: 28rpx;
  color: #333;
}

.item-value {
  font-size: 26rpx;
  color: #999;
  margin-right: 32rpx;
}

.logout-section {
  margin: 48rpx 24rpx 0;
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #fff;
  color: #ff6600;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 16rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.04);
}
</style>
