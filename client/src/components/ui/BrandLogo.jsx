import React from 'react';

export function BrandLogo({
  name = 'Ponto Certo',
  subtitle = null,
  logoUrl = null,
  size = 'md',
}) {
  const iconBox = size === 'sm' ? 'h-8 w-8 rounded-lg' : size === 'lg' ? 'h-11 w-11 rounded-xl' : 'h-9 w-9 rounded-lg';
  const titleCls = size === 'sm' ? 'text-sm' : size === 'lg' ? 'text-xl' : 'text-base';

  return (
    <div className="inline-flex items-center gap-2.5 select-none">
      {logoUrl ? (
        <img
          src={logoUrl}
          alt={name}
          className={`${iconBox} object-contain border border-[var(--border-subtle)] bg-white p-1`}
        />
      ) : (
        <span
          className={`${iconBox} flex items-center justify-center shrink-0`}
          style={{ backgroundColor: 'var(--brand-600)', color: 'var(--brand-fg)' }}
          aria-hidden="true"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 7v5l3 2" />
            <path d="M17.5 4.5l2 2" />
          </svg>
        </span>
      )}
      <div className="leading-tight min-w-0">
        <div className={`${titleCls} font-semibold tracking-tight text-[var(--text-primary)] truncate`}>
          {name}
        </div>
        {subtitle && (
          <div className="text-[11px] font-medium text-[var(--text-muted)] truncate">
            {subtitle}
          </div>
        )}
      </div>
    </div>
  );
}

export default BrandLogo;
