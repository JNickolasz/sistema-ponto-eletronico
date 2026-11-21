import React, { useEffect, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Loader2,
  Lock,
  LockOpen,
  RefreshCw,
} from 'lucide-react';
import toast from 'react-hot-toast';
import fechamentoService from '../../services/fechamentoService';
import { Badge, Button, Card, EmptyState, PageHeader } from '../../components/ui/Primitives';
import Modal from '../../components/ui/Modal';

function mesAtualISO() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

export default function FechamentoPeriodoSection() {
  const [competencia, setCompetencia] = useState(mesAtualISO);
  const [previa, setPrevia] = useState(null);
  const [historico, setHistorico] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [observacao, setObservacao] = useState('');
  const [confirmarComPendencias, setConfirmarComPendencias] = useState(false);
  const [modalReabrir, setModalReabrir] = useState(false);
  const [motivoReabertura, setMotivoReabertura] = useState('');

  const carregarTudo = async (comp = competencia) => {
    try {
      setLoading(true);
      const [prev, hist] = await Promise.all([
        fechamentoService.consultarPrevia(comp).catch(() => null),
        fechamentoService.listarFechamentos().catch(() => []),
      ]);
      setPrevia(prev);
      setHistorico(Array.isArray(hist) ? hist : []);
    } catch (err) {
      toast.error(err?.message || 'Erro ao carregar fechamento de período.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarTudo(competencia);
  }, [competencia]);

  const handleFechar = async () => {
    try {
      setSubmitting(true);
      await fechamentoService.fecharPeriodo({
        competencia,
        confirmarComPendencias,
        observacao: observacao.trim(),
      });
      toast.success(`Competência ${competencia} fechada com sucesso!`);
      setObservacao('');
      setConfirmarComPendencias(false);
      await carregarTudo(competencia);
    } catch (err) {
      toast.error(err?.message || 'Não foi possível fechar o período.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReabrir = async (e) => {
    e.preventDefault();
    if (!motivoReabertura.trim()) {
      toast.error('Informe a justificativa obrigatória para reabertura auditada.');
      return;
    }
    try {
      setSubmitting(true);
      await fechamentoService.reabrirPeriodo({
        competencia,
        motivo: motivoReabertura.trim(),
      });
      toast.success(`Competência ${competencia} reaberta com registro de auditoria.`);
      setModalReabrir(false);
      setMotivoReabertura('');
      await carregarTudo(competencia);
    } catch (err) {
      toast.error(err?.message || 'Falha ao reabrir período.');
    } finally {
      setSubmitting(false);
    }
  };

  const estaFechado =
    previa?.status === 'CONCLUIDO' ||
    previa?.status === 'CONCLUIDO_COM_PENDENCIAS' ||
    previa?.fechado === true;

  const totalPendencias =
    (previa?.inconsistenciasAbertas ?? 0) +
    (previa?.batidasOrfasPendentes ?? 0) +
    (previa?.solicitacoesPendentes ?? 0);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Fechamento de Período"
        description="Congele a competência mensal após validar pendências, impedindo alterações retroativas em batidas e ocorrências."
        actions={
          <div className="flex items-center gap-2">
            <input
              type="month"
              value={competencia}
              onChange={(e) => setCompetencia(e.target.value)}
              className="h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
            />
            <Button variant="secondary" size="sm" onClick={() => carregarTudo(competencia)} disabled={loading}>
              <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
              Atualizar
            </Button>
          </div>
        }
      />

      {loading ? (
        <Card className="p-12 flex justify-center">
          <Loader2 size={24} className="animate-spin text-[var(--text-muted)]" />
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Checklist & Action */}
          <Card className="lg:col-span-7 p-6 space-y-5">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-sm font-semibold">Prévia da Competência {competencia}</h2>
                <p className="text-xs text-[var(--text-secondary)] mt-0.5">
                  Verificação de inconsistências, batidas órfãs e solicitações em aberto.
                </p>
              </div>
              <Badge tone={estaFechado ? 'success' : totalPendencias > 0 ? 'warning' : 'brand'}>
                {estaFechado ? <Lock size={11} /> : <LockOpen size={11} />}
                {estaFechado ? 'Fechado' : 'Aberto'}
              </Badge>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3.5">
                <div className="text-[11px] text-[var(--text-muted)]">Inconsistências abertas</div>
                <div className="text-2xl font-bold font-mono-tabular mt-1">
                  {previa?.inconsistenciasAbertas ?? 0}
                </div>
              </div>
              <div className="rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3.5">
                <div className="text-[11px] text-[var(--text-muted)]">Batidas órfãs</div>
                <div className="text-2xl font-bold font-mono-tabular mt-1">
                  {previa?.batidasOrfasPendentes ?? 0}
                </div>
              </div>
              <div className="rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3.5">
                <div className="text-[11px] text-[var(--text-muted)]">Ajustes pendentes</div>
                <div className="text-2xl font-bold font-mono-tabular mt-1">
                  {previa?.solicitacoesPendentes ?? 0}
                </div>
              </div>
            </div>

            {!estaFechado ? (
              <div className="space-y-4 pt-2 border-t border-[var(--border-subtle)]">
                {totalPendencias > 0 && (
                  <label className="flex items-start gap-2.5 rounded-lg border border-amber-200 dark:border-amber-800/60 bg-amber-50/70 dark:bg-amber-950/30 p-3 text-xs cursor-pointer">
                    <input
                      type="checkbox"
                      checked={confirmarComPendencias}
                      onChange={(e) => setConfirmarComPendencias(e.target.checked)}
                      className="mt-0.5"
                    />
                    <span className="text-amber-900 dark:text-amber-200">
                      <AlertTriangle size={13} className="inline mr-1" />
                      Estou ciente das {totalPendencias} pendência(s) aberta(s) e autorizo o fechamento com ressalva.
                    </span>
                  </label>
                )}

                <div className="space-y-1.5">
                  <label className="block text-xs font-medium">Observação de fechamento (opcional)</label>
                  <textarea
                    rows={2}
                    value={observacao}
                    onChange={(e) => setObservacao(e.target.value)}
                    placeholder="Ex: Conferido com lideranças e liberado para folha."
                    className="w-full rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] p-2.5 text-sm"
                  />
                </div>

                <Button
                  variant="primary"
                  onClick={handleFechar}
                  disabled={submitting || (totalPendencias > 0 && !confirmarComPendencias)}
                >
                  {submitting ? <Loader2 size={15} className="animate-spin" /> : <Lock size={15} />}
                  Fechar Competência {competencia}
                </Button>
              </div>
            ) : (
              <div className="pt-3 border-t border-[var(--border-subtle)] flex items-center justify-between gap-4">
                <div className="flex items-center gap-2 text-xs text-emerald-700 dark:text-emerald-400">
                  <CheckCircle2 size={16} />
                  Competência bloqueada para alterações e pronta para exportação da folha.
                </div>
                <Button variant="secondary" size="sm" onClick={() => setModalReabrir(true)}>
                  <LockOpen size={14} />
                  Reabrir Período
                </Button>
              </div>
            )}
          </Card>

          {/* History */}
          <Card className="lg:col-span-5 p-6">
            <h2 className="text-sm font-semibold mb-4">Histórico de Competências</h2>
            {historico.length === 0 ? (
              <EmptyState
                icon={Lock}
                title="Nenhum fechamento registrado"
                description="As competências fechadas aparecerão aqui com status e data."
              />
            ) : (
              <div className="divide-y divide-[var(--border-subtle)] border border-[var(--border-subtle)] rounded-xl overflow-hidden">
                {historico.map((item) => (
                  <div key={item.id || item.mesReferencia} className="p-3.5 flex items-center justify-between text-xs">
                    <div>
                      <div className="font-semibold font-mono-tabular">
                        {item.mesReferencia || item.competencia}
                      </div>
                      <div className="text-[11px] text-[var(--text-muted)] mt-0.5">
                        {item.responsavel || 'RH'}
                      </div>
                    </div>
                    <Badge
                      tone={
                        item.status === 'REABERTO'
                          ? 'warning'
                          : item.status === 'CONCLUIDO_COM_PENDENCIAS'
                          ? 'brand'
                          : 'success'
                      }
                    >
                      {item.status}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      )}

      <Modal
        open={modalReabrir}
        onClose={() => setModalReabrir(false)}
        title={`Reabrir Competência ${competencia}`}
        subtitle="A reabertura fica registrada permanentemente na trilha de auditoria."
      >
        <form onSubmit={handleReabrir} className="space-y-4">
          <div className="space-y-1.5">
            <label className="block text-xs font-medium">Motivo da reabertura *</label>
            <textarea
              rows={3}
              required
              autoFocus
              value={motivoReabertura}
              onChange={(e) => setMotivoReabertura(e.target.value)}
              placeholder="Descreva o motivo para reabrir o período fechado..."
              className="w-full rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] p-2.5 text-sm"
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setModalReabrir(false)}>
              Cancelar
            </Button>
            <Button type="submit" variant="danger" disabled={submitting}>
              {submitting ? <Loader2 size={14} className="animate-spin" /> : <LockOpen size={14} />}
              Confirmar Reabertura
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
