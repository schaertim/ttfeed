<script lang="ts">
	import Accordion   from '$lib/ui/Accordion.svelte';
	import Avatar      from '$lib/ui/Avatar.svelte';
	import Badge       from '$lib/ui/Badge.svelte';
	import Button      from '$lib/ui/Button.svelte';
	import Card        from '$lib/ui/Card.svelte';
	import Chip        from '../lib/components/ui/Chip.svelte';
	import Divider     from '$lib/ui/Divider.svelte';
	import Icon        from '$lib/ui/Icon.svelte';
	import Input       from '$lib/ui/Input.svelte';
	import ListItem    from '$lib/ui/ListItem.svelte';
	import ProgressBar from '$lib/ui/ProgressBar.svelte';
	import Stat        from '../lib/components/ui/Stat.svelte';
	import Tabs        from '$lib/ui/Tabs.svelte';

	let searchValue = $state('');
	let accordionOpen = $state(false);
</script>

<!-- ─────────────────────────────────────────────────────── -->
<!-- Wrapper: page bg comes from body, just add padding     -->
<!-- ─────────────────────────────────────────────────────── -->
<div class="max-w-lg mx-auto px-4 py-10 space-y-12">

	<!-- Page title -->
	<div>
		<p class="text-label uppercase text-on-surface-subtle mb-1">Design system</p>
		<h1 class="text-title text-on-surface">Component showcase</h1>
	</div>

	<!-- ── Badge ────────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Badge" />

		<div class="flex flex-wrap gap-2">
			<Badge variant="default">Default</Badge>
			<Badge variant="win">Win</Badge>
			<Badge variant="loss">Loss</Badge>
			<Badge variant="draw">Draw</Badge>
		</div>

		<p class="text-label uppercase text-on-surface-subtle">Klassierung tiers</p>
		<div class="flex flex-wrap gap-2">
			<Badge variant="klass-e">E2</Badge>
			<Badge variant="klass-d">D4</Badge>
			<Badge variant="klass-c">C8</Badge>
			<Badge variant="klass-b">B11</Badge>
			<Badge variant="klass-b">B13</Badge>
			<Badge variant="klass-a">A18</Badge>
			<Badge variant="klass-a">A20</Badge>
		</div>

		<p class="text-label uppercase text-on-surface-subtle">Sizes</p>
		<div class="flex items-center gap-2">
			<Badge variant="klass-b" size="sm">B11 sm</Badge>
			<Badge variant="klass-b" size="md">B11 md</Badge>
		</div>
	</section>

	<!-- ── Chip ─────────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Chip" />

		<p class="text-label uppercase text-on-surface-subtle">Set scores</p>
		<div class="flex flex-wrap gap-2">
			<Chip variant="win">11:8</Chip>
			<Chip variant="win">11:9</Chip>
			<Chip variant="loss">9:11</Chip>
			<Chip variant="win">12:10</Chip>
			<Chip variant="loss">8:11</Chip>
		</div>

		<p class="text-label uppercase text-on-surface-subtle">Match scores</p>
		<div class="flex gap-2">
			<Chip variant="win"  size="lg">3:1</Chip>
			<Chip variant="loss" size="lg">1:3</Chip>
			<Chip variant="neutral" size="lg">5:5</Chip>
		</div>

		<p class="text-label uppercase text-on-surface-subtle">Sizes</p>
		<div class="flex items-center gap-2">
			<Chip variant="win" size="sm">sm</Chip>
			<Chip variant="win" size="md">md</Chip>
			<Chip variant="win" size="lg">lg</Chip>
		</div>
	</section>

	<!-- ── Avatar ───────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Avatar" />

		<p class="text-label uppercase text-on-surface-subtle">Sizes</p>
		<div class="flex items-center gap-3">
			<Avatar initials="TS" size="xs" />
			<Avatar initials="TS" size="sm" />
			<Avatar initials="TS" size="md" />
			<Avatar initials="TS" size="lg" />
		</div>

		<p class="text-label uppercase text-on-surface-subtle">Klassierung colors</p>
		<div class="flex items-center gap-3">
			<Avatar initials="EX" color="klass-e" size="md" />
			<Avatar initials="DY" color="klass-d" size="md" />
			<Avatar initials="CZ" color="klass-c" size="md" />
			<Avatar initials="BW" color="klass-b" size="md" />
			<Avatar initials="AV" color="klass-a" size="md" />
		</div>
	</section>

	<!-- ── Button ───────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Button" />

		<p class="text-label uppercase text-on-surface-subtle">Variants</p>
		<div class="flex flex-wrap gap-3">
			<Button variant="primary">Primary</Button>
			<Button variant="ghost">Ghost</Button>
			<Button variant="danger">Danger</Button>
		</div>

		<p class="text-label uppercase text-on-surface-subtle">Sizes</p>
		<div class="flex flex-wrap items-center gap-3">
			<Button variant="ghost" size="sm">Small</Button>
			<Button variant="ghost" size="md">Medium</Button>
			<Button variant="ghost" size="lg">Large</Button>
		</div>

		<p class="text-label uppercase text-on-surface-subtle">Disabled</p>
		<div class="flex gap-3">
			<Button variant="primary" disabled>Disabled</Button>
			<Button variant="ghost"   disabled>Disabled</Button>
		</div>

		<p class="text-label uppercase text-on-surface-subtle">With icon</p>
		<div class="flex gap-3">
			<Button variant="ghost">
				<Icon name="search" size={16} />
				Search
			</Button>
			<Button variant="ghost">
				View all
				<Icon name="chevron_right" size={16} />
			</Button>
		</div>
	</section>

	<!-- ── Icon ─────────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Icon" />

		<div class="flex items-center gap-4">
			<Icon name="home"         size={20} />
			<Icon name="emoji_events" size={20} />
			<Icon name="search"       size={20} />
			<Icon name="notifications" size={20} />
			<Icon name="star"         size={20} />
			<Icon name="star"         size={20} fill />
			<Icon name="favorite"     size={20} />
			<Icon name="favorite"     size={20} fill />
			<Icon name="arrow_back"   size={20} />
			<Icon name="chevron_right" size={20} />
			<Icon name="expand_more"  size={20} />
		</div>

		<p class="text-label uppercase text-on-surface-subtle">Colored</p>
		<div class="flex items-center gap-4">
			<Icon name="star" size={20} fill color="var(--color-win)" />
			<Icon name="star" size={20} fill color="var(--color-klass-b)" />
			<Icon name="star" size={20} fill color="var(--color-klass-a)" />
		</div>
	</section>

	<!-- ── Input ────────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Input" />

		<Input placeholder="Search players..." icon="search" bind:value={searchValue} />
		<Input placeholder="No icon" />
		<Input placeholder="Disabled" disabled />

		{#if searchValue}
			<p class="text-caption text-on-surface-muted">Value: {searchValue}</p>
		{/if}
	</section>

	<!-- ── ProgressBar ──────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="ProgressBar" />

		<div class="space-y-3">
			<div class="space-y-1.5">
				<div class="flex justify-between">
					<span class="text-caption text-on-surface-muted">Win — 64%</span>
				</div>
				<ProgressBar value={64} variant="win" height="md" />
			</div>

			<div class="space-y-1.5">
				<span class="text-caption text-on-surface-muted">Loss — 31%</span>
				<ProgressBar value={31} variant="loss" height="md" />
			</div>

			<div class="space-y-1.5">
				<span class="text-caption text-on-surface-muted">Default — 50%</span>
				<ProgressBar value={50} variant="default" height="md" />
			</div>
		</div>

		<p class="text-label uppercase text-on-surface-subtle">Klassierung</p>
		<div class="space-y-2">
			<ProgressBar value={20} variant="klass-e" height="sm" />
			<ProgressBar value={35} variant="klass-d" height="sm" />
			<ProgressBar value={50} variant="klass-c" height="sm" />
			<ProgressBar value={77} variant="klass-b" height="sm" />
			<ProgressBar value={92} variant="klass-a" height="sm" />
		</div>

		<p class="text-label uppercase text-on-surface-subtle">Heights</p>
		<div class="space-y-2">
			<ProgressBar value={70} variant="win" height="xs" />
			<ProgressBar value={70} variant="win" height="sm" />
			<ProgressBar value={70} variant="win" height="md" />
		</div>
	</section>

	<!-- ── Stat ─────────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Stat" />

		<div class="grid grid-cols-3 gap-3">
			<Card>
				<Stat label="ELO Rating" value="1452" size="md" />
			</Card>
			<Card>
				<Stat label="Win rate"   value="64%"  variant="win"  size="md" />
			</Card>
			<Card>
				<Stat label="vs Higher"  value="31%"  variant="loss" size="md" />
			</Card>
		</div>

		<p class="text-label uppercase text-on-surface-subtle">Sizes</p>
		<div class="space-y-3">
			<Stat label="Small"  value="1452" size="sm" />
			<Stat label="Medium" value="1452" size="md" />
			<Stat label="Large"  value="1452" size="lg" />
		</div>
	</section>

	<!-- ── Card ─────────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Card" />

		<p class="text-label uppercase text-on-surface-subtle">Variants</p>
		<Card variant="sunken">
			<p class="text-caption text-on-surface-muted">sunken — bg-surface</p>
		</Card>
		<Card variant="default">
			<p class="text-caption text-on-surface-muted">default — bg-surface-card</p>
		</Card>
		<Card variant="elevated">
			<p class="text-caption text-on-surface-muted">elevated — bg-surface-elevated</p>
		</Card>

		<p class="text-label uppercase text-on-surface-subtle">Interactive (hover me)</p>
		<Card variant="default" onclick={() => {}}>
			<p class="text-caption text-on-surface-muted">Clickable card — hover brightens</p>
		</Card>
		<Card variant="default" href="/showcase">
			<p class="text-caption text-on-surface-muted">Link card — navigates on click</p>
		</Card>

		<p class="text-label uppercase text-on-surface-subtle">Padding variants</p>
		<div class="space-y-2">
			<Card padding="sm"><p class="text-caption text-on-surface-muted">padding sm</p></Card>
			<Card padding="md"><p class="text-caption text-on-surface-muted">padding md</p></Card>
			<Card padding="lg"><p class="text-caption text-on-surface-muted">padding lg</p></Card>
		</div>
	</section>

	<!-- ── Divider ───────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Divider" />

		<p class="text-label uppercase text-on-surface-subtle">Plain</p>
		<Divider />

		<p class="text-label uppercase text-on-surface-subtle">With label</p>
		<Divider label="Your leagues" />
		<Divider label="Browse regions" />
	</section>

	<!-- ── ListItem ─────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="ListItem" />

		<Card padding="none">
			<!-- Static -->
			<ListItem>
				{#snippet leading()}
					<Avatar initials="TS" color="klass-b" size="md" />
				{/snippet}
				<p class="text-body-lg text-on-surface">Tim Schär</p>
				<p class="text-caption text-on-surface-muted">TTC Muttenz</p>
				{#snippet trailing()}
					<Badge variant="klass-b">B11</Badge>
				{/snippet}
			</ListItem>

			<Divider />

			<!-- Interactive link -->
			<ListItem href="/showcase">
				{#snippet leading()}
					<Avatar initials="JH" color="klass-a" size="md" />
				{/snippet}
				<p class="text-body-lg text-on-surface">Janick Hänni</p>
				<p class="text-caption text-on-surface-muted">TTC Neuhausen</p>
				{#snippet trailing()}
					<div class="flex items-center gap-2">
						<Badge variant="klass-a">A20</Badge>
						<Icon name="chevron_right" size={18} />
					</div>
				{/snippet}
			</ListItem>

			<Divider />

			<!-- With rank number -->
			<ListItem href="/showcase">
				{#snippet leading()}
					<span class="text-caption text-on-surface-subtle w-5 text-right tabular-nums">03</span>
					<Avatar initials="DK" color="klass-a" size="md" />
				{/snippet}
				<p class="text-body-lg text-on-surface">Dominic König</p>
				<p class="text-caption text-on-surface-muted">TTC Young Stars</p>
				{#snippet trailing()}
					<div class="text-right">
						<Badge variant="klass-a">A19</Badge>
						<p class="text-caption text-on-surface-muted tabular-nums mt-0.5">2312</p>
					</div>
				{/snippet}
			</ListItem>
		</Card>
	</section>

	<!-- ── Tabs ─────────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Tabs" />

		<Tabs tabs={[
      { value: 'standings', label: 'Standings' },
      { value: 'results',   label: 'Results'   },
      { value: 'schedule',  label: 'Schedule'  },
    ]}>
			{#snippet children({ active })}
				<Card>
					<p class="text-caption text-on-surface-muted">
						Active tab: <span class="text-on-surface font-bold">{active}</span>
					</p>
				</Card>
			{/snippet}
		</Tabs>
	</section>

	<!-- ── Accordion ────────────────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Accordion" />

		<Accordion bind:open={accordionOpen}>
			{#snippet header({ open, toggle })}
				<button
					onclick={toggle}
					class="w-full flex items-center justify-between
                 bg-surface-card rounded-xl px-4 py-3
                 hover:bg-surface-elevated transition-colors"
				>
					<div>
						<p class="text-label uppercase text-on-surface-subtle">März 2026</p>
						<div class="flex items-center gap-2 mt-0.5">
							<span class="text-body-lg font-bold text-on-surface">3 matches</span>
							<Badge variant="win">+2 ELO</Badge>
						</div>
					</div>
					<Icon
						name={open ? 'expand_more' : 'chevron_right'}
						size={20}
					/>
				</button>
			{/snippet}

			<div class="mt-2 space-y-2">
				<Card>
					<p class="text-caption text-on-surface-muted">Match card 1 would go here</p>
				</Card>
				<Card>
					<p class="text-caption text-on-surface-muted">Match card 2 would go here</p>
				</Card>
				<Card>
					<p class="text-caption text-on-surface-muted">Match card 3 would go here</p>
				</Card>
			</div>
		</Accordion>
	</section>

	<!-- ── Composition example ──────────────────────────────── -->
	<section class="space-y-3">
		<Divider label="Composition example" />
		<p class="text-caption text-on-surface-muted">
			A realistic match card assembled from primitives
		</p>

		<Card>
			<!-- Meta -->
			<p class="text-label uppercase text-on-surface-subtle mb-3">
				14. März · NWTTV 1. Liga
			</p>

			<!-- Opponent -->
			<div class="flex items-center gap-2 mb-3">
				<Avatar initials="JH" color="klass-b" size="md" />
				<span class="text-body-lg text-on-surface">Janick Hänni</span>
				<Badge variant="klass-b">B12</Badge>
			</div>

			<!-- Set chips -->
			<div class="flex gap-2 mb-3">
				<Chip variant="win">11:8</Chip>
				<Chip variant="win">11:9</Chip>
				<Chip variant="loss">9:11</Chip>
				<Chip variant="win">12:10</Chip>
			</div>

			<Divider />

			<!-- Footer -->
			<div class="flex items-center justify-between mt-3">
				<Chip variant="win" size="lg">3:1</Chip>
				<div class="flex items-center gap-1 text-win">
					<Icon name="arrow_upward" size={14} color="var(--color-win)" />
					<span class="text-caption font-bold">+7 ELO</span>
				</div>
			</div>
		</Card>
	</section>

	<div class="h-8"></div>
</div>