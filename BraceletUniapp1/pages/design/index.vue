<template>
  <view class="page">
    <!-- 顶部状态栏 -->
    <view class="header">
      <view class="header-left">
        <view class="stat-box">
          <text class="stat-text">{{ beads.length }}颗 {{ weightG }}g</text>
        </view>
      </view>
      
      <view class="header-right">
        <view class="price-box">
          <text class="price-label">合计</text>
          <text class="price-num">¥{{ totalPrice }}</text>
        </view>
        <view class="action-btns">

          <button 
            class="checkout-btn" 
            :disabled="!beads.length" 
            @click="showPreview"
          >
            结算1
          </button>
        </view>
      </view>
    </view>

    <!-- 主画布区域 -->
    <view class="main-area">
      <!-- 提示图标 -->
      <view class="tip-icon" @click="showTips">!!</view>
      <!-- AI算命入口 -->
      <view class="ai-icon" @click="showAiModal = true">AI</view>
      
      <!-- 删除区域指示器（拖动珠子到此处删除） -->
      <view v-if="isDragging" class="delete-zone" :class="{ active: isInDeleteZone }">
        <view class="delete-zone-icon">🗑</view>
        <text class="delete-zone-text">拖到此处删除</text>
      </view>

      <!-- 中心画布 -->
      <view class="canvas-container">
        <view
          class="canvas"
          @touchstart="onTouchStart"
          @touchmove.stop.prevent="onTouchMove"
          @touchend="onTouchEnd"
        >
          <view :style="{transform: `scale(${canvasScale})`, transformOrigin: 'center center', width: '100%', height: '100%'}">
          <!-- 绳子 -->
          <view class="rope-circle" :style="{width: layoutRadius * 2 + 'rpx', height: layoutRadius * 2 + 'rpx', zIndex: 5}"></view>
          
          <!-- 中心Logo -->
          <view class="center-logo" :style="{zIndex: 2}">
            <image class="logo-img" :src="logoPath" mode="widthFix" />
            <text class="logo-name">满彩珠宝</text>
          </view>
          
          <!-- 珠子 -->
          <view
            v-for="(b, i) in beads"
            :key="b._id"
            class="bead"
            :class="{
              'is-new': b.isNew,
              'is-mirrored': b.mirrored,
              'is-pendant': isPendant(b),
              'is-dragging': draggingIndex === i,
              'is-drag-target': isDragging && dragTargetIndex === i,
              'is-delete-target': isDragging && draggingIndex === i && isInDeleteZone,
              'is-swap-source': swapMode && firstSwapIndex === i,
              'is-swap-target': swapMode && firstSwapIndex !== i
            }"
            :style="getBeadStyle(b, i)"
            :data-index="i"
            @touchstart="onBeadTouchStart(i, $event)"
            @touchmove.stop.prevent="onBeadTouchMove(i, $event)"
            @touchend="onBeadTouchEnd(i, $event)"
          >
             <image v-if="b.imageUrl && !b.loadFailed" :src="b.imageUrl" mode="aspectFit" class="bead-img" @error="onBeadImageError(b, i)"></image>
             <view v-else class="bead-placeholder" :style="{background: b.color || '#f0f0f0'}"></view>
             <view v-if="swapMode && firstSwapIndex === i" class="swap-indicator">1</view>
          </view>
          </view>
        </view>
      </view>

      <!-- 飞行动画：原图从商品卡片飞到画布中心 -->
      <view
        v-if="flyingBead"
        class="flying-bead-overlay"
        :style="flyingBeadStyle"
      >
        <image
          v-if="flyingBead.imageUrl"
          :src="flyingBead.imageUrl"
          mode="aspectFit"
          class="flying-bead-img"
        />
        <view v-else class="flying-bead-dot" :style="{ background: flyingBead.color || '#e8e8e8' }" />
      </view>

      <!-- 底部工具栏 -->
      <view class="toolbar-container" :class="{ collapsed: isToolbarCollapsed }" @click="onToolbarContainerClick">
        <view class="tool-bar">
          <view class="tool-item" :class="{ active: showSizeSelector }" @click="showSizeSelector = true">
            <Icon name="ruler" size="32rpx" color="#666" />
            <text class="tool-text">{{ selectedSize }}cm</text>
          </view>
          <view class="tool-item" :class="{ active: isAutoArranged }" @click="toggleAutoArrange">
            <text class="tool-icon-img">⊞</text>
            <text class="tool-text">均匀</text>
          </view>
          <view class="tool-item" @click="mirrorAllBeads">
            <text class="tool-icon-img">⇄</text>
            <text class="tool-text">镜像</text>
          </view>
          <view class="tool-item" @click="undoLastBead">
            <text class="tool-icon-img">↩</text>
            <text class="tool-text">撤销</text>
          </view>
          <view class="tool-item" @click="confirmClear">
            <Icon name="trash" size="32rpx" color="#e74c3c" />
            <text class="tool-text">清空</text>
          </view>
        </view>
        <view class="tool-collapse-hit" @click.stop="toggleToolbar">
          <view class="tool-collapse-btn">
            <text class="collapse-icon">{{ isToolbarCollapsed ? '›' : '‹' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部商品选择区 -->
    <view class="goods-section">
<!-- 暂时屏蔽以排查 wx://not-found 错误 -->
<!-- <canvas type="2d" id="exportCanvas" class="export-canvas" style="width: 560rpx; height: 560rpx; position: fixed; left: -9999px;"></canvas> -->      <view class="section-header">
        <scroll-view class="category-tabs" scroll-x :show-scrollbar="false">
          <view 
            class="cat-tab" 
            :class="{active: activeCategory === cat.keyCode}" 
            v-for="cat in displayCategories" 
            :key="cat.keyCode" 
            @click="switchCategory(cat.keyCode)"
          >
            <text>{{ cat.name }}</text>
          </view>
        </scroll-view>
      </view>
      
      <view class="section-body">
        <!-- 侧边栏筛选 -->
        <scroll-view class="color-sidebar" scroll-y :show-scrollbar="false">
          <!-- 子分类/色系 (统一展示) -->
          <view class="sidebar-group">
            <view
              class="color-tag"
              :class="{active: !activeColor}"
              @click="switchColor('')"
            >
              全部
            </view>
            <view
              class="color-tag"
              :class="{active: activeColor === getSubCategoryKey(sub)}"
              v-for="sub in currentSubCategories"
              :key="getSubCategoryKey(sub)"
              @click="switchColor(getSubCategoryKey(sub))"
            >
              {{ sub.name }}
            </view>
          </view>
        </scroll-view>
        
        <!-- 商品列表 -->
        <scroll-view class="product-area" scroll-y :show-scrollbar="false" :scroll-top="productScrollTop" @scrolltolower="onProductScrollToLower">
          <view v-if="loading && page === 1" class="state-box">
            <view class="loading-dot"></view>
            <text>加载中</text>
          </view>
          <view v-else-if="!goods.length" class="state-box">
            <text>暂无商品</text>
          </view>
          <view v-else class="product-list">
            <view 
              class="product-card" 
              :class="{active: clickedId === g.id, soldout: g.stock === 0}"
              v-for="g in goods" 
              :key="g.id" 
              @click="addBead(g, $event)"
            >
              <view class="p-visual">
                <view class="p-bead" :style="{background: g.color || 'transparent'}" @click.stop="previewProductImage(g, $event)">
                <image
                  v-if="g.imageUrl"
                  :src="g.imageUrl"
                  mode="aspectFit"
                  class="p-img"
                  :class="{loaded: g.loaded}"
                  lazy-load
                  @load="g.loaded = true"
                />
              </view>
                <view class="p-stock">
                  库存{{ g.stock }}
                </view>
              </view>
              <text class="p-name">{{ g.title }}</text>
              <text class="p-price">¥{{ g.price }}</text>
              <text class="p-size" v-if="g.size">{{ g.size }}mm</text>
            </view>
          </view>
          <!-- <view v-if="loading && page > 1" class="loading-more">
            <text>加载更多...</text>
          </view> -->
        </scroll-view>
      </view>
    </view>

    <!-- 尺寸选择弹窗 -->
    <view v-if="showSizeSelector" class="mask" @click="showSizeSelector = false">
      <view class="popup" @click.stop>
        <view class="popup-header">
          <text class="popup-title">选择手围</text>
          <view @click="showSizeSelector = false"><Icon name="close" size="32rpx" color="#999" /></view>
        </view>
        <view class="popup-body">
          <view class="size-options">
            <view 
              class="size-opt" 
              :class="{selected: selectedSize === s}" 
              v-for="s in sizeOptions" 
              :key="s" 
              @click="selectSize(s)"
            >
              <text class="opt-num">{{ s }}</text>
              <text class="opt-unit">cm</text>
            </view>
          </view>
        </view>
        <button class="popup-btn" @click="confirmSize">确定</button>
      </view>
    </view>

    <!-- 订单预览弹窗 -->
    <view v-if="showOrderPreview" class="mask" @click="showOrderPreview = false">
      <view class="popup preview-popup" @click.stop>
        <view class="popup-header">
          <text class="popup-title">确认订单</text>
          <view @click="showOrderPreview = false"><Icon name="close" size="32rpx" color="#999" /></view>
        </view>
        <view class="preview-body">
          <view class="preview-meta">
            <text>手围 {{ selectedSize }}cm</text>
            <text>{{ beads.length }}颗珠子</text>
            <text>约{{ weightG }}g</text>
          </view>
          <view class="preview-items">
            <view class="pv-item" v-for="(item, i) in orderItems" :key="i">
              <text class="pv-name">{{ item.title }}</text>
              <text class="pv-qty">×{{ item.quantity }}</text>
              <text class="pv-price">¥{{ (item.price * item.quantity).toFixed(2) }}</text>
            </view>
          </view>
          <view class="preview-total">
            <text>合计</text>
            <text class="pv-total-num">¥{{ totalPrice }}</text>
          </view>
        </view>
        <view class="preview-actions">
          <button class="act-cancel" @click="showOrderPreview = false">继续编辑</button>
          <button class="act-submit" @click="submitOrder">提交订单</button>
        </view>
      </view>
    </view>

    <!-- 首次进入引导 -->
    <view v-if="showInitGuide" class="mask">
      <view class="popup guide-popup" @click.stop>
        <view class="guide-icon"><Icon name="star" size="48rpx" color="#FFC107" /></view>
        <text class="guide-title">欢迎来到DIY设计台</text>
        <text class="guide-desc">请先选择您的手围尺寸</text>
        <view class="size-options compact">
          <view 
            class="size-opt" 
            :class="{selected: selectedSize === s}" 
            v-for="s in sizeOptions" 
            :key="s" 
            @click="selectSize(s)"
          >
            <text class="opt-num">{{ s }}</text>
            <text class="opt-unit">cm</text>
          </view>
        </view>
        <button class="popup-btn" @click="startDesign">开始设计</button>
      </view>
    </view>
    
    <!-- 操作指南弹窗 -->
    <view v-if="showHelpPopup" class="mask" @click="closeHelpPopup">
      <view class="popup help-popup" @click.stop>
        <view class="popup-header">
          <view class="header-deco"><Icon name="star" size="32rpx" color="#FFC107" /></view>
          <text class="popup-title">操作指南</text>
          <view @click="closeHelpPopup"><Icon name="close" size="32rpx" color="#999" /></view>
        </view>
        <view class="help-grid">
          <view class="help-item">
            <view class="help-icon-box">
              <view class="demo-bead"></view>
              <view class="demo-hand"><Icon name="tap" size="48rpx" color="#666" /></view>
            </view>
            <text class="help-text">点击下方珠子添加</text>
          </view>
          <view class="help-item">
            <view class="help-icon-box">
              <view class="demo-circle"></view>
              <view class="demo-drag-hand"><Icon name="tap" size="48rpx" color="#666" /></view>
            </view>
            <text class="help-text">长按拖动调整位置</text>
          </view>
          <view class="help-item">
            <view class="help-icon-box">
              <view class="demo-bead-mirror"></view>
            </view>
            <text class="help-text">单击珠子镜像翻转</text>
          </view>
          <view class="help-item">
            <view class="help-icon-box">
              <view class="demo-toolbar">
                <view class="dt-dot"></view>
                <view class="dt-dot active"></view>
                <view class="dt-dot"></view>
              </view>
            </view>
            <text class="help-text">工具栏更多功能</text>
          </view>
          <view class="help-item">
            <view class="help-icon-box">
              <view class="demo-pinch">
                <view class="finger finger-1"></view>
                <view class="finger finger-2"></view>
              </view>
            </view>
            <text class="help-text">双指捏合缩放画布</text>
          </view>
        </view>
        <view class="help-footer">
          <view class="checkbox-row" @click="toggleDontShowHelp">
            <view class="checkbox" :class="{checked: dontShowHelpAgain}"></view>
            <text class="checkbox-label">下次不再弹出</text>
          </view>
          <button class="help-btn" @click="closeHelpPopup">我知道了</button>
        </view>
      </view>
    </view>

    <!-- AI算命输入弹窗 -->
    <view v-if="showAiModal" class="mask" @click="showAiModal = false">
      <view class="popup ai-popup" @click.stop>
        <view class="popup-header">
          <text class="popup-title">AI 智能搭配</text>
          <view @click="showAiModal = false"><Icon name="close" size="32rpx" color="#999" /></view>
        </view>
        <view class="ai-popup-body">
          <textarea
            class="ai-input"
            v-model="aiPrompt"
            placeholder="描述你的需求，如：我1990年5月出生，男生，想要一条招财手链"
            :maxlength="200"
            auto-height
          />
          <view class="ai-tarot-row">
            <text class="ai-tarot-label">启用塔罗牌分析</text>
            <switch :checked="aiUseTarot" @change="aiUseTarot = $event.detail.value" color="#9c7afb" />
          </view>
          <button class="ai-submit-btn" :disabled="aiLoading || !aiPrompt.trim()" @click="handleAiRecommend">
            {{ aiLoading ? 'AI 推算中...' : '开始推算' }}
          </button>
        </view>
      </view>
    </view>

    <!-- AI算命结果弹窗 -->
    <view v-if="showAiResult" class="mask" @click="showAiResult = false">
      <view class="popup ai-result-popup" @click.stop>
        <view class="popup-header">
          <text class="popup-title">AI 推荐结果</text>
          <view @click="showAiResult = false"><Icon name="close" size="32rpx" color="#999" /></view>
        </view>
        <scroll-view class="ai-result-body" scroll-y>
          <view v-if="aiResult.bazi" class="ai-section">
            <text class="ai-section-title">生辰八字</text>
            <text class="ai-section-text">{{ aiResult.bazi }}</text>
          </view>
          <view v-if="aiResult.analysis" class="ai-section">
            <text class="ai-section-title">命理分析</text>
            <text class="ai-section-text">{{ aiResult.analysis }}</text>
          </view>
          <view v-if="aiResult.tarotInfo" class="ai-section">
            <text class="ai-section-title">塔罗牌解读</text>
            <text class="ai-section-text">{{ aiResult.tarotInfo }}</text>
          </view>
          <view v-if="aiResult.materials && aiResult.materials.length" class="ai-section">
            <text class="ai-section-title">推荐材料 ({{ aiResult.materials.length }})</text>
            <view class="ai-material-list">
              <view class="ai-material-item" v-for="(m, i) in aiResult.materials" :key="i">
                <image v-if="m.imageUrl" :src="resolveImageUrl(m.imageUrl)" mode="aspectFit" class="ai-material-img" />
                <view class="ai-material-info">
                  <text class="ai-material-name">{{ m.title }}</text>
                  <text class="ai-material-pos">{{ m.position }} ×{{ m.quantity || 1 }}</text>
                  <text v-if="m.reason" class="ai-material-reason">{{ m.reason }}</text>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
        <view class="ai-result-actions">
          <button class="ai-result-cancel" @click="showAiResult = false">关闭</button>
          <button class="ai-result-apply" :disabled="aiLoading" @click="applyAiDesign">应用到设计台</button>
        </view>
      </view>
    </view>

    <canvas type="2d" id="exportCanvas" class="export-canvas" style="width: 560rpx; height: 560rpx; position: fixed; left: 200%; top: 0; pointer-events: none; opacity: 0;"></canvas>

    <!-- 实物图预览弹窗 -->
    <view v-if="showImagePreview" class="mask" @click="showImagePreview = false">
      <view class="popup image-preview-popup" @click.stop>
        <view class="popup-header">
          <text class="popup-title">{{ previewProduct?.title || '实物图' }}</text>
          <view @click="showImagePreview = false"><Icon name="close" size="32rpx" color="#999" /></view>
        </view>
        <view class="image-preview-body">
          <image
            v-if="previewImageUrl"
            :src="previewImageUrl"
            mode="widthFix"
            class="preview-full-img"
            @click="previewFullImage"
          />
          <view v-else class="preview-no-image">暂无实物图</view>
        </view>
        <view class="image-preview-info" v-if="previewProduct">
          <text class="preview-info-name">{{ previewProduct.title }}</text>
          <text class="preview-info-price">¥{{ previewProduct.price }}</text>
          <text class="preview-info-size" v-if="previewProduct.size">{{ previewProduct.size }}mm</text>
          <text class="preview-info-stock">库存: {{ previewProduct.stock }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance, nextTick, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
// 直接从 api.js 导入以避免 index.js 可能的重导出问题
import {
  designCategoryList,
  designProductList,
  designOrderCreate,
  uploadFile,
  addToCart,
  aiRecommend
} from '../../api/api.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'
import { updateCartBadgeNow } from '../../utils/cartBadge.js'
import Icon from '../../components/Icon.vue'

// 实例
const instance = getCurrentInstance()

const isMounted = ref(false)
const didInit = ref(false)
const logoPath = '/static/logo/final_logo.jpg'
console.log('Logo Path set to absolute:', logoPath)

// 数据状态
const categories = ref([])
const colorSeries = ref([])
const colorSeriesAll = ref([])
const activeCategory = ref('main_bead')
const activeColor = ref('')
const goods = ref([])
const allGoods = ref([]) // 用于前端分页的完整数据缓存
const loading = ref(false)
// 分页状态
const page = ref(1)
const pageSize = ref(20)
const totalPages = ref(1)
const productScrollTop = ref(0)

const beads = ref([])
const clickedId = ref(null)

// 尺寸
const selectedSize = ref(Number(uni.getStorageSync('diy_selected_size')) || 16)
watch(selectedSize, (newVal) => {
  uni.setStorageSync('diy_selected_size', newVal)
})
const sizeOptions = [13, 14, 15, 16, 17, 18, 19, 20, 21, 22]
const showSizeSelector = ref(false)

// 拖拽和交换
const draggingIndex = ref(-1)
const isDragging = ref(false)
const swapMode = ref(false)
const firstSwapIndex = ref(-1)
const isAutoArranged = ref(false) // 是否一键排列（均匀分布）

// 弹窗
const showOrderPreview = ref(false)
const showInitGuide = ref(false)
const showHelpPopup = ref(false)
const dontShowHelpAgain = ref(false)
const isToolbarCollapsed = ref(false)

// AI算命
const showAiModal = ref(false)
const showAiResult = ref(false)
const aiLoading = ref(false)
const aiPrompt = ref('')
const aiUseTarot = ref(false)
const aiResult = ref(null)

// 实物图预览
const showImagePreview = ref(false)
const previewImageUrl = ref('')
const previewProduct = ref(null)

// 删除区域
const isInDeleteZone = ref(false)

// 飞行动画
const flyingBead = ref(null)
const flyingBeadStyle = ref('')

let beadIdCounter = 0
let productRequestSeq = 0
let suppressScrollToLowerUntil = 0

function resetProductListView() {
  productScrollTop.value = 0
  suppressScrollToLowerUntil = Date.now() + 600
}

// 过滤绳子分类
const displayCategories = computed(() => categories.value.filter(c => c.keyCode !== 'rope'))

// 获取当前选中主分类的子分类列表
const currentSubCategories = computed(() => {
  const current = categories.value.find(c => c.keyCode === activeCategory.value)
  return current && Array.isArray(current.children) ? current.children : []
})

function getSubCategoryKey(item) {
  if (!item) return ''
  // 优先使用 keyCode，其次 id/key/code/value/name
  const v = item.keyCode ?? item.id ?? item.key ?? item.code ?? item.value ?? item.name
  return normalizeFilterValue(v)
}

function getColorItemKey(item) {
  return getSubCategoryKey(item)
}

function getClassificationItemKey(item) {
  return getSubCategoryKey(item)
}

function normalizeOptionItem(item) {
  if (item === undefined || item === null) return null
  if (typeof item === 'string' || typeof item === 'number') {
    const v = String(item)
    return { id: v, name: v }
  }
  if (typeof item === 'object') {
    const id = normalizeFilterValue(item.id ?? item.keyCode ?? item.key ?? item.code ?? item.value ?? item.name ?? item.title ?? item.label)
    const name = String(item.name ?? item.title ?? item.label ?? item.value ?? item.keyCode ?? item.id ?? id)
    return { ...item, id: id || name, name }
  }
  return null
}

function normalizeOptionList(list) {
  if (!Array.isArray(list)) return []
  return list.map(normalizeOptionItem).filter(Boolean)
}

function normalizeFilterValue(v) {
  if (v === undefined || v === null || v === '') return ''
  return String(v)
}

function isPendant(bead) {
  const title = String(bead?.title || '')
  return title.endsWith('吊坠')
}

// 目标珠子数
const targetCount = computed(() => {
  if (!beads.value.length) return Math.round(selectedSize.value * 10 / 8)
  const avg = beads.value.reduce((s, b) => s + Number(b.size || 8), 0) / beads.value.length
  return Math.round(selectedSize.value * 10 / avg)
})

// 总价
const totalPrice = computed(() => beads.value.reduce((s, b) => s + Number(b.price || 0), 0).toFixed(2))

// 重量
const weightG = computed(() => {
  return beads.value.reduce((s, b) => {
    const sz = Number(b.size || 8)
    return s + (sz >= 10 ? 0.6 : sz >= 8 ? 0.4 : 0.3)
  }, 0).toFixed(1)
})

// 订单项
const orderItems = computed(() => {
  const map = {}
  beads.value.forEach(b => {
    const k = `${b.title}_${b.price}`
    if (!map[k]) map[k] = { 
      productId: b.productId,
      materialId: b.productId, // Compatible with backend expectations
      id: b.productId, // Compatible with backend expectations
      title: b.title, 
      price: b.price, 
      quantity: 0,
      imageUrl: b.imageUrl,
      color: b.color 
    }
    map[k].quantity++
  })
  return Object.values(map)
})

function getBeadArcSizeRpx(bead) {
  // 吊坠占一小部分空间 (例如 3mm)，避免重叠
  if (isPendant(bead)) return 3 * 7
  
  const base = Number(bead?.size || 8) * 7
  return base
}

const maxRadiusHistory = ref(0)
const manualScale = ref(1.0) // 手动缩放比例
const initialPinchDistance = ref(0) // 初始双指距离
const startManualScale = ref(1.0) // 缩放开始时的比例

// 动态计算半径
const visualRadius = computed(() => {
  // 严格按照表格规格计算
  // 表格数据显示，绳长大约为 (手围 + 2.4cm)
  // 例如 16cm 手围，8mm 珠子 23 颗 = 184mm，即 18.4cm
  const circumferenceMm = (selectedSize.value + 2.4) * 10
  const circumferenceRpx = circumferenceMm * 7
  return circumferenceRpx / (2 * Math.PI)
})

const rawLayoutRadius = computed(() => {
  const count = beads.value.length
  if (!count) return visualRadius.value
  const totalArc = beads.value.reduce((s, b) => s + getBeadArcSizeRpx(b), 0)
  const minR = totalArc / (2 * Math.PI)
  return Math.max(visualRadius.value, minR)
})

const layoutRadius = computed(() => {
  return Math.max(rawLayoutRadius.value, maxRadiusHistory.value)
})

watch(rawLayoutRadius, (val) => {
  if (val > maxRadiusHistory.value) {
    maxRadiusHistory.value = val
  }
})

watch(selectedSize, () => {
  maxRadiusHistory.value = 0
})

// 动态缩放画布，确保大尺寸也能完整显示
const canvasScale = computed(() => {
  const maxR = 250 // 最大安全半径 (留出30rpx边距)
  const maxBeadSize = beads.value.reduce((m, b) => Math.max(m, getBeadArcSizeRpx(b)), 0)
  const safeR = layoutRadius.value + maxBeadSize / 2
  
  let autoScale = 1
  if (safeR > maxR) {
    autoScale = maxR / safeR
  }
  
  // 结合手动缩放
  return autoScale * manualScale.value
})

// 计算珠子布局
const beadLayouts = computed(() => {
  const count = beads.value.length
  if (!count) return []
  
  const radius = layoutRadius.value
  const layouts = []
  
  // 计算所有珠子的总弧长（不含间隙）
  let totalBeadArc = 0
  beads.value.forEach(b => {
    const s = getBeadArcSizeRpx(b)
    totalBeadArc += s
  })
  
  // 计算圆周长
  const circumference = 2 * Math.PI * radius
  
  // 确定间隙
  let gap = 0 // 默认间隙设为0，由 visualRadius 保证不重叠
  let startAngle = -Math.PI / 2 // 默认从顶部开始
  
  if (isAutoArranged.value) {
    // 均匀分布模式
    const remainingArc = circumference - totalBeadArc
    
    // 计算有效间隙数量：只有 珠子-珠子 之间才分配间隙
    // 吊坠前后都紧贴，不分配间隙
    let validGapCount = 0
    
    if (count > 0) {
        for (let i = 0; i < count; i++) {
            const curr = beads.value[i]
            const next = beads.value[(i + 1) % count]
            // 如果当前和下一个都不是吊坠，则需要分配间隙
            if (!isPendant(curr) && !isPendant(next)) {
                validGapCount++
            }
        }
    }
    
    if (validGapCount > 0) {
        gap = remainingArc / validGapCount
    } else {
        gap = 0
    }
  }
  
  // 布局计算
  let currentAngle = startAngle
  
  for (let i = 0; i < count; i++) {
    if (i === 0) {
      layouts.push({ angle: currentAngle })
    } else {
      const prevBead = beads.value[i-1]
      const currBead = beads.value[i]
      const prevSize = getBeadArcSizeRpx(prevBead)
      const currSize = getBeadArcSizeRpx(currBead)
      
      // 判断是否需要添加间隙
      // 只有当前一个不是吊坠，且当前也不是吊坠时，才应用 gap
      const applyGap = (!isPendant(prevBead) && !isPendant(currBead)) ? gap : 0

      // 累加角度
      // 使用弧长计算角度：arc = r1 + r2 + gap
      const arc = (prevSize / 2) + (currSize / 2) + applyGap
      const theta = arc / radius
      currentAngle += theta
      layouts.push({ angle: currentAngle })
    }
  }
  
  return layouts
})

// 检查容量（防止重叠超过一圈）
function checkCapacity(newItem) {
  // 最大周长 (mm) - 严格按照表格规格 (手围 + 2.4cm)
  const maxMm = (selectedSize.value + 2.4) * 10
  
  // 当前总长 (mm)
  let currentMm = 0
  beads.value.forEach(b => {
    // 吊坠也占长度 (3mm)
    if (isPendant(b)) {
      currentMm += 3
    } else {
      currentMm += Number(b.size || 8)
    }
  })
  
  // 新增的珠子
  let addSize = 0
  if (isPendant(newItem)) {
    addSize = 3
  } else {
    addSize = Number(newItem.size || 8)
  }
  
  // 允许一定的误差（例如 25mm，约3颗珠子），防止因历史数据轻微超载导致无法操作
  // 只要不是恶意堆叠，稍微超一点点是可以接受的
  if (currentMm + addSize > maxMm + 25) {
    // 调试用：打印详细容量信息
    console.log(`[Capacity Check Failed] Current: ${currentMm}mm, Adding: ${addSize}mm, Total: ${currentMm + addSize}mm`)
    console.log(`[Capacity Limit] SelectedSize: ${selectedSize.value}, MaxMm: ${maxMm}, Threshold: ${maxMm + 25}`)
    return false
  }
  console.log(`[Capacity Check Passed] Total: ${currentMm + addSize}mm <= Threshold: ${maxMm + 25}mm`)
  return true
}

// 珠子样式 - 适应动态半径
function getBeadStyle(bead, index) {
  const count = beads.value.length
  if (!count) return ''
  
  const layout = beadLayouts.value[index]
  if (!layout) return ''
  
  const size = Number(bead.size || 8) * 7
  const radius = layoutRadius.value
  const angle = layout.angle
  
  // 280 是画布中心 (560/2)
  const pendant = isPendant(bead)
  const placeRadius = pendant ? (radius + size / 2) : radius
  const cx = 280 + placeRadius * Math.cos(angle) - size / 2
  const cy = 280 + placeRadius * Math.sin(angle) - size / 2
  
  const rotationDeg = pendant
    ? (angle - Math.PI / 2) * (180 / Math.PI)
    : (angle + Math.PI / 2) * (180 / Math.PI)
  
  let color = bead.color || '#e8e8e8'
  if (color.includes('gradient')) {
    const m = color.match(/#[0-9a-fA-F]{6}/)
    if (m) color = m[0]
  }

  // 如果有图片且未加载失败，背景设为透明
  if (bead.imageUrl && !bead.loadFailed) {
    color = 'transparent'
  }
  
  const scaleX = bead.mirrored ? -1 : 1
  const scale = draggingIndex.value === index ? 1.15 : 1
  
  // 大尺寸材料放在底层但仍高于logo和绳子 (logo z-index=0, rope z-index=2)
  // 普通材料放在顶层 (z-index=10)
  // 阈值：24mm * 7 = 168rpx
  const zIndex = size >= 168 ? 3 : 10
  
  return `left:${cx}rpx;top:${cy}rpx;width:${size}rpx;height:${size}rpx;background:${color};transform:rotate(${rotationDeg}deg) scaleX(${scaleX}) scale(${scale});z-index:${zIndex};`
}

// 切换排列模式
function toggleAutoArrange() {
  if (!beads.value.length) return
  vibrate()
  isAutoArranged.value = !isAutoArranged.value
  uni.showToast({ 
    title: isAutoArranged.value ? '已均匀排列' : '已紧凑排列', 
    icon: 'none' 
  })
}

function toggleToolbar() {
  vibrate()
  isToolbarCollapsed.value = !isToolbarCollapsed.value
}

function onToolbarContainerClick() {
  if (isToolbarCollapsed.value) toggleToolbar()
}

// 添加珠子 - 立即添加 + 原图飞跃特效
function addBead(g, event) {
  if (g.stock === 0) {
    uni.showToast({ title: '暂时缺货', icon: 'none' })
    return
  }

  if (!checkCapacity(g)) {
    uni.showToast({ title: '已达到当前手围的最大容量', icon: 'none' })
    return
  }

  vibrate()
  clickedId.value = g.id

  // 扣减库存
  if (g.stock > 0) {
    g.stock--
  }

  // 立即添加珠子到数组
  beads.value.push({
    _id: `b_${++beadIdCounter}`,
    productId: g.id,
    title: g.title,
    price: g.price,
    size: g.size,
    color: g.color,
    imageUrl: g.imageUrl,
    loadFailed: false,
    isNew: true
  })

  // 动画结束后清除 isNew 标记
  setTimeout(() => {
    const bead = beads.value.find(b => b._id === `b_${beadIdCounter}`)
    if (bead) bead.isNew = false
  }, 850)

  // ---- 原图飞跃特效（视觉装饰，不阻塞数据） ----
  // 获取点击的卡片元素位置作为起飞点
  let startX = '50vw'
  let startY = '80vh'

  if (event && event.currentTarget) {
    // 尝试通过 event 获取元素位置
    const target = event.currentTarget
    if (target && target.offsetLeft !== undefined) {
      startX = target.offsetLeft + 'px'
      startY = target.offsetTop + 'px'
    }
  }

  // 获取画布中心作为降落点
  const query = uni.createSelectorQuery().in(instance)
  query.select('.canvas').boundingClientRect(data => {
    let endX = '50vw'
    let endY = '40vh'
    if (data) {
      endX = (data.left + data.width / 2) + 'px'
      endY = (data.top + data.height / 2) + 'px'
    }

    const color = g.color || '#e8e8e8'
    const imgUrl = g.imageUrl || ''

    // 阶段1：在卡片位置出现
    flyingBead.value = { color, imageUrl: imgUrl }
    flyingBeadStyle.value = `
      left: ${startX};
      top: ${startY};
      opacity: 1;
      transform: translate(-50%, -50%) scale(1);
      transition: none;
    `

    // 阶段2：飞向画布中心并淡出
    const beadSize = Math.max(Number(g.size || 8) * 4, 32)
    setTimeout(() => {
      flyingBeadStyle.value = `
        left: ${endX};
        top: ${endY};
        opacity: 0;
        transform: translate(-50%, -50%) scale(0.8);
        transition: all 0.55s cubic-bezier(0.25, 0.1, 0.25, 1);
      `
    }, 30)

    // 阶段3：动画结束清理
    setTimeout(() => {
      flyingBead.value = null
      flyingBeadStyle.value = ''
    }, 600)
  }).exec()
}

// 预览商品实物图
function previewProductImage(g, event) {
  if (event) event.stopPropagation?.()
  vibrate()
  previewProduct.value = g
  previewImageUrl.value = g.imageUrl || ''
  showImagePreview.value = true
}

function previewFullImage() {
  if (previewImageUrl.value) {
    uni.previewImage({ urls: [previewImageUrl.value] })
  }
}

function updateStock(productId, delta) {
  const g = goods.value.find(x => x.id === productId)
  if (g) g.stock += delta
  
  // 同时更新 allGoods 中的库存（如果存在）
  const ag = allGoods.value.find(x => x.id === productId)
  if (ag && ag !== g) ag.stock += delta
}

function onBeadImageError(bead, index) {
  console.error('Bead image load failed:', bead.imageUrl)
  bead.loadFailed = true
}

// 珠子拖动状态
const dragStartPos = ref({ x: 0, y: 0 })
const dragTargetIndex = ref(-1)
const canvasRect = ref(null)
const beadTouchStartPos = ref({ x: 0, y: 0 })
const beadTouchActive = ref(false)
const dragThreshold = 8 // 像素阈值，超过此值进入拖动模式
const beadStartIndex = ref(-1) // 记录开始拖动的珠子索引

// 判断触摸点是否在画布圆外（删除区域）
function checkInDeleteZone(touch) {
  if (!canvasRect.value) return false
  const rect = canvasRect.value
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 2
  const dx = touch.clientX - centerX
  const dy = touch.clientY - centerY
  const distance = Math.sqrt(dx * dx + dy * dy)
  // 画布半径约为 rect.width/2，超出80%即认为是删除区域
  const canvasRadius = (rect.width / 2) * 0.85
  return distance > canvasRadius
}

// 珠子触摸开始 - 直接触摸即可开始拖动
function onBeadTouchStart(i, e) {
  if (swapMode.value) {
    // 交换模式下，点击处理交换
    if (firstSwapIndex.value === i) {
      swapMode.value = false
      firstSwapIndex.value = -1
      vibrate()
      uni.showToast({ title: '已取消', icon: 'none', duration: 800 })
    } else {
      swapBeads(firstSwapIndex.value, i)
      swapMode.value = false
      firstSwapIndex.value = -1
      vibrate()
      uni.showToast({ title: '已交换位置', icon: 'success', duration: 800 })
    }
    return
  }

  if (beads.value.length < 2) {
    // 只有一颗珠子，不支持拖动，点击显示菜单
    return
  }

  beadTouchActive.value = true
  beadStartIndex.value = i
  const touch = e.touches[0]
  beadTouchStartPos.value = { x: touch.clientX, y: touch.clientY }

  // 更新画布位置信息
  const query = uni.createSelectorQuery().in(instance)
  query.select('.canvas').boundingClientRect(data => {
    if (data) canvasRect.value = data
  }).exec()
}

// 珠子触摸移动
function onBeadTouchMove(i, e) {
  if (!beadTouchActive.value) return

  const touch = e.touches[0]
  const dx = touch.clientX - beadTouchStartPos.value.x
  const dy = touch.clientY - beadTouchStartPos.value.y
  const distance = Math.sqrt(dx * dx + dy * dy)

  // 超过阈值进入拖动模式
  if (distance > dragThreshold && !isDragging.value) {
    isDragging.value = true
    draggingIndex.value = beadStartIndex.value
    vibrate()
  }

  if (isDragging.value && draggingIndex.value >= 0) {
    // 检查是否在删除区域
    isInDeleteZone.value = checkInDeleteZone(touch)

    // 如果不在删除区域，检测最近的目标位置用于交换
    if (!isInDeleteZone.value && canvasRect.value) {
      const rect = canvasRect.value
      const centerX = rect.left + rect.width / 2
      const centerY = rect.top + rect.height / 2
      const tdx = touch.clientX - centerX
      const tdy = touch.clientY - centerY
      const angle = Math.atan2(tdy, tdx)

      let normalizedAngle = angle + Math.PI / 2
      if (normalizedAngle < 0) normalizedAngle += 2 * Math.PI

      let targetIndex = -1
      let minDiff = Infinity

      beadLayouts.value.forEach((layout, idx) => {
        let layoutNorm = layout.angle + Math.PI / 2
        layoutNorm = layoutNorm % (2 * Math.PI)
        if (layoutNorm < 0) layoutNorm += 2 * Math.PI

        let diff = Math.abs(layoutNorm - normalizedAngle)
        if (diff > Math.PI) diff = 2 * Math.PI - diff

        if (diff < minDiff) {
          minDiff = diff
          targetIndex = idx
        }
      })

      if (targetIndex >= 0 && targetIndex !== draggingIndex.value) {
        swapBeads(draggingIndex.value, targetIndex)
        draggingIndex.value = targetIndex
        vibrate()
      }
    }
  }
}

// 珠子触摸结束
function onBeadTouchEnd(i, e) {
  if (isDragging.value && draggingIndex.value >= 0) {
    // 检查是否需要删除（在删除区域松手）
    if (isInDeleteZone.value) {
      const removed = beads.value[draggingIndex.value]
      // 添加移除动画
      if (removed) {
        removed.removing = true
        vibrate()
        setTimeout(() => {
          beads.value.splice(draggingIndex.value, 1)
          if (removed.productId) {
            updateStock(removed.productId, 1)
          }
        }, 250)
      }
      uni.showToast({ title: '已删除', icon: 'none', duration: 800 })
    }

    isDragging.value = false
    draggingIndex.value = -1
    dragTargetIndex.value = -1
    isInDeleteZone.value = false

    // 延迟重置 beadTouchActive，防止立即触发 click
    setTimeout(() => {
      beadTouchActive.value = false
    }, 100)
    return
  }

  // 没有拖动，作为点击处理
  beadTouchActive.value = false

  if (swapMode.value) return // already handled in touchstart

  // 显示操作菜单
  const bead = beads.value[i]
  if (!bead) return
  const mirrorText = bead.mirrored ? '取消镜像' : '镜像翻转'
  uni.showActionSheet({
    itemList: ['移除此珠', '调整位置', mirrorText],
    success: (res) => {
      if (res.tapIndex === 0) {
        vibrate()
        bead.removing = true
        setTimeout(() => {
          const idx = beads.value.findIndex(b => b._id === bead._id)
          if (idx >= 0) {
            const removed = beads.value[idx]
            beads.value.splice(idx, 1)
            if (removed && removed.productId) {
              updateStock(removed.productId, 1)
            }
          }
        }, 250)
        uni.showToast({ title: '已移除', icon: 'none', duration: 800 })
      } else if (res.tapIndex === 1) {
        swapMode.value = true
        firstSwapIndex.value = i
        vibrate()
        uni.showToast({ title: '点击另一颗珠子交换位置', icon: 'none', duration: 1500 })
      } else if (res.tapIndex === 2) {
        toggleBeadMirror(i)
        vibrate()
        uni.showToast({ title: bead.mirrored ? '已取消镜像' : '已镜像', icon: 'none', duration: 800 })
      }
    }
  })
}

// 画布触摸事件（处理双指缩放 + 全局拖拽目标检测）
function onTouchStart(e) {
  // 更新画布位置
  const query = uni.createSelectorQuery().in(instance)
  query.select('.canvas').boundingClientRect(data => {
    if (data) canvasRect.value = data
  }).exec()

  // 双指缩放
  if (e.touches.length === 2) {
    const t1 = e.touches[0]
    const t2 = e.touches[1]
    const dx = t2.clientX - t1.clientX
    const dy = t2.clientY - t1.clientY
    initialPinchDistance.value = Math.sqrt(dx * dx + dy * dy)
    startManualScale.value = manualScale.value
    return
  }
}

function onTouchMove(e) {
  // 双指缩放
  if (e.touches.length === 2 && initialPinchDistance.value > 0) {
    const t1 = e.touches[0]
    const t2 = e.touches[1]
    const dx = t2.clientX - t1.clientX
    const dy = t2.clientY - t1.clientY
    const currentDist = Math.sqrt(dx * dx + dy * dy)

    const scaleRatio = currentDist / initialPinchDistance.value
    let newScale = startManualScale.value * scaleRatio

    if (newScale < 0.5) newScale = 0.5
    if (newScale > 3.0) newScale = 3.0

    manualScale.value = newScale
    return
  }
}

function onTouchEnd(e) {
  if (e.touches.length < 2) {
    initialPinchDistance.value = 0
  }
}

// 辅助功能
function swapBeads(i, j) {
  if (i < 0 || j < 0 || i >= beads.value.length || j >= beads.value.length) return
  const temp = beads.value[i]
  beads.value[i] = beads.value[j]
  beads.value[j] = temp
}

function toggleBeadMirror(i) {
  const bead = beads.value[i]
  bead.mirrored = !bead.mirrored
}

function mirrorAllBeads() {
  if (!beads.value.length) return
  vibrate()
  const anyMirrored = beads.value.some(b => b.mirrored)
  beads.value.forEach(b => b.mirrored = !anyMirrored)
  uni.showToast({ 
    title: anyMirrored ? '已全部取消镜像' : '已全部镜像', 
    icon: 'none' 
  })
}

function undoLastBead() {
  if (!beads.value.length) return
  vibrate()
  const removed = beads.value.pop()
  if (removed && removed.productId) {
    updateStock(removed.productId, 1)
  }
}

function confirmClear() {
  if (!beads.value.length) return
  uni.showModal({
    title: '确认清空',
    content: '确定要清空所有珠子吗？',
    success: (res) => {
      if (res.confirm) {
        // 恢复所有库存
        beads.value.forEach(b => {
          if (b.productId) updateStock(b.productId, 1)
        })
        beads.value = []
        maxRadiusHistory.value = 0 // 重置绳子记忆半径
        vibrate()
      }
    }
  })
}

// 切换不再提示
function toggleDontShowHelp() {
  dontShowHelpAgain.value = !dontShowHelpAgain.value
  // 实时更新缓存状态
  if (dontShowHelpAgain.value) {
    uni.setStorageSync('dontShowHelp', 'true')
  } else {
    uni.removeStorageSync('dontShowHelp')
  }
}

// 关闭帮助弹窗
function closeHelpPopup() {
  showHelpPopup.value = false
  // 缓存逻辑已移至 toggleDontShowHelp 实时处理
}

// 显示提示
function showTips() {
  // 打开时同步缓存状态
  const hide = uni.getStorageSync('dontShowHelp')
  dontShowHelpAgain.value = !!hide
  showHelpPopup.value = true
}

function vibrate() {
  uni.vibrateShort()
}

// API交互
async function initData() {
  try {
    const catRes = await designCategoryList()
    // 确保 categories 是数组
    // 接口现在返回 [{ keyCode, name, children: [] }]
    categories.value = Array.isArray(catRes) ? catRes : (catRes.data || [])
    
    // 清空旧数据
    colorSeries.value = []
    colorSeriesAll.value = []
    
    if (categories.value.length > 0) {
      // 默认选中第一个非绳子分类
      // 优先选中 'main_bead'，如果没有则选中第一个非rope
      const mainBead = categories.value.find(c => c.keyCode === 'main_bead')
      const firstValid = categories.value.find(c => c.keyCode !== 'rope')
      
      if (mainBead) {
        activeCategory.value = mainBead.keyCode
      } else if (firstValid) {
        activeCategory.value = firstValid.keyCode
      } else {
        activeCategory.value = categories.value[0].keyCode
      }
    }
    loadProducts()
  } catch (e) {
    console.error('初始化设计台数据失败:', e)
  }
}

function ensureInit() {
  // 每次显示页面时检查是否需要弹出操作指南
  try {
    const hide = uni.getStorageSync('dontShowHelp')
    if (!hide && !showHelpPopup.value) {
      setTimeout(() => {
        // 再次检查，避免在延时期间用户已经离开或手动打开
        if (!showHelpPopup.value) {
            showHelpPopup.value = true
        }
      }, 500)
    }
  } catch (e) {
    console.error('Storage error:', e)
  }

  if (didInit.value) return
  didInit.value = true

  nextTick(() => {
    initData()
  })
}

// 切换分类
function switchCategory(key) {
  if (activeCategory.value === key) return
  activeCategory.value = key
  activeColor.value = '' // 切换分类时重置子分类
  
  // 重置状态
  resetProductListView()
  page.value = 1
  totalPages.value = 1
  goods.value = []
  allGoods.value = []
  loadProducts()
}

// 切换子分类 (原 switchColor)
function switchColor(key) {
  const normalizedKey = normalizeFilterValue(key)
  if (normalizedKey === '') {
    activeColor.value = ''
  } else if (activeColor.value === normalizedKey) {
    // 再次点击取消选中? 还是保持? 
    // 通常点击已选中的可以取消，或者不做操作。这里保持 toggle 逻辑
    activeColor.value = '' // 取消选中
  } else {
    activeColor.value = normalizedKey
  }

  resetProductListView()
  page.value = 1
  totalPages.value = 1
  goods.value = []
  allGoods.value = []
  loadProducts()
}

// 废弃的方法，保留空实现以防模板报错
function switchClassification(key, id) {}

// 加载商品
async function loadProducts(isLoadMore = false) {
  const requestSeq = ++productRequestSeq
  
  // 如果是加载更多且正在使用前端分页
  if (isLoadMore && allGoods.value.length > 0) {
      const start = (page.value - 1) * pageSize.value
      const end = start + pageSize.value
      const nextBatch = allGoods.value.slice(start, end)
      if (nextBatch.length) {
          goods.value.push(...nextBatch)
      }
      loading.value = false
      return
  }

  if (isLoadMore && page.value > totalPages.value) return

  loading.value = true
  try {
    const params = {
      categories: activeCategory.value ? [activeCategory.value] : [],
      colorSeries: activeColor.value ? [activeColor.value] : [],
      page: page.value,
      size: pageSize.value
    }
    
    // 不再传递 classificationDetailKey1-8，因为 API 已经整合

    const res = await designProductList(params)
    if (requestSeq !== productRequestSeq) return
    
    // 处理返回数据
    // 预期 res 是一个数组(materials) 或者包含 materials 的对象
    // api.js 的 getDiyMaterialList 已经处理了大部分解构逻辑，返回的是 materials 数组 (带有 totalPages 属性)
    
    const list = Array.isArray(res) ? res : []
    const total = res.totalPages || 0
    
    const processedList = list.map(item => {
      // 确保图片路径完整
      let imageUrl = item.imageUrl || item.image || ''
      imageUrl = resolveImageUrl(imageUrl)
      
      // 尝试对中文路径进行编码
      if (imageUrl && !imageUrl.startsWith('data:')) {
        try {
          imageUrl = encodeURI(imageUrl)
        } catch (e) {}
      }
      
      // 确保有 color 字段
      if (!item.color) item.color = '#f5f5f5'
      return { ...item, imageUrl, loaded: false }
    })

    if (isLoadMore) {
      goods.value = [...goods.value, ...processedList]
      if (total) totalPages.value = total
    } else {
      // 第一次加载
      // 检查是否需要前端分页 (如果后端没分页但返回了大量数据)
      if (total === 0 && processedList.length > pageSize.value) {
          allGoods.value = processedList
          totalPages.value = Math.ceil(processedList.length / pageSize.value)
          goods.value = processedList.slice(0, pageSize.value)
      } else {
          allGoods.value = []
          goods.value = processedList
          if (total) {
            totalPages.value = total
          } else {
             // 估算
             totalPages.value = processedList.length < pageSize.value ? page.value : page.value + 1
          }
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function onProductScrollToLower() {
  if (Date.now() < suppressScrollToLowerUntil) return
  if (page.value < totalPages.value && !loading.value) {
    page.value++
    loadProducts(true)
  }
}

// 尺寸选择
function selectSize(s) {
  selectedSize.value = s
  uni.setStorageSync('diy_selected_size', s)
}

function confirmSize() {
  showSizeSelector.value = false
  showInitGuide.value = false
}

function showPreview() {
  if (!beads.value.length) return
  showOrderPreview.value = true
}

function startDesign() {
  confirmSize()
}

// 生成设计图 (Canvas 2D)
async function generateDesignImage() {
  uni.showLoading({ title: '生成设计图中...', mask: true })
  
  try {
    // 1. 获取 Canvas 节点
    const { canvas, width: canvasWidth, height: canvasHeight } = await new Promise((resolve, reject) => {
      const query = uni.createSelectorQuery().in(instance)
      query.select('#exportCanvas')
        .fields({ node: true, size: true })
        .exec((res) => {
          if (res[0] && res[0].node) {
            resolve({
                canvas: res[0].node,
                width: res[0].width,
                height: res[0].height
            })
          } else {
            // reject(new Error('未找到 Canvas 节点'))
            // 降级处理：如果没有Canvas（可能被暂时屏蔽），则跳过生成
            console.warn('Canvas not found or disabled')
            resolve({ canvas: null })
          }
        })
    })

    if (!canvas) {
        // 返回 Logo 路径作为替补，确保流程走通
        return logoPath
    }

    const ctx = canvas.getContext('2d')
    
    // 获取设备像素比和窗口信息
    let dpr = 2
    try {
        const info = uni.getWindowInfo ? uni.getWindowInfo() : uni.getSystemInfoSync()
        dpr = info.pixelRatio || 2
    } catch (e) {
        console.warn('获取系统信息失败', e)
    }
    
    // 设置画布物理尺寸 (解决模糊问题)
    canvas.width = canvasWidth * dpr
    canvas.height = canvasHeight * dpr
    
    // 缩放上下文，使得后续绘图可以使用逻辑像素
    ctx.scale(dpr, dpr)
    
    // rpx to px 比例 (基于 750 设计稿)
    const ratio = canvasWidth / 560 // 560rpx是CSS设置的宽度
    const r2p = (rpx) => rpx * ratio
    
    // 逻辑宽高
    const width = canvasWidth
    const height = canvasHeight
    const centerX = width / 2
    const centerY = height / 2
    
    // 辅助函数：加载图片
    const loadImage = (src) => {
      return new Promise((resolve) => {
        const img = canvas.createImage()
        img.src = src
        img.onload = () => resolve(img)
        img.onerror = (e) => {
          console.error('Canvas图片加载失败:', src, e)
          resolve(null)
        }
      })
    }

    // 2. 绘制背景
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, width, height)
    
    // 应用整体缩放
    const scale = canvasScale.value
    const ropeRadius = r2p(visualRadius.value)
    const beadRadius = r2p(layoutRadius.value)
    
    ctx.save()
    ctx.translate(centerX, centerY)
    ctx.scale(scale, scale)
    ctx.translate(-centerX, -centerY)
    
    // 3. 准备珠子图片 (提前加载)
    const beadList = beads.value
    const beadImages = new Array(beadList.length).fill(null)
    
    // 辅助函数：获取图片路径
    let hasDomainError = false
    const getImagePath = async (url) => {
      if (!url) return null
      return url
    }

    const BATCH_SIZE = 6
    for (let i = 0; i < beadList.length; i += BATCH_SIZE) {
        const batch = beadList.slice(i, i + BATCH_SIZE).map((b, idx) => ({ b, originalIndex: i + idx }))
        
        await Promise.all(batch.map(async ({ b, originalIndex }) => {
            if (!b.imageUrl) return
            
            const path = await getImagePath(b.imageUrl)
            if (path) {
                const img = await loadImage(path)
                if (img) {
                    beadImages[originalIndex] = {
                        img,
                        aspectRatio: img.height / img.width
                    }
                }
            }
        }))
    }
    
    if (hasDomainError) {
        uni.showToast({
            title: '图片加载失败：请在小程序后台配置 downloadFile 域名',
            icon: 'none',
            duration: 3000
        })
    }
    
    // 绘制单个珠子的函数
    const drawBead = (b, i, layout) => {
        const angle = layout.angle
        const beadSizeRpx = Number(b.size || 8) * 7
        const beadSizePx = r2p(beadSizeRpx)
        
        const pendant = isPendant(b)
        const placeRadius = pendant ? (beadRadius + beadSizePx / 2) : beadRadius
        const bx = centerX + placeRadius * Math.cos(angle)
        const by = centerY + placeRadius * Math.sin(angle)
        
        ctx.save()
        ctx.translate(bx, by)
        ctx.rotate(pendant ? (angle - Math.PI / 2) : (angle + Math.PI / 2))
        
        if (b.mirrored) {
          ctx.scale(-1, 1)
        }
        
        const imgInfo = beadImages[i]
        if (imgInfo && imgInfo.img) {
          let drawWidth = beadSizePx
          let drawHeight = beadSizePx
          if (imgInfo.aspectRatio) {
            drawHeight = drawWidth * imgInfo.aspectRatio
          }
          ctx.drawImage(imgInfo.img, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight)
        } else {
          ctx.beginPath()
          ctx.arc(0, 0, beadSizePx / 2, 0, 2 * Math.PI)
          ctx.fillStyle = b.color || '#e8e8e8'
          ctx.fill()
        }
        ctx.restore()
    }
    
    // 4. 绘制底层珠子 (大尺寸 >= 24mm)
    for (let i = 0; i < beadList.length; i++) {
      const b = beadList[i]
      const layout = beadLayouts.value[i]
      if (!layout) continue
      
      const sizeMm = Number(b.size || 8)
      if (sizeMm >= 24) {
          drawBead(b, i, layout)
      }
    }

    // 5. 绘制绳子
    ctx.beginPath()
    ctx.strokeStyle = '#e0e0e0'
    ctx.lineWidth = r2p(2)
    ctx.setLineDash([])
    ctx.arc(centerX, centerY, ropeRadius, 0, 2 * Math.PI)
    ctx.stroke()
    
    // 6. 绘制 Logo
    const logoSize = r2p(200)
    try {
      // 尝试加载 Logo
      const logoImg = await loadImage(logoPath)
      if (logoImg) {
        ctx.drawImage(logoImg, centerX - logoSize / 2, centerY - logoSize / 2, logoSize, logoSize)
      } else {
        throw new Error('Logo load failed')
      }
    } catch (e) {
      // 降级绘制
      ctx.beginPath()
      ctx.fillStyle = '#f7f7f7'
      ctx.arc(centerX, centerY, logoSize / 2, 0, 2 * Math.PI)
      ctx.fill()
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillStyle = '#bfbfbf'
      ctx.font = `${r2p(24)}px sans-serif`
      ctx.fillText('满彩珠宝', centerX, centerY)
    }
    
    // 7. 绘制顶层珠子 (普通尺寸 < 24mm)
    for (let i = 0; i < beadList.length; i++) {
      const b = beadList[i]
      const layout = beadLayouts.value[i]
      if (!layout) continue
      
      const sizeMm = Number(b.size || 8)
      if (sizeMm < 24) {
          drawBead(b, i, layout)
      }
    }
    
    ctx.restore() // 恢复全局缩放
    
    // 8. 导出图片
    return new Promise((resolve, reject) => {
      setTimeout(() => { // 稍作延迟确保绘制完成
        uni.canvasToTempFilePath({
          canvas: canvas, // Canvas 2D 必须传实例
          destWidth: width * dpr, // 高清导出
          destHeight: height * dpr,
          fileType: 'jpg',
          quality: 0.9,
          success: (res) => resolve(res.tempFilePath),
          fail: (err) => reject(err)
        })
      }, 100)
    })
    
  } catch (e) {
    uni.hideLoading()
    console.error('Generate Image Error:', e)
    
    let errorDetail = ''
    if (e && e.message) errorDetail += `错误: ${e.message}\n`
    if (e && e.errMsg) errorDetail += `Msg: ${e.errMsg}\n`
    if (JSON.stringify(e) !== '{}') errorDetail += `Detail: ${JSON.stringify(e)}`
    
    uni.showModal({
        title: '生成图片失败',
        content: errorDetail || '未知错误，请重试',
        showCancel: false
    })
    
    throw e
  }
}

// 提交订单
async function submitOrder() {
  if (loading.value) return
  
  try {
    // Generate & Upload Image
    const tempFilePath = await generateDesignImage()
    
    uni.showLoading({ title: '上传设计图...' })
    const uploadRes = await uploadFile(tempFilePath, 'diy_design')
    console.log('Design upload result:', uploadRes)
    
    // 获取返回的图片路径
    let rawPath = ''
    if (typeof uploadRes === 'string') {
        rawPath = uploadRes
    } else if (uploadRes) {
        // 尝试多种可能的字段名
        rawPath = uploadRes.url || uploadRes.path || uploadRes.fileUrl || uploadRes.fileName || ''
        if (!rawPath && uploadRes.data) {
             rawPath = uploadRes.data.url || uploadRes.data.path || ''
        }
    }
    
    console.log('Extracted raw path:', rawPath)

    // 确保是完整的URL
    let designUrl = ''
    if (rawPath) {
        // 使用 resolveImageUrl 统一处理，它会自动判断是否需要添加 /admin/common/image 前缀
        designUrl = resolveImageUrl(rawPath)
    }
    
    console.log('Final design URL:', designUrl)
    
    uni.hideLoading()
    
    // 生成详细的珠子排列描述
    let beadDescription = ''
    if (beads.value.length > 0) {
        beadDescription = '顺时针从顶部到最后一颗描述为：'
        beads.value.forEach((b, index) => {
            const num = index + 1
            const name = b.name || b.title || '未知珠子'
            const size = b.size ? `(${b.size}mm)` : ''
            beadDescription += `第${num}颗：${name}${size}`
            if (index < beads.value.length - 1) {
                beadDescription += '，'
            }
        })
    }

    // Store Data
    const orderData = {
        items: orderItems.value,
        designImage: designUrl,
        categories: activeCategory.value,
        classificationDetail1: null, // 废弃
        classificationDetail2: null, // 废弃
        beadDescription: beadDescription // 新增珠子描述
    }
    
    // 如果有选中的色系，可以作为 classificationDetail1 保存，以便后台参考
    if (activeColor.value) {
        orderData.classificationDetail1 = activeColor.value
    }
    
    uni.setStorageSync('diy_order_data', orderData)
    uni.setStorageSync('diy_order_items', orderItems.value) // Keep compatible
    
    uni.navigateTo({
      url: `/pages/order/confirm?mode=diy&size=${selectedSize.value}`
    })
    showOrderPreview.value = false
    
  } catch (e) {
    uni.hideLoading()
    const msg = e.message || '处理失败'
    uni.showToast({ title: msg.length > 15 ? '处理失败，请查看控制台' : msg, icon: 'none' })
    console.error('Submit Order Error:', e)
  }
}

// 批量加入购物车
async function addDiyToCart() {
  if (!beads.value.length) {
    uni.showToast({ title: '请先添加珠子', icon: 'none' })
    return
  }

  // 统计每个商品的数量
  const map = {}
  beads.value.forEach(b => {
    if (b.productId) {
      map[b.productId] = (map[b.productId] || 0) + 1
    }
  })

  const productIds = Object.keys(map)
  if (!productIds.length) {
    uni.showToast({ title: '没有有效的商品可添加', icon: 'none' })
    return
  }

  uni.showLoading({ title: '正在加入购物车...', mask: true })

  try {
    // 并发调用添加接口
    // 注意：如果商品数量很多，并发过高可能导致问题，这里假设数量有限
    const promises = productIds.map(pid => addToCart(Number(pid), map[pid]))
    
    await Promise.all(promises)
    
    uni.showToast({ title: '已全部加入购物车', icon: 'success' })
    showOrderPreview.value = false
    
    // 更新购物车角标
    updateCartBadgeNow()
    
  } catch (e) {
    console.error('批量添加购物车失败:', e)
    uni.showToast({ title: '部分商品添加失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

// ==================== AI算命 ====================
function handleAiRecommend() {
  const prompt = aiPrompt.value.trim()
  if (!prompt) return
  aiLoading.value = true

  aiRecommend(prompt, aiUseTarot.value)
    .then(res => {
      aiResult.value = res
      showAiModal.value = false
      showAiResult.value = true
    })
    .catch(e => {
      console.error('AI推荐失败:', e)
      uni.showToast({ title: e.msg || e.message || '推荐失败', icon: 'none' })
    })
    .finally(() => {
      aiLoading.value = false
    })
}

function applyAiDesign() {
  if (!aiResult.value || !aiResult.value.materials || !aiResult.value.materials.length) return

  const materials = aiResult.value.materials

  // 清空现有珠子，恢复库存
  if (beads.value.length) {
    beads.value.forEach(b => {
      if (b.productId) updateStock(b.productId, 1)
    })
    beads.value = []
    maxRadiusHistory.value = 0
  }

  // 按 position 排序：主珠在前
  const order = { '主珠': 0, '配珠': 1, '绳子': 2 }
  const sorted = [...materials].sort((a, b) => {
    return (order[a.position] ?? 9) - (order[b.position] ?? 9)
  })

  // 匹配商品列表获取价格和尺寸
  const findProduct = (id) => goods.value.find(g => g.id === id) || allGoods.value.find(g => g.id === id)

  let addedCount = 0
  sorted.forEach(m => {
    const qty = m.quantity || 1
    for (let i = 0; i < qty; i++) {
      const matched = findProduct(m.materialId)
      const price = matched ? Number(matched.price) : 0
      const size = matched ? Number(matched.size) : 8
      const imageUrl = resolveImageUrl(m.imageUrl || (matched && matched.imageUrl) || '')
      const color = (matched && matched.color) || '#f5f5f5'
      const title = (matched && matched.title) || m.title || '推荐材料'

      // 扣减库存
      if (matched && matched.stock > 0) {
        matched.stock--
      }

      beads.value.push({
        _id: `b_${++beadIdCounter}`,
        productId: m.materialId,
        title,
        price,
        size,
        color,
        imageUrl,
        loadFailed: false,
        isNew: true
      })
      addedCount++
    }
  })

  // 动画结束后清除 isNew 标记
  setTimeout(() => {
    beads.value.forEach(b => { b.isNew = false })
  }, 900)

  showAiResult.value = false
  uni.showToast({ title: `已应用 ${addedCount} 颗珠子`, icon: 'success' })
}

onMounted(() => {
  console.log('Design Page Mounted')
  isMounted.value = true
  ensureInit()
})

onShow(() => {
  ensureInit()
})
</script>

<style lang="scss">
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #fff;
}

/* 顶部 Header */
.header {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  background: #fff;
  z-index: 100;
  border-bottom: 1rpx solid #f5f5f5;
}

.stat-box {
  background: #fdf8f4;
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  
  .stat-text {
    font-size: 26rpx;
    color: #d4a574;
    font-weight: 600;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.price-box {
  display: flex;
  align-items: baseline;
  
  .price-label {
    font-size: 24rpx;
    color: #666;
    margin-right: 4rpx;
  }
  .price-num {
    font-size: 32rpx;
    font-weight: bold;
    color: #d4a574;
  }
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.cart-btn {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fdf8f4;
    border-radius: 50%;
    border: 1rpx solid #f0e0d0;
    
    &:active {
      background: #f0e0d0;
    }
    
    .cart-icon-img {
      width: 36rpx;
      height: 36rpx;
    }
  }

  .checkout-btn {
  margin: 0;
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  font-size: 24rpx;
  background: #f8f8f8;
  color: #666;
  border-radius: 28rpx;
  border: 1rpx solid #eee;
  
  &::after { border: none; }
  
  &.ready {
    background: #d4a574;
    color: #fff;
    border-color: #d4a574;
  }
}

/* 主区域 */
.main-area {
  flex: 1;
  position: relative;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.tip-icon {
  position: absolute;
  top: 20rpx;
  left: 20rpx;
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  border-radius: 50%;
  border: 2rpx solid #d4a574;
  color: #d4a574;
  font-size: 28rpx;
  font-weight: bold;
  z-index: 50;
  background: #fff;
}

.canvas-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding-bottom: 96rpx;
  /* margin-top: -60rpx; 移除上移，避免被Header遮挡 */
}

.canvas {
  width: 560rpx;
  height: 560rpx;
  position: relative;
}

.rope-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 400rpx;
  height: 400rpx;
  border-radius: 50%;
  border: 2rpx solid #e0e0e0;
  z-index: 1;
}

.center-logo {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 0;
  opacity: 0.5;
  pointer-events: none;
}

.logo-img {
  width: 140rpx;
  height: 140rpx;
  margin-bottom: 6rpx;
}

.logo-name {
  font-size: 24rpx;
  color: #ccc;
  letter-spacing: 4rpx;
}

/* 底部工具栏 */
.toolbar-container {
  position: absolute;
  left: 10rpx;
  right: 10rpx;
  bottom: 16rpx;
  z-index: 80;
  display: flex;
  align-items: center;
  margin: 0;
  padding: 8rpx;
  background: #f7f7f7;
  border: 1rpx solid rgba(0, 0, 0, 0.05);
  border-radius: 44rpx;
  box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  overflow: visible;
  
  &.collapsed {
    right: auto;
    width: 92rpx;
    .tool-bar {
      flex: 0;
      width: 0;
      opacity: 0;
      padding: 0;
    }
  }
}

.tool-bar {
  flex: 1;
  height: 72rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 0;
  transition: all 0.3s ease;
  
  .tool-item {
    flex: none;
    height: 56rpx;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    padding: 0 18rpx;
    background: #ffffff;
    border: 1rpx solid rgba(0, 0, 0, 0.06);
    border-radius: 32rpx;
    white-space: nowrap;
    
    .tool-icon-img {
      font-size: 30rpx;
      line-height: 30rpx;
      margin-right: 8rpx;
    }
    
    .tool-text {
      margin-top: 0;
      font-size: 24rpx;
      line-height: 24rpx;
      color: rgba(0, 0, 0, 0.78);
    }

    &.active {
      border-color: rgba(0, 0, 0, 0.14);
      background: #ffffff;
      .tool-text {
        color: rgba(0, 0, 0, 0.9);
        font-weight: 600;
      }
    }
  }
}

.tool-collapse-hit {
  width: 72rpx;
  height: 72rpx;
  padding-right: 56rpx;
  margin-right: -56rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  position: relative;
  z-index: 3;
}

.tool-collapse-btn {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border: 1rpx solid rgba(0, 0, 0, 0.06);
  border-radius: 36rpx;
  position: relative;
  z-index: 2;
  box-shadow: none;
}

.collapse-icon {
  font-size: 44rpx;
  color: rgba(0, 0, 0, 0.45);
}

/* 商品选择区 */
.goods-section {
  height: 600rpx; /* 增加高度，利用工具栏节省的空间 */
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: 0 -4rpx 16rpx rgba(0,0,0,0.05);
}

.section-header {
  height: 80rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.category-tabs {
  white-space: nowrap;
  height: 100%;
  line-height: 80rpx;
  padding: 0 20rpx;
}

.cat-tab {
  display: inline-block;
  padding: 0 30rpx;
  font-size: 28rpx;
  color: #999;
  position: relative;
  
  &.active {
    color: #333;
    font-weight: bold;
    font-size: 30rpx;
    
    &::after {
      content: '';
      position: absolute;
      bottom: 16rpx;
      left: 50%;
      transform: translateX(-50%);
      width: 40rpx;
      height: 6rpx;
      background: #d4a574;
      border-radius: 4rpx;
    }
  }
}

.section-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 色系侧栏 */
.color-sidebar {
  width: 160rpx;
  background: #f8f8f8;
  
  .sidebar-group {
    margin-bottom: 10rpx;
  }
  
  .group-divider {
    height: 2rpx;
    background: #e0e0e0;
    margin: 0;
  }

  .color-tag {
    height: 90rpx;
    line-height: 90rpx;
    text-align: center;
    font-size: 26rpx;
    color: #666;
    
    &.active {
      background: #fff;
      color: #d4a574;
      font-weight: bold;
      position: relative;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 30rpx;
        bottom: 30rpx;
        width: 6rpx;
        background: #d4a574;
        border-radius: 0 4rpx 4rpx 0;
      }
    }
  }
}

/* 商品列表 Grid */
.product-area {
  flex: 1;
  background: #fff;
  padding: 20rpx;
}

.product-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
  padding-bottom: 40rpx;
}

.product-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.08);
  padding-bottom: 16rpx;
  
  .p-visual {
    width: 100%;
    position: relative;
    padding-bottom: 100%; /* 正方形 */
    margin-bottom: 8rpx;
    
    .p-bead {
      position: absolute;
      top: 0; left: 0; right: 0; bottom: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      
      .p-img {
        width: 70%;
        height: 70%;
        opacity: 0;
        transition: opacity 0.5s ease;
      }
      
      .p-img.loaded {
        opacity: 1;
      }
    }
    
    .p-stock {
      position: absolute;
      top: 12rpx;
      left: 12rpx;
      background: rgba(0,0,0,0.6);
      color: #fff;
      font-size: 20rpx;
      padding: 4rpx 12rpx;
      border-radius: 20rpx;
      z-index: 2;
    }
  }
  
  .p-name {
    font-size: 24rpx;
    color: #333;
    font-weight: bold;
    width: 96%;
    text-align: center;
    
    /* 改为双行显示 */
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    overflow: hidden;
    white-space: normal;
    
    margin-bottom: 8rpx;
    height: 72rpx; /* 固定高度，确保对齐 */
    line-height: 36rpx;
  }
  
  .p-price {
    font-size: 28rpx;
    color: #333;
    font-weight: bold;
    margin-bottom: 4rpx;
  }
  
  .p-size {
    font-size: 22rpx;
    color: #999;
  }
}

/* 珠子样式 */
.bead {
  position: absolute;
  /* border-radius: 50%; 移除强制圆角，允许异形配饰 */
  z-index: 10;
  transition: transform 0.5s cubic-bezier(0.25, 0.1, 0.25, 1), opacity 0.5s ease;

  &.is-new {
    animation: bead-pop-in 0.8s cubic-bezier(0.25, 0.1, 0.1, 1);
  }

  &.removing {
    opacity: 0;
    transform: scale(0.1) rotate(45deg) !important;
    transition: all 0.5s ease-in;
    pointer-events: none;
  }

  &.is-dragging {
    transform: scale(1.25) !important;
    z-index: 999 !important;
    filter: drop-shadow(0 12rpx 24rpx rgba(0,0,0,0.3));
    transition: none;
  }

  &.is-drag-target {
    filter: brightness(1.2);
    transform: scale(1.1);
    transition: transform 0.25s ease, filter 0.25s ease;
  }

  &.is-delete-target {
    opacity: 0.3;
    filter: grayscale(1) blur(2rpx);
    transition: all 0.3s ease;
  }

  &.is-swap-source {
    box-shadow: 0 0 0 8rpx #d4a574;
    z-index: 20 !important;
    animation: pulse-glow 0.8s ease-in-out infinite;
  }

  &.is-swap-target {
    box-shadow: 0 0 0 8rpx rgba(212,165,116,0.4);
    transition: box-shadow 0.3s ease;
  }
}

@keyframes bead-pop-in {
  0%   { opacity: 0; transform: scale(0) rotate(-30deg); }
  50%  { opacity: 0.7; transform: scale(1.2) rotate(5deg); }
  70%  { opacity: 1; transform: scale(0.9) rotate(-2deg); }
  85%  { transform: scale(1.04) rotate(1deg); }
  100% { opacity: 1; transform: scale(1) rotate(0deg); }
}

@keyframes bead-remove {
  0%   { opacity: 1; transform: scale(1); }
  100% { opacity: 0; transform: scale(0.1) rotate(30deg); }
}

@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 0 6rpx #d4a574; }
  50%      { box-shadow: 0 0 0 14rpx rgba(212,165,116,0.2); }
}
.bead-img {
  width: 100%;
  height: 100%;
  /* border-radius: 50%; 移除强制圆角 */
}
.bead-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}
.swap-indicator {
  position: absolute;
  top: 0;
  right: 0;
  background: #d4a574;
  color: #fff;
  font-size: 20rpx;
  width: 30rpx;
  height: 30rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 飞跃特效：原图从卡片飞到画布 */
.flying-bead-overlay {
  position: fixed;
  z-index: 9999;
  pointer-events: none;
  will-change: left, top, opacity, transform;
}

.flying-bead-img {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  box-shadow: 0 8rpx 28rpx rgba(0, 0, 0, 0.2);
  display: block;
}

.flying-bead-dot {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.18);
}

.export-canvas {
  position: absolute;
  left: -9999rpx;
  width: 560rpx;
  height: 560rpx;
  background: #fff;
}

/* 删除区域指示器 */
.delete-zone {
  position: absolute;
  bottom: -20rpx;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(231, 76, 60, 0.1);
  border: 2rpx dashed rgba(231, 76, 60, 0.3);
  border-radius: 24rpx;
  padding: 16rpx 32rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  z-index: 200;
  transition: all 0.3s ease;
  pointer-events: none;

  &.active {
    background: rgba(231, 76, 60, 0.2);
    border-color: rgba(231, 76, 60, 0.8);
    transform: translateX(-50%) scale(1.1);
    box-shadow: 0 4rpx 20rpx rgba(231, 76, 60, 0.3);
  }
}

.delete-zone-icon {
  font-size: 32rpx;
}

.delete-zone-text {
  font-size: 24rpx;
  color: #e74c3c;
  font-weight: 600;
}

/* 弹窗通用 */
.mask {
  position: fixed;
  left: 0; right: 0; top: 0; bottom: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.popup {
  width: 85%;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
}
.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 28rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.popup-title { font-size: 30rpx; font-weight: 700; color: #333; }
.popup-close { font-size: 32rpx; color: #999; padding: 8rpx; }
.popup-body { padding: 24rpx 28rpx; }
.popup-btn {
  margin: 20rpx 28rpx 28rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #d4a574, #c9976c);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 40rpx;
  border: none;
}

/* 尺寸选项 */
.size-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
  margin-bottom: 20rpx;
}
.size-options.compact { grid-template-columns: repeat(4, 1fr); }
.size-opt {
  padding: 16rpx 8rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
  text-align: center;
  border: 2rpx solid transparent;
  transition: all 0.2s ease;
}
.size-opt.selected { background: #fdf8f3; border-color: #d4a574; }
.opt-num { font-size: 28rpx; font-weight: 600; color: #333; }
.opt-unit { font-size: 18rpx; color: #999; margin-left: 2rpx; }

.custom-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
  margin-top: 10rpx;
}
.custom-label { font-size: 24rpx; color: #666; }
.custom-ipt { flex: 1; height: 48rpx; font-size: 28rpx; background: transparent; }
.custom-unit { font-size: 24rpx; color: #999; }

/* 预览弹窗 */
.preview-popup { max-height: 80vh; }
.preview-body { padding: 20rpx 28rpx; max-height: 50vh; overflow-y: auto; }
.preview-meta {
  display: flex;
  justify-content: center;
  gap: 24rpx;
  font-size: 24rpx;
  color: #888;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
  margin-bottom: 16rpx;
}
.preview-items { max-height: 240rpx; overflow-y: auto; }
.pv-item {
  display: flex;
  align-items: center;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f8f8f8;
}
.pv-name { flex: 1; font-size: 26rpx; color: #333; }
.pv-qty { font-size: 24rpx; color: #999; margin-right: 16rpx; }
.pv-price { font-size: 26rpx; color: #d4a574; font-weight: 600; }
.preview-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-top: 2rpx solid #f0f0f0;
  margin-top: 12rpx;
  font-size: 28rpx;
  color: #333;
}
.pv-total-num { font-size: 36rpx; color: #d4a574; font-weight: 700; }
.preview-actions {
  display: flex;
  gap: 16rpx;
  padding: 0 28rpx 28rpx;
}
.act-cancel, .act-submit {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 38rpx;
  font-size: 28rpx;
  font-weight: 600;
  border: none;
}
.act-cancel { background: #f5f5f5; color: #666; }
.act-submit { background: linear-gradient(135deg, #d4a574, #c9976c); color: #fff; }

/* 引导弹窗 */
.guide-popup { text-align: center; padding: 40rpx 28rpx; }
.guide-icon { font-size: 64rpx; margin-bottom: 16rpx; }
.guide-title { display: block; font-size: 32rpx; font-weight: 700; color: #333; margin-bottom: 12rpx; }
.guide-desc { display: block; font-size: 24rpx; color: #999; margin-bottom: 28rpx; }
/* 操作指南弹窗 */
.help-popup {
  width: 640rpx;
  background: #fff;
  border-radius: 32rpx;
  overflow: hidden;
}

.header-deco {
  font-size: 32rpx;
  margin-right: 12rpx;
}

.help-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
  padding: 10rpx 30rpx 30rpx;
}

.help-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #f9f9f9;
  padding: 24rpx 12rpx;
  border-radius: 20rpx;
}

.help-icon-box {
  position: relative;
  width: 120rpx;
  height: 120rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;
}

.help-text {
  font-size: 24rpx;
  color: #333;
  text-align: center;
}

/* 演示动画图标 */
.demo-bead {
  width: 40rpx;
  height: 40rpx;
  background: #e6c5a1;
  border-radius: 50%;
  animation: demo-pop 1.5s infinite;
}

.demo-hand {
  position: absolute;
  bottom: 20rpx;
  right: 20rpx;
  font-size: 36rpx;
  animation: demo-tap 1.5s infinite;
}

.demo-circle {
  width: 32rpx;
  height: 32rpx;
  background: #e6c5a1;
  border-radius: 50%;
  position: absolute;
}

.demo-drag-hand {
  position: absolute;
  font-size: 36rpx;
  animation: demo-drag 2s infinite linear;
}

.demo-bead-mirror {
  width: 40rpx;
  height: 40rpx;
  background: linear-gradient(90deg, #e6c5a1 50%, #d4a574 50%);
  border-radius: 50%;
  animation: demo-flip 2s infinite;
}

.demo-toolbar {
  display: flex;
  gap: 6rpx;
}

.dt-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #ddd;
}

.dt-dot.active {
  background: #d4a574;
  animation: demo-blink 1.5s infinite;
}

/* 动画定义 */
@keyframes demo-pop {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

@keyframes demo-tap {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-10rpx, -10rpx); }
}

@keyframes demo-drag {
  0% { top: 60rpx; left: 60rpx; }
  25% { top: 20rpx; left: 60rpx; }
  50% { top: 20rpx; left: 20rpx; }
  75% { top: 60rpx; left: 20rpx; }
  100% { top: 60rpx; left: 60rpx; }
}

@keyframes demo-flip {
  0%, 100% { transform: scaleX(1); }
  50% { transform: scaleX(-1); }
}

@keyframes demo-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

@keyframes demo-pinch-1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(12rpx, 12rpx); }
}

@keyframes demo-pinch-2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-12rpx, -12rpx); }
}

.demo-pinch {
  position: relative;
  width: 60rpx;
  height: 60rpx;
}

.finger {
  position: absolute;
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background-color: #d4a574;
  opacity: 0.6;
}

.finger-1 {
  top: 0;
  left: 0;
  animation: demo-pinch-1 2s infinite ease-in-out;
}

.finger-2 {
  bottom: 0;
  right: 0;
  animation: demo-pinch-2 2s infinite ease-in-out;
}

.help-footer {
  padding: 0 30rpx 30rpx;
}

.checkbox-row {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
  opacity: 0.8;
}

.checkbox {
  width: 32rpx;
  height: 32rpx;
  border: 2rpx solid #ccc;
  border-radius: 50%;
  margin-right: 12rpx;
  position: relative;
}

.checkbox.checked {
  background: #d4a574;
  border-color: #d4a574;
}

.checkbox.checked::after {
  content: '';
  position: absolute;
  left: 10rpx;
  top: 6rpx;
  width: 8rpx;
  height: 14rpx;
  border-right: 4rpx solid #fff;
  border-bottom: 4rpx solid #fff;
  transform: rotate(45deg);
}

.checkbox-label {
  font-size: 24rpx;
  color: #999;
}

.help-btn {
  margin: 0;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #d4a574, #c9976c);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 40rpx;
}

/* Loading States */
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40rpx 0;
  color: #999;
  font-size: 26rpx;
}

.loading-dot {
  width: 40rpx;
  height: 40rpx;
  border: 4rpx solid #f3f3f3;
  border-top: 4rpx solid #d4a574;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16rpx;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-more {
  text-align: center;
  padding: 20rpx 0;
  color: #999;
  font-size: 24rpx;
}

/* AI算命入口 */
.ai-icon {
  position: absolute;
  top: 20rpx;
  right: 20rpx;
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  border-radius: 50%;
  border: 2rpx solid #d4a574;
  color: #d4a574;
  font-size: 20rpx;
  font-weight: bold;
  z-index: 50;
  background: #fff;
}

/* AI弹窗 */
.ai-popup { max-height: 80vh; }
.ai-popup-body { padding: 24rpx 28rpx; }
.ai-input {
  width: 100%;
  min-height: 160rpx;
  background: #f9f9f9;
  border-radius: 16rpx;
  padding: 20rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
  line-height: 1.6;
}
.ai-tarot-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
}
.ai-tarot-label {
  font-size: 28rpx;
  color: #666;
}
.ai-submit-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #b48aff, #9c7afb);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 40rpx;
  border: none;
  margin-top: 16rpx;
}
.ai-submit-btn[disabled] {
  opacity: 0.5;
}

/* AI结果弹窗 */
.ai-result-popup { max-height: 85vh; display: flex; flex-direction: column; }
.ai-result-body { flex: 1; padding: 24rpx 28rpx; max-height: 60vh; }
.ai-section { margin-bottom: 28rpx; }
.ai-section-title {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #9c7afb;
  margin-bottom: 12rpx;
}
.ai-section-text {
  display: block;
  font-size: 26rpx;
  color: #555;
  line-height: 1.7;
}
.ai-material-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.ai-material-item {
  display: flex;
  gap: 16rpx;
  padding: 16rpx;
  background: #f9f7ff;
  border-radius: 12rpx;
}
.ai-material-img {
  width: 80rpx;
  height: 80rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
  background: #eee;
}
.ai-material-info { flex: 1; }
.ai-material-name {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 4rpx;
}
.ai-material-pos {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-bottom: 4rpx;
}
.ai-material-reason {
  display: block;
  font-size: 22rpx;
  color: #888;
  line-height: 1.5;
}
.ai-result-actions {
  display: flex;
  gap: 16rpx;
  padding: 20rpx 28rpx 28rpx;
}
.ai-result-cancel {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  background: #f5f5f5;
  color: #666;
  font-size: 28rpx;
  border-radius: 38rpx;
  border: none;
}
.ai-result-apply {
  flex: 1.5;
  height: 76rpx;
  line-height: 76rpx;
  background: linear-gradient(135deg, #b48aff, #9c7afb);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 38rpx;
  border: none;
}

/* 实物图预览弹窗 */
.image-preview-popup {
  width: 600rpx;
  max-height: 80vh;
}

.image-preview-body {
  padding: 20rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300rpx;
  background: #fafafa;
}

.preview-full-img {
  width: 100%;
  border-radius: 16rpx;
  max-height: 500rpx;
}

.preview-no-image {
  font-size: 28rpx;
  color: #999;
  padding: 80rpx 0;
}

.image-preview-info {
  padding: 20rpx 28rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  align-items: baseline;
  border-top: 1rpx solid #f0f0f0;
}

.preview-info-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  flex: 1 0 100%;
}

.preview-info-price {
  font-size: 32rpx;
  font-weight: 700;
  color: #e54d42;
}

.preview-info-size {
  font-size: 24rpx;
  color: #999;
  background: #f5f5f5;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}

.preview-info-stock {
  font-size: 24rpx;
  color: #999;
  background: #f5f5f5;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}
</style>
