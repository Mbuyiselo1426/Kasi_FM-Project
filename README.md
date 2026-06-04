
# KasiStream - Learning Context

## About This Project

This codebase was created as a **learning exercise** using AI assistance (GitHub Copilot / AI prompts). I did not write every line from memory—instead, I:

1. Used AI prompts to generate boilerplate code
2. Reviewed and understood each component
3. Modified and adapted the code to fit my learning goals
4. Used this as hands-on practice for Android development with Java

## What I Learned

- How ExoPlayer works for audio streaming
- Android Service lifecycle for background playback
- Managing notifications for foreground services
- UI event handling in Android

## Why This Approach

As a student developer, AI tools help me:
- Learn by seeing working examples
- Understand project structure faster
- Focus on learning concepts rather than syntax errors

## My Actual Contributions

- Understanding the code I'm committing
- Adapting it to the Kasie FM use case
- Setting up the Android project structure
- Planning future features (data-saving mode, push notifications)

## Future Plans

- Replace placeholder stream URL with actual Kasie FM stream
- Add "Data Lite" mode for low-data usage
- Implement song request feature via WhatsApp
- Add push notifications for show reminders



-Added the UI design 

┌─────────────────────────────────┐
│         KASIE FM                │  ← Top Toolbar
├─────────────────────────────────┤
│                                 │
│      [ LISTEN NOW ]             │  ← Big play button
│                                 │
│  ┌─────────────────────────┐    │
│  │ NOW PLAYING             │    │
│  │ Kasie FM Live           │    │
│  │ Kasie FM                │    │
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ NOW ON AIR              │    │
│  │ #HomeDrive              │    │
│  │ 15:00 - 18:00           │    │
│  └─────────────────────────┘    │
│                                 │
├─────────────────────────────────┤
│ [KASIE FM] [FEATURED] [HIGHLIGHTS] [NEWS] │ ← Bottom Nav

└─────────────────────────────────┘

# Delete corrupted cache
rm -rf ~/.gradle/caches/9.4.1
rm -rf ~/.gradle/caches/transforms-*

# Then rebuild
./gradlew clean build --refresh-dependencies

# Check what files you changed
git status

# Add all Server Core files
git add za/co/wethinkcode/robots/server/*.java

# Commit with message
git commit -m "feat(server-core): implement complete world foundation for Iteration 1

- Add Position, Direction, Obstacle, Robot, World classes
- Implement dump() and robotsCommand()
- Add thread safety and collision detection"

# Push to remote branch
git push origin your-branch-name