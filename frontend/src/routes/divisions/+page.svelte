<script lang="ts">
	import type { PageData } from './$types';
	import type { Group, Season } from '$lib/api';
	import { api } from '$lib/api';
	import * as Select from '$lib/components/ui/select/index.js';
	import * as Collapsible from '$lib/components/ui/collapsible/index.js';
	import * as Card from '$lib/components/ui/card/index.js';
	import { Separator } from '$lib/components/ui/separator/index.js';
	import { Skeleton } from '$lib/components/ui/skeleton/index.js';
	import { SvelteSet } from 'svelte/reactivity';
	import { CaretDown, CaretRight, Star, Globe, MapPin, TrendUp, Clock } from 'phosphor-svelte';

	let { data }: { data: PageData } = $props();

	let selectedSeasonId = $state(data.seasons[0]?.id ?? '');

	const selectedSeason = $derived(data.seasons.find((s: Season) => s.id === selectedSeasonId));

	let groups = $state<Group[]>([]);
	let loadingGroups = $state(false);
	let expandedFederations = $state<Set<string>>(new Set());

	async function loadGroups() {
		loadingGroups = true;
		expandedFederations = new SvelteSet();
		try {
			groups = await api.groups.list({ season: selectedSeason?.name });
		} finally {
			loadingGroups = false;
		}
	}

	function toggleFederation(federationId: string) {
		if (expandedFederations.has(federationId)) {
			expandedFederations.delete(federationId);
		} else {
			expandedFederations.add(federationId);
		}
		expandedFederations = new SvelteSet(expandedFederations);
	}

	// Groups grouped by federation name
	const groupsByFederation = $derived(
		data.federations
			.map((fed) => ({
				...fed,
				groups: groups.filter((g) => g.federation === fed.name)
			}))
			.filter((fed) => fed.groups.length > 0)
	);

	$effect(() => {
		loadGroups();
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

		{#if loadingGroups}
			<div class="space-y-2">
				{#each [1, 2, 3] as i (i)}
					<Skeleton class="h-16 w-full rounded-xl" />
				{/each}
			</div>
		{:else if groupsByFederation.length === 0}
			<p class="text-center text-sm text-muted-foreground py-12">
				No groups found for this season
			</p>
		{:else}
			<div class="space-y-2">
				{#each groupsByFederation as fed (fed.id)}
					<Collapsible.Root
						open={expandedFederations.has(fed.id)}
						onOpenChange={() => toggleFederation(fed.id)}
						class="bg-card rounded-xl overflow-hidden ring-1 ring-border shadow-sm"
					>
						<Collapsible.Trigger
							class="w-full flex items-center justify-between px-4 py-4 hover:bg-accent transition-colors text-left
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
							{#each fed.groups as group (group.id)}
								<a
									href="/groups/{group.id}"
									class="flex items-center justify-between px-4 py-3 hover:bg-accent transition-colors group"
								>
									<span class="text-sm font-medium">{group.name}</span>
									<CaretRight class="w-4 h-4 text-muted-foreground group-hover:text-foreground transition-colors" />
								</a>
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