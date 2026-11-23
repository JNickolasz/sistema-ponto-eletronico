import React, { useEffect, useState } from 'react';
import {
  BarChart3,
  Check,
  Clock,
  FileCheck,
  Loader2,
  RefreshCw,
  Scale,
  X,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { useAuth } from '../hooks/useAuth';
import gestorService from '../services/gestorService';
import solicitacaoAjusteService from '../services/solicitacaoAjusteService';
import AppShell from '../layouts/AppShell';
import { Badge, Button, Card, EmptyState, PageHeader } from '../components/ui/Primitives';
import Modal from '../components/ui/Modal';
import BancoHorasSection from '../components/BancoHorasSection';
import RelatoriosSection from '../features/rh/RelatoriosSection';

export function GestorDashboard() {
  const { user } = useAuth();
  const [aba, setAba] = useState('solicitacoes');

  const [painelData, setPainelData] = useState(null);
  const [loadingPainel, setLoadingPainel] = useState(true);

  const [solicitacoes, setSolicitacoes] = useState([]);
  const [loadingSolicitacoes, setLoadingSolicitacoes] = useState(true);
  const [filtroStatus, setFiltroStatus] = useState('PENDENTE');

  const [modalAnalise, setModalAnalise] = useState({
    open: false,
    item: null,
    acao: 'DEFERIDO',
    justificativa: '',
    loading: false,
  });

  const carregarPainel = async () => {
    try {
      setLoadingPainel(true);
      const data = await gestorService.getPainel();
      setPainelData(data);
    } catch (err) {
      toast.error(err?.message || 'Erro ao carregar painel do gestor.');
    } finally {
      setLoadingPainel(false);
    }
  };

  const carregarSolicitacoes = async () => {
    try {
      setLoadingSolicitacoes(true);
      const statusParam = filtroStatus === 'TODAS' ? null : filtroStatus;
      const data = await solicitacaoAjusteService.listarEquipe(statusParam);
      setSolicitacoes(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error(err?.message || 'Erro ao carregar solicitações da equipe.');
    } finally {
      setLoadingSolicitacoes(false);
    }
  };

  useEffect(() => {
    carregarPainel();
  }, []);

  useEffect(() => {
    carregarSolicitacoes();
  }, [filtroStatus]);

  const handleConfirmarAnalise = async (e) => {
    e.preventDefault();
    if (!modalAnalise.justificativa.trim()) {
      toast.error('A justificativa da decisão é obrigatória.');
      return;
    }

    try {
      setModalAnalise((prev) => ({ ...prev, loading: true }));
      await solicitacaoAjusteService.analisarSolicitacao(modalAnalise.item.id, {
        status: modalAnalise.acao,
        justificativaGestor: modalAnalise.justificativa.trim(),
      });
      toast.success(
        modalAnalise.acao === 'DEFERIDO'
          ? 'Solicitação aprovada e apuração recalculada!'
          : 'Solicitação indeferida.'
      );
      setModalAnalise({ open: false, item: null, acao: 'DEFERIDO', justificativa: '', loading: false });
      await carregarSolicitacoes();
    } catch (err) {
      toast.error(err?.message || 'Erro ao registrar decisão.');
      setModalAnalise((prev) => ({ ...prev, loading: false }));
    }
  };

  const pendentesCount = solicitacoes.filter((s) => s.status === 'PENDENTE').length;

  const navItems = [
    { type: 'divider', key: 'div-gestor', label: 'Gestão de Equipe' },
    {
      key: 'solicitacoes',
      label: 'Solicitações de Ajuste',
      icon: FileCheck,
      badge: pendentesCount > 0 ? pendentesCount : undefined,
    },
    { key: 'banco', label: 'Banco de Horas da Equipe', icon: Scale },
    { key: 'relatorios', label: 'Relatórios da Equipe', icon: BarChart3 },
  ];

  return (
    <AppShell
      brandSubtitle="Portal do Gestor"
      navItems={navItems}
      activeKey={aba}
      onSelectNav={setAba}
    >
      {aba === 'solicitacoes' && (
        <div className="space-y-6">
          <PageHeader
            title={`Gestão de Equipe · ${painelData?.nomeCompleto || user?.usuario}`}
            description="Analise pedidos de ajuste de ponto dos seus liderados diretos com registro em trilha de auditoria."
            actions={
              <Button variant="secondary" size="sm" onClick={carregarSolicitacoes} disabled={loadingSolicitacoes}>
                <RefreshCw size={14} className={loadingSolicitacoes ? 'animate-spin' : ''} />
                Atualizar
              </Button>
            }
          />

          <div className="flex flex-wrap gap-2">
            {['PENDENTE', 'DEFERIDO', 'INDEFERIDO', 'TODAS'].map((st) => {
              const active = st === filtroStatus;
              return (
                <button
                  key={st}
                  type="button"
                  onClick={() => setFiltroStatus(st)}
                  className={`h-8 px-3 rounded-lg border text-xs font-medium transition-colors cursor-pointer ${
                    active
                      ? 'border-transparent text-[var(--brand-fg)]'
                      : 'border-[var(--border-subtle)] bg-[var(--surface-card)] text-[var(--text-secondary)] hover:bg-[var(--surface-hover)]'
                  }`}
                  style={active ? { backgroundColor: 'var(--brand-600)' } : undefined}
                >
                  {st === 'TODAS' ? 'Todas' : st}
                </button>
              );
            })}
          </div>

          <Card className="overflow-hidden">
            {loadingSolicitacoes || loadingPainel ? (
              <div className="py-12 flex justify-center">
                <Loader2 size={22} className="animate-spin text-[var(--text-muted)]" />
              </div>
            ) : solicitacoes.length === 0 ? (
              <EmptyState
                icon={Clock}
                title="Nenhuma solicitação neste filtro"
                description="Quando algum colaborador da sua equipe solicitar ajuste de marcação, ele aparecerá aqui."
              />
            ) : (
              <div className="divide-y divide-[var(--border-subtle)]">
                {solicitacoes.map((item) => (
                  <div
                    key={item.id}
                    className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-[var(--surface-hover)] transition-colors"
                  >
                    <div className="space-y-1 min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-semibold text-[var(--text-primary)]">
                          {item.nomeColaborador || item.colaboradorNome || 'Colaborador'}
                        </span>
                        <Badge
                          tone={
                            item.status === 'DEFERIDO'
                              ? 'success'
                              : item.status === 'INDEFERIDO'
                              ? 'danger'
                              : 'warning'
                          }
                        >
                          {item.status}
                        </Badge>
                        {item.tipoRegistro && <Badge tone="neutral">{item.tipoRegistro}</Badge>}
                      </div>
                      <div className="text-xs font-mono-tabular text-[var(--text-secondary)]">
                        Horário solicitado:{' '}
                        {item.dataAlvo
                          ? `${item.dataAlvo.split('-').reverse().join('/')} às ${String(item.horarioPretendido || '').slice(0, 5)}`
                          : item.dataHoraSolicitada
                            ? new Date(item.dataHoraSolicitada).toLocaleString('pt-BR')
                            : '—'}
                      </div>
                      <p className="text-xs text-[var(--text-secondary)]">
                        Justificativa: {item.motivo || item.justificativa || '—'}
                      </p>
                    </div>

                    {item.status === 'PENDENTE' && (
                      <div className="flex items-center gap-2 shrink-0">
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() =>
                            setModalAnalise({
                              open: true,
                              item,
                              acao: 'INDEFERIDO',
                              justificativa: '',
                              loading: false,
                            })
                          }
                        >
                          <X size={14} />
                          Indeferir
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() =>
                            setModalAnalise({
                              open: true,
                              item,
                              acao: 'DEFERIDO',
                              justificativa: '',
                              loading: false,
                            })
                          }
                        >
                          <Check size={14} />
                          Aprovar
                        </Button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      )}

      {aba === 'banco' && (
        <div className="space-y-4 theme-harmonized">
          <PageHeader
            title="Banco de Horas da Equipe"
            description="Acompanhe saldos e movimentações dos colaboradores sob sua gestão."
          />
          <BancoHorasSection modo="gestor" />
        </div>
      )}

      {aba === 'relatorios' && <RelatoriosSection />}

      <Modal
        open={modalAnalise.open}
        onClose={() =>
          !modalAnalise.loading &&
          setModalAnalise({ open: false, item: null, acao: 'DEFERIDO', justificativa: '', loading: false })
        }
        title={modalAnalise.acao === 'DEFERIDO' ? 'Aprovar Solicitação de Ajuste' : 'Indeferir Solicitação'}
        subtitle={modalAnalise.item?.nomeColaborador || modalAnalise.item?.colaboradorNome}
      >
        <form onSubmit={handleConfirmarAnalise} className="space-y-4">
          <div className="space-y-1.5">
            <label className="block text-xs font-medium">Justificativa do gestor *</label>
            <textarea
              rows={3}
              required
              autoFocus
              value={modalAnalise.justificativa}
              onChange={(e) => setModalAnalise((prev) => ({ ...prev, justificativa: e.target.value }))}
              placeholder="Registre o parecer para histórico de auditoria..."
              className="w-full rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] p-2.5 text-sm"
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button
              variant="secondary"
              onClick={() =>
                setModalAnalise({ open: false, item: null, acao: 'DEFERIDO', justificativa: '', loading: false })
              }
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              variant={modalAnalise.acao === 'DEFERIDO' ? 'primary' : 'danger'}
              disabled={modalAnalise.loading}
            >
              {modalAnalise.loading && <Loader2 size={14} className="animate-spin" />}
              Confirmar {modalAnalise.acao === 'DEFERIDO' ? 'Aprovação' : 'Indeferimento'}
            </Button>
          </div>
        </form>
      </Modal>
    </AppShell>
  );
}

export default GestorDashboard;
