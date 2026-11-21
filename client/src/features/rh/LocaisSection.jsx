import React, { useState } from 'react';
import { Building2, Loader2, MapPin, Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import rhService from '../../services/rhService';
import { Badge, Button, Card, EmptyState, PageHeader } from '../../components/ui/Primitives';

export default function LocaisSection({ empresaId, empresaNome, locais = [], loading, onAtualizado }) {
  const [nome, setNome] = useState('');
  const [endereco, setEndereco] = useState('');
  const [municipio, setMunicipio] = useState('');
  const [uf, setUf] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!nome.trim() || !endereco.trim()) {
      toast.error('Informe o nome e o endereço completo do local de trabalho.');
      return;
    }

    try {
      setSubmitting(true);
      await rhService.cadastrarLocalTrabalho({
        empresaId,
        nome: nome.trim(),
        endereco: endereco.trim(),
        municipio: municipio.trim() || undefined,
        uf: uf.trim().toUpperCase() || undefined,
      });
      toast.success('Local de trabalho cadastrado com sucesso.');
      setNome('');
      setEndereco('');
      setMunicipio('');
      setUf('');
      await onAtualizado?.();
    } catch (err) {
      toast.error(err?.message || 'Erro ao cadastrar local de trabalho.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Locais de Trabalho"
        description={`Unidades, matriz e filiais vinculadas a ${empresaNome || 'sua empresa'}.`}
      />

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Formulário */}
        <Card className="lg:col-span-5 p-6">
          <h2 className="text-sm font-semibold mb-4">Cadastrar Local de Trabalho</h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <label htmlFor="local-nome" className="block text-xs font-medium">
                Nome do local *
              </label>
              <input
                id="local-nome"
                type="text"
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                placeholder="Ex: Matriz Centro, Filial São Paulo"
                className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
              />
            </div>

            <div className="space-y-1.5">
              <label htmlFor="local-endereco" className="block text-xs font-medium">
                Endereço completo *
              </label>
              <input
                id="local-endereco"
                type="text"
                value={endereco}
                onChange={(e) => setEndereco(e.target.value)}
                placeholder="Ex: Av. Paulista, 1000 — Bela Vista"
                className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="sm:col-span-2 space-y-1.5">
                <label htmlFor="local-municipio" className="block text-xs font-medium">
                  Município
                </label>
                <input
                  id="local-municipio"
                  type="text"
                  value={municipio}
                  onChange={(e) => setMunicipio(e.target.value)}
                  placeholder="Ex: Recife"
                  className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                />
              </div>
              <div className="space-y-1.5">
                <label htmlFor="local-uf" className="block text-xs font-medium">
                  UF
                </label>
                <input
                  id="local-uf"
                  type="text"
                  maxLength={2}
                  value={uf}
                  onChange={(e) => setUf(e.target.value.toUpperCase())}
                  placeholder="PE"
                  className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm uppercase font-mono-tabular focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                />
              </div>
            </div>

            <Button type="submit" variant="primary" size="lg" className="w-full" disabled={submitting}>
              {submitting ? <Loader2 size={15} className="animate-spin" /> : <Plus size={15} />}
              Cadastrar Local
            </Button>
          </form>
        </Card>

        {/* Listagem */}
        <Card className="lg:col-span-7 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold">Locais Cadastrados</h2>
            <Badge tone="neutral">{locais.length} unidade(s)</Badge>
          </div>

          {loading ? (
            <div className="py-12 flex justify-center">
              <Loader2 size={22} className="animate-spin text-[var(--text-muted)]" />
            </div>
          ) : locais.length === 0 ? (
            <EmptyState
              icon={MapPin}
              title="Nenhum local cadastrado"
              description="Cadastre a matriz ou filiais para vincular colaboradores e equipamentos."
            />
          ) : (
            <div className="divide-y divide-[var(--border-subtle)] border border-[var(--border-subtle)] rounded-xl overflow-hidden">
              {locais.map((local) => (
                <div
                  key={local.id}
                  className="p-4 flex items-start justify-between gap-4 bg-[var(--surface-card)] hover:bg-[var(--surface-hover)]/50 transition-colors"
                >
                  <div className="flex items-start gap-3 min-w-0">
                    <div
                      className="h-9 w-9 rounded-lg flex items-center justify-center shrink-0 mt-0.5"
                      style={{ backgroundColor: 'var(--brand-50)', color: 'var(--brand-600)' }}
                    >
                      <Building2 size={17} />
                    </div>
                    <div className="min-w-0">
                      <div className="text-sm font-semibold text-[var(--text-primary)] truncate">
                        {local.nome}
                      </div>
                      <div className="text-xs text-[var(--text-secondary)] mt-0.5">
                        {local.endereco}
                        {local.municipio ? ` · ${local.municipio}` : ''}
                        {local.uf ? `/${local.uf}` : ''}
                      </div>
                    </div>
                  </div>
                  <Badge tone="brand">{local.uf || 'Unidade'}</Badge>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
