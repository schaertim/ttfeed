import { api } from '$lib/api';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async () => {
	const [seasons, federations] = await Promise.all([
		api.seasons.list(),
		api.federations.list(),
	]);

	return { seasons, federations };
};