import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Clock, Lock, User, Eye, EyeOff, ArrowRight, Server, Sparkles } from 'lucide-react';
import toast from 'react-hot-toast';


export function LoginPage() {
  const [usuario, setUsuario] = useState('');
  const [senha, setSenha] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [backendStatus, setBackendStatus] = useState('checking'); // 'online' | 'offline' | 'checking'

  const { login, isAuthenticated, getDashboardPath } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Se já estiver logado, redireciona
  useEffect(() => {
    if (isAuthenticated) {
      const from = location.state?.from?.pathname || getDashboardPath();
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, navigate, location, getDashboardPath]);

  // Checagem rápida de conectividade com o backend
  useEffect(() => {
    const checkBackend = async () => {
      try {
        const res = await fetch('http://localhost:8080/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ usuario: '', senha: '' }),
        });
        // 400 Bad Request ou 401 significa que o Spring Boot está respondendo
        if (res.status === 400 || res.status === 401 || res.status === 405 || res.ok) {
          setBackendStatus('online');
        } else {
          setBackendStatus('online');
        }
      } catch {
        setBackendStatus('offline');
      }
    };
    checkBackend();
  }, []);


  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!usuario.trim() || !senha.trim()) {
      toast.error('Preencha o usuário e a senha');
      return;
    }

    try {
      setLoading(true);
      const data = await login(usuario.trim(), senha.trim());
      toast.success(`Login realizado com sucesso! Perfil: ${data.perfil}`);

      const destination = location.state?.from?.pathname || getDashboardPath(data.perfil);
      navigate(destination, { replace: true });
    } catch (err) {
      toast.error(err.message || 'Falha ao autenticar. Verifique suas credenciais.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickFill = (u, s) => {
    setUsuario(u);
    setSenha(s);
    toast('Credenciais preenchidas!', { icon: '🔑' });
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      {/* Background glow accents */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-indigo-600/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-10 right-10 w-80 h-80 bg-purple-600/10 rounded-full blur-3xl pointer-events-none" />

      <div className="sm:mx-auto sm:w-full sm:max-w-md relative z-10">
        {/* Brand Header */}
        <div className="text-center">
          <div className="inline-flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-violet-500 text-white shadow-xl shadow-indigo-500/25 mb-4">
            <Clock className="h-8 w-8" />
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-white">
            PontoCerto
          </h1>
          <p className="mt-2 text-sm text-slate-400">
            Sistema Integrado de Gestão de Ponto Eletrônico
          </p>
        </div>

        {/* Backend Connectivity Status Badge */}
        <div className="mt-4 flex justify-center">
          {backendStatus === 'checking' && (
            <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-900/80 px-3 py-1 text-xs text-slate-400 border border-slate-800">
              <Server size={12} className="animate-spin text-slate-500" /> Verificando backend...
            </span>
          )}
          {backendStatus === 'online' && (
            <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-950/60 px-3 py-1 text-xs font-medium text-emerald-400 border border-emerald-800/40">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
              </span>
              Backend Conectado (Porta 8080)
            </span>
          )}
          {backendStatus === 'offline' && (
            <span className="inline-flex items-center gap-1.5 rounded-full bg-red-950/60 px-3 py-1 text-xs font-medium text-red-400 border border-red-800/40">
              <span className="h-2 w-2 rounded-full bg-red-500"></span>
              Backend Offline (Porta 8080 não respondeu)
            </span>
          )}
        </div>

        {/* Login Form Card */}
        <div className="mt-6 sm:mx-auto sm:w-full sm:max-w-md">
          <div className="bg-slate-900/70 backdrop-blur-2xl border border-slate-800 py-8 px-6 shadow-2xl rounded-3xl sm:px-10">
            <form className="space-y-5" onSubmit={handleSubmit}>
              {/* Usuario Field */}
              <div>
                <label
                  htmlFor="usuario"
                  className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5"
                >
                  Usuário de Acesso
                </label>
                <div className="relative rounded-xl shadow-sm">
                  <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400">
                    <User size={18} />
                  </div>
                  <input
                    id="usuario"
                    name="usuario"
                    type="text"
                    autoComplete="username"
                    required
                    value={usuario}
                    onChange={(e) => setUsuario(e.target.value)}
                    placeholder="Ex: admin.rh"
                    className="block w-full rounded-xl border border-slate-700/80 bg-slate-950/60 py-3 pl-10 pr-4 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 focus:outline-none transition-all"
                  />
                </div>
              </div>

              {/* Senha Field */}
              <div>
                <label
                  htmlFor="senha"
                  className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5"
                >
                  Senha
                </label>
                <div className="relative rounded-xl shadow-sm">
                  <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400">
                    <Lock size={18} />
                  </div>
                  <input
                    id="senha"
                    name="senha"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    required
                    value={senha}
                    onChange={(e) => setSenha(e.target.value)}
                    placeholder="••••••••"
                    className="block w-full rounded-xl border border-slate-700/80 bg-slate-950/60 py-3 pl-10 pr-11 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 focus:outline-none transition-all"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 flex items-center pr-3.5 text-slate-400 hover:text-slate-200 cursor-pointer focus:outline-none"
                    title={showPassword ? 'Ocultar senha' : 'Exibir senha'}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              {/* Submit Button */}
              <div>
                <button
                  type="submit"
                  disabled={loading}
                  className="group relative flex w-full justify-center items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 via-indigo-500 to-violet-600 px-4 py-3.5 text-sm font-semibold text-white shadow-lg shadow-indigo-600/30 hover:from-indigo-500 hover:to-violet-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-slate-900 disabled:opacity-50 disabled:cursor-not-allowed transition-all cursor-pointer"
                >
                  {loading ? (
                    <>
                      <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                      <span>Autenticando...</span>
                    </>
                  ) : (
                    <>
                      <span>Entrar no Sistema</span>
                      <ArrowRight size={16} className="group-hover:translate-x-1 transition-transform" />
                    </>
                  )}
                </button>
              </div>
            </form>

            {/* Quick Test / Seed Helpers */}
            <div className="mt-8 pt-6 border-t border-slate-800">
              <div className="flex items-center gap-2 mb-3">
                <Sparkles size={16} className="text-amber-400" />
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                  Teste Rápido com o Backend
                </span>
              </div>
              <p className="text-xs text-slate-400 mb-3">
                O backend Spring Boot possui contas de teste prontas para validação de cada perfil:
              </p>

              <div className="space-y-2">
                {/* Botão RH */}
                <button
                  type="button"
                  onClick={() => handleQuickFill('admin.rh', 'admin123')}
                  className="w-full text-left rounded-xl bg-slate-950/80 hover:bg-slate-800/80 border border-slate-800 hover:border-purple-500/50 p-2.5 transition-all cursor-pointer group"
                >

                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-purple-500/20 text-purple-400 text-xs font-bold">
                        RH
                      </span>
                      <div>
                        <div className="text-xs font-semibold text-slate-200 group-hover:text-purple-300 transition-colors">
                          Administrador RH (admin.rh)
                        </div>
                        <div className="text-[11px] text-slate-500 font-mono">
                          Senha: admin123
                        </div>
                      </div>
                    </div>
                    <span className="text-[11px] text-purple-400 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                      Preencher →
                    </span>
                  </div>
                </button>

                {/* Botão Colaborador */}
                <button
                  type="button"
                  onClick={() => handleQuickFill('colaborador.teste', 'user123')}
                  className="w-full text-left rounded-xl bg-slate-950/80 hover:bg-slate-800/80 border border-slate-800 hover:border-emerald-500/50 p-2.5 transition-all cursor-pointer group"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-500/20 text-emerald-400 text-xs font-bold">
                        COL
                      </span>
                      <div>
                        <div className="text-xs font-semibold text-slate-200 group-hover:text-emerald-300 transition-colors">
                          Colaborador (colaborador.teste)
                        </div>
                        <div className="text-[11px] text-slate-500 font-mono">
                          Senha: user123
                        </div>
                      </div>
                    </div>
                    <span className="text-[11px] text-emerald-400 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                      Preencher →
                    </span>
                  </div>
                </button>

                {/* Botão Gestor */}
                <button
                  type="button"
                  onClick={() => handleQuickFill('gestor.teste', 'gestor123')}
                  className="w-full text-left rounded-xl bg-slate-950/80 hover:bg-slate-800/80 border border-slate-800 hover:border-blue-500/50 p-2.5 transition-all cursor-pointer group"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-500/20 text-blue-400 text-xs font-bold">
                        GES
                      </span>
                      <div>
                        <div className="text-xs font-semibold text-slate-200 group-hover:text-blue-300 transition-colors">
                          Gestor de Equipe (gestor.teste)
                        </div>
                        <div className="text-[11px] text-slate-500 font-mono">
                          Senha: gestor123
                        </div>
                      </div>
                    </div>
                    <span className="text-[11px] text-blue-400 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                      Preencher →
                    </span>
                  </div>
                </button>
              </div>

              <div className="mt-3 rounded-xl bg-indigo-950/30 border border-indigo-900/40 p-3 text-[11px] text-indigo-300/80">
                💡 <span className="font-semibold text-indigo-200">Dica:</span> Você pode usar as contas de teste pré-configuradas acima ou cadastrar novos funcionários personalizados diretamente no painel do <strong>RH</strong>!
              </div>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
