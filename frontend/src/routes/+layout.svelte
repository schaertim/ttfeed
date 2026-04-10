<script lang="ts">
	import '../app.css';

	const navItems = [
		{ href: '/',          label: 'Home',    icon: 'home'         },
		{ href: '/divisions', label: 'Leagues', icon: 'emoji_events' },
		{ href: '/players',   label: 'Search',  icon: 'search'       },
	];

	let { children } = $props();
</script>

<svelte:head>
	<link
		href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap"
		rel="stylesheet"
	/>
</svelte:head>

<!-- Top bar -->
<header class="fixed top-0 w-full z-50 h-16 bg-surface border-b border-border-base">
	<div class="flex justify-between items-center h-full px-4 max-w-2xl mx-auto">
		<a href="/" class="font-black text-xl tracking-tighter text-on-surface font-mono">
			TTF
		</a>
		<div class="flex items-center gap-3">
			<button class="text-on-surface-muted hover:text-on-surface transition-colors p-1">
				<span class="material-symbols-outlined" style="font-size:22px">notifications</span>
			</button>
			<div class="w-8 h-8 rounded-full bg-surface-elevated flex items-center justify-center">
				<span class="text-[10px] font-bold text-on-surface-muted">TS</span>
			</div>
		</div>
	</div>
</header>

<!-- Page content -->
<main class="pt-16 pb-20 px-4 max-w-2xl mx-auto">
	{@render children()}
</main>

<!-- Bottom nav -->
<nav class="fixed bottom-0 left-0 w-full z-50 h-16 bg-surface border-t border-border-base"
     style="backdrop-filter: blur(24px);">
	<div class="flex justify-around items-center h-full px-4 max-w-2xl mx-auto">
		{#each navItems as item}
			<a
				href={item.href}
				class="flex flex-col items-center justify-center gap-0.5 flex-1 pt-1
               text-on-surface-subtle hover:text-on-surface transition-colors"
			>
				<span class="material-symbols-outlined" style="font-size:22px">{item.icon}</span>
				<span class="text-label uppercase tracking-widest">{item.label}</span>
			</a>
		{/each}
	</div>
</nav>