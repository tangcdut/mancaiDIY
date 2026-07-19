<template>
  <view class="page">
    <!-- 顶部状态栏占位 -->
    <view class="status-bar"></view>

    <!-- 用户信息头部 -->
    <view class="user-header">
      <!-- 已登录状态 -->
      <block v-if="user">
        <image class="avatar" :src="user.avatarUrl || '/static/tabbar/mine.png'" mode="aspectFill"></image>
        <view class="user-info">
          <text class="nickname">{{ user.nickName || '微信用户' }}</text>
          <text class="welcome-text">欢迎回来，开始你的创作之旅</text>
        </view>
      </block>
      
      <!-- 未登录状态 -->
      <block v-else>
        <view class="info-icon-wrapper">
          <text class="info-icon">i</text>
        </view>
        <view class="user-info">
          <text class="nickname">Hi，欢迎光临</text>
          <text class="welcome-text">登录后可同步设计与订单</text>
        </view>
        <button class="login-btn" @click="handleUserClick">微信登录</button>
      </block>
    </view>

    <!-- 轮播图 -->
    <view class="banner-section">
      <swiper 
        class="banner-swiper" 
        circular 
        :indicator-dots="true" 
        :autoplay="true" 
        :interval="4000" 
        :duration="500"
        indicator-active-color="#ffffff"
        indicator-color="rgba(255,255,255,0.6)"
      >
        <swiper-item v-for="(item, index) in banners" :key="item.id || index">
          <image 
            class="banner-image" 
            :src="item.imageUrl" 
            mode="aspectFill" 
            @click="onBannerClick(item)"
          ></image>
        </swiper-item>
      </swiper>
    </view>

    <!-- 功能卡片区域 -->
    <view class="action-section">
      <!-- DIY定制 -->
      <view class="action-card diy-card" @click="goDesign">
        <view class="icon-wrapper diy-icon-bg">
          <image class="card-icon" src="/static/tabbar/design_selected.png" mode="aspectFit"></image>
        </view>
        <view class="card-text">
          <text class="card-title">DIY定制</text>
          <text class="card-subtitle">设计专属手链</text>
        </view>
      </view>

      <!-- 满彩严选 -->
      <view class="action-card select-card" @click="goProductList">
        <view class="icon-wrapper select-icon-bg">
          <image class="card-icon" src="/static/tabbar/cart_selected.png" mode="aspectFit"></image>
        </view>
        <view class="card-text">
          <text class="card-title">满彩严选</text>
          <text class="card-subtitle">精选成品手链</text>
        </view>
      </view>
    </view>

    <!-- 底部Slogan -->
    <view class="footer">
      <text class="footer-text">满彩珠宝 · 匠心定制</text>
    </view>

    <!-- 完善资料弹窗 -->
    <view v-if="showEditProfile" class="overlay">
      <view class="edit-popup">
        <text class="popup-title">完善个人资料</text>
        
        <view class="form-item">
          <text class="form-label">头像</text>
          <button class="avatar-wrapper" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
            <image class="avatar-preview" :src="tempAvatarUrl || '/static/tabbar/mine.png'" mode="aspectFill"></image>
            <text class="avatar-tip">点击更换</text>
          </button>
        </view>
        
        <view class="form-item">
          <text class="form-label">昵称</text>
          <input type="nickname" class="nickname-input" v-model="tempNickname" placeholder="请输入昵称" @blur="onNicknameBlur" />
        </view>
        
        <button class="confirm-btn" @click="confirmLogin">确认登录</button>
        <text class="skip-btn" @click="skipLogin">跳过，使用默认</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getBannerList, userGet, userSet, loginWithWeixinCode } from '../../api/index.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'

const banners = ref([])
const user = ref(null)

// 登录弹窗相关状态
const showEditProfile = ref(false)
const tempAvatarUrl = ref('')
const tempNickname = ref('')

