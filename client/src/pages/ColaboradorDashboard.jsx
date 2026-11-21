import React, { useEffect, useState } from 'react';
import {
  CalendarDays,
  Clock,
  Coffee,
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
import { formatarDataHora, formatarNsr } from '../utils/brazilianDocuments';
import AppShell from '../layouts/AppShell';
import { Badge, Button, Card, EmptyState, PageHeader } from '../components/ui/Primitives';
import SolicitarAjusteModal from '../components/SolicitarAjusteModal';
import MinhasSolicitacoesSection from '../components/MinhasSolicitacoesSection';
import ApuracaoEspelhoSection from '../components/ApuracaoEspelhoSection';
import BancoHorasSection from '../components/BancoHorasSection';
import ComprovanteModal from '../components/ComprovanteModal';

function formatarOrigemBatida(origem) {
  switch (origem) {
    case 'ESTACAO':
      return 'Estação de Ponto';
    case 'ARQUIVO_IMPORTADO':
      return 'Relógio Físico (AFD)';
    case 'AJUSTE_MANUAL':
      return 'Ajuste Aprovado';
    case 'APLICATIVO_WEB':
      return 'Portal Web';
    default:
      return origem || 'Estação de Ponto';
  }
}

function formatarMinutos(min) {
  if (!min || min <= 0) return '0h 00m';
  const h = Math.floor(min / 60);
  const m = min % 60;
  return `${h}h ${String(m).padStart(2, '0')}m`;
}

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
    { key: 'resumo', label: 'Resumo & Batidas', icon: Clock, badge: batidas.length || undefined },
    { key: 'espelho', label: 'Espelho de Ponto', icon: FileSpreadsheet },
    { key: 'banco', label: 'Banco de Horas', icon: Scale },
    { key: 'solicitacoes', label: 'Minhas Solicitações', icon: History },
  ];

  const ehDiaTrabalho =
    horarioPrevisto?.diaTrabalho !== undefined
      ? Boolean(horarioPrevisto.diaTrabalho)
      : horarioPrevisto
        ? !horarioPrevisto.ehFolga
        : false;

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
            title={`Olá, ${painelData?.nomeCompleto || painelData?.usuarioLogado || user?.usuario}`}
            description="Acompanhe suas marcações registradas, comprovantes oficiais da Portaria 671 e horário previsto."
            actions={
              <div className="flex items-center gap-2.5">
                <Button variant="secondary" size="sm" onClick={carregarDados} disabled={loadingPainel}>
                  <RefreshCw size={15} className={loadingPainel ? 'animate-spin' : ''} />
                  Atualizar
                </Button>
                <Button variant="primary" size="sm" onClick={() => setIsModalAjusteOpen(true)}>
                  <Plus size={15} />
                  Solicitar Ajuste
                </Button>
              </div>
            }
          />

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Horário Previsto */}
            <Card className="lg:col-span-5 p-6 space-y-5">
              <div className="flex items-center justify-between gap-2">
                <div className="inline-flex items-center gap-2 text-base font-bold">
                  <CalendarDays size={18} style={{ color: 'var(--brand-600)' }} />
                  Horário Previsto
                </div>
                <input
                  type="date"
                  value={dataHorario}
                  onChange={(e) => setDataHorario(e.target.value)}
                  className="min-h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] px-3 text-xs font-mono-tabular"
                />
              </div>

              {horarioPrevisto ? (
                <div className="space-y-3.5 text-sm">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-[var(--text-secondary)]">Regime</span>
                    <Badge tone="brand">
                      {horarioPrevisto.descricaoRegime ||
                        horarioPrevisto.nomeRegime ||
                        horarioPrevisto.tipoRegime ||
                        'Jornada'}
                    </Badge>
                  </div>

                  {horarioPrevisto.diaSemana && (
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-[var(--text-secondary)]">Dia da semana</span>
                      <span className="font-semibold text-[var(--text-primary)]">
                        {horarioPrevisto.diaSemana}
                      </span>
                    </div>
                  )}

                  <div className="flex items-center justify-between gap-2">
                    <span className="text-[var(--text-secondary)]">Situação do dia</span>
                    <Badge tone={ehDiaTrabalho ? 'success' : 'neutral'}>
                      {ehDiaTrabalho ? 'Dia útil previsto' : 'Folga / Descanso (DSR)'}
                    </Badge>
                  </div>

                  {ehDiaTrabalho ? (
                    <div className="grid grid-cols-2 gap-3 pt-2">
                      <div className="rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3">
                        <div className="text-xs font-medium text-[var(--text-muted)]">Entrada / Saída</div>
                        <div className="font-mono-tabular font-bold text-sm mt-1">
                          {horarioPrevisto.horaEntrada
                            ? String(horarioPrevisto.horaEntrada).slice(0, 5)
                            : '—'}{' '}
                          –{' '}
                          {horarioPrevisto.horaSaida
                            ? String(horarioPrevisto.horaSaida).slice(0, 5)
                            : '—'}
                        </div>
                      </div>

                      <div className="rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3">
                        <div className="text-xs font-medium text-[var(--text-muted)]">Intervalo</div>
                        <div className="font-mono-tabular font-bold text-sm mt-1">
                          {horarioPrevisto.intervaloInicio
                            ? `${String(horarioPrevisto.intervaloInicio).slice(0, 5)} – ${String(horarioPrevisto.intervaloFim || '').slice(0, 5)}`
                            : 'Sem intervalo'}
                        </div>
                      </div>

                      <div className="col-span-2 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3 flex items-center justify-between">
                        <span className="text-xs font-medium text-[var(--text-muted)]">
                          Carga horária líquida prevista
                        </span>
                        <span className="font-mono-tabular font-bold text-sm">
                          {formatarMinutos(
                            horarioPrevisto.cargaDiariaMinutos ?? horarioPrevisto.cargaHorariaMinutos
                          )}{' '}
                          <span className="text-xs font-normal text-[var(--text-muted)]">
                            (±{horarioPrevisto.toleranciaMinutos ?? 10}m tol.)
                          </span>
                        </span>
                      </div>
                    </div>
                  ) : (
                    <div className="p-4 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-subtle)]/70 flex items-start gap-3">
                      <Coffee size={18} className="text-[var(--text-muted)] shrink-0 mt-0.5" />
                      <div className="text-xs text-[var(--text-secondary)] space-y-1">
                        <div className="font-semibold text-[var(--text-primary)]">
                          Dia de descanso previsto no seu regime
                        </div>
                        <div>
                          {horarioPrevisto.mensagem ||
                            'Não há expediente obrigatório previsto para esta data. Marcações realizadas são apuradas como hora extra.'}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <p className="text-sm text-[var(--text-muted)]">
                  Nenhum regime de trabalho vinculado para a data selecionada.
                </p>
              )}
            </Card>

            {/* Últimas Batidas */}
            <Card className="lg:col-span-7 p-6">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-base font-bold">Histórico Recente de Marcações</h2>
                  <p className="text-xs text-[var(--text-secondary)]">
                    Clique em qualquer marcação para abrir ou imprimir o Comprovante Portaria 671.
                  </p>
                </div>
                <Badge tone="brand">{batidas.length} registro(s)</Badge>
              </div>

              {loadingBatidas ? (
                <div className="py-12 flex justify-center">
                  <Loader2 size={24} className="animate-spin text-[var(--text-muted)]" />
                </div>
              ) : batidas.length === 0 ? (
                <EmptyState
                  icon={Clock}
                  title="Nenhuma marcação encontrada"
                  description="Suas batidas registradas na estação ou relógio aparecerão aqui."
                />
              ) : (
                <div className="divide-y divide-[var(--border-subtle)] border border-[var(--border-subtle)] rounded-xl overflow-hidden max-h-96 overflow-y-auto">
                  {batidas.slice(0, 25).map((b) => {
                    const timestamp = b.dataHoraBatida || b.dataHora;
                    return (
                      <div
                        key={b.id}
                        className="px-4 py-3.5 flex items-center justify-between gap-3 text-sm hover:bg-[var(--surface-hover)] transition-colors"
                      >
                        <div className="space-y-0.5 min-w-0">
                          <div className="font-mono-tabular font-bold text-[var(--text-primary)]">
                            {timestamp ? formatarDataHora(timestamp) : '—'}
                          </div>
                          <div className="text-xs text-[var(--text-secondary)] truncate">
                            NSR #{formatarNsr(b.nsr)} · {formatarOrigemBatida(b.tipoOrigem)}
                          </div>
                        </div>
                        <div className="flex items-center gap-2.5 shrink-0">
                          <Badge tone={b.tipoRegistro === 'ENTRADA' ? 'success' : 'brand'}>
                            {b.tipoRegistro}
                          </Badge>
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => setComprovanteSelecionado(b)}
                            title="Ver comprovante oficial Portaria 671"
                          >
                            <FileText size={14} />
                            Comprovante
                          </Button>
                        </div>
                      </div>
                    );
                  })}
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
                <Plus size={15} />
                Nova Solicitação
              </Button>
            }
          />
          <MinhasSolicitacoesSection
            refreshKey={solicitacoesRefreshKey}
            onAbrirModal={() => setIsModalAjusteOpen(true)}
          />
        </div>
      )}

      <SolicitarAjusteModal
        isOpen={isModalAjusteOpen}
        onClose={() => setIsModalAjusteOpen(false)}
        onSuccess={() => setSolicitacoesRefreshKey((k) => k + 1)}
      />

      <ComprovanteModal
        isOpen={Boolean(comprovanteSelecionado)}
        comprovante={comprovanteSelecionado}
        onClose={() => setComprovanteSelecionado(null)}
      />
    </AppShell>
  );
}

export default ColaboradorDashboard;
