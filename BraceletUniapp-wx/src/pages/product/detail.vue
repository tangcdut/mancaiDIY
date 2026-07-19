<template>
  <view class="page" v-if="detail">
    <!-- 轮播图区域 -->
    <swiper class="gallery" circular :indicator-dots="bannerList.length > 1" :autoplay="true" :interval="4000" :duration="500">
      <swiper-item v-for="(img, index) in bannerList" :key="index">
        <image class="img" :src="img" mode="aspectFill" @click="handlePreview(index)" />
      </swiper-item>
    </swiper>

    <view class="info">
      <view class="title">{{ detail.title }}</view>
      <view class="price">¥{{ detail.price }}</view>
      <view class="desc">{{ detail.description }}</view>
    </view>

    <view class="qty-box">
      <view class="label">数量</view>
      <view class="stepper">
        <view class="s-btn" @click="dec">-</view>
        <input class="ipt" type="number" v-model.number="count" />
        <view class="s-btn" @click="inc">+</view>
      </view>
    </view>

    <view class="space"></view>

    <view class="action-bar">
      <button class="btn ghost" @click="handleAdd">加入购物车</button>
      <button class="btn primary" @click="buyNow">立即购买</button>
    </view>
  </view>
  
</template>

<script setup>
import { ref } from 'vue'
import { productDetail, cartAdd, orderCreate } from '../../api/index.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'
import { onLoad } from '@dcloudio/uni-app'
import { updateCartBadge } from '../../utils/cartBadge.js'

const detail = ref(null)
const product = ref(null)
const bannerList = ref([])
let pid = ''
const count = ref(1)

function inc () { count.value = Number(count.value || 1) + 1 }
function dec () { count.value = Math.max(1, Number(count.value || 1) - 1) }

// 图片预览
function handlePreview(current) {
  if (!bannerList.value || bannerList.value.length === 0) return
  uni.previewImage({
    urls: bannerList.value,
    current: bannerList.value[current],
    indicator: 'number',
    loop: true
  })
}

async function handleAdd() {
  try {
    await cartAdd(Number(pid), Number(count.value))
    uni.showToast({ title: '已加入购物车', icon: 'success' })
    // 更新购物车角标
    updateCartBadge()
  } catch (e) {
    console.error('添加购物车失败:', e)
    if (e.code === 401) {
      uni.showModal({
        title: '提示',
        content: '请先登录后再操作',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) {
            uni.reLaunch({ url: '/pages/index/index' })
          }
        }
      })
    } else {
      uni.showToast({ 
        title: e.msg || '添加失败，请重试', 
        icon: 'none' 
      })
    }
  }
}

async function buyNow() {
  try {
    // 立即购买不经过购物车，直接跳转到确认页
    const item = {
        productId: product.value.id,
        title: product.value.title,
        price: product.value.price,
        image: product.value.coverImage,
        imageUrl: product.value.coverImage,
        quantity: Number(count.value)
    }
    uni.setStorageSync('direct_buy_item', item)
    
    // 跳转到订单确认页，模式为直接购买
    uni.navigateTo({ url: '/pages/order/confirm?mode=direct' })
  } catch (e) {
    console.error('立即购买失败:', e)
    uni.showToast({ title: '操作失败，请重试', icon: 'none' })
  }
}

onLoad(async (options) => {
  pid = options && options.id ? options.id : ''
  if (!pid) return
  try {
    const res = await productDetail(Number(pid))
    product.value = res && res.product ? res.product : res
    
    // 处理图片URL
    if (product.value) {
      if (product.value.coverImage) {
        product.value.coverImage = resolveImageUrl(product.value.coverImage)
      }
      
      // 处理详情图
      let details = []
      if (product.value.detailImages) {
        // 兼容可能是字符串的情况
        details = Array.isArray(product.value.detailImages) 
          ? product.value.detailImages 
          : (typeof product.value.detailImages === 'string' ? JSON.parse(product.value.detailImages) : [])
      }
      
      const processedDetails = details.map(img => resolveImageUrl(img))
      
      // 构建轮播图数据 (封面图 + 详情图)
      // 使用 Set 去重，防止详情图中包含封面图导致重复
      const images = new Set()
      if (product.value.coverImage) images.add(product.value.coverImage)
      processedDetails.forEach(img => images.add(img))
      
      bannerList.value = Array.from(images).filter(Boolean)
    }
    
    detail.value = product.value || null
  } catch (e) {
    console.error('获取详情失败', e)
  }
})
</script>

<style>
.page { background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%); min-height: 100vh; padding-bottom: 140rpx; }
.gallery { height: 460rpx; background: #e9eef3; }
.img { width: 100%; height: 100%; }
.info { background: #ffffff; border-radius: 0; padding: 24rpx; margin-top: 16rpx; }
.title { font-size: 32rpx; font-weight: 700; color: #333; }
.price { color: #e54d42; margin-top: 8rpx; font-size: 32rpx; font-weight: 700; }
.desc { color: #666; line-height: 1.7; margin-top: 12rpx; }
.qty-box { display: flex; justify-content: space-between; align-items: center; background: #ffffff; margin-top: 16rpx; padding: 16rpx 24rpx; }
.label { color: #333; font-size: 28rpx; }
.stepper { display: flex; align-items: center; }
.s-btn { width: 64rpx; height: 64rpx; display: flex; align-items: center; justify-content: center; background: #f5f5f5; border-radius: 10rpx; font-size: 36rpx; color: #333; }
.ipt { width: 120rpx; margin: 0 12rpx; text-align: center; height: 64rpx; border: 2rpx solid #eee; border-radius: 10rpx; }
.space { height: 20rpx; }
.action-bar { position: fixed; left: 0; right: 0; bottom: 0; background: #ffffff; padding: 12rpx 24rpx calc(12rpx + env(safe-area-inset-bottom)); display: flex; gap: 12rpx; box-shadow: 0 -6rpx 12rpx rgba(0,0,0,0.04); }
.btn { flex: 1; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-weight: 600; }
.btn.ghost { background: #fffbe6; color: #a76a00; border: 2rpx solid #ffd84c; }
.btn.primary { background: #ffd84c; color: #333; }
</style>
