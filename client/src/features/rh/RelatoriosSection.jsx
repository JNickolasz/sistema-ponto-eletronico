import React, { useEffect, useState } from 'react';
import { BarChart3, Download, Loader2, RefreshCw } from 'lucide-react';
import toast from 'react-hot-toast';
import relatorioService from '../../services/relatorioService';
import { Badge, Button, Card, EmptyState, PageHeader } from '../../components/ui/Primitives';

const TIPOS = [
  { id: 'horas-extras', label: 'Horas Extras' },
  { id: 'absenteismo', label: 'Absenteísmo & Atrasos' },
  { id: 'banco-horas', label: 'Banco de Horas' },
  { id: 'pendencias', label: 'Pendências' },
];

function mesAtualISO() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

function formatarMinutos(min, comSinal = false) {
  if (min == null || Number.isNaN(Number(min))) return '00:00';
  const n = Number(min);
  const abs = Math.abs(n);
  const hh = String(Math.floor(abs / 60)).padStart(2, '0');
  const mm = String(abs % 60).padStart(2, '0');
  if (n < 0) return `-${hh}:${mm}`;
  if (comSinal && n > 0) return `+${hh}:${mm}`;
  return `${hh}:${mm}`;
}

function formatarDataBr(iso) {
  if (!iso) return '—';
  const partes = String(iso).split('-');
  return partes.length === 3 ? `${partes[2]}/${partes[1]}/${partes[0]}` : iso;
}

export default function RelatoriosSection() {
  const [tipo, setTipo] = useState('horas-extras');
  const [competencia, setCompetencia] = useState(mesAtualISO);
  const [dados, setDados] = useState(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  const carregar = async () => {
    try {
      setLoading(true);
      const res = await relatorioService.consultar(tipo, { competencia });
      setDados(res);
    } catch (err) {
      toast.error(err?.message || 'Erro ao gerar relatório.');
      setDados(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregar();
  }, [tipo, competencia]);

  const handleCsv = async () => {
    try {
      setExporting(true);
      await relatorioService.baixarCsv(tipo, { competencia });
      toast.success('CSV exportado com sucesso!');
    } catch (err) {
      toast.error(err?.message || 'Erro ao exportar CSV.');
    } finally {
      setExporting(false);
    }
  };

  const itens =
    tipo === 'pendencias'
      ? [...(dados?.abertas || []), ...(dados?.tratadas || [])]
      : dados?.itens || dados?.colaboradores || dados?.linhas || [];

  const renderIndicadorPrincipal = (row) => {
    if (tipo === 'horas-extras') {
      return formatarMinutos(row.totalMinutosExtras ?? 0);
    }
    if (tipo === 'absenteismo') {
      const totalAusencia =
        (row.totalMinutosFalta || 0) +
        (row.totalMinutosAtraso || 0) +
        (row.totalMinutosSaidaAntecipada || 0);
      const pct = Number(row.percentualAbsenteismo || 0).toFixed(1);
      return `${formatarMinutos(totalAusencia)} (${pct}%)`;
    }
    if (tipo === 'banco-horas') {
      return formatarMinutos(row.saldoFinalMinutos ?? 0, true);
    }
    if (tipo === 'pendencias') {
      return row.tipo || '—';
    }
    return '—';
  };

  const renderDetalhes = (row) => {
    const local = row.localTrabalhoNome || row.localTrabalho;
    if (tipo === 'horas-extras') {
      const dias = `${row.diasComExtra ?? 0} dia(s) c/ extra`;
      return local ? `${dias} · ${local}` : dias;
    }
    if (tipo === 'absenteismo') {
      const resumo = `Faltas: ${row.diasComFalta ?? 0}d · Atrasos: ${row.diasComAtraso ?? 0}d · Abonado: ${formatarMinutos(row.totalMinutosAbonados ?? 0)}`;
      return local ? `${resumo} · ${local}` : resumo;
    }
    if (tipo === 'banco-horas') {
      const resumo = `Créd: ${formatarMinutos(row.creditosPeriodoMinutos ?? 0)} · Déb: ${formatarMinutos(row.debitosPeriodoMinutos ?? 0)}`;
      return local ? `${resumo} · ${local}` : resumo;
    }
    if (tipo === 'pendencias') {
      const dataStr = formatarDataBr(row.dataReferencia);
      const sit = row.situacao || 'ABERTA';
      return local ? `${dataStr} · ${sit} · ${local}` : `${dataStr} · ${sit}`;
    }
    return local || 'Consolidado';
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Relatórios Gerenciais"
        description="Consulte indicadores consolidados de jornada, absenteísmo, banco de horas e pendências com exportação em CSV."
        actions={
          <div className="flex items-center gap-2">
            <input
              type="month"
              value={competencia}
              onChange={(e) => setCompetencia(e.target.value)}
              className="h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
            />
            <Button variant="secondary" size="sm" onClick={carregar} disabled={loading}>
              <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            </Button>
            <Button variant="primary" size="sm" onClick={handleCsv} disabled={exporting || loading}>
              {exporting ? <Loader2 size={14} className="animate-spin" /> : <Download size={14} />}
              Exportar CSV
            </Button>
          </div>
        }
      />

      {/* Tabs */}
      <div className="flex flex-wrap gap-2">
        {TIPOS.map((t) => {
          const active = t.id === tipo;
          return (
            <button
              key={t.id}
              type="button"
              onClick={() => setTipo(t.id)}
              className={`h-9 px-3.5 rounded-lg text-xs font-medium border transition-colors cursor-pointer ${
                active
                  ? 'border-transparent text-[var(--brand-fg)]'
                  : 'border-[var(--border-subtle)] bg-[var(--surface-card)] text-[var(--text-secondary)] hover:bg-[var(--surface-hover)]'
              }`}
              style={active ? { backgroundColor: 'var(--brand-600)' } : undefined}
            >
              {t.label}
            </button>
          );
        })}
      </div>

      <Card className="overflow-hidden">
        {loading ? (
          <div className="py-14 flex justify-center">
            <Loader2 size={24} className="animate-spin text-[var(--text-muted)]" />
          </div>
        ) : itens.length === 0 ? (
          <EmptyState
            icon={BarChart3}
            title="Sem registros no período"
            description="Nenhuma movimentação encontrada para o relatório e competência selecionados."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[var(--surface-subtle)] border-b border-[var(--border-subtle)] text-[var(--text-muted)] uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3 font-semibold">Colaborador</th>
                  <th className="px-4 py-3 font-semibold">Matrícula</th>
                  <th className="px-4 py-3 font-semibold text-right">Indicador Principal</th>
                  <th className="px-4 py-3 font-semibold text-right">Detalhes</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border-subtle)]">
                {itens.map((row, idx) => (
                  <tr key={row.id || row.funcionarioId || idx} className="hover:bg-[var(--surface-hover)]">
                    <td className="px-4 py-3 font-medium text-[var(--text-primary)]">
                      <div>{row.nomeCompleto || row.nomeColaborador || row.colaboradorNome || row.nome || '—'}</div>
                      {row.cargo && (
                        <div className="text-[11px] font-normal text-[var(--text-muted)]">{row.cargo}</div>
                      )}
                    </td>
                    <td className="px-4 py-3 font-mono-tabular text-[var(--text-secondary)]">
                      {row.matricula || '—'}
                    </td>
                    <td className="px-4 py-3 text-right font-mono-tabular font-semibold">
                      {renderIndicadorPrincipal(row)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Badge tone="neutral">
                        {renderDetalhes(row)}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
