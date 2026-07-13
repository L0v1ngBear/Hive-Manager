import { defineStore } from 'pinia'

const SHOW_DELAY_MS = 180

export const useRequestStatusStore = defineStore('requestStatus', {
  state: () => ({
    activeCount: 0,
    queryCount: 0,
    mutationCount: 0,
    visible: false,
    delayTimer: null
  }),
  getters: {
    message: (state) => {
      if (state.mutationCount > 0) {
        return '处理中，请稍候...'
      }
      if (state.queryCount > 0) {
        return '查询中，请稍候...'
      }
      return '处理中，请稍候...'
    }
  },
  actions: {
    start(type = 'query') {
      this.activeCount += 1
      if (type === 'mutation') {
        this.mutationCount += 1
      } else {
        this.queryCount += 1
      }
      if (this.visible || this.delayTimer) {
        return
      }
      this.delayTimer = window.setTimeout(() => {
        this.delayTimer = null
        if (this.activeCount > 0) {
          this.visible = true
        }
      }, SHOW_DELAY_MS)
    },
    finish(type = 'query') {
      this.activeCount = Math.max(0, this.activeCount - 1)
      if (type === 'mutation') {
        this.mutationCount = Math.max(0, this.mutationCount - 1)
      } else {
        this.queryCount = Math.max(0, this.queryCount - 1)
      }
      if (this.activeCount > 0) {
        return
      }
      if (this.delayTimer) {
        window.clearTimeout(this.delayTimer)
        this.delayTimer = null
      }
      this.visible = false
    },
    reset() {
      this.activeCount = 0
      this.queryCount = 0
      this.mutationCount = 0
      if (this.delayTimer) {
        window.clearTimeout(this.delayTimer)
        this.delayTimer = null
      }
      this.visible = false
    }
  }
})
