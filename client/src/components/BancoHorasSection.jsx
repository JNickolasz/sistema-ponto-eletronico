import React, { useState, useEffect } from 'react';
import bancoHorasService from '../services/bancoHorasService';
import {
  Clock,
  TrendingUp,
  TrendingDown,
  Lock,
  Plus,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  Calendar,
  Users,
  FileText,
  X,
  ArrowRight,
  UserCheck
} from 'lucide-react';

function formatSaldoMinutos(minutos, incluirSinalZero = false) {
  if (minutos == null || isNaN(minutos)) return '00:00';
  const abs = Math.abs(minutos);
  const h = Math.floor(abs / 60);
  const m = abs % 60;
  const str = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  if (minutos > 0) return `+${str}`;
  if (minutos < 0) return `-${str}`;
  return incluirSinalZero ? `+${str}` : str;
}

function formatDataBr(dataStr) {
  if (!dataStr) return '';
  const [ano, mes, dia] = dataStr.split('-');
  return `${dia}/${mes}/${ano}`;
}

export default function BancoHorasSection({
  funcionarioId = '',
  funcionarios = [],
  podeRegistrarCompensacao = false,
  exibirVisaoEquipe = false,
}) {
  const hoje = new Date();
  const primeiroDiaMes = `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}-01`;
  const ultimoDiaDate = new Date(hoje.getFullYear(), hoje.getMonth() + 1, 0);
  const ultimoDiaMes = `${ultimoDiaDate.getFullYear()}-${String(ultimoDiaDate.getMonth() + 1).padStart(2, '0')}-${String(ultimoDiaDate.getDate()).padStart(2, '0')}`;

  // Aba interna: 'extrato' | 'equipe'
  const [abaInterna, setAbaInterna] = useState(exibirVisaoEquipe && !funcionarioId ? 'equipe' : 'extrato');

  // Filtros de data e colaborador
  const [colaboradorSelecionadoId, setColaboradorSelecionadoId] = useState(funcionarioId || '');
  const [dataInicio, setDataInicio] = useState(primeiroDiaMes);
  const [dataFim, setDataFim] = useState(ultimoDiaMes);

  // Estado do Extrato Individual
  const [extrato, setExtrato] = useState(null);
  const [loadingExtrato, setLoadingExtrato] = useState(false);

  // Estado do Relatório de Equipe
  const [equipeSaldos, setEquipeSaldos] = useState([]);
  const [loadingEquipe, setLoadingEquipe] = useState(false);

  // Modal de Compensação
  const [modalCompAberto, setModalCompAberto] = useState(false);
  const [compFuncionarioId, setCompFuncionarioId] = useState('');
  const [compData, setCompData] = useState(new Date().toISOString().split('T')[0]);
  const [compMinutos, setCompMinutos] = useState(60);
  const [compMotivo, setCompMotivo] = useState('');
  const [salvandoComp, setSalvandoComp] = useState(false);

  // Alertas
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');

  useEffect(() => {
    if (funcionarioId) {
      setColaboradorSelecionadoId(funcionarioId);
    }
  }, [funcionarioId]);

  const carregarExtrato = async (alvoId = colaboradorSelecionadoId) => {
    if (!alvoId && !funcionarioId) return;
    setLoadingExtrato(true);
    setErro('');
    try {
      const resp = await bancoHorasService.consultarExtrato({
        funcionarioId: alvoId || funcionarioId,
        dataInicio,
        dataFim,
      });
      setExtrato(resp);
    } catch (err) {
      setErro(err.message || 'Não foi possível carregar o extrato de banco de horas.');
    } finally {
      setLoadingExtrato(false);
    }
  };

  const carregarEquipe = async () => {
    if (!exibirVisaoEquipe) return;
    setLoadingEquipe(true);
    setErro('');
    try {
      const resp = await bancoHorasService.consultarSaldoEquipe({
        dataInicio,
        dataFim,
      });
      setEquipeSaldos(Array.isArray(resp) ? resp : []);
    } catch (err) {
      setErro(err.message || 'Não foi possível carregar o relatório consolidado da equipe.');
    } finally {
      setLoadingEquipe(false);
    }
  };

  useEffect(() => {
    if (abaInterna === 'extrato' && (colaboradorSelecionadoId || funcionarioId)) {
      carregarExtrato(colaboradorSelecionadoId || funcionarioId);
    } else if (abaInterna === 'equipe' && exibirVisaoEquipe) {
      carregarEquipe();
    }
  }, [abaInterna, colaboradorSelecionadoId, funcionarioId]);

  const handleFiltrarPeriodo = (e) => {
    e.preventDefault();
    if (abaInterna === 'extrato') {
      carregarExtrato();
    } else {
      carregarEquipe();
    }
  };

  const abrirModalCompensacao = (colabId = '') => {
    setCompFuncionarioId(colabId || colaboradorSelecionadoId || funcionarioId || '');
    setCompData(new Date().toISOString().split('T')[0]);
    setCompMinutos(60);
    setCompMotivo('');
    setErro('');
    setModalCompAberto(true);
  };

  const handleRegistrarCompensacao = async (e) => {
    e.preventDefault();
    setErro('');
    setSucesso('');

    if (!compFuncionarioId) {
      setErro('Selecione o colaborador para registrar a compensação.');
      return;
    }
    if (!compData) {
      setErro('Informe a data da compensação.');
      return;
    }
    if (!compMinutos || Number(compMinutos) <= 0) {
      setErro('A quantidade de minutos compensados deve ser maior que zero.');
      return;
    }
    if (!compMotivo.trim()) {
      setErro('O motivo da compensação é obrigatório.');
      return;
    }

    setSalvandoComp(true);
    try {
      await bancoHorasService.registrarCompensacao({
        funcionarioId: compFuncionarioId,
        dataReferencia: compData,
        minutos: Number(compMinutos),
        motivo: compMotivo.trim(),
      });

      setSucesso('Compensação de horas registrada com sucesso no extrato!');
      setModalCompAberto(false);
      if (abaInterna === 'extrato') {
        carregarExtrato(compFuncionarioId);
      } else {
        carregarEquipe();
      }
    } catch (err) {
      setErro(err.message || 'Erro ao registrar compensação.');
    } finally {
      setSalvandoComp(false);
    }
  };

  const renderBadgeTipo = (tipo) => {
    switch (tipo) {
      case 'CREDITO_APURACAO':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
            <TrendingUp className="w-3 h-3" /> Crédito Apuração
          </span>
        );
      case 'DEBITO_APURACAO':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800">
            <TrendingDown className="w-3 h-3" /> Débito Apuração
          </span>
        );
      case 'COMPENSACAO':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-100 text-indigo-800">
            <Clock className="w-3 h-3" /> Compensação
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
            {tipo}
          </span>
        );
    }
  };

  // Lista unificada de colaboradores para o select (vinda de props ou do relatório de equipe)
  const opcoesColaboradores =
    funcionarios.length > 0
      ? funcionarios
      : equipeSaldos.map((s) => ({
          id: s.funcionarioId,
          nomeCompleto: s.nomeFuncionario,
          matricula: s.matricula,
        }));

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-6">
      {/* Cabeçalho */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 border-b border-gray-100 pb-4">
        <div>
          <h2 className="text-xl font-bold text-gray-800 flex items-center gap-2">
            <TrendingUp className="w-6 h-6 text-indigo-600" />
            Banco de Horas & Extrato de Compensação (T16)
          </h2>
          <p className="text-sm text-gray-500">
            Cálculo derivado da apuração diária, compensações justificadas e congelamento imutável por período fechado.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {exibirVisaoEquipe && (
            <div className="bg-gray-100 p-1 rounded-xl flex gap-1 text-xs font-semibold">
              <button
                type="button"
                onClick={() => setAbaInterna('equipe')}
                className={`px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 cursor-pointer ${
                  abaInterna === 'equipe'
                    ? 'bg-white text-indigo-600 shadow-xs'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                <Users className="w-3.5 h-3.5" /> Saldos da Equipe
              </button>
              <button
                type="button"
                onClick={() => setAbaInterna('extrato')}
                className={`px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 cursor-pointer ${
                  abaInterna === 'extrato'
                    ? 'bg-white text-indigo-600 shadow-xs'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                <FileText className="w-3.5 h-3.5" /> Extrato Individual
              </button>
            </div>
          )}

          {podeRegistrarCompensacao && (
            <button
              type="button"
              onClick={() => abrirModalCompensacao()}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-xl shadow-xs transition-colors cursor-pointer"
            >
              <Plus className="w-4 h-4" /> Registrar Compensação
            </button>
          )}
        </div>
      </div>

      {/* Barra de Filtros de Período e Colaborador */}
      <form
        onSubmit={handleFiltrarPeriodo}
        className="bg-gray-50 border border-gray-200/80 rounded-xl p-4 flex flex-wrap items-end gap-3"
      >
        {abaInterna === 'extrato' && opcoesColaboradores.length > 0 && (
          <div className="min-w-[240px] flex-1">
            <label className="block text-[11px] font-semibold text-gray-600 uppercase mb-1">
              Colaborador
            </label>
            <select
              value={colaboradorSelecionadoId}
              onChange={(e) => setColaboradorSelecionadoId(e.target.value)}
              className="w-full bg-white border border-gray-200 text-gray-800 text-xs rounded-lg px-3 py-2 focus:border-indigo-500 focus:outline-none"
            >
              <option value="">Selecione um colaborador...</option>
              {opcoesColaboradores.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.nomeCompleto} ({f.matricula})
                </option>
              ))}
            </select>
          </div>
        )}

        <div>
          <label className="block text-[11px] font-semibold text-gray-600 uppercase mb-1">
            Data Inicial
          </label>
          <input
            type="date"
            value={dataInicio}
            onChange={(e) => setDataInicio(e.target.value)}
            className="bg-white border border-gray-200 text-gray-800 text-xs rounded-lg px-3 py-2 focus:border-indigo-500 focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-[11px] font-semibold text-gray-600 uppercase mb-1">
            Data Final
          </label>
          <input
            type="date"
            value={dataFim}
            onChange={(e) => setDataFim(e.target.value)}
            className="bg-white border border-gray-200 text-gray-800 text-xs rounded-lg px-3 py-2 focus:border-indigo-500 focus:outline-none"
          />
        </div>

        <button
          type="submit"
          disabled={loadingExtrato || loadingEquipe}
          className="inline-flex items-center gap-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-lg transition-colors cursor-pointer disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loadingExtrato || loadingEquipe ? 'animate-spin' : ''}`} />
          Consultar Período
        </button>
      </form>

      {/* Alertas */}
      {erro && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-sm flex items-start justify-between gap-2">
          <div className="flex items-start gap-2">
            <AlertTriangle className="w-5 h-5 flex-shrink-0 text-rose-500 mt-0.5" />
            <span>{erro}</span>
          </div>
          <button onClick={() => setErro('')} className="text-rose-400 hover:text-rose-600">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {sucesso && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-sm flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 flex-shrink-0 text-emerald-600" />
            <span>{sucesso}</span>
          </div>
          <button onClick={() => setSucesso('')} className="text-emerald-400 hover:text-emerald-600">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* ======================= ABA 1: EXTRATO INDIVIDUAL ======================= */}
      {abaInterna === 'extrato' && (
        <div className="space-y-6">
          {!colaboradorSelecionadoId && !funcionarioId ? (
            <div className="p-10 text-center text-gray-400 border border-dashed border-gray-200 rounded-xl text-sm">
              Selecione um colaborador acima para consultar o extrato detalhado do banco de horas.
            </div>
          ) : (
            <>
              {/* 5 Cards de Resumo */}
              {extrato && (
                <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                  <div className="bg-slate-50 border border-slate-200/80 rounded-xl p-3.5">
                    <span className="text-xs font-medium text-slate-500">Saldo Inicial</span>
                    <p
                      className={`text-lg font-bold font-mono mt-0.5 ${
                        extrato.saldoInicialMinutos >= 0 ? 'text-slate-800' : 'text-rose-700'
                      }`}
                    >
                      {formatSaldoMinutos(extrato.saldoInicialMinutos, true)}
                    </p>
                    <span className="text-[10px] text-slate-400">Acumulado anterior</span>
                  </div>

                  <div className="bg-emerald-50/70 border border-emerald-200/80 rounded-xl p-3.5">
                    <span className="text-xs font-medium text-emerald-700">Créditos (+)</span>
                    <p className="text-lg font-bold font-mono text-emerald-800 mt-0.5">
                      +{formatSaldoMinutos(extrato.creditosPeriodoMinutos).replace(/^[+-]/, '')}
                    </p>
                    <span className="text-[10px] text-emerald-600">Horas excedentes apuradas</span>
                  </div>

                  <div className="bg-rose-50/70 border border-rose-200/80 rounded-xl p-3.5">
                    <span className="text-xs font-medium text-rose-700">Débitos (-)</span>
                    <p className="text-lg font-bold font-mono text-rose-800 mt-0.5">
                      -{formatSaldoMinutos(extrato.debitosPeriodoMinutos).replace(/^[+-]/, '')}
                    </p>
                    <span className="text-[10px] text-rose-600">Atrasos, saídas e faltas</span>
                  </div>

                  <div className="bg-indigo-50/70 border border-indigo-200/80 rounded-xl p-3.5">
                    <span className="text-xs font-medium text-indigo-700">Compensações (-)</span>
                    <p className="text-lg font-bold font-mono text-indigo-800 mt-0.5">
                      -{formatSaldoMinutos(extrato.compensacoesPeriodoMinutos).replace(/^[+-]/, '')}
                    </p>
                    <span className="text-[10px] text-indigo-600">Folgas e abatimentos</span>
                  </div>

                  <div
                    className={`rounded-xl p-3.5 border ${
                      extrato.saldoFinalMinutos >= 0
                        ? 'bg-emerald-600 text-white border-emerald-700'
                        : 'bg-rose-600 text-white border-rose-700'
                    }`}
                  >
                    <span className="text-xs font-semibold opacity-90">Saldo Final Acumulado</span>
                    <p className="text-xl font-bold font-mono mt-0.5">
                      {formatSaldoMinutos(extrato.saldoFinalMinutos, true)}
                    </p>
                    <span className="text-[10px] opacity-80">
                      {extrato.saldoFinalMinutos >= 0 ? 'Saldo positivo' : 'Saldo devedor'}
                    </span>
                  </div>
                </div>
              )}

              {/* Tabela do Extrato Dia a Dia */}
              <div className="overflow-x-auto border border-gray-100 rounded-xl shadow-xs">
                <table className="w-full text-left text-xs text-gray-600">
                  <thead className="bg-gray-50 text-gray-700 uppercase font-semibold text-[11px] border-b border-gray-200">
                    <tr>
                      <th className="py-3 px-4">Data</th>
                      <th className="py-3 px-4">Tipo de Evento</th>
                      <th className="py-3 px-4">Descrição / Origem</th>
                      <th className="py-3 px-4 text-center">Movimentação</th>
                      <th className="py-3 px-4 text-center">Saldo Acumulado</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 bg-white">
                    {loadingExtrato ? (
                      <tr>
                        <td colSpan="5" className="text-center py-8 text-gray-400">
                          <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-indigo-500" />
                          Carregando extrato de banco de horas...
                        </td>
                      </tr>
                    ) : !extrato || !extrato.movimentacoes || extrato.movimentacoes.length === 0 ? (
                      <tr>
                        <td colSpan="5" className="text-center py-8 text-gray-400">
                          Nenhuma movimentação de banco de horas registrada neste período.
                        </td>
                      </tr>
                    ) : (
                      extrato.movimentacoes.map((item) => {
                        const ehPositivo = item.minutos > 0;
                        return (
                          <tr key={item.id} className="hover:bg-gray-50/80 transition-colors">
                            <td className="py-3 px-4 whitespace-nowrap font-medium text-gray-900">
                              <div className="flex items-center gap-1.5">
                                {formatDataBr(item.dataReferencia)}
                                {item.periodoFechado && (
                                  <span
                                    className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 text-[10px] font-semibold"
                                    title="Período fechado e congelado — imune a alterações"
                                  >
                                    <Lock className="w-2.5 h-2.5" /> Congelado
                                  </span>
                                )}
                              </div>
                            </td>

                            <td className="py-3 px-4 whitespace-nowrap">{renderBadgeTipo(item.tipo)}</td>

                            <td className="py-3 px-4">
                              <div className="text-gray-800 font-medium">{item.descricao}</div>
                              {item.registradoPor && (
                                <span className="text-[10px] text-gray-400">
                                  Registrado por: {item.registradoPor}
                                </span>
                              )}
                            </td>

                            <td className="py-3 px-4 text-center whitespace-nowrap font-mono font-bold">
                              <span className={ehPositivo ? 'text-emerald-700' : 'text-rose-700'}>
                                {formatSaldoMinutos(item.minutos, true)}
                              </span>
                            </td>

                            <td className="py-3 px-4 text-center whitespace-nowrap font-mono font-bold">
                              <span
                                className={`px-2 py-0.5 rounded ${
                                  item.saldoAcumuladoMinutos >= 0
                                    ? 'bg-emerald-50 text-emerald-800'
                                    : 'bg-rose-50 text-rose-800'
                                }`}
                              >
                                {formatSaldoMinutos(item.saldoAcumuladoMinutos, true)}
                              </span>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      )}

      {/* ======================= ABA 2: VISÃO CONSOLIDADA DA EQUIPE ======================= */}
      {abaInterna === 'equipe' && exibirVisaoEquipe && (
        <div className="overflow-x-auto border border-gray-100 rounded-xl shadow-xs">
          <table className="w-full text-left text-xs text-gray-600">
            <thead className="bg-gray-50 text-gray-700 uppercase font-semibold text-[11px] border-b border-gray-200">
              <tr>
                <th className="py-3 px-4">Colaborador</th>
                <th className="py-3 px-4 text-center">Saldo Anterior</th>
                <th className="py-3 px-4 text-center">Créditos (+)</th>
                <th className="py-3 px-4 text-center">Débitos (-)</th>
                <th className="py-3 px-4 text-center">Compensações (-)</th>
                <th className="py-3 px-4 text-center">Saldo Acumulado Atual</th>
                <th className="py-3 px-4 text-center">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {loadingEquipe ? (
                <tr>
                  <td colSpan="7" className="text-center py-8 text-gray-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-indigo-500" />
                    Carregando saldos consolidados da equipe...
                  </td>
                </tr>
              ) : equipeSaldos.length === 0 ? (
                <tr>
                  <td colSpan="7" className="text-center py-8 text-gray-400">
                    Nenhum colaborador encontrado na equipe para este período.
                  </td>
                </tr>
              ) : (
                equipeSaldos.map((membro) => (
                  <tr key={membro.funcionarioId} className="hover:bg-gray-50/80 transition-colors">
                    <td className="py-3 px-4 whitespace-nowrap">
                      <div className="font-semibold text-gray-900">{membro.nomeFuncionario}</div>
                      <span className="text-[10px] text-gray-400">
                        {membro.matricula} {membro.cargo ? `· ${membro.cargo}` : ''}
                      </span>
                    </td>

                    <td className="py-3 px-4 text-center font-mono">
                      {formatSaldoMinutos(membro.saldoAnteriorMinutos, true)}
                    </td>

                    <td className="py-3 px-4 text-center font-mono text-emerald-700 font-semibold">
                      +{formatSaldoMinutos(membro.creditosPeriodoMinutos).replace(/^[+-]/, '')}
                    </td>

                    <td className="py-3 px-4 text-center font-mono text-rose-700 font-semibold">
                      -{formatSaldoMinutos(membro.debitosPeriodoMinutos).replace(/^[+-]/, '')}
                    </td>

                    <td className="py-3 px-4 text-center font-mono text-indigo-700 font-semibold">
                      -{formatSaldoMinutos(membro.compensacoesPeriodoMinutos).replace(/^[+-]/, '')}
                    </td>

                    <td className="py-3 px-4 text-center whitespace-nowrap font-mono font-bold">
                      <span
                        className={`px-2.5 py-1 rounded-full text-xs ${
                          membro.saldoFinalMinutos >= 0
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-rose-100 text-rose-800'
                        }`}
                      >
                        {formatSaldoMinutos(membro.saldoFinalMinutos, true)}
                      </span>
                    </td>

                    <td className="py-3 px-4 text-center whitespace-nowrap">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            setColaboradorSelecionadoId(membro.funcionarioId);
                            setAbaInterna('extrato');
                          }}
                          className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 font-semibold text-[11px] transition-colors cursor-pointer"
                        >
                          Ver Extrato <ArrowRight className="w-3 h-3" />
                        </button>
                        {podeRegistrarCompensacao && (
                          <button
                            type="button"
                            onClick={() => abrirModalCompensacao(membro.funcionarioId)}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-gray-100 text-gray-700 hover:bg-gray-200 font-medium text-[11px] transition-colors cursor-pointer"
                          >
                            Compensar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* ======================= MODAL DE REGISTRO DE COMPENSAÇÃO ======================= */}
      {modalCompAberto && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl shadow-xl border border-gray-100 w-full max-w-md p-6 space-y-4">
            <div className="flex justify-between items-center border-b border-gray-100 pb-3">
              <h3 className="text-base font-bold text-gray-800 flex items-center gap-2">
                <Clock className="w-5 h-5 text-indigo-600" />
                Registrar Compensação de Horas
              </h3>
              <button
                type="button"
                onClick={() => setModalCompAberto(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleRegistrarCompensacao} className="space-y-4">
              {opcoesColaboradores.length > 0 && (
                <div>
                  <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                    Colaborador *
                  </label>
                  <select
                    value={compFuncionarioId}
                    onChange={(e) => setCompFuncionarioId(e.target.value)}
                    required
                    className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl px-3.5 py-2 focus:border-indigo-500 focus:bg-white focus:outline-none"
                  >
                    <option value="">Selecione o colaborador...</option>
                    {opcoesColaboradores.map((f) => (
                      <option key={f.id} value={f.id}>
                        {f.nomeCompleto} ({f.matricula})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                  Data da Compensação *
                </label>
                <input
                  type="date"
                  value={compData}
                  onChange={(e) => setCompData(e.target.value)}
                  required
                  className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl px-3.5 py-2 focus:border-indigo-500 focus:bg-white focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                  Minutos a Abater do Saldo *
                </label>
                <input
                  type="number"
                  min="1"
                  max="1440"
                  value={compMinutos}
                  onChange={(e) => setCompMinutos(e.target.value)}
                  required
                  className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl px-3.5 py-2 focus:border-indigo-500 focus:bg-white focus:outline-none"
                />
                <span className="text-[11px] text-gray-500 mt-0.5 block">
                  Abatimento de -{formatSaldoMinutos(Number(compMinutos)).replace(/^[+-]/, '')} no banco de horas
                </span>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                  Motivo / Justificativa da Compensação *
                </label>
                <textarea
                  rows="2"
                  value={compMotivo}
                  onChange={(e) => setCompMotivo(e.target.value)}
                  placeholder="Ex: Folga compensatória acordada / Saída antecipada compensada..."
                  required
                  className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl p-3 focus:border-indigo-500 focus:bg-white focus:outline-none"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setModalCompAberto(false)}
                  className="px-4 py-2 text-xs font-medium text-gray-600 hover:bg-gray-100 rounded-xl transition-colors cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={salvandoComp}
                  className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl transition-colors shadow-xs disabled:opacity-50 cursor-pointer"
                >
                  {salvandoComp ? 'Registrando...' : 'Confirmar Compensação'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
