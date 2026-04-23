<script lang="ts">
	import type { PageData } from './$types';
	import * as Card from '$lib/components/ui/card/index.js';
	import { Badge } from '$lib/components/ui/badge/index.js';
	import { Separator } from '$lib/components/ui/separator/index.js';

	let { data }: { data: PageData } = $props();

	function formatDate(dateStr: string | null): string {
		if (!dateStr) return 'TBD';
		return new Date(dateStr).toLocaleDateString('de-CH', {
			weekday: 'short',
			day: '2-digit',
			month: '2-digit',
			year: 'numeric'
		});
	}

	// Calculate aggregate match stats for the scoreboard header
	const aggregates = $derived.by(() => {
		let totalHomeSets = 0;
		let totalAwaySets = 0;
		let totalHomePoints = 0;
		let totalAwayPoints = 0;

		for (const game of data.match.games) {
			totalHomeSets += game.homeSets ?? 0;
			totalAwaySets += game.awaySets ?? 0;
			for (const set of game.sets) {
				totalHomePoints += set.homePoints ?? 0;
				totalAwayPoints += set.awayPoints ?? 0;
			}
		}

		return { totalHomeSets, totalAwaySets, totalHomePoints, totalAwayPoints };
	});
</script>

<div class="p-4 pb-20 space-y-6 max-w-2xl mx-auto">
	<header class="flex flex-col gap-1 px-1">
		<a href="javascript:history.back()" class="text-muted-foreground hover:text-primary transition-colors flex items-center gap-1 mb-2">
			<span class="material-symbols-outlined text-sm">chevron_left</span>
			<span class="text-xs font-bold uppercase tracking-widest">Back</span>
		</a>
		<span class="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">
			Round {data.match.round} · {formatDate(data.match.playedAt)}
		</span>
		<h1 class="text-3xl font-black uppercase tracking-tighter leading-none">
			Match Details
		</h1>
	</header>

	<Card.Root class="overflow-hidden border-border/50 shadow-lg">
		<div class="bg-card p-6 flex flex-col items-center gap-6">
			<div class="flex justify-between items-center w-full">
				<div class="flex-1 flex flex-col text-center">
					<span class="font-bold text-base md:text-lg leading-tight text-balance">{data.match.homeTeam}</span>
				</div>

				<div class="px-6 flex flex-col items-center">
					<Badge variant="outline" class="mb-2 text-[10px] font-black uppercase tracking-widest text-muted-foreground border-border/40">
						{data.match.status}
					</Badge>
					<span class="text-5xl font-black tabular-nums tracking-tighter">
						{data.match.homeScore ?? '?'}<span class="text-muted-foreground/30 mx-1">:</span>{data.match.awayScore ?? '?'}
					</span>
				</div>

				<div class="flex-1 flex flex-col text-center">
					<span class="font-bold text-base md:text-lg leading-tight text-balance">{data.match.awayTeam}</span>
				</div>
			</div>

			<Separator class="bg-border/40 w-3/4" />

			<div class="flex gap-8 text-[10px] font-bold text-muted-foreground uppercase tracking-widest">
				<div class="flex flex-col items-center">
					<span class="mb-1">Sets</span>
					<span class="text-foreground text-sm">{aggregates.totalHomeSets} : {aggregates.totalAwaySets}</span>
				</div>
				<div class="flex flex-col items-center">
					<span class="mb-1">Points</span>
					<span class="text-foreground text-sm">{aggregates.totalHomePoints} : {aggregates.totalAwayPoints}</span>
				</div>
			</div>
		</div>
	</Card.Root>

	<section class="space-y-3">
		<h2 class="text-xs font-bold uppercase tracking-[0.2em] text-muted-foreground px-1 mt-8">
			Game Breakdown
		</h2>

		<div class="space-y-3">
			{#each data.match.games as game}
				<Card.Root class="p-4 border-border/60 hover:border-primary/30 transition-all bg-card/50">
					<div class="flex justify-between items-center mb-4">
						<span class="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">
							Game #{game.orderInMatch} · {game.gameType}
						</span>
						<Badge
							variant="outline"
							class="tabular-nums font-black {game.result === 'HOME' ? 'text-win border-win/30 bg-win/10' : game.result === 'AWAY' ? 'text-loss border-loss/30 bg-loss/10' : 'text-muted-foreground'}"
						>
							{game.homeSets ?? 0}:{game.awaySets ?? 0}
						</Badge>
					</div>

					<div class="space-y-2 mb-4">
						<div class="flex justify-between items-center">
							<span class="text-sm {game.result === 'HOME' ? 'font-bold' : 'text-muted-foreground'}">
								{game.homePlayerName ?? 'Unknown Player'}
							</span>
							{#if game.result === 'HOME'}
								<span class="material-symbols-outlined text-win text-sm">check_circle</span>
							{/if}
						</div>
						<Separator class="bg-border/40" />
						<div class="flex justify-between items-center">
							<span class="text-sm {game.result === 'AWAY' ? 'font-bold' : 'text-muted-foreground'}">
								{game.awayPlayerName ?? 'Unknown Player'}
							</span>
							{#if game.result === 'AWAY'}
								<span class="material-symbols-outlined text-loss text-sm">check_circle</span>
							{/if}
						</div>
					</div>

					{#if game.sets && game.sets.length > 0}
						<div class="bg-muted/20 p-2.5 rounded-lg flex flex-wrap gap-2 items-center">
							<span class="text-[10px] font-bold text-muted-foreground uppercase tracking-widest mr-1">Sets</span>
							{#each game.sets as set}
								<Badge variant="secondary" class="text-xs tabular-nums font-medium bg-background border-border/40 shadow-sm">
									{set.homePoints}:{set.awayPoints}
								</Badge>
							{/each}
						</div>
					{:else if data.match.status === 'COMPLETED'}
						<span class="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">No set data available</span>
					{/if}
				</Card.Root>
			{/each}
		</div>
	</section>
</div>