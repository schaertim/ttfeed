<script lang="ts">
	import type { PageData } from './$types';
	import type { Division, Group, Season } from '$lib/api';
	import { api } from '$lib/api';
	import * as Select from '$lib/components/ui/select/index.js';
	import * as Collapsible from '$lib/components/ui/collapsible/index.js';
	import * as Card from '$lib/components/ui/card/index.js';
	import { Input } from '$lib/components/ui/input/index.js';
	import { Separator } from '$lib/components/ui/separator/index.js';
	import { Badge } from '$lib/components/ui/badge/index.js';
	import { Skeleton } from '$lib/components/ui/skeleton/index.js';
	import { SvelteSet } from 'svelte/reactivity';
	import {
		MagnifyingGlass,
		CaretDown,
		CaretRight,
		Star,
		Globe,
		MapPin,
		TrendUp,
		Clock
	} from 'phosphor-svelte';

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

<div class="py-4 space-y-8">
	<div class="flex flex-col gap-4">
		<div class="flex items-end justify-between px-1">
			<div>
				<p class="text-xs font-bold uppercase tracking-widest text-muted-foreground">
					League Browser
				</p>
				<h1 class="text-3xl font-extrabold tracking-tight">Leagues</h1>
			</div>
			<Select.Root
				type="single"
				value={selectedSeasonId}
				onValueChange={(v) => { if (v) selectedSeasonId = v; }}
			>
				<Select.Trigger class="w-32 text-xs font-bold bg-card border-b border-border hover:border-primary transition-all">
					{selectedSeason?.name ?? 'Season'}
				</Select.Trigger>
				<Select.Content>
					{#each data.seasons as season (season.id)}
						<Select.Item value={season.id}>{season.name}</Select.Item>
					{/each}
				</Select.Content>
			</Select.Root>
		</div>
	</div>

	<section class="space-y-4">
		<div class="flex items-center justify-between px-1">
			<h3 class="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground whitespace-nowrap">Your Leagues</h3>
			<Separator class="flex-1 ml-4 bg-border/60" />
		</div>

		<div class="grid grid-cols-1 md:grid-cols-2 gap-3">
			<Card.Root class="p-4 flex items-center justify-between group hover:bg-accent transition-all cursor-pointer">
				<div class="flex items-center gap-4">
					<div class="w-10 h-10 bg-yellow-500 rounded-xl flex items-center justify-center text-yellow-950 font-black">NW</div>
					<div>
						<p class="font-bold text-sm">NWTTV 1. Liga</p>
						<p class="text-[10px] font-bold uppercase text-muted-foreground">Group 2 • Active Season</p>
					</div>
				</div>
				<Star weight="fill" class="w-5 h-5 text-yellow-500" />
			</Card.Root>

			<Card.Root class="p-4 flex items-center justify-between group hover:bg-accent transition-all cursor-pointer">
				<div class="flex items-center gap-4">
					<div class="w-10 h-10 bg-green-500 rounded-xl flex items-center justify-center text-green-950 font-black">ST</div>
					<div>
						<p class="font-bold text-sm">STT Nationalliga B</p>
						<p class="text-[10px] font-bold uppercase text-muted-foreground">Men • Professional</p>
					</div>
				</div>
				<Star weight="fill" class="w-5 h-5 text-yellow-500" />
			</Card.Root>
		</div>
	</section>

	<section class="space-y-4">
		<div class="flex items-center justify-between px-1">
			<h3 class="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground whitespace-nowrap">Browse Regions</h3>
			<Separator class="flex-1 ml-4 bg-border/60" />
		</div>

		{#if loadingDivisions}
			<div class="space-y-2">
				{#each [1, 2, 3] as i (i)}
					<Skeleton class="h-16 w-full rounded-xl" />
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
						class="bg-card rounded-xl overflow-hidden ring-1 ring-border shadow-sm"
					>
						<Collapsible.Trigger
							class="w-full flex items-center justify-between px-4 py-4
                           hover:bg-accent transition-colors text-left
                           {expandedFederations.has(fed.id) ? 'bg-secondary/10' : ''}"
						>
							<div class="flex items-center gap-4">
								{#if expandedFederations.has(fed.id)}
									<MapPin weight="bold" class="w-5 h-5 text-primary" />
									<span class="font-bold text-primary">{fed.name}</span>
								{:else}
									<Globe class="w-5 h-5 text-muted-foreground" />
									<span class="font-bold opacity-80">{fed.name}</span>
								{/if}
							</div>
							<CaretDown
								class="w-5 h-5 text-muted-foreground transition-transform duration-200
								{expandedFederations.has(fed.id) ? 'rotate-180' : ''}"
							/>
						</Collapsible.Trigger>

						<Collapsible.Content class="divide-y divide-border/40 bg-background/30">
							{#each fed.divisions as division (division.id)}
								<Collapsible.Root
									open={!!groupsMap[division.id]}
									onOpenChange={() => loadGroups(division.id)}
								>
									<Collapsible.Trigger
										class="w-full flex items-center justify-between p-4
                                       hover:bg-accent transition-colors text-left group"
									>
										<div class="flex items-center gap-4">
											<Badge variant="secondary" class="text-[10px] font-black px-2 py-1 rounded w-12 justify-center">
												{division.name.split(' ')[0] || 'LIG'}
											</Badge>
											<div>
												<p class="text-sm font-bold">{division.name}</p>
												<div class="flex gap-3 mt-1">
													<span class="text-[10px] font-bold text-muted-foreground uppercase">-- Teams</span>
													<span class="text-[10px] font-bold text-muted-foreground uppercase">Rd --/--</span>
												</div>
											</div>
										</div>
										{#if loadingGroups[division.id]}
											<span class="text-xs text-muted-foreground">...</span>
										{:else}
											<CaretRight
												class="w-5 h-5 text-muted-foreground transition-transform duration-200 group-hover:text-foreground
												{groupsMap[division.id] ? 'rotate-90' : ''}"
											/>
										{/if}
									</Collapsible.Trigger>

									<Collapsible.Content class="bg-background/60 divide-y divide-border/40">
										{#if groupsMap[division.id]}
											{#each groupsMap[division.id] as group (group.id)}
												<a
													href="/groups/{group.id}"
													class="flex items-center justify-between px-4 py-3 pl-[4.5rem]
                                                   hover:bg-accent transition-colors"
												>
													<span class="text-sm font-medium">{group.name}</span>
													<CaretRight class="w-4 h-4 text-muted-foreground" />
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
	</section>

	<section class="grid grid-cols-2 gap-4 pt-4">
		<Card.Root class="p-6 border-l-4 border-l-primary border-y-border border-r-border shadow-sm">
			<p class="text-[10px] font-black uppercase text-muted-foreground mb-2">Players Registered</p>
			<p class="text-4xl font-black">4.2K</p>
			<div class="mt-4 flex items-center gap-2 text-green-500">
				<TrendUp class="w-4 h-4" />
				<span class="text-[10px] font-bold">+12% this season</span>
			</div>
		</Card.Root>

		<Card.Root class="p-6 border-l-4 border-l-destructive border-y-border border-r-border shadow-sm">
			<p class="text-[10px] font-black uppercase text-muted-foreground mb-2">Matches Played</p>
			<p class="text-4xl font-black">186</p>
			<div class="mt-4 flex items-center gap-2 text-muted-foreground">
				<Clock class="w-4 h-4" />
				<span class="text-[10px] font-bold">Last 24 hours</span>
			</div>
		</Card.Root>
	</section>
</div>