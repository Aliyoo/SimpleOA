# SimpleOA 前端样式系统

本项目根据《UI样式逻辑.md》文档进行了全面的前端样式优化，建立了完整的设计系统。

## 样式架构

### 文件结构
```
src/styles/
├── variables.css      # 全局CSS变量定义
├── global.css         # 全局样式和工具类
├── components.css     # Element Plus组件样式增强
├── animations.css     # 交互动效和动画
└── README.md         # 本文档
```

### 样式导入顺序
```css
@import './variables.css';    # 变量优先
@import './components.css';   # 组件样式
@import './animations.css';   # 动效样式
```

## 设计规范

### 1. 色彩体系
- **主色调**: `#409EFF` (Element UI 默认蓝)
- **功能色**: 成功、警告、危险、信息色
- **文本色**: 主文本、常规文本、次要文本、占位符文本
- **背景色**: 主背景、卡片背景、遮罩背景等
- **边框色**: 浅、基础、深、更深四个层级

### 2. 字体系统
- **字体族**: "PingFang SC", "Microsoft YaHei", Arial, sans-serif
- **字号**: 12px-24px 共7个尺寸规格
- **字重**: 400、500、600 三个等级
- **行高**: 1.2-1.6 四个层级

### 3. 间距系统
- **基础间距**: 4px-48px 统一间距规范
- **布局间距**: 页面边距24px，内容区16px
- **组件间距**: 统一的内外边距规范

### 4. 布局尺寸
- **顶部导航**: 60px 高度
- **侧边栏**: 200px 宽度，可收缩至64px
- **内容区**: 最小宽度1200px，自适应布局
- **圆角**: 2px-16px 多层级圆角系统

## 组件增强

### 按钮组件
- 圆角优化和悬停动效
- 统一的颜色和尺寸规范
- 点击反馈动画

### 表格组件
- 优化表头样式和行高
- 悬停高亮效果
- 操作按钮样式统一

### 卡片组件
- 阴影层次和悬停效果
- 统一的边框和圆角
- 头部和内容区样式规范

### 表单组件
- 输入框聚焦效果优化
- 错误状态样式增强
- 标签和间距规范

### 弹窗组件
- 圆角和阴影优化
- 头部和底部样式统一
- 进入退出动画

## 交互动效

### 基础动画
- 淡入淡出 (fadeIn/fadeOut)
- 滑动进入 (slideIn 四个方向)
- 缩放进入 (scaleIn)
- 弹跳效果 (bounce)
- 脉冲效果 (pulse)

### 悬停效果
- 悬浮上升 (hover-lift)
- 缩放效果 (hover-scale)
- 旋转效果 (hover-rotate)
- 发光效果 (hover-glow)
- 滑动光效 (hover-slide)

### 加载动画
- 旋转加载器 (loading-spinner)
- 点状加载器 (loading-dots)
- 进度条动画 (progress-bar)
- 骨架屏效果 (skeleton)

### Vue过渡
- 页面切换动画
- 模态框动画
- 列表项动画
- 通知消息动画

## 主题系统

### 主题切换功能
- 亮色主题 (theme-light)
- 暗色主题 (theme-dark)
- 自动切换 (theme-auto，跟随系统)

### 主题组件
- ThemeToggle.vue: 主题切换组件
- useTheme.js: 主题管理组合式函数
- 支持按钮、下拉、单选、开关四种切换模式

### CSS变量切换
- 使用CSS自定义属性实现主题切换
- 支持媒体查询自动切换
- localStorage本地存储用户偏好

## 响应式设计

### 断点设置
- 移动端: ≤768px
- 平板端: 769px-1024px  
- 桌面端: ≥1025px

### 响应式适配
- 侧边栏自动收缩
- 内容区自适应
- 组件尺寸响应式调整
- 隐藏/显示类工具

## 工具类系统

### 文本类
- 颜色: .text-primary, .text-success 等
- 尺寸: .text-xs, .text-sm 等
- 字重: .font-normal, .font-medium 等
- 对齐: .text-center, .text-left 等

### 间距类
- 外边距: .m-xs, .mt-base 等
- 内边距: .p-sm, .p-lg 等

### 布局类
- Flex布局: .flex, .flex-col 等
- 对齐: .items-center, .justify-between 等
- 尺寸: .w-full, .h-full 等

### 响应式类
- 隐藏类: .hidden-xs, .hidden-sm 等
- 打印样式: .no-print

## 性能优化

### 动画性能
- 使用transform代替position变化
- 开启GPU加速 (.gpu-accelerated)
- will-change属性优化
- 支持减少动画偏好设置

### 文件加载
- CSS文件模块化
- 按需导入样式
- 变量统一管理

## 使用指南

### 1. 在组件中使用
```vue
<template>
  <div class="oa-card hover-lift">
    <div class="oa-card-header">
      <h3 class="oa-card-title">标题</h3>
    </div>
    <div class="oa-card-body">
      内容
    </div>
  </div>
</template>
```

### 2. 使用主题切换
```vue
<template>
  <ThemeToggle mode="dropdown" />
</template>

<script setup>
import ThemeToggle from '@/components/ThemeToggle.vue'
</script>
```

### 3. 使用动画
```vue
<template>
  <transition name="fade">
    <div v-if="show" class="fade-in">内容</div>
  </transition>
</template>
```

### 4. 使用工具类
```vue
<template>
  <div class="flex items-center justify-between p-lg">
    <span class="text-primary font-medium">文字</span>
    <button class="hover-lift click-effect">按钮</button>
  </div>
</template>
```

## 维护建议

1. **保持一致性**: 优先使用设计系统中定义的变量和类
2. **避免硬编码**: 使用CSS变量而非具体数值
3. **响应式优先**: 考虑不同设备的适配
4. **性能考虑**: 合理使用动画，避免过度渲染
5. **可访问性**: 支持键盘导航和屏幕阅读器

## 扩展指南

如需添加新的样式规范：

1. **变量**: 在 `variables.css` 中定义新的CSS变量
2. **组件**: 在 `components.css` 中添加组件样式
3. **动画**: 在 `animations.css` 中定义新的动画效果
4. **工具类**: 在 `global.css` 中添加新的工具类

遵循现有的命名规范和代码结构，确保样式系统的一致性和可维护性。