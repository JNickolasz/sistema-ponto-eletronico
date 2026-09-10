import React, { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import rhService from '../services/rhService';
import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import {
  ShieldCheck,
  UserPlus,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Sparkles,
  KeyRound,
  Briefcase,
  Terminal,
} from 'lucide-react';
import toast from 'react-hot-toast';

export function RhDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Estado do painel do backend
  const [painelData, setPainelData] = useState(null);
  const [loadingPainel, setLoadingPainel] = useState(true);
  const [painelError, setPainelError] = useState(null);

  // Formulário de cadastro de funcionário
  const [nomeCompleto, setNomeCompleto] = useState('');
  const [usuario, setUsuario] = useState('');
  const [senha, setSenha] = useState('');
  const [perfilAcesso, setPerfilAcesso] = useState('COLABORADOR');
  const [submitting, setSubmitting] = useState(false);

  // Histórico de cadastrados na sessão
  const [usuariosCadastrados, setUsuariosCadastrados] = useState(() => {
    try {
      const saved = localStorage.getItem('pontoCerto_usuarios_criados');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const recarregarPainel = async () => {
    try {
      setLoadingPainel(true);
      setPainelError(null);
      const data = await rhService.getPainel();
      setPainelData(data);
    } catch (err) {
      setPainelError(err.message || 'Erro ao carregar dados do painel do RH');
    } finally {
      setLoadingPainel(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    rhService
      .getPainel()
      .then((data) => {
        if (isMounted) setPainelData(data);
      })
      .catch((err) => {
        if (isMounted) setPainelError(err.message || 'Erro ao carregar dados do painel do RH');
      })
      .finally(() => {
        if (isMounted) setLoadingPainel(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);


  const handleCadastrar = async (e) => {
    e.preventDefault();

    if (!nomeCompleto.trim() || !usuario.trim() || !senha.trim()) {
      toast.error('Preencha todos os campos obrigatórios');
      return;
    }

    try {
      setSubmitting(true);
      const payload = {
        nomeCompleto: nomeCompleto.trim(),
        usuario: usuario.trim(),
        senha: senha.trim(),
        perfilAcesso,
      };

      const novo = await rhService.cadastrarFuncionario(payload);
      toast.success(`Funcionário criado com sucesso! ID: ${novo.id}`);

      const novaLista = [novo, ...usuariosCadastrados];
      setUsuariosCadastrados(novaLista);
      localStorage.setItem('pontoCerto_usuarios_criados', JSON.stringify(novaLista));

      // Limpar formulário
      setNomeCompleto('');
      setUsuario('');
      setSenha('');
    } catch (err) {
      toast.error(err.message || 'Erro ao cadastrar funcionário');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePreencherExemplo = (tipo) => {
    const timestamp = Math.floor(Math.random() * 899 + 100);
    if (tipo === 'COLABORADOR') {
      setNomeCompleto('João da Silva');
      setUsuario(`colaborador.${timestamp}`);
      setSenha('senha123');
      setPerfilAcesso('COLABORADOR');
    } else if (tipo === 'GESTOR') {
      setNomeCompleto('Mariana Costa');
      setUsuario(`gestor.${timestamp}`);
      setSenha('gestor123');
      setPerfilAcesso('GESTOR');
    }
    toast('Exemplo preenchido no formulário!', { icon: '📝' });
  };

  const handleTestarLoginNovo = (usr) => {
    logout();
    navigate('/login');
    toast(`Faça login com o usuário: ${usr.usuario}`, { icon: '🔑', duration: 6000 });
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Header do Painel */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-800/80 pb-6">
          <div>
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-400">
                <ShieldCheck size={24} />
              </div>
              <div>
                <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
                  Painel de Recursos Humanos (RH)
                </h1>
                <p className="text-sm text-slate-400 mt-0.5">
                  Gerenciamento de acessos corporativos e integração de novos colaboradores.
                </p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={recarregarPainel}
              disabled={loadingPainel}
              className="inline-flex items-center gap-2 rounded-xl bg-slate-900 border border-slate-700/80 px-4 py-2 text-xs font-semibold text-slate-200 hover:bg-slate-800 transition-all cursor-pointer disabled:opacity-50"
            >
              <RefreshCw size={14} className={loadingPainel ? 'animate-spin' : ''} />
              Recarregar Endpoint
            </button>
          </div>

        </div>

        {/* Backend Response Card (GET /api/rh/painel) */}
        <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-6 backdrop-blur-xl">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Terminal size={18} className="text-purple-400" />
              <h2 className="text-base font-semibold text-slate-100">
                Resposta do Endpoint do Backend: <span className="font-mono text-purple-300">GET /api/rh/painel</span>
              </h2>
            </div>
            {painelData && (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-medium text-emerald-400 border border-emerald-500/20">
                <CheckCircle2 size={12} /> 200 OK - Autorizado
              </span>
            )}
            {painelError && (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-red-500/10 px-2.5 py-0.5 text-xs font-medium text-red-400 border border-red-500/20">
                <AlertCircle size={12} /> Erro de Comunicação
              </span>
            )}
          </div>

          {loadingPainel ? (
            <div className="flex items-center gap-3 py-4 text-slate-400 text-sm">
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-purple-500 border-t-transparent" />
              <span>Consultando endpoint do backend...</span>
            </div>
          ) : painelError ? (
            <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-300 text-sm">
              {painelError}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="bg-slate-950/60 rounded-xl p-3.5 border border-slate-800">
                <span className="text-xs text-slate-400">Mensagem da API</span>
                <p className="text-sm font-semibold text-purple-300 mt-1">
                  {painelData?.mensagem || '—'}
                </p>
              </div>
              <div className="bg-slate-950/60 rounded-xl p-3.5 border border-slate-800">
                <span className="text-xs text-slate-400">Usuário Autenticado</span>
                <p className="text-sm font-semibold text-slate-200 mt-1 font-mono">
                  {painelData?.usuarioLogado || user?.usuario || '—'}
                </p>
              </div>
              <div className="bg-slate-950/60 rounded-xl p-3.5 border border-slate-800">
                <span className="text-xs text-slate-400">Permissões (Authorities)</span>
                <div className="flex flex-wrap gap-1.5 mt-1">
                  {painelData?.permissoes?.map((p, idx) => (
                    <span
                      key={idx}
                      className="px-2 py-0.5 rounded bg-purple-900/40 text-purple-200 text-xs font-mono border border-purple-700/30"
                    >
                      {p.authority}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Grid: Formulário de Adicionar + Histórico de Usuários Criados */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Coluna do Formulário (POST /api/rh/adicionar) */}
          <div className="lg:col-span-7">
            <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
                    <UserPlus size={20} />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-white">Cadastrar Novo Usuário</h3>
                    <p className="text-xs text-slate-400">
                      Dispara requisição real para <span className="font-mono text-indigo-300">POST /api/rh/adicionar</span>
                    </p>
                  </div>
                </div>

                {/* Botões de Preenchimento Rápido */}
                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => handlePreencherExemplo('COLABORADOR')}
                    className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer"
                  >
                    + Colaborador
                  </button>
                  <button
                    type="button"
                    onClick={() => handlePreencherExemplo('GESTOR')}
                    className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer"
                  >
                    + Gestor
                  </button>
                </div>
              </div>

              <form onSubmit={handleCadastrar} className="space-y-4 mt-6">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                    Nome Completo *
                  </label>
                  <input
                    type="text"
                    required
                    value={nomeCompleto}
                    onChange={(e) => setNomeCompleto(e.target.value)}
                    placeholder="Ex: Carlos Eduardo de Oliveira"
                    className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                      Usuário (Username) *
                    </label>
                    <input
                      type="text"
                      required
                      value={usuario}
                      onChange={(e) => setUsuario(e.target.value)}
                      placeholder="Ex: carlos.oliveira"
                      className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                      Senha Inicial *
                    </label>
                    <input
                      type="password"
                      required
                      value={senha}
                      onChange={(e) => setSenha(e.target.value)}
                      placeholder="••••••••"
                      className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                    Perfil de Acesso (Role) *
                  </label>
                  <select
                    value={perfilAcesso}
                    onChange={(e) => setPerfilAcesso(e.target.value)}
                    className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none cursor-pointer"
                  >
                    <option value="COLABORADOR">COLABORADOR - Registro de ponto e consulta de espelho</option>
                    <option value="GESTOR">GESTOR - Supervisão de equipes e espelhos</option>
                    <option value="RH">RH - Gestão global e cadastro de funcionários</option>
                  </select>
                </div>

                <div className="pt-3">
                  <button
                    type="submit"
                    disabled={submitting}
                    className="w-full flex items-center justify-center gap-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 px-4 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-600/25 focus:outline-none transition-all cursor-pointer disabled:opacity-50"
                  >
                    {submitting ? (
                      <>
                        <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                        <span>Enviando para o Backend...</span>
                      </>
                    ) : (
                      <>
                        <UserPlus size={16} />
                        <span>Criar Funcionário no Backend</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>

          {/* Coluna de Usuários Cadastrados para Teste Rápido */}
          <div className="lg:col-span-5 space-y-4">
            <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Sparkles size={18} className="text-amber-400" />
                  <h3 className="text-base font-bold text-white">Usuários Criados para Teste</h3>
                </div>
                <span className="text-xs text-slate-400 bg-slate-800 px-2.5 py-1 rounded-full">
                  Total: {usuariosCadastrados.length}
                </span>
              </div>

              {usuariosCadastrados.length === 0 ? (
                <div className="text-center py-8 px-4 rounded-xl border border-dashed border-slate-800 text-slate-400 text-sm">
                  <UserPlus size={32} className="mx-auto mb-2 text-slate-600" />
                  <p>Nenhum novo funcionário criado nesta sessão ainda.</p>
                  <p className="text-xs text-slate-500 mt-1">
                    Cadastre um COLABORADOR ou GESTOR ao lado para testar as outras telas!
                  </p>
                </div>
              ) : (
                <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                  {usuariosCadastrados.map((item, idx) => (
                    <div
                      key={idx}
                      className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 hover:border-slate-700 transition-colors flex items-center justify-between"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-sm text-slate-200">
                            {item.nomeCompleto}
                          </span>
                          <span
                            className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                              item.perfilAcesso === 'COLABORADOR'
                                ? 'bg-emerald-500/20 text-emerald-400'
                                : item.perfilAcesso === 'GESTOR'
                                ? 'bg-blue-500/20 text-blue-400'
                                : 'bg-purple-500/20 text-purple-400'
                            }`}
                          >
                            {item.perfilAcesso}
                          </span>
                        </div>
                        <div className="text-xs text-slate-400 font-mono mt-0.5">
                          Login: <span className="text-indigo-300 font-semibold">{item.usuario}</span> | ID #{item.id}
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleTestarLoginNovo(item)}
                        className="inline-flex items-center gap-1 rounded-lg bg-indigo-600/20 border border-indigo-500/30 px-2.5 py-1.5 text-xs font-medium text-indigo-300 hover:bg-indigo-600 hover:text-white transition-all cursor-pointer"
                        title="Desconectar e logar como este usuário"
                      >
                        <KeyRound size={12} />
                        Testar
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Guia de Acessos rápidos */}
            <div className="rounded-2xl bg-indigo-950/30 border border-indigo-900/40 p-5 text-xs text-indigo-300/90 space-y-2">
              <div className="font-semibold text-indigo-200 flex items-center gap-1.5 text-sm">
                <Briefcase size={16} /> Fluxo de Teste Recomendado:
              </div>
              <ol className="list-decimal list-inside space-y-1 text-slate-300 pl-1">
                <li>Cadastre um <strong>COLABORADOR</strong> com o formulário ao lado.</li>
                <li>Cadastre um <strong>GESTOR</strong> com o formulário ao lado.</li>
                <li>Clique em <strong>Testar</strong> no card do usuário para ir à tela de login.</li>
                <li>Faça login e veja a tela do painel exclusiva do perfil correspondente!</li>
              </ol>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default RhDashboard;
