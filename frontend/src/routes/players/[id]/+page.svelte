<script lang="ts">
	import type { PageData } from './$types';
	import * as Card from '$lib/components/ui/card/index.js';
	import { Badge } from '$lib/components/ui/badge/index.js';
	import { Skeleton } from '$lib/components/ui/skeleton/index.js';
	import { Separator } from '$lib/components/ui/separator/index.js';
	import { Chart, Svg, Spline, Axis, Grid, Points } from 'layerchart';
	import type { PlayerGame } from '$lib/api';

	let { data }: { data: PageData } = $props();

	function formatDate(dateStr: string | null): string {
		if (!dateStr) return '—';
		return new Date(dateStr).toLocaleDateString('de-CH', { day: '2-digit', month: '2-digit' });
	}

	function isWin(game: PlayerGame): boolean {
		return (
			(game.result === 'HOME' && game.playerSide === 'home') ||
			(game.result === 'AWAY' && game.playerSide === 'away')
		);
	}

	function playerSets(game: PlayerGame): number {
		return game.playerSide === 'home' ? (game.homeSets ?? 0) : (game.awaySets ?? 0);
	}

	function opponentSets(game: PlayerGame): number {
		return game.playerSide === 'home' ? (game.awaySets ?? 0) : (game.homeSets ?? 0);
	}

	function formatDelta(delta: number | null): string {
		if (delta == null) return '';
		const rounded = Math.round(delta);
		return rounded >= 0 ? `+${rounded}` : String(rounded);
	}
</script>

