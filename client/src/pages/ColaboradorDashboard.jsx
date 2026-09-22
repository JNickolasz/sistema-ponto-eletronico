import React, { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import colaboradorService from '../services/colaboradorService';
import jornadaService from '../services/jornadaService';
import { useAuth } from '../hooks/useAuth';
import {
  Clock,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  FileSpreadsheet,
  Calendar,
  Timer,
  Terminal,
  ArrowRight,
  History,
  TrendingUp,
  Award,
  Coffee,
  CalendarDays,
  Briefcase,
} from 'lucide-react';
import toast from 'react-hot-toast';

export function ColaboradorDashboard() {
  const { user } = useAuth();

  const [painelData, setPainelData] = useState(null);
  const [loadingPainel, setLoadingPainel] = useState(true);
  const [painelError, setPainelError] = useState(null);

  const [currentTime, setCurrentTime] = useState(new Date());

  const [espelhoIdInput, setEspelhoIdInput] = useState('1');
  const [espelhoResult, setEspelhoResult] = useState(null);
  const [loadingEspelho, setLoadingEspelho] = useState(false);

  const [horarioPrevistoColab, setHorarioPrevistoColab] = useState(null);
  const [loadingHorarioColab, setLoadingHorarioColab] = useState(false);
  const [dataHorarioColab, setDataHorarioColab] = useState(new Date().toISOString().split('T')[0]);
  const [funcionarioIdColab, setFuncionarioIdColab] = useState('');

  // Histórico de batidas simuladas do dia
  const [pontosHoje, setPontosHoje] = useState([
    { tipo: 'Entrada Manhã', hora: '08:02:15', status: 'confirmado' },
    { tipo: 'Saída Almoço', hora: '12:01:40', status: 'confirmado' },
    { tipo: 'Retorno Almoço', hora: '13:00:22', status: 'confirmado' },
  ]);

  // Atualização contínua do relógio
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const recarregarPainel = async () => {
    try {
      setLoadingPainel(true);
      setPainelError(null);
      const data = await colaboradorService.getPainel();
      setPainelData(data);
      if (data?.funcionarioId) {
        setFuncionarioIdColab(data.funcionarioId);
        consultarHorarioSilencioso(data.funcionarioId, dataHorarioColab);
      }
    } catch (err) {
      setPainelError(err.message || 'Erro ao consultar painel do Colaborador');
    } finally {
      setLoadingPainel(false);
    }
  };

  const consultarHorarioSilencioso = async (funcId, data) => {
    if (!funcId) return;
    try {
      setLoadingHorarioColab(true);
      const res = await jornadaService.obterHorarioPrevisto(funcId, data);
      setHorarioPrevistoColab(res);
    } catch {
      // Ignora falha silenciosa no autoload
    } finally {
      setLoadingHorarioColab(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    colaboradorService
      .getPainel()
      .then((data) => {
        if (isMounted) {
          setPainelData(data);
          if (data?.funcionarioId) {
            setFuncionarioIdColab(data.funcionarioId);
            jornadaService
              .obterHorarioPrevisto(data.funcionarioId, dataHorarioColab)
              .then((res) => {
                if (isMounted) setHorarioPrevistoColab(res);
              })
              .catch(() => {});
          }
        }
      })
      .catch((err) => {
        if (isMounted) setPainelError(err.message || 'Erro ao consultar painel do Colaborador');
      })
      .finally(() => {
        if (isMounted) setLoadingPainel(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const handleConsultarMeuHorario = async (e) => {
    if (e) e.preventDefault();
    if (!funcionarioIdColab) {
      toast.error('Informe seu UUID de colaborador');
      return;
    }
    try {
      setLoadingHorarioColab(true);
      const res = await jornadaService.obterHorarioPrevisto(funcionarioIdColab, dataHorarioColab);
      setHorarioPrevistoColab(res);
      toast.success('Horário previsto consultado com sucesso!');
    } catch (err) {
      toast.error(err.message || 'Erro ao consultar horário previsto');
      setHorarioPrevistoColab(null);
    } finally {
      setLoadingHorarioColab(false);
    }
  };

  // Registrar batida de ponto
  const handleBaterPonto = () => {
    const horaFormatada = currentTime.toLocaleTimeString('pt-BR');
    const tipos = ['Entrada', 'Saída para Intervalo', 'Retorno do Intervalo', 'Saída Final'];
    const proximoTipo = tipos[pontosHoje.length % tipos.length];

    const novoPonto = {
      tipo: proximoTipo,
      hora: horaFormatada,
      status: 'confirmado',
    };

    setPontosHoje([...pontosHoje, novoPonto]);
    toast.success(`Ponto registrado com sucesso: ${proximoTipo} às ${horaFormatada}!`, {
      icon: '⏱️',
      duration: 5000,
    });
  };

  // Testar endpoint do espelho
  const handleTestarEspelho = async (e) => {
    e.preventDefault();
    if (!espelhoIdInput) return;

    try {
      setLoadingEspelho(true);
      setEspelhoResult(null);
      const res = await colaboradorService.getEspelho(espelhoIdInput);
      setEspelhoResult({ success: true, data: res });
      toast.success('Espelho de ponto retornado com sucesso!');
    } catch (err) {
      setEspelhoResult({
        success: false,
        status: err.status || 403,
        error: err.message || 'Acesso negado ao espelho de outro colaborador.',
      });
      toast.error(err.message || 'Acesso negado: Você só pode acessar o seu próprio espelho.');
    } finally {
      setLoadingEspelho(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Header do Painel */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-800/80 pb-6">
          <div>
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                <Clock size={24} />
              </div>
              <div>
                <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
                  Portal do Colaborador
                </h1>
                <p className="text-sm text-slate-400 mt-0.5">
                  Registro eletrônico de jornada, espelho de ponto e banco de horas.
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

        {/* Backend Response Card (GET /api/v1/colaborador/painel) */}
        <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-6 backdrop-blur-xl">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Terminal size={18} className="text-emerald-400" />
              <h2 className="text-base font-semibold text-slate-100">
                Resposta do Endpoint do Backend:{' '}
                <span className="font-mono text-emerald-300">GET /api/v1/colaborador/painel</span>
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
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-emerald-500 border-t-transparent" />
              <span>Consultando endpoint do backend...</span>
            </div>
          ) : painelError ? (
            <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-300 text-sm">
              {painelError}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="bg-slate-950/60 rounded-xl p-3.5 border border-slate-800">
                <span className="text-xs text-slate-400">Mensagem da API</span>
                <p className="text-sm font-semibold text-emerald-300 mt-1">
                  {painelData?.mensagem || '—'}
                </p>
              </div>
              <div className="bg-slate-950/60 rounded-xl p-3.5 border border-slate-800">
                <span className="text-xs text-slate-400">Colaborador Autenticado</span>
                <p className="text-sm font-semibold text-slate-200 mt-1 font-mono">
                  {painelData?.usuarioLogado || user?.usuario || '—'}
                </p>
              </div>
              <div className="bg-slate-950/60 rounded-xl p-3.5 border border-slate-800">
                <span className="text-xs text-slate-400">UUID do Colaborador</span>
                <p className="text-xs font-semibold text-indigo-300 mt-1 font-mono truncate" title={painelData?.funcionarioId}>
                  {painelData?.funcionarioId || '—'}
                </p>
              </div>
              <div className="bg-slate-950/60 rounded-xl p-3.5 border border-slate-800">
                <span className="text-xs text-slate-400">Permissões (Authorities)</span>
                <div className="flex flex-wrap gap-1.5 mt-1">
                  {painelData?.permissoes?.map((p, idx) => (
                    <span
                      key={idx}
                      className="px-2 py-0.5 rounded bg-emerald-900/40 text-emerald-200 text-xs font-mono border border-emerald-700/30"
                    >
                      {p.authority}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Linha Principal: Relógio de Ponto + Estatísticas */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Relógio Digital e Ação de Bater Ponto */}
          <div className="lg:col-span-6">
            <div className="rounded-3xl bg-gradient-to-br from-slate-900 to-slate-950 border border-slate-800 p-8 shadow-2xl flex flex-col items-center text-center relative overflow-hidden">
              <div className="absolute top-0 right-0 w-48 h-48 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />

              <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-emerald-400 mb-2">
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                </span>
                Horário Oficial de Brasília
              </div>

              {/* Horário Grande */}
              <div className="font-mono text-5xl sm:text-6xl font-black tracking-tight text-white my-4 tabular-nums">
                {currentTime.toLocaleTimeString('pt-BR')}
              </div>

              {/* Data formatada */}
              <div className="text-sm font-medium text-slate-400 capitalize mb-6 flex items-center gap-1.5">
                <Calendar size={15} className="text-slate-500" />
                {currentTime.toLocaleDateString('pt-BR', {
                  weekday: 'long',
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                })}
              </div>

              {/* Botão de Registro */}
              <button
                type="button"
                onClick={handleBaterPonto}
                className="w-full max-w-sm flex items-center justify-center gap-3 rounded-2xl bg-gradient-to-r from-emerald-600 via-emerald-500 to-teal-600 px-6 py-4 text-base font-bold text-white shadow-xl shadow-emerald-600/30 hover:scale-[1.02] active:scale-[0.98] transition-all cursor-pointer"
              >
                <Timer size={22} />
                <span>Registrar Ponto Agora</span>
              </button>

              {/* Registros de Hoje */}
              <div className="w-full mt-8 pt-6 border-t border-slate-800/80">
                <div className="flex items-center justify-between mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400">
                  <span className="flex items-center gap-1.5">
                    <History size={14} /> Batidas Registradas Hoje
                  </span>
                  <span className="text-emerald-400">{pontosHoje.length} marcações</span>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  {pontosHoje.map((p, idx) => (
                    <div
                      key={idx}
                      className="bg-slate-950/80 border border-slate-800 rounded-xl p-2.5 text-center"
                    >
                      <div className="text-[10px] text-slate-400 truncate">{p.tipo}</div>
                      <div className="text-xs font-mono font-bold text-white mt-0.5">{p.hora}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Teste do Espelho de Ponto (GET /api/v1/colaborador/{id}/espelho) */}
          <div className="lg:col-span-6 space-y-6">
            <div className="rounded-3xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
              <div className="flex items-center gap-2.5 mb-2">
                <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
                  <FileSpreadsheet size={20} />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Consulta do Espelho de Ponto</h3>
                  <p className="text-xs text-slate-400">
                    Endpoint: <span className="font-mono text-indigo-300">GET /api/v1/colaborador/&#123;id&#125;/espelho</span>
                  </p>
                </div>
              </div>

              <p className="text-xs text-slate-400 mt-2">
                O backend Spring Boot possui uma regra de segurança restrita: um colaborador <strong>só tem autorização para visualizar seu próprio espelho</strong>. Tentar consultar o ID de outro funcionário retornará <span className="text-amber-400 font-mono">403 Forbidden</span>.
              </p>

              <div className="flex gap-2 mt-3">
                <button
                  type="button"
                  onClick={() => {
                    setEspelhoIdInput('2');
                    colaboradorService.getEspelho(2)
                      .then(data => {
                        setEspelhoResult({ success: true, data });
                        toast.success('Espelho liberado para seu próprio ID!');
                      })
                      .catch(err => {
                        setEspelhoResult({ success: false, status: err.status || 403, error: err.message });
                      });
                  }}
                  className="px-2.5 py-1 rounded-lg bg-emerald-950/60 border border-emerald-800/40 text-emerald-300 text-xs hover:bg-emerald-900/50 transition-colors cursor-pointer"
                >
                  ✓ Testar Meu ID (2) → 200 OK
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setEspelhoIdInput('1');
                    colaboradorService.getEspelho(1)
                      .then(data => {
                        setEspelhoResult({ success: true, data });
                      })
                      .catch(err => {
                        setEspelhoResult({ success: false, status: err.status || 403, error: err.message });
                        toast.error('Bloqueado pelo Spring Boot: 403 Forbidden!');
                      });
                  }}
                  className="px-2.5 py-1 rounded-lg bg-red-950/60 border border-red-800/40 text-red-300 text-xs hover:bg-red-900/50 transition-colors cursor-pointer"
                >
                  ✕ Testar Outro ID (1) → 403 Forbidden
                </button>
              </div>

              <form onSubmit={handleTestarEspelho} className="mt-3 flex gap-2">
                <div className="flex-1">
                  <input
                    type="number"
                    min="1"
                    required
                    value={espelhoIdInput}
                    onChange={(e) => setEspelhoIdInput(e.target.value)}
                    placeholder="Digite o ID do funcionário para testar"
                    className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>
                <button
                  type="submit"
                  disabled={loadingEspelho}
                  className="rounded-xl bg-indigo-600 hover:bg-indigo-500 px-4 py-2.5 text-xs font-semibold text-white shadow-md shadow-indigo-600/20 transition-all cursor-pointer disabled:opacity-50 flex items-center gap-1.5"
                >
                  {loadingEspelho ? (
                    <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                  ) : (
                    <>
                      <span>Consultar</span>
                      <ArrowRight size={14} />
                    </>
                  )}
                </button>
              </form>


              {/* Resultado do Teste de Espelho */}
              {espelhoResult && (
                <div
                  className={`mt-4 p-4 rounded-xl border text-xs font-mono ${
                    espelhoResult.success
                      ? 'bg-emerald-950/40 border-emerald-800/50 text-emerald-300'
                      : 'bg-red-950/40 border-red-800/50 text-red-300'
                  }`}
                >
                  <div className="font-bold mb-1 flex items-center gap-1.5">
                    {espelhoResult.success ? (
                      <>
                        <CheckCircle2 size={14} className="text-emerald-400" />
                        <span>Status: 200 OK (Espelho Liberado)</span>
                      </>
                    ) : (
                      <>
                        <AlertCircle size={14} className="text-red-400" />
                        <span>Status: {espelhoResult.status} (Bloqueio de Segurança)</span>
                      </>
                    )}
                  </div>
                  <pre className="mt-2 p-2.5 bg-slate-950/80 rounded-lg overflow-x-auto text-[11px]">
                    {JSON.stringify(espelhoResult.data || { erro: espelhoResult.error }, null, 2)}
                  </pre>
                </div>
              )}
            </div>

            {/* Meu Horário Previsto do Dia (T04) */}
            <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-6 backdrop-blur-xl">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-4">
                <div className="flex items-center gap-2">
                  <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    <CalendarDays size={18} />
                  </div>
                  <div>
                    <h2 className="text-base font-semibold text-slate-100 flex items-center gap-2">
                      Meu Horário Previsto do Dia
                      <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                        Tarefa T04
                      </span>
                    </h2>
                    <p className="text-xs text-slate-400">
                      Calculado automaticamente a partir do Regime Vigente (Jornada fixa ou Escala cíclica)
                    </p>
                  </div>
                </div>
                {horarioPrevistoColab && (
                  <div className="flex items-center gap-2">
                    <span
                      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium border ${
                        horarioPrevistoColab.diaTrabalho
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                          : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                      }`}
                    >
                      {horarioPrevistoColab.diaTrabalho ? (
                        <>
                          <CheckCircle2 size={12} /> Dia de Trabalho
                        </>
                      ) : (
                        <>
                          <Coffee size={12} /> Folga / DSR
                        </>
                      )}
                    </span>
                    <span className="inline-flex items-center rounded-full bg-slate-800 px-2.5 py-0.5 text-xs font-mono text-slate-300 border border-slate-700">
                      {horarioPrevistoColab.tipoRegime}
                    </span>
                  </div>
                )}
              </div>

              <form onSubmit={handleConsultarMeuHorario} className="grid grid-cols-1 sm:grid-cols-12 gap-3 mb-4">
                <div className="sm:col-span-4">
                  <label className="block text-xs font-medium text-slate-400 mb-1">
                    Data de Referência
                  </label>
                  <input
                    type="date"
                    value={dataHorarioColab}
                    onChange={(e) => setDataHorarioColab(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500 transition-colors"
                  />
                </div>
                <div className="sm:col-span-5">
                  <label className="block text-xs font-medium text-slate-400 mb-1">
                    UUID do Funcionário
                  </label>
                  <input
                    type="text"
                    value={funcionarioIdColab}
                    onChange={(e) => setFuncionarioIdColab(e.target.value)}
                    placeholder="Auto-preenchido pelo login"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-emerald-500 transition-colors"
                  />
                </div>
                <div className="sm:col-span-3 flex items-end">
                  <button
                    type="submit"
                    disabled={loadingHorarioColab}
                    className="w-full h-[34px] inline-flex items-center justify-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow-lg shadow-emerald-900/30 transition-all cursor-pointer disabled:opacity-50"
                  >
                    <RefreshCw size={14} className={loadingHorarioColab ? 'animate-spin' : ''} />
                    Consultar
                  </button>
                </div>
              </form>

              {loadingHorarioColab ? (
                <div className="flex items-center justify-center py-6 text-slate-400 text-xs gap-2">
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-emerald-500 border-t-transparent" />
                  Calculando horário previsto do dia...
                </div>
              ) : horarioPrevistoColab ? (
                <div className="space-y-3">
                  {horarioPrevistoColab.diaTrabalho ? (
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-3">
                        <span className="text-[11px] text-slate-400">Entrada Prevista</span>
                        <p className="text-base font-bold font-mono text-emerald-400 mt-0.5">
                          {horarioPrevistoColab.horaEntrada || '—'}
                        </p>
                      </div>
                      <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-3">
                        <span className="text-[11px] text-slate-400">Saída Prevista</span>
                        <p className="text-base font-bold font-mono text-emerald-400 mt-0.5">
                          {horarioPrevistoColab.horaSaida || '—'}
                        </p>
                      </div>
                      <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-3">
                        <span className="text-[11px] text-slate-400">Intervalo</span>
                        <p className="text-xs font-bold font-mono text-slate-200 mt-1">
                          {horarioPrevistoColab.intervaloInicio
                            ? `${horarioPrevistoColab.intervaloInicio} - ${horarioPrevistoColab.intervaloFim}`
                            : 'Sem intervalo'}
                        </p>
                      </div>
                      <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-3">
                        <span className="text-[11px] text-slate-400">Carga Diária</span>
                        <p className="text-xs font-bold font-mono text-indigo-300 mt-1">
                          {horarioPrevistoColab.cargaDiariaMinutos
                            ? `${Math.floor(horarioPrevistoColab.cargaDiariaMinutos / 60)}h ${horarioPrevistoColab.cargaDiariaMinutos % 60}m`
                            : '0m'}{' '}
                          <span className="text-[10px] text-slate-500 font-normal">
                            (±{horarioPrevistoColab.toleranciaMinutos || 0}m tol.)
                          </span>
                        </p>
                      </div>
                    </div>
                  ) : (
                    <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs flex items-center gap-3">
                      <Coffee size={20} className="text-amber-400 shrink-0" />
                      <div>
                        <p className="font-semibold text-amber-200">Hoje é dia de descanso!</p>
                        <p className="text-amber-300/80 mt-0.5">
                          {horarioPrevistoColab.mensagem || 'Você não tem escala ou jornada prevista para esta data.'}
                        </p>
                      </div>
                    </div>
                  )}

                  <div className="p-3 rounded-xl bg-slate-950/50 border border-slate-800/80 text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <span className="text-slate-400">
                      Regime: <strong className="text-slate-200">{horarioPrevistoColab.descricaoRegime}</strong> ({horarioPrevistoColab.diaSemana})
                    </span>
                    <span className="text-slate-500 font-mono text-[11px]">
                      {horarioPrevistoColab.mensagem}
                    </span>
                  </div>
                </div>
              ) : (
                <div className="p-4 rounded-xl bg-slate-950/40 border border-dashed border-slate-800 text-slate-500 text-xs text-center">
                  Informe o ID do colaborador e a data para simular ou visualizar o horário previsto.
                </div>
              )}
            </div>

            {/* Banco de Horas Summary */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-4">
                <div className="flex items-center gap-2 text-xs font-semibold text-slate-400">
                  <TrendingUp size={16} className="text-emerald-400" /> Saldo de Banco de Horas
                </div>
                <div className="mt-2 text-2xl font-bold font-mono text-emerald-400">+04h 32m</div>
                <div className="text-[11px] text-slate-500 mt-0.5">Positivo neste período</div>
              </div>

              <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-4">
                <div className="flex items-center gap-2 text-xs font-semibold text-slate-400">
                  <Award size={16} className="text-indigo-400" /> Assiduidade
                </div>
                <div className="mt-2 text-2xl font-bold font-mono text-indigo-400">100%</div>
                <div className="text-[11px] text-slate-500 mt-0.5">Sem pendências registradas</div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default ColaboradorDashboard;
