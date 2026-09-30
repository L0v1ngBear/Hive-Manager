import { randomUUID } from 'node:crypto'

export function releaseVersionPlugin(buildId = randomUUID()) {
  return {
    name: 'hive-release-version',
    apply: 'build',
    config() {
      return { define: { 'import.meta.env.VITE_HIVE_BUILD_ID': JSON.stringify(buildId) } }
    },
    transformIndexHtml() {
      return [{ tag: 'meta', attrs: { name: 'hive-build-id', content: buildId }, injectTo: 'head' }]
    },
    generateBundle() {
      this.emitFile({ type: 'asset', fileName: 'version.json', source: JSON.stringify({ buildId }) + '\n' })
    },
  }
}
