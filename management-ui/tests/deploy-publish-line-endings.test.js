import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

test('publish script uses Linux line endings after checkout', () => {
  const publish = readFileSync(new URL('../../deploy/publish.sh', import.meta.url), 'utf8')

  assert.match(publish, /^#!\/usr\/bin\/env bash\nset -euo pipefail\n/)
  assert.doesNotMatch(publish, /\r/)
})
