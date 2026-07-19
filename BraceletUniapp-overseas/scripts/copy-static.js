// H5 打包后把 static/ 静态资源拷贝到构建产物目录。
// 该项目为扁平结构 + UNI_INPUT_DIR=.，uni H5 构建不会自动拷贝 static/，
// 导致 tabbar 图标、logo、icons 等在部署后加载不出来，这里手动补齐。
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const src = path.join(root, 'static')
// 与 vite/uni 的 H5 默认输出目录保持一致，允许用 UNI_OUTPUT_DIR 覆盖
const outDir = process.env.UNI_OUTPUT_DIR
  ? path.resolve(root, process.env.UNI_OUTPUT_DIR)
  : path.join(root, 'dist', 'build', 'h5')
const dest = path.join(outDir, 'static')

if (!fs.existsSync(src)) {
  console.warn('[copy-static] 源 static 目录不存在，跳过：', src)
  process.exit(0)
}
if (!fs.existsSync(outDir)) {
  console.warn('[copy-static] 构建输出目录不存在，跳过：', outDir)
  process.exit(0)
}

fs.cpSync(src, dest, { recursive: true })
console.log('[copy-static] 已拷贝 static -> ', dest)