const loadBanners = async () => {
  try {
    const list = await getBannerList()
    console.log('Banner list:', list)
    if (Array.isArray(list)) {
      // Process image URLs
      banners.value = list.map(item => {
        let imageUrl = item.imageUrl || ''
        imageUrl = resolveImageUrl(imageUrl)
        return {
          ...item,
          imageUrl
        }
      })
    }
  } catch (e) {
    console.error('获取轮播图失败', e)
    // Fallback or empty state
  }
}

const checkLogin = () => {
  user.value = userGet()
}

const handleUserClick = () => {
  if (!user.value) {
    showEditProfile.value = true
    tempNickname.value = '微信用户'
    tempAvatarUrl.value = ''
  }
}

const onChooseAvatar = (e) => {
  const { avatarUrl } = e.detail
  tempAvatarUrl.value = avatarUrl
}

const onNicknameBlur = (e) => {
  tempNickname.value = e.detail.value
}

const confirmLogin = () => {
  const userInfo = {
    nickName: tempNickname.value || '微信用户',
    avatarUrl: tempAvatarUrl.value || ''
  }
  performLogin(userInfo)
}

const skipLogin = () => {
  const userInfo = {
    nickName: '微信用户',
    avatarUrl: ''
  }
  performLogin(userInfo)
}

const performLogin = (userInfo) => {
  showEditProfile.value = false
  uni.showLoading({ title: '登录中...', mask: true })

  // 开发工具和真机统一走微信登录流程
  // wx.login() 在开发工具中正常返回有效 code，jscode2session 正常校验通过
  // 微信服务端根据环境自行返回测试 openid 或正式 openid，业务代码无需区分
  uni.login({
    provider: 'weixin',
    success: async (loginRes) => {
      console.log('微信登录code获取成功:', loginRes.code)
      if (loginRes.code) {
        doBackendLogin(loginRes.code, userInfo)
      } else {
        uni.hideLoading()
        console.error('微信登录code为空')
        uni.showToast({ title: '获取授权失败，请重试', icon: 'none' })
      }
    },
    fail: (err) => {
      uni.hideLoading()
      console.error('微信登录失败:', JSON.stringify(err))
      uni.showToast({ title: '微信登录失败，请重试', icon: 'none' })
    }
  })
}

// 抽取后端登录逻辑，避免重复代码
async function doBackendLogin(code, userInfo) {
  try {
    const res = await loginWithWeixinCode(code, userInfo)
    console.log('后端登录响应:', JSON.stringify(res))

    if (res && res.token) {
      // 后端登录成功 — wechatLogin内部已保存token和user
      user.value = {
        id: res.id,
        nickName: res.nickname,
        avatarUrl: res.avatar,
        openid: res.openid
      }
      uni.hideLoading()
      uni.showToast({ title: '登录成功', icon: 'success' })
    } else {
      uni.hideLoading()
      console.error('后端登录返回缺少token:', JSON.stringify(res))
      uni.showToast({ title: (res && res.msg) || '登录失败，请重试', icon: 'none' })
    }
  } catch(e) {
    uni.hideLoading()
    console.error('后端登录失败:', JSON.stringify(e))
    uni.showToast({ title: (e && e.msg) || '登录失败，请检查网络', icon: 'none' })
  }
}

const onBannerClick = (item) => {
  if (item.link) {
    // Determine if link is a tabbar page or normal page
    const tabbarPages = [
      '/pages/index/index',
      '/pages/design/index',
      '/pages/cart/index',
      '/pages/mine/index'
    ]
    if (tabbarPages.includes(item.link)) {
      uni.switchTab({ url: item.link })
    } else {
      uni.navigateTo({ url: item.link })
    }
  }
}

const goDesign = () => {
  uni.switchTab({
    url: '/pages/design/index'
  })
}

const goProductList = () => {
  // 假设严选是商品列表页，且可能传递某个分类ID
  // 如果没有特定分类，直接跳转
  uni.navigateTo({
    url: '/pages/product/list'
  })
}

onShow(() => {
  checkLogin()
})

onMounted(() => {
  loadBanners()
  checkLogin()
})
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%);
  padding: 0 30rpx;
  box-sizing: border-box;
}

.status-bar {
  /* 适配状态栏高度，这里简单给个高度，实际可动态计算 */
  height: var(--status-bar-height); 
  width: 100%;
}

