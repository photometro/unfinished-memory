# Browser prototype verification

Current revision: Study 18, requested directly in this chat. Default writing → before sealing → seal. Cues are optional inline choices/short edits; policy, preference and context remain optional full task pages. Study 16's environment and the protected paper/current sources are unchanged. This is not a #7/#8 backlog status transition. Older studies below are historical snapshots; capture-preview.mjs is retired.

## Study 18 — original first, optional cues, explicit continue

2026-10-06. [Scope, actual checks and limits](verification/leave-study-18/REPORT.md) · [Screenshot gallery](verification/leave-study-18/index.html).

69/69 pure tests, paper geometry (40,040 samples), current phase contract and build pass. 23 protected source hashes match the pre-study-18 backup. Browser checks cover text/photo-only, photo view/remove hit separation, cue select/unselect/edit/custom, auxiliary returns, fixed-date validation, draft save/resume, failed submission and single-record retry, reading actual saved input, normal completion with continuing current, no-cue sealing and night/reduced quiet mode. Five actual container sizes, 130% text plus simulated safe area, and an independent limited-height editor were checked. This does not validate native keyboard, AI/V3, persistence, accessibility or performance. Details and intermediate tooling failures are in the report; visual acceptance remains pending.

## Study 17 — leaving UI, optional tasks and responsive composition

Full scope, Before/After/Why, actual browser checks and limits: [REPORT](verification/leave-study-17/REPORT.md). Actual screenshots: [UI gallery](verification/leave-study-17/index.html).

63 tests, 40,040 paper geometry samples, completion-current script and build passed. All 23 protected old source files match `.history/2026-10-05-before-leave-study-17/`. Actual browser checks covered default path, optional updates/returns, direct editing, draft resume, local photos/photo-only review/reading, fixed-date validation, pending lock, failure+retry, normal completion current, night and reduced quiet mode. Five actual preview containers, 130% text plus simulated safe areas, standalone limited height and normal phone ratio were inspected. Responsive short standalone tasks no longer add workbench top padding.

CUA screenshot and requested viewport units differ from observed DOM sizes. The final five-page gallery retains observed full App bounds; a failed early coordinate crop and pre-fix outer-scroll samples are not shown as final UI. Native Android keyboard, system text scaling, real safe areas, assistive technology, FPS/GPU/memory, production photo processing and user visual sign-off remain untested. In-memory prototype only; no AI/V3/native installation/publication or knowledge upload.

Status: local candidate delivered; **user visual acceptance pending**. Original theme, embedded font, backgrounds, continuous page folding and completion current retained.

## Study 16 — selected daylight, calm water and spatial entry

2026-10-05. Local candidate checks below do not constitute user visual/native acceptance.

- 61/61 unit tests pass. New pure field/mask tests assert progress endpoints, monotone coverage, simultaneously transparent flats/opaque channels, featureless fallback. Five loader cases cover missing sea, missing shore with unresolved sea, admission deadline ignoring late sea, paired decoded readiness and cancelled callbacks. Historical tide tests preserve dormant code, not active-home tide verification.
- `verify-paper.mjs`: 40,040 finite samples; `verify-journey-current.mjs`: completed current remains active. Build passes with existing nonfatal use-client warnings, no dependency install.
- Three complete home captures and three paired entry start/mid captures saved. Mid entry samples: daylight progress .5631 / coverage .6861; slant .5349 / .6764; night .5798 / .6919; all sea-layer opacity 1 and stage ebb. Actual screenshots show exposed upper flats and covered lower estuary. Normal entry ends stage shore, opening canvas 0, tide 0.0000, paused true, animationActive false. Replay, skip and mid-entry reduced motion end on the same fixed shore without canvas.
- Actual computedStyle: 12 combinations A/B/C × quiet/encounter × daylight/night all pseudo-element backgroundImage none/backgroundColor transparent. No photographic filter. C encounter's late CSS rule is included, correcting R4's insufficient single-file check.
- Actual App bounds: 319.989×567.990,389.992×843.990,767.990×1023.987 CSS units. Background photo covers the inset surface, not merely outer App borders. Small130% main text + simulated44/34 safe area: settings/leave/past actions inside App, heights43.990/47.988/45.989. Subpixel zoom rounding is not a device-specific layout rule.
- Dark on: source手动深色/periodnight; off: source当地时间自动, settings route unchanged and no entry canvas. Pure local clock boundary/legacy-noon checks remain in the 61 tests; actual advancing-boundary replay is historical Study15, not repeated here.
- Runtime pixel read attempt was unsupported by the read-only browser DOM wrapper (`getContext` unavailable); no bypass. Spatial behavior evidence is actual mid screenshots plus pure alpha tests, not a browser pixel-perfect claim. First DOM-coordinate screenshot crop was blank due host screenshot/DOM scaling; replaced with uncropped full-page captures. A body Control+Home action timed out and was not used as evidence.
- Source/asset review: 12 protected sources byte-identical to this Run backup; main only version/environment prose, journey only removal of two homepage gradients, styles removal of C encounter gradients. All seven new PNGs 887×1774; exact selected source SHA in ASSETS. Six source images may remain cached (~36MiB decoded RGBA lower-bound estimate); entry mask/canvas buffers additional. No heap/GPU/native frame-rate benchmark or claim all images released.
- Final fictional flow (after source/document updates stopped): “最终回归示例：同一段内容，随平静水面一起交给时间。” Four steps reach review; daylight→slant preserves content/cue. Simulated submit failure retains review/text and3 records, no successful seal. Retry reaches paper-boat phase complete with4 records and one visible canvas under `.current-field` (opacity1,490×1091); subsequent night switch keeps phase complete and one `.seal-journey canvas`. Return岸边 works. The first `.current-map canvas` probe was an incorrect selector, not a missing-current result; correction is recorded in evidence.json. Motion remains active by the existing current verification script; no pixel/frame-rate benchmark claim.
- App-only responsive view follows viewport changes rather than the desktop preview shell. Requested overrides390×844/320×568/768×1024 measured inner582×1260/478×848/1146×1528 due host zoom. Photos cover the actual measured surfaces. These screenshots prove responsive aspect/coverage, not exact CSS viewport dimensions; explicit measured container tests above cover the target320/390/768 widths. Overrides reset and temporary responsive tab closed.
- Test content belongs only to the separate temporary QA tab; no original-tab test input or manual reload. File-update hot reloads can reset the memory-only prototype, so final interaction verification ran after source/document edits stopped. Final synchronization is independently reviewed in the Run report.
- Browser real network failure/delay, hidden/resume/timezone jump, complete typography/keyboards, actual safe areas and Android performance remain untested. User visual review remains pending.

Evidence: `verification/UI-20261005-8-06/`. Report: `docs/implementation-reports/UI-20261005-8-06.md`.

## Study 15 — three states and paused homepage tides

2026-10-05. Candidate implementation and local validation complete; #8 pending-validation for user visual approval. Existing opening, writing, boat and current assets/code are protected. No new image generation or editing.

- `node --test tests/*.test.mjs`: 55/55. Includes local boundaries/no11:00 change, legacy noon alias, all1440 local minutes, paused target across old tide modes, 15 cover/rotation cases and three independent-load failure/cancellation cases. Historical tide tests remain preservation checks only, not active-home tide acceptance.
- `node scripts/verify-paper.mjs`: 40,040 finite connected-face samples; `verify-journey-current.mjs`: completion keeps current active. `npm run build`: passes with pre-existing nonfatal use-client bundler warnings. No dependency install.
- Separate localhost QA tab, no test content typed into or reload of the original user tab. Three-state selector contains only 日光/斜阳/夜色. Completed surfaces show `renderer=photo`, `tidePaused=true`, `tide=0.0000`, `animationActive=false`, matching shown/requested period; no homepage canvas or photographic CSS filter. Legacy `tide=high` remains in QA URL but cannot change the fixed shore.
- Actual complete-page captures: `daylight.jpg`, `slant.jpg`, `night.jpg`. Viewed in context: calm title area, fine middle/lower creeks and bottom actions. These are candidates, not user acceptance or exact terrain-correspondence certification.
- Night replay observed original sea layer near opacity .9934, then shore. Daylight replay skipped and slant replay reduced mid-entry: both end at shore, no sea layer, same effective image, paused fixed target. No homepage tidal timer is mounted; source-only pause/cleanup reviewed. Browser delayed-sea appearance is not tested.
- Simulated local clock advances at 05:59:58,13:59:58,17:59:58 reach morning/afternoon/night. Real system time not changed. Dark on overrides daylight preview to night/source手动深色; dark off clears override/clock and returns source当地时间自动 while staying in settings.
- Measured App CSS bounds: small319.989×567.990; regular389.992×843.990; wide767.990×1023.987. Background surface is inset by its existing border, so image coverage must be compared to the surface, not outer App bounds. Cover/A-B-C rotations are proportional; tiny ~.013px DOM rounding differences are not an intentional gap. Small130% main text+simulated44/34 safe area: three home action rectangles are inside the App bounds, heights~44/48/46. Evidence: small-large-safe/wide/variant-b screenshots and evidence.json. This is not real Android safe-area or full system font-scale testing.
- Fictional draft “三态回归演示：岸边不涨退，纸船仍交给洋流。” remains in the final review after daylight→slant switch. Failure alert retains review/text with3 records and no success animation; retry reaches `seal-journey[data-phase=complete]`,4 records and one current canvas. `failed-submit.jpg`/`seal-complete.jpg`. Waiting for the long animation initially exceeded the browser bridge deadline; subsequent visible state confirmed completion, not a changed route.
- Twelve protected source files are byte-identical to `.history/UI-20261005-8-05/src/`: journey-state,writing-state,journey.css,PageFold,PaperBoat,paper-geometry,MemoryMotion,current-flow,Ocean,water-shader,shore-terrain,environment-material. Existing sea SHA unchanged. New copies match recorded review-image hashes in ASSETS.
- Independent preflight required paused timer/legacy compatibility/unfiltered assets; post-review found shore/sea Promise.all failure coupling. New independent loader and three injected unit cases close P-01; independent rerun55/55 and source re-review passed. Browser actual network failure, slow decode, hide/show/timezone jump, all layout/large-text matrices and native memory/performance remain pending. Final browser warn/error query returned [] for this session.
- Eight screenshots are in `verification/UI-20261005-8-05/`. Screenshot capture uses host zoom/scroll and does not prove pixel-exact viewport size. No Android build/install, backend/V3, Git commit/push or Dify upload. Full report: `docs/implementation-reports/UI-20261005-8-05.md`.

