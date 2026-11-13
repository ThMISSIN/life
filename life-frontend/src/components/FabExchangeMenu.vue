<template>
  <div class="fab-stack" :class="{ 'is-open': open }" @mouseleave="scheduleClose">
    <!-- 二级: 分类图标按钮（hover 商品按钮依次旋出, 点击筛选对应分类商品） -->
    <button
      v-for="(cat, i) in CATS"
      :key="cat.name"
      :ref="(el) => setCatRef(i, el)"
      class="fab-cat"
      :title="cat.name"
      @click="onPickCategory(cat.name)"
      @mouseenter="cancelClose"
      @mouseleave="scheduleClose"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path v-for="(d, j) in cat.paths" :key="j" :d="d" />
      </svg>
    </button>

    <!-- 一级: 商品按钮（主球左上方; 直接点击=全部商品, hover=旋出分类按钮） -->
    <button
      ref="goodsBtnRef"
      class="fab-child"
      title="商品兑换（悬停选分类）"
      @click="chooseGoods"
      @mouseenter="onGoodsEnter"
      @mouseleave="onGoodsLeave"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z" />
        <path d="M3 6h18" />
        <path d="M16 10a4 4 0 0 1-8 0" />
      </svg>
    </button>

    <!-- 一级: 优惠券按钮（主球左下方） -->
    <button
      ref="couponBtnRef"
      class="fab-child"
      title="优惠券兑换"
      @click="chooseCoupon"
      @mouseenter="cancelClose"
      @mouseleave="scheduleClose"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
        <path d="M13 5v2" />
        <path d="M13 11v2" />
        <path d="M13 17v2" />
      </svg>
    </button>

    <!-- 主 Fab -->
    <button
      class="fab-main"
      title="兑换快捷入口"
      @mouseenter="expand"
      @mouseleave="scheduleClose"
    >
      <span class="fab-main__plus">+</span>
    </button>
  </div>
</template>

<script setup>
/**
 * 悬浮 Fab 兑换快捷菜单（独立封装, 可复用）
 *
 * 两级圆弧旋出（hover 驱动, WAAPI 关键帧模拟圆弧轨迹, transform only）:
 * - 一级: 悬浮主球 → 优惠券按钮旋向主球左下方, 商品按钮旋向主球左上方
 *   （优惠券 0s, 商品 0.08s, 缓动 cubic-bezier(0.34,1.56,0.64,1), 时长 0.4s）
 * - 二级: 悬浮商品按钮 → 美食/出行/娱乐/生活/购物 五个分类图标按钮
 *   以商品按钮为中心依次旋出成上方扇形（延时逐个递增 60ms）
 * - 热区保护: 主球移出后延迟 400ms 收回, 期间进入任意子按钮/分类按钮取消收回
 *
 * 事件:
 * - switch-mode('COUPON'|'GOODS'): 切换兑换模式
 * - select-category(名称): 分类筛选（'全部' 表示全部商品）
 */
import { onBeforeUnmount, ref } from 'vue'

const emit = defineEmits(['switch-mode', 'select-category'])

const EASING = 'cubic-bezier(0.34, 1.56, 0.64, 1)'
const DURATION = 400
const GOODS_DELAY = 80
const CAT_STEP_DELAY = 60

// 分类按钮定义: 名称（事件回传）+ 线条 SVG path
const CATS = [
  {
    name: '美食',
    paths: ['M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2', 'M7 2v20', 'M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7'],
  },
  {
    name: '出行',
    paths: ['M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2', 'M9 17h6'],
  },
  {
    name: '娱乐',
    paths: ['M20.2 6 3 11l-.9-2.4c-.3-1.1.3-2.2 1.3-2.5l13.5-4c1.1-.3 2.2.3 2.5 1.3Zm-6.2-1 1.6 4.5M4.8 8.1l1.6 4.4', 'M12.5 9.5 14 14', 'm8 21h12a2 2 0 0 0 2-2v-5Z'],
  },
  {
    name: '生活',
    paths: ['M17 8h1a4 4 0 1 1 0 8h-1', 'M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z', 'M6 2v2', 'M10 2v2', 'M14 2v2'],
  },
  {
    name: '购物',
    paths: ['M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z', 'M3 6h18', 'M16 10a4 4 0 0 1-8 0'],
  },
]

// 一级子按钮圆弧轨迹（对象格式, WAAPI 严格实现不接受字符串数组）
// 商品按钮 → 主球左上方（红圈位）; 优惠券按钮 → 主球正左侧（红圈位, 与主球同高）
const PATHS = {
  coupon: [
    { transform: 'translate(0, 0) scale(0)' },
    { transform: 'translate(-84px, 8px) scale(0.72)' },
    { transform: 'translate(-150px, -4px) scale(1)' },
  ],
  goods: [
    { transform: 'translate(0, 0) scale(0)' },
    { transform: 'translate(-46px, -46px) scale(0.72)' },
    { transform: 'translate(-84px, -88px) scale(1)' },
  ],
}

// 二级分类按钮终点: 以商品按钮（-84,-88）为中心的上方紧凑扇形
const CAT_TARGETS = {
  美食: { x: -158, y: -122 },
  出行: { x: -112, y: -156 },
  娱乐: { x: -70, y: -162 },
  生活: { x: -28, y: -156 },
  购物: { x: 16, y: -122 },
}