/* 用户头部 */
.user-header {
  display: flex;
  align-items: center;
  padding: 30rpx;
  background: linear-gradient(135deg, #fffdf8, #fff);
  border-radius: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(180,150,100,0.08), 0 1rpx 4rpx rgba(0,0,0,0.04);
  border: 1rpx solid rgba(212,165,116,0.08);
}

.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-right: 24rpx;
  background-color: #eee;
  border: 2rpx solid #fff;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05);
}

.info-icon-wrapper {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-right: 24rpx;
  background-color: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid #fff;
}

.info-icon {
  font-size: 40rpx;
  color: #cda274;
  font-family: monospace;
  font-weight: bold;
  border: 2rpx solid #cda274;
  border-radius: 50%;
  width: 44rpx;
  height: 44rpx;
  line-height: 40rpx;
  text-align: center;
}

.user-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.nickname {
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.welcome-text {
  font-size: 24rpx;
  color: #999;
}

.login-btn {
  margin: 0;
  padding: 0 30rpx;
  height: 60rpx;
  line-height: 60rpx;
  background: linear-gradient(135deg, #d4a574, #c19a68);
  color: #fff;
  font-size: 26rpx;
  border-radius: 30rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(196,154,104,0.3);

  &::after {
    border: none;
  }

  &:active {
    opacity: 0.9;
  }
}

/* 轮播图 */
  .banner-section {
    width: 100%;
    margin-bottom: 30rpx;
    
    swiper {
      height: 690rpx;
      border-radius: 20rpx;
      overflow: hidden;
      
      .banner-image {
        width: 100%;
        height: 100%;
        border-radius: 20rpx;
      }
    }
  }

/* 弹窗样式 */
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0,0,0,0.6);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.edit-popup {
  width: 600rpx;
  background-color: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.popup-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 40rpx;
}

.form-item {
  width: 100%;
  margin-bottom: 30rpx;
  
  .form-label {
    font-size: 28rpx;
    color: #666;
    margin-bottom: 16rpx;
    display: block;
  }
}

.avatar-wrapper {
  width: 100%;
  height: 200rpx;
  background-color: #f9f9f9;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px dashed #ddd;
  
  &::after {
    border: none;
  }
  
  .avatar-preview {
    width: 100rpx;
    height: 100rpx;
    border-radius: 50%;
    margin-bottom: 10rpx;
  }
  
  .avatar-tip {
    font-size: 24rpx;
    color: #999;
  }
}

.nickname-input {
  width: 100%;
  height: 88rpx;
  background-color: #f9f9f9;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.confirm-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background-color: #cda274;
  color: #fff;
  font-size: 30rpx;
  border-radius: 44rpx;
  margin-top: 20rpx;
  margin-bottom: 20rpx;
  
  &::after {
    border: none;
  }
}

.skip-btn {
  font-size: 26rpx;
  color: #999;
  padding: 10rpx;
}

/* 功能卡片 */
.action-section {
  display: flex;
  justify-content: space-between;
  margin-bottom: 40rpx;
}

.action-card {
  width: 48%;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.04), 0 2rpx 8rpx rgba(0,0,0,0.02);
  border: 1rpx solid rgba(0,0,0,0.03);
  transition: all 0.2s;

  &:active {
    transform: scale(0.97);
    box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.06);
  }
}

.icon-wrapper {
  width: 100rpx;
  height: 100rpx;
  border-radius: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
}

.diy-icon-bg {
  background: linear-gradient(135deg, #fff8e1, #fff0c2);
}

.select-icon-bg {
  background: linear-gradient(135deg, #e8f4fd, #d6ecfb);
}

.card-icon {
  width: 56rpx;
  height: 56rpx;
}

.emoji-icon {
  font-size: 48rpx;
}

.card-text {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.card-subtitle {
  font-size: 22rpx;
  color: #999;
}

/* 底部 */
.footer {
  width: 100%;
  text-align: center;
  padding-bottom: 40rpx;
}

.footer-text {
  font-size: 24rpx;
  color: #ccc;
  letter-spacing: 2rpx;
}
</style>
