import React, { useEffect, useState } from 'react';
import { Download, FileSpreadsheet, Loader2, RefreshCw, ShieldCheck } from 'lucide-react';
import toast from 'react-hot-toast';
import exportacaoFolhaService from '../../services/exportacaoFolhaService';
import { Badge, Button, Card, EmptyState, PageHeader } from '../../components/ui/Primitives';

function mesAtualISO() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

export default function ExportacaoFolhaSection() {
  const [competencia, setCompetencia] = useState(mesAtualISO);
  const [previa, setPrevia] = useState(null);
  const [historico, setHistorico] = useState([]);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  const carregar = async () => {
    try {
      setLoading(true);
      const [prev, hist] = await Promise.all([
        exportacaoFolhaService.consultarPrevia(competencia).catch(() => null),
        exportacaoFolhaService.listarHistorico(competencia).catch(() => []),
      ]);
      setPrevia(prev);
      setHistorico(Array.isArray(hist) ? hist : []);
    } catch (err) {
      toast.error(err?.message || 'Erro ao carregar prévia da folha.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregar();
  }, [competencia]);

  const handleExportar = async () => {
    try {
      setExporting(true);
      const { sha256 } = await exportacaoFolhaService.exportarCsv(competencia);
      toast.success(
        sha256
          ? `Folha exportada! SHA-256: ${sha256.slice(0, 12)}...`
          : 'Arquivo CSV da folha exportado com sucesso!'
      );
      await carregar();
    } catch (err) {
      toast.error(err?.message || 'Não foi possível exportar a folha (verifique se a competência está fechada).');
    } finally {
      setExporting(false);
    }
  };

  const linhas = previa?.linhas || [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Exportação para Folha de Pagamento"
        description="Gere o arquivo CSV oficial com integridade SHA-256 e proteção contra CSV injection para envio à contabilidade."
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
            <Button variant="primary" size="sm" onClick={handleExportar} disabled={exporting || loading}>
              {exporting ? <Loader2 size={14} className="animate-spin" /> : <Download size={14} />}
              Gerar CSV da Folha
            </Button>
          </div>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <Card className="lg:col-span-8 overflow-hidden">
          <div className="px-5 py-4 border-b border-[var(--border-subtle)] flex items-center justify-between">
            <h2 className="text-sm font-semibold">Prévia Consolidada ({competencia})</h2>
            <Badge tone="neutral">{linhas.length} colaboradores</Badge>
          </div>

          {loading ? (
            <div className="py-12 flex justify-center">
              <Loader2 size={22} className="animate-spin text-[var(--text-muted)]" />
            </div>
          ) : linhas.length === 0 ? (
            <EmptyState
              icon={FileSpreadsheet}
              title="Prévia indisponível"
              description="Feche a competência ou apure os dias do mês para visualizar os eventos da folha."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-[var(--surface-subtle)] border-b border-[var(--border-subtle)] text-[var(--text-muted)] uppercase">
                  <tr>
                    <th className="px-4 py-2.5 font-semibold">Colaborador</th>
                    <th className="px-4 py-2.5 font-semibold">Matrícula</th>
                    <th className="px-4 py-2.5 font-semibold text-right">Normais</th>
                    <th className="px-4 py-2.5 font-semibold text-right">Extras</th>
                    <th className="px-4 py-2.5 font-semibold text-right">Faltas/Atrasos</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border-subtle)]">
                  {linhas.map((l, i) => (
                    <tr key={l.funcionarioId || i} className="hover:bg-[var(--surface-hover)]">
                      <td className="px-4 py-2.5 font-medium">{l.nomeColaborador || l.nome}</td>
                      <td className="px-4 py-2.5 font-mono-tabular text-[var(--text-secondary)]">{l.matricula}</td>
                      <td className="px-4 py-2.5 text-right font-mono-tabular">{l.horasNormaisFormatado || '00:00'}</td>
                      <td className="px-4 py-2.5 text-right font-mono-tabular text-emerald-600 dark:text-emerald-400">
                        {l.horasExtrasFormatado || '00:00'}
                      </td>
                      <td className="px-4 py-2.5 text-right font-mono-tabular text-amber-600 dark:text-amber-400">
                        {l.faltasAtrasosFormatado || '00:00'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        <Card className="lg:col-span-4 p-5">
          <div className="flex items-center gap-2 mb-4">
            <ShieldCheck size={16} style={{ color: 'var(--brand-600)' }} />
            <h2 className="text-sm font-semibold">Histórico de Exportações</h2>
          </div>

          {historico.length === 0 ? (
            <EmptyState
              icon={FileSpreadsheet}
              title="Nenhuma exportação ainda"
              description="Cada arquivo gerado registra autor, data e hash SHA-256."
            />
          ) : (
            <div className="space-y-2.5">
              {historico.map((h) => (
                <div
                  key={h.id}
                  className="rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3 text-xs space-y-1"
                >
                  <div className="font-semibold truncate">{h.nomeArquivo}</div>
                  <div className="text-[11px] text-[var(--text-secondary)]">
                    Por {h.exportadoPor || 'RH'}
                  </div>
                  {h.hashSha256 && (
                    <div className="text-[10px] font-mono-tabular text-[var(--text-muted)] truncate">
                      SHA-256: {h.hashSha256}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
