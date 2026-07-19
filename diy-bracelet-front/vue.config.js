const { defineConfig } = require('@vue/cli-service')

// 使用环境变量 `VUE_APP_API_TARGET` 来控制代理目标，便于本地调试和上线切换。
// 开发时可在项目根目录创建 `.env.development`：
// VUE_APP_API_TARGET=http://localhost:8080
// 上线时在 `.env.production` 中设置为远端地址：
// VUE_APP_API_TARGET=https://springboot-gq7m-207364-6-1391651365.sh.run.tcloudbase.com
const API_TARGET = process.env.VUE_APP_API_TARGET || 'http://localhost:8080'

module.exports = defineConfig({
  transpileDependencies: true,
  publicPath: '/manage/',
  outputDir: 'dist',
  assetsDir: 'static',
  indexPath: 'index.html',
  filenameHashing: true,
  chainWebpack: config => {
    // 禁用 copy 插件对 public/index.html 的处理
    config.plugin('copy').tap(args => {
      args[0].patterns = args[0].patterns.filter(pattern => {
        // 过滤掉 index.html
        if (pattern.from && pattern.from.includes('index.html')) {
          return false
        }
        // 如果 globOptions 存在，添加忽略 index.html 的规则
        if (pattern.globOptions) {
          pattern.globOptions.ignore = pattern.globOptions.ignore || []
          pattern.globOptions.ignore.push('**/index.html', 'index.html')
        } else {
          pattern.globOptions = {
            ignore: ['**/index.html', 'index.html']
          }
        }
        return true
      })
      return args
    })
  },
  devServer: {
    port: 3000,
    proxy: {
      '/admin': {
        target: API_TARGET,
        changeOrigin: true,
        pathRewrite: {
          '^/admin': '/admin'
        }
      }
    }
  }
})
