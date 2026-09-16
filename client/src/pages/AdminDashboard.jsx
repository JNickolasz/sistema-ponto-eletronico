import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import adminService from '../services/adminService';
import {
  Building2,
  Plus,
  RefreshCw,
  LogOut,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  ExternalLink,
  Sparkles,
  ArrowRight
} from 'lucide-react';
import toast from 'react-hot-toast';

export function AdminDashboard() {
  const { logout, login } = useAuth();
  const navigate = useNavigate();

  const [empresas, setEmpresas] = useState([]);
  const [carregandoEmpresas, setCarregandoEmpresas] = useState(false);
  const [salvandoEmpresa, setSalvandoEmpresa] = useState(false);

  // Formulário de Empresa
  const [razaoSocial, setRazaoSocial] = useState('');
  const [cnpj, setCnpj] = useState('');
  const [subdominio, setSubdominio] = useState('');
  const [endereco, setEndereco] = useState('');
  const [logoUrl, setLogoUrl] = useState('');

  const carregarEmpresas = async () => {
    try {
      setCarregandoEmpresas(true);
      const data = await adminService.listarEmpresas();
      setEmpresas(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error('Erro ao carregar lista de empresas: ' + (err.message || ''));
    } finally {
      setCarregandoEmpresas(false);
    }
  };

  useEffect(() => {
    carregarEmpresas();
  }, []);

  const handlePreencherExemplo = () => {
    const random = Math.floor(1000 + Math.random() * 9000);
    setRazaoSocial(`Empresa Exemplo ${random} Ltda`);
    // CNPJ válido da Petrobras ou Banco do Brasil para passar no validador @CNPJ
    setCnpj('33.000.167/0001-01');
    setSubdominio(`empresa${random}`);
    setEndereco('Av. das Américas, 4200 - Barra da Tijuca, RJ');
    setLogoUrl('https://placehold.co/150x150/png?text=Logo');
    toast('Dados de exemplo preenchidos!', { icon: '✨' });
  };

  const handleCadastrarEmpresa = async (e) => {
    e.preventDefault();

    if (!razaoSocial.trim() || !cnpj.trim() || !subdominio.trim() || !endereco.trim()) {
      toast.error('Preencha todos os campos obrigatórios');
      return;
    }

    try {
      setSalvandoEmpresa(true);
      await adminService.cadastrarEmpresa({
        razaoSocial: razaoSocial.trim(),
        cnpj: cnpj.trim(),
        subdominio: subdominio.trim().toLowerCase(),
        endereco: endereco.trim(),
        logoUrl: logoUrl.trim() || undefined,
      });

      toast.success('Empresa cadastrada com sucesso pelo Administrador!');
      setRazaoSocial('');
      setCnpj('');
      setSubdominio('');
      setEndereco('');
      setLogoUrl('');
      await carregarEmpresas();
    } catch (err) {
      toast.error(err.message || 'Falha ao cadastrar empresa.');
    } finally {
      setSalvandoEmpresa(false);
    }
  };

  const handleSwitchToRh = async () => {
    try {
      await login('admin.rh', 'admin123');
      toast.success('Conectado como Administrador de RH!');
      navigate('/rh');
    } catch (err) {
      toast.error('Erro ao alternar para RH: ' + err.message);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* Header */}
      <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur-md sticky top-0 z-30 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-amber-600 to-amber-400 text-slate-950 font-bold shadow-lg shadow-amber-500/20">
            <ShieldCheck size={24} />
          </div>
          <div>
            <h1 className="text-lg font-bold text-white leading-none flex items-center gap-2">
              PontoCerto
              <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/30">
                Painel do Administrador Geral
              </span>
            </h1>
            <p className="text-xs text-slate-400 mt-1">
              Gestão Centralizada de Tenants e Cadastro de Empresas
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={handleSwitchToRh}
            className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-purple-950/60 border border-purple-800/60 text-purple-300 hover:bg-purple-900/50 text-xs font-semibold transition-all cursor-pointer shadow-sm"
            title="Trocar imediatamente para o perfil de RH"
          >
            <span>Acessar Painel do RH (admin.rh)</span>
            <ArrowRight size={14} />
          </button>

          <button
            type="button"
            onClick={() => {
              logout();
              navigate('/login');
            }}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white text-xs font-medium transition-all cursor-pointer"
          >
            <LogOut size={14} />
            <span>Sair</span>
          </button>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-6 space-y-6">
        {/* Info Banner */}
        <div className="rounded-2xl bg-amber-950/20 border border-amber-500/20 p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <div className="p-2 rounded-xl bg-amber-500/10 text-amber-400 mt-0.5 sm:mt-0">
              <ShieldCheck size={20} />
            </div>
            <div>
              <h2 className="text-sm font-semibold text-amber-200">
                Acesso Mestre Administrativo Ativo
              </h2>
              <p className="text-xs text-slate-300 mt-0.5">
                Você está autenticado como <strong>Admin Geral da Plataforma</strong>. As requisições são autorizadas diretamente via <code className="text-amber-300 bg-amber-950/60 px-1.5 py-0.5 rounded border border-amber-800/40">X-Admin-Key</code>, dispensando o uso de Postman.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handlePreencherExemplo}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 text-xs font-semibold transition-all cursor-pointer border border-amber-500/30 whitespace-nowrap"
          >
            <Sparkles size={14} /> Preencher Exemplo
          </button>
        </div>

        {/* Grid: Cadastro + Listagem */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Formulário de Cadastro */}
          <div className="lg:col-span-6 bg-slate-900/60 border border-slate-800/80 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
            <div className="flex items-center gap-2 mb-4 pb-3 border-b border-slate-800">
              <Building2 className="text-amber-400" size={20} />
              <div>
                <h3 className="text-sm font-bold text-white">Cadastrar Nova Empresa</h3>
                <p className="text-xs text-slate-400">Endpoint: POST /api/v1/admin/empresas</p>
              </div>
            </div>

            <form onSubmit={handleCadastrarEmpresa} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Razão Social *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ex: Petrobras Distribuidora S.A."
                  value={razaoSocial}
                  onChange={(e) => setRazaoSocial(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    CNPJ Válido *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="00.000.000/0000-00"
                    value={cnpj}
                    onChange={(e) => setCnpj(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500 font-mono"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Subdomínio Único *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="ex: petrobras"
                    value={subdominio}
                    onChange={(e) => setSubdominio(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Endereço Completo *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ex: Av. Paulista, 1000 - Bela Vista, São Paulo - SP"
                  value={endereco}
                  onChange={(e) => setEndereco(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  URL da Logomarca (Opcional)
                </label>
                <input
                  type="text"
                  placeholder="https://..."
                  value={logoUrl}
                  onChange={(e) => setLogoUrl(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  disabled={salvandoEmpresa}
                  className="w-full flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-amber-600 to-amber-500 hover:from-amber-500 hover:to-amber-400 py-3 text-sm font-semibold text-slate-950 shadow-lg shadow-amber-600/20 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {salvandoEmpresa ? (
                    <>
                      <div className="h-4 w-4 animate-spin rounded-full border-2 border-slate-950 border-t-transparent" />
                      <span>Cadastrando Empresa...</span>
                    </>
                  ) : (
                    <>
                      <Plus size={16} />
                      <span>Cadastrar Empresa no Sistema</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>

          {/* Listagem de Empresas */}
          <div className="lg:col-span-6 bg-slate-900/60 border border-slate-800/80 rounded-2xl p-6 shadow-xl backdrop-blur-sm flex flex-col">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-800">
              <div>
                <h3 className="text-sm font-bold text-white">Empresas Cadastradas ({empresas.length})</h3>
                <p className="text-xs text-slate-400">Total de tenants ativos no banco de dados</p>
              </div>
              <button
                type="button"
                onClick={carregarEmpresas}
                disabled={carregandoEmpresas}
                className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-all cursor-pointer"
                title="Recarregar lista de empresas"
              >
                <RefreshCw size={16} className={carregandoEmpresas ? 'animate-spin' : ''} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto max-h-[440px] space-y-3 pr-1">
              {carregandoEmpresas ? (
                <div className="flex flex-col items-center justify-center py-12 text-slate-400 space-y-3">
                  <div className="h-8 w-8 animate-spin rounded-full border-2 border-amber-500 border-t-transparent" />
                  <p className="text-xs">Consultando empresas no banco...</p>
                </div>
              ) : empresas.length === 0 ? (
                <div className="text-center py-12 text-slate-500">
                  <Building2 size={36} className="mx-auto text-slate-600 mb-2" />
                  <p className="text-sm font-medium">Nenhuma empresa cadastrada</p>
                  <p className="text-xs mt-1">Utilize o formulário ao lado para cadastrar a primeira empresa.</p>
                </div>
              ) : (
                empresas.map((emp) => (
                  <div
                    key={emp.id}
                    className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800 hover:border-amber-500/40 transition-all flex items-center justify-between"
                  >
                    <div className="flex items-center gap-3">
                      <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-500/10 text-amber-400 font-bold text-xs">
                        <Building2 size={18} />
                      </div>
                      <div>
                        <div className="text-xs font-semibold text-slate-200">
                          {emp.razaoSocial}
                        </div>
                        <div className="text-[11px] text-slate-500 font-mono">
                          ID: {emp.id}
                        </div>
                      </div>
                    </div>
                    <span className="inline-flex items-center gap-1 text-[11px] text-emerald-400 bg-emerald-950/40 border border-emerald-800/40 px-2 py-0.5 rounded-full font-medium">
                      <CheckCircle2 size={12} /> Ativa
                    </span>
                  </div>
                ))
              )}
            </div>

            {/* Dica para o teste da Issue 2 */}
            <div className="mt-4 pt-3 border-t border-slate-800/80 text-xs text-slate-400 flex items-center justify-between">
              <span>Pronto para vincular locais de trabalho?</span>
              <button
                type="button"
                onClick={handleSwitchToRh}
                className="text-amber-400 hover:text-amber-300 font-semibold inline-flex items-center gap-1 cursor-pointer transition-colors"
              >
                Abrir Painel RH →
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default AdminDashboard;
