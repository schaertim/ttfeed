<script lang="ts">
	import { Input } from '$lib/components/ui/input/index.js';
	import * as Card from '$lib/components/ui/card/index.js';
	import { Badge } from '$lib/components/ui/badge/index.js';
	import * as Avatar from '$lib/components/ui/avatar/index.js';
	import { Skeleton } from '$lib/components/ui/skeleton/index.js';
	import { MagnifyingGlass, Heart } from 'phosphor-svelte';
	import { api } from '$lib/api';
	import type { Player } from '$lib/api'; // Make sure Player type matches PlayerResponse

	let searchQuery = $state('');
	let isSearching = $state(false);
	let searchResults = $state<Player[]>([]);

	// Debounce timer
	let timer: ReturnType<typeof setTimeout>;

	$effect(() => {
		if (searchQuery.length >= 3) {
			isSearching = true;
			clearTimeout(timer);

			// Debounce for 300ms so we don't spam the Ktor backend on every keystroke
			timer = setTimeout(async () => {
				try {
					const response = await api.players.search(searchQuery);
					searchResults = response.items;
				} catch (e) {
					console.error("Search failed", e);
					searchResults = [];
				} finally {
					isSearching = false;
				}
			}, 300);
		} else {
			searchResults = [];
			isSearching = false;
		}

		return () => clearTimeout(timer);
	});

	// Helper to extract initials (e.g. "Tim Schär" -> "TS")
	function getInitials(name: string) {
		return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
	}

	// ... Keep your favorites and topPlayers mock arrays here exactly as they were ...
</script>

<div class="py-4 space-y-8 pb-20">
	<div class="px-1">
		<h1 class="text-3xl font-extrabold tracking-tight">Search</h1>
		<p class="text-xs font-bold uppercase tracking-widest text-muted-foreground mt-1">Players & Clubs</p>
	</div>

	<section class="relative group">
		<MagnifyingGlass class="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground w-5 h-5 group-focus-within:text-primary transition-colors" />
		<Input
			bind:value={searchQuery}
			class="w-full bg-card border-none border-b border-border focus-visible:ring-0 pl-12 py-6 rounded-xl transition-all shadow-sm text-base"
			placeholder="Search players (min 3 chars)..."
		/>
	</section>

	{#if searchQuery.length < 3}
	{:else}
		<section class="space-y-4">
			<div class="flex items-baseline justify-between px-1">
				<h2 class="text-xs font-bold uppercase tracking-[0.1em] text-muted-foreground">Results for "{searchQuery}"</h2>
			</div>

			{#if isSearching}
				<div class="space-y-3">
					{#each [1, 2, 3] as _}
						<Card.Root class="p-4 flex items-center gap-4">
							<Skeleton class="w-10 h-10 rounded-full" />
							<div class="space-y-2 flex-1">
								<Skeleton class="h-4 w-32" />
								<Skeleton class="h-3 w-24" />
							</div>
						</Card.Root>
					{/each}
				</div>
			{:else if searchResults.length === 0}
				<p class="text-center text-sm text-muted-foreground py-12">
					No players found.
				</p>
			{:else}
				<div class="space-y-3">
					{#each searchResults as player (player.id)}
						<Card.Root class="p-4 flex items-center justify-between hover:bg-accent transition-colors cursor-pointer">
							<div class="flex items-center gap-4">
								<Avatar.Root class="w-10 h-10 border border-border bg-primary/10 text-primary">
									<Avatar.Fallback class="font-black text-xs">{getInitials(player.fullName)}</Avatar.Fallback>
								</Avatar.Root>

								<div>
									<h3 class="text-sm font-bold leading-tight">{player.fullName}</h3>
									<p class="text-[10px] text-muted-foreground font-medium uppercase tracking-wider">
										{player.currentClubName ?? 'No Club'}
									</p>
								</div>
							</div>

							<div class="text-right">
								<div class="flex items-center justify-end gap-1.5">
									{#if player.klass}
										<Badge variant="secondary" class="text-[9px] font-black px-1.5 py-0 rounded-sm h-4">
											{player.klass}
										</Badge>
									{/if}
								</div>
							</div>
						</Card.Root>
					{/each}
				</div>
			{/if}
		</section>
	{/if}
</div>