## Study 14 — restored opening, photo-bank terrain, local light and slow tide

2026-10-05. Candidate implemented and locally reviewed; final visual acceptance remains pending. No bitmap editing or generation. Evidence directory `verification/UI-20261005-8-04/`; `index.html` compares current four periods/three tides and retains a Study 13 history comparison.

- Final tests: 49/49 (41 environment, 8 journey); `verify-paper.mjs` verifies 40,040 samples; `verify-journey-current.mjs` passes; production build passes with existing non-fatal use-client module directive warnings. No console error/warning during the captured QA flow.
- 12 WebGL state images: morning/noon/afternoon/night × low/mid/high. Each capture waits for actual DOM tide .0800/.3000/.5200 and light-settled=true; it is not a shader thumbnail. `four-times-three-tides.json` retains lighting, tide, wet history, mapping and backend for every frame. Wet history intentionally remains after lowering tide, so identical instantaneous tide need not mean identical wetness after different paths.
- 8 forced CPU images: all four × low/high; `static-states.json`. Shared quantized NEAREST geography/material/light formula; no animated refraction in fallback and some edge-antialiasing difference. This is visual comparison, not pixel-exact certification.
- Four normal opening captures start near tide .996–.998 on old sea texture. Normal, skip and mid-intro reduced endpoints all observed at tide .3000, recentTide .3000, shore/home; one environment throughout. `intro-states.json` and `intro-*.jpg`. Direct replay is disabled while reduced, so the test activates reduced during entry rather than forcing a disabled control.
- Actual measured App bounds: 319.989×567.990, 359.995×639.992, 389.992×843.990, 429.997×931.996, 767.990×1023.987. Cover mapping stays proportional, homepage actions remain inside these bounds. `layouts.json`; B/C full-page evidence retained. Browser viewport override is temporary and not a CSS-size assertion: host zoom causes different coordinate systems, and some screenshot crops contain offsets. All dimensions derive from observed DOM; do not claim screenshot crop dimensions prove layout.
- Short screen 320×568 +130% text+simulated44/34 safe area: DOM action heights~48/46, contained in frame. `short-large-safe.json`. The cropped screenshot has scroll-offset capture limitations and does not fully prove large-font aesthetics; real safe area and complete device text scale remain pending.
- Fictional content: “界面验证用的虚构记忆：午后沿河散步，风吹过树梢。” stays after noon→afternoon. Four main steps preserve content; confirmed seal increments demo records 3→4 and reaches its complete phase (app route remains `seal`, not a `complete` route). The existing current stays visible with the boat gone. `flow-review.txt`, `flow-complete.txt/json`, full screenshot. Original user tab was not reloaded or given test data.
- Nine protected source files (journey-state, writing-state, journey.css, PageFold, PaperBoat, paper-geometry, MemoryMotion, current-flow, Ocean) are byte-identical to the Run backup. Mother SHA unchanged; restored opening asset SHA `C567A428A7DB905037F067B876A1B963BD2133B3C52969F4C6E51BE46E6A6CD7`.
- Independent review found and closed normal-entry wet-history mismatch and static/reduced non-drying. Final re-review has no source blocker; wetHistory has a pure endpoint/frame-rate test. A bounded 0.5Hz wet timer handles static/reduced drying and stops off-screen/unmounted.
- First atlas preparation in this browser session about 876ms; cached backend switching 0–0.1ms. Packed cache 1,577,088 bytes; three uploaded textures estimate4,731,264 bytes, decoded sources estimate12,588,304 bytes. These are not heap/GPU/native allocations or frame-time measurements. First-load long-task/Android performance remains explicitly pending.
- No Android build/install, V3 memory logic, backend, deployment, Git push or Dify upload. Prior external-query rejection was not bypassed. Current technical progress is not user visual acceptance.

## Study 12 — same geography, local lighting, artistic tide and shared intro

2026-10-04. User selected the attached image; its bytes match shore-master-v4.png. Technical implementation complete for the candidate, #8 remains pending-validation; final visual and uncovered runtime checks are explicitly pending, not waived.

- Build passes, with existing non-fatal use-client bundler warnings. 40 Node tests pass (32 environment/mapping/architecture, 8 existing writing). Existing paper geometry check: 40,040 finite samples; completed-current state check passes. Protected journey.css, PageFold, PaperBoat, paper-geometry, MemoryMotion, current-flow, writing-state and journey-state are byte-identical to this Run backup.
- Browser QA uses Codex in-app browser on loopback 5189. Observed one natural-surface, WebGL two textures, loaded 887×1774 pair; byte budgets are estimates, not measured GPU allocation. Backend and mapping are observed DOM diagnostics, no React internals or injected events.
- Observed actual container bounds, CSS pixels: 319.989×567.990, 359.995×639.992, 389.992×843.990, 429.997×931.996, 767.990×1023.987. Both home actions contained in every container, heights about48 and46 px. These are actual resizeable App windows, NOT browser viewport claims. Browser viewport override 320×568 was distorted by host67% zoom (innerWidth478/height848), so reset it and use directly measured container sizes. App-only view removes workspace chrome but does not override browser zoom.
- CPU and GL share source affine; compared 390×844 high-tide screenshots. Also CPU narrow320 and wide768 mapping matches GL diagnostics exactly. Static water has no animated refraction; that difference is intended. Static normal intro: frame counter11 at sea/1.0 then37 at shore/.3, only26 additional draws; then idle with fixed tide, no endless no-op rAF. This is a draw-count sample, not frame-time certification.
- High tide .52 remains fixed across morning/noon/afternoon/night clicks; each had one surface and home route. High water retains channel traces, control bounds stay fixed. Low/mid/high and continuous transitions have been viewed; color/mood still needs user visual judgment.
- Settings dark mode tested from four preview periods: on→night, off→local-hour20/night/source当地时间自动, route stays settings, fixed tide stays .3000. Ordinary settings has dark override only, no manual four-period modes. No persistence or system-theme-follow feature added.
- Foreground clock preview was filled at 05:59:58; observed night then morning after scheduled boundary, subsequent writing and afternoon preview preserved exact fictional content. Date/time input required onInput compatibility (onChange-only bridge fill did not update React); corrected and rechecked. No system clock change.
- Normal auto intro and repeated intro finish at shore/current target. During intro, switched to morning and mid tide, then skipped: stage shore, tide .3000, one surface. Enabling reduced during intro directly finished at .3000; no intro restart afterward. In short320×568 with130% main text and simulated safe44/34, header top44px, skip top44px, actions remain inside. This is selected key-text enlargement and simulated safe area, not full system-font scaling or Android certification.
- Existing flow regression: fictional draft persists when lighting switches, saves and resumes, all four steps advance. Simulated failure retains original text, three memories and review route without success motion; retry adds exactly one memory and reaches seal complete. Switching afternoon→night there leaves phase complete; existing current field remains mounted. Console warning/error query during failure run returned empty. No continuous-current source changes.
- Independent preflight identified old double surfaces, fixed intro0/home0, eight textures and fixed UV. Independent post-review found static throttle, exact boundary scheduling and skip safe area P2s; fixed and re-reviewed Pass (source-only, not visual acceptance).
- Not executed: real background/timezone/system-clock jump exercise, native context-loss or asset-failure injection, all A/B/C visual matrices, actual Android safe area/keyboard/font/performance, heap/GPU profiling. Source pause/cleanup reviewed, does not substitute runtime tests. No native/algorithm/publish/knowledge operations.

