# Verdant Studio — Dream Garden Designer

Verdant Studio is an interactive Android garden design studio that turns a user's space dimensions, climate, sunlight, soil, and style preferences into structured, editable 2D garden layouts paired with AI-generated botanical visuals and care guidance.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Because the Phase 1 clarifying questions were skipped, this plan adopts the recommended defaults below. Confirm or request adjustments before we begin building.

- **Visual Direction (Default)**: Warm Botanical Studio aesthetic featuring deep forest and sage greens, terracotta accents, warm alabaster surfaces, and editorial serif + geometric sans typography (`Fraunces` + `Plus Jakarta Sans`).
- **Layout & Visual Generation (Default)**: Combines an interactive, editable 2D garden layout canvas (planted zones, pathways, seating, water features) with runtime AI-generated plant illustrations and full garden concept scenes via Firebase AI Logic (`gemini-3.8-flash` for structured JSON layouts and `gemini-nano-banana-2.1` for botanical visuals).
- **Persistence & Sync (Default)**: Durable cloud storage in Firebase Firestore gated behind Google Sign-In (Jetpack Credential Manager) so saved gardens, plant palettes, and layouts persist across devices.
- **Unrelated Attachment Excluded**: The attached `smart_health_tracker_6f324179.html` file is unrelated to the dream garden prompt and is excluded from the app scope.
- **Firebase Provisioning Gate**: Executing this plan will trigger the Firebase setup consent UI for both `firestore` and `ai_logic` (plus App Check). Note that runtime AI image generation (`gemini-nano-banana-2.1`) requires the linked Firebase project to be on the Blaze billing plan.

---

## 1. Overview & Core Concept

- **What It Does**: Users define their dream garden parameters—plot dimensions, shape, sunlight exposure, USDA/climate zone, soil drainage, aesthetic style (e.g., Cottage, Zen Courtyard, Mediterranean, Pollinator Meadow, Modern Patio), and desired features (water feature, pergola, raised beds). Verdant Studio generates a tailored, zoned 2D blueprint with curated companion plants, lets users rearrange or swap plants on an interactive canvas, and generates bespoke visual previews of individual plants and the overall garden scene.
- **Target Audience / Persona**: Homeowners, balcony/patio gardeners, and landscape enthusiasts looking to visualize planting combinations and spatial zoning before buying plants or digging.
- **Key Value**: Bridges the gap between abstract plant lists and real spatial planning by uniting climate-aware botanical curation, interactive 2D layout editing, and on-demand AI visual previews in one studio.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Authentication & Studio Entry**:
   - Launches into a serene botanical welcome gate with silent Google Sign-In attempt (`GetGoogleIdOption`) and a primary "Sign in with Google" CTA (`GetSignInWithGoogleOption`).
   - Once signed in, users land on **My Gardens** (studio home) displaying saved garden designs as visual blueprint cards, or a welcoming zero-state prompt to start their first garden.
2. **Dream Garden Preference Wizard**:
   - A focused, multi-section preference studio:
     - *Space & Conditions*: Plot width/length (ft or m), yard type (Backyard, Courtyard, Balcony, Front Yard), Sunlight (Full Sun, Part Shade, Full Shade), Soil (Loam, Clay, Sandy, Raised Bed Mix), and Hardiness/Climate profile.
     - *Style & Features*: Style selector chips (Warm Cottage, Japanese Zen, Mediterranean, Pollinator Haven, Modern Minimal) + toggles for pathways, seating patio, water feature, or edible beds.
     - *Optional Yard Photo*: Zero-permission Android Photo Picker (`PickVisualMedia`) allowing users to attach a reference photo of their current space for multimodal AI analysis.
   - Tapping **Generate Layout** triggers structured AI generation (`gemini-3.8-flash` with strict JSON `responseSchema`), transitioning smoothly into the interactive Layout Studio.
