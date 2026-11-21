import React, { useState, useRef, useEffect } from 'react';
import { Sun, Moon, Palette, Check } from 'lucide-react';
import { useTheme, BRAND_PRESETS } from '../../context/ThemeContext';

export function ThemeToggle({ showBrandPicker = false }) {
  const { isDark, toggleColorScheme, brandPreset, setBrandPreset } = useTheme();
  const [openPalette, setOpenPalette] = useState(false);
  const popRef = useRef(null);

  useEffect(() => {
    if (!openPalette) return;
    const onClickOutside = (e) => {
      if (popRef.current && !popRef.current.contains(e.target)) {
        setOpenPalette(false);
      }
    };
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, [openPalette]);

  return (
    <div className="inline-flex items-center gap-1.5 relative" ref={popRef}>
      {showBrandPicker && (
        <>
          <button
            type="button"
            onClick={() => setOpenPalette((v) => !v)}
            className="inline-flex items-center gap-1.5 h-9 px-2.5 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] hover:bg-[var(--surface-hover)] text-[var(--text-secondary)] text-xs font-medium transition-colors cursor-pointer"
            title="Personalizar cor da marca (White-label)"
            aria-label="Personalizar cor da marca"
          >
            <span
              className="h-3.5 w-3.5 rounded-full border border-black/15"
              style={{ backgroundColor: BRAND_PRESETS[brandPreset]?.swatch || '#2563eb' }}
            />
            <Palette size={14} />
          </button>

          {openPalette && (
            <div className="absolute right-0 top-11 z-50 w-56 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] shadow-lg p-2.5 animate-fadeIn">
              <div className="px-2 py-1 text-[11px] font-semibold uppercase tracking-wider text-[var(--text-muted)]">
                Cor da marca (White-label)
              </div>
              <div className="mt-1 space-y-0.5">
                {Object.values(BRAND_PRESETS).map((preset) => {
                  const active = preset.id === brandPreset;
                  return (
                    <button
                      key={preset.id}
                      type="button"
                      onClick={() => {
                        setBrandPreset(preset.id);
                        setOpenPalette(false);
                      }}
                      className="w-full flex items-center justify-between px-2 py-1.5 rounded-lg text-xs font-medium text-[var(--text-primary)] hover:bg-[var(--surface-hover)] transition-colors cursor-pointer"
                    >
                      <span className="inline-flex items-center gap-2">
                        <span
                          className="h-3.5 w-3.5 rounded-full border border-black/15"
                          style={{ backgroundColor: preset.swatch }}
                        />
                        {preset.label}
                      </span>
                      {active && <Check size={14} style={{ color: 'var(--brand-600)' }} />}
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </>
      )}

      <button
        type="button"
        onClick={toggleColorScheme}
        className="inline-flex items-center justify-center h-9 w-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] hover:bg-[var(--surface-hover)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors cursor-pointer"
        aria-label={isDark ? 'Alternar para tema claro' : 'Alternar para tema escuro'}
        title={isDark ? 'Tema claro' : 'Tema escuro'}
      >
        {isDark ? <Sun size={16} /> : <Moon size={16} />}
      </button>
    </div>
  );
}

export default ThemeToggle;
