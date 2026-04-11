<script lang="ts">
	import type { PageData } from './$types';
	import type { Match } from '$lib/api';
	import * as Tabs from '$lib/components/ui/tabs/index.js';
	import * as Table from '$lib/components/ui/table/index.js';
	import { Badge } from '$lib/components/ui/badge/index.js';

	let { data }: { data: PageData } = $props();

	const sorted = $derived(
		[...data.standings].sort((a, b) => a.position - b.position)
	);

	const completedMatches = $derived(
		data.matches
			.filter((m: Match) => m.status === 'completed')
			.sort((a: Match, b: Match) =>
				(b.round ?? '0').localeCompare(a.round ?? '0', undefined, { numeric: true })
			)
	);

	const scheduledMatches = $derived(
		data.matches
			.filter((m: Match) => m.status === 'scheduled')
			.sort((a: Match, b: Match) =>
				(a.round ?? '0').localeCompare(b.round ?? '0', undefined, { numeric: true })
			)
	);

	function zone(pos: number): 'promotion' | 'relegation' | null {
		const { promotionSpots, relegationSpots } = data.group;
		if (promotionSpots && pos <= promotionSpots) return 'promotion';
		if (relegationSpots && pos > sorted.length - relegationSpots) return 'relegation';
		return null;
	}

	function diff(won: number, lost: number): string {
		const d = won - lost;
		return d > 0 ? `+${d}` : `${d}`;
	}

	function formatDate(dateStr: string | null): string {
		if (!dateStr) return 'TBD';
		return new Date(dateStr).toLocaleDateString('de-CH', {
			day: '2-digit',
			month: '2-digit',
			year: '2-digit',
		});
	}
</script>

<div class="py-4 space-y-1 px-1">
	<p class="text-s font-bold uppercase tracking-widest text-muted-foreground">
		{data.group.name}
	</p>
</div>

<Tabs.Root value="standings">
	<Tabs.List class="w-full">
		<Tabs.Trigger value="standings" class="flex-1">Standings</Tabs.Trigger>
		<Tabs.Trigger value="results" class="flex-1">Results</Tabs.Trigger>
		<Tabs.Trigger value="schedule" class="flex-1">Schedule</Tabs.Trigger>
	</Tabs.List>

	<Tabs.Content value="standings" class="mt-4 space-y-3">
		<div class="rounded-xl overflow-hidden border border-border">
			<Table.Root>
				<Table.Header>
					<Table.Row class="hover:bg-transparent border-border">
						<Table.Head class="w-8 pl-5 text-xs">Pos</Table.Head>
						<Table.Head class="text-xs">Team</Table.Head>
						<Table.Head class="text-center w-10 text-xs">Pld</Table.Head>
						<Table.Head class="text-center w-10 text-xs">Pts</Table.Head>
						<Table.Head class="text-right pr-4 w-12 text-xs">+/-</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each sorted as row (row.teamId)}
						{@const z = zone(row.position)}
						<Table.Row class="border-border relative">
							{#if z === 'promotion'}
								<td class="absolute left-0 top-0 bottom-0 w-0.75 bg-win" aria-hidden="true"></td>
							{:else if z === 'relegation'}
								<td class="absolute left-0 top-0 bottom-0 w-0.75 bg-loss" aria-hidden="true"></td>
							{/if}

							<Table.Cell
								class="pl-5 font-bold tabular-nums"
							>
								{row.position}
							</Table.Cell>

							<Table.Cell class="text-sm font-medium">
								<a href="/teams/{row.teamId}" class="hover:underline text-primary">
									{row.team}
								</a>
							</Table.Cell>

							<Table.Cell class="text-center tabular-nums">
								{row.played}
							</Table.Cell>

							<Table.Cell
								class="text-center tabular-nums font-bold"
							>
								{row.points}
							</Table.Cell>

							<Table.Cell
								class="text-right pr-4 tabular-nums font-medium
                       {row.gamesWon - row.gamesLost > 0 ? 'text-win' :
                        row.gamesWon - row.gamesLost < 0 ? 'text-loss' :
                        'text-muted-foreground'}"
							>
								{diff(row.gamesWon, row.gamesLost)}
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
	</Tabs.Content>

	<Tabs.Content value="results" class="mt-4 space-y-2">
		{#if completedMatches.length === 0}
			<p class="text-center text-sm text-muted-foreground py-12">No results yet</p>
		{:else}
			{#each completedMatches as match (match.id)}
				<a
					href="/matches/{match.id}"
					class="flex items-center justify-between px-4 py-3 rounded-xl
                 bg-card border border-border hover:bg-accent transition-colors"
				>
					<div class="flex flex-col gap-0.5 min-w-0">
            <span class="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
              Rd {match.round} · {formatDate(match.playedAt)}
            </span>
						<div class="flex items-center gap-1.5 min-w-0 text-sm">
							<span class="font-medium truncate">{match.homeTeam}</span>
							<span class="text-muted-foreground flex-shrink-0">vs</span>
							<span class="font-medium truncate">{match.awayTeam}</span>
						</div>
					</div>
					<Badge
						variant="outline"
						class="flex-shrink-0 ml-3 tabular-nums font-bold
                   {match.homeScore != null && match.awayScore != null
                     ? match.homeScore > match.awayScore
                       ? 'border-win/30 text-win bg-win/10'
                       : match.homeScore < match.awayScore
                       ? 'border-loss/30 text-loss bg-loss/10'
                       : 'text-muted-foreground'
                     : 'text-muted-foreground'}"
					>
						{match.homeScore ?? '?'}:{match.awayScore ?? '?'}
					</Badge>
				</a>
			{/each}
		{/if}
	</Tabs.Content>

	<Tabs.Content value="schedule" class="mt-4 space-y-2">
		{#if scheduledMatches.length === 0}
			<p class="text-center text-sm text-muted-foreground py-12">No upcoming matches</p>
		{:else}
			{#each scheduledMatches as match (match.id)}
				<div
					class="flex items-center justify-between px-4 py-3 rounded-xl
                 bg-card border border-border"
				>
					<div class="flex flex-col gap-0.5 min-w-0">
            <span class="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
              Rd {match.round} · {formatDate(match.playedAt)}
            </span>
						<div class="flex items-center gap-1.5 min-w-0 text-sm">
							<span class="font-medium truncate">{match.homeTeam}</span>
							<span class="text-muted-foreground flex-shrink-0">vs</span>
							<span class="font-medium truncate">{match.awayTeam}</span>
						</div>
					</div>
					<Badge variant="outline" class="flex-shrink-0 ml-3 text-muted-foreground text-xs">
						{formatDate(match.playedAt)}
					</Badge>
				</div>
			{/each}
		{/if}
	</Tabs.Content>
</Tabs.Root>