Evidence: `verification/environment-v4/` screenshots and the corresponding local report `docs/implementation-reports/UI-20261004-8-02.md`. Recovery: `.history/UI-20261004-8-02/`. Selected artwork remains unchanged; #7 and overall #6 acceptance remain independent.

## Study 11.1 — original composition, improved lower-half creek detail

2026-10-04. The user rejected v1's homepage fit and explicitly asked to apply the recommendation: keep the original composition/palette, improve only middle-lower creek detail. One built-in image edit of the original noon shore was generated; the user's second photo is structure-only reference. The new file is shore-master-v2.png, v1 and original textures are retained. Exact prompt and inputs: SHORE-V2-PROMPT.md. This is still a visual candidate, not a user-approved master or real photograph.

- Reused existing DAYLIGHT candidate constant, RealisticSurface input, original/candidate studio switch and A/B/C route. Source edits are limited to the candidate filename, renderer data label and studio-only copy. No homepage/control geometry, functional palette, CSS, fold, current, algorithm or native code was edited.
- V2 maintains a warm pale low-detail upper half, original broad trunk/estuary placement and lower-right water, with finer tapering channels in lower land. Generated edits are not guaranteed pixel-identical or scientifically accurate; judge the actual rendered composition.
- In the Codex in-app browser, A shows the title on quiet pale terrain, lower fine tributaries and navigation over open water. Original/noon and v2 selection both work; the candidate data attribute is candidate-v2, its observed image src points to shore-master-v2.png, and the renderer is WebGL. Default document has no horizontal overflow. Captured final browser warnings/errors are empty.
- Existing B/C transforms were viewed and captured, not reauthored or accepted. B still places detailed channels behind the upper text; C places the small top status over water. Those legacy composition tradeoffs remain for a later layout decision. A is the main preview for this targeted asset iteration; no universal crop/readability acceptance is claimed.
- Build, eight writing-state tests, 40,040-sample paper geometry check and completed-current phase check pass. Existing non-fatal use-client bundler warnings remain. This asset-only pass does not re-certify the entire workflow, physical device, 320px/large-text/keyboard matrix, dynamic crop, performance or tide model; see Study 11 for previously covered checks.
- The old preview service had stopped. It was restarted on loopback port 5189; a stale test tab remained on an internal connection-error page, so verification used a fresh test tab in the same selected browser. Automated cleanup of that stale tab was also rejected by its URL check; it was left untouched rather than bypassing the check. No browser safety setting, user tab input, proxy or firewall was changed. Screenshot evidence is actual rendered UI, not generated mockup.

Evidence: verification/shore-v2/home-v2.jpg, home-original-same-layout.jpg, home-v2-b.jpg and home-v2-c.jpg. Pre-edit source/document recovery: .history/2026-10-04-before-shore-v2/. Pending: user visual judgment of A/v2 before new time derivatives. No backlog write, production deployment, Git commit, push, APK, AI or V3 implementation.

## Study 11 — four writing steps, functional colours, neutral shore master candidate

2026-10-04. Run UI-20261004-7-8-01 implements backlog #7 R3 and only stage one of #8 R1 in the realistic browser prototype. The user confirmed the two optional full-page routes before implementation. Both items remain pending-validation: #7 awaits visual judgment and further accessibility/device checks; #8 must stop for master-artwork approval before its time, tide and production-crop stages. This is not native App or V3 algorithm acceptance.

Implemented:

- Four full-page tasks: 写下 / 留线索 / 怎样回来 / 收好. Optional 补充背景 and 回忆偏好 pages return to their invoking step and keep central draft edits. Confirmation contains read-only results and the sole final submit. No modal task cards or ordinary pale writing boards remain.
- Default local cue suggestions still require explicit adoption. Adopt, edit, remove and skip remain distinct. Fixed date is checked at the policy step and defensively before submit; quiet hides inapplicable preference without erasing it.
- Day functional roots: writing/reading #F2EADF, Past #E6EDF0, Settings #E8ECE6. Night: #302D29 / #263842 / #2D3732. Inputs and content groups are transparent; functional action colours and focus contours use the frozen R2 palette. Snapshot colour is defined on the captured page root, not inherited from a changing ancestor.
- One generated neutral-daylight tidal-tree master is a separate candidate, not a replacement for the eight original textures. Studio selection fixes it to noon and labels it as a candidate. Static 9:16, 9:19.5 and 3:4 cover previews are artwork checks, NOT a production shader/UV adaptation implementation. Reference, exact generation prompt and provenance are recorded in ASSETS.md and DAYLIGHT-PROMPTS.md.
- Accepted paper-to-boat and vector current architecture is retained. Pending submit also disables studio navigation, theme/composition switches and associated keyboard shortcuts, so leaving Review cannot silently unmount its pending commit.

Verified interactively using fictional data in an agent-created Codex in-app browser tab:

- Empty Next reports and focuses the editor. Temporary save/resume keeps text. Background fields/source survive arrow return, reopen and Escape. Cue adoption/edit/removal/skip work. Preference and fixed date survive auxiliary return and policy changes. The final confirmation has no editable input/select/radio controls.
- Date filling initially changed the native DOM value without updating the React draft. A shared onInput/onChange handler now keeps draft and date value synchronized; subsequent auxiliary return and confirmation retain the same ISO date. This is a compatibility correction observed through the browser UI, not a real-device date-picker certification.
- Simulated failure keeps the draft and initial three-record pool, mounts no Seal and plays no success animation. Successful retry adds one record. During pending, submit and studio/switcher navigation are disabled. Navigation and commit-lock guards were independently reviewed in source.
- Successful day and night page snapshots preserve the original text, page-root colour and actual nonzero scroll. Sample captured values: day rgb(242,234,223), scrollTop about 403; night rgb(48,45,41), scrollTop about 298. This is a current captured-state check, not frame-by-frame scroll instrumentation.
- Normal completed flow remains mounted and visibly moves after the boat departs. Two stable complete-state JPEGs were compared with read-only pixel sampling every three pixels: 30 samples exceeded summed RGB delta 30, within the ocean area. Reduced-flow counterparts had zero such changed samples. This is rendered moving/static evidence, not an FPS, power or hidden-tab benchmark.
- Quiet reduced completion shows an intact stationary boat and no current Canvas. Reduced ordinary completion goes directly to complete with static flow. Returning home clears the mesh and field after the existing exit transition; no accepted motion-file edit was required.
- Day writing roots are consistent at morning/noon/afternoon with transparent input backgrounds. Stable night writing, Past and Settings use the specified dark roots. Night confirmation and long-content folding were inspected. Immediate theme-transition intermediate colours were not counted as final settled colours.
- Old/candidate studio selection, A/B/C compositions and the neutral-noon candidate data attribute work. The candidate uses the existing WebGL surface. Embedded Unfinished Noto font reports loaded. Final sampled browser warning/error logs contain no captured entries.
- Actual narrow viewport 358x640 CSS px and short viewport 375x560 CSS px retain a page-contained footer, natural content scrolling and no document horizontal overflow. Larger 1200x1000 and 1200x1200 viewports also have no document horizontal overflow. The preview shell stays capped near 390 CSS px, so these are not native/tablet-width acceptance checks. The requested 320 px width was not achieved in this browser environment and is explicitly NOT claimed. Tab from the date input was exercised; a real soft keyboard was not.

Local validation:

