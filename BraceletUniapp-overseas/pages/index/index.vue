<template>
  <view class="page">
    <!-- 顶部状态栏占位 -->
    <view class="status-bar"></view>

    <!-- 用户信息头部 -->
    <view class="user-header">
      <view class="info-icon-wrapper">
        <text class="info-icon">i</text>
      </view>
      <view class="user-info">
        <text class="nickname">Hi，欢迎光临 / Welcome</text>
        <text class="welcome-text">开始你的创作之旅 · Design Your Bracelet</text>
      </view>
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
          <text class="card-title">DIY定制 / DIY Custom</text>
          <text class="card-subtitle">设计专属手链 / Custom Bracelet</text>
        </view>
      </view>

      <!-- 满彩严选 -->
      <view class="action-card select-card" @click="goProductList">
        <view class="icon-wrapper select-icon-bg">
          <image class="card-icon" src="/static/tabbar/cart_selected.png" mode="aspectFit"></image>
        </view>
        <view class="card-text">
          <text class="card-title">满彩严选 / Finished Products</text>
          <text class="card-subtitle">精选成品手链 / Selected Bracelets</text>
        </view>
      </view>
    </view>

    <!-- 底部Slogan -->
    <view class="footer">
      <text class="footer-text">满彩珠宝 · 匠心定制 / Man Cai Jewelry</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBannerList } from '../../api/index.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'

const banners = ref([])

const loadBanners = async () => {
  try {
    const list = await getBannerList()
    console.log('Banner list:', list)
    if (Array.isArray(list)) {
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
    console.error('Failed to load banners', e)
  }
}

const onBannerClick = (item) => {
  if (item.link) {
    const tabbarPages = [
      '/pages/index/index',
      '/pages/design/index',
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
  uni.switchTab({ url: '/pages/design/index' })
}

const goProductList = () => {
  uni.navigateTo({ url: '/pages/product/list' })
}

onMounted(() => {
  loadBanners()
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
