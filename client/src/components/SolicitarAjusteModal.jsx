import React, { useState } from 'react';
import { AlertTriangle, Loader2, Send } from 'lucide-react';
import toast from 'react-hot-toast';
import solicitacaoAjusteService from '../services/solicitacaoAjusteService';
import Modal from './ui/Modal';
import { Button } from './ui/Primitives';

export function SolicitarAjusteModal({ isOpen, onClose, onSuccess }) {
  const [dataAlvo, setDataAlvo] = useState(new Date().toISOString().split('T')[0]);
  const [horarioPretendido, setHorarioPretendido] = useState('08:00');
  const [tipoRegistro, setTipoRegistro] = useState('ENTRADA');
  const [motivo, setMotivo] = useState('');
  const [loading, setLoading] = useState(false);

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
        horarioPretendido:
          horarioPretendido.length === 5 ? `${horarioPretendido}:00` : horarioPretendido,
        tipoRegistro,
        motivo: motivo.trim(),
      };

      await solicitacaoAjusteService.abrirSolicitacao(payload);
      toast.success('Solicitação de ajuste enviada com sucesso ao seu gestor!');
      setMotivo('');
      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      const msg = err.message || 'Erro ao enviar solicitação de ajuste.';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open={Boolean(isOpen)}
      onClose={() => !loading && onClose?.()}
      title="Solicitar Ajuste de Ponto"
      subtitle="Envie o pedido de inclusão de batida para análise do seu gestor imediato."
      maxWidth="max-w-lg"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* Data Alvo */}
          <div className="space-y-1.5">
            <label htmlFor="ajuste-data" className="block text-sm font-semibold text-[var(--text-primary)]">
              Data do Esquecimento / Ajuste
            </label>
            <input
              id="ajuste-data"
              name="dataAlvo"
              type="date"
              max={hoje}
              required
              value={dataAlvo}
              onChange={(e) => setDataAlvo(e.target.value)}
              className="w-full min-h-11 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3.5 text-sm text-[var(--text-primary)] font-mono-tabular focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
            />
            <span className="text-xs text-[var(--text-muted)] block">
              Períodos já fechados pelo RH serão recusados.
            </span>
          </div>

          {/* Horário Pretendido */}
          <div className="space-y-1.5">
            <label htmlFor="ajuste-hora" className="block text-sm font-semibold text-[var(--text-primary)]">
              Horário da Marcação
            </label>
            <input
              id="ajuste-hora"
              name="horarioPretendido"
              type="time"
              required
              step="60"
              value={horarioPretendido}
              onChange={(e) => setHorarioPretendido(e.target.value)}
              className="w-full min-h-11 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3.5 text-sm text-[var(--text-primary)] font-mono-tabular focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
            />
          </div>
        </div>

        {/* Tipo de Registro */}
        <div className="space-y-1.5">
          <label className="block text-sm font-semibold text-[var(--text-primary)]">
            Tipo de Marcação
          </label>
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setTipoRegistro('ENTRADA')}
              className={`min-h-11 px-3.5 rounded-xl border text-sm font-semibold transition-all cursor-pointer flex items-center justify-center ${
                tipoRegistro === 'ENTRADA'
                  ? 'border-transparent text-[var(--brand-fg)] shadow-xs'
                  : 'border-[var(--border-subtle)] bg-[var(--surface-subtle)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
              }`}
              style={
                tipoRegistro === 'ENTRADA' ? { backgroundColor: 'var(--brand-600)' } : undefined
              }
            >
              Entrada / Início de Turno
            </button>
            <button
              type="button"
              onClick={() => setTipoRegistro('SAIDA')}
              className={`min-h-11 px-3.5 rounded-xl border text-sm font-semibold transition-all cursor-pointer flex items-center justify-center ${
                tipoRegistro === 'SAIDA'
                  ? 'border-transparent text-[var(--brand-fg)] shadow-xs'
                  : 'border-[var(--border-subtle)] bg-[var(--surface-subtle)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
              }`}
              style={
                tipoRegistro === 'SAIDA' ? { backgroundColor: 'var(--brand-600)' } : undefined
              }
            >
              Saída / Término de Turno
            </button>
          </div>
        </div>

        {/* Motivo */}
        <div className="space-y-1.5">
          <label htmlFor="ajuste-motivo" className="block text-sm font-semibold text-[var(--text-primary)]">
            Motivo ou Justificativa <span className="text-red-600 dark:text-red-400">*</span>
          </label>
          <textarea
            id="ajuste-motivo"
            name="motivo"
            required
            rows={3}
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            placeholder="Ex: Esquecimento ao retornar do intervalo de almoço / Falta de sinal no celular ao chegar no cliente..."
            className="w-full rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] p-3.5 text-sm text-[var(--text-primary)] placeholder:text-[var(--text-muted)] focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
          />
        </div>

        <div className="p-3.5 rounded-xl bg-[var(--surface-subtle)] border border-[var(--border-subtle)] text-xs text-[var(--text-secondary)] flex items-start gap-2.5">
          <AlertTriangle size={16} className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
          <span>
            Ao ser deferida pelo gestor, a batida será gravada com origem{' '}
            <strong className="text-[var(--text-primary)]">AJUSTE_MANUAL</strong> e gerará um
            comprovante oficial com NSR sequencial (Portaria 671).
          </span>
        </div>

        {/* Botões */}
        <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-[var(--border-subtle)]">
          <Button type="button" variant="secondary" onClick={onClose} disabled={loading}>
            Cancelar
          </Button>
          <Button type="submit" variant="primary" disabled={loading}>
            {loading ? (
              <>
                <Loader2 size={16} className="animate-spin" />
                <span>Enviando...</span>
              </>
            ) : (
              <>
                <Send size={15} />
                <span>Submeter Solicitação</span>
              </>
            )}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default SolicitarAjusteModal;
