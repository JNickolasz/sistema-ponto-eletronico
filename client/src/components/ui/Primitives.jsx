import React from 'react';

export function Button({
  children,
  variant = 'primary',
  size = 'md',
  className = '',
  disabled = false,
  type = 'button',
  ...props
}) {
  const sizes = {
    sm: 'min-h-9 px-3.5 py-1.5 text-xs rounded-lg gap-1.5',
    md: 'min-h-10 px-4 py-2 text-sm rounded-lg gap-2',
    lg: 'min-h-12 px-5 py-2.5 text-base rounded-xl gap-2',
  };

  const variants = {
    primary:
      'bg-[var(--brand-600)] hover:bg-[var(--brand-700)] text-[var(--brand-fg)] border border-transparent shadow-xs',
    secondary:
      'bg-[var(--surface-card)] hover:bg-[var(--surface-hover)] text-[var(--text-primary)] border border-[var(--border-subtle)]',
    ghost:
      'bg-transparent hover:bg-[var(--surface-hover)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-transparent',
    danger:
      'bg-red-600 hover:bg-red-700 text-white border border-transparent shadow-xs',
  };

  return (
    <button
      type={type}
      disabled={disabled}
      className={`inline-flex items-center justify-center font-semibold transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed ${sizes[size]} ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}

export function Card({ children, className = '', ...props }) {
  return (
    <div
      className={`rounded-2xl border border-[var(--border-subtle)] bg-[var(--surface-card)] ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

export function Badge({ children, tone = 'neutral', className = '' }) {
  const tones = {
    neutral: 'bg-[var(--surface-subtle)] text-[var(--text-secondary)] border-[var(--border-subtle)]',
    brand: 'bg-[var(--brand-50)] text-[var(--brand-700)] border-[var(--brand-200)] dark:bg-blue-950/50 dark:text-blue-300 dark:border-blue-800/60',
    success: 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/50 dark:text-emerald-300 dark:border-emerald-800/60',
    warning: 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/50 dark:text-amber-300 dark:border-amber-800/60',
    danger: 'bg-red-50 text-red-700 border-red-200 dark:bg-red-950/50 dark:text-red-300 dark:border-red-800/60',
  };

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-3 py-0.5 text-xs font-semibold ${tones[tone]} ${className}`}
    >
      {children}
    </span>
  );
}

export function PageHeader({ title, description, actions }) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-5 border-b border-[var(--border-subtle)]">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-[var(--text-primary)]">{title}</h1>
        {description && (
          <p className="mt-1 text-sm text-[var(--text-secondary)]">{description}</p>
        )}
      </div>
      {actions && <div className="flex items-center gap-2 shrink-0">{actions}</div>}
    </div>
  );
}

export function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-12 px-4">
      {Icon && (
        <div className="h-12 w-12 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-subtle)] flex items-center justify-center text-[var(--text-muted)] mb-3">
          <Icon size={22} />
        </div>
      )}
      <h3 className="text-base font-semibold text-[var(--text-primary)]">{title}</h3>
      {description && (
        <p className="mt-1 text-sm text-[var(--text-secondary)] max-w-sm">{description}</p>
      )}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function Input({ label, error, hint, className = '', ...props }) {
  return (
    <div className="space-y-1.5">
      {label && (
        <label className="block text-sm font-semibold text-[var(--text-primary)]">
          {label}
        </label>
      )}
      <input
        className={`w-full min-h-11 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3.5 text-base text-[var(--text-primary)] placeholder:text-[var(--text-muted)] focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)] transition-colors ${className}`}
        {...props}
      />
      {error && <p className="text-xs font-medium text-red-600 dark:text-red-400">{error}</p>}
      {!error && hint && <p className="text-xs text-[var(--text-muted)]">{hint}</p>}
    </div>
  );
}
