import React, { useState } from 'react';
import { X, Calendar, Clock, FileText, Send, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';
import solicitacaoAjusteService from '../services/solicitacaoAjusteService';

export function SolicitarAjusteModal({ isOpen, onClose, onSuccess }) {
  const [dataAlvo, setDataAlvo] = useState(new Date().toISOString().split('T')[0]);
  const [horarioPretendido, setHorarioPretendido] = useState('08:00');
  const [tipoRegistro, setTipoRegistro] = useState('ENTRADA');
  const [motivo, setMotivo] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const hoje = new Date().toISOString().split('T')[0];

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!motivo.trim()) {
      toast.error('Informe o motivo ou justificativa da solicitação.');
      return;
    }

    if (dataAlvo > hoje) {
      toast.error('A data da solicitação não pode ser futura.');
      return;
    }

    try {
      setLoading(true);
      const payload = {
        dataAlvo,
        horarioPretendido: horarioPretendido.length === 5 ? `${horarioPretendido}:00` : horarioPretendido,
        tipoRegistro,
        motivo: motivo.trim(),
      };

      await solicitacaoAjusteService.abrirSolicitacao(payload);
      toast.success('Solicitação de ajuste enviada com sucesso ao seu gestor!', {
        duration: 4000,
        icon: '📝',
      });
      setMotivo('');
      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      const msg = err.message || 'Erro ao enviar solicitação de ajuste.';
      toast.error(msg, { duration: 5000 });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl p-6 sm:p-8 space-y-6 overflow-hidden">
        {/* Glow de fundo */}
        <div className="absolute top-0 right-0 w-36 h-36 bg-emerald-500/10 rounded-full blur-2xl pointer-events-none" />

        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
              <FileText size={22} />
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">Solicitar Ajuste de Ponto</h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Envie o pedido de inclusão de batida para análise do seu gestor.
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            disabled={loading}
            className="p-1.5 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X size={18} />
          </button>
        </div>

        {/* Formulário */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Data Alvo */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Data do Esquecimento / Ajuste
              </label>
              <div className="relative">
                <input
                  type="date"
                  max={hoje}
                  required
                  value={dataAlvo}
                  onChange={(e) => setDataAlvo(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-emerald-500 transition-colors font-mono"
                />
              </div>
              <span className="text-[10px] text-slate-500 mt-1 block">
                Períodos já fechados pelo RH serão recusados.
              </span>
            </div>

            {/* Horário Pretendido */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Horário da Marcação
              </label>
              <div className="relative">
                <input
                  type="time"
                  required
                  step="60"
                  value={horarioPretendido}
                  onChange={(e) => setHorarioPretendido(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-emerald-500 transition-colors font-mono"
                />
              </div>
            </div>
          </div>

          {/* Tipo de Registro */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              Tipo de Marcação
            </label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setTipoRegistro('ENTRADA')}
                className={`py-2 px-3 rounded-xl border text-xs font-semibold transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                  tipoRegistro === 'ENTRADA'
                    ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-300 shadow-sm shadow-emerald-500/20'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200'
                }`}
              >
                Entrada / Início de Turno
              </button>
              <button
                type="button"
                onClick={() => setTipoRegistro('SAIDA')}
                className={`py-2 px-3 rounded-xl border text-xs font-semibold transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                  tipoRegistro === 'SAIDA'
                    ? 'bg-blue-500/20 border-blue-500/40 text-blue-300 shadow-sm shadow-blue-500/20'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200'
                }`}
              >
                Saída / Término de Turno
              </button>
            </div>
          </div>

          {/* Motivo */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              Motivo ou Justificativa <span className="text-rose-400">*</span>
            </label>
            <textarea
              required
              rows={3}
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              placeholder="Ex: Esquecimento ao retornar do intervalo de almoço / Falta de sinal no celular ao chegar no cliente..."
              className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-xs text-white placeholder-slate-600 focus:outline-none focus:border-emerald-500 transition-colors resize-none"
            />
          </div>

          <div className="p-3 rounded-xl bg-slate-950/80 border border-slate-800 text-[11px] text-slate-400 flex items-start gap-2">
            <AlertTriangle size={15} className="text-amber-400 shrink-0 mt-0.5" />
            <span>
              Ao ser deferida pelo gestor, a batida será gravada com origem <strong>AJUSTE_MANUAL</strong> e gerará um comprovante com NSR sequencial (Portaria 671).
            </span>
          </div>

          {/* Botões */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2.5 rounded-xl border border-slate-700 text-xs font-semibold text-slate-300 hover:bg-slate-800 transition-all cursor-pointer disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-xs font-bold text-white shadow-lg shadow-emerald-600/20 transition-all cursor-pointer flex items-center gap-2 disabled:opacity-50"
            >
              {loading ? (
                <>
                  <div className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent" />
                  <span>Enviando...</span>
                </>
              ) : (
                <>
                  <Send size={14} />
                  <span>Submeter Solicitação</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default SolicitarAjusteModal;
