import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  Building2,
  Eye,
  EyeOff,
  Loader2,
  ShieldCheck,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { useAuth } from '../hooks/useAuth';
import { useTheme } from '../context/ThemeContext';
import { API_BASE } from '../services/api';
import { buildRootUrl, buildTenantUrl } from '../utils/tenantHost';
import BrandLogo from '../components/ui/BrandLogo';
import ThemeToggle from '../components/ui/ThemeToggle';
import { Button, Card } from '../components/ui/Primitives';

/**
 * Login estilo 1Doc:
 * 1) Em subdomínio de empresa (ex.: http://vertice.localhost:5173/login):
 *    Exibe logo + razão social + cor da empresa vindos do banco e pede APENAS Usuário e Senha.
 * 2) No domínio raiz (ex.: http://localhost:5173/login):
 *    Oferece duas abas claras:
 *    - "Sou Empresa (Cliente)": informa o subdomínio e redireciona para o portal dedicado da empresa.
 *    - "Admin da Plataforma": informa APENAS a chave mestra de administrador (sem subdomínio!).
 */
export default function LoginPage() {
  const { hostSubdomain, tenantBranding } = useTheme();
  const { login, isAuthenticated, getDashboardPath } = useAuth();
  const navigate = useNavigate();

  // Estado para domínio raiz
  const [abaRaiz, setAbaRaiz] = useState('empresa'); // 'empresa' | 'admin'
  const [subdominioBusca, setSubdominioBusca] = useState('');
  const [verificandoSub, setVerificandoSub] = useState(false);

  // Estado de credenciais
  const [usuario, setUsuario] = useState('');
  const [senha, setSenha] = useState('');
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      navigate(getDashboardPath(), { replace: true });
    }
  }, [isAuthenticated, navigate, getDashboardPath]);

  const handleIrParaSubdominio = async (e) => {
    e.preventDefault();
    const clean = subdominioBusca.trim().toLowerCase().replace(/[^a-z0-9-]/g, '');
    if (!clean) {
      toast.error('Informe o identificador (subdomínio) da sua empresa.');
      return;
    }

    try {
      setVerificandoSub(true);
      const res = await fetch(`${API_BASE}/public/tenant/${encodeURIComponent(clean)}`);
      if (!res.ok) {
        toast.error(`Nenhuma empresa encontrada com o subdomínio "${clean}".`);
        return;
      }
      window.location.href = buildTenantUrl(clean, '/login');
    } catch {
      toast.error('Não foi possível conectar ao servidor para validar o subdomínio.');
    } finally {
      setVerificandoSub(false);
    }
  };

  const handleLoginAdmin = async (e) => {
    e.preventDefault();
    if (!senha.trim()) {
      toast.error('Informe a chave de administrador.');
      return;
    }

    try {
      setSubmitting(true);
      const data = await login('admin', senha.trim(), '');
      toast.success('Acesso administrativo autorizado.');
      navigate(getDashboardPath(data.perfil), { replace: true });
    } catch (err) {
      toast.error(err?.message || 'Chave de administrador inválida.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleLoginTenant = async (e) => {
    e.preventDefault();
    if (!usuario.trim() || !senha.trim()) {
      toast.error('Informe seu usuário e senha.');
      return;
    }

    try {
      setSubmitting(true);
      const data = await login(usuario.trim(), senha.trim(), hostSubdomain);
      toast.success(`Bem-vindo, ${data.usuario}`);
      navigate(getDashboardPath(data.perfil), { replace: true });
    } catch (err) {
      toast.error(err?.message || 'Usuário ou senha inválidos.');
    } finally {
      setSubmitting(false);
    }
  };

  // ==================== MODO SUBDOMÍNIO DA EMPRESA (ex: vertice.localhost:5173) ====================
  if (hostSubdomain) {
    return (
      <div className="min-h-screen flex flex-col bg-[var(--surface-page)] text-[var(--text-primary)]">
        <header className="h-16 px-4 sm:px-6 flex items-center justify-between max-w-5xl w-full mx-auto">
          <Link
            to="/"
            className="inline-flex items-center gap-2 text-xs font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors"
          >
            <ArrowLeft size={15} />
            Voltar ao Relógio de Ponto
          </Link>
          <div className="flex items-center gap-2">
            <ThemeToggle />
          </div>
        </header>

        <main className="flex-1 flex items-center justify-center px-4 py-10">
          {tenantBranding.loading ? (
            <div className="flex flex-col items-center gap-3 text-[var(--text-secondary)]">
              <Loader2 size={24} className="animate-spin" style={{ color: 'var(--brand-600)' }} />
              <span className="text-xs font-medium">Carregando ambiente da empresa...</span>
            </div>
          ) : tenantBranding.notFound ? (
            <Card className="w-full max-w-md p-8 text-center space-y-4">
              <div className="mx-auto h-12 w-12 rounded-xl bg-amber-50 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400 flex items-center justify-center">
                <AlertCircle size={24} />
              </div>
              <h1 className="text-lg font-semibold">Empresa não encontrada</h1>
              <p className="text-xs text-[var(--text-secondary)]">
                O endereço <span className="font-mono-tabular font-semibold">{hostSubdomain}</span> não
                corresponde a nenhuma empresa ativa na plataforma.
              </p>
              <a href={buildRootUrl('/login')}>
                <Button variant="primary" className="w-full mt-2">
                  Voltar ao portal principal
                </Button>
              </a>
            </Card>
          ) : (
            <div className="w-full max-w-md space-y-6">
              <div className="flex flex-col items-center text-center">
                <BrandLogo
                  name={tenantBranding.nome}
                  subtitle="Portal de Controle de Ponto"
                  logoUrl={tenantBranding.logoUrl}
                  size="lg"
                />
              </div>

              <Card className="p-6 sm:p-8 shadow-xs">
                <div className="mb-6">
                  <h1 className="text-lg font-semibold tracking-tight">Entrar na sua conta</h1>
                  <p className="mt-1 text-xs text-[var(--text-secondary)]">
                    Use seu usuário e senha corporativos cadastrados pelo RH.
                  </p>
                </div>

                <form onSubmit={handleLoginTenant} className="space-y-4">
                  <div className="space-y-1.5">
                    <label htmlFor="tenant-user" className="block text-xs font-medium">
                      Usuário
                    </label>
                    <input
                      id="tenant-user"
                      type="text"
                      autoComplete="username"
                      autoFocus
                      value={usuario}
                      onChange={(e) => setUsuario(e.target.value)}
                      disabled={submitting}
                      placeholder="seu.usuario"
                      className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label htmlFor="tenant-pass" className="block text-xs font-medium">
                      Senha
                    </label>
                    <div className="relative">
                      <input
                        id="tenant-pass"
                        type={mostrarSenha ? 'text' : 'password'}
                        autoComplete="current-password"
                        value={senha}
                        onChange={(e) => setSenha(e.target.value)}
                        disabled={submitting}
                        placeholder="••••••••"
                        className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] pl-3 pr-10 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                      />
                      <button
                        type="button"
                        onClick={() => setMostrarSenha((v) => !v)}
                        className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)] p-1 cursor-pointer"
                        aria-label={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
                      >
                        {mostrarSenha ? <EyeOff size={16} /> : <Eye size={16} />}
                      </button>
                    </div>
                  </div>

                  <Button type="submit" variant="primary" size="lg" className="w-full mt-2" disabled={submitting}>
                    {submitting ? (
                      <>
                        <Loader2 size={16} className="animate-spin" />
                        Autenticando...
                      </>
                    ) : (
                      'Entrar'
                    )}
                  </Button>
                </form>
              </Card>
            </div>
          )}
        </main>
      </div>
    );
  }

  // ==================== MODO DOMÍNIO RAIZ (localhost:5173/login) ====================
  return (
    <div className="min-h-screen flex flex-col bg-[var(--surface-page)] text-[var(--text-primary)]">
      <header className="h-16 px-4 sm:px-6 flex items-center justify-between max-w-6xl w-full mx-auto">
        <Link
          to="/"
          className="inline-flex items-center gap-2 text-xs font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors"
        >
          <ArrowLeft size={15} />
          Voltar à página inicial
        </Link>
        <div className="flex items-center gap-2">
          <ThemeToggle />
        </div>
      </header>

      <main className="flex-1 flex items-center justify-center px-4 py-10">
        <div className="w-full max-w-md space-y-6">
          <div className="flex flex-col items-center text-center">
            <BrandLogo name="Ponto Certo" subtitle="Acesso ao sistema" size="lg" />
          </div>

          <Card className="p-6 sm:p-8 shadow-xs space-y-6">
            {/* Seletor claro: Empresa Cliente vs Admin da Plataforma */}
            <div className="grid grid-cols-2 gap-1.5 p-1 rounded-xl bg-[var(--surface-subtle)] border border-[var(--border-subtle)]">
              <button
                type="button"
                onClick={() => {
                  setAbaRaiz('empresa');
                  setSenha('');
                }}
                className={`h-9 rounded-lg text-xs font-semibold inline-flex items-center justify-center gap-1.5 transition-colors cursor-pointer ${
                  abaRaiz === 'empresa'
                    ? 'bg-[var(--surface-card)] text-[var(--text-primary)] shadow-xs'
                    : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
                }`}
              >
                <Building2 size={14} />
                Sou Empresa
              </button>
              <button
                type="button"
                onClick={() => {
                  setAbaRaiz('admin');
                  setSenha('');
                }}
                className={`h-9 rounded-lg text-xs font-semibold inline-flex items-center justify-center gap-1.5 transition-colors cursor-pointer ${
                  abaRaiz === 'admin'
                    ? 'bg-[var(--surface-card)] text-[var(--text-primary)] shadow-xs'
                    : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
                }`}
              >
                <ShieldCheck size={14} />
                Admin do Sistema
              </button>
            </div>

            {abaRaiz === 'empresa' ? (
              <form onSubmit={handleIrParaSubdominio} className="space-y-4">
                <div>
                  <h1 className="text-base font-semibold tracking-tight">Encontrar sua empresa</h1>
                  <p className="mt-1 text-xs text-[var(--text-secondary)]">
                    Informe o endereço da sua empresa para ser direcionado ao portal exclusivo dela.
                  </p>
                </div>

                <div className="space-y-1.5">
                  <label htmlFor="subdominio-busca" className="block text-xs font-medium">
                    Subdomínio da empresa
                  </label>
                  <div className="flex items-center rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] overflow-hidden focus-within:ring-2 focus-within:ring-[var(--brand-600)]/25 focus-within:border-[var(--brand-600)]">
                    <input
                      id="subdominio-busca"
                      type="text"
                      autoFocus
                      value={subdominioBusca}
                      onChange={(e) => setSubdominioBusca(e.target.value)}
                      disabled={verificandoSub}
                      placeholder="suaempresa"
                      className="flex-1 h-10 px-3 text-sm bg-transparent text-[var(--text-primary)] placeholder:text-[var(--text-muted)] focus:outline-none font-mono-tabular"
                    />
                    <span className="px-3 h-10 inline-flex items-center bg-[var(--surface-subtle)] border-l border-[var(--border-subtle)] text-xs text-[var(--text-muted)] font-mono-tabular">
                      .pontocerto
                    </span>
                  </div>
                </div>

                <Button type="submit" variant="primary" size="lg" className="w-full" disabled={verificandoSub}>
                  {verificandoSub ? (
                    <>
                      <Loader2 size={16} className="animate-spin" />
                      Localizando portal...
                    </>
                  ) : (
                    <>
                      Continuar para o portal da empresa
                      <ArrowRight size={16} />
                    </>
                  )}
                </Button>
              </form>
            ) : (
              <form onSubmit={handleLoginAdmin} className="space-y-4">
                <div>
                  <h1 className="text-base font-semibold tracking-tight">Administração Global do SaaS</h1>
                  <p className="mt-1 text-xs text-[var(--text-secondary)]">
                    Acesso central a todas as empresas clientes, estatísticas globais, personalização white-label e feriados nacionais.
                  </p>
                </div>

                <div className="space-y-1.5">
                  <label htmlFor="admin-key" className="block text-xs font-medium">
                    Chave de Administrador
                  </label>
                  <div className="relative">
                    <input
                      id="admin-key"
                      type={mostrarSenha ? 'text' : 'password'}
                      autoFocus
                      value={senha}
                      onChange={(e) => setSenha(e.target.value)}
                      disabled={submitting}
                      placeholder="Informe sua X-Admin-Key"
                      className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] pl-3 pr-10 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                    />
                    <button
                      type="button"
                      onClick={() => setMostrarSenha((v) => !v)}
                      className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)] p-1 cursor-pointer"
                      aria-label={mostrarSenha ? 'Ocultar chave' : 'Mostrar chave'}
                    >
                      {mostrarSenha ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>

                <Button type="submit" variant="primary" size="lg" className="w-full" disabled={submitting}>
                  {submitting ? (
                    <>
                      <Loader2 size={16} className="animate-spin" />
                      Verificando chave...
                    </>
                  ) : (
                    'Entrar no Painel Admin'
                  )}
                </Button>
              </form>
            )}
          </Card>
        </div>
      </main>
    </div>
  );
}
