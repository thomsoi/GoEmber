# Ember Passport — Project Context

## 1. Project Overview

**Ember Passport** is a mobile-first gamified travel companion built on Ember's real transport network.

### Core idea

> **Ember tells you how to get somewhere. Ember Passport gives you a reason to go.**

Ember Passport turns ordinary bus journeys into a collection game. Users build a digital passport of Scotland by riding Ember buses, discovering stops, completing routes, exploring towns, and eventually discovering nearby landmarks.

The goal is to make public transport feel less like a purely functional means of getting from A → B and more like an opportunity for discovery and progression.

### Core product loop

```text
TRAVEL
  ↓
DISCOVER
  ↓
COLLECT
  ↓
PROGRESS
  ↓
EXPLORE
  ↓
TRAVEL AGAIN
```

Every journey should contribute to the user's passport.

Every new place should feel like an achievement.

---

# 2. Hackathon Constraint

This is a **7-hour hackathon project**.

The most important engineering/product principle is:

> **Build a small, polished, working experience rather than a large collection of unfinished features.**

Do NOT over-engineer the application.

Do NOT build unnecessary infrastructure.

Do NOT implement every feature described in this document unless the core experience is already complete.

The application should have a compelling demo even if only the Tier 1 functionality exists.

---

# 3. Target Users

### Primary users

- Students
- Young adults
- Regular public transport users
- People who enjoy games, collecting, achievements, streaks, and exploration
- People who want inexpensive ways to explore Scotland

### Secondary users

- Curious travellers
- Tourists
- People who want to discover destinations they would not normally visit

---

# 4. Technology Stack

## Frontend

- React
- TypeScript
- Vite
- Mobile-first responsive UI
- Tailwind CSS or another lightweight styling solution
- Map library only if required for a feature

The frontend should be designed primarily for a phone-sized screen.

A mobile web application is sufficient for the hackathon. Do NOT spend significant time creating a native iOS/Android application.

## Backend

- Java
- Spring Boot
- REST API
- Spring WebClient for external Ember API requests

The backend is responsible for:

- Communicating with Ember
- Normalising Ember API responses
- Journey/session logic
- Passport logic
- Stamp awarding
- Progress calculations
- Challenge calculations
- User statistics

## Database

Use a simple relational database such as PostgreSQL if persistent data is required.

For the hackathon, keep the data model simple.

Do not introduce unnecessary microservices.

## External services

Primary:

- Ember production APIs

Potentially:

- Geolocation/browser location APIs
- Landmark/places API
- Optional LLM API

An LLM must never be treated as the source of truth for transport information.

Real transport information must come from Ember.

---

# 5. Product Principles

## Principle 1 — Discovery over navigation

This is NOT primarily a journey planner.

The question we are trying to answer is:

> "Where could I go and what can I discover?"

rather than only:

> "How do I get from A to B?"

## Principle 2 — Real transport data

Ember's API is the source of truth for:

- journeys
- stops
- routes
- transport information
- journey timing
- other available transport metadata

Do not invent transport information.

## Principle 3 — Collection should feel rewarding

A newly discovered stop should feel like an achievement.

Use:

- animations
- visual stamps
- progress
- rarity
- badges
- streaks

where practical.

## Principle 4 — Mobile first

The primary UI should work beautifully on a phone.

Avoid desktop-first layouts.

## Principle 5 — Scope aggressively

If a feature threatens the core demo, remove it.

The core experience is more important than feature count.

---

# 6. TIER 1 — MUST HAVE

These are the essential features.

If the hackathon ended after approximately 4–5 hours, these are the features that should already work.

---

## 6.1 Digital Passport

The user has a digital Ember Passport.

The passport should show at minimum:

- Number of stops discovered
- Number of routes ridden
- Number of journeys completed
- Total distance travelled
- Exploration streak
- Number of stamps collected

Example:

```text
┌─────────────────────────────────┐
│                                 │
│          EMBER PASSPORT         │
│                                 │
│              🏴                 │
│                                 │
│         EXPLORER #1842          │
│                                 │
│       ───────────────           │
│                                 │
│       24 STAMPS                 │
│       8 ROUTES                  │
│       6 TOWNS                   │
│       312 KM                    │
│                                 │
│       🔥 5 DAY STREAK           │
│                                 │
└─────────────────────────────────┘
```

This is the primary home screen.

---

# 6.2 Stop Stamps

Every Ember stop should be treated as a potential collectible.

When a user visits a stop for the first time, the stop becomes unlocked in their passport.

A stamp should contain:

- Stop name
- Date first discovered
- Number of visits by the user
- Whether it is newly discovered
- Stamp tier
- Number/percentage of Ember Passport users who have discovered it, if this data is available

Example:

```text
✨ NEW STAMP! ✨

📍 INVERKEITHING

First discovered:
3 October 2026

Visits:
1

★ NEW ★

12% of Ember Passport users
have discovered this stop.
```

### Stamp tiers

Use a simple progression:

```text
1 visit    ★
3 visits   ★★
5 visits   ★★★
10 visits  ★★★★
25 visits  👑
```

Do not make the exact numbers overly complicated.

The important mechanic is:

> Repeated visits increase the stamp's tier.

---

# 6.3 Start Journey

The user must be able to start a journey.

Basic flow:

```text
Select/find journey
        ↓
START JOURNEY
        ↓
Journey becomes active
        ↓
User sees journey progress
        ↓
Stops are discovered
        ↓
Journey completes
        ↓
Passport updates
```

The active journey screen should display:

- Origin
- Destination
- Route
- Current progress
- Stops visited
- Remaining journey time if available
- Relevant transport information

Example:

```text
🚌 JOURNEY ACTIVE

Dundee → Edinburgh

Dundee
  ✓

Dunfermline
  ✓

Inverkeithing
  ✨ NEW

Edinburgh
  ○

━━━━━━━━━━●━━━━━━

4 / 8 stops discovered
```

---

# 6.4 Stop Discovery

While a journey is active, the application should determine which Ember stops the user has reached/passed.

The ideal implementation uses browser/device geolocation and matches the user's location against Ember stop data.

Conceptually:

```text
User GPS location
       ↓
Find nearby Ember stop
       ↓
Confirm stop belongs to active journey
       ↓
Award stamp
       ↓
Update passport
```

GPS accuracy must be considered.

Use a reasonable geofence rather than requiring the user's coordinates to exactly equal a stop's coordinates.

### EXAMPLE ARCHITECTURE
ARCHITECTURE:
                 📱 USER'S PHONE
                       │
                       │ HTTPS
                       ▼
              ┌─────────────────┐
              │   React + Vite  │
              │                 │
              │ Mobile UI       │
              │ Passport        │
              │ Journey screen  │
              │ Stamps           │
              └────────┬────────┘
                       │
                       │ HTTP / JSON
                       ▼
              ┌─────────────────┐
              │   Spring Boot   │
              │                 │
              │ Your backend    │
              │                 │
              │ Business logic  │
              │ User data       │
              │ Stamp logic     │
              └───────┬─────────┘
                      │
             ┌────────┴─────────┐
             │                  │
             ▼                  ▼
      ┌─────────────┐    ┌─────────────┐
      │  Ember API  │    │  PostgreSQL │
      │             │    │             │
      │ Journeys    │    │ Users       │
      │ Stops       │    │ Stamps      │
      │ Routes      │    │ Progress    │
      └─────────────┘    └─────────────┘

### Hackathon fallback

If reliable real-time GPS tracking is too time-consuming, create a deterministic demo mode based on the active Ember journey.

The architecture should still make it possible to replace simulated