<div class="p-4 pb-20 space-y-6 max-w-2xl mx-auto">
	<header class="space-y-4">
		<a
			href="javascript:history.back()"
			class="text-muted-foreground hover:text-primary transition-colors flex items-center gap-1"
		>
			<span class="material-symbols-outlined text-sm">chevron_left</span>
			<span class="text-xs font-bold uppercase tracking-widest">Back</span>
		</a>

		<div class="flex items-start justify-between gap-4">
			<div class="min-w-0">
				<h1 class="text-3xl font-black uppercase tracking-tighter leading-none break-words">
					{data.player.fullName}
				</h1>
				<p class="text-sm text-muted-foreground mt-1.5">
					{data.player.currentClubName ?? 'No Club'}
				</p>
			</div>

			<div class="flex flex-col items-end gap-1 shrink-0">
				{#if data.player.currentElo}
					<span class="text-4xl font-black tabular-nums leading-none">{data.player.currentElo}</span>
					<span class="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">ELO</span>
				{/if}
				{#if data.player.klass}
					<Badge variant="secondary" class="font-black mt-1">{data.player.klass}</Badge>
				{/if}
			</div>
		</div>
	</header>

	<!-- ELO History Chart -->
	<section class="space-y-2">
		<h2 class="text-xs font-bold uppercase tracking-[0.2em] text-muted-foreground px-1">
			ELO History
		</h2>
		<Card.Root class="overflow-hidden border-border/50">
			{#await data.streamed.elo}
				<Skeleton class="h-52 rounded-none" />
			{:then eloHistory}
				{#if eloHistory.length < 2}
					<div class="h-40 flex items-center justify-center">
						<p class="text-sm text-muted-foreground">Not enough ELO data yet.</p>
					</div>
				{:else}
					{@const eloPoints = eloHistory.map((e) => ({
						date: new Date(e.recordedAt),
						value: e.eloValue
					}))}
					{@const values = eloPoints.map((p) => p.value)}
					{@const yMin = Math.min(...values) - 30}
					{@const yMax = Math.max(...values) + 30}
					<div class="p-4 h-52">
						<Chart
							data={eloPoints}
							x="date"
							y="value"
							yDomain={[yMin, yMax]}
							padding={{ left: 44, bottom: 28, top: 8, right: 8 }}
						>
							<Svg>
								<Grid y={{ class: 'stroke-border/30' }} />
								<Axis placement="left" ticks={4} />
								<Axis
									placement="bottom"
									ticks={5}
									format={(d) =>
										d.toLocaleDateString('de-CH', { month: 'short', year: '2-digit' })}
								/>
								<Spline class="stroke-primary stroke-[1.5]" />
								<Points class="fill-primary [r:2]" />
							</Svg>
						</Chart>
					</div>
				{/if}
			{/await}
		</Card.Root>
	</section>

	<!-- Season Stats + Match History (single await) -->
	{#await data.streamed.matches}
		<div class="grid grid-cols-3 gap-3">
			{#each [1, 2, 3] as _}
				<Card.Root class="bg-card/50">
					<Card.Content class="p-4 flex flex-col items-center gap-2">
						<Skeleton class="h-3 w-12" />
						<Skeleton class="h-7 w-8" />
					</Card.Content>
				</Card.Root>
			{/each}
		</div>
		<section class="space-y-2">
			<Skeleton class="h-3 w-24 rounded" />
			{#each [1, 2, 3, 4] as _}
				<Skeleton class="h-16 w-full rounded-2xl" />
			{/each}
		</section>
	{:then matches}
		{@const played = matches.filter((m) => m.result !== 'NOT_PLAYED')}
		{@const wins = played.filter((m) => isWin(m)).length}
		{@const losses = played.length - wins}
		{@const winPct = played.length > 0 ? Math.round((wins / played.length) * 100) : 0}

		<div class="grid grid-cols-3 gap-3">
			<Card.Root class="bg-card/50">
				<Card.Content class="p-4 flex flex-col items-center">
					<span class="text-[10px] font-bold text-muted-foreground uppercase tracking-widest"
						>Wins</span
					>
					<span class="text-2xl font-black text-win">{wins}</span>
				</Card.Content>
			</Card.Root>
			<Card.Root class="bg-card/50">
				<Card.Content class="p-4 flex flex-col items-center">
					<span class="text-[10px] font-bold text-muted-foreground uppercase tracking-widest"
						>Losses</span
					>
					<span class="text-2xl font-black text-loss">{losses}</span>
				</Card.Content>
			</Card.Root>
			<Card.Root class="bg-card/50">
				<Card.Content class="p-4 flex flex-col items-center">
					<span class="text-[10px] font-bold text-muted-foreground uppercase tracking-widest"
						>Win %</span
					>
					<span class="text-2xl font-black">{winPct}%</span>
				</Card.Content>
			</Card.Root>
		</div>

		<section class="space-y-3">
			<h2 class="text-xs font-bold uppercase tracking-[0.2em] text-muted-foreground px-1">
				Match History
			</h2>

			{#if matches.length === 0}
				<p class="text-center text-sm text-muted-foreground py-8">No matches found.</p>
			{:else}
				<div class="space-y-2">
					{#each matches as game (game.gameId)}
						<a
							href="/matches/{game.matchId}"
							class="flex items-center justify-between p-4 rounded-2xl bg-card border border-border/60 hover:border-primary/30 transition-colors group"
						>
							<div class="flex flex-col gap-1 min-w-0">
								<div class="flex items-center gap-2">
									<span
										class="text-[10px] font-black text-muted-foreground uppercase tracking-widest"
									>
										{game.round ? `Rd ${game.round}` : '—'}
									</span>
									<Separator orientation="vertical" class="h-2" />
									<span class="text-[10px] font-bold text-muted-foreground/60"
										>{formatDate(game.playedAt)}</span
									>
								</div>
								<span class="text-sm font-semibold truncate">
									{game.opponentName ?? 'Unknown'}
								</span>
								<span class="text-[10px] text-muted-foreground/50 truncate">
									{game.homeTeam} vs {game.awayTeam}
								</span>
							</div>

							<div class="flex flex-col items-end gap-1.5 shrink-0 ml-3">
								<Badge
									variant="outline"
									class="font-black tabular-nums {isWin(game)
										? 'text-win border-win/30 bg-win/10'
										: game.result === 'NOT_PLAYED'
											? 'text-muted-foreground'
											: 'text-loss border-loss/30 bg-loss/10'}"
								>
									{playerSets(game)}:{opponentSets(game)}
								</Badge>
								{#if game.eloDelta != null}
									<span
										class="text-[11px] font-black {game.eloDelta >= 0
											? 'text-win'
											: 'text-loss'}"
									>
										{formatDelta(game.eloDelta)}
									</span>
								{/if}
							</div>
						</a>
					{/each}
				</div>
			{/if}
		</section>
	{/await}
</div>
