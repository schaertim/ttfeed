# Development Roadmap

## Guiding principles

- Ship something real at every milestone — deployed, shareable, genuinely usable
- Beautiful UI and fast loading are non-negotiable from day one — not polish you add later
- Free beats paywalled every time — never lock a feature behind login or subscription
- Simple to understand first, deep on demand — don't overwhelm, let users explore
- Historical depth and set scores are the moat — keep them front and centre
- Real feedback from the Swiss TT community shapes what comes next

---

## MVP

The MVP is the point at which a Swiss TT player can open the app, find their league, see who's leading the table, look up a match and see the set scores, and look up any player and see their ELO history and stats. This is the version that gets shared in a WhatsApp group.

Three milestones build toward it.

---

### Milestone 1 — League & Results

**Goal:** A complete, polished league browser. Better than anything free and available today for browsing the Swiss TT season.

**Scope:**
- Season selector — switch between active and past seasons
- League browser — all federations, all divisions, clearly organised
- Division standings — correct points, clean table, nothing hidden
- Match results list per division — round navigation
- Match detail page — team score, all individual games, player names

**Quality bar:**
- Loads fast on mobile (target: standings visible in under 2s)
- Clean, readable layout — TT Mobile level of clarity as a floor
- Works well offline / on slow connections (PWA caching)

**Done when:** A player can find their division, see the full standings table, and check any result. Nothing behind a login. Nothing truncated.

---

### Milestone 2 — Set Scores

**Goal:** Ship the unique feature. No other platform shows individual set scores. This is the hook that makes someone say "wait, where did you find that?"

**Scope:**
- Individual set scores per game on the match detail page (e.g. 11:8, 9:11, 11:7, 11:4)
- Clear presentation — each game shows the player(s), the sets result, and the full set-by-set breakdown
- Graceful fallback when click-tt data is not yet available for a match

**Done when:** You open any completed match and can see exactly how each game was played, set by set.

---

### Milestone 3 — Player Profiles

**Goal:** Make it personal. Every player has a real page. This is what creates word-of-mouth — someone looks up themselves or a rival and the data tells a story.

**Scope:**
- Player search by name
- Player profile page:
  - Current ELO, classification (Klass), club, season
  - ELO progression graph — with classification thresholds annotated (B13, B12, B11...)
  - This season's match history — each game with result and ELO delta
  - Season stats — win rate, wins, losses, games played
- Club history — which club(s) per season

**Quality bar:**
- ELO graph is the centrepiece — make it look as good as TT Stats' version or better
- Profile loads fast — player pages should feel instant
- Works for players with data going back multiple seasons

**Done when:** You search your own name, land on your profile, and it accurately reflects your current season. The ELO graph works. Your match history is there. Someone else from the community sees it and asks for the link.

---

## Beyond MVP

Features added after the MVP is live and being used. Order driven by community feedback.

---

### Follow & Notifications

The feature that turns a casual visitor into a retained user. Solves the core pain: having to manually check for results.

- User accounts (email / magic link — no password)
- Follow a player, team, or division
- Push notifications when a followed match result lands (scheduled → completed)
- PWA install prompt — nudge to add to homescreen for notifications
- Notification preferences — mute, pause, manage follows
- Personal dashboard on login — your ELO badge, rank, classification, recent results

---

### Historical Depth

The moat. No competitor has 35 years of data. Make it count.

- Full career stats going back to 1989/1990 on the player profile
- Season-by-season breakdown — every season a player has played, club, record, ELO
- Head-to-head record vs any specific opponent across all seasons
- Career milestones surfaced inline — 100th match, first win vs higher-ranked, longest streak

---

### Rich Statistics

Deeper analytics for players who want more than a record and a graph.

- Win rate breakdowns: vs higher ELO, vs lower ELO, after winning first set, after losing first set, home vs away
- Set score distribution — donut chart (0:3, 1:3, 2:3, 3:2, 3:1, 3:0)
- Monthly performance bar chart — wins/losses by month across a season
- ELO performance by opponent tier — W/L record per classification band

---

### Discovery & Community

High-engagement features that drive organic sharing and return visits. Added one at a time.

- **Most improved leaderboard** — monthly ELO delta rankings, filterable by club
- **Top players / top clubs** — monthly snapshot rankings
- **All-time records** — most wins ever, longest streak, highest ELO in Swiss TT history (only possible with 35 years of data)
- **Rivalry detection** — recurring player matchups surfaced automatically across seasons
- **Dramatic match finder** — algorithmically surface the most exciting matches (upsets, comebacks, 10:9 results)
- **Head-to-head previews** — before a fixture, show h2h stats for likely player matchups (opponent scouting)
- **Season trajectory** — form curves showing how a team or player progressed through a season

---

## Explicitly not planned

- AI-generated form scores or mental strength ratings — dressed-up weighted formulas, not honest analytics
- Win rate by weekday — noise, not signal
- Native mobile app — PWA covers the use case
- Paywall or subscription of any kind
- Cup competitions — different format, lower priority, deferred indefinitely
