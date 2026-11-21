import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LogOut, Menu, X } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { useTheme } from '../context/ThemeContext';
import BrandLogo from '../components/ui/BrandLogo';
import ThemeToggle from '../components/ui/ThemeToggle';

/**
 * Clean B2B AppShell:
 * - Subtle distinct sidebar tone with crisp 1px border & delicate section separators
 * - Light/Dark + Per-Tenant White-Label aware
 */
export function AppShell({
  brandName,
  brandSubtitle,
  brandLogoUrl,
  navItems = [],
  activeKey,
  onSelectNav,
  showBrandPicker = false,
  children,
}) {
  const { user, logout } = useAuth();
  const { tenantBranding } = useTheme();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const displayName = brandName || tenantBranding?.nome || 'Ponto Certo';
  const displaySub =
    brandSubtitle ||
    (user?.subdominio ? `${user.subdominio}.pontocerto` : tenantBranding?.subtitulo);
  const displayLogo = brandLogoUrl !== undefined ? brandLogoUrl : tenantBranding?.logoUrl;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const renderNav = (closeMobile = false) => (
    <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-1">
      {navItems.map((item, idx) => {
        if (item.type === 'divider') {
          return (
            <div
              key={item.key}
              className={`${
                idx > 0 ? 'mt-4 pt-4 border-t border-[var(--border-subtle)]/80' : ''
              } pb-1.5 px-2.5 text-[10px] font-semibold uppercase tracking-widest text-[var(--text-muted)]`}
            >
              {item.label}
            </div>
          );
        }
        const Icon = item.icon;
        const active = item.key === activeKey;
        return (
          <button
            key={item.key}
            type="button"
            onClick={() => {
              onSelectNav?.(item.key);
              if (closeMobile) setMobileOpen(false);
            }}
            className={`w-full flex items-center justify-between gap-2.5 px-3 py-2 rounded-lg text-[13px] font-medium transition-all duration-200 cursor-pointer border ${
              active
                ? 'border-transparent text-[var(--brand-fg)] shadow-xs'
                : 'border-transparent text-[var(--text-secondary)] hover:bg-[var(--surface-card)] hover:border-[var(--border-subtle)] hover:text-[var(--text-primary)] hover:translate-x-0.5'
            }`}
            style={active ? { backgroundColor: 'var(--brand-600)' } : undefined}
          >
            <span className="inline-flex items-center gap-2.5 truncate">
              {Icon && <Icon size={16} className="shrink-0 opacity-90" />}
              <span className="truncate">{item.label}</span>
            </span>
            {item.badge != null && (
              <span
                className={`text-[11px] font-mono-tabular px-1.5 py-0.5 rounded-md ${
                  active
                    ? 'bg-white/20 text-white'
                    : 'bg-[var(--surface-card)] border border-[var(--border-subtle)] text-[var(--text-secondary)]'
                }`}
              >
                {item.badge}
              </span>
            )}
          </button>
        );
      })}
    </nav>
  );

  return (
    <div className="min-h-screen flex bg-[var(--surface-page)] text-[var(--text-primary)]">
      {/* Desktop Sidebar — distinct subtle surface separated by delicate border */}
      <aside className="hidden lg:flex lg:flex-col lg:w-64 lg:shrink-0 border-r border-[var(--border-subtle)] bg-[var(--surface-sidebar)] sticky top-0 h-screen">
        <div className="h-16 flex items-center px-4 border-b border-[var(--border-subtle)] bg-[var(--surface-card)]">
          <div className="min-w-0">
            <BrandLogo name={displayName} subtitle={displaySub} logoUrl={displayLogo} size="sm" />
          </div>
        </div>

        {renderNav(false)}

        <div className="p-3 border-t border-[var(--border-subtle)] bg-[var(--surface-card)]/60">
          <div className="flex items-center justify-between gap-2 px-2 py-1.5">
            <div className="min-w-0">
              <div className="text-xs font-semibold text-[var(--text-primary)] truncate">
                {user?.usuario}
              </div>
              <div className="text-[11px] text-[var(--text-muted)] truncate">{user?.perfil}</div>
            </div>
            <button
              type="button"
              onClick={handleLogout}
              className="h-8 w-8 inline-flex items-center justify-center rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] text-[var(--text-secondary)] hover:text-red-600 dark:hover:text-red-400 hover:border-red-200 dark:hover:border-red-900 transition-colors cursor-pointer"
              title="Sair da conta"
              aria-label="Sair da conta"
            >
              <LogOut size={14} />
            </button>
          </div>
        </div>
      </aside>

      {/* Mobile Drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden flex">
          <div className="fixed inset-0 bg-slate-900/50" onClick={() => setMobileOpen(false)} />
          <div className="relative z-10 w-72 max-w-[85vw] flex flex-col bg-[var(--surface-sidebar)] border-r border-[var(--border-subtle)] h-full shadow-xl">
            <div className="h-16 flex items-center justify-between px-4 border-b border-[var(--border-subtle)] bg-[var(--surface-card)]">
              <BrandLogo name={displayName} subtitle={displaySub} logoUrl={displayLogo} size="sm" />
              <button
                type="button"
                onClick={() => setMobileOpen(false)}
                className="h-8 w-8 inline-flex items-center justify-center rounded-lg text-[var(--text-secondary)] hover:bg-[var(--surface-hover)] cursor-pointer"
                aria-label="Fechar menu"
              >
                <X size={18} />
              </button>
            </div>
            {renderNav(true)}
          </div>
        </div>
      )}

      {/* Main Column */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-16 sticky top-0 z-30 border-b border-[var(--border-subtle)] bg-[var(--surface-card)]/95 backdrop-blur-xs px-4 sm:px-6 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 min-w-0">
            <button
              type="button"
              onClick={() => setMobileOpen(true)}
              className="lg:hidden h-9 w-9 inline-flex items-center justify-center rounded-lg border border-[var(--border-subtle)] text-[var(--text-secondary)] hover:bg-[var(--surface-hover)] cursor-pointer"
              aria-label="Abrir menu"
            >
              <Menu size={18} />
            </button>
            <div className="lg:hidden min-w-0">
              <BrandLogo name={displayName} logoUrl={displayLogo} size="sm" />
            </div>
            <div className="hidden lg:flex items-center gap-2 text-xs text-[var(--text-muted)]">
              <span
                className="h-2 w-2 rounded-full"
                style={{ backgroundColor: 'var(--brand-600)' }}
              />
              <span className="font-medium text-[var(--text-secondary)]">{displayName}</span>
              <span>/</span>
              <span className="text-[var(--text-primary)] font-medium">
                {navItems.find((n) => n.key === activeKey)?.label || 'Painel'}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2.5">
            <ThemeToggle showBrandPicker={showBrandPicker} />
            <button
              type="button"
              onClick={handleLogout}
              className="inline-flex items-center gap-1.5 h-9 px-3 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] hover:bg-[var(--surface-hover)] text-xs font-medium text-[var(--text-secondary)] hover:text-red-600 dark:hover:text-red-400 transition-colors cursor-pointer"
              title="Sair"
            >
              <LogOut size={15} />
              <span className="hidden sm:inline">Sair</span>
            </button>
          </div>
        </header>

        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          <div key={activeKey} className="animate-panelEnter">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}

export default AppShell;
