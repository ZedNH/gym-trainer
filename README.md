# Gym Trainer — Home & Gym workouts (Android)

A native, offline Android app that combines guided **home workouts** with a **gym planner
and set logger**, personalised from a short onboarding. No account, no server, no API keys.

## New in v6.5

- **বাংলা ভয়েস কোচ / Bangla voice coach**: the trainer talks in Bangla during guided workouts and in the gym
  logger: get ready, start, reps/seconds, 3-2-1, halfway, rest, next exercise, set done, rest over.
- **Tips while you train**: form cues for the exercise you're doing (squat, push-up, plank, lunge, jumps…),
  motivation, breathing and water reminders during rest, in Bangla or English.
- Switch language in the player (EN / বাং chip) or Profile → Coach language. Uses the phone's Bangla
  text-to-speech voice; if it isn't installed the app offers to install it and uses English meanwhile.
- **YouTube player rebuilt**: YouTube's own embed page in a WebView, set up as YouTube requires for apps.

## New in v6.0 — a coach that drives progress

- **Auto-progression (double progression)** — every exercise gets *Today's target*: hit the top of
  the rep range on all sets and the next session adds weight (2.5 kg upper body, 5 kg legs, 2 kg
  dumbbells); miss twice and it drops 10% to build back up. Weight and reps are pre-filled.
- **Automatic deloads** — every 5th week is lighter (~10% less weight, ~40% fewer sets), and a
  deload starts by itself when your last sessions felt too heavy. Skip or start one from *My gym plan*.
- **Weekly sets per muscle** — hard sets for 10 muscles against a target band for your level, with
  "needs more" hints that link to workouts for that muscle.
- **Warm-up sets** — a ramp (bar × 10, 50% × 5, 70% × 3, 85% × 1) for heavy lifts, with plates per side.
- **1,351 exercises** (v6.2: your requested A–Z list, 506 names) — 240 new equipment variants (Band, Barbell, Cable, Dumbbell, Kettlebell,
  Machine, Smith Machine), each using the photos of its base movement.
- **In-app updates** — the app checks this repository's GitHub Releases and offers new versions
  (Profile → App updates). Your data is kept when you update.

## Get the app / updates

Download the latest `GymTrainer-*.apk` from **[Releases](https://github.com/ZedNH/gym-trainer/releases/latest)**
and install it. Later versions are offered inside the app. Every release is signed with the same key,
so updates install over the top without losing data.

## New in v5.0 — organised Gym mode

The Gym tab now follows the same layout as Home:
- **Your gym plan** hero: plan name, day dots, *Up next* session and a **Start session** button;
  tap it for the new **My gym plan** screen (stats, full schedule, coach notes, change/rebuild).
- **Programs** carousel filtered by level.
- **Browse by muscle**: Chest, Back, Shoulders, Arms, Legs, Glutes, Core, Full Body and
  Push · Pull · Legs — **27 single gym workouts** (Beginner / Intermediate / Advanced for each muscle),
  each with time, exercises and sets at a glance.
- **My routines** as cards with a Create tile, and **Gym tools** (1RM, plates, BMI) as tiles.

## New in v4.0

- **Floating dock** — Today · Workouts · [+] · Nutrition · Progress, with a quick-action sheet
  (7-min workout, log a meal, add water, body stats, exercise library, new routine).
- **In-app video tutorials** — official YouTube player; 78 verified tutorials for 40 core
  exercises; paste any YouTube link; optional in-app search with your own YouTube API key.
- **Nutrition** — calorie & macro targets (Mifflin-St Jeor, goal-adjusted), 75-food local
  database (rice, roti, dal, fish, eggs, …) with household servings, custom foods, water tracker.
- **Body measurements** (weight, waist, chest, hips, arms, thighs) with trend charts.
- **Exercise history** — best set and estimated-1RM chart per exercise.
- **17 achievements** with progress.
- **More content** — 52 home workouts in 13 categories (adds Bands & Kettlebell, Low Impact,
  desk/bedtime/posture stretches, Tabata, Booty Burn, Core Stability), 7 thirty-day challenges
  (adds Squat & Flexibility), 12 gym programs (adds Lean & Toned, Glute Builder, Dumbbell PPL).
- Database v4 with a tested 3→4 migration (keeps your history).

## What's inside (v3.0)

**Design** — clean light theme (optional dark mode), Poppins typeface, one accent colour,
level colours (green / amber / red), consistent cards, empty states and bottom actions.
Tabs: **Today · Workouts · Library · Progress · Profile**.

**Onboarding** — one question per screen: name, goal, where you train (home / gym / both),
level, focus areas, body (number pickers), schedule, equipment, injuries → plan is built.

**Personal plans** — a generated 4-week home plan matched to your goal, level, days per week,
session length, focus areas, equipment and injuries, getting harder every week; plus a gym
plan from the rules engine.

**Home workouts** — 37 workouts in 11 categories (Full Body, Abs, Chest, Arms, Legs, Back &
Shoulders, HIIT, Cardio, Mobility, Dumbbell, Quick), each at Beginner / Intermediate / Advanced;
12 multi-week plans (Lose Weight, Build Muscle, Tone & Shape, Flexibility × 3 levels);
5 thirty-day challenges (Full Body, Abs, Butt & Legs, Push-Up, Plank). Guided player with
countdown, timers, rest screen, voice coach.

**Gym** — 9 multi-week programs by level (Beginner Full Body, Machine Starter, Dumbbell Only,
Upper/Lower, Push Pull Legs, Strength 5×5, Body-Part Split, PPL ×2, Powerbuilding), 9 single
workouts, custom routines, routine preview, set logger with rest timer, effort check, smart
next-weight, records; tools (1RM, plate loader, BMI).

**Library** — 887 exercises with photos, instructions and mistakes; search + filters.
**Progress** — totals, weight trend, BMI, weekly volume, records, history.

## Build & test

Requires JDK 17 and the Android SDK (platform 34, build-tools 34.0.0).

```
gradle assembleRelease      # signed if keystore.properties exists
gradle testDebugUnitTest    # 37 Robolectric tests incl. end-to-end UI flow
```

`ScreenshotTest` renders every main screen to `app/build/screens/*.png` for design review.

Database upgrades from v2 keep all history (migrations 2→3→4→5).

`tools/gen_variants.py` regenerates `assets/exercises_variants.json` (equipment variants).

## Credits
Exercise photos/instructions: [free-exercise-db](https://github.com/yuhonas/free-exercise-db)
(public domain). Typeface: Poppins (SIL OFL). See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
All workouts, plans and code are original; nothing is taken from commercial fitness apps.