3. **Interactive 2D Layout Canvas & Plant Editor**:
   - **Blueprint Canvas**: Custom Compose `Canvas` rendering the scaled plot grid, sun-orientation compass, hardscape zones (stone paths, patio deck, water feature), and color-coded circular plant canopies sized by mature spread.
   - **Tap-to-Inspect & Move**: Tapping any placed plant or zone opens a bottom sheet with botanical name, mature height/spread, sun/water needs, bloom season, companion pairing notes, and actions to **Move**, **Swap Plant**, **Remove**, or **Generate Plant Visual**.
   - **Add & Filter Palette**: Users can add new plants from the AI-curated companion palette or ask AI for alternatives tailored to a specific shaded/sunny corner.
4. **AI Visual Studio (Plant Portraits & Garden Scene Renders)**:
   - **Plant Visuals**: On any plant card or inspector sheet, users tap **Visualize Plant** to generate a botanical illustration / photorealistic close-up (`gemini-nano-banana-2.1`) tailored to the selected garden style.
   - **Dream Garden Concept Render**: From the Layout Studio toolbar, users tap **Render Garden View** to synthesize a cohesive concept visual of their entire designed garden incorporating the chosen style, key hero plants, and hardscape features (with optional Photo Picker yard-photo conditioning for image-to-image transformation).
   - **Saved Visual Gallery**: Generated visuals are cached locally in app-private storage (referenced by URI in Firestore metadata) so users can revisit them without redundant API calls.

### Visual Identity & Theme

- **Aesthetic Direction**: *Warm Botanical Studio* — tactile drafting paper meets lush conservatory elegance. Clean editorial framing with generous whitespace, organic rounded corners, and crisp hairline borders evoking architectural landscape blueprints.
- **Color Palette & Mood** (mapped to Material 3 `ColorScheme` tokens, supporting light & dark modes):
  - `background` / `surface`: Warm Alabaster (`#F8F6F0` light) / Deep Moss Charcoal (`#121A15` dark)
  - `surfaceVariant` / `surfaceContainer`: Soft Sage Linen (`#ECEFE8` light) / Pine Slate (`#1C2720` dark)
  - `primary`: Deep Botanical Forest (`#234F36` light) / Bright Sage (`#8FD1A6` dark)
  - `secondary`: Sunlit Terracotta (`#B85D3B` light) / Warm Clay (`#E59A7D` dark)
  - `tertiary`: Meadow Gold (`#C49A3A` light) / Soft Pollen (`#E3C270` dark)
  - `outlineVariant`: Fine Drafting Hairline (`#D4DBD0`)
- **Typography & Hierarchy**:
  - *Display & Section Headers*: `Fraunces` (bundled locally via `font-util`) for organic, editorial character.
  - *Body, Controls & Telemetry*: `Plus Jakarta Sans` (bundled locally via `font-util`) for crisp legibility in dimension labels, care badges, and form inputs.
- **Adaptive Layout & Components**:
  - Edge-to-edge layout (`enableEdgeToEdge()`) with proper `WindowInsets.safeDrawing` and `WindowInsets.navigationBars` handling.
  - `Compact` screens use a Material 3 `NavigationBar` switching between **Gardens**, **Create**, and **Plant Studio**, plus full-screen **Layout Studio** destination via `@Serializable` type-safe Navigation Compose.
  - `Medium` / `Expanded` screens adapt to a `NavigationRail` and side-by-side Canvas + Plant Inspector pane.

### Interactive Feedback & Motion

- **Layout Generation**: Animated botanical drafting skeleton while structured JSON layout synthesizes.
- **Image Generation (30–60s Latency)**: Dedicated shimmer canvas card with elapsed progress feedback ("Composing botanical visual — typically 30–60s") and cooperative cancellation support.
- **Error Boundaries**: Explicit `UiState.Error` cards with retry actions—never silent failures or fake procedural fallback images.
- **Micro-Interactions**: Spring-animated selection rings on canvas plants, ripple feedback on all cards/chips, and minimum 48dp touch targets across every interactive control.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Structured JSON Layout Generation + Interactive 2D Compose Canvas**
  - *Chosen Approach*: Use `gemini-3.8-flash` with `Schema.obj(...)` (`responseMimeType = "application/json"`) to output normalized plot coordinates `(x, y)`, plant radii, hardscape polygons/rects, and care metadata, rendered on an interactive Jetpack Compose `Canvas`.
  - *Why*: Purely text or flat images cannot be edited. Normalized coordinate JSON lets users tap, move, resize, and inspect every single plant and hardscape element smoothly on device.
  - *Alternatives Considered*: Generating static SVG/HTML layouts (harder to edit natively in Compose and error-prone in WebViews).
