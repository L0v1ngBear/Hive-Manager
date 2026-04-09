<template>
  <main class="flex min-h-screen bg-background text-on-surface overflow-hidden font-body">

    <section class="hidden lg:flex w-1/2 relative flex-col justify-end p-20 overflow-hidden bg-[#e8eef6]">
      <img
          alt="纺织机"
          class="absolute inset-0 w-full h-full object-cover opacity-30 mix-blend-multiply pointer-events-none"
          src="https://lh3.googleusercontent.com/aida-public/AB6AXuCuIwA6L-s4oCozLMdH_yfiMdnU0mX3BNuYqJ8eL_plQToEhrFac1WjHNcEdRuN1RgYYCqi-YiEcoTm8REv0nbBH5cU2siKa-rdLvfzS0q7Tnco3uQg1a3oNwHXA2eykkjJ_E7iiYhjXah9w5j8cZJ4KO6ujDNoQcB4A6RuJaz0qaKlTajXwovmmPm0I795XTYPs0zGA0mPeNz3h8dQvGrF-GdMqhlWuodTHW8UNIJO55FxfmsKJdQ7S5jjN38u9jSAgO4S_yvuwFte"
      />
      <div class="absolute inset-0 bg-gradient-to-t from-[#d1dceb] via-transparent to-transparent pointer-events-none"></div>

      <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 flex gap-12 z-20 pointer-events-none">

        <svg
            ref="char1Ref"
            :class="['pixel-char w-32 h-32', { 'error-shake': isError }]"
            viewBox="0 0 100 100"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
        >
          <rect fill="#455f88" height="80" width="80" x="10" y="10"></rect>
          <rect fill="#2d476f" height="10" width="80" x="10" y="80"></rect>
          <g class="eye-container">
            <rect class="eye-white" fill="white" height="15" width="15" x="25" y="30"></rect>
            <rect class="eye-pupil" fill="#002045" height="8" width="8" x="28" y="33" :transform="pupil1Transform"></rect>
            <rect class="eye-white" fill="white" height="15" width="15" x="60" y="30"></rect>
            <rect class="eye-pupil" fill="#002045" height="8" width="8" x="63" y="33" :transform="pupil1Transform"></rect>
          </g>
          <path class="mouth" :d="mouth1Path" stroke="white" stroke-linecap="round" stroke-width="4"></path>
        </svg>

        <svg
            ref="char2Ref"
            :class="['pixel-char w-24 h-24 mt-12', { 'error-shake': isError }]"
            viewBox="0 0 100 100"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
        >
          <rect fill="#f2bc82" height="80" width="80" x="10" y="10"></rect>
          <rect fill="#633f0f" height="10" width="80" x="10" y="80"></rect>
          <g class="eye-container">
            <rect class="eye-white" fill="white" height="12" width="12" x="30" y="35"></rect>
            <rect class="eye-pupil" fill="#321b00" height="6" width="6" x="33" y="38" :transform="pupil2Transform"></rect>
            <rect class="eye-white" fill="white" height="12" width="12" x="58" y="35"></rect>
            <rect class="eye-pupil" fill="#321b00" height="6" width="6" x="61" y="38" :transform="pupil2Transform"></rect>
          </g>
          <path class="mouth" :d="mouth2Path" stroke="#321b00" stroke-linecap="round" stroke-width="3"></path>
        </svg>
      </div>

      <div class="relative z-10 space-y-4">
        <div class="w-12 h-1 bg-primary mb-8"></div>
        <h1 class="text-5xl font-extrabold text-primary tracking-tighter leading-tight">
          Hive 蜂巢 <br/>数字化工厂管理系统
        </h1>
        <p class="text-on-surface-variant text-lg max-w-md font-medium leading-relaxed">
          通过高保真数据结构与精湛的纺织工艺智慧，协调工业生产效率。
        </p>
      </div>

      <div class="absolute top-12 left-12 z-10 pointer-events-none">
        <span class="text-primary/10 text-8xl font-black tracking-widest select-none">HIVE</span>
      </div>
    </section>

    <section class="w-full lg:w-1/2 bg-surface flex flex-col items-center justify-center p-8 md:p-16 relative">
      <div class="w-full max-w-md space-y-12">

        <div class="flex flex-col items-center lg:items-start text-center lg:text-left">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-10 h-10 bg-primary flex items-center justify-center rounded-none shadow-sm">
              <span class="material-symbols-outlined text-white text-2xl" style="font-variation-settings: 'FILL' 1;">texture</span>
            </div>
            <span class="text-2xl font-bold tracking-tighter text-on-surface">Hive 蜂巢</span>
          </div>
          <p class="text-on-surface-variant font-medium text-sm tracking-wide uppercase mt-4">安全登录网关</p>
        </div>

        <form class="space-y-8" @submit.prevent="handleLogin">
          <div class="space-y-6">

            <div class="group relative">
              <div v-if="isError" class="absolute left-0 top-0 bottom-0 w-[2px] bg-error rounded-full"></div>
              <div :class="{'pl-3': isError}" class="transition-all duration-200">
                <label class="block text-xs font-semibold uppercase tracking-widest text-on-surface-variant mb-2" for="email">
                  账号 / 电子邮箱
                </label>
                <input
                    id="email"
                    v-model="loginForm.email"
                    type="email"
                    required
                    placeholder="name@hive-erp.com"
                    class="w-full py-4 bottom-line-input text-on-surface placeholder:text-outline/50"
                />
              </div>
            </div>

            <div class="group relative">
              <div v-if="isError" class="absolute left-0 top-0 bottom-0 w-[2px] bg-error rounded-full"></div>
              <div :class="{'pl-3': isError}" class="transition-all duration-200">
                <div class="flex justify-between items-end mb-2">
                  <label class="block text-xs font-semibold uppercase tracking-widest text-on-surface-variant" for="password">
                    密码
                  </label>
                  <a href="#" class="text-xs font-medium text-primary hover:text-surface-tint transition-colors">忘记密码？</a>
                </div>
                <input
                    id="password"
                    v-model="loginForm.password"
                    type="password"
                    required
                    placeholder="••••••••"
                    class="w-full py-4 bottom-line-input text-on-surface placeholder:text-outline/50"
                />
              </div>
            </div>
          </div>

          <div class="space-y-4 pt-4">
            <button
                type="submit"
                class="w-full py-4 bg-primary text-white font-bold text-sm tracking-widest uppercase hover:opacity-90 transition-all active:scale-95 duration-150 flex items-center justify-center gap-2"
                :disabled="isLoading"
            >
              <span v-if="isLoading" class="material-symbols-outlined animate-spin text-lg">progress_activity</span>
              {{ isLoading ? '验证中...' : '登 录' }}
            </button>

            <div class="relative py-4">
              <div aria-hidden="true" class="absolute inset-0 flex items-center">
                <div class="w-full border-t border-surface-variant"></div>
              </div>
              <div class="relative flex justify-center text-xs uppercase tracking-widest">
                <span class="bg-surface px-4 text-outline">或通过以下方式认证</span>
              </div>
            </div>

            <button type="button" class="w-full py-4 bg-surface-container-high text-on-surface font-semibold text-sm tracking-widest uppercase hover:bg-surface-variant transition-all flex items-center justify-center gap-2">
              <span class="material-symbols-outlined text-xl">chat</span>
              微信登录
            </button>
          </div>

          <p v-show="isError" class="text-error text-xs font-medium text-center transition-opacity duration-300">
            用户名或密码错误，请重试。
          </p>
        </form>

        <div class="pt-8 flex flex-col items-center lg:items-start space-y-4">
          <div class="flex gap-2">
            <div class="w-1.5 h-1.5 rounded-full bg-primary/20"></div>
            <div class="w-1.5 h-1.5 rounded-full bg-primary/20"></div>
            <div class="w-1.5 h-1.5 rounded-full bg-primary/20"></div>
          </div>
        </div>
      </div>

      <footer class="absolute bottom-0 w-full bg-surface/80 backdrop-blur-sm flex justify-between items-center px-8 py-6 z-50">
        <div class="flex gap-6 items-center">
          <span class="text-[10px] uppercase tracking-widest font-medium text-slate-400">© 2024 Hive 蜂巢. 工业精度。</span>
        </div>
        <nav class="flex gap-8">
          <a href="#" class="text-[10px] uppercase tracking-widest font-medium text-slate-400 hover:text-primary transition-colors">隐私政策</a>
          <a href="#" class="text-[10px] uppercase tracking-widest font-medium text-slate-400 hover:text-primary transition-colors">服务条款</a>
          <a href="#" class="text-[10px] uppercase tracking-widest font-medium text-slate-400 hover:text-primary transition-colors">联系支持</a>
        </nav>
      </footer>
    </section>
  </main>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'

