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

export type Division = {
	id: string;
	name: string;
	federation: string;
	season: string;
};

export type Group = {
	id: string;
	name: string;
	promotionSpots: number | null;
	relegationSpots: number | null;
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

	divisions: {
		list: (params?: { league?: string; season?: string }) => {
			const qs = new URLSearchParams();
			console.log(params?.season);
			if (params?.league) qs.set('league', params.league);
			if (params?.season) qs.set('season', params.season);
			const query = qs.toString();
			return get<Division[]>(`/divisions${query ? '?' + query : ''}`);
		},
		groups: (divisionId: string) =>
			get<Group[]>(`/divisions/${divisionId}/groups`),
	},

	groups: {
		get: (groupId: string) =>
			get<Group>(`/groups/${groupId}`),
		standings: (groupId: string) =>
			get<Standing[]>(`/groups/${groupId}/standings`),
		matches: (groupId: string) =>
			get<Match[]>(`/groups/${groupId}/matches`),
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
	},
};