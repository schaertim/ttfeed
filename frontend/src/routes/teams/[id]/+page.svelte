<script lang="ts">
	import type { PageData } from './$types';
	import * as Card from '$lib/components/ui/card/index.js';
	import * as Table from '$lib/components/ui/table/index.js';
	import { Badge } from '$lib/components/ui/badge/index.js';
	import { Separator } from '$lib/components/ui/separator/index.js';
	import { Skeleton } from '$lib/components/ui/skeleton/index.js';

	let { data }: { data: PageData } = $props();

	function formatDate(dateStr: string | null): string {
		if (!dateStr) return 'TBD';
		return new Date(dateStr).toLocaleDateString('de-CH', { day: '2-digit', month: '2-digit' });
	}

	function getResultColor(match: any, teamName: string) {
		if (match.homeScore == null || match.awayScore == null) return 'text-muted-foreground';
		const win = match.homeTeam === teamName ? match.homeScore > match.awayScore : match.awayScore > match.homeScore;
		const draw = match.homeScore === match.awayScore;
		if (draw) return 'text-muted-foreground';
		return win ? 'text-win bg-win/10 border-win/20' : 'text-loss bg-loss/10 border-loss/20';
	}
</script>

<div class="p-4 pb-20 space-y-8 max-w-2xl mx-auto">
	<div class="space-y-4">
		<div class="space-y-1 px-1">
			<h1 class="text-3xl font-black uppercase tracking-tighter leading-none">{data.team.name}</h1>
		</div>

		<div class="grid grid-cols-3 gap-3 px-1">
			<Card.Root class="bg-card/50">
				<Card.Content class="p-4 flex flex-col items-center">
					<span class="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Record</span>
					<span class="text-xl font-black">{data.team.record}</span>
				</Card.Content>
			</Card.Root>
			<Card.Root class="bg-card/50">
				<Card.Content class="p-4 flex flex-col items-center">
					<span class="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Points</span>
					<span class="text-xl font-black">{data.team.points}</span>
				</Card.Content>
			</Card.Root>
			<Card.Root class="bg-card/50">
				<Card.Content class="p-4 flex flex-col items-center">
					<span class="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Streak</span>
					<span class="text-xl font-black {data.team.streak.includes('W') ? 'text-win' : data.team.streak.includes('L') ? 'text-loss' : ''}">
						{data.team.streak}
					</span>
				</Card.Content>
			</Card.Root>
		</div>
	</div>

	<section class="space-y-3">
		<h2 class="text-xs font-bold uppercase tracking-[0.2em] text-muted-foreground px-1">Active Roster</h2>
		<Card.Root class="overflow-hidden border-border/50">
			{#await data.streamed.roster}
				<div class="p-4 space-y-4">
					<Skeleton class="h-10 w-full" />
					<Skeleton class="h-10 w-full" />
					<Skeleton class="h-10 w-full" />
				</div>
			{:then roster}
				<Table.Root>
					<Table.Body>
						{#each roster as player}
							<Table.Row class="border-border/40">
								<Table.Cell class="py-3 pl-4">
									<div class="font-semibold text-sm">{player.fullName}</div>
									<div class="text-[10px] text-muted-foreground uppercase">{player.licenceNr}</div>
								</Table.Cell>
								<Table.Cell class="py-3 text-right pr-4">
									<Badge variant="secondary" class="font-mono text-xs">
										{player.wins}W - {player.losses}L
									</Badge>
								</Table.Cell>
							</Table.Row>
						{/each}
					</Table.Body>
				</Table.Root>
			{/await}
		</Card.Root>
	</section>

	<section class="space-y-4">
		<h2 class="text-xs font-bold uppercase tracking-[0.2em] text-muted-foreground px-1">Season Results</h2>
		<div class="space-y-2">
			{#await data.streamed.matches}
				<Skeleton class="h-20 w-full rounded-2xl" />
				<Skeleton class="h-20 w-full rounded-2xl" />
				<Skeleton class="h-20 w-full rounded-2xl" />
			{:then matches}
				{#each matches as match}
					<a href="/matches/{match.id}" class="flex items-center justify-between p-4 rounded-2xl bg-card border border-border/60 hover:border-primary/30 group">
						<div class="flex flex-col gap-1 min-w-0">
							<div class="flex items-center gap-2">
								<span class="text-[10px] font-black text-muted-foreground uppercase tracking-widest">Rd {match.round}</span>
								<Separator orientation="vertical" class="h-2" />
								<span class="text-[10px] font-bold text-muted-foreground/60">{formatDate(match.playedAt)}</span>
							</div>
							<div class="flex items-center gap-2 text-sm">
								<span class="truncate {match.homeTeam === data.team.name ? 'font-bold' : 'text-muted-foreground/80'}">{match.homeTeam}</span>
								<span class="text-[10px] font-bold text-muted-foreground/40">VS</span>
								<span class="truncate {match.awayTeam === data.team.name ? 'font-bold' : 'text-muted-foreground/80'}">{match.awayTeam}</span>
							</div>
						</div>
						<div class="flex flex-col items-end gap-1">
							<Badge variant="outline" class="font-black tabular-nums py-1 px-3 text-sm {getResultColor(match, data.team.name)}">
								{match.homeScore ?? '?'}:{match.awayScore ?? '?'}
							</Badge>
						</div>
					</a>
				{/each}
			{/await}
		</div>
	</section>
</div>