// ====== 1. 表单状态 ======
const loginForm = reactive({
  email: '',
  password: ''
})

const isLoading = ref(false)
const isError = ref(false)

// ====== 2. SVG 动画交互状态 ======
const char1Ref = ref(null)
const char2Ref = ref(null)

const pupil1Transform = ref('translate(0, 0)')
const pupil2Transform = ref('translate(0, 0)')

const mouth1Path = ref('M35 65 H65')
const mouth2Path = ref('M40 65 H60')

// ====== 3. 登录提交逻辑 ======
const handleLogin = async () => {
  if (!loginForm.email || !loginForm.password) return

  isLoading.value = true
  isError.value = false

  // TODO: 这里替换为您真实的 API 请求 (例如: await loginApi(loginForm))
  await new Promise(resolve => setTimeout(resolve, 600))

  isLoading.value = false

  // 这里演示登录失败，触发错误动画。如果成功，可以直接 router.push('/')
  triggerErrorState()
}

// ====== 4. 错误交互 (抖动 & 惊讶表情) ======
const triggerErrorState = () => {
  isError.value = true

  // 改变口型为惊讶(圆形)
  mouth1Path.value = 'M 40,65 a 10,10 0 1,0 20,0 a 10,10 0 1,0 -20,0'
  mouth2Path.value = 'M 45,65 a 5,5 0 1,0 10,0 a 5,5 0 1,0 -10,0'

  // 3秒后恢复默认状态
  setTimeout(() => {
    isError.value = false
    mouth1Path.value = 'M35 65 H65'
    mouth2Path.value = 'M40 65 H60'
  }, 3000)
}

