import React, { useState, useEffect, useRef, useId } from 'react';
import inconsistenciaService from '../services/inconsistenciaService';
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  Filter,
  HardDrive,
  Link2,
  Lock,
  PlusCircle,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Trash2,
  User,
  X,
  CalendarCheck,
  FileText,
} from 'lucide-react';
import toast from 'react-hot-toast';

const TIPOS_META = {
  ORFA: {
    label: 'Batida Órfã',
    badgeClass: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
    acaoPadrao: 'VINCULAR',
  },
  DIA_IMPAR: {
    label: 'Dia Ímpar',
    badgeClass: 'bg-rose-500/15 text-rose-300 border-rose-500/30',
    acaoPadrao: 'INCLUIR_MARCACAO_AJUSTE',
  },
  DUPLICADA_MINUTO: {
    label: 'Duplicada no Minuto',
    badgeClass: 'bg-purple-500/15 text-purple-300 border-purple-500/30',
    acaoPadrao: 'DESCARTAR_DUPLICADA',
  },
  FORA_DA_JORNADA: {
    label: 'Fora da Jornada / Feriado',
    badgeClass: 'bg-cyan-500/15 text-cyan-300 border-cyan-500/30',
    acaoPadrao: 'ABONAR_DIA',
  },
  SEQUENCIA_INVALIDA: {
    label: 'Sequência Inválida',
    badgeClass: 'bg-orange-500/15 text-orange-300 border-orange-500/30',
    acaoPadrao: 'INCLUIR_MARCACAO_AJUSTE',
  },
  EQUIPAMENTO_DESCONHECIDO: {
    label: 'Equipamento Desconhecido',
    badgeClass: 'bg-indigo-500/15 text-indigo-300 border-indigo-500/30',
    acaoPadrao: 'VINCULAR',
  },
};

const ACOES_LABELS = {
  VINCULAR: 'Vincular (Colaborador / Equipamento)',
  INCLUIR_MARCACAO_AJUSTE: 'Incluir Marcação de Ajuste',
  DESCARTAR_DUPLICADA: 'Descartar Marcação Duplicada/Indevida',
  ABONAR_DIA: 'Abonar o Dia',
};

