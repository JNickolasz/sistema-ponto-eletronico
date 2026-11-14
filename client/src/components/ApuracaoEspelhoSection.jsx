import React, { useState, useEffect } from 'react';
import apuracaoService from '../services/apuracaoService';
import {
  Calendar,
  Clock,
  AlertTriangle,
  RefreshCw,
  Moon,
  ChevronRight,
  TrendingUp,
  UserCheck,
  CheckCircle2,
  XCircle,
  FileText
} from 'lucide-react';

function formatMinutos(minutos) {
  if (!minutos || minutos === 0) return '0h00';
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  return `${h}h${m.toString().padStart(2, '0')}`;
}

function formatDataBr(dataStr) {
  if (!dataStr) return '';
  const [ano, mes, dia] = dataStr.split('-');
  return `${dia}/${mes}/${ano}`;
}

const DIAS_SEMANA_NOMES = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];

export default function ApuracaoEspelhoSection({ funcionarioId, podeRecalcular = false }) {
  const [mesReferencia, setMesReferencia] = useState(() => {
    const hoje = new Date();
    const ano = hoje.getFullYear();
    const mes = String(hoje.getMonth() + 1).padStart(2, '0');
    return `${ano}-${mes}`;
  });

  const [dados, setDados] = useState(null);
  const [carregando, setCarregando] = useState(false);
  const [recalculando, setRecalculando] = useState(false);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');

  const carregarApuracao = async () => {
    setCarregando(true);
    setErro('');
    try {
      const resp = await apuracaoService.consultar({
        funcionarioId,
        mesReferencia,
      });
      setDados(resp);
    } catch (err) {
      setErro(err.response?.data?.message || 'Falha ao carregar apuração do período.');
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    carregarApuracao();
  }, [funcionarioId, mesReferencia]);

  const handleRecalcular = async () => {
    if (!dados) return;
    setRecalculando(true);
    setErro('');
    setSucesso('');
    try {
      const resp = await apuracaoService.recalcular({
        funcionarioId,
        dataInicio: dados.dataInicio,
        dataFim: dados.dataFim,
      });
      setDados(resp);
      setSucesso('Período recalculado com sucesso!');
      setTimeout(() => setSucesso(''), 4000);
    } catch (err) {
      setErro(err.response?.data?.message || 'Não foi possível recalcular o período.');
    } finally {
      setRecalculando(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'APURADO':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800">
            <CheckCircle2 className="w-3 h-3" /> Apurado
          </span>
        );
      case 'PENDENTE_INCONSISTENCIA':
        return (
          <span
            className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-900 border border-amber-300 shadow-sm"
            title="Dia ímpar ou pendência pendente de tratamento no espelho"
          >
            <AlertTriangle className="w-3 h-3 text-amber-600" /> Dia Ímpar / Pendente
          </span>
        );
      case 'FALTA':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-rose-100 text-rose-800">
            <XCircle className="w-3 h-3" /> Falta
          </span>
        );
      case 'FOLGA':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700">
            Folga / DSR
          </span>
        );
      case 'ABONADO':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
            Abonado
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
            {status}
          </span>
        );
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 space-y-6">
      {/* Header com Filtros e Ação */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-100 pb-4">
        <div>
          <h2 className="text-xl font-bold text-gray-800 flex items-center gap-2">
            <FileText className="w-6 h-6 text-indigo-600" />
            Espelho de Ponto & Apuração Diária
          </h2>
          <p className="text-sm text-gray-500">
            Pareamento de marcações, tolerância CLT, horas extras por faixas e redução noturna.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 bg-gray-50 border border-gray-200 rounded-lg px-3 py-1.5">
            <Calendar className="w-4 h-4 text-gray-500" />
            <input
              type="month"
              value={mesReferencia}
              onChange={(e) => setMesReferencia(e.target.value)}
              className="bg-transparent text-sm font-medium text-gray-700 outline-none cursor-pointer"
            />
          </div>

          {podeRecalcular && (
            <button
              onClick={handleRecalcular}
              disabled={recalculando || carregando}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors shadow-sm disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${recalculando ? 'animate-spin' : ''}`} />
              {recalculando ? 'Recalculando...' : 'Recalcular Período'}
            </button>
          )}
        </div>
      </div>

      {erro && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-lg text-rose-700 text-sm flex items-center gap-2">
          <AlertTriangle className="w-5 h-5 flex-shrink-0 text-rose-500" />
          <span>{erro}</span>
        </div>
      )}

      {sucesso && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-800 text-sm flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0 text-emerald-600" />
          <span>{sucesso}</span>
        </div>
      )}

      {/* Cards de Resumo */}
      {dados && (
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
          <div className="bg-slate-50 border border-slate-200/70 rounded-xl p-3">
            <span className="text-xs font-medium text-slate-500">Horas Previstas</span>
            <p className="text-lg font-bold text-slate-800 mt-0.5">
              {formatMinutos(dados.totalMinutosPrevistos)}
            </p>
          </div>

          <div className="bg-emerald-50/60 border border-emerald-200/70 rounded-xl p-3">
            <span className="text-xs font-medium text-emerald-700">Horas Trabalhadas</span>
            <p className="text-lg font-bold text-emerald-900 mt-0.5">
              {formatMinutos(dados.totalMinutosTrabalhados)}
            </p>
          </div>

          <div className="bg-indigo-50/60 border border-indigo-200/70 rounded-xl p-3">
            <span className="text-xs font-medium text-indigo-700">Horas Extras</span>
            <p className="text-lg font-bold text-indigo-900 mt-0.5">
              {formatMinutos(dados.totalMinutosHoraExtra)}
            </p>
          </div>

          <div className="bg-amber-50/60 border border-amber-200/70 rounded-xl p-3">
            <span className="text-xs font-medium text-amber-700">Atrasos / Saídas</span>
            <p className="text-lg font-bold text-amber-900 mt-0.5">
              {formatMinutos(dados.totalMinutosAtraso + dados.totalMinutosSaidaAntecipada)}
            </p>
          </div>

          <div className="bg-purple-50/60 border border-purple-200/70 rounded-xl p-3">
            <span className="text-xs font-medium text-purple-700 flex items-center gap-1">
              <Moon className="w-3.5 h-3.5" /> Adic. Noturno
            </span>
            <p className="text-lg font-bold text-purple-900 mt-0.5">
              {formatMinutos(dados.totalMinutosAdicionalNoturno)}
            </p>
          </div>

          <div className="bg-rose-50/60 border border-rose-200/70 rounded-xl p-3">
            <span className="text-xs font-medium text-rose-700">Faltas</span>
            <p className="text-lg font-bold text-rose-900 mt-0.5">
              {formatMinutos(dados.totalMinutosFalta)}
            </p>
          </div>
        </div>
      )}

      {/* Tabela do Espelho Diário */}
      <div className="overflow-x-auto border border-gray-100 rounded-xl shadow-xs">
        <table className="w-full text-left text-xs text-gray-600">
          <thead className="bg-gray-50 text-gray-700 uppercase font-semibold text-[11px] border-b border-gray-200">
            <tr>
              <th className="py-3 px-3">Data</th>
              <th className="py-3 px-3">Status</th>
              <th className="py-3 px-3">Pares Registrados</th>
              <th className="py-3 px-3 text-center">Intervalo</th>
              <th className="py-3 px-3 text-center">Trabalhado</th>
              <th className="py-3 px-3 text-center">Previsto</th>
              <th className="py-3 px-3 text-center">Atraso / Saída</th>
              <th className="py-3 px-3 text-center">Hora Extra</th>
              <th className="py-3 px-3 text-center">Adic. Noturno</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {carregando ? (
              <tr>
                <td colSpan="9" className="text-center py-8 text-gray-400">
                  <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-indigo-500" />
                  Carregando espelho de ponto...
                </td>
              </tr>
            ) : !dados || dados.dias.length === 0 ? (
              <tr>
                <td colSpan="9" className="text-center py-8 text-gray-400">
                  Nenhum dia apurado encontrado para este período.
                </td>
              </tr>
            ) : (
              dados.dias.map((dia) => {
                const ehImpar = dia.status === 'PENDENTE_INCONSISTENCIA';
                const ehFalta = dia.status === 'FALTA';

                return (
                  <tr
                    key={dia.dataReferencia}
                    className={`hover:bg-gray-50/80 transition-colors ${
                      ehImpar ? 'bg-amber-50/30' : ehFalta ? 'bg-rose-50/20' : ''
                    }`}
                  >
                    {/* Data */}
                    <td className="py-3 px-3 whitespace-nowrap font-medium text-gray-900">
                      <div>{formatDataBr(dia.dataReferencia)}</div>
                    </td>

                    {/* Status */}
                    <td className="py-3 px-3 whitespace-nowrap">
                      {getStatusBadge(dia.status)}
                    </td>

                    {/* Pares Formados */}
                    <td className="py-3 px-3 min-w-[200px]">
                      {ehImpar ? (
                        <span className="text-amber-800 font-medium italic">
                          Dia ímpar — aguardando tratamento
                        </span>
                      ) : dia.pares && dia.pares.length > 0 ? (
                        <div className="flex flex-wrap gap-1.5">
                          {dia.pares.map((p, idx) => (
                            <span
                              key={idx}
                              className="inline-flex items-center px-2 py-0.5 rounded bg-gray-100 text-gray-800 font-mono text-[11px]"
                            >
                              {p.entrada?.slice(0, 5)} - {p.saida?.slice(0, 5)}
                              <span className="ml-1 text-gray-400 font-sans">
                                ({formatMinutos(p.duracaoMinutos)})
                              </span>
                            </span>
                          ))}
                        </div>
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </td>

                    {/* Intervalo */}
                    <td className="py-3 px-3 text-center whitespace-nowrap">
                      {dia.minutosIntervalo > 0 ? formatMinutos(dia.minutosIntervalo) : '—'}
                    </td>

                    {/* Trabalhado */}
                    <td className="py-3 px-3 text-center whitespace-nowrap font-semibold">
                      {ehImpar ? (
                        <span className="text-gray-400">0h00</span>
                      ) : dia.minutosTrabalhados > 0 ? (
                        <span className="text-emerald-700">{formatMinutos(dia.minutosTrabalhados)}</span>
                      ) : (
                        <span className="text-gray-400">0h00</span>
                      )}
                    </td>

                    {/* Previsto */}
                    <td className="py-3 px-3 text-center whitespace-nowrap text-gray-500">
                      {formatMinutos(dia.minutosPrevistos)}
                    </td>

                    {/* Atraso / Saída Antecipada */}
                    <td className="py-3 px-3 text-center whitespace-nowrap">
                      {dia.minutosAtraso > 0 || dia.minutosSaidaAntecipada > 0 ? (
                        <span className="text-amber-700 font-medium">
                          {formatMinutos(dia.minutosAtraso + dia.minutosSaidaAntecipada)}
                        </span>
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </td>

                    {/* Hora Extra por Faixa */}
                    <td className="py-3 px-3 text-center whitespace-nowrap">
                      {dia.minutosHoraExtraTotal > 0 ? (
                        <div className="flex flex-col items-center gap-0.5">
                          <span className="text-indigo-700 font-semibold">
                            +{formatMinutos(dia.minutosHoraExtraTotal)}
                          </span>
                          {dia.horasExtrasPorFaixa && Object.keys(dia.horasExtrasPorFaixa).length > 0 && (
                            <div className="flex gap-1 text-[10px]">
                              {Object.entries(dia.horasExtrasPorFaixa).map(([faixa, mins]) => (
                                <span
                                  key={faixa}
                                  className="px-1.5 py-0.2 rounded bg-indigo-50 text-indigo-700 border border-indigo-100"
                                >
                                  {faixa}: {formatMinutos(mins)}
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </td>

                    {/* Adicional Noturno */}
                    <td className="py-3 px-3 text-center whitespace-nowrap">
                      {dia.minutosAdicionalNoturno > 0 ? (
                        <span
                          className="inline-flex items-center gap-1 text-purple-700 font-medium"
                          title="Minutos com redução ficta da hora noturna (52m30s)"
                        >
                          <Moon className="w-3 h-3" />
                          {formatMinutos(dia.minutosAdicionalNoturno)}
                        </span>
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
