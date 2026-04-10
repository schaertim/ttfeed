<script lang="ts">
	type Variant = 'default' | 'win' | 'loss';

	type Props = {
		label: string;
		value: string;
		variant?: Variant;
		size?: 'sm' | 'md' | 'lg';
		class?: string;
	};

	let { label, value, variant = 'default', size = 'md', class: className = '' }: Props = $props();

	const valueClasses: Record<Variant, string> = {
		default: 'text-on-surface',
		win:     'text-win',
		loss:    'text-loss',
	};

	const sizeClasses: Record<string, string> = {
		sm: 'text-xl',
		md: 'text-3xl',
		lg: 'text-4xl',
	};
</script>

<div class={className}>
	<p class="text-label uppercase text-on-surface-subtle mb-1">
		{label}
	</p>
	<p class="font-black tabular-nums leading-none {sizeClasses[size]} {valueClasses[variant]}">
		{value}
	</p>
</div>