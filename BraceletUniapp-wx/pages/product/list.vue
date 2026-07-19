<template>
  <view class="page">
    <scroll-view class="tabs" scroll-x enable-flex>
      <view class="tab" :class="{active: activeCid === 0}" @click="switchCategory(0)">全部</view>
      <view class="tab" v-for="c in categories" :key="c.id" :class="{active: activeCid === c.id}" @click="switchCategory(c.id)">{{ c.name }}</view>
    </scroll-view>
    
    <!-- 瀑布流布局：左右两列 -->
    <view class="waterfall">
      <!-- 左列 -->
      <view class="column">
        <view v-for="p in leftProducts" :key="p.id" class="card" @click="goDetail(p.id)">
          <view class="img" :style="{height: p.imgHeight + 'rpx'}">
            <image v-if="p.coverImage" :src="p.coverImage" mode="aspectFill" class="cover" />
          </view>
          <view class="info">
            <view class="title">{{ p.title }}</view>
            <view class="desc" v-if="p.description">{{ p.description }}</view>
            <view class="bottom">
              <view class="price">¥{{ p.price }}</view>
              <view class="tag" v-if="p.stock > 0">库存{{ p.stock }}</view>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 右列 -->
      <view class="column">
        <view v-for="p in rightProducts" :key="p.id" class="card" @click="goDetail(p.id)">
          <view class="img" :style="{height: p.imgHeight + 'rpx'}">
            <image v-if="p.coverImage" :src="p.coverImage" mode="aspectFill" class="cover" />
          </view>
          <view class="info">
            <view class="title">{{ p.title }}</view>
            <view class="desc" v-if="p.description">{{ p.description }}</view>
            <view class="bottom">
              <view class="price">¥{{ p.price }}</view>
              <view class="tag" v-if="p.stock > 0">库存{{ p.stock }}</view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { productList, categoryList } from '../../api/index.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'
import { onLoad } from '@dcloudio/uni-app'

const products = ref([])
const categories = ref([])
const activeCid = ref(0)

// 为每个商品生成随机图片高度，模拟瀑布流效果
function generateImgHeight() {
  // 随机高度：320-480rpx之间（更大的图片）
  return Math.floor(Math.random() * 160) + 320
}

// 将商品分配到左右两列，实现瀑布流布局
const leftProducts = computed(() => {
  return products.value.filter((_, index) => index % 2 === 0)
})

const rightProducts = computed(() => {
  return products.value.filter((_, index) => index % 2 === 1)
})

function goDetail(id) {
  uni.navigateTo({ url: '/pages/product/detail?id=' + id })
}

async function loadProducts(cid) {
  try {
    const res = await productList(cid)
    let productData = Array.isArray(res) ? res : (res.data || [])
    
    // 为每个商品添加随机图片高度和完整图片URL
    products.value = productData.map(p => ({
      ...p,
      imgHeight: generateImgHeight(),
      coverImage: resolveImageUrl(p.coverImage)
    }))
  } catch (e) {
    console.error('加载商品失败:', e)
  }
}

onLoad(async (options) => {
  // 默认显示全部分类（categoryId = 0），除非明确指定其他分类
  const categoryId = options && options.categoryId ? Number(options.categoryId) : 0
  try {
    const cats = await categoryList()
    categories.value = Array.isArray(cats) ? cats : (cats.data || [])
    // 始终从"全部"开始
    activeCid.value = 0
    await loadProducts(0)
  } catch (e) {}
})

async function switchCategory(cid) {
  activeCid.value = Number(cid) || 0
  await loadProducts(activeCid.value)
}
</script>

<style>
.page {
  padding: 24rpx;
  background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%);
  min-height: 100vh;
  box-sizing: border-box;
}

/* 分类标签栏 */
.tabs { 
  display: flex; 
  gap: 16rpx; 
  white-space: nowrap; 
  padding-bottom: 12rpx; 
  margin-bottom: 16rpx; 
}
.tab { 
  display: inline-flex; 
  padding: 10rpx 20rpx; 
  background: #f6f6f6; 
  color: #666; 
  border-radius: 24rpx; 
  font-size: 24rpx; 
  transition: all 0.3s ease;
}
.tab.active { 
  color: #333; 
  font-weight: 700; 
  background: #fff;
  box-shadow: 0 4rpx 12rpx rgba(255, 216, 76, 0.3);
  position: relative; 
}
.tab.active::after { 
  content: ''; 
  position: absolute; 
  left: 12rpx; 
  right: 12rpx; 
  bottom: -6rpx; 
  height: 6rpx; 
  background: #ffd84c; 
  border-radius: 6rpx; 
}

/* 瀑布流容器 */
.waterfall {
  display: flex;
  gap: 24rpx;
  align-items: flex-start;
}

/* 左右两列 */
.column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

/* 商品卡片 */
.card {
  background: #ffffff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.05), 0 2rpx 8rpx rgba(0,0,0,0.02);
  border: 1rpx solid rgba(0,0,0,0.03);
  transition: all 0.3s ease;
  margin-bottom: 0;
}

.card:active {
  transform: scale(0.98);
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.1);
}

/* 商品图片 */
.img { 
  width: 100%;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9eef3 100%);
  position: relative;
  overflow: hidden;
}

.cover {
  width: 100%;
  height: 100%;
  display: block;
}

/* 商品信息 */
.info { 
  padding: 20rpx; 
}

.title { 
  font-size: 30rpx; 
  color: #333; 
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 10rpx;
  font-weight: 600;
}

.desc {
  font-size: 24rpx;
  color: #999;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 12rpx;
}

/* 底部信息 */
.bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8rpx;
}

.price { 
  color: #ff4d4f; 
  font-weight: 700;
  font-size: 36rpx;
  font-family: 'DIN Alternate', 'Arial', sans-serif;
}

.price::before {
  content: '¥';
  font-size: 26rpx;
  margin-right: 4rpx;
}

.tag {
  font-size: 20rpx;
  color: #999;
  padding: 4rpx 12rpx;
  background: #f5f5f5;
  border-radius: 4rpx;
}
</style>
