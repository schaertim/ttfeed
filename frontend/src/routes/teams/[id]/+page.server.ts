import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { api } from '$lib/api';

export const load: PageServerLoad = async ({ params }) => {
	try {
		// Await the fast, essential data
		const team = await api.teams.get(params.id);

		if (!team) throw error(404, 'Team not found');

		// Do NOT await these. Pass the promises to enable streaming.
		return {
			team,
			streamed: {
				roster: api.teams.roster(params.id),
				matches: api.teams.matches(params.id)
			}
		};
	} catch (e) {
		throw error(404, 'Team not found or backend unavailable');
	}
};