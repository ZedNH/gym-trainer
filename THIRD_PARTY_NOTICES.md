# Third-party notices

## free-exercise-db

Exercise names, instructions and photos in `app/src/main/assets/` (`exercises.json`
and `ex/<id>/<n>.webp`) are derived from **free-exercise-db** by yuhonas:
https://github.com/yuhonas/free-exercise-db

It is released into the public domain under **The Unlicense**:

> This is free and unencumbered software released into the public domain.
> Anyone is free to copy, modify, publish, use, compile, sell, or distribute this
> software, either in source code form or as a compiled binary, for any purpose,
> commercial or non-commercial, and by any means.

The photos were resized to 440 px and re-encoded as WebP; each exercise was mapped to
this app's muscle-group, equipment, movement-pattern and rep/rest schema by a build
script. Attribution is not required by the license but is given here as a courtesy.

## Poppins typeface

`app/src/main/res/font/poppins_*.ttf` — Copyright 2020 The Poppins Project Authors
(https://github.com/itfoundry/Poppins), licensed under the SIL Open Font License 1.1.
The full license text is in [POPPINS_OFL.txt](POPPINS_OFL.txt).

## YouTube tutorials

In-app video tutorials embed YouTube's official embed player (`youtube.com/embed`) in an Android
WebView, identified to YouTube with the app's id as required by YouTube's API terms. Videos are
streamed from YouTube, never downloaded or re-hosted, and belong to their creators.

`app/src/main/assets/videos.json` lists tutorial video IDs that were checked via YouTube's
public oEmbed endpoint (exists + embeddable) when the list was built.

## Everything else

The home workouts, 30-day challenges, gym templates, plan generator, the eleven custom
exercises prefixed `x_`, and all app code are original to this project. No code,
artwork, icons or text were taken from any commercial fitness app.
