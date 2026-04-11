import { api } from '$lib/api';
import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ params }) => {
	const id = params.id;

	const [group, standings, matches] = await Promise.all([
		api.groups.get(id).catch(() => null),
		api.groups.standings(id).catch(() => null),
		api.groups.matches(id).catch(() => null),
	]);

	if (!group || !standings) error(404, 'Group not found');

	return { group, standings, matches: matches ?? [] };
};