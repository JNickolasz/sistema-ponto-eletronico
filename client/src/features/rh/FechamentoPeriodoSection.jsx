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
import { formatarDataHora } from '../../utils/brazilianDocuments';
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

  const fechamentoAtual = historico.find(
    (h) => (h.competencia || h.mesReferencia) === competencia
  );

  const statusAtual =
    previa?.statusAtual ||
    previa?.status ||
    fechamentoAtual?.status ||
    'ABERTO';

  const estaFechado =
    previa?.periodoFechado === true ||
    statusAtual === 'CONCLUIDO' ||
    statusAtual === 'CONCLUIDO_COM_PENDENCIAS';

  const pendenciasLista = Array.isArray(previa?.pendenciasAbertas)
    ? previa.pendenciasAbertas
    : [];
  const totalPendencias =
    previa?.totalPendenciasAbertas ?? pendenciasLista.length;
  const qtdOrfas = pendenciasLista.filter((p) => p.tipo === 'ORFA').length;
  const qtdOutrasInconsistencias = Math.max(0, totalPendencias - qtdOrfas);

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
              className="min-h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
            />
            <Button
              variant="secondary"
              size="sm"
              onClick={() => carregarTudo(competencia)}
              disabled={loading}
            >
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
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="text-base font-bold">Prévia da Competência {competencia}</h2>
                <p className="text-xs text-[var(--text-secondary)] mt-0.5">
                  Verificação de pendências de apuração, batidas órfãs e bloqueio mensal.
                </p>
              </div>
              <Badge
                tone={
                  statusAtual === 'CONCLUIDO'
                    ? 'success'
                    : statusAtual === 'CONCLUIDO_COM_PENDENCIAS'
                      ? 'brand'
                      : totalPendencias > 0
                        ? 'warning'
                        : 'neutral'
                }
              >
                {estaFechado ? <Lock size={12} /> : <LockOpen size={12} />}
                {statusAtual === 'CONCLUIDO'
                  ? 'Fechado'
                  : statusAtual === 'CONCLUIDO_COM_PENDENCIAS'
                    ? 'Fechado c/ Pendências'
                    : 'Aberto'}
              </Badge>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3.5">
                <div className="text-xs text-[var(--text-muted)]">Total de pendências</div>
                <div className="text-2xl font-bold font-mono-tabular mt-1">{totalPendencias}</div>
              </div>
              <div className="rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3.5">
                <div className="text-xs text-[var(--text-muted)]">Inconsistências</div>
                <div className="text-2xl font-bold font-mono-tabular mt-1">
                  {qtdOutrasInconsistencias}
                </div>
              </div>
              <div className="rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3.5">
                <div className="text-xs text-[var(--text-muted)]">Batidas órfãs</div>
                <div className="text-2xl font-bold font-mono-tabular mt-1">{qtdOrfas}</div>
              </div>
            </div>

            {!estaFechado ? (
              <div className="space-y-4 pt-3 border-t border-[var(--border-subtle)]">
                {totalPendencias > 0 && (
                  <label className="flex items-start gap-2.5 rounded-xl border border-amber-200 dark:border-amber-800/60 bg-amber-50/70 dark:bg-amber-950/30 p-3.5 text-xs cursor-pointer">
                    <input
                      type="checkbox"
                      checked={confirmarComPendencias}
                      onChange={(e) => setConfirmarComPendencias(e.target.checked)}
                      className="mt-0.5"
                    />
                    <span className="text-amber-900 dark:text-amber-200">
                      <AlertTriangle size={14} className="inline mr-1" />
                      Estou ciente das <strong>{totalPendencias}</strong> pendência(s) aberta(s) e
                      autorizo o fechamento com ressalva auditada.
                    </span>
                  </label>
                )}

                <div className="space-y-1.5">
                  <label className="block text-xs font-semibold">
                    Observação de fechamento (opcional)
                  </label>
                  <textarea
                    rows={2}
                    value={observacao}
                    onChange={(e) => setObservacao(e.target.value)}
                    placeholder="Ex: Conferido com lideranças e liberado para folha."
                    className="w-full rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] p-3 text-sm"
                  />
                </div>

                <Button
                  variant="primary"
                  onClick={handleFechar}
                  disabled={submitting || (totalPendencias > 0 && !confirmarComPendencias)}
                >
                  {submitting ? <Loader2 size={16} className="animate-spin" /> : <Lock size={16} />}
                  Fechar Competência {competencia}
                </Button>
              </div>
            ) : (
              <div className="pt-4 border-t border-[var(--border-subtle)] space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <div className="flex items-start gap-2.5 text-xs text-emerald-700 dark:text-emerald-400">
                    <CheckCircle2 size={18} className="shrink-0 mt-0.5" />
                    <div>
                      <div className="font-bold text-sm">
                        Competência {competencia} congelada e bloqueada
                      </div>
                      <div className="text-[var(--text-secondary)] mt-0.5">
                        Alterações retroativas estão bloqueadas e a exportação da folha está liberada.
                      </div>
                    </div>
                  </div>
                  <Button variant="secondary" size="sm" onClick={() => setModalReabrir(true)}>
                    <LockOpen size={15} />
                    Reabrir Período
                  </Button>
                </div>

                {fechamentoAtual?.ressalvaOuAviso && (
                  <div className="p-3 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-subtle)] text-xs text-[var(--text-secondary)]">
                    <strong className="text-[var(--text-primary)]">Ressalva / Observação:</strong>{' '}
                    {fechamentoAtual.ressalvaOuAviso}
                  </div>
                )}
              </div>
            )}
          </Card>

          {/* History */}
          <Card className="lg:col-span-5 p-6">
            <h2 className="text-base font-bold mb-4">Histórico de Competências</h2>
            {historico.length === 0 ? (
              <EmptyState
                icon={Lock}
                title="Nenhum fechamento registrado"
                description="As competências fechadas aparecerão aqui com status e data."
              />
            ) : (
              <div className="divide-y divide-[var(--border-subtle)] border border-[var(--border-subtle)] rounded-xl overflow-hidden">
                {historico.map((item) => {
                  const compItem = item.competencia || item.mesReferencia;
                  const selecionada = compItem === competencia;
                  return (
                    <button
                      type="button"
                      key={item.id || compItem}
                      onClick={() => compItem && setCompetencia(compItem)}
                      className={`w-full text-left p-3.5 flex items-center justify-between gap-3 text-xs transition-colors cursor-pointer ${
                        selecionada
                          ? 'bg-[var(--surface-subtle)]'
                          : 'hover:bg-[var(--surface-hover)]'
                      }`}
                    >
                      <div className="min-w-0">
                        <div className="font-bold font-mono-tabular text-sm text-[var(--text-primary)]">
                          {compItem}
                        </div>
                        <div className="text-xs text-[var(--text-muted)] mt-0.5 truncate">
                          {item.fechadoPor || item.responsavel || 'RH'}
                          {item.fechadoEm ? ` · ${formatarDataHora(item.fechadoEm)}` : ''}
                        </div>
                      </div>
                      <Badge
                        tone={
                          item.status === 'ABERTO'
                            ? 'warning'
                            : item.status === 'CONCLUIDO_COM_PENDENCIAS'
                              ? 'brand'
                              : 'success'
                        }
                      >
                        {item.status === 'CONCLUIDO'
                          ? 'Fechado'
                          : item.status === 'CONCLUIDO_COM_PENDENCIAS'
                            ? 'Fechado c/ Pend.'
                            : item.status}
                      </Badge>
                    </button>
                  );
                })}
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
            <label className="block text-sm font-semibold">Motivo da reabertura *</label>
            <textarea
              rows={3}
              required
              autoFocus
              value={motivoReabertura}
              onChange={(e) => setMotivoReabertura(e.target.value)}
              placeholder="Descreva o motivo para reabrir o período fechado..."
              className="w-full rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] p-3 text-sm"
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setModalReabrir(false)}>
              Cancelar
            </Button>
            <Button type="submit" variant="danger" disabled={submitting}>
              {submitting ? <Loader2 size={15} className="animate-spin" /> : <LockOpen size={15} />}
              Confirmar Reabertura
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
