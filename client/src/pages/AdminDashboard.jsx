import React, { useEffect, useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  BarChart3,
  Building2,
  CalendarDays,
  CheckCircle2,
  Clock,
  ExternalLink,
  HardDrive,
  Loader2,
  MapPin,
  Palette,
  Plus,
  RefreshCw,
  Search,
  Users,
} from 'lucide-react';
import toast from 'react-hot-toast';
import adminService from '../services/adminService';
import { buildTenantUrl } from '../utils/tenantHost';
import { BRAND_PRESETS } from '../context/ThemeContext';
import AppShell from '../layouts/AppShell';
import { Badge, Button, Card, EmptyState, PageHeader } from '../components/ui/Primitives';

export default function AdminDashboard() {
  const [aba, setAba] = useState('visao-geral'); // 'visao-geral' | 'nova-empresa' | 'feriados'
  const [stats, setStats] = useState(null);
  const [loadingStats, setLoadingStats] = useState(true);
  const [busca, setBusca] = useState('');

  // Visão individual de uma empresa selecionada
  const [empresaDetalhe, setEmpresaDetalhe] = useState(null);
  const [loadingDetalhe, setLoadingDetalhe] = useState(false);
  const [editCorPrimaria, setEditCorPrimaria] = useState('#2563eb');
  const [editLogoUrl, setEditLogoUrl] = useState('');
  const [salvandoBranding, setSalvandoBranding] = useState(false);

  // Nova Empresa
  const [razaoSocial, setRazaoSocial] = useState('');
  const [cnpj, setCnpj] = useState('');
  const [subdominio, setSubdominio] = useState('');
  const [endereco, setEndereco] = useState('');
  const [logoUrl, setLogoUrl] = useState('');
  const [salvandoEmpresa, setSalvandoEmpresa] = useState(false);

  // Novo Feriado Nacional Global
  const [feriadoDesc, setFeriadoDesc] = useState('');
  const [feriadoData, setFeriadoData] = useState('');
  const [salvandoFeriado, setSalvandoFeriado] = useState(false);

  const carregarEstatisticas = async () => {
    try {
      setLoadingStats(true);
      const data = await adminService.obterEstatisticas();
      setStats(data);
    } catch (err) {
      toast.error(err?.message || 'Erro ao carregar estatísticas do sistema.');
    } finally {
      setLoadingStats(false);
    }
  };

  useEffect(() => {
    carregarEstatisticas();
  }, []);

  const abrirDetalhesEmpresa = async (empresaId) => {
    try {
      setLoadingDetalhe(true);
      const det = await adminService.obterDetalhesEmpresa(empresaId);
      setEmpresaDetalhe(det);
      setEditCorPrimaria(det.corPrimaria || '#2563eb');
      setEditLogoUrl(det.logoUrl || '');
    } catch (err) {
      toast.error(err?.message || 'Erro ao carregar detalhes da empresa.');
    } finally {
      setLoadingDetalhe(false);
    }
  };

  const handleSalvarBranding = async (e) => {
    e.preventDefault();
    if (!empresaDetalhe) return;
    try {
      setSalvandoBranding(true);
      const atualizado = await adminService.atualizarBranding(empresaDetalhe.id, {
        logoUrl: editLogoUrl.trim(),
        corPrimaria: editCorPrimaria,
      });
      setEmpresaDetalhe(atualizado);
      toast.success('Identidade visual da empresa atualizada no banco!');
      carregarEstatisticas();
    } catch (err) {
      toast.error(err?.message || 'Falha ao salvar personalização.');
    } finally {
      setSalvandoBranding(false);
    }
  };

  const handleCadastrarEmpresa = async (e) => {
    e.preventDefault();
    if (!razaoSocial.trim() || !cnpj.trim() || !subdominio.trim() || !endereco.trim()) {
      toast.error('Preencha razão social, CNPJ, subdomínio e endereço.');
      return;
    }

    try {
      setSalvandoEmpresa(true);
      await adminService.cadastrarEmpresa({
        razaoSocial: razaoSocial.trim(),
        cnpj: cnpj.replace(/\D/g, ''),
        subdominio: subdominio.trim().toLowerCase(),
        endereco: endereco.trim(),
        logoUrl: logoUrl.trim(),
      });
      toast.success('Empresa provisionada com sucesso!');
      setRazaoSocial('');
      setCnpj('');
      setSubdominio('');
      setEndereco('');
      setLogoUrl('');
      await carregarEstatisticas();
      setAba('visao-geral');
    } catch (err) {
      toast.error(err?.message || 'Falha ao cadastrar empresa.');
    } finally {
      setSalvandoEmpresa(false);
    }
  };

  const handleCadastrarFeriadoNacional = async (e) => {
    e.preventDefault();
    if (!feriadoDesc.trim() || !feriadoData) {
      toast.error('Informe a descrição e a data do feriado nacional.');
      return;
    }

    try {
      setSalvandoFeriado(true);
      await adminService.cadastrarFeriadoGlobal({
        empresaId: null,
        descricao: feriadoDesc.trim(),
        data: feriadoData,
        alcance: 'NACIONAL',
        uf: null,
        municipio: null,
      });
      toast.success('Feriado nacional cadastrado para todos os tenants!');
      setFeriadoDesc('');
      setFeriadoData('');
    } catch (err) {
      toast.error(err?.message || 'Falha ao cadastrar feriado nacional.');
    } finally {
      setSalvandoFeriado(false);
    }
  };

  const empresas = stats?.empresas || [];
  const empresasFiltradas = empresas.filter((e) => {
    if (!busca.trim()) return true;
    const t = busca.trim().toLowerCase();
    return (
      (e.razaoSocial || '').toLowerCase().includes(t) ||
      (e.subdominio || '').toLowerCase().includes(t) ||
      (e.cnpj || '').includes(t)
    );
  });

  const navItems = [
    { type: 'divider', key: 'div-saas', label: 'Plataforma SaaS' },
    { key: 'visao-geral', label: 'Visão Geral & Empresas', icon: BarChart3, badge: stats?.totalEmpresas },
    { key: 'nova-empresa', label: 'Nova Empresa', icon: Plus },
    { key: 'feriados', label: 'Feriados Nacionais', icon: CalendarDays },
  ];

  return (
    <AppShell
      brandName="Ponto Certo"
      brandSubtitle="Administração Global"
      brandLogoUrl={null}
      navItems={navItems}
      activeKey={aba}
      onSelectNav={(k) => {
        setAba(k);
        setEmpresaDetalhe(null);
      }}
    >
      {/* ==================== DETALHE INDIVIDUAL DE UMA EMPRESA ==================== */}
      {aba === 'visao-geral' && empresaDetalhe && (
        <div className="space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <Button variant="ghost" size="sm" onClick={() => setEmpresaDetalhe(null)}>
              <ArrowLeft size={15} />
              Voltar para todas as empresas
            </Button>

            <a
              href={buildTenantUrl(empresaDetalhe.subdominio, '/login')}
              target="_blank"
              rel="noreferrer"
            >
              <Button variant="primary" size="sm">
                Abrir portal ({empresaDetalhe.subdominio}.pontocerto)
                <ExternalLink size={14} />
              </Button>
            </a>
          </div>

          <PageHeader
            title={empresaDetalhe.razaoSocial}
            description={`CNPJ ${empresaDetalhe.cnpj} · Subdomínio: ${empresaDetalhe.subdominio} · ${empresaDetalhe.endereco || ''}`}
          />

          {/* KPIs Individuais da Empresa */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <Card className="p-4">
              <div className="text-xs text-[var(--text-muted)]">Colaboradores Ativos</div>
              <div className="text-2xl font-bold font-mono-tabular mt-1">
                {empresaDetalhe.colaboradoresAtivos}
              </div>
              <div className="text-[11px] text-[var(--text-secondary)] mt-1">
                RH: {empresaDetalhe.perfis?.RH ?? 0} · Gestores: {empresaDetalhe.perfis?.GESTOR ?? 0} · Colab: {empresaDetalhe.perfis?.COLABORADOR ?? 0}
              </div>
            </Card>
            <Card className="p-4">
              <div className="text-xs text-[var(--text-muted)]">Batidas (Mês / Hoje)</div>
              <div className="text-2xl font-bold font-mono-tabular mt-1">
                {empresaDetalhe.batidasMes}
              </div>
              <div className="text-[11px] text-[var(--text-secondary)] mt-1">
                {empresaDetalhe.batidasHoje} registradas hoje
              </div>
            </Card>
            <Card className="p-4">
              <div className="text-xs text-[var(--text-muted)]">Estrutura Física</div>
              <div className="text-2xl font-bold font-mono-tabular mt-1">
                {empresaDetalhe.locais?.length ?? 0} locais
              </div>
              <div className="text-[11px] text-[var(--text-secondary)] mt-1">
                {empresaDetalhe.equipamentos?.length ?? 0} equipamento(s)
              </div>
            </Card>
            <Card className="p-4">
              <div className="text-xs text-[var(--text-muted)]">Inconsistências Abertas</div>
              <div className="text-2xl font-bold font-mono-tabular mt-1">
                {empresaDetalhe.inconsistenciasAbertas}
              </div>
              <div className="text-[11px] text-[var(--text-secondary)] mt-1">
                Pendências de ponto
              </div>
            </Card>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Personalização White-Label salva no banco */}
            <Card className="lg:col-span-5 p-6 space-y-4">
              <div className="flex items-center gap-2">
                <Palette size={17} style={{ color: 'var(--brand-600)' }} />
                <h2 className="text-sm font-semibold">Personalização White-Label</h2>
              </div>
              <p className="text-xs text-[var(--text-secondary)]">
                Defina a cor primária e o logotipo exibidos automaticamente no portal exclusivo desta empresa.
              </p>

              <form onSubmit={handleSalvarBranding} className="space-y-4">
                <div className="space-y-2">
                  <label className="block text-xs font-medium">Cor principal da marca</label>
                  <div className="flex items-center gap-2.5">
                    <input
                      type="color"
                      value={editCorPrimaria}
                      onChange={(e) => setEditCorPrimaria(e.target.value)}
                      className="h-9 w-12 rounded-lg border border-[var(--border-subtle)] cursor-pointer bg-transparent"
                    />
                    <input
                      type="text"
                      value={editCorPrimaria}
                      onChange={(e) => setEditCorPrimaria(e.target.value)}
                      className="flex-1 h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-xs font-mono-tabular"
                    />
                  </div>
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    {Object.values(BRAND_PRESETS).map((p) => (
                      <button
                        key={p.id}
                        type="button"
                        onClick={() => setEditCorPrimaria(p.swatch)}
                        className="h-6 w-6 rounded-full border border-black/15 cursor-pointer"
                        style={{ backgroundColor: p.swatch }}
                        title={p.label}
                      />
                    ))}
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="block text-xs font-medium">URL ou caminho do logotipo</label>
                  <input
                    type="text"
                    value={editLogoUrl}
                    onChange={(e) => setEditLogoUrl(e.target.value)}
                    placeholder="/logos/vertice.svg ou https://..."
                    className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-xs font-mono-tabular"
                  />
                </div>

                <Button type="submit" variant="primary" className="w-full" disabled={salvandoBranding}>
                  {salvandoBranding && <Loader2 size={14} className="animate-spin" />}
                  Salvar Personalização no Banco
                </Button>
              </form>
            </Card>

            {/* Locais e Equipamentos da empresa */}
            <div className="lg:col-span-7 space-y-6">
              <Card className="p-5">
                <div className="flex items-center gap-2 mb-3">
                  <MapPin size={16} style={{ color: 'var(--brand-600)' }} />
                  <h3 className="text-sm font-semibold">Locais de Trabalho</h3>
                </div>
                {empresaDetalhe.locais?.length ? (
                  <div className="divide-y divide-[var(--border-subtle)] border border-[var(--border-subtle)] rounded-lg overflow-hidden">
                    {empresaDetalhe.locais.map((l) => (
                      <div key={l.id} className="px-3.5 py-2.5 flex items-center justify-between text-xs">
                        <span className="font-medium">{l.nome}</span>
                        <span className="text-[var(--text-secondary)]">{l.municipio}/{l.uf}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-[var(--text-muted)]">Nenhum local cadastrado.</p>
                )}
              </Card>

              <Card className="p-5">
                <div className="flex items-center gap-2 mb-3">
                  <HardDrive size={16} style={{ color: 'var(--brand-600)' }} />
                  <h3 className="text-sm font-semibold">Equipamentos &amp; Estações</h3>
                </div>
                {empresaDetalhe.equipamentos?.length ? (
                  <div className="divide-y divide-[var(--border-subtle)] border border-[var(--border-subtle)] rounded-lg overflow-hidden">
                    {empresaDetalhe.equipamentos.map((eq) => (
                      <div key={eq.id} className="px-3.5 py-2.5 flex flex-wrap items-center justify-between gap-2 text-xs">
                        <div>
                          <div className="font-medium">{eq.identificacao}</div>
                          <div className="text-[11px] font-mono-tabular text-[var(--text-muted)]">
                            {eq.codigo} · UUID: {eq.id}
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <Badge tone={eq.status === 'ATIVO' ? 'success' : 'neutral'}>{eq.tipo}</Badge>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-[var(--text-muted)]">Nenhum equipamento cadastrado.</p>
                )}
              </Card>
            </div>
          </div>
        </div>
      )}

      {/* ==================== VISÃO GERAL GLOBAL DO SAAS ==================== */}
      {aba === 'visao-geral' && !empresaDetalhe && (
        <div className="space-y-6">
          <PageHeader
            title="Visão Geral da Plataforma"
            description="Acompanhe indicadores consolidados de todas as empresas clientes e inspecione cada ambiente individualmente."
            actions={
              <div className="flex items-center gap-2">
                <Button variant="secondary" size="sm" onClick={carregarEstatisticas} disabled={loadingStats}>
                  <RefreshCw size={14} className={loadingStats ? 'animate-spin' : ''} />
                  Atualizar
                </Button>
                <Button variant="primary" size="sm" onClick={() => setAba('nova-empresa')}>
                  <Plus size={14} />
                  Nova Empresa
                </Button>
              </div>
            }
          />

          {loadingStats ? (
            <Card className="p-12 flex justify-center">
              <Loader2 size={24} className="animate-spin text-[var(--text-muted)]" />
            </Card>
          ) : (
            <>
              {/* Global SaaS KPIs */}
              <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                <Card className="p-5">
                  <div className="flex items-center justify-between text-xs text-[var(--text-muted)]">
                    <span>Empresas Clientes</span>
                    <Building2 size={16} />
                  </div>
                  <div className="text-2xl font-bold font-mono-tabular mt-2">
                    {stats?.totalEmpresas ?? 0}
                  </div>
                  <div className="text-[11px] text-[var(--text-secondary)] mt-1">
                    {stats?.totalLocaisTrabalho ?? 0} locais · {stats?.totalEquipamentos ?? 0} equip.
                  </div>
                </Card>

                <Card className="p-5">
                  <div className="flex items-center justify-between text-xs text-[var(--text-muted)]">
                    <span>Colaboradores</span>
                    <Users size={16} />
                  </div>
                  <div className="text-2xl font-bold font-mono-tabular mt-2">
                    {stats?.colaboradoresAtivos ?? 0}
                  </div>
                  <div className="text-[11px] text-[var(--text-secondary)] mt-1">
                    {stats?.totalColaboradores ?? 0} cadastrados ({stats?.colaboradoresDesligados ?? 0} desligados)
                  </div>
                </Card>

                <Card className="p-5">
                  <div className="flex items-center justify-between text-xs text-[var(--text-muted)]">
                    <span>Batidas ({stats?.competenciaAtual})</span>
                    <Clock size={16} />
                  </div>
                  <div className="text-2xl font-bold font-mono-tabular mt-2">
                    {stats?.batidasMes ?? 0}
                  </div>
                  <div className="text-[11px] text-emerald-600 dark:text-emerald-400 mt-1">
                    {stats?.batidasHoje ?? 0} registradas hoje
                  </div>
                </Card>

                <Card className="p-5">
                  <div className="flex items-center justify-between text-xs text-[var(--text-muted)]">
                    <span>Inconsistências Abertas</span>
                    <AlertTriangle size={16} />
                  </div>
                  <div className="text-2xl font-bold font-mono-tabular mt-2">
                    {stats?.inconsistenciasAbertas ?? 0}
                  </div>
                  <div className="text-[11px] text-[var(--text-secondary)] mt-1">
                    Em todas as empresas
                  </div>
                </Card>
              </div>

              {/* Tabela de Empresas */}
              <Card className="overflow-hidden">
                <div className="p-4 border-b border-[var(--border-subtle)] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <h2 className="text-sm font-semibold">Empresas Cadastradas</h2>
                    <p className="text-xs text-[var(--text-secondary)]">
                      Clique em uma empresa para ver métricas individuais, equipamentos e personalizar cor/logo.
                    </p>
                  </div>
                  <div className="relative w-full sm:w-64">
                    <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                    <input
                      type="text"
                      value={busca}
                      onChange={(e) => setBusca(e.target.value)}
                      placeholder="Buscar empresa ou subdomínio..."
                      className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] pl-8 pr-3 text-xs"
                    />
                  </div>
                </div>

                {empresasFiltradas.length === 0 ? (
                  <EmptyState
                    icon={Building2}
                    title="Nenhuma empresa encontrada"
                    description="Cadastre uma nova empresa cliente para começar."
                  />
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs">
                      <thead className="bg-[var(--surface-subtle)] border-b border-[var(--border-subtle)] text-[var(--text-muted)] uppercase">
                        <tr>
                          <th className="px-4 py-3 font-semibold">Empresa</th>
                          <th className="px-4 py-3 font-semibold">Subdomínio</th>
                          <th className="px-4 py-3 font-semibold text-right">Colaboradores</th>
                          <th className="px-4 py-3 font-semibold text-right">Batidas/Mês</th>
                          <th className="px-4 py-3 font-semibold text-right">Pendências</th>
                          <th className="px-4 py-3 font-semibold text-right">Ações</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-[var(--border-subtle)]">
                        {empresasFiltradas.map((emp) => (
                          <tr key={emp.id} className="hover:bg-[var(--surface-hover)] transition-colors">
                            <td className="px-4 py-3">
                              <div className="flex items-center gap-2.5">
                                <span
                                  className="h-3 w-3 rounded-full shrink-0 border border-black/15"
                                  style={{ backgroundColor: emp.corPrimaria || '#2563eb' }}
                                  title={`Cor: ${emp.corPrimaria}`}
                                />
                                <div>
                                  <div className="font-semibold text-[var(--text-primary)]">
                                    {emp.razaoSocial}
                                  </div>
                                  <div className="text-[11px] font-mono-tabular text-[var(--text-muted)]">
                                    CNPJ {emp.cnpj}
                                  </div>
                                </div>
                              </div>
                            </td>
                            <td className="px-4 py-3 font-mono-tabular">
                              <a
                                href={buildTenantUrl(emp.subdominio, '/login')}
                                target="_blank"
                                rel="noreferrer"
                                className="inline-flex items-center gap-1 hover:underline"
                                style={{ color: 'var(--brand-600)' }}
                              >
                                {emp.subdominio}.pontocerto
                                <ExternalLink size={11} />
                              </a>
                            </td>
                            <td className="px-4 py-3 text-right font-mono-tabular">
                              {emp.colaboradoresAtivos} / {emp.totalColaboradores}
                            </td>
                            <td className="px-4 py-3 text-right font-mono-tabular font-medium">
                              {emp.batidasMes}
                            </td>
                            <td className="px-4 py-3 text-right">
                              <Badge tone={emp.inconsistenciasAbertas > 0 ? 'warning' : 'success'}>
                                {emp.inconsistenciasAbertas > 0
                                  ? `${emp.inconsistenciasAbertas} aberta(s)`
                                  : 'Em dia'}
                              </Badge>
                            </td>
                            <td className="px-4 py-3 text-right">
                              <Button
                                variant="secondary"
                                size="sm"
                                disabled={loadingDetalhe}
                                onClick={() => abrirDetalhesEmpresa(emp.id)}
                              >
                                Detalhes &amp; Marca
                              </Button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </Card>
            </>
          )}
        </div>
      )}

      {/* ==================== PROVISIONAR NOVA EMPRESA ==================== */}
      {aba === 'nova-empresa' && (
        <div className="space-y-6">
          <PageHeader
            title="Provisionar Nova Empresa"
            description="Crie um novo tenant isolado com seu próprio subdomínio."
          />

          <Card className="max-w-xl p-6">
            <form onSubmit={handleCadastrarEmpresa} className="space-y-4">
              <div className="space-y-1">
                <label className="text-xs font-medium">Razão Social *</label>
                <input
                  type="text"
                  value={razaoSocial}
                  onChange={(e) => setRazaoSocial(e.target.value)}
                  placeholder="Ex: Horizonte Logística Ltda"
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm"
                />
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="text-xs font-medium">CNPJ *</label>
                  <input
                    type="text"
                    value={cnpj}
                    onChange={(e) => setCnpj(e.target.value)}
                    placeholder="00.000.000/0001-00"
                    className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-medium">Subdomínio *</label>
                  <input
                    type="text"
                    value={subdominio}
                    onChange={(e) => setSubdominio(e.target.value)}
                    placeholder="horizonte"
                    className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
                  />
                </div>
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium">Endereço *</label>
                <input
                  type="text"
                  value={endereco}
                  onChange={(e) => setEndereco(e.target.value)}
                  placeholder="Av. Principal, 100 — Cidade/UF"
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm"
                />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium">URL do Logotipo (opcional)</label>
                <input
                  type="text"
                  value={logoUrl}
                  onChange={(e) => setLogoUrl(e.target.value)}
                  placeholder="/logos/empresa.svg ou https://..."
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm"
                />
              </div>
              <Button type="submit" variant="primary" disabled={salvandoEmpresa}>
                {salvandoEmpresa ? <Loader2 size={15} className="animate-spin" /> : <CheckCircle2 size={15} />}
                Provisionar Empresa
              </Button>
            </form>
          </Card>
        </div>
      )}

      {/* ==================== FERIADOS NACIONAIS ==================== */}
      {aba === 'feriados' && (
        <div className="space-y-6">
          <PageHeader
            title="Feriados Nacionais (Globais)"
            description="Feriados nacionais cadastrados aqui aparecem automaticamente no calendário de todas as empresas do SaaS."
          />

          <Card className="max-w-lg p-6">
            <h2 className="text-sm font-semibold mb-4">Novo Feriado Nacional</h2>
            <form onSubmit={handleCadastrarFeriadoNacional} className="space-y-4">
              <div className="space-y-1">
                <label className="text-xs font-medium">Descrição *</label>
                <input
                  type="text"
                  value={feriadoDesc}
                  onChange={(e) => setFeriadoDesc(e.target.value)}
                  placeholder="Ex: Independência do Brasil"
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm"
                />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium">Data *</label>
                <input
                  type="date"
                  value={feriadoData}
                  onChange={(e) => setFeriadoData(e.target.value)}
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
                />
              </div>
              <Button type="submit" variant="primary" disabled={salvandoFeriado}>
                {salvandoFeriado ? <Loader2 size={15} className="animate-spin" /> : <Plus size={15} />}
                Cadastrar Feriado Nacional
              </Button>
            </form>
          </Card>
        </div>
      )}
    </AppShell>
  );
}
