import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { getHostSubdomain } from '../utils/tenantHost';
import { API_BASE } from '../services/api';

export const BRAND_PRESETS = {
  blue: {
    id: 'blue',
    label: 'Azul Ponto Certo',
    swatch: '#2563eb',
    secondary: '#1d4ed8',
  },
  teal: {
    id: 'teal',
    label: 'Petróleo / Vértice',
    swatch: '#0f766e',
    secondary: '#115e59',
  },
  emerald: {
    id: 'emerald',
    label: 'Esmeralda Corporativo',
    swatch: '#059669',
    secondary: '#047857',
  },
  indigo: {
    id: 'indigo',
    label: 'Índigo Executivo',
    swatch: '#4338ca',
    secondary: '#3730a3',
  },
  slate: {
    id: 'slate',
    label: 'Grafite Minimalista',
    swatch: '#18181b',
    secondary: '#09090b',
  },
  amber: {
    id: 'amber',
    label: 'Terracota / Indústria',
    swatch: '#b45309',
    secondary: '#92400e',
  },
};

function darkenHex(hex, amount = 0.16) {
  const clean = (hex || '#2563eb').replace('#', '');
  if (clean.length !== 6) return '#1d4ed8';
  const r = Math.max(0, Math.round(parseInt(clean.slice(0, 2), 16) * (1 - amount)));
  const g = Math.max(0, Math.round(parseInt(clean.slice(2, 4), 16) * (1 - amount)));
  const b = Math.max(0, Math.round(parseInt(clean.slice(4, 6), 16) * (1 - amount)));
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
}

function tintHex(hex, ratio = 0.9) {
  const clean = (hex || '#2563eb').replace('#', '');
  if (clean.length !== 6) return '#eff6ff';
  const r = Math.round(parseInt(clean.slice(0, 2), 16) + (255 - parseInt(clean.slice(0, 2), 16)) * ratio);
  const g = Math.round(parseInt(clean.slice(2, 4), 16) + (255 - parseInt(clean.slice(2, 4), 16)) * ratio);
  const b = Math.round(parseInt(clean.slice(4, 6), 16) + (255 - parseInt(clean.slice(4, 6), 16)) * ratio);
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
}

function applyBrandHex(primaryHex, secondaryHex = null) {
  const base = /^#[0-9a-fA-F]{6}$/.test(primaryHex || '') ? primaryHex : '#2563eb';
  const dark = /^#[0-9a-fA-F]{6}$/.test(secondaryHex || '') ? secondaryHex : darkenHex(base, 0.16);
  const root = document.documentElement;
  root.style.setProperty('--brand-50', tintHex(base, 0.92));
  root.style.setProperty('--brand-100', tintHex(base, 0.84));
  root.style.setProperty('--brand-200', tintHex(base, 0.70));
  root.style.setProperty('--brand-500', base);
  root.style.setProperty('--brand-600', base);
  root.style.setProperty('--brand-700', dark);
  root.style.setProperty('--brand-fg', '#ffffff');
}

const ThemeContext = createContext(null);

function getSystemScheme() {
  if (typeof window === 'undefined' || !window.matchMedia) return 'light';
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function readStoredScheme() {
  try {
    const val = localStorage.getItem('color-scheme');
    return val === 'light' || val === 'dark' ? val : null;
  } catch {
    return null;
  }
}

function applyMetaScheme(stored) {
  const meta = document.querySelector('meta[name="color-scheme"]');
  if (meta) {
    meta.content = stored || 'light dark';
  }
}

export function ThemeProvider({ children }) {
  const hostSubdomain = getHostSubdomain();
  const [storedScheme, setStoredScheme] = useState(readStoredScheme);
  const [systemScheme, setSystemScheme] = useState(getSystemScheme);
  const [brandPreset, setBrandPresetState] = useState('blue');
  const [tenantBranding, setTenantBranding] = useState({
    nome: 'Ponto Certo',
    subtitulo: null,
    subdominio: hostSubdomain,
    logoUrl: null,
    corPrimaria: '#2563eb',
    corSecundaria: '#1d4ed8',
    estacoes: [],
    loading: !!hostSubdomain,
    notFound: false,
  });

  const resolvedScheme = storedScheme || systemScheme;

  useEffect(() => {
    applyMetaScheme(storedScheme);
  }, [storedScheme]);

  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (e) => setSystemScheme(e.matches ? 'dark' : 'light');
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);

  useEffect(() => {
    const onStorage = (e) => {
      if (e.key === 'color-scheme') {
        const next = e.newValue === 'light' || e.newValue === 'dark' ? e.newValue : null;
        setStoredScheme(next);
      }
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  // Carrega branding real do banco quando acessado via subdomínio (ex.: vertice.localhost:5173)
  useEffect(() => {
    if (!hostSubdomain) {
      applyBrandHex('#2563eb', '#1d4ed8');
      document.title = 'Ponto Certo — Controle de Ponto Eletrônico';
      return;
    }

    let active = true;
    fetch(`${API_BASE}/public/tenant/${encodeURIComponent(hostSubdomain)}`)
      .then(async (res) => {
        if (!res.ok) throw new Error('Empresa não encontrada');
        return res.json();
      })
      .then((data) => {
        if (!active) return;
        const primary = data.corPrimaria || '#2563eb';
        const secondary = data.corSecundaria || darkenHex(primary, 0.16);
        applyBrandHex(primary, secondary);
        setTenantBranding({
          nome: data.razaoSocial || hostSubdomain,
          subtitulo: `${data.subdominio}.pontocerto`,
          subdominio: data.subdominio,
          logoUrl: data.logoUrl || null,
          corPrimaria: primary,
          corSecundaria: secondary,
          estacoes: Array.isArray(data.estacoes) ? data.estacoes : [],
          loading: false,
          notFound: false,
        });
        document.title = `${data.razaoSocial || hostSubdomain} — Ponto Eletrônico`;
      })
      .catch(() => {
        if (!active) return;
        applyBrandHex('#2563eb', '#1d4ed8');
        setTenantBranding({
          nome: hostSubdomain,
          subtitulo: null,
          subdominio: hostSubdomain,
          logoUrl: null,
          corPrimaria: '#2563eb',
          corSecundaria: '#1d4ed8',
          loading: false,
          notFound: true,
        });
      });

    return () => {
      active = false;
    };
  }, [hostSubdomain]);

  const toggleColorScheme = useCallback(() => {
    const currentRendered = storedScheme || getSystemScheme();
    const target = currentRendered === 'dark' ? 'light' : 'dark';
    const currentSys = getSystemScheme();

    if (target === currentSys) {
      setStoredScheme(null);
      try {
        localStorage.removeItem('color-scheme');
      } catch {}
    } else {
      setStoredScheme(target);
      try {
        localStorage.setItem('color-scheme', target);
      } catch {}
    }
  }, [storedScheme]);

  const setBrandPreset = useCallback((presetId) => {
    const preset = BRAND_PRESETS[presetId];
    if (!preset) return;
    setBrandPresetState(presetId);
    applyBrandHex(preset.swatch, preset.secondary);
  }, []);

  const setCustomBrandColor = useCallback((primaryHex, secondaryHex) => {
    applyBrandHex(primaryHex, secondaryHex);
  }, []);

  return (
    <ThemeContext.Provider
      value={{
        hostSubdomain,
        storedScheme,
        resolvedScheme,
        isDark: resolvedScheme === 'dark',
        toggleColorScheme,
        brandPreset,
        setBrandPreset,
        setCustomBrandColor,
        tenantBranding,
        setTenantBranding,
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useTheme deve ser usado dentro de ThemeProvider');
  }
  return ctx;
}
