<script lang="ts">
	import type { Snippet } from 'svelte';

	type Variant = 'default' | 'win' | 'loss' | 'neutral';

	type Props = {
		variant?: Variant;
		size?: 'sm' | 'md' | 'lg';
		class?: string;
		children: Snippet;
	};

	let { variant = 'default', size = 'md', class: className = '', children }: Props = $props();

	// win/loss chips use the vivid color as bg (e.g. green pill with dark text)
	// unlike Badge which uses subtle bg + vivid text
	const variantClasses: Record<Variant, string> = {
		default: 'bg-surface-elevated text-on-surface',
		win:     'bg-win             text-win-subtle',
		loss:    'bg-loss            text-loss-subtle',
		neutral: 'bg-surface-elevated text-on-surface-muted',
	};

	const sizeClasses: Record<string, string> = {
		sm: 'px-2   py-1   text-[10px] rounded-sm',
		md: 'px-2.5 py-1.5 text-xs     rounded-md',
		lg: 'px-3   py-2   text-sm     rounded-md',
	};
</script>

<span
	class="inline-flex items-center justify-center font-bold tabular-nums leading-none
         {variantClasses[variant]} {sizeClasses[size]} {className}"
>
  {@render children()}
</span>