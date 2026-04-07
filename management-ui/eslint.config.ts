import pluginVue from 'eslint-plugin-vue'
import pluginOxlint from 'eslint-plugin-oxlint'
import skipFormatting from 'eslint-config-prettier/flat'

export default [
  // 1. 全局忽略配置 (替代旧版的 .eslintignore)
  {
    ignores: ['**/dist/**', '**/dist-ssr/**', '**/coverage/**'],
  },

  // 2. Vue 的基础 JavaScript 校验规则
  ...pluginVue.configs['flat/essential'],

  // 3. 引入 Oxlint 规则 (用于加速 lint)
  ...pluginOxlint.buildFromOxlintConfigFile('.oxlintrc.json'),

  // 4. 关闭与 Prettier 冲突的格式化规则
  skipFormatting,
]
