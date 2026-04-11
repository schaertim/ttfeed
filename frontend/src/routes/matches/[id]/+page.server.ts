import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { api } from '$lib/api';

export const load: PageServerLoad = async ({ params }) => {
	try {
		const match = await api.matches.detail(params.id);

		if (!match) {
			throw error(404, 'Match not found');
		}

		return {
			match
		};
	} catch (e) {
		console.error('Error loading match details:', e);
		throw error(404, 'Match not found or backend unavailable');
	}
};