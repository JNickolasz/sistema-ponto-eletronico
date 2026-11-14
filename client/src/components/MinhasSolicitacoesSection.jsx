import React, { useState, useEffect } from 'react';
import {
  FileText,
  Clock,
  CheckCircle2,
  XCircle,
  RefreshCw,
  AlertCircle,
  PlusCircle,
  Calendar,
  UserCheck,
} from 'lucide-react';
import toast from 'react-hot-toast';
import solicitacaoAjusteService from '../services/solicitacaoAjusteService';

export function MinhasSolicitacoesSection({ onAbrirModal }) {
  const [solicitacoes, setSolicitacoes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const carregarSolicitacoes = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await solicitacaoAjusteService.listarMinhas();
      setSolicitacoes(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Erro ao carregar solicitações de ajuste.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarSolicitacoes();
  }, []);

  const formatarData = (dataStr) => {
    if (!dataStr) return '—';
    try {
      const [ano, mes, dia] = dataStr.split('-');
      return `${dia}/${mes}/${ano}`;
    } catch {
      return dataStr;
    }
  };

  const formatarHora = (horaStr) => {
    if (!horaStr) return '—';
    return horaStr.substring(0, 5);
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'DEFERIDO':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <CheckCircle2 size={13} /> Deferido
          </span>
        );
      case 'INDEFERIDO':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
            <XCircle size={13} /> Indeferido
          </span>
        );
      case 'PENDENTE':
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <Clock size={13} /> Pendente de Análise
          </span>
        );
    }
  };

  return (
    <div className="rounded-3xl bg-slate-900/60 border border-slate-800 p-6 sm:p-8 backdrop-blur-xl space-y-6">
      {/* Header da seção */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
            <FileText size={22} />
          </div>
          <div>
            <h2 className="text-lg font-bold text-white flex items-center gap-2">
              Minhas Solicitações de Ajuste
              <span className="text-xs px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 font-mono">
                {solicitacoes.length}
              </span>
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Histórico de pedidos de inclusão/correção de batida enviados ao seu gestor.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={carregarSolicitacoes}
            disabled={loading}
            className="p-2 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700 text-slate-300 transition-all cursor-pointer disabled:opacity-50"
            title="Atualizar histórico"
          >
            <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
          </button>
          <button
            type="button"
            onClick={onAbrirModal}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white text-xs font-bold shadow-lg shadow-emerald-600/20 transition-all cursor-pointer"
          >
            <PlusCircle size={15} />
            <span>Solicitar Ajuste</span>
          </button>
        </div>
      </div>

      {/* Conteúdo */}
      {loading ? (
        <div className="py-12 flex flex-col items-center justify-center text-slate-400 gap-3">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-indigo-500 border-t-transparent" />
          <span className="text-xs">Carregando histórico de solicitações...</span>
        </div>
      ) : error ? (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center gap-2.5">
          <AlertCircle size={16} className="shrink-0" />
          <span>{error}</span>
        </div>
      ) : solicitacoes.length === 0 ? (
        <div className="py-12 px-4 text-center rounded-2xl border border-dashed border-slate-800 text-slate-500 space-y-2">
          <Clock size={36} className="mx-auto text-slate-600" />
          <p className="text-sm font-medium text-slate-400">Você não possui solicitações de ajuste registradas.</p>
          <p className="text-xs text-slate-600">
            Caso tenha esquecido de bater o ponto em algum dia, clique em "Solicitar Ajuste" acima.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {solicitacoes.map((item) => (
            <div
              key={item.id}
              className="p-4 sm:p-5 rounded-2xl bg-slate-950/70 border border-slate-800/80 hover:border-slate-700/80 transition-all space-y-3"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex items-center gap-2.5">
                  <span className="p-1.5 rounded-lg bg-slate-800 text-slate-300">
                    <Calendar size={14} />
                  </span>
                  <div>
                    <span className="text-sm font-bold text-white font-mono">
                      {formatarData(item.dataAlvo)}
                    </span>
                    <span className="text-xs text-slate-400 ml-2">
                      às <strong className="font-mono text-emerald-400">{formatarHora(item.horarioPretendido)}</strong>
                    </span>
                  </div>
                  <span
                    className={`ml-2 text-[10px] uppercase font-bold px-2 py-0.5 rounded border ${
                      item.tipoRegistro === 'ENTRADA'
                        ? 'bg-emerald-950/60 border-emerald-800/40 text-emerald-300'
                        : 'bg-blue-950/60 border-blue-800/40 text-blue-300'
                    }`}
                  >
                    {item.tipoRegistro}
                  </span>
                </div>

                <div>{getStatusBadge(item.status)}</div>
              </div>

              {/* Justificativa do Colaborador */}
              <div className="text-xs text-slate-300 bg-slate-900/70 p-3 rounded-xl border border-slate-800/60">
                <span className="text-[11px] font-semibold text-slate-400 block mb-1">
                  Seu motivo:
                </span>
                {item.motivo}
              </div>

              {/* Retorno do Gestor se analisado */}
              {item.status !== 'PENDENTE' && (
                <div
                  className={`p-3 rounded-xl border text-xs space-y-1 ${
                    item.status === 'DEFERIDO'
                      ? 'bg-emerald-950/30 border-emerald-800/40 text-emerald-200'
                      : 'bg-rose-950/30 border-rose-800/40 text-rose-200'
                  }`}
                >
                  <div className="flex items-center justify-between text-[11px] font-semibold text-slate-400">
                    <span className="flex items-center gap-1.5">
                      <UserCheck size={13} />
                      Analisado por: <strong className="text-slate-200">{item.gestorNome || 'Gestor'}</strong>
                    </span>
                    {item.batidaCriadaId && (
                      <span className="text-emerald-400 font-mono text-[10px]">
                        ✓ Batida gerada no espelho
                      </span>
                    )}
                  </div>
                  <p className="italic">
                    "{item.justificativaGestor || 'Sem parecer registrado.'}"
                  </p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default MinhasSolicitacoesSection;