export default function FilaInconsistenciasSection({
  funcionarios = [],
  equipamentos = [],
  onAtualizado,
}) {
  const mesAtualPadrao = new Date().toISOString().slice(0, 7);

  // Filtros da fila
  const [situacaoFiltro, setSituacaoFiltro] = useState('ABERTA');
  const [tipoFiltro, setTipoFiltro] = useState('');
  const [funcionarioFiltro, setFuncionarioFiltro] = useState('');
  const [mesReferenciaFiltro, setMesReferenciaFiltro] = useState('');
  const [dataInicioFiltro, setDataInicioFiltro] = useState('');
  const [dataFimFiltro, setDataFimFiltro] = useState('');

  // Dados da fila e painel
  const [inconsistencias, setInconsistencias] = useState([]);
  const [painel, setPainel] = useState({
    totalAbertas: 0,
    totalResolvidas: 0,
    abertasPorTipo: {},
  });
  const [fechamentos, setFechamentos] = useState([]);
  const [loading, setLoading] = useState(false);

  // Estado do modal de tratamento
  const [itemSelecionado, setItemSelecionado] = useState(null);
  const [acaoEscolhida, setAcaoEscolhida] = useState('VINCULAR');
  const [motivoTratamento, setMotivoTratamento] = useState('');
  const [funcionarioIdTratamento, setFuncionarioIdTratamento] = useState('');
  const [equipamentoIdTratamento, setEquipamentoIdTratamento] = useState('');
  const [horarioAjusteTratamento, setHorarioAjusteTratamento] = useState('18:00');
  const [tipoRegistroAjuste, setTipoRegistroAjuste] = useState('SAIDA');
  const [submittingTratamento, setSubmittingTratamento] = useState(false);

  // Estado do Fechamento de Período
  const [mesFechamento, setMesFechamento] = useState(mesAtualPadrao);
  const [bloqueioFechamentoMsg, setBloqueioFechamentoMsg] = useState(null);
  const [submittingFechamento, setSubmittingFechamento] = useState(false);

  const dialogTratarRef = useRef(null);
  const dialogTratarTitleId = useId();
  const dialogFechamentoRef = useRef(null);
  const dialogFechamentoTitleId = useId();

  const carregarDados = async () => {
    try {
      setLoading(true);
      const filtros = {
        tipo: tipoFiltro || undefined,
        situacao: situacaoFiltro === 'TODAS' ? undefined : situacaoFiltro,
        funcionarioId: funcionarioFiltro || undefined,
        mesReferencia: mesReferenciaFiltro || undefined,
        dataInicio: dataInicioFiltro || undefined,
        dataFim: dataFimFiltro || undefined,
      };

      const [listaResp, painelResp, fechamentosResp] = await Promise.all([
        inconsistenciaService.listarInconsistencias(filtros).catch(() => []),
        inconsistenciaService
          .obterPainel({
            mesReferencia: mesReferenciaFiltro || undefined,
            dataInicio: dataInicioFiltro || undefined,
            dataFim: dataFimFiltro || undefined,
          })
          .catch(() => ({ totalAbertas: 0, totalResolvidas: 0, abertasPorTipo: {} })),
        inconsistenciaService.listarFechamentos().catch(() => []),
      ]);

      setInconsistencias(Array.isArray(listaResp) ? listaResp : []);
      setPainel(painelResp || { totalAbertas: 0, totalResolvidas: 0, abertasPorTipo: {} });
      setFechamentos(Array.isArray(fechamentosResp) ? fechamentosResp : []);
    } catch (err) {
      toast.error(err.message || 'Erro ao carregar fila de inconsistências');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarDados();
  }, [situacaoFiltro, tipoFiltro, funcionarioFiltro, mesReferenciaFiltro, dataInicioFiltro, dataFimFiltro]);

  const abrirModalTratamento = (item) => {
    setItemSelecionado(item);
    const meta = TIPOS_META[item.tipo] || {};
    setAcaoEscolhida(meta.acaoPadrao || 'ABONAR_DIA');
    setMotivoTratamento('');
    setFuncionarioIdTratamento(item.funcionarioId || (funcionarios[0]?.id ?? ''));
    setEquipamentoIdTratamento(item.equipamentoId || (equipamentos[0]?.id ?? ''));
    setHorarioAjusteTratamento('18:00');
    setTipoRegistroAjuste('SAIDA');

    if (dialogTratarRef.current && !dialogTratarRef.current.open) {
      dialogTratarRef.current.showModal();
    }
  };

  const fecharModalTratamento = () => {
    if (dialogTratarRef.current && dialogTratarRef.current.open) {
      dialogTratarRef.current.close();
    }
    setItemSelecionado(null);
    setMotivoTratamento('');
  };

  const handleConfirmarTratamento = async (e) => {
    e.preventDefault();
    if (!itemSelecionado) return;

    if (!motivoTratamento.trim()) {
      toast.error('O motivo do tratamento é obrigatório.');
      return;
    }

    try {
      setSubmittingTratamento(true);
      const payload = {
        acao: acaoEscolhida,
        motivo: motivoTratamento.trim(),
        funcionarioId: funcionarioIdTratamento || null,
        equipamentoId: equipamentoIdTratamento || null,
        horarioAjuste:
          acaoEscolhida === 'INCLUIR_MARCACAO_AJUSTE' ? horarioAjusteTratamento : null,
        tipoRegistroAjuste:
          acaoEscolhida === 'INCLUIR_MARCACAO_AJUSTE' ? tipoRegistroAjuste : null,
      };

      await inconsistenciaService.tratarInconsistencia(itemSelecionado.id, payload);
      toast.success('Inconsistência resolvida e registrada na auditoria!');
      fecharModalTratamento();
      await carregarDados();
      if (onAtualizado) onAtualizado();
    } catch (err) {
      toast.error(err.message || 'Erro ao tratar inconsistência');
    } finally {
      setSubmittingTratamento(false);
    }
  };

  const handleTentarFecharPeriodo = async (confirmarComPendencias = false) => {
    if (!mesFechamento) {
      toast.error('Informe o mês de referência (YYYY-MM) para fechamento.');
      return;
    }

    try {
      setSubmittingFechamento(true);
      const resultado = await inconsistenciaService.fecharPeriodo({
        mesReferencia: mesFechamento,
        confirmarComPendencias,
      });

      if (dialogFechamentoRef.current && dialogFechamentoRef.current.open) {
        dialogFechamentoRef.current.close();
      }
      setBloqueioFechamentoMsg(null);

      if (resultado.fechadoComRessalva) {
        toast.success(`Período ${mesFechamento} fechado com ressalva registrada!`);
      } else {
        toast.success(`Período ${mesFechamento} fechado sem pendências!`);
      }
      await carregarDados();
    } catch (err) {
      const msg = err.message || 'Existem pendências abertas neste período.';
      setBloqueioFechamentoMsg(msg);
      if (dialogFechamentoRef.current && !dialogFechamentoRef.current.open) {
        dialogFechamentoRef.current.showModal();
      }
    } finally {
      setSubmittingFechamento(false);
    }
  };

  const formatarDataHora = (iso) => {
    if (!iso) return '—';
    try {
      const d = new Date(iso);
      return d.toLocaleString('pt-BR');
    } catch {
      return iso;
    }
  };

  return (
    <div className="space-y-6">
      {/* Cabeçalho + Fechamento de Período */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
          <div className="flex items-start gap-3">
            <div className="p-2.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400">
              <ShieldAlert size={22} />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">
                Fila de Inconsistências e Fechamento (T12)
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Toda divergência entra automaticamente na fila. Nada é resolvido em silêncio: ação, autor, data e motivo ficam auditados.
              </p>
            </div>
          </div>

          {/* Barra rápida de Fechamento de Período */}
          <div className="flex flex-wrap items-center gap-2.5 bg-slate-950/70 p-3 rounded-xl border border-slate-800">
            <div className="flex items-center gap-2">
              <Lock size={15} className="text-indigo-400" />
              <span className="text-xs font-semibold text-slate-300">Mês Fechamento:</span>
              <input
                type="month"
                value={mesFechamento}
                onChange={(e) => setMesFechamento(e.target.value)}
                className="rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-xs text-slate-100 font-mono focus:border-indigo-500 focus:outline-none"
              />
            </div>
            <button
              type="button"
              disabled={submittingFechamento}
              onClick={() => handleTentarFecharPeriodo(false)}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-xs font-semibold text-white transition-all cursor-pointer disabled:opacity-50"
            >
              <CalendarCheck size={14} />
              <span>Fechar Período</span>
            </button>
            <button
              type="button"
              onClick={carregarDados}
              disabled={loading}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-700 bg-slate-900 hover:bg-slate-800 text-xs font-semibold text-slate-200 transition-all cursor-pointer"
            >
              <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
              <span>Atualizar</span>
            </button>
          </div>
        </div>

        {/* Painel de Contagem por Tipo (Critério 6) */}
        <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-3 mt-6">
          <div
            onClick={() => {
              setSituacaoFiltro('ABERTA');
              setTipoFiltro('');
            }}
            className="rounded-xl bg-rose-950/30 border border-rose-500/30 p-3 cursor-pointer hover:border-rose-400 transition-all"
          >
            <span className="text-[11px] font-semibold uppercase tracking-wider text-rose-300">
              Abertas na Fila
            </span>
            <p className="text-2xl font-bold text-white mt-1 font-mono">
              {painel.totalAbertas ?? 0}
            </p>
          </div>

          <div
            onClick={() => {
              setSituacaoFiltro('RESOLVIDA');
              setTipoFiltro('');
            }}
            className="rounded-xl bg-emerald-950/30 border border-emerald-500/30 p-3 cursor-pointer hover:border-emerald-400 transition-all"
          >
            <span className="text-[11px] font-semibold uppercase tracking-wider text-emerald-300">
              Resolvidas (Hist.)
            </span>
            <p className="text-2xl font-bold text-white mt-1 font-mono">
              {painel.totalResolvidas ?? 0}
            </p>
          </div>

          {Object.entries(TIPOS_META).map(([key, meta]) => {
            const count = painel.abertasPorTipo?.[key] ?? 0;
            const ativo = tipoFiltro === key;
            return (
              <div
                key={key}
                onClick={() => setTipoFiltro(ativo ? '' : key)}
                className={`rounded-xl p-3 border cursor-pointer transition-all ${
                  ativo
                    ? 'bg-indigo-950/50 border-indigo-400 ring-1 ring-indigo-400'
                    : 'bg-slate-950/60 border-slate-800 hover:border-slate-700'
                }`}
              >
                <span className="text-[11px] font-medium text-slate-400 block truncate">
                  {meta.label}
                </span>
                <p className="text-xl font-bold text-slate-100 mt-1 font-mono">{count}</p>
              </div>
            );
          })}
        </div>
      </div>

      {/* Barra de Filtros da Fila (Critério 2) */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-5 shadow-xl backdrop-blur-xl">
        <div className="flex items-center gap-2 mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400">
          <Filter size={14} className="text-indigo-400" />
          <span>Filtros da Fila (Tipo, Situação, Colaborador e Período)</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          <div>
            <label className="block text-xs text-slate-400 mb-1">Situação</label>
            <select
              value={situacaoFiltro}
              onChange={(e) => setSituacaoFiltro(e.target.value)}
              className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
            >
              <option value="ABERTA">Fila Aberta (Pendentes)</option>
              <option value="RESOLVIDA">Histórico de Resolvidas</option>
              <option value="TODAS">Todas (Abertas + Resolvidas)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Tipo de Inconsistência</label>
            <select
              value={tipoFiltro}
              onChange={(e) => setTipoFiltro(e.target.value)}
              className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
            >
              <option value="">Todos os 6 tipos</option>
              {Object.entries(TIPOS_META).map(([key, meta]) => (
                <option key={key} value={key}>
                  {meta.label} ({key})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Colaborador</label>
            <select
              value={funcionarioFiltro}
              onChange={(e) => setFuncionarioFiltro(e.target.value)}
              className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
            >
              <option value="">Todos os colaboradores</option>
              {funcionarios.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.nomeCompleto} ({f.matricula || f.usuario})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Data Início</label>
            <input
              type="date"
              value={dataInicioFiltro}
              onChange={(e) => setDataInicioFiltro(e.target.value)}
              className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 font-mono focus:border-indigo-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Data Fim</label>
            <input
              type="date"
              value={dataFimFiltro}
              onChange={(e) => setDataFimFiltro(e.target.value)}
              className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 font-mono focus:border-indigo-500 focus:outline-none"
            />
          </div>
        </div>
      </div>

      {/* Lista de Inconsistências */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-bold text-white">
            {situacaoFiltro === 'ABERTA'
              ? 'Pendências Abertas para Tratamento'
              : situacaoFiltro === 'RESOLVIDA'
              ? 'Histórico de Pendências Resolvidas (Auditável)'
              : 'Todas as Inconsistências Registradas'}{' '}
            <span className="ml-2 px-2 py-0.5 text-xs rounded-full bg-slate-800 text-slate-300 font-mono">
              {inconsistencias.length}
            </span>
          </h3>
        </div>

        {loading ? (
          <div className="py-12 text-center text-slate-400 text-sm">
            Carregando fila de inconsistências...
          </div>
        ) : inconsistencias.length === 0 ? (
          <div className="py-12 text-center border border-dashed border-slate-800 rounded-xl">
            <CheckCircle2 size={32} className="mx-auto text-emerald-400 mb-2" />
            <p className="text-sm font-semibold text-slate-200">
              Nenhum registro encontrado para os filtros selecionados.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-[11px] uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-3">Tipo / Situação</th>
                  <th className="py-3 px-3">Data Ref.</th>
                  <th className="py-3 px-3">Colaborador / Equipamento</th>
                  <th className="py-3 px-3">Origem da Pendência</th>
                  <th className="py-3 px-3">Resolução / Ação</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/70 text-xs">
                {inconsistencias.map((item) => {
                  const meta = TIPOS_META[item.tipo] || {
                    label: item.tipo,
                    badgeClass: 'bg-slate-800 text-slate-300 border-slate-700',
                  };
                  const resolvida = item.situacao === 'RESOLVIDA';

                  return (
                    <tr key={item.id} className="hover:bg-slate-950/40 transition-colors">
                      <td className="py-3.5 px-3 align-top">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold border ${meta.badgeClass}`}
                        >
                          {meta.label}
                        </span>
                        <div className="mt-1.5">
                          {resolvida ? (
                            <span className="inline-flex items-center gap-1 text-[11px] text-emerald-400 font-semibold">
                              <CheckCircle2 size={12} /> RESOLVIDA
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[11px] text-amber-400 font-semibold">
                              <AlertTriangle size={12} /> ABERTA
                            </span>
                          )}
                        </div>
                      </td>

                      <td className="py-3.5 px-3 align-top font-mono text-slate-200">
                        <div>{item.dataReferencia || '—'}</div>
                        <div className="text-[11px] text-slate-400 mt-0.5">
                          {formatarDataHora(item.dataHoraOrigem)}
                        </div>
                      </td>

                      <td className="py-3.5 px-3 align-top">
                        {item.funcionarioNome ? (
                          <div className="font-semibold text-slate-100">
                            {item.funcionarioNome}
                            {item.funcionarioMatricula && (
                              <span className="ml-1.5 text-[11px] font-mono text-slate-400">
                                ({item.funcionarioMatricula})
                              </span>
                            )}
                          </div>
                        ) : (
                          <div className="text-amber-300 font-mono text-[11px]">
                            ID Recebido: {item.identificadorRecebido || 'Não vinculado'}
                          </div>
                        )}
                        {item.equipamentoIdentificacao && (
                          <div className="text-[11px] text-slate-400 mt-0.5">
                            Equip.: {item.equipamentoIdentificacao}
                          </div>
                        )}
                        {item.nomeArquivo && (
                          <div className="text-[11px] text-slate-400 font-mono">
                            Arq.: {item.nomeArquivo}
                          </div>
                        )}
                      </td>

                      <td className="py-3.5 px-3 align-top max-w-md text-slate-300">
                        {item.descricaoOrigem}
                      </td>

                      <td className="py-3.5 px-3 align-top">
                        {resolvida ? (
                          <div className="rounded-xl bg-emerald-950/20 border border-emerald-500/20 p-2.5 space-y-1">
                            <div className="font-semibold text-emerald-300">
                              Ação: {item.acaoTomada}
                            </div>
                            <div className="text-slate-300">
                              <strong>Motivo:</strong> {item.motivoTratamento}
                            </div>
                            {item.detalheResolucao && (
                              <div className="text-[11px] text-slate-400">
                                {item.detalheResolucao}
                              </div>
                            )}
                            <div className="text-[11px] text-slate-400 font-mono">
                              Por {item.tratadoPor} em {formatarDataHora(item.tratadoEm)}
                            </div>
                          </div>
                        ) : (
                          <button
                            type="button"
                            onClick={() => abrirModalTratamento(item)}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-xs font-semibold text-white transition-all cursor-pointer"
                          >
                            <ShieldCheck size={14} />
                            <span>Resolver</span>
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Histórico de Fechamentos de Período */}
      {fechamentos.length > 0 && (
        <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
          <h3 className="text-sm font-bold text-white mb-3 flex items-center gap-2">
            <FileText size={16} className="text-indigo-400" />
            <span>Histórico de Fechamentos de Período</span>
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {fechamentos.map((f) => (
              <div
                key={f.id}
                className={`rounded-xl p-3.5 border ${
                  f.fechadoComRessalva
                    ? 'bg-amber-950/20 border-amber-500/30'
                    : 'bg-emerald-950/20 border-emerald-500/30'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-mono font-bold text-sm text-white">
                    Período {f.mesReferencia} ({f.dataInicio} a {f.dataFim})
                  </span>
                  <span
                    className={`px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                      f.fechadoComRessalva
                        ? 'bg-amber-500/20 text-amber-300'
                        : 'bg-emerald-500/20 text-emerald-300'
                    }`}
                  >
                    {f.status}
                  </span>
                </div>
                <p className="text-xs text-slate-300 mt-1.5">{f.avisoPendencias}</p>
                <p className="text-[11px] text-slate-400 mt-1 font-mono">
                  Fechado por {f.fechadoPor} em {formatarDataHora(f.dataFechamento)}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Modal Nativo <dialog> de Tratamento de Inconsistência */}
      <dialog
        ref={dialogTratarRef}
        aria-labelledby={dialogTratarTitleId}
        className="w-full max-w-lg rounded-2xl bg-slate-900 border border-slate-800 p-6 text-slate-100 shadow-2xl m-auto"
      >
        {itemSelecionado && (
          <form onSubmit={handleConfirmarTratamento} className="space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 id={dialogTratarTitleId} className="text-base font-bold text-white">
                Resolver Inconsistência ({TIPOS_META[itemSelecionado.tipo]?.label || itemSelecionado.tipo})
              </h3>
              <button
                type="button"
                onClick={fecharModalTratamento}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 cursor-pointer"
              >
                <X size={18} />
              </button>
            </div>

            <div className="rounded-xl bg-slate-950/80 border border-slate-800 p-3 text-xs text-slate-300">
              <div className="font-semibold text-slate-100 mb-1">
                Data Ref.: {itemSelecionado.dataReferencia}
              </div>
              <div>{itemSelecionado.descricaoOrigem}</div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Ação de Resolução *
              </label>
              <select
                value={acaoEscolhida}
                onChange={(e) => setAcaoEscolhida(e.target.value)}
                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
              >
                {Object.entries(ACOES_LABELS).map(([val, label]) => (
                  <option key={val} value={val}>
                    {label}
                  </option>
                ))}
              </select>
            </div>

            {acaoEscolhida === 'VINCULAR' &&
              itemSelecionado.tipo === 'EQUIPAMENTO_DESCONHECIDO' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Equipamento para Vincular *
                  </label>
                  <select
                    value={equipamentoIdTratamento}
                    onChange={(e) => setEquipamentoIdTratamento(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
                  >
                    <option value="">Selecione o equipamento...</option>
                    {equipamentos.map((eq) => (
                      <option key={eq.id} value={eq.id}>
                        {eq.identificacao} ({eq.codigo})
                      </option>
                    ))}
                  </select>
                </div>
              )}

            {acaoEscolhida === 'VINCULAR' &&
              itemSelecionado.tipo !== 'EQUIPAMENTO_DESCONHECIDO' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Colaborador para Vincular *
                  </label>
                  <select
                    value={funcionarioIdTratamento}
                    onChange={(e) => setFuncionarioIdTratamento(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
                  >
                    <option value="">Selecione o colaborador...</option>
                    {funcionarios.map((f) => (
                      <option key={f.id} value={f.id}>
                        {f.nomeCompleto} ({f.matricula || f.usuario})
                      </option>
                    ))}
                  </select>
                </div>
              )}

            {acaoEscolhida === 'INCLUIR_MARCACAO_AJUSTE' && (
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Horário do Ajuste *
                  </label>
                  <input
                    type="time"
                    value={horarioAjusteTratamento}
                    onChange={(e) => setHorarioAjusteTratamento(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 font-mono focus:border-indigo-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Tipo do Registro *
                  </label>
                  <select
                    value={tipoRegistroAjuste}
                    onChange={(e) => setTipoRegistroAjuste(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
                  >
                    <option value="SAIDA">SAÍDA</option>
                    <option value="ENTRADA">ENTRADA</option>
                  </select>
                </div>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Motivo do Tratamento (Obrigatório para Auditoria) *
              </label>
              <textarea
                rows={3}
                required
                value={motivoTratamento}
                onChange={(e) => setMotivoTratamento(e.target.value)}
                placeholder="Informe a justificativa do tratamento..."
                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-800">
              <button
                type="button"
                onClick={fecharModalTratamento}
                className="px-4 py-2 rounded-xl border border-slate-700 bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={submittingTratamento}
                className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-xs font-semibold text-white cursor-pointer disabled:opacity-50"
              >
                {submittingTratamento ? 'Registrando...' : 'Confirmar Resolução'}
              </button>
            </div>
          </form>
        )}
      </dialog>

      {/* Modal Nativo <dialog> de Bloqueio / Ressalva no Fechamento de Período */}
      <dialog
        ref={dialogFechamentoRef}
        aria-labelledby={dialogFechamentoTitleId}
        className="w-full max-w-lg rounded-2xl bg-slate-900 border border-amber-500/40 p-6 text-slate-100 shadow-2xl m-auto"
      >
        <div className="space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div className="flex items-center gap-2 text-amber-400">
              <AlertTriangle size={20} />
              <h3 id={dialogFechamentoTitleId} className="text-base font-bold text-white">
                Fechamento Bloqueado por Pendências Abertas
              </h3>
            </div>
            <button
              type="button"
              onClick={() => dialogFechamentoRef.current?.close()}
              className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 cursor-pointer"
            >
              <X size={18} />
            </button>
          </div>

          <div className="rounded-xl bg-amber-950/30 border border-amber-500/30 p-3.5 text-xs text-amber-200">
            {bloqueioFechamentoMsg}
          </div>

          <p className="text-xs text-slate-300">
            Você pode voltar para a fila e resolver cada pendência antes de fechar, ou confirmar o fechamento com ressalva (o aviso do que ficou pendente será gravado no registro do fechamento).
          </p>

          <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={() => dialogFechamentoRef.current?.close()}
              className="px-4 py-2 rounded-xl border border-slate-700 bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 cursor-pointer"
            >
              Voltar e Resolver Pendências
            </button>
            <button
              type="button"
              disabled={submittingFechamento}
              onClick={() => handleTentarFecharPeriodo(true)}
              className="px-4 py-2 rounded-xl bg-amber-600 hover:bg-amber-500 text-xs font-semibold text-white cursor-pointer disabled:opacity-50"
            >
              {submittingFechamento ? 'Fechando...' : 'Fechar Período Registrando Aviso'}
            </button>
          </div>
        </div>
      </dialog>
    </div>
  );
}
