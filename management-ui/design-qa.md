# Login Layout Design QA

## Evidence

- Source visual truth:
  - `D:\HiveManager\management-ui\design-qa-artifacts\reference-risemap-login-desktop.png`
  - `D:\HiveManager\management-ui\design-qa-artifacts\reference-risemap-login-mobile.png`
- Implementation:
  - `D:\HiveManager\management-ui\design-qa-artifacts\implementation-hive-login-desktop-final.png`
  - `D:\HiveManager\management-ui\design-qa-artifacts\implementation-hive-login-mobile-final.png`
- Side-by-side comparisons:
  - `D:\HiveManager\management-ui\design-qa-artifacts\comparison-login-desktop.png`
  - `D:\HiveManager\management-ui\design-qa-artifacts\comparison-login-mobile.png`
- Desktop viewport: 1440 × 900 CSS px, device scale factor 1.
- Mobile viewport: 390 × 844 CSS px, device scale factor 1.
- Comparison normalization: implementation screenshots were scaled to the matching source viewport only for the side-by-side comparison because Chrome excluded the native scrollbar gutter from captured pixels.
- State: account login selected, password hidden, form empty.

## Full-view comparison

- Desktop uses the same approximately 56/44 full-height split, dark brand area, white form area, centered 30rem form column, heading-first hierarchy and segmented login selector.
- Mobile hides the desktop brand panel and preserves the source rhythm: centered brand lockup, welcome heading, mode selector, fields, remember/reset row and primary action.
- The source network-path artwork was intentionally omitted because the user requested layout-only changes and explicitly rejected a generated image.
- The source has four login modes; the implementation intentionally keeps only the existing account and scan modes.

## Focused comparison

- Form controls: labels, 44px desktop inputs, restrained radii, selected tab elevation and full-width primary action follow the source density and alignment.
- Password recovery: “忘记密码？” is aligned to the right of “记住账号”, matching the reference placement.
- The focused form region is large enough in both comparisons to inspect type, spacing, control borders and action hierarchy without additional crops.

## Required fidelity surfaces

- Fonts and typography: system sans-serif stack retained; heading weight, field-label weight and helper hierarchy match the source while preserving Chinese readability.
- Spacing and layout rhythm: split ratio, form width, heading/tab/input sequence and mobile vertical rhythm align with the reference.
- Colors and visual tokens: reference navy/white balance retained; Hive teal remains the intentional product accent instead of copying the source blue.
- Image quality and asset fidelity: only the existing Hive logo is used. No generated image, copied RiseMap asset, placeholder, emoji or handmade illustration is present.
- Copy and content: Hive branding, 杭州毫端科技有限公司 and existing product actions are retained. All “首次登录 / 忘记密码” login-page wording was replaced by “忘记密码？”.

## Interaction and runtime checks

- Account and scan tabs both resolve uniquely, switch correctly and expose the linked tab panels.
- Account mode was restored after interaction testing.
- No horizontal overflow at desktop or mobile viewport.
- Browser console errors and warnings: none after the local page settled.

## Comparison history

1. Initial mobile implementation placed the welcome block and controls too low compared with the source.
2. Reduced mobile brand/control spacing and input height, then aligned the welcome heading to y=226 and the mode selector to y=305 at 390 × 844.
3. Post-fix comparison found no remaining actionable P0, P1 or P2 layout mismatch.

## Follow-up polish

- P3: the global compliance footer remains below the fold on mobile and can produce a native scrollbar gutter. It does not obscure the login task or change the requested login-page layout.

final result: passed
