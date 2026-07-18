import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'

const projectRoot = process.cwd()
const distRoot = path.join(projectRoot, 'dist')
const manifestPath = path.join(projectRoot, 'dist-manifest.sha256')

const sha256 = (content) => crypto.createHash('sha256').update(content).digest('hex')

const walk = (directory) =>
  fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = path.join(directory, entry.name)
    return entry.isDirectory() ? walk(entryPath) : [entryPath]
  })

if (!fs.existsSync(path.join(distRoot, 'index.html'))) {
  throw new Error('management UI dist is missing index.html')
}

const files = walk(distRoot).map((filePath) => ({
  filePath,
  relativePath: path.relative(distRoot, filePath).split(path.sep).join('/'),
}))

files.sort((left, right) =>
  Buffer.compare(Buffer.from(left.relativePath), Buffer.from(right.relativePath)),
)

const manifest = files
  .map(({ filePath, relativePath }) => `${sha256(fs.readFileSync(filePath))}  ${relativePath}\n`)
  .join('')

fs.writeFileSync(manifestPath, manifest, 'utf8')
console.log(`Wrote ${files.length} management UI files to ${manifestPath}`)
