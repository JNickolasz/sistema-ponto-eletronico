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

  const itens = dados?.itens || dados?.colaboradores || dados?.linhas || [];

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
                  <tr key={row.funcionarioId || idx} className="hover:bg-[var(--surface-hover)]">
                    <td className="px-4 py-3 font-medium text-[var(--text-primary)]">
                      {row.nomeColaborador || row.colaboradorNome || row.nome || '—'}
                    </td>
                    <td className="px-4 py-3 font-mono-tabular text-[var(--text-secondary)]">
                      {row.matricula || '—'}
                    </td>
                    <td className="px-4 py-3 text-right font-mono-tabular font-semibold">
                      {row.totalHorasExtrasFormatado ||
                        row.saldoFormatado ||
                        row.totalFaltasFormatado ||
                        (row.totalPendencias != null ? `${row.totalPendencias} pendência(s)` : '—')}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Badge tone="neutral">
                        {row.localTrabalho || row.gestorNome || row.status || 'Consolidado'}
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
