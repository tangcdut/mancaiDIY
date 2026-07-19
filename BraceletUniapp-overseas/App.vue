<template>
  <view />
</template>

<script>
import { updateCartBadgeNow } from './utils/cartBadge.js'

export default {
  onLaunch() {
    setTimeout(() => {
      updateCartBadgeNow()
    }, 500)
  },
  onShow() {
    updateCartBadgeNow()
  },
  onHide() {}
}
</script>

<style>
/* 基础与手机端默认布局 */
html, body {
  background: #e8e8e8;
  margin: 0;
  padding: 0;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
}

#app {
  max-width: 430px;
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%);
  box-shadow: 0 0 30px rgba(0,0,0,0.15);
  margin: 0 auto;
}

page { background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%); }

/* PC端自适应布局适配 */
@media screen and (min-width: 768px) {
  html, body {
    background: #f0f2f5;
  }

  /* 允许 #app 容器在 PC 上铺开到 1200px 宽度 */
  #app {
    max-width: 1200px !important;
    box-shadow: 0 0 40px rgba(0,0,0,0.08) !important;
  }

  /* 锁定 H5 的 rpx 换算比例在 430px 级别，防止大屏上所有文字、珠子和间距变成巨无霸 */
  :root, body, #app {
    --window-width: 430px !important;
  }

  /* 头部导航与底部选项卡最大宽度限制与居中对齐 */
  uni-page-head .uni-page-head {
    left: 50% !important;
    right: auto !important;
    transform: translateX(-50%) !important;
    max-width: 1200px !important;
    width: 100% !important;
    box-sizing: border-box !important;
  }
  
  uni-tabbar .uni-tabbar {
    left: 50% !important;
    right: auto !important;
    transform: translateX(-50%) !important;
    max-width: 1200px !important;
    width: 100% !important;
    box-sizing: border-box !important;
  }
  
  /* 页面内固定底部的动作栏容器，同样做 1200px 宽度居中限制 */
  .bar,
  .action-bar,
  .submit-bar,
  .bottom-bar {
    left: 50% !important;
    right: auto !important;
    transform: translateX(-50%) !important;
    max-width: 1200px !important;
    width: 100% !important;
    box-sizing: border-box !important;
  }

  /* 针对购物车、确认订单、个人中心等页面，限制其内容最大宽度并居中，防止在宽屏下铺得太扁 */
  uni-page[data-page="pages/cart/index"] .page,
  uni-page[data-page="pages/order/confirm"] .page,
  uni-page[data-page="pages/mine/index"] .page,
  uni-page[data-page="pages/order/list"] .page,
  uni-page[data-page="pages/order/detail"] .page,
  uni-page[data-page="pages/address/list"] .page,
  uni-page[data-page="pages/address/select"] .page,
  uni-page[data-page="pages/address/edit"] .page,
  uni-page[data-page="pages/about/index"] .page,
  uni-page[data-page="pages/setting/index"] .page {
    max-width: 800px !important;
    margin: 0 auto !important;
    padding-left: 20px !important;
    padding-right: 20px !important;
    box-sizing: border-box !important;
    background: transparent !important;
  }

  /* 对于限制了 800px 宽度的页面，其固定底部栏也限制为 800px 并居中 */
  uni-page[data-page="pages/cart/index"] .bar,
  uni-page[data-page="pages/order/confirm"] .submit-bar,
  uni-page[data-page="pages/address/list"] .bottom-bar {
    max-width: 800px !important;
  }
}
</style>
