import { paypalPayment } from '../api/index.js'

/**
 * PayPal payment handler - Overseas H5 version
 * Redirects user to PayPal approval page
 * @param {String|Object} order - Order number or order object
 * @param {Function} onSuccess - Payment success callback
 * @param {Function} onFail - Payment failure callback
 */
export async function handleOrderPayment(order, onSuccess, onFail) {
  let orderNo
  if (typeof order === 'object' && order !== null) {
    orderNo = order.orderNo || order.order_no || order.id || order.orderId
  } else {
    orderNo = order
  }

  orderNo = String(orderNo)

  if (!orderNo || orderNo === 'undefined' || orderNo === 'null') {
    uni.showToast({ title: 'Invalid order number / 订单号无效', icon: 'none' })
    if (onFail) onFail(new Error('Invalid order number'))
    return
  }

  uni.showLoading({ title: 'Redirecting to PayPal...', mask: true })

  try {
    const res = await paypalPayment(orderNo, 'USD')
    console.log('PayPal payment response:', res)

    uni.hideLoading()

    if (res && res.approvalUrl) {
      // Save orderNo for return handling
      uni.setStorageSync('paying_order_no', orderNo)

      if (onSuccess) onSuccess()

      // Redirect to PayPal approval page
      window.location.href = res.approvalUrl
    } else {
      throw new Error('No PayPal approval URL returned')
    }
  } catch (error) {
    uni.hideLoading()
    console.error('PayPal payment error:', error)

    uni.showModal({
      title: 'Payment Failed / 支付失败',
      content: (error.message || 'Payment failed') + '. Retry? / 重试？',
      confirmText: 'Retry / 重试',
      cancelText: 'Cancel / 取消',
      success: (modalRes) => {
        if (modalRes.confirm) {
          handleOrderPayment(orderNo, onSuccess, onFail)
        }
      }
    })

    if (onFail) onFail(error)
  }
}
