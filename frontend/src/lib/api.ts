import { PUBLIC_API_URL } from '$env/static/public';

const BASE = PUBLIC_API_URL + '/api/v1';

async function get<T>(path: string): Promise<T> {
	const res = await fetch(`${BASE}${path}`);
	if (!res.ok) throw new Error(`API error ${res.status}: ${path}`);
	return res.json();
}

// ── Types ────────────────────────────────────────────────────

export type Season = {
	id: string;
	name: string;
};

export type Federation = {
	id: string;
	name: string;
};

export type Group = {
	id: string;
	name: string;
	federation: string;
	season: string;
	promotionSpots: number | null;
	relegationSpots: number | null;
};

export type TeamSummary = {
	id: string;
	name: string;
	record: string;
	points: number;
	streak: string;
};

export type TeamPlayer = {
	id: string;
	fullName: string;
	licenceNr: string;
	wins: number;
	losses: number;
};

export type Standing = {
	teamId: string;
	team: string;
	position: number;
	played: number;
	won: number;
	drawn: number;
	lost: number;
	gamesWon: number;
	gamesLost: number;
	points: number;
};

export type Match = {
	id: string;
	homeTeam: string;
	awayTeam: string;
	homeScore: number | null;
	awayScore: number | null;
	round: string | null;
	playedAt: string | null;
	status: string;
};

export type SetScore = {
	setNumber: number;
	homePoints: number;
	awayPoints: number;
};

export type Game = {
	id: string;
	orderInMatch: number;
	gameType: string;
	homePlayerName: string | null;
	awayPlayerName: string | null;
	homeSets: number | null;
	awaySets: number | null;
	result: string;
	sets: SetScore[];
};

export type MatchDetail = Match & {
	games: Game[];
};

export type Player = {
	id: string;
	fullName: string;
	licenceNr: string;
	currentClubName: string | null;
	klass: string | null;
	currentElo: number | null;
	isSyncing: boolean;
};

export type EloEntry = {
	eloValue: number;
	recordedAt: string;
	seasonName: string;
};

export type PlayerGame = {
	matchId: string;
	gameId: string;
	playedAt: string | null;
	homeTeam: string;
	awayTeam: string;
	homeScore: number | null;
	awayScore: number | null;
	round: string | null;
	status: string;
	playerSide: 'home' | 'away';
	opponentName: string | null;
	homeSets: number | null;
	awaySets: number | null;
	result: 'HOME' | 'AWAY' | 'NOT_PLAYED';
	eloDelta: number | null;
};

export type PagedResponse<T> = {
	items: T[];
	page: number;
	size: number;
	total: number;
};

// ── API functions ────────────────────────────────────────────

export const api = {
	seasons: {
		list: () =>
			get<Season[]>('/seasons'),
	},

	federations: {
		list: () =>
			get<Federation[]>('/federations'),
	},

	groups: {
		list: (params?: { league?: string; season?: string }) => {
			const qs = new URLSearchParams();
			if (params?.league) qs.set('league', params.league);
			if (params?.season) qs.set('season', params.season);
			const query = qs.toString();
			return get<Group[]>(`/groups${query ? '?' + query : ''}`);
		},
		get: (groupId: string) =>
			get<Group>(`/groups/${groupId}`),
		standings: (groupId: string) =>
			get<Standing[]>(`/groups/${groupId}/standings`),
		matches: (groupId: string) =>
			get<Match[]>(`/groups/${groupId}/matches`),
	},

	teams: {
		get: (teamId: string) => get<TeamSummary>(`/teams/${teamId}`),
		roster: (teamId: string) => get<TeamPlayer[]>(`/teams/${teamId}/roster`),
		matches: (teamId: string) => get<Match[]>(`/teams/${teamId}/matches`),
	},

	matches: {
		detail: (matchId: string) =>
			get<MatchDetail>(`/matches/${matchId}`),
	},

	players: {
		search: (name: string, page = 0, size = 20) =>
			get<PagedResponse<Player>>(
				`/players/search?name=${encodeURIComponent(name)}&page=${page}&size=${size}`
			),
		get: (playerId: string) =>
			get<Player>(`/players/${playerId}`),
		elo: (playerId: string) =>
			get<EloEntry[]>(`/players/${playerId}/elo`),
		matches: (playerId: string) =>
			get<PlayerGame[]>(`/players/${playerId}/matches`),
	},
};