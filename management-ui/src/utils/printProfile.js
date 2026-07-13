const STORAGE_PREFIX = 'hive.print.profile.'

export const PRINT_PROFILE_KEYS = {
  LABEL: 'browser_label',
  RECEIPT: 'browser_receipt'
}

export const DEFAULT_PRINT_PROFILES = {
  [PRINT_PROFILE_KEYS.LABEL]: {
    paperWidthMm: 70,
    paperHeightMm: 50,
    pageMarginMm: 0,
    offsetXmm: 0,
    offsetYmm: 0,
    scale: 1
  },
  [PRINT_PROFILE_KEYS.RECEIPT]: {
    paperWidthMm: 215.9,
    paperHeightMm: 139.7,
    pageMarginMm: 0,
    offsetXmm: 0,
    offsetYmm: 0,
    scale: 1
  }
}

export function loadPrintProfile(key, fallback = {}) {
  const defaults = normalizePrintProfile({ ...DEFAULT_PRINT_PROFILES[key], ...fallback })
  if (typeof window === 'undefined') return defaults
  try {
    const raw = window.localStorage.getItem(storageKey(key))
    if (!raw) return defaults
    return normalizePrintProfile({ ...defaults, ...JSON.parse(raw) })
  } catch {
    return defaults
  }
}

export function savePrintProfile(key, profile) {
  const normalized = normalizePrintProfile(profile)
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(storageKey(key), JSON.stringify(normalized))
  }
  return normalized
}

export function resetPrintProfile(key, fallback = {}) {
  const defaults = normalizePrintProfile({ ...DEFAULT_PRINT_PROFILES[key], ...fallback })
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(storageKey(key))
  }
  return defaults
}

export function normalizePrintProfile(profile = {}) {
  return {
    paperWidthMm: clampNumber(profile.paperWidthMm, 20, 500, 70),
    paperHeightMm: clampNumber(profile.paperHeightMm, 10, 500, 50),
    pageMarginMm: clampNumber(profile.pageMarginMm, 0, 30, 0),
    offsetXmm: clampNumber(profile.offsetXmm, -50, 50, 0),
    offsetYmm: clampNumber(profile.offsetYmm, -50, 50, 0),
    scale: clampNumber(profile.scale, 0.5, 1.5, 1)
  }
}

export function buildPrintTransformCss(profile) {
  const normalized = normalizePrintProfile(profile)
  return `translate(${normalized.offsetXmm}mm, ${normalized.offsetYmm}mm) scale(${normalized.scale})`
}

export function injectPrintPageStyle(styleId, css) {
  if (typeof document === 'undefined') return
  let style = document.getElementById(styleId)
  if (!style) {
    style = document.createElement('style')
    style.id = styleId
    document.head.appendChild(style)
  }
  style.textContent = css
}

export function openCalibrationPrint(profile, title = '打印校准页') {
  const normalized = normalizePrintProfile(profile)
  const printWindow = window.open('about:blank', '_blank', 'width=900,height=700')
  if (!printWindow) return false
  printWindow.document.open()
  printWindow.document.write(buildCalibrationHtml(normalized, title))
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => printWindow.print(), 250)
  return true
}

function buildCalibrationHtml(profile, title) {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>${escapeHtml(title)}</title>
  <style>
    @page { size: ${profile.paperWidthMm}mm ${profile.paperHeightMm}mm; margin: ${profile.pageMarginMm}mm; }
    * { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; background: #fff; color: #000; font-family: Arial, "Microsoft YaHei", sans-serif; }
    .sheet {
      width: ${profile.paperWidthMm}mm;
      height: ${profile.paperHeightMm}mm;
      position: relative;
      overflow: hidden;
      transform: ${buildPrintTransformCss(profile)};
      transform-origin: top left;
      background:
        linear-gradient(90deg, rgba(0,0,0,.18) 1px, transparent 1px),
        linear-gradient(rgba(0,0,0,.18) 1px, transparent 1px);
      background-size: 5mm 5mm;
      border: 1px solid #000;
    }
    .sheet::before,
    .sheet::after {
      content: "";
      position: absolute;
      background: #000;
    }
    .sheet::before { left: 0; right: 0; top: 50%; height: 1px; }
    .sheet::after { top: 0; bottom: 0; left: 50%; width: 1px; }
    .meta {
      position: absolute;
      left: 4mm;
      top: 4mm;
      padding: 2mm;
      font-size: 10px;
      line-height: 1.5;
      background: rgba(255,255,255,.82);
      border: 1px solid #000;
    }
    .corner {
      position: absolute;
      width: 8mm;
      height: 8mm;
      border: 2px solid #000;
    }
    .tl { left: 2mm; top: 2mm; border-right: 0; border-bottom: 0; }
    .tr { right: 2mm; top: 2mm; border-left: 0; border-bottom: 0; }
    .bl { left: 2mm; bottom: 2mm; border-right: 0; border-top: 0; }
    .br { right: 2mm; bottom: 2mm; border-left: 0; border-top: 0; }
  </style>
</head>
<body>
  <div class="sheet">
    <div class="meta">
      <strong>${escapeHtml(title)}</strong><br>
      纸张：${profile.paperWidthMm}mm × ${profile.paperHeightMm}mm<br>
      边距：${profile.pageMarginMm}mm<br>
      偏移：X ${profile.offsetXmm}mm / Y ${profile.offsetYmm}mm<br>
      缩放：${profile.scale}
    </div>
    <i class="corner tl"></i><i class="corner tr"></i><i class="corner bl"></i><i class="corner br"></i>
  </div>
</body>
</html>`
}

function storageKey(key) {
  return `${STORAGE_PREFIX}${key}`
}

function clampNumber(value, min, max, fallback) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) return fallback
  return Math.min(max, Math.max(min, Number(numberValue.toFixed(3))))
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
