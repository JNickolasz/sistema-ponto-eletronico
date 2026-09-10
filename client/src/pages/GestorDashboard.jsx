import React, { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import gestorService from '../services/gestorService';
import { useAuth } from '../hooks/useAuth';
import {
  Building2,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Users,
  Check,
  X,
  FileCheck,
  TrendingUp,
  AlertTriangle,
  Terminal,
} from 'lucide-react';
import toast from 'react-hot-toast';

export function GestorDashboard() {
  const { user } = useAuth();

  // Estado do painel do backend
  const [painelData, setPainelData] = useState(null);
  const [loadingPainel, setLoadingPainel] = useState(true);
  const [painelError, setPainelError] = useState(null);

  // Lista simulada de aprovações pendentes de liderança
  const [solicitacoes, setSolicitacoes] = useState([
    {
      id: 101,
      colaborador: 'João da Silva',
      tipo: 'Esquecimento de Batida',
      data: '08/09/2026',
      horarioProposto: '17:30 (Saída)',
      motivo: 'Falta de conexão no terminal móvel no encerramento da jornada.',
    },
    {
      id: 102,
      colaborador: 'Ana Beatriz Ferreira',
      tipo: 'Declaração Médica',
      data: '07/09/2026',
      horarioProposto: '2 horas de abono',
      motivo: 'Atestado de comparecimento odontológico.',
    },
  ]);

  // Lista da equipe sob gestão
  const [membrosEquipe] = useState([
    { nome: 'João da Silva', cargo: 'Desenvolvedor Pleno', status: 'Em Expediente', entrada: '08:02' },
    { nome: 'Ana Beatriz Ferreira', cargo: 'UI/UX Designer', status: 'Em Expediente', entrada: '08:15' },
    { nome: 'Lucas Mendonça', cargo: 'Analista de QA', status: 'Intervalo Almoço', entrada: '07:55' },
    { nome: 'Fernanda Lima', cargo: 'DevOps Engineer', status: 'Folga Escala', entrada: '—' },
  ]);

  const recarregarPainel = async () => {
    try {
      setLoadingPainel(true);
      setPainelError(null);
      const data = await gestorService.getPainel();
      setPainelData(data);
    } catch (err) {
      setPainelError(err.message || 'Erro ao consultar painel do Gestor');
    } finally {
      setLoadingPainel(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    gestorService
      .getPainel()
      .then((data) => {
        if (isMounted) setPainelData(data);
      })
      .catch((err) => {
        if (isMounted) setPainelError(err.message || 'Erro ao consultar painel do Gestor');
      })
      .finally(() => {
        if (isMounted) setLoadingPainel(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);


  const handleAprovar = (id, nome) => {
    setSolicitacoes(solicitacoes.filter((s) => s.id !== id));
    toast.success(`Solicitação de ${nome} aprovada com sucesso!`);
  };

  const handleRecusar = (id, nome) => {
    setSolicitacoes(solicitacoes.filter((s) => s.id !== id));
    toast.error(`Solicitação de ${nome} foi recusada.`);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Header do Painel */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-800/80 pb-6">
          <div>
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-400">
                <Building2 size={24} />
              </div>
              <div>
                <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
                  Painel de Liderança e Gestão de Equipe
                </h1>
                <p className="text-sm text-slate-400 mt-0.5">
                  Supervisão de assiduidade, validação de espelhos e aprovações de jornada.
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

        {/* Backend Response Card (GET /api/gestor/painel) */}
        <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-6 backdrop-blur-xl">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Terminal size={18} className="text-blue-400" />
              <h2 className="text-base font-semibold text-slate-100">
                Resposta do Endpoint do Backend:{' '}
                <span className="font-mono text-blue-300">GET /api/gestor/painel</span>
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
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
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
                <p className="text-sm font-semibold text-blue-300 mt-1">
                  {painelData?.mensagem || '—'}
                </p>
              </div>
              <div className="bg-slate-950/60 rounded-xl p-3.5 border border-slate-800">
                <span className="text-xs text-slate-400">Gestor Autenticado</span>
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
                      className="px-2 py-0.5 rounded bg-blue-900/40 text-blue-200 text-xs font-mono border border-blue-700/30"
                    >
                      {p.authority}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Métricas da Equipe */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="rounded-2xl bg-slate-900/70 border border-slate-800 p-5 backdrop-blur-xl">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Presença Hoje
              </span>
              <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400">
                <Users size={18} />
              </div>
            </div>
            <div className="text-2xl font-bold font-mono text-white mt-2">3 / 4 Ativos</div>
            <div className="text-xs text-emerald-400 mt-1">75% da equipe em expediente</div>
          </div>

          <div className="rounded-2xl bg-slate-900/70 border border-slate-800 p-5 backdrop-blur-xl">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Ajustes Pendentes
              </span>
              <div className="p-2 rounded-xl bg-amber-500/10 text-amber-400">
                <AlertTriangle size={18} />
              </div>
            </div>
            <div className="text-2xl font-bold font-mono text-amber-400 mt-2">
              {solicitacoes.length} Solicitações
            </div>
            <div className="text-xs text-slate-400 mt-1">Necessitam de sua validação</div>
          </div>

          <div className="rounded-2xl bg-slate-900/70 border border-slate-800 p-5 backdrop-blur-xl">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Horas Extras da Semana
              </span>
              <div className="p-2 rounded-xl bg-blue-500/10 text-blue-400">
                <TrendingUp size={18} />
              </div>
            </div>
            <div className="text-2xl font-bold font-mono text-blue-400 mt-2">08h 45m</div>
            <div className="text-xs text-slate-400 mt-1">Dentro do teto acordado</div>
          </div>
        </div>

        {/* Linha Dupla: Solicitações Pendentes + Lista da Equipe */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Fila de Aprovações */}
          <div className="lg:col-span-7 space-y-4">
            <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <FileCheck size={20} className="text-amber-400" />
                  <h3 className="text-lg font-bold text-white">Solicitações de Ajuste de Ponto</h3>
                </div>
                <span className="text-xs font-semibold text-slate-400 bg-slate-800 px-2.5 py-1 rounded-full">
                  {solicitacoes.length} pendentes
                </span>
              </div>

              {solicitacoes.length === 0 ? (
                <div className="text-center py-10 px-4 rounded-xl border border-dashed border-slate-800 text-slate-400 text-sm">
                  <CheckCircle2 size={36} className="mx-auto mb-2 text-emerald-500" />
                  <p className="font-medium text-slate-300">Tudo limpo por aqui!</p>
                  <p className="text-xs text-slate-500 mt-1">
                    Não há solicitações pendentes de aprovação no momento.
                  </p>
                </div>
              ) : (
                <div className="space-y-3">
                  {solicitacoes.map((item) => (
                    <div
                      key={item.id}
                      className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-3"
                    >
                      <div className="flex items-start justify-between">
                        <div>
                          <div className="font-semibold text-sm text-slate-100">
                            {item.colaborador}
                          </div>
                          <div className="text-xs text-amber-400 font-medium mt-0.5">
                            {item.tipo} • Data: {item.data}
                          </div>
                        </div>
                        <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300">
                          {item.horarioProposto}
                        </span>
                      </div>

                      <p className="text-xs text-slate-400 bg-slate-900/80 p-2.5 rounded-lg border border-slate-800/60">
                        {item.motivo}
                      </p>

                      <div className="flex justify-end gap-2 pt-1">
                        <button
                          type="button"
                          onClick={() => handleRecusar(item.id, item.colaborador)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-red-500/20 text-slate-300 hover:text-red-400 text-xs font-medium border border-slate-700 transition-all cursor-pointer"
                        >
                          <X size={14} /> Recusar
                        </button>
                        <button
                          type="button"
                          onClick={() => handleAprovar(item.id, item.colaborador)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-md shadow-emerald-600/20 transition-all cursor-pointer"
                        >
                          <Check size={14} /> Aprovar Ajuste
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Lista de Membros da Equipe */}
          <div className="lg:col-span-5 space-y-4">
            <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Users size={18} className="text-blue-400" />
                  <h3 className="text-base font-bold text-white">Status da Equipe</h3>
                </div>
              </div>

              <div className="space-y-3">
                {membrosEquipe.map((m, idx) => (
                  <div
                    key={idx}
                    className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 flex items-center justify-between"
                  >
                    <div>
                      <div className="font-semibold text-xs text-slate-200">{m.nome}</div>
                      <div className="text-[11px] text-slate-400">{m.cargo}</div>
                    </div>
                    <div className="text-right">
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                          m.status === 'Em Expediente'
                            ? 'bg-emerald-500/20 text-emerald-400'
                            : m.status === 'Intervalo Almoço'
                            ? 'bg-amber-500/20 text-amber-400'
                            : 'bg-slate-800 text-slate-400'
                        }`}
                      >
                        {m.status}
                      </span>
                      <div className="text-[10px] text-slate-500 font-mono mt-0.5">
                        {m.entrada !== '—' ? `Entrada: ${m.entrada}` : 'Ausente'}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default GestorDashboard;
