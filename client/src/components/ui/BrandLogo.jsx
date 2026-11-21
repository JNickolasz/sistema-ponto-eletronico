import React, { useEffect, useState } from 'react';

function isUsableLogoUrl(url) {
  if (!url || typeof url !== 'string') return false;
  const trimmed = url.trim();
  if (!trimmed) return false;
  // Ignora URLs fictícias de placeholder antigo (ex: https://exemplo.com/logo.png)
  if (/exemplo\.com|example\.com/i.test(trimmed)) return false;
  return true;
}

export function BrandLogo({
  name = 'Ponto Certo',
  subtitle = null,
  logoUrl = null,
  size = 'md',
}) {
  const [imgFailed, setImgFailed] = useState(false);

  useEffect(() => {
    setImgFailed(false);
  }, [logoUrl]);

  const showCustomImage = isUsableLogoUrl(logoUrl) && !imgFailed;

  const iconBox =
    size === 'sm'
      ? 'h-10 w-10 rounded-xl'
      : size === 'lg'
        ? 'h-14 w-14 rounded-2xl'
        : 'h-11 w-11 rounded-xl';
  const svgSize = size === 'sm' ? 22 : size === 'lg' ? 28 : 24;
  const titleCls =
    size === 'sm'
      ? 'text-[15px] leading-tight line-clamp-2'
      : size === 'lg'
        ? 'text-2xl leading-tight'
        : 'text-lg leading-tight';
  const subCls = size === 'lg' ? 'text-sm' : 'text-xs';

  return (
    <div className="flex items-center gap-2.5 select-none min-w-0 max-w-full">
      {showCustomImage ? (
        <img
          src={logoUrl}
          alt={name}
          onError={() => setImgFailed(true)}
          className={`${iconBox} object-contain shrink-0 shadow-xs`}
        />
      ) : (
        <span
          className={`${iconBox} flex items-center justify-center shrink-0 shadow-xs`}
          style={{ backgroundColor: 'var(--brand-600)', color: 'var(--brand-fg)' }}
          aria-hidden="true"
        >
          <svg
            width={svgSize}
            height={svgSize}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.25"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="12" cy="12" r="9" />
            <path d="M12 7v5l3 2" />
            <path d="M17.5 4.5l2 2" />
          </svg>
        </span>
      )}
      <div className="min-w-0 flex-1 overflow-hidden">
        <div
          className={`${titleCls} font-bold tracking-tight text-[var(--text-primary)] break-words`}
          style={{ fontFamily: 'var(--font-display)' }}
          title={name}
        >
          {name}
        </div>
        {subtitle && (
          <div className={`${subCls} font-medium text-[var(--text-secondary)] truncate mt-0.5`}>
            {subtitle}
          </div>
        )}
      </div>
    </div>
  );
}

export default BrandLogo;