- **Decision 2: Runtime AI Plant & Garden Visuals via Firebase AI Logic (`gemini-nano-banana-2.1`)**
  - *Chosen Approach*: Use Firebase AI Logic (`Firebase.ai`) exclusively—`gemini-3.8-flash` for layout/plant curation (with optional Photo Picker bitmap input) and `gemini-nano-banana-2.1` for runtime plant and garden scene visuals, secured via Firebase App Check (`configureAppCheck(this, intent)`).
  - *Why*: Zero client-side API keys, native Android SDK integration, and authentic on-demand visual previews matching the user's exact layout and style.
  - *Alternatives Considered*: Bundling fixed stock plant photos (fails to match custom preferences and style combinations).
- **Decision 3: Cloud Persistence in Firestore with Google Sign-In**
  - *Chosen Approach*: Persist garden designs, placed plant items, hardscape zones, and preference profiles in `/users/{userId}/gardens/{gardenId}` in Cloud Firestore (custom DB ID via `R.string.firestore_database_id`), gated behind Credential Manager Google Sign-In. Generated bitmaps are saved as compressed PNGs in app-private storage (`filesDir`) with filenames linked to the garden/plant IDs (avoiding Firestore's 1MB document limit).
  - *Why*: Protects user design effort across app reinstalls and devices while keeping Firestore documents lightweight and fast.
  - *Alternatives Considered*: Room-only local storage (loses data on device switch/uninstall) or storing raw base64 images inside Firestore (hits document size limits quickly).

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         MainActivity (Edge-to-Edge)                       │
│   1. configureAppCheck(this, intent)  ──►  2. Auth Gate (Google Sign-In)  │
└────────────────────────────────────┬──────────────────────────────────────┘
                                     │ Authenticated (uid)
                                     ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                 Type-Safe Navigation Compose (@Serializable)              │
│  ┌──────────────┐   ┌──────────────────┐   ┌───────────────────────────┐  │
│  │  My Gardens  │   │  Design Wizard   │   │  Layout & Visual Studio   │  │
│  │  Dashboard   │   │  (Prefs + Photo) │   │  (2D Canvas + AI Visuals) │  │
│  └──────┬───────┘   └────────┬─────────┘   └─────────────┬─────────────┘  │
└─────────┼────────────────────┼───────────────────────────┼────────────────┘
          │                    │                           │
          ▼                    ▼                           ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                      MVVM ViewModels (StateFlow)                          │
│    GardenListViewModel   │   GardenGeneratorViewModel & StudioViewModel   │
└──────────────┬───────────┴────────────────────────┬───────────────────────┘
               │                                    │
               ▼                                    ▼
┌──────────────────────────────┐     ┌──────────────────────────────────────┐
│    GardenFirestoreRepository │     │       GardenAiRepository             │
│  • /users/{uid}/gardens      │     │  • gemini-3.8-flash (JSON Layout)    │
│  • Realtime .snapshots()     │     │  • gemini-nano-banana-2.1 (Visuals)  │
│  • Local Bitmap File Cache   │     │  • 60s Coroutine Timeouts            │
└──────────────┬───────────────┘     └──────────────────┬───────────────────┘
               │                                        │
               ▼                                        ▼
┌──────────────────────────────┐     ┌──────────────────────────────────────┐
│  Firebase Auth + Firestore   │     │   Firebase AI Logic + App Check      │
│  (Zero-Trust Security Rules) │     │   (Debug Token / Release reCAPTCHA)  │
└──────────────────────────────┘     └──────────────────────────────────────┘
```

### Data Model & State

- **`GardenPreferences`**: `style`, `yardType`, `widthFeet`, `lengthFeet`, `sunlight`, `soilType`, `climateZone`, `includePathway`, `includeSeating`, `includeWaterFeature`, `includeEdibles`, `notes`.
- **`GardenDesign`** (Firestore document under `/users/{uid}/gardens/{gardenId}`):
  - `@DocumentId val id: String = ""`
  - `userId: String`, `title: String`, `summary: String`, `style: String`, `widthFeet: Double`, `lengthFeet: Double`, `sunlight: String`, `soilType: String`, `climateZone: String`
  - `hardscapeZones: List<HardscapeZone>` (id, type, label, normalized `x`, `y`, `width`, `height`, `materialNote`)
  - `plants: List<PlacedPlant>` (id, commonName, botanicalName, category, sunNeed, waterNeed, matureSpreadFeet, matureHeightFeet, bloomSeason, companionNote, normalized `x`, `y`, `colorHex`, `localVisualPath`)
  - `conceptVisualPath: String` (local cached PNG path for rendered garden scene)
  - `@ServerTimestamp createdAt: Timestamp?`, `@ServerTimestamp updatedAt: Timestamp?`
- **UI States**: Sealed interfaces (`UiState<T>`, `GenerationState`, `ImageGenUiState`) with explicit `Idle`, `Loading`, `Success`, and `Error(message)` branches.

### Interactive Component & State Mapping

- **Preference Wizard Form**: Validates plot dimensions (> 0) and selected preferences; optional Photo Picker URI decodes downscaled `Bitmap` (< 20MB) before passing to `GardenGeneratorViewModel.generateGardenLayout(...)`.
- **Interactive 2D Layout Canvas**: Binds `pointerInput` tap and drag gestures to hit-test `PlacedPlant` circles using `DrawScope` dimensions (`size.width`, `size.height`), updating plant `(x, y)` coordinates in ViewModel state and persisting edits to Firestore.
- **Plant Inspector Bottom Sheet**: Displays plant telemetry, allows swapping via AI companion suggestions, deleting, or triggering `generatePlantVisual(plant)` (`gemini-nano-banana-2.1` with `withTimeout(60_000L)`).
- **Garden Concept Renderer**: Synthesizes a cohesive scene prompt from the active `GardenDesign` (style, hardscape zones, and top plant species) — and optional yard reference bitmap — storing the resulting `Bitmap` to app-private files and updating `conceptVisualPath`.

### Key Sequences & Integrations

1. **Firebase Provisioning & App Check**:
   - Set unique `applicationId` (`com.aistudio.verdantgarden.<random>`), app name (`Verdant`), and `metadata.json`.
   - Extract `debug.keystore` SHA-1 & SHA-256, request user consent via `show_aistudio_ui` (`["firestore", "ai_logic"]`), verify ToS, and invoke `ProvisionFirebase`.
   - Implement `configureAppCheck(this, intent)` in `MainActivity.onCreate()` before `setContent` (using synchronous `.commit()` for debug token storage).
2. **Zero-Trust Security Rules & Two-Tier Testing**:
   - Author `firebase-blueprint.json`, `firestore.rules` (enforcing `request.auth != null`, `request.auth.uid == userId`, and strict `isValidGardenDesign()` field/type validation), and single-database `firebase.json`.
   - Start local Firestore/Auth emulators, pass both JavaScript (`firestore.test.js`) and Robolectric Kotlin (`GardenRepositoryRuleTest.kt`) rule tests, then deploy rules via `DeployRules`.
3. **Custom Typography & Adaptive Icon**:
   - Install `Fraunces` and `Plus Jakarta Sans` into `res/font/` via `font-util`.
   - Configure custom botanical adaptive launcher icon and density PNG fallbacks per `app-icon-generation`.
