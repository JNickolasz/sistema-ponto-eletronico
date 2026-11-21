import React, { useEffect, useState } from 'react';
import {
  AlertCircle,
  Calendar,
  CheckCircle2,
  Clock,
  Loader2,
  Plus,
  RefreshCw,
  UserCheck,
  XCircle,
} from 'lucide-react';
import solicitacaoAjusteService from '../services/solicitacaoAjusteService';
import { Badge, Button, Card, EmptyState } from './ui/Primitives';

export function MinhasSolicitacoesSection({ onAbrirModal, refreshKey = 0 }) {
  const [solicitacoes, setSolicitacoes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const carregarSolicitacoes = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await solicitacaoAjusteService.listarMinhas();
      setSolicitacoes(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Erro ao carregar solicitações de ajuste.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarSolicitacoes();
  }, [refreshKey]);

  const formatarData = (dataStr) => {
    if (!dataStr) return '—';
    try {
      const [ano, mes, dia] = dataStr.split('-');
      return `${dia}/${mes}/${ano}`;
    } catch {
      return dataStr;
    }
  };

  const formatarHora = (horaStr) => {
    if (!horaStr) return '—';
    return String(horaStr).substring(0, 5);
  };

  const renderStatusBadge = (status) => {
    switch (status) {
      case 'DEFERIDO':
        return (
          <Badge tone="success">
            <CheckCircle2 size={13} />
            Deferido
          </Badge>
        );
      case 'INDEFERIDO':
        return (
          <Badge tone="danger">
            <XCircle size={13} />
            Indeferido
          </Badge>
        );
      case 'PENDENTE':
      default:
        return (
          <Badge tone="warning">
            <Clock size={13} />
            Pendente de Análise
          </Badge>
        );
    }
  };

  return (
    <Card className="p-6 space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-4 border-b border-[var(--border-subtle)]">
        <div className="flex items-center gap-2.5">
          <h2 className="text-base font-bold text-[var(--text-primary)]">
            Histórico de Solicitações
          </h2>
          <Badge tone="neutral">{solicitacoes.length}</Badge>
        </div>

        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={carregarSolicitacoes}
            disabled={loading}
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            Atualizar
          </Button>
          {onAbrirModal && (
            <Button type="button" variant="primary" size="sm" onClick={onAbrirModal}>
              <Plus size={14} />
              Solicitar Ajuste
            </Button>
          )}
        </div>
      </div>

      {loading ? (
        <div className="py-12 flex flex-col items-center justify-center text-[var(--text-muted)] gap-2.5">
          <Loader2 size={24} className="animate-spin" />
          <span className="text-xs font-medium">Carregando histórico de solicitações...</span>
        </div>
      ) : error ? (
        <div className="p-4 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 text-red-700 dark:text-red-300 text-xs flex items-center gap-2.5">
          <AlertCircle size={16} className="shrink-0" />
          <span>{error}</span>
        </div>
      ) : solicitacoes.length === 0 ? (
        <EmptyState
          icon={Clock}
          title="Você não possui solicitações de ajuste registradas"
          description="Caso tenha esquecido de bater o ponto em algum dia, clique em 'Nova Solicitação' para enviar ao seu gestor."
          action={
            onAbrirModal ? (
              <Button variant="primary" size="sm" onClick={onAbrirModal}>
                <Plus size={14} />
                Solicitar Ajuste
              </Button>
            ) : null
          }
        />
      ) : (
        <div className="space-y-3">
          {solicitacoes.map((item) => (
            <div
              key={item.id}
              className="p-4 sm:p-5 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-page)] space-y-3"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex flex-wrap items-center gap-2.5">
                  <span
                    className="h-8 w-8 rounded-lg flex items-center justify-center"
                    style={{ backgroundColor: 'var(--brand-50)', color: 'var(--brand-600)' }}
                  >
                    <Calendar size={15} />
                  </span>
                  <div>
                    <span className="text-sm font-bold text-[var(--text-primary)] font-mono-tabular">
                      {formatarData(item.dataAlvo)}
                    </span>
                    <span className="text-xs text-[var(--text-secondary)] ml-2">
                      às{' '}
                      <strong
                        className="font-mono-tabular"
                        style={{ color: 'var(--brand-600)' }}
                      >
                        {formatarHora(item.horarioPretendido)}
                      </strong>
                    </span>
                  </div>
                  <Badge tone={item.tipoRegistro === 'ENTRADA' ? 'success' : 'brand'}>
                    {item.tipoRegistro}
                  </Badge>
                </div>

                <div>{renderStatusBadge(item.status)}</div>
              </div>

              <div className="text-xs text-[var(--text-secondary)] bg-[var(--surface-card)] p-3.5 rounded-xl border border-[var(--border-subtle)]">
                <span className="text-xs font-semibold text-[var(--text-muted)] block mb-1">
                  Seu motivo:
                </span>
                <span className="text-[var(--text-primary)]">{item.motivo}</span>
              </div>

              {item.status !== 'PENDENTE' && (
                <div
                  className={`p-3.5 rounded-xl border text-xs space-y-1 ${
                    item.status === 'DEFERIDO'
                      ? 'bg-emerald-50/70 dark:bg-emerald-950/30 border-emerald-200 dark:border-emerald-900/60 text-emerald-900 dark:text-emerald-200'
                      : 'bg-red-50/70 dark:bg-red-950/30 border-red-200 dark:border-red-900/60 text-red-900 dark:text-red-200'
                  }`}
                >
                  <div className="flex flex-wrap items-center justify-between gap-2 text-xs font-semibold">
                    <span className="flex items-center gap-1.5">
                      <UserCheck size={14} />
                      Analisado por: <strong>{item.gestorNome || 'Gestor'}</strong>
                    </span>
                    {item.batidaCriadaId && (
                      <span className="font-mono-tabular text-xs font-bold">
                        ✓ Marcação gerada no espelho
                      </span>
                    )}
                  </div>
                  <p className="italic">
                    "{item.justificativaGestor || 'Sem parecer registrado.'}"
                  </p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}

export default MinhasSolicitacoesSection;
