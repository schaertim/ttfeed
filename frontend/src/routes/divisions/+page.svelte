<script lang="ts">
	import type { PageData } from './$types';
	import type { Division, Group, Season } from '$lib/api';
	import { api } from '$lib/api';
	import { Badge } from '$lib/components/ui/badge/index.js';
	import * as Select from '$lib/components/ui/select/index.js';
	import * as Collapsible from '$lib/components/ui/collapsible/index.js';
	import { SvelteSet } from 'svelte/reactivity';

	let { data }: { data: PageData } = $props();

	// Default to most recent season
	let selectedSeasonId = $state(data.seasons[0]?.id ?? '');

	const selectedSeason = $derived(
		data.seasons.find((s: Season) => s.id === selectedSeasonId)
	);

	// Divisions loaded client-side when season changes
	let divisions = $state<Division[]>([]);
	let loadingDivisions = $state(false);

	// Groups per division — keyed by division id
	let groupsMap = $state<Record<string, Group[]>>({});
	let loadingGroups = $state<Record<string, boolean>>({});

	// Track which federations are expanded
	let expandedFederations = $state<Set<string>>(new Set());

	async function loadDivisions() {
		loadingDivisions = true;
		groupsMap = {};
		expandedFederations = new SvelteSet();
		try {
			divisions = await api.divisions.list({ season: selectedSeason?.name });
		} finally {
			loadingDivisions = false;
		}
	}

	async function toggleFederation(federationId: string) {
		if (expandedFederations.has(federationId)) {
			expandedFederations.delete(federationId);
			expandedFederations = new SvelteSet(expandedFederations);
		} else {
			expandedFederations.add(federationId);
			expandedFederations = new SvelteSet(expandedFederations);
		}
	}

	async function loadGroups(divisionId: string) {
		if (groupsMap[divisionId]) return; // already loaded
		loadingGroups = { ...loadingGroups, [divisionId]: true };
		try {
			const groups = await api.divisions.groups(divisionId);
			groupsMap = { ...groupsMap, [divisionId]: groups };
		} finally {
			const { [divisionId]: _, ...rest } = loadingGroups;
			loadingGroups = rest;
		}
	}

	// Divisions grouped by federation
	const divisionsByFederation = $derived(
		data.federations.map((fed) => ({
			...fed,
			divisions: divisions.filter((d) => d.federation === fed.name),
		})).filter((fed) => fed.divisions.length > 0)
	);

	// Load divisions on mount and when season changes
	$effect(() => {
		loadDivisions();
	});
</script>

<div class="py-4 space-y-6">
	<!-- Header + season selector -->
	<div class="flex items-end justify-between px-1">
		<div>
			<p class="text-xs font-bold uppercase tracking-widest text-muted-foreground">
				League Browser
			</p>
			<h1 class="text-2xl font-bold">Leagues</h1>
		</div>
		<Select.Root
			type="single"
			value={selectedSeasonId}
			onValueChange={(v) => { if (v) selectedSeasonId = v; }}
		>
			<Select.Trigger class="w-32 text-xs">
				{selectedSeason?.name ?? 'Season'}
			</Select.Trigger>
			<Select.Content>
				{#each data.seasons as season (season.id)}
					<Select.Item value={season.id}>{season.name}</Select.Item>
				{/each}
			</Select.Content>
		</Select.Root>
	</div>

	<!-- Division list -->
	{#if loadingDivisions}
		<div class="space-y-2">
			{#each [1, 2, 3] as i (i)}
				<div class="h-14 rounded-xl bg-card animate-pulse"></div>
			{/each}
		</div>
	{:else if divisionsByFederation.length === 0}
		<p class="text-center text-sm text-muted-foreground py-12">
			No divisions found for this season
		</p>
	{:else}
		<div class="space-y-2">
			{#each divisionsByFederation as fed (fed.id)}
				<Collapsible.Root
					open={expandedFederations.has(fed.id)}
					onOpenChange={() => toggleFederation(fed.id)}
				>
					<!-- Federation header -->
					<Collapsible.Trigger
						class="w-full flex items-center justify-between px-4 py-3.5
                   rounded-xl bg-card border border-border
                   hover:bg-accent transition-colors text-left"
					>
						<div class="flex items-center gap-3">
							<Badge variant="outline" class="font-mono font-bold text-xs w-10 justify-center">
								{fed.name}
							</Badge>
							<span class="text-sm font-medium">
								{fed.divisions.length} division{fed.divisions.length !== 1 ? 's' : ''}
							</span>
						</div>
						<span class="text-muted-foreground text-xs">
							{expandedFederations.has(fed.id) ? '▲' : '▼'}
						</span>
					</Collapsible.Trigger>

					<Collapsible.Content class="mt-1 space-y-1 pl-2">
						{#each fed.divisions as division (division.id)}
							<!-- Division row — expands to show groups -->
							<Collapsible.Root
								open={!!groupsMap[division.id]}
								onOpenChange={() => loadGroups(division.id)}
							>
								<Collapsible.Trigger
									class="w-full flex items-center justify-between px-4 py-3
                         rounded-xl bg-card border border-border
                         hover:bg-accent transition-colors text-left"
								>
									<span class="text-sm font-medium">{division.name}</span>
									{#if loadingGroups[division.id]}
										<span class="text-xs text-muted-foreground">Loading...</span>
									{:else}
										<span class="text-muted-foreground text-xs">
											{groupsMap[division.id] ? '▲' : '▼'}
										</span>
									{/if}
								</Collapsible.Trigger>

								<Collapsible.Content class="mt-1 space-y-1 pl-2">
									{#if groupsMap[division.id]}
										{#each groupsMap[division.id] as group (group.id)}
											<a
												href="/groups/{group.id}"
												class="flex items-center justify-between px-4 py-2.5
                               rounded-xl bg-card border border-border
                               hover:bg-accent transition-colors"
											>
												<span class="text-sm">{group.name}</span>
												<span class="text-muted-foreground text-xs">→</span>
											</a>
										{/each}
									{/if}
								</Collapsible.Content>
							</Collapsible.Root>
						{/each}
					</Collapsible.Content>
				</Collapsible.Root>
			{/each}
		</div>
	{/if}
</div>