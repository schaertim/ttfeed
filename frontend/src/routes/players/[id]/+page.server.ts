import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { api } from '$lib/api';

export const load: PageServerLoad = async ({ params }) => {
	const player = await api.players.get(params.id).catch(() => null);
	if (!player) throw error(404, 'Player not found');

	return {
		player,
		streamed: {
			elo: api.players.elo(params.id),
			matches: api.players.matches(params.id),
		}
	};
};
