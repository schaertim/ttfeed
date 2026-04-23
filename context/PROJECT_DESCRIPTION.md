# Swiss Table Tennis Platform — Project Description

## What is this?

A modern web platform and PWA for Swiss table tennis players. Free, fast, and complete — the best way to follow the Swiss TT scene.

---

## The problem

Two meaningful alternatives exist today, but both have clear ceilings:

**TT Mobile** is a polished mobile wrapper around click-tt.ch. Fast, simple, and easy to understand — but it only shows the current season, has no statistics, no clubs, no tournament games, and no historical data. It is a better click-tt, not a platform.

**TT Stats** takes the opposite approach: deep player-centric statistics, beautiful UI, and an impressive feature set. But almost everything beyond the surface is locked behind a subscription. Group standings beyond the top 3 require a paid plan. It also has noticeable performance issues and bugs.

The gap is obvious: a platform with TT Stats' depth, TT Mobile's simplicity and speed, and no paywall.

---

## The vision

> The home for Swiss table tennis. Free, complete, and genuinely better.

A place where every player has a real profile — ELO history, season stats, career arc, head-to-head records. Where you can browse your league, see who's leading the table, and drill into any match and see the exact set scores. Where results land on your phone before you've thought to check. Where the data goes back to 1989 because no one else bothered.

Built for the community, free for everyone.

---

## What makes it different

| | TT Mobile | TT Stats | This |
|---|---|---|---|
| League browser | ✓ | ✗ | ✓ |
| Player profiles | Basic | ✓ (paywalled) | ✓ Free |
| ELO graph | ✓ | ✓ (paywalled) | ✓ Free |
| Set scores | ✗ | ✗ | ✓ |
| Historical data | Current season only | Limited | 1989–present |
| Statistics | ✗ | ✓ (paywalled) | ✓ Free |
| Push notifications | ✗ | ✗ | ✓ (planned) |
| Performance | Fast | Slow | Fast |
| Price | Free | Subscription | Free |

**Individual set scores** are a unique feature — no other platform shows them. Scraped directly from click-tt.ch.

**Historical depth** is our moat. Career arcs going back to 1989/1990. No competitor has this.

---

## Data sources

- **knob.ch** — league structure, match results, player rankings, historical data back to 1989/1990
- **click-tt.ch** — individual set scores per game, official ELO ratings, player portraits
- Players are linked between sources via their STT licence number

---

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | SvelteKit (TypeScript, Svelte 5 Runes) |
| PWA | vite-plugin-pwa + Web Push API |
| Backend | Ktor (Kotlin) |
| Database | PostgreSQL (Supabase) |
| Frontend hosting | Vercel |
| Backend hosting | Railway |

---

## Project type

Solo developer side project. Goals: build something genuinely useful for the Swiss TT community, learn SvelteKit and Ktor, sharpen product and UX instincts. No monetisation planned.