const open = ref(false)
const catOpen = ref(false)
const couponBtnRef = ref(null)
const goodsBtnRef = ref(null)
const catBtnRefs = ref([])

let closeTimer = null
let catOpenTimer = null

function clearTimers() {
  clearTimeout(closeTimer)
  clearTimeout(catOpenTimer)
  closeTimer = catOpenTimer = null
}

// 函数 ref: 收集分类按钮元素（注意模板中顶层 ref 自动解包, 不能在模板内联函数里访问 .value）
function setCatRef(i, el) {
  catBtnRefs.value[i] = el
}

// 播放子按钮动画（direction: forward 展开 / backward 收回）
function playAnim(el, frames, delay, direction) {
  if (!el) return
  el.animate(direction === 'forward' ? frames : [...frames].reverse(), {
    duration: DURATION,
    delay,
    easing: EASING,
    fill: 'forwards',
  })
}

function expand() {
  open.value = true
  playAnim(couponBtnRef.value, PATHS.coupon, 0, 'forward')
  playAnim(goodsBtnRef.value, PATHS.goods, GOODS_DELAY, 'forward')
  if (catOpen.value) {
    // 二级原本展开（快速移出再移回）, 同步保持在展开位置
    CATS.forEach((cat, i) => {
      const t = CAT_TARGETS[cat.name]
      playAnim(
        catBtnRefs.value[i],
        [
          { transform: 'translate(-84px, -88px) scale(0)' },
          { transform: `translate(${t.x}px, ${t.y}px) scale(1)` },
        ],
        GOODS_DELAY + 40 + i * CAT_STEP_DELAY,
        'forward',
      )
    })
  }
}

function collapse() {
  playAnim(couponBtnRef.value, PATHS.coupon, 0, 'backward')
  playAnim(goodsBtnRef.value, PATHS.goods, GOODS_DELAY, 'backward')
  CATS.forEach((cat, i) => {
    const t = CAT_TARGETS[cat.name]
    if (catOpen.value) {
      playAnim(
        catBtnRefs.value[i],
        [
          { transform: `translate(${t.x}px, ${t.y}px) scale(1)` },
          { transform: 'translate(-84px, -88px) scale(0)' },
        ],
        i * CAT_STEP_DELAY,
        'forward',
      )
    }
  })
  catOpen.value = false
  open.value = false
}

// 热区保护: 主球/子按钮移出后延迟收回, 期间进入其他按钮可取消
// 400ms 缓冲给鼠标足够的反应时间移向目标按钮, 避免未悬停到就被收回
function scheduleClose() {
  clearTimeout(closeTimer)
  closeTimer = setTimeout(() => {
    if (open.value) collapse()
  }, 400)
}

function cancelClose() {
  clearTimeout(closeTimer)
}

function onGoodsEnter() {
  cancelClose()
  clearTimeout(catOpenTimer)
  // 悬停商品按钮 320ms 后依次旋出分类按钮（稍作停留防路过误触）
  catOpenTimer = setTimeout(() => {
    if (!catOpen.value) {
      catOpen.value = true
      CATS.forEach((cat, i) => {
        const t = CAT_TARGETS[cat.name]
        playAnim(
          catBtnRefs.value[i],
          [
            { transform: 'translate(-84px, -88px) scale(0)' },
            { transform: `translate(${t.x}px, ${t.y}px) scale(1)` },
          ],
          GOODS_DELAY + i * CAT_STEP_DELAY,
          'forward',
        )
      })
    }
  }, 320)
}

function onGoodsLeave() {
  clearTimeout(catOpenTimer)
  scheduleClose()
}

function chooseCoupon() {
  emit('switch-mode', 'COUPON')
  collapse()
}

function chooseGoods() {
  emit('switch-mode', 'GOODS')
  emit('select-category', '全部')
  collapse()
}

function onPickCategory(name) {
  emit('switch-mode', 'GOODS')
  emit('select-category', name)
  collapse()
}

onBeforeUnmount(clearTimers)
</script>

<style scoped>
.fab-stack {
  position: fixed;
  right: 24px;
  bottom: 32px;
  z-index: 999;
  width: 56px;
  height: 56px;
}

/* 主 Fab */
.fab-main {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border: none;
  border-radius: 50%;
  background: #2563eb;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
  cursor: pointer;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.fab-stack.is-open .fab-main {
  transform: rotate(45deg);
}

.fab-main__plus {
  font-size: 30px;
  font-weight: 400;
  line-height: 1;
  color: #fff;
  user-select: none;
}

/* 一级子按钮: 初始收起于主球中心, 位置由 WAAPI 驱动 */
.fab-child {
  position: absolute;
  top: 4px;
  left: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 50%;
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  pointer-events: none;
  transform: translate(0, 0) scale(0);
  transition: background-color 0.2s;
}

.fab-stack.is-open .fab-child {
  pointer-events: auto;
}

.fab-child:hover {
  background: #eff6ff;
}

.fab-child svg {
  width: 22px;
  height: 22px;
}

/* 二级分类按钮: 比一级略小, 初始收起于商品按钮位置 */
.fab-cat {
  position: absolute;
  top: 8px;
  left: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 50%;
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  pointer-events: none;
  transform: translate(-84px, -88px) scale(0);
  transition: background-color 0.2s;
}

.fab-stack.is-open .fab-cat {
  pointer-events: auto;
}

.fab-cat:hover {
  background: #eff6ff;
}

.fab-cat svg {
  width: 20px;
  height: 20px;
}
</style>