// ====== 5. 鼠标眼球跟随逻辑 ======
const onMouseMove = (e) => {
  const { clientX, clientY } = e

  // 人物 1
  if (char1Ref.value) {
    const rect1 = char1Ref.value.getBoundingClientRect()
    const centerX1 = rect1.left + rect1.width / 2
    const centerY1 = rect1.top + rect1.height / 2

    const angle1 = Math.atan2(clientY - centerY1, clientX - centerX1)
    const distance1 = Math.min(3, Math.hypot(clientX - centerX1, clientY - centerY1) / 100)
    pupil1Transform.value = `translate(${Math.cos(angle1) * distance1}, ${Math.sin(angle1) * distance1})`
  }

  // 人物 2
  if (char2Ref.value) {
    const rect2 = char2Ref.value.getBoundingClientRect()
    const centerX2 = rect2.left + rect2.width / 2
    const centerY2 = rect2.top + rect2.height / 2

    const angle2 = Math.atan2(clientY - centerY2, clientX - centerX2)
    const distance2 = Math.min(3, Math.hypot(clientX - centerX2, clientY - centerY2) / 100)
    pupil2Transform.value = `translate(${Math.cos(angle2) * distance2}, ${Math.sin(angle2) * distance2})`
  }
}

// ====== 6. 生命周期钩子 ======
onMounted(() => {
  window.addEventListener('mousemove', onMouseMove)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onMouseMove)
})
</script>

<style scoped>
/* Bottom-Line Input 规范 (参考设计文档) */
.bottom-line-input {
  border: none;
  border-bottom: 1px solid #c2c7cf;
  background: transparent;
  transition: all 0.3s ease;
}

.bottom-line-input:focus {
  outline: none;
  border-bottom-color: #0f4c81; /* 对应您的 primary 颜色 */
  box-shadow: none;
}

/* 消除浏览器原生自动填充导致的背景色变白问题 */
input:-webkit-autofill {
  -webkit-box-shadow: 0 0 0px 1000px transparent inset !important;
  transition: background-color 5000s ease-in-out 0s;
}

/* 像素字符动画基础过渡属性 */
.pixel-char { transition: transform 0.2s ease-out; }
.eye-pupil { transition: transform 0.1s ease-out; }
.mouth { transition: d 0.3s ease-in-out; }

/* 错误时左右抖动效果 */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.error-shake {
  animation: shake 0.2s ease-in-out 0s 3;
}
</style>