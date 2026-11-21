import React, { useEffect, useState } from 'react';
import {
  CalendarDays,
  Clock,
  FileSpreadsheet,
  FileText,
  History,
  Loader2,
  Plus,
  RefreshCw,
  Scale,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { useAuth } from '../hooks/useAuth';
import colaboradorService from '../services/colaboradorService';
import jornadaService from '../services/jornadaService';
import AppShell from '../layouts/AppShell';
import { Badge, Button, Card, EmptyState, PageHeader } from '../components/ui/Primitives';
import SolicitarAjusteModal from '../components/SolicitarAjusteModal';
import MinhasSolicitacoesSection from '../components/MinhasSolicitacoesSection';
import ApuracaoEspelhoSection from '../components/ApuracaoEspelhoSection';
import BancoHorasSection from '../components/BancoHorasSection';
import ComprovanteModal from '../components/ComprovanteModal';

export function ColaboradorDashboard() {
  const { user } = useAuth();
  const [aba, setAba] = useState('resumo');

  const [painelData, setPainelData] = useState(null);
  const [loadingPainel, setLoadingPainel] = useState(true);
  const [batidas, setBatidas] = useState([]);
  const [loadingBatidas, setLoadingBatidas] = useState(false);

  const [horarioPrevisto, setHorarioPrevisto] = useState(null);
  const [dataHorario, setDataHorario] = useState(() => new Date().toISOString().split('T')[0]);
  const [funcionarioId, setFuncionarioId] = useState('');

  const [isModalAjusteOpen, setIsModalAjusteOpen] = useState(false);
  const [solicitacoesRefreshKey, setSolicitacoesRefreshKey] = useState(0);
  const [comprovanteSelecionado, setComprovanteSelecionado] = useState(null);

  const carregarDados = async () => {
    try {
      setLoadingPainel(true);
      const data = await colaboradorService.getPainel();
      setPainelData(data);
      const fId = data?.funcionarioId || '';
      setFuncionarioId(fId);
      if (fId) {
        jornadaService.obterHorarioPrevisto(fId, dataHorario).then(setHorarioPrevisto).catch(() => {});
        setLoadingBatidas(true);
        colaboradorService
          .listarBatidas(fId)
          .then((list) => setBatidas(Array.isArray(list) ? list : []))
          .catch(() => setBatidas([]))
          .finally(() => setLoadingBatidas(false));
      }
    } catch (err) {
      toast.error(err?.message || 'Erro ao carregar dados do colaborador.');
    } finally {
      setLoadingPainel(false);
    }
  };

  useEffect(() => {
    carregarDados();
  }, []);

  useEffect(() => {
    if (funcionarioId && dataHorario) {
      jornadaService.obterHorarioPrevisto(funcionarioId, dataHorario).then(setHorarioPrevisto).catch(() => {});
    }
  }, [funcionarioId, dataHorario]);

  const navItems = [
    { type: 'divider', key: 'div-colab', label: 'Meu Ponto' },
    { key: 'resumo', label: 'Resumo & Batidas', icon: Clock },
    { key: 'espelho', label: 'Espelho de Ponto', icon: FileSpreadsheet },
    { key: 'banco', label: 'Banco de Horas', icon: Scale },
    { key: 'solicitacoes', label: 'Minhas Solicitações', icon: History },
  ];

  return (
    <AppShell
      brandSubtitle="Portal do Colaborador"
      navItems={navItems}
      activeKey={aba}
      onSelectNav={setAba}
    >
      {aba === 'resumo' && (
        <div className="space-y-6">
          <PageHeader
            title={`Olá, ${painelData?.nomeCompleto || user?.usuario}`}
            description="Acompanhe suas marcações registradas, comprovantes da Portaria 671 e horário previsto."
            actions={
              <div className="flex items-center gap-2">
                <Button variant="secondary" size="sm" onClick={carregarDados} disabled={loadingPainel}>
                  <RefreshCw size={14} className={loadingPainel ? 'animate-spin' : ''} />
                  Atualizar
                </Button>
                <Button variant="primary" size="sm" onClick={() => setIsModalAjusteOpen(true)}>
                  <Plus size={14} />
                  Solicitar Ajuste
                </Button>
              </div>
            }
          />

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Horário Previsto */}
            <Card className="lg:col-span-5 p-6 space-y-4">
              <div className="flex items-center justify-between gap-2">
                <div className="inline-flex items-center gap-2 text-sm font-semibold">
                  <CalendarDays size={16} style={{ color: 'var(--brand-600)' }} />
                  Horário Previsto
                </div>
                <input
                  type="date"
                  value={dataHorario}
                  onChange={(e) => setDataHorario(e.target.value)}
                  className="h-8 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] px-2.5 text-xs font-mono-tabular"
                />
              </div>

              {horarioPrevisto ? (
                <div className="space-y-3 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="text-[var(--text-secondary)]">Regime</span>
                    <Badge tone="brand">{horarioPrevisto.nomeRegime || horarioPrevisto.tipoRegime || 'Jornada'}</Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-[var(--text-secondary)]">Situação do dia</span>
                    <Badge tone={horarioPrevisto.ehFolga ? 'neutral' : 'success'}>
                      {horarioPrevisto.ehFolga ? 'Folga / Descanso' : 'Dia útil previsto'}
                    </Badge>
                  </div>
                  {!horarioPrevisto.ehFolga && (
                    <div className="grid grid-cols-2 gap-2 pt-2">
                      <div className="rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] p-2.5">
                        <div className="text-[11px] text-[var(--text-muted)]">Entrada / Saída</div>
                        <div className="font-mono-tabular font-semibold mt-0.5">
                          {horarioPrevisto.horaEntrada || '—'} – {horarioPrevisto.horaSaida || '—'}
                        </div>
                      </div>
                      <div className="rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] p-2.5">
                        <div className="text-[11px] text-[var(--text-muted)]">Carga prevista</div>
                        <div className="font-mono-tabular font-semibold mt-0.5">
                          {horarioPrevisto.cargaHorariaFormatada || `${horarioPrevisto.cargaHorariaMinutos ?? 0} min`}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <p className="text-xs text-[var(--text-muted)]">
                  Nenhum regime de trabalho vinculado para a data selecionada.
                </p>
              )}
            </Card>

            {/* Últimas Batidas */}
            <Card className="lg:col-span-7 p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-semibold">Histórico Recente de Marcações</h2>
                <Badge tone="neutral">{batidas.length} registro(s)</Badge>
              </div>

              {loadingBatidas ? (
                <div className="py-10 flex justify-center">
                  <Loader2 size={22} className="animate-spin text-[var(--text-muted)]" />
                </div>
              ) : batidas.length === 0 ? (
                <EmptyState
                  icon={Clock}
                  title="Nenhuma marcação encontrada"
                  description="Suas batidas registradas na estação ou relógio aparecerão aqui."
                />
              ) : (
                <div className="divide-y divide-[var(--border-subtle)] border border-[var(--border-subtle)] rounded-xl overflow-hidden max-h-80 overflow-y-auto">
                  {batidas.slice(0, 15).map((b) => (
                    <div
                      key={b.id}
                      className="px-4 py-3 flex items-center justify-between gap-3 text-xs hover:bg-[var(--surface-hover)]"
                    >
                      <div className="space-y-0.5">
                        <div className="font-mono-tabular font-semibold text-[var(--text-primary)]">
                          {b.dataHora ? new Date(b.dataHora).toLocaleString('pt-BR') : '—'}
                        </div>
                        <div className="text-[11px] text-[var(--text-muted)]">
                          NSR {b.nsr ?? '—'} · Origem: {b.tipoOrigem || 'ESTACAO'}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge tone={b.tipoRegistro === 'ENTRADA' ? 'success' : 'brand'}>
                          {b.tipoRegistro}
                        </Badge>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setComprovanteSelecionado(b)}
                          title="Ver comprovante"
                        >
                          <FileText size={14} />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>
        </div>
      )}

      {aba === 'espelho' && (
        <div className="space-y-4 theme-harmonized">
          <PageHeader
            title="Meu Espelho de Ponto"
            description="Detalhamento diário de horas trabalhadas, extras, atrasos e abonos."
          />
          <ApuracaoEspelhoSection funcionarioId={funcionarioId} podeRecalcular={false} />
        </div>
      )}

      {aba === 'banco' && (
        <div className="space-y-4 theme-harmonized">
          <PageHeader
            title="Meu Banco de Horas"
            description="Consulte créditos, débitos, compensações e saldo acumulado."
          />
          <BancoHorasSection funcionarioId={funcionarioId} modo="colaborador" />
        </div>
      )}

      {aba === 'solicitacoes' && (
        <div className="space-y-4 theme-harmonized">
          <PageHeader
            title="Minhas Solicitações de Ajuste"
            description="Acompanhe o andamento dos pedidos de inclusão ou correção de batidas."
            actions={
              <Button variant="primary" size="sm" onClick={() => setIsModalAjusteOpen(true)}>
                <Plus size={14} />
                Nova Solicitação
              </Button>
            }
          />
          <MinhasSolicitacoesSection refreshKey={solicitacoesRefreshKey} />
        </div>
      )}

      <SolicitarAjusteModal
        isOpen={isModalAjusteOpen}
        onClose={() => setIsModalAjusteOpen(false)}
        onSuccess={() => setSolicitacoesRefreshKey((k) => k + 1)}
      />

      {comprovanteSelecionado && (
        <ComprovanteModal
          batida={comprovanteSelecionado}
          onClose={() => setComprovanteSelecionado(null)}
        />
      )}
    </AppShell>
  );
}

export default ColaboradorDashboard;