- node --test tests/*.test.mjs: 8/8 pure writing-state tests pass.
- node scripts/verify-paper.mjs: 40,040 finite face/matrix samples, connected edges, endpoints and key-pose/material continuity pass.
- node scripts/verify-journey-current.mjs: phase gate, including active normal completion, passes.
- Production build passes; pre-existing non-fatal animation-library use-client warnings remain in this client-only prototype.
- #7/#8 entry schemas, revision synchronization and skill-contract compatibility pass. Local implementation report is docs/implementation-reports/UI-20261004-7-8-01.md.
- Independent read-only contract audit passed after correcting pending studio navigation and the stale warm-brown focus contour. PageFold.jsx, PaperBoat.jsx, paper-geometry.js, MemoryMotion.jsx, current-flow.js, journey-state.js and water-shader.js remain SHA-256 identical to the pre-run snapshot.

Evidence: verification/study11/ contains actual browser JPEGs: confirmation-day, clues-day, past-night-empty, settings-night, confirmation-night-long, fold-night-start, quiet-reduced, reduced-normal, flow-complete-1/2, reduced-flow-1/2, mother-candidate-wide/b/c, policy-short, confirmation-short, mother-narrow-358, policy-narrow-358 and final-home. Narrow/short evidence reports measured CSS dimensions rather than requested viewport labels. Final-home is the clean candidate after restoring the default browser viewport and clearing fictional test inputs by reloading the agent-created QA tab; the user's existing preview tabs were not filled or cleared. Prior screenshot folders remain intact. Candidate artwork itself is generated imagery; these screenshots are rendered UI captures.

Not verified or implemented: full-app regression, exact 320/375/390 device matrix, large text, safe-area matrix, real soft keyboard, Android/WebView/Meizu 21, persistence, real AI, V3, backend, notifications, clock-based switching, dark-mode time policy, simulated tide, new four-period derivatives, UV/crop correction, asset compression, FPS/GPU/battery, or formal visual acceptance. Current candidate studio controls are manual design tools, not implemented product policies. Temporary fictional input remains in RAM and refresh clears it.

Recovery: pre-run prototype files are in .history/2026-10-04-before-items-7-8/. The original eight textures, font, abstract prototype, native App, backend and algorithm were not modified. No commit, push, APK installation, ECS deployment or knowledge-base upload occurred. #8 resumes only after explicit neutral-master approval; #7 still needs user page judgment and remaining layout/accessibility checks.

## Study 02 — initial realistic environment

2026-10-02. Isolated, sandbox-enabled headless Microsoft Edge, without attaching to a user profile.

Verified:

- Original environment assets load locally; native WebGL renderer is active.
- Full-sea observation changes visibly over time, checked with two actual rendered frames.
- Opening/ebb/replay/skip; shallow-water and exposed-ground transition visually inspected.
- Three structural compositions A/B/C, quiet/encounter and night views.
- Clues, direct reading and deferral; empty writing validation, draft retention and sealing demonstration.
- Search results and empty state.
- Manual/system reduced motion; browser without WebGL uses static textures and remains usable.
- Reading hides home controls rather than ghosting them beneath the quiet material surface.
- Local Chinese font loaded; viewport widths 320, 375, 768 and 1440 px have no document-wide horizontal overflow.
- Smoke check has no page errors or failed HTTP resources.
- `npm run build` succeeds. Client-directive warnings are emitted by the animation library; this is a client-only prototype.
- 35 original baseline files (excluding node_modules) match the pre-work SHA-256 snapshot, including prior screenshots and build outputs.

Re-run: start `npm run dev`, then `node capture-preview.mjs`. Screenshots are written to `screenshots/`.

Not verified: native Android/WebView rendering, Meizu 21 performance, GPU/battery use across devices, algorithm correctness, data persistence, final product acceptance, or physically accurate ocean/tide modeling. Input remains in memory and is cleared by refresh. Neither the original prototype nor native app/backlog was changed.

## Study 03 — four-period lighting and glass controls

2026-10-03. Updated only `tidal-realistic-prototype`. Full smoke check passed in isolated headless Microsoft Edge; `npm run build` passed after the final responsive-layout change.

Verified:

- All eight local PNG assets load: four overhead-sea images and four tidal-flat images. Noon uses the unchanged original master textures.
- Morning/noon/afternoon/night selection updates the URL and selected button. All three compositions share this lighting system.
- Switching light at the shore or in sea-observation mode preserves the tide stage. Rapidly retargeted lighting settles at the last requested period.
- Full-sea frames differ over time, so water motion is actually rendered rather than implied by a static screenshot.
- Floating controls use a computed backdrop-filter; manual lower-transparency mode changes the blur token to zero.
- Opening, ebb, replay and skip remain usable. Clues, direct reading and deferral remain usable.
- Writing validation, temporary draft retention, light changes during writing, sealing, search and empty states pass. Returning from a record opened in Past first returns to Past, then to the shore.
- Night writing, reading and solid-material settings were visually inspected. Reading hides home controls; long content remains on a quiet, mostly opaque surface.
- At viewport widths 320, 375, 768 and 1440 px, the document has no horizontal overflow and the embedded Chinese font loads. Narrow C layouts reserve navigation space when clues expand; the measured gap is at least 12 px in these checks.
- Manual/system reduced motion pass. With WebGL deliberately disabled, the selected night textures remain available and writing is usable.
- No page/console errors or failed HTTP resources were reported in the final smoke run.
- All 35 abstract-prototype baseline files still match their pre-work SHA-256 snapshot; no files were added there. Original realistic textures and old screenshots remain preserved. Prior realistic source was backed up before this change.

Re-run: start `npm run dev`, then `node capture-preview.mjs`. New screenshots are written to `screenshots/four-period/`; this does not overwrite the Study 02 screenshots.

Boundaries: this is a browser visual prototype, not a native App update. Clock-based switching is intentionally not enabled because the time boundaries are undecided. Four-period PNGs are full-quality review assets, not production-compressed delivery assets. Glass is a CSS material approximation, not optical refraction or native Liquid Glass. AI texture edits are not guaranteed pixel-identical; the tide geometry uses the noon master consistently. Native Android/WebView performance, GPU/battery use, larger accessibility text scales, final artwork acceptance and algorithm behavior remain unverified. System lower-transparency/high-contrast fallbacks are implemented in CSS, but only the manual lower-transparency switch was exercised in this smoke run.

## Study 04 — memory carrier and continuous UI journey

2026-10-03. Checked interactively in the Codex in-app browser through the browser UI, using only fictional test text. This is a UI demonstration, not a V3 algorithm test. The Study 02/03 automated reports above apply to their historical snapshots only; their old selectors do not validate this revision.

Verified through interaction and visible state:

- Empty writing is rejected. Writing can proceed to a separate pre-seal confirmation page.
- A local place sheet applies a coarse fictional event location without location access. The local cue suggestion reflects the field and can be adopted.
- A simulated failed submission keeps the draft, the cue, and the three-record pool intact; no seal scene is entered. Retrying successfully creates one demonstration record before the seal journey.
- A successful seal, the ocean view, and the manually triggered arrival display the same memory ID in their carrier DOM attributes.
- Full ocean and shore use distinct scene roles. Ocean and seal route layers are transparent to the environment, while long content uses mostly opaque surfaces. The ocean navigation control was enlarged and given a readable material background during review.
- Free recall followed by one requested clue creates a non-empty pre-clue snapshot. Updating the recall and revealing the original produces two separate locked snapshots, with 0 and 1 visible clues respectively. The original remains in its own section.
- Direct reading explicitly produces no empty recall snapshot. Deferral remains a distinct result without revealing the original or assigning an algorithm duration.
- Choosing "cannot recall" stops the prompt and removes further clue requests from that view, retaining read and defer exits.
- An optional response survives leaving the reader and reopening the record from Past. Returning to the filtered archive preserves its query. Empty search, clearing the query, and quiet-only filtering work.
- Temporary draft retention works. Switching from afternoon to night while writing preserves the draft and route.
- Fixed-date selection with no date displays an inline error instead of a seal animation. Quiet submission uses the quiet journey; the resulting record is present in the quiet archive filter.
- Manual reduced-motion and lower-transparency controls update their states; reduced motion disables opening replay. A/B/C switching works. C's encounter message and navigation had a positive measured gap of about 22 px in the current browser layout.
- The local embedded Chinese font reports loaded; the inspected default document has no horizontal overflow.
- The build succeeds. Existing animation-library client-directive warnings remain non-fatal in this client-only Vite prototype.

Limitations: a viewport-override attempt in the in-app browser did not produce reliable requested dimensions and interrupted the temporary tab on reset. Exact 320/375/390 px and short-screen results are therefore NOT claimed for Study 04. Use UI-CHECKLIST.md for a later responsive acceptance pass. Current keyboard focus containment and system-reduced-motion/static-WebGL fallbacks were reviewed in source but not fully re-exercised here. No Android/WebView, real keyboard, real persistence, server, AI, privacy-permission, GPU/battery, physical ocean accuracy, or formal algorithm validation was performed. All input stays in memory and refresh clears it.

Final shore and ocean screenshots are saved in screenshots/memory-journey/ as JPEG files. The final browser console sample contains no captured warnings or errors. The final ocean screenshot was used to strengthen the light material under text while retaining open sea at the carrier, and the build passed again after that adjustment.

Previous realistic source and notes are preserved in .history/2026-10-03-before-memory-journey/. The abstract prototype, native app, backend, backlog and algorithm were not modified.

## Study 05 — full pages, internal hierarchy and solid-blue flow imprint

2026-10-03. Checked interactively in a separate Codex in-app browser tab, using fictional text only. Changes are confined to this realistic UI prototype. The prototype and frontend design skills guided full-page composition, typography/spacing hierarchy, and isolated continuous animation instead of additional decorative card layers.

Verified through the browser UI and rendered DOM:

- Home navigation has exactly 留下一段 and 过去. The left studio's flow observation is explicitly design-only.
- Main task pages fill the phone's inner frame. Writing and reading route layers have 0px corner radius and no independent shadow; the photographic environment is hidden in task/ritual spaces.
- Writing rejects empty content; draft save/restore and light switching retain fictional input. Multiline placeholders render actual line breaks.
- Review presents the original in the overall page scroll rather than the former 86px nested preview. Context/cue and appearance policy are separate groups. A coarse fictional place can be applied and its local cue suggestion adopted.
- A simulated failed commit retains original text and cue, keeps the three-record count, and does not enter a seal scene. A retry adds exactly one demonstration record before its ritual.
- Fixed policy with no date stays on review with an inline error. Quiet commit has a flow mark but no CurrentField or drift; its record appears under the quiet archive filter.
- Normal sealing actually passes through closing/drifting/complete. During the ritual the background settles to rgb(60,96,116), the photo layer is hidden, and the carrier fades to opacity 0 at completion. Completion explicitly says the record remains in Past. Actual rendered drift and completion frames were inspected and saved.
- Skipping during the closing phase returns to the shore with only one new record; it does not resubmit or undo the saved demonstration record.
- Manual reduced motion directly gives the complete seal state with no ritual CSS animation. Night/solid-material settings are usable; the switch hit area measures approximately 48 by 44 CSS px in this layout.
- Free recall, a requested clue, and changed recall produce two separate pre-reveal snapshots with 0/1 visible clues. Reader headings distinguish 当时留下 / 这次想起 / 现在回应; original text uses 16px type and has no leading section divider.
- Direct reveal creates no empty snapshot. Cannot-recall removes further questioning while preserving read/defer exits; defer records its distinct state without revealing the original or assigning a duration.
- Saving a response collapses the editor and presents its saved text once. Reopening the same memory from Past retains that response in this tab's memory.
- Archive query survives reading and returning. Empty results and clear-search work; quiet-only filtering works.
- Local option keyboard focus stays inside the dialog when Shift+Tab wraps, Escape closes, and focus returns to the invoking context button. Main task remains separate from the local option layer.
- A/B/C remain selectable. B and C encounter layouts have a positive measured message/navigation gap of about 22 CSS px in the default browser layout, including a requested clue in C. The default document has no horizontal overflow and reports the embedded Chinese font loaded.
- Final sampled browser console has no captured warning/error entries. Production build succeeds; animation-library client-directive warnings remain non-fatal in this client-only Vite prototype.

Visual evidence is saved in screenshots/flow-imprint/ as JPEGs, including writing, review, reading, night settings, an actual flow-mark drift frame and the completed blue ritual. Intermediate screenshots may show test input, page scrolling or the earlier frame of the colour transition; seal-flow.jpg is the full ritual demonstration frame.

Scope/limitations: no new imagery, dependencies, persistent storage, AI calls, native application, backend, backlog or algorithm work. The normal ritual timing is a prototype trial, not a product contract. Submission duplication is also guarded in source; no formal concurrency test is claimed. These checks used the browser's default layout, not a reliable exact device viewport override. No new 320/375/390/short-screen matrix, system text scaling, native keyboard, Android/WebView/GPU/battery, final artwork or V3 algorithm acceptance is claimed. System-reduced-motion and canvas-less fallbacks were not newly exercised. All test data is in memory and a final reload clears it.

Revision 04 source and notes are retained in .history/2026-10-03-before-flow-imprint/. Earlier history and screenshots remain preserved. The abstract prototype is unchanged.

## Study 06 — open current field and sea-glass memory piece

2026-10-03. Checked interactively in a separate Codex in-app browser tab with fictional text. The prototype skill kept the previous revision recoverable; the frontend design skill guided compact material, restrained visual density, transform-only carrier motion and isolated continuous drawing. Only the realistic prototype was changed.

Verified through UI interaction, DOM and actual rendered frames:

- Fixed intersecting curve bundles were replaced by independent advected traces in a shared continuous field. The primary direction crosses the frame diagonally; unequal localized eddies sit away from the carrier's central starting position. Two rendered observation frames and an actual ritual drift frame were inspected; this is an artistic visualization, not real ocean data.
- The carrier now has an irregular worn, filled sea-glass silhouette, slight edge thickness and internal strata. It has no readable text, photos, envelope features, pointed hollow lens or importance colouring.
- A simulated failure leaves the original and cue on review, retains the three-record pool, and does not mount a seal journey. Retrying adds exactly one record before the ritual.
- Normal sealing passes through closing / drifting / complete. At completion the carrier opacity is 0, the background is rgb(60,96,116), the photographic environment is hidden, and the message states that the record remains in Past.
- The same new memory ID is present in the seal carrier and the archive thumbnail. Opening the record from Past displays the original fictional text unchanged. Multiple archive carriers have distinct SVG gradient IDs.
- Skipping during closing returns home with five total records after the second success; no duplicate is added.
- Quiet submission adds the sixth record and displays a stationary memory piece with no CurrentField. Manual reduced motion adds the seventh record and directly displays complete, with a stationary piece and static field.
- The arrival demonstration uses that latest memory ID. Home still has only 留下一段 / 过去, and keeps four encounter choices. A/B/C arrival layouts and C at night were visually inspected at the browser's default layout; no new home ocean button or task-page card navigation was introduced.
- The design-only observation has a moving piece that samples the same field as the traces, and manual reduced mode displays an intact stationary piece. The sampled document width equals its scroll width (684 CSS px).
- Pure-flow path calculations at container aspect ratios 1.3 / 2 / 2.8 remain finite and never descend below the starting y=.63 into the footer. This is a geometry check, NOT responsive-device layout acceptance.
- Final sampled browser console contains no captured warning/error entries. The production build succeeds; existing non-fatal use-client bundler warnings remain in this client-only Vite prototype.

Evidence: screenshots/open-current/ contains seal-closing.jpg, seal-drifting.jpg, seal-complete.jpg, quiet.jpg, reduced.jpg, past.jpg, shore-a.jpg, shore-b.jpg, shore-c-night.jpg, current-field-1.jpg, current-field-2.jpg and current-static.jpg. The actual seal-drifting.jpg is the primary demonstration frame. Closing captures may include the paper-to-blue colour transition. Observation frames may catch the carrier's design-only fade/repeat.

Scope/limitations: no new dependencies or bitmap assets, no algorithm/backlog/backend/native update, no real persistence or AI. Input remains in RAM and refresh clears it. Trial ritual duration is approximately 4.3 seconds, not a finalized product specification. Canvas scheduling, visibility cleanup and system-reduced handling were inspected in source; manual reduced mode was exercised, but no system-preference change, hidden-tab performance benchmark or canvas-less fallback was newly tested. No exact 320/375/390/short-screen matrix, large text, real soft keyboard, Android/WebView, GPU/battery, physical accuracy, concurrency or final artwork acceptance is claimed. Existing recall/dialog/settings logic was left intact, not fully re-tested in this focused visual pass.

Revision 05 source and notes are preserved in .history/2026-10-03-before-open-current/. Existing screenshots and earlier histories remain untouched. The abstract prototype and original natural assets were not modified.

## Study 07 — matte paper boat and compressed folding ritual

2026-10-03. Focused checks in a separate Codex in-app browser tab, using fictional input only. The prototype skill kept the existing A/B/C context, old revision and in-memory boundaries; animation design principles guided recognizable key poses, a cancellable ritual and reduced-motion fallback. No production App or backlog update.

Verified through UI, rendered DOM and actual frames:

- The original boat has six opaque paper faces, an elevated central fold, pointed ends, cabin shadow and thin creases. Warm paper gradients replace sea-glass translucency. Archive, arrival and ritual use the same component and memory identity; archive thumbnails measure about 44 CSS px in the default layout.
- Ordinary successful sealing passes through gathering / folding / opening / ready / drifting / complete. Captured intermediate paper halves have actual non-identity rotateY matrices. Captured SVG opening and final boat poses were inspected. This is compressed artistic folding, not a physically accurate traditional fold simulation.
- Source content appears only inside the early written-paper layer. At the completed boat pose that layer's computed opacity is 0 and the opaque boat's is 1. Archive carriers contain no printed source text. Reading the first test record from Past displays the original fictional multiline content unchanged.
- A simulated failed commit retains the original and leaves the three-record pool on Review, with no seal journey. Retrying adds exactly one memory. Skipping during folding returns home with exactly one new memory; no re-submit occurs. After the subsequent responsive adjustment, the final normal journey was exercised again.
- Quiet sealing ends with a stationary boat and zero CurrentField elements. Manual reduced motion goes directly to complete, with an intact stationary boat, hidden paper sheet and CSS ritual animationName=none. These two states were rechecked after the safe-space adjustment.
- Initial short-layout checks exposed carrier/footer overlap. The revised origin measures header/footer dimensions and reserves paper/boat extents; compact container rules reduce explanation spacing. At the actual observed viewport of 358 by 640 CSS px, quiet and manually reduced states have about 27 CSS px of footer clearance and positive header clearance, without document-wide horizontal overflow.
- The viewport override did not yield the requested exact dimensions (a requested 320 by 640 initially reported 478 by 955). Only the measured actual viewport results above are claimed, NOT an exact 320/375/390 device matrix. The temporary override was reset.
- The final ordinary ritual still settles to rgb(60,96,116), hides the photographic environment, fades the boat out and says the record remains in Past. No sinking, shredding, vortex swallowing or arrival countdown was added.
- The latest ordinary memory ID is also used by the manual arrival demonstration. A/B/C arrival and C-at-night were inspected; home navigation remains only 留下一段 / 过去, with the four existing encounter choices. The studio observation remains design-only.
- Final sampled browser console reports no captured warning/error entries. Production build passes; existing non-fatal use-client bundler warnings remain in this client-only project.

Evidence: screenshots/paper-boat/ holds actual full-page browser frames: folding.jpg, opening.jpg, ready.jpg, drifting.jpg, complete.jpg, quiet.jpg, quiet-short.jpg, reduced-short.jpg, shore-a.jpg, shore-b.jpg and shore-c-night.jpg. Short-layout screenshots may be scrolled because the studio controls sit below the phone. The final drifting.jpg is the primary visual demonstration. Earlier evidence directories were not overwritten.

Limitations: no full-app regression suite, system-preference emulation, exact device-width matrix, large-font or keyboard test, hidden-tab profiling, WebView, native device, GPU/battery, storage, AI or V3 algorithm validation. During the short fold, six SVG face paths repaint; only the position/fade stage uses transform/opacity, so GPU-only performance is not claimed. Trial duration is approximately 4.1 seconds. All input is temporary RAM state, cleared on reload. Readability and fold choreography still require user visual acceptance.

Revision 06 source and notes are recoverable in .history/2026-10-03-before-paper-boat/. Natural images/fonts, the abstract prototype, Android App, backend, algorithm and IMPROVEMENTS.md were not changed.

## Study 08 — continuous confirmation-page to paper-boat transition

2026-10-03. Focused interaction and rendered-frame checks in an independent Codex in-app browser tab, using only fictional input. The prototype skill preserved revision 07; emil-design-eng guided source continuity, transform origins, cancellable sequencing and reduced motion. Only this realistic prototype was edited.

Verified through actual UI interaction and DOM measurements:

- Review and Seal now share a stable route host, avoiding an exit fade and separate page entry. Successful submission captures an inert, aria-hidden local visual copy of the entire Review page before removing it. The initial copy fills the phone's inner content bounds; source multiline text and entered cue survive. Copies have no duplicate field IDs and no event handlers. No secondary popup or interactive form was added.
- Withdrawing / gathering / folding / opening / ready / drifting / complete were observed and saved as actual rendered frames. The original page uniformly contracts, revealing solid blue behind its edges; two original-page halves fold before the existing six-faced boat opens. The former independent tiny paper-sheet layer is absent from the ritual. The completed drift leaves no PageFold layer and has carrier opacity 0.
- The first long, scrolled check exposed a scrollbar-gutter bug: hiding the copied scrollbar widened the text, changed wrapping and clamped scrollTop. Capturing its actual gutter as equivalent padding corrected this. Retesting a 25-line fictional record showed EXACT matching original/copy values: scrollHeight 2022 CSS px, scrollTop 1339.019 CSS px, text width 309 CSS px and text height 1091 CSS px, in both halves. The page no longer jumps to the top or reflows during contraction.
- Simulated failure retained the original and cue on Review, kept the three-record pool and mounted no seal scene. Retry added one record. Skipping during folding returned home with one additional record, without re-submit. The source cancellation path stops timers and animation controls immediately; unloading cleans remaining controls.
- The first successful memory identity matched its archive carrier. Reading from Past displayed the fictional multiline original unchanged. This was checked before the gutter-only correction; data and archive logic were not subsequently modified.
- Quiet sealing ends with a stationary paper boat, no flow Canvas and no PageFold layer. Initial manual reduced motion goes directly to complete with a stationary boat and static field. Enabling reduced motion during gathering removes the fold and completes the same record; disabling it afterwards does not replay the ceremony or increment the count.
- Short-layout night check measured an actual viewport of 358 by 640 CSS px and an inner phone of approximately 294 by 549 CSS px. Completed quiet boat bounds retain about 27 CSS px clearance above the footer, positive header clearance and no document horizontal overflow. The scene uses rgb(39,71,88) at night. The viewport override did not reliably match requested dimensions; it was reset. This is not an exact device-width matrix or large-text acceptance.
- Final normal ritual was exercised again after the gutter correction. It retains the warm matte boat and open-flow design, returns to rgb(60,96,116), and states that the record remains in Past. No photo cut, new home entry, sinking or algorithm progress indicator was added.
- The sampled final browser console has no captured warning/error entries. Production build passes with the existing non-fatal client-directive bundler warnings.

Evidence: screenshots/page-fold/ contains before.jpg, start.jpg, gather.jpg, crease.jpg, open.jpg, ready.jpg, drift.jpg, complete.jpg, long-start.jpg, quiet.jpg, reduced.jpg, short-gather.jpg and short-quiet.jpg. They are actual browser frames, not generated mockups. Short-layout screenshots may include page scrolling caused by studio controls; reported clearances use DOM bounds relative to the scene. Earlier screenshot folders remain untouched.

Limitations: roughly 5-second prototype choreography, not finalized folding physics. Small SVG faces repaint while opening; GPU-only performance is not claimed. No full-app regression suite, system-preference emulation, exact 320/375/390 matrix, text scaling, real soft keyboard, Android/WebView, physical-device, GPU/battery, hidden-tab profiling, real storage, AI or V3 acceptance. User visual acceptance of the timing and fold shape is still pending. In-memory test input is cleared by closing/reloading the temporary QA tab.

Revision 07 source and notes are preserved in .history/2026-10-03-before-page-fold/. The existing photographs/fonts, abstract prototype, Android App, backend, algorithm and backlog were not edited.

## Study 09 — one connected paper net, not a page/boat crossfade

2026-10-03. Focused implementation and interactive checks in a separate Codex in-app browser tab, with fictional text only. The prototype skill retained revision 08 and the existing A/B/C context; emil-design-eng guided continuity, purposeful key poses, cancellation and reduced-motion behavior. This is an experimental realistic UI prototype, not a production or visual-acceptance declaration.

Implemented:

- One persistent SVG mesh, consisting of ten connected paper triangles, starts by covering the actual Review viewport and remains mounted through folding, settling and drifting. Shared vertices preserve connected edges; the final geometry/material definitions also render static archive and arrival boats. The old white matte, compressed half-page layer, independent boat reveal and paper/boat crossfade were removed.
- Each printed front clips an inert local copy of the same current page. Its original wrapping, form values and scroll position are preserved. An SVG user-space affine matrix maps that print to its own moving triangle. Upper prints leave sight as their face turns away; lower prints are covered by the closed upper sheet. There is no preliminary text/controls fade-to-blank. Back-face paper colour develops from the original surface into matte warm paper, with cabin shading delayed until the later folds.
- Source-to-boat choreography now uses lifting / folding / forming / opening / ready / drifting / complete. Gathering happens concurrently with folding. The authored triangle/boat poses are connected artwork, NOT a rigid-body, equal-area or physically exact traditional origami simulation. Only the final whole boat fades when it leaves view.

Observed issues and corrections:

- Initial snapshots showed displaced printed content. Motion interpreted the SVG transform through CSS positioning; an explicit subscribed SVG matrix corrected the mapping. Retest frames show the original text on the lifting face and the reverse face covering it. No blank replacement sheet was added.
- An early turning pose crossed the sides and produced a pinwheel/star-like intermediate. It was replaced by corner-contraction and triangular-fold poses with consistent left/right order. Material shading was delayed so the large folded rectangle is not painted as unrelated coloured wedges.
- Reduced mode initially exposed a cached 1x1-origin position, rendering a clipped boat at the upper-left. Pre-paint measurement plus one initialization rerender corrected the derived coordinates. The first subsequently captured reduced state has a full boat within the scene: about x=145..244, y=487..543 in a 388x848 scene, with zero page clones. Resizing that state to the short layout keeps it intact and clear of header/footer. This is a rendered-state check, not high-speed first-paint instrumentation.

Verified through UI and rendered DOM:

- A simulated failure kept the original and entered cue on Review, retained the initial three-record pool, and mounted no Seal scene. Retrying added one record. Skipping during a forming pose returned home with exactly one additional record; no second submission occurred.
- Initial and settled normal-scene gradient IDs are identical, with one mesh, ten persistent face identities, the same memory ID, no separate .memory-boat and no .page-fold-matte. Printed faces have their actual finite SVG matrices; all printed sides are hidden once the boat forms. Snapshot copies contain no duplicate field IDs/names and are under an aria-hidden inert visual layer.
- A 25-line fictional long record was scrolled to its policy controls. All ten copies exactly match the source measurements: scrollHeight 2926, scrollTop 2243.0703125, text width 309 and text height 1995 CSS px. They do not jump to the top or rewrap. Quiet completion retains a stationary opaque boat with zero flow Canvas elements.
- Past uses the same successful memory ID and ten-faced static boat, with no printed-page copies. Opening that entry displays the original fictional multiline text unchanged. These archive/data checks preceded the final layout-initialization-only correction; archive/data code was not modified afterwards.
- Manual reduced mode completes directly. Enabling it during lifting ends the same record; disabling it again does not replay or increment the count. The static-location correction was subsequently retested from a fresh reduced submission and at the short layout. No system-preference emulation is claimed.
- Actual short viewport is 478x642 CSS px with an approximately 388x551 inner scene; the requested viewport was not applied literally. Quiet night completion leaves about 51 CSS px of footer clearance and positive header clearance. Resizing a reduced night scene also keeps about 51 CSS px footer clearance. No document horizontal overflow was measured. Temporary viewport override was reset. These are actual measured bounds, NOT an exact 320/375/390 device matrix.
- Final ordinary choreography was replayed after the initialization correction. It retains the same mesh IDs to completion, has opacity 0 on the departing boat, ends on rgb(60,96,116), hides the photographic environment and explicitly retains the record in Past. Night completion uses rgb(39,71,88). No home ocean entry, task-page modal, sinking or algorithm timing indicator was added.
- scripts/verify-paper.mjs checks 40,040 finite face/matrix samples across four source sizes, shared vertex identity, exact page/boat endpoints, key-pose continuity and colour endpoints. These are pure geometry assertions, not browser, device or visual-acceptance tests. Production build succeeds. Final sampled browser console contains no captured warning/error entries; existing non-fatal use-client bundler warnings remain.

Evidence: screenshots/continuous-facets/ contains actual browser frames named facets-before, facets-start, facets-lift, facets-tuck, facets-close, facets-form, facets-open, facets-ready, facets-drift, facets-complete, facets-failure, facets-long-start, facets-quiet, facets-reduced, facets-reduced-short, facets-night-lift and facets-night-short (JPEGs). Main normal frames were recaptured after the final layout correction; long-text/failure frames precede that layout-only change. Short evidence uses full-page browser capture because studio interaction may scroll the phone outside the viewport. Existing evidence folders were not overwritten.

Limitations: approximately 5.5-second trial choreography, awaiting the user's dynamic visual judgment. Ten DOM print copies, affine mapping and SVG face rendering can repaint; no GPU-only or production-performance acceptance is claimed. No new assets/dependencies, full-app regression, rigid folding physics, hidden-tab profiling, real soft keyboard, text scaling, WebView, Android/device, GPU/battery, concurrency, persistence, AI or V3 algorithm verification. Data remains in temporary RAM. The QA tab is closed after inspection; the user's original preview is retained without navigating or clearing its inputs.

Revision 08 source and notes are recoverable in .history/2026-10-03-before-continuous-facets/. The abstract prototype, natural assets/fonts, Android App, backend, algorithm and IMPROVEMENTS.md remain unchanged by this turn. The previous timing/white-matte sequence remains only in that recoverable historical snapshot.

## Study 10 — writing hierarchy and a persistent completion current

2026-10-03. The user accepted Study 09's folding motion and requested clearer writing/confirmation pages plus restoration of continuous flow after departure. Prototype and emil-design-eng guided full-page task hierarchy and progressive disclosure; diagnosing-bugs guided the flow's red/green check. Only the realistic browser prototype was changed. New page design still awaits user visual feedback.

Implemented:

- Two explicit steps, Write and Keep. Write gives the main editor its own quiet paper surface; source sits below it. Next opens confirmation without submitting. Temporary save returns home and can be resumed; blank temporary saves are disabled. Empty Next reports the error beside the editor and focuses it. Page-entry completion now preserves a field/control already being used instead of stealing its focus.
- Confirmation orders original excerpt, appearance choice, optional cue and collapsed extra context. Original defaults to two visual lines, can expand fully, and never gains a tiny nested reading scroller. Three native radios share a compact row; only the selected explanation is rendered. Fixed date reveals the date control and missing-date validation focuses it.
- Local cue suggestion remains visible by default, with explicit adopt or write actions. Adopted, removed and editing states are distinct; suggestions are not automatically stored. Context fields and memory-level preference are grouped inside a native details disclosure. Quiet mode hides the inapplicable preference entry AND its summary badge without erasing the user's choice when switching back. Footer retains one submit action, adjacent error and pending feedback. Main pages remain full-size, not dialog cards. Context sheets remain local, cancellable, focus-contained selections without a black scrim.
- Current activity is independent of vessel completion: opening, ready, drifting and complete remain active. Quiet still mounts no field; reduced mode remains static. Field visibility pause and unmount cleanup are unchanged. No geometry, current vector field or fold timing was changed.

Flow diagnosis and regression:

| Before | After | Why |
| --- | --- | --- |
| Source and instructions compete with writing | One paper editor; source is a secondary row | First step has one primary task |
| All policy explanations, cue input, suggestion and context are open | Selected policy explanation; separate cue states; collapsed context | Optional work no longer looks mandatory |
| Divider on each option and several adjacent input boundaries | Semantic spacing, quiet fills, one disclosure boundary | Hierarchy without fragmented decoration |
| Boat complete disables CurrentField | Complete remains an active environment phase | Vessel departure must not stop the ocean |

- Before the fix, an ordinary completion in the actual UI produced two byte-identical screenshots (82,672 bytes each): FAIL, completed ocean is static. The boat had gone and the completion text was stable. Ranked checks considered the phase gate, field replacement and document visibility. The phase gate excluded complete; the existing Canvas remained mounted.
- The original phase predicate was extracted into journey-state.js and wired at the same CurrentField call site. Before changing its complete case, `node scripts/verify-journey-current.mjs` failed with `AssertionError: complete: surrounding current must keep flowing`, `false !== true`. Adding complete made the test pass. The script is a phase-contract regression, not a Canvas renderer test.
- The original UI scenario was rerun. Normal completed screenshots differ; final post-build resampling again differs at phase complete, with one Canvas, document.hidden false and boat opacity zero. Reduced completed screenshots are byte-identical, with zero frozen-page copies. This demonstrates the intended moving/static distinction; it is not a frame-rate or hidden-tab performance benchmark.

Interaction checks through the independent QA tab (fictional data only):

- Empty Next remains on Write; final fast-entry retest focuses memory-input and exposes the alert. Temporary save/resume and return from confirmation retain multiline text. A source change cancelled after selecting backfill keeps the original source and returns focus to its trigger; applying backfill later shows its separate-time explanation. Changing daylight retains the entered text.
- Default cue suggestion and collapsed extra context are present. Suggestion adoption, removal, custom cue entry and closing the editor work. Time, place and person values are applied and the summary count reaches three. Preference application works; cancelling another selection with Escape preserves the prior value. Tab from the sheet's last action wraps to its close control. Final quiet-mode retest hides both preference entry and badge; switching back restores the prior preference. Native radio ArrowRight chooses the fixed-date option.
- Missing date keeps the initial three-record pool, mounts no Seal and focuses fixed-date. Native keyboard entry commits a date which survives switching policies. The browser automation's direct date fill alone did not dispatch React's change in this environment; an ArrowUp event committed 2030-07-01. This is recorded as an automation limitation, not claimed as a product date-input bug.
- Simulated submission failure keeps the original, custom cue, three context fields and preference, shows a footer error, mounts no Seal and adds no record. Retry adds exactly one record. Past then shows its fixed-date policy and occurrence time; opening it reproduces the multiline original unchanged. Storage remains RAM only.
- An attempted skip during the initial non-interactive withdrawing frame did not activate the invisible footer; it was not counted as a pass. A forming-stage retest successfully returns home, removes all page copies and adds only the one successfully submitted record. Timers/controls still use the pre-existing cleanup path.
- An 18-paragraph expanded original uses one Review page scroller. A short-night sealing capture retains scrollHeight 2746, scrollTop 2198.720703125 and details open on all ten copies, with zero copied field IDs/names. Printed text width differs only by subpixel transforms (about 201.04 versus 201.02 CSS px); no rewrapping or scroll reset was observed. This capture precedes the final scrollbar/focus-only cleanup.
- Quiet night completes with no Canvas and an intact stationary boat. Actual visible face bounds, not the oversized SVG viewport, leave approximately 45.5 CSS px above the footer in that sample. Reduced ordinary sealing directly completes with static flow and no page snapshots. The accepted geometry/path files are SHA-256 identical to the revision 09 backup.
- Final narrow/short retest measures viewport 358x640 CSS px and inner scene 294x549. The three options each have clientWidth=scrollWidth=79, no document horizontal overflow, and footer remains within the page. It uses natural vertical scrolling, not forced one-screen packing. Requested viewport dimensions did not apply literally, so this is NOT an exact device matrix. Override was reset. Default Write/Review and final normal fold/current images were recaptured after source cleanup.
- Both verification scripts and production build pass. verify-paper still checks 40,040 finite geometry/matrix samples, shared edges and endpoints. Sampled final browser error/warning logs are empty; pre-existing non-fatal use-client bundler warnings remain.

Evidence: screenshots/writing-hierarchy/ contains actual browser JPEGs named workflow-writing, workflow-review, workflow-options, workflow-failure, workflow-fold, workflow-current-before, workflow-current-a, workflow-current-b, workflow-read, workflow-writing-short, workflow-review-short, workflow-long-fold, workflow-quiet-short and workflow-reduced. Default pages, ordinary folding and completion frames are final-source captures; optional/failure/read/long/quiet/reduced examples precede the small focus, quiet-summary and scrollbar cleanup. Prior evidence is retained. Documentation/build changes can reload the development preview and reset RAM; the final normal current scenario was deliberately rerun after the build.

Limitations: no full-app regression suite, actual AI cue generation, reminders, real storage, V3 logic, backend, Android/WebView, physical-device, real soft keyboard, large text, exact multi-device matrix, system-reduced emulation, frame-rate/battery or hidden-tab profiling. The new internal page design is a preview for user judgment, not declared visually accepted. The existing 09 motion's geometry, material definitions and vector flow remain unchanged. No new dependencies/assets, commits, upload or backlog edits.

Revision 09 is recoverable in .history/2026-10-03-before-writing-hierarchy/. The temporary QA tab is closed after inspection and the original user preview remains available without agent navigation or form filling.
# Study 13 — four light fields, calm water and a visible homepage tide

2026-10-04; Run UI-20261004-8-03; #8 R2. The user requested checking **all four** periods, smaller waves and water-level changes in the homepage itself. Implement-ideas governed the scoped run and pending acceptance; imagegen supplied one original calm-water bitmap; browser checks used computer-use. All changes remain in the realistic Web prototype. Study 12 already had a persistent homepage tide; this run refines its visible coverage and wet edge rather than inventing it only for splash.

## Implemented and protected

- The exact v4 shore is unchanged. Four full code-native light fields vary warm/cool balance, brightness, broad directional light, land/water lift and subdued water sheen without redrawing terrain. New calm-estuary water replaces the old sea-noon source and most original water in the shore image. Fine detail contrast and refraction are lower, not large ocean swells.
- Raw source color classifies channels before relighting. `environment-material.js` supplies shared tide parameters and CPU helpers; GLSL uses the same constants and formulas. Approximate source-space height, a narrow damp edge and restrained channel-depth shading make low/mid/high materially different. This is illustrative, not a DEM, hydrodynamic simulation or real tide table.
- Single persistent surface, two image sources/textures, existing cover anchors, artistic eight-minute target, time parsing, dark priority and intro target logic are preserved. Functional page colors, writing steps, page folding, boat geometry and completion current are not redesigned.
- SHA256 comparisons against `.history/UI-20261004-8-03/src/` are identical for all 12 protected files: journey.css, PageFold.jsx, PaperBoat.jsx, paper-geometry.js, MemoryMotion.jsx, current-flow.js, writing-state.js, journey-state.js, useEnvironment.js, Ocean.jsx, environment-geometry.js, styles.css.

## Final-source verification

| Item | Method / observed result | Boundary |
|---|---|---|
| Pure state/material/layout/writing tests | `node --test tests/*.test.mjs`: 45/45 pass; includes independent light/tide, monotone continuous coverage, wet edge, all 19 light values and 15 proportional mappings | Not a GPU renderer test |
| Existing motion checks | verify-paper: 40,040 finite samples, shared edges/endpoints; verify-journey-current: complete remains active | Not native folding physics or frame-rate proof |
| Production build | `npm run build` passes, final JS 469.76 kB / gzip 149.78 kB; pre-existing nonfatal use-client warnings | Local build only, not Android/release |
| All four × low/mid/high | 12 actual GL captures after exact rendered light/tide stamps; 1 surface, 2 textures, source dimensions 887×1774 | Current composition A; final light/tide aesthetic remains user's judgment |
| All four × low/high fallback | 8 actual forced CPU captures, 0 GPU textures; visual composition, tone and water extent comparable | Not pixel-exact matching or simulated hardware failure |
| Homepage automatic water | Auto tide stamp changes .2995 → .2689 while four preview lights are switched; state tests assert no light dependence | Not an observed full eight-minute cycle or real tidal timing |
| Intro/end/skip | Morning normal mid .3000; noon skip .3000; afternoon reduced during entry .3000; night static normal high .5200; rapid four-light/target change then skip night mid .3000 | Each sample returns shore/home with one renderer; no physical sun model |
| Five actual containers | CSS bounds approximately 320×568, 360×640, 390×844, 430×932, 768×1024; proportional static mapping changes accordingly; 1 instance | DOM/layout checks, not five physical devices; some samples are outside studio viewport |
| Draft and seal | Fictional text survives morning→night plus temporary save/resume; four-step ordinary submit changes count 3→4; complete has one current Canvas; two same-state completion screenshots differ | Only this scoped regression; failure/quiet/long text were not all rerun |
| Completion light change | Complete remains complete with one current after switching to noon; realistic surface remains inactive behind task/ritual | Does not sign off #7's page design |
| Runtime resource budget | 2 uploads 444×888 ≈3.01 MiB RGBA; sources ≈12.01 MiB decoded lower bound, CPU extra buffers excluded | Dimension-derived estimates, not measured heap/GPU/Android memory |

## Evidence and visual observations

`verification/environment-study-13/` retains 20 full-page JPEGs, `matrix.json`, `regression.json`, the two same-state completion samples and a browser-viewable `index.html`. Four columns show morning/noon/afternoon/night, rows keep one tide constant. Low tide retains dendritic branches, mid wets lower sand, high covers fine branches while keeping the trunk underwater. Morning is softly warm, noon clearer/cooler, afternoon warmer/golden, night blue and readable. No oversized wave ridge dominates the bottom in the checked samples. These are visual observations, not user acceptance or quantified physical accuracy.

Browser inspection and screenshots used different layouts: the saved full-page sources are 1082×3655 while DOM reports a different inspection viewport. The comparison page uses CSS clipping of the screenshot's observed App bounds (x629/y120/390×844); these constants apply **only to saved evidence**, not app layout or production device placement. An initial DOM-coordinate crop was wrong and is not accepted evidence. Do not infer actual multi-size coverage from the contact sheet; use the recorded CSS bounds and mappings. Temporary browser viewport override was reset.

## Failures, recovery and limits

- Multi-file HMR temporarily compiled a vec2/vec3 mismatch before the corresponding declaration was updated; the historical warning is retained. Final captures report WebGL with two textures and the expected light vector. No final-source shader failure was observed.
- During QA the development server disconnected/restarted and reset preview RAM. Matrix capture was restarted and all final 20 combinations were recaptured. A later build reload also reset some controls; intro/draft/completion regressions were run after that reset.
- Initial tab selection failed; a new independent QA tab was bound through the selected browser. One initial static wait deadline expired and was rechecked before continuing. Reduced mode deliberately disables replay, so a replay click was not counted as pass; instead the intro was started normally and reduced mode enabled during it. A completion wait deadline expired before the sampled state reached complete; the follow-up observed complete, then collected the flow evidence.
- Plain sandbox build/cache writes and evidence copies were denied (EPERM). Narrow approved elevated calls wrote the scoped local build and evidence only; no ACL changes or security workaround.
- No actual frame-time/long-task profiling of the heavier CPU relighting, heap/GPU measurements, real context-loss/asset-failure injection, hidden-tab runtime, timezone jump, full large-font/keyboard/safe-area regression, Android/WebView/device, native install, release, Git or knowledge upload. Earlier tests below retain their historical scope, not a claim that all were rerun.

Status: candidate delivered, #8 **pending-validation**, Run local-validation. Four-period brightness, calmness, flood range and final composition require user visual approval. Backup: `.history/UI-20261004-8-03/`. Report: `../../docs/implementation-reports/UI-20261004-8-03.md`.

---
