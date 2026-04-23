# Domain Knowledge — Swiss Table Tennis

## Competition structure

### Organisations
Swiss table tennis is organised by Swiss Table Tennis (STT) at the national level, with regional associations underneath:
- **STT** — national body, runs Nationalliga A/B/C and CH-CUP
- **NWTTV** — Northwest Switzerland, runs 1. Liga through 6. Liga
- **AGTT, ANJTT, ATTT, AVVF, MTTV, OTTV, TTVI** — other regional associations, each with their own league tiers

### Hierarchy
```
Season
└── League (e.g. NWTTV, STT)
    └── Division (e.g. 1. Liga, 2. Liga, NLA Men)
        └── Team (e.g. Carouge 1, Lugano 1)
            └── Match (team encounter)
                └── Game (individual singles or doubles)
                    └── Set (individual set score, from click-tt)
```

### Season structure
- Season runs roughly September to April, named e.g. "2025/2026"
- Each season has a **Vorrunde** (first half) and **Rückrunde** (second half)
- Every fixture is played twice — once in each half (home/away swapped)
- The two legs are treated as completely independent matches
- knob.ch displays both results side by side in the match list for reference

### Team match format
- Each team has 3 players
- Format: every player plays against every opponent (3×3 = 9 singles) + 1 doubles = **10 games maximum**
- Team score ranges from 0:10 to 10:0
- A draw is 5:5

### Points system (match points awarded per team encounter)
| Team score | Winner points | Loser points |
|---|---|---|
| 10:0, 9:1, 8:2 | 4 | 0 |
| 7:3, 6:4 | 3 | 1 |
| 5:5 | 2 | 2 |
| 4:6, 3:7 | 1 | 3 |
| 2:8, 1:9, 0:10 | 0 | 4 |

Points cannot be derived from win/draw/loss alone — the actual score is required.

### Veteran leagues
- **O40** — Over 40s league
- **O50** — Over 50s league
- Same match format and scoring as regular leagues

### Cup competitions
- CH-CUP (national) and regional cups exist
- Knockout format, different number of games per match (not the standard 10-game format)
- **Excluded from Phase 1** — will be added in a later phase

## Player classification system

### Klass
Every player has an official classification, displayed as e.g. `A21`, `A20`, `A19`, `A17`.
- The letter is always `A`
- The number represents a rating bracket derived from the player's Elo value
- The Elo range that corresponds to each Klass number has changed over time
- Klass is stored per player per season (it can differ between seasons)

### Klass update rules
- Updated at most **twice per season** — at season start and mid-season
- Mid-season updates: **only upward** (a player can be promoted to a higher class mid-season but never demoted)
- End-of-season updates: can go up or down

### Elo rating
- Computed and published by click-tt.ch (the official STT platform)
- Updated **monthly**
- Separate from Klass — Elo is a continuous number, Klass is a bracket
- Available per player per season on click-tt.ch player portrait pages
- Used for progression graphs (store as time series)

## Player identity and registration

### Licence number
- Every licensed Swiss TT player has a permanent STT licence number
- This is the **primary identifier** across all systems
- Present on both knob.ch and click-tt.ch
- Used as the bridge between the two data sources
- Stable for life — does not change when a player changes clubs

### Stammspieler (registered squad players)
- At the start of each season, clubs register their official squad players per team
- These are the "Stammspieler" — the officially registered players for that team/season
- knob.ch shows a Stammspieler filter in the individual player rankings
- A player can only play for one club per season

### Player transfers
- Players can only transfer between clubs **between seasons**, never mid-season
- One club per player per season — no exceptions

## Match data specifics

### Forfeits
- If a team fails to show up, the result is recorded as **10:0** or **0:10**
- Indistinguishable from a genuine result in the data — no special flag
- Treated identically to any other result

### Result corrections
- Results are final once entered
- Corrections can happen if something was entered incorrectly, but this is rare
- The scraper should treat completed results as immutable once stored

### Individual game data
- knob.ch shows the **set score** per game (e.g. 3:1 meaning player won 3 sets to 1)
- click-tt.ch shows the **individual set scores** (e.g. 11:8, 9:11, 11:7, 11:9)
- Set score data from click-tt is the "killer feature" — no other platform shows this

## knob.ch data source

### URL structure
- Base: `https://www.knob.ch/ms/index.php`
- Division/standings: `?gruppe={gruppe_id}`
- Player page: `?gid={knob_player_id}`
- Match detail: `?gruppe={gruppe_id}&matchid={match_id}`
- Team page: `?clubid={club_id}&teamid={team_id}`
- Season filter: add `&saison={season}` (e.g. `&saison=2024/2025`)
- League filter: `?rvid={league_id}`

### Key IDs visible in URLs
- `gruppe` — division identifier (e.g. gruppe=1 is STT NLA Men)
- `gid` — knob player ID
- `matchid` — match identifier
- `clubid` — club identifier
- `teamid` — team identifier

### Historical data
- Available back to 1989/1990
- All seasons accessible via season selector
- Full backfill required for rich player profiles

### Session handling
- knob.ch injects a `KSID` session parameter into URLs during a browser session
- The scraper should fetch the main page first to obtain a valid session if needed

## click-tt.ch data source

### What it provides
- Official STT platform — this is where clubs enter results
- Individual set scores per game (e.g. 11:8, 9:11, 11:7, 11:9)
- Official Elo rating per player
- Elo history (for progression graphs)

### URL structure
- Player portrait: `https://www.click-tt.ch/cgi-bin/WebObjects/nuLigaTTCH.woa/wa/playerPortrait?federation=STT&season={season}&person={clicktt_id}&club={clicktt_club_id}`
- Key parameters: `person` (click-tt player ID), `club` (click-tt club ID), `season` (e.g. `2025/26`)

### Linking to knob.ch
- Players are linked via the STT **licence number**, which appears on both platforms
- This is the join key — no fuzzy name matching required
