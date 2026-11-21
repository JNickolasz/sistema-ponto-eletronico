import React, { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  Camera,
  CheckCircle2,
  Clock,
  Eye,
  EyeOff,
  Loader2,
  LogIn,
  MapPin,
  Printer,
  RefreshCw,
  ShieldCheck,
  Upload,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { useAuth } from '../hooks/useAuth';
import { useTheme } from '../context/ThemeContext';
import { API_BASE } from '../services/api';
import { buildRootUrl } from '../utils/tenantHost';
import BrandLogo from '../components/ui/BrandLogo';
import ThemeToggle from '../components/ui/ThemeToggle';
import { Badge, Button, Card } from '../components/ui/Primitives';

const API_ORIGIN = API_BASE.replace(/\/api\/v1\/?$/, '');

/**
 * Portal Multi-Tenant com transição horizontal fluida (slide 100vw por painel):
 * Painel Esquerdo (/)      -> Relógio de Ponto Principal da Empresa (Corredor / Recepção)
 * Painel Direito  (/login) -> Login Corporativo Centralizado (RH / Gestor / Colaborador)
 */
export default function TenantPortalSlider() {
  const { hostSubdomain, tenantBranding } = useTheme();
  const { login, isAuthenticated, getDashboardPath } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const isLoginPane = location.pathname === '/login';
  const estacoes = tenantBranding?.estacoes || [];

  useEffect(() => {
    if (isAuthenticated && (location.pathname === '/' || location.pathname === '/login')) {
      navigate(getDashboardPath(), { replace: true });
    }
  }, [isAuthenticated, location.pathname, navigate, getDashboardPath]);

  // ===== Estado do Relógio de Ponto =====
  const [estacaoSelecionadaId, setEstacaoSelecionadaId] = useState('');
  const [horaAtual, setHoraAtual] = useState(() => new Date());
  const [pontoUsuario, setPontoUsuario] = useState('');
  const [pontoSenha, setPontoSenha] = useState('');
  const [mostrarPontoSenha, setMostrarPontoSenha] = useState(false);
  const [submittingPonto, setSubmittingPonto] = useState(false);
  const [comprovante, setComprovante] = useState(null);

  // Câmera
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);
  const [cameraAtiva, setCameraAtiva] = useState(false);
  const [cameraErro, setCameraErro] = useState('');
  const [fotoBlob, setFotoBlob] = useState(null);
  const [fotoPreview, setFotoPreview] = useState(null);

  // ===== Estado do Login Corporativo =====
  const [loginUsuario, setLoginUsuario] = useState('');
  const [loginSenha, setLoginSenha] = useState('');
  const [mostrarLoginSenha, setMostrarLoginSenha] = useState(false);
  const [submittingLogin, setSubmittingLogin] = useState(false);

  useEffect(() => {
    const t = setInterval(() => setHoraAtual(new Date()), 1000);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    if (estacoes.length > 0 && !estacaoSelecionadaId) {
      setEstacaoSelecionadaId(estacoes[0].id);
    }
  }, [estacoes, estacaoSelecionadaId]);

  const estacaoAtual = estacoes.find((e) => e.id === estacaoSelecionadaId) || estacoes[0] || null;

  const irParaPane = (destino) => {
    if (destino === 'login' && !isLoginPane) {
      navigate('/login');
    } else if (destino === 'relogio' && isLoginPane) {
      navigate('/');
    }
  };

  const iniciarCamera = async () => {
    setCameraErro('');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } },
        audio: false,
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
      setCameraAtiva(true);
    } catch {
      setCameraErro('Câmera indisponível neste dispositivo. Você pode anexar uma foto abaixo.');
      setCameraAtiva(false);
    }
  };

  const pararCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((tr) => tr.stop());
      streamRef.current = null;
    }
    setCameraAtiva(false);
  };

  useEffect(() => {
    return () => pararCamera();
  }, []);

  const capturarDoVideo = () =>
    new Promise((resolve) => {
      const video = videoRef.current;
      const canvas = canvasRef.current;
      if (!video || !canvas || !cameraAtiva) {
        resolve(null);
        return;
      }
      canvas.width = video.videoWidth || 640;
      canvas.height = video.videoHeight || 480;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      canvas.toBlob((blob) => resolve(blob), 'image/jpeg', 0.85);
    });

  const handleCapturarFoto = async () => {
    const blob = await capturarDoVideo();
    if (!blob) return;
    setFotoBlob(blob);
    setFotoPreview(URL.createObjectURL(blob));
  };

  const handleUploadFoto = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setFotoBlob(file);
    setFotoPreview(URL.createObjectURL(file));
  };

  const handleBaterPonto = async (e) => {
    e.preventDefault();
    if (!estacaoAtual) {
      toast.error('Nenhuma estação ativa cadastrada para esta empresa.');
      return;
    }
    if (!pontoUsuario.trim() || !pontoSenha.trim()) {
      toast.error('Informe seu usuário e senha.');
      return;
    }

    let blobParaEnviar = fotoBlob;
    if (!blobParaEnviar && cameraAtiva) {
      blobParaEnviar = await capturarDoVideo();
    }
    if (!blobParaEnviar) {
      toast.error('Ative a câmera ou anexe uma foto para confirmar a marcação.');
      return;
    }

    try {
      setSubmittingPonto(true);
      const formData = new FormData();
      formData.append('estacaoId', estacaoAtual.id);
      formData.append('usuario', pontoUsuario.trim());
      formData.append('senha', pontoSenha);
      formData.append('foto', blobParaEnviar, 'captura_ponto.jpg');

      const res = await fetch(`${API_ORIGIN}/api/estacao/bater-ponto`, {
        method: 'POST',
        body: formData,
      });
      const data = await res.json().catch(() => null);
      if (!res.ok) {
        throw new Error(data?.message || data?.erro || 'Credenciais inválidas ou registro não permitido.');
      }

      setComprovante(data);
      setPontoUsuario('');
      setPontoSenha('');
      setFotoBlob(null);
      setFotoPreview(null);
      toast.success('Ponto registrado com sucesso!');
    } catch (err) {
      toast.error(err?.message || 'Não foi possível registrar o ponto.');
    } finally {
      setSubmittingPonto(false);
    }
  };

  const handleLoginTenant = async (e) => {
    e.preventDefault();
    if (!loginUsuario.trim() || !loginSenha.trim()) {
      toast.error('Informe seu usuário e senha.');
      return;
    }

    try {
      setSubmittingLogin(true);
      const data = await login(loginUsuario.trim(), loginSenha.trim(), hostSubdomain);
      toast.success(`Bem-vindo, ${data.usuario}`);
      navigate(getDashboardPath(data.perfil), { replace: true });
    } catch (err) {
      toast.error(err?.message || 'Usuário ou senha inválidos.');
    } finally {
      setSubmittingLogin(false);
    }
  };

  if (tenantBranding.loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[var(--surface-page)] text-[var(--text-secondary)]">
        <div className="flex flex-col items-center gap-3">
          <Loader2 size={24} className="animate-spin" style={{ color: 'var(--brand-600)' }} />
          <span className="text-xs font-medium">Carregando ambiente da empresa...</span>
        </div>
      </div>
    );
  }

  if (tenantBranding.notFound) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[var(--surface-page)] px-4">
        <Card className="w-full max-w-md p-8 text-center space-y-4">
          <div className="mx-auto h-12 w-12 rounded-xl bg-amber-50 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400 flex items-center justify-center">
            <AlertCircle size={24} />
          </div>
          <h1 className="text-lg font-semibold">Empresa não encontrada</h1>
          <p className="text-xs text-[var(--text-secondary)]">
            O subdomínio <span className="font-mono-tabular font-semibold">{hostSubdomain}</span> não
            corresponde a nenhuma empresa ativa.
          </p>
          <a href={buildRootUrl('/')}>
            <Button variant="primary" className="w-full mt-2">
              Ir para o Ponto Certo
            </Button>
          </a>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-[var(--surface-page)] text-[var(--text-primary)] overflow-x-hidden">
      {/* Header fixo compartilhado com pill deslizante */}
      <header className="h-16 border-b border-[var(--border-subtle)] bg-[var(--surface-card)]/95 backdrop-blur-xs px-4 sm:px-6 sticky top-0 z-30 no-print">
        <div className="max-w-5xl mx-auto h-full flex items-center justify-between gap-3">
          <BrandLogo
            name={tenantBranding.nome}
            subtitle={isLoginPane ? 'Acesso ao Portal Corporativo' : 'Estação & Relógio de Ponto'}
            logoUrl={tenantBranding.logoUrl}
            size="sm"
          />

          <div className="flex items-center gap-2.5">
            {/* Segmented Pill Switcher com indicador deslizante */}
            <div
              role="tablist"
              aria-label="Alternar entre Relógio de Ponto e Login"
              className="relative grid grid-cols-2 p-1 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-subtle)] min-w-[240px] sm:min-w-[280px]"
            >
              {/* Pílula de fundo que desliza junto com a tela */}
              <span
                aria-hidden="true"
                className="pointer-events-none absolute top-1 bottom-1 left-1 w-[calc(50%-4px)] rounded-lg shadow-xs transition-transform duration-500 ease-[cubic-bezier(0.22,1,0.36,1)]"
                style={{
                  backgroundColor: 'var(--brand-600)',
                  transform: isLoginPane ? 'translateX(100%)' : 'translateX(0%)',
                }}
              />

              <button
                type="button"
                role="tab"
                aria-selected={!isLoginPane}
                onClick={() => irParaPane('relogio')}
                className={`relative z-10 h-8 px-3 rounded-lg text-xs font-medium inline-flex items-center justify-center gap-1.5 transition-colors duration-200 cursor-pointer ${
                  !isLoginPane
                    ? 'text-[var(--brand-fg)] font-semibold'
                    : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
                }`}
              >
                <Clock size={13} />
                <span>Relógio de Ponto</span>
              </button>

              <button
                type="button"
                role="tab"
                aria-selected={isLoginPane}
                onClick={() => irParaPane('login')}
                className={`relative z-10 h-8 px-3 rounded-lg text-xs font-medium inline-flex items-center justify-center gap-1.5 transition-colors duration-200 cursor-pointer ${
                  isLoginPane
                    ? 'text-[var(--brand-fg)] font-semibold'
                    : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
                }`}
              >
                <LogIn size={13} />
                <span>Acessar Portal</span>
              </button>
            </div>

            <ThemeToggle />
          </div>
        </div>
      </header>

      {/* Viewport horizontal (200% track com 2 painéis de 100vw cada, sempre centralizados) */}
      <div className="tenant-slide-viewport">
        <div
          className="tenant-slide-track"
          style={{ transform: isLoginPane ? 'translateX(-50%)' : 'translateX(0%)' }}
        >
          {/* ==================== PAINEL 1 (ESQUERDO - 100vw): RELÓGIO DE PONTO ==================== */}
          <section
            data-active={!isLoginPane}
            aria-hidden={isLoginPane}
            inert={isLoginPane ? '' : undefined}
            className="tenant-slide-pane px-4 sm:px-6 py-8 flex items-center justify-center"
          >
            <div className="w-full max-w-5xl mx-auto">
              {comprovante ? (
                <Card id="comprovante-print-card" className="max-w-md mx-auto p-6 sm:p-7 space-y-5 shadow-sm animate-fadeIn">
                  <div className="flex items-center gap-3 pb-4 border-b border-[var(--border-subtle)]">
                    <div className="h-10 w-10 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
                      <CheckCircle2 size={22} />
                    </div>
                    <div>
                      <h2 className="text-base font-semibold">Ponto Registrado</h2>
                      <p className="text-xs text-[var(--text-secondary)]">
                        Comprovante Portaria 671/MTP · {tenantBranding.nome}
                      </p>
                    </div>
                  </div>

                  <dl className="grid grid-cols-2 gap-4 text-xs">
                    <div className="col-span-2">
                      <dt className="text-[var(--text-muted)]">Colaborador</dt>
                      <dd className="text-sm font-semibold mt-0.5">
                        {comprovante.nomeColaborador || comprovante.colaborador || '—'}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-[var(--text-muted)]">Tipo</dt>
                      <dd className="mt-1">
                        <Badge tone={comprovante.tipoRegistro === 'ENTRADA' ? 'success' : 'brand'}>
                          {comprovante.tipoRegistro}
                        </Badge>
                      </dd>
                    </div>
                    <div>
                      <dt className="text-[var(--text-muted)]">NSR</dt>
                      <dd className="font-mono-tabular font-semibold mt-1">{comprovante.nsr ?? '—'}</dd>
                    </div>
                    <div className="col-span-2">
                      <dt className="text-[var(--text-muted)]">Data e Hora Oficial</dt>
                      <dd className="font-mono-tabular font-semibold mt-0.5">
                        {comprovante.dataHora || comprovante.horario || horaAtual.toLocaleString('pt-BR')}
                      </dd>
                    </div>
                  </dl>

                  <div className="flex items-center justify-end gap-2 pt-4 border-t border-[var(--border-subtle)] no-print">
                    <Button variant="secondary" size="md" onClick={() => window.print()}>
                      <Printer size={15} />
                      Imprimir
                    </Button>
                    <Button variant="primary" size="md" onClick={() => setComprovante(null)}>
                      Novo Registro
                    </Button>
                  </div>
                </Card>
              ) : (
                <Card className="overflow-hidden shadow-xs">
                  <div className="grid grid-cols-1 lg:grid-cols-12">
                    {/* Relógio + Câmera */}
                    <div className="lg:col-span-7 p-6 sm:p-8 border-b lg:border-b-0 lg:border-r border-[var(--border-subtle)] bg-[var(--surface-sidebar)]/50 space-y-6">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <div className="inline-flex items-center gap-2 text-xs font-medium text-[var(--text-secondary)]">
                          <span className="h-2 w-2 rounded-full bg-emerald-500" />
                          {estacaoAtual ? (
                            <span>
                              {estacaoAtual.identificacao}
                              {estacaoAtual.local ? ` · ${estacaoAtual.local}` : ''}
                            </span>
                          ) : (
                            <span className="text-amber-600 dark:text-amber-400">
                              Nenhuma estação ativa configurada
                            </span>
                          )}
                        </div>
                        <span className="text-xs text-[var(--text-muted)] capitalize">
                          {horaAtual.toLocaleDateString('pt-BR', {
                            weekday: 'long',
                            day: '2-digit',
                            month: 'long',
                          })}
                        </span>
                      </div>

                      <div className="py-2 text-center">
                        <div className="text-5xl sm:text-6xl font-bold font-mono-tabular tracking-tight text-[var(--text-primary)]">
                          {horaAtual.toLocaleTimeString('pt-BR')}
                        </div>
                        {estacaoAtual?.local && (
                          <div className="mt-2 inline-flex items-center gap-1 text-xs text-[var(--text-muted)]">
                            <MapPin size={12} />
                            {estacaoAtual.local}
                          </div>
                        )}
                      </div>

                      <div className="rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] overflow-hidden aspect-[16/10] relative flex items-center justify-center">
                        {fotoPreview ? (
                          <img src={fotoPreview} alt="Foto capturada" className="w-full h-full object-cover" />
                        ) : (
                          <video
                            ref={videoRef}
                            autoPlay
                            playsInline
                            muted
                            className={`w-full h-full object-cover ${cameraAtiva ? 'block' : 'hidden'}`}
                          />
                        )}

                        {!cameraAtiva && !fotoPreview && (
                          <div className="text-center p-6 space-y-3">
                            <div
                              className="mx-auto h-11 w-11 rounded-xl flex items-center justify-center"
                              style={{ backgroundColor: 'var(--brand-50)', color: 'var(--brand-600)' }}
                            >
                              <Camera size={22} />
                            </div>
                            <p className="text-xs text-[var(--text-secondary)] max-w-xs mx-auto">
                              {cameraErro || 'Ative a câmera para registrar sua foto no momento da marcação.'}
                            </p>
                            <div className="flex flex-wrap items-center justify-center gap-2 pt-1">
                              <Button variant="primary" size="sm" onClick={iniciarCamera}>
                                <Camera size={14} />
                                Abrir Câmera
                              </Button>
                              <label className="inline-flex items-center gap-1.5 h-8 px-3 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] hover:bg-[var(--surface-hover)] text-xs font-medium cursor-pointer transition-colors">
                                <Upload size={14} />
                                Anexar foto
                                <input type="file" accept="image/*" className="hidden" onChange={handleUploadFoto} />
                              </label>
                            </div>
                          </div>
                        )}
                        <canvas ref={canvasRef} className="hidden" />
                      </div>

                      {(cameraAtiva || fotoPreview) && (
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex items-center gap-2">
                            {cameraAtiva && !fotoPreview && (
                              <Button variant="secondary" size="sm" onClick={handleCapturarFoto}>
                                <Camera size={14} />
                                Capturar agora
                              </Button>
                            )}
                            {fotoPreview && (
                              <Button
                                variant="secondary"
                                size="sm"
                                onClick={() => {
                                  setFotoBlob(null);
                                  setFotoPreview(null);
                                }}
                              >
                                <RefreshCw size={13} />
                                Refazer foto
                              </Button>
                            )}
                          </div>
                          {cameraAtiva && (
                            <Button variant="ghost" size="sm" onClick={pararCamera}>
                              Desligar câmera
                            </Button>
                          )}
                        </div>
                      )}
                    </div>

                    {/* Formulário Bater Ponto */}
                    <div className="lg:col-span-5 p-6 sm:p-8 flex flex-col justify-between bg-[var(--surface-card)]">
                      <form onSubmit={handleBaterPonto} className="space-y-5">
                        <div>
                          <h1 className="text-lg font-semibold tracking-tight">Registrar Ponto</h1>
                          <p className="mt-1 text-xs text-[var(--text-secondary)]">
                            Informe suas credenciais corporativas para registrar entrada ou saída.
                          </p>
                        </div>

                        <div className="space-y-4">
                          <div className="space-y-1.5">
                            <label htmlFor="ponto-user" className="block text-xs font-medium">
                              Usuário
                            </label>
                            <input
                              id="ponto-user"
                              type="text"
                              autoComplete="username"
                              value={pontoUsuario}
                              onChange={(e) => setPontoUsuario(e.target.value)}
                              disabled={submittingPonto}
                              placeholder="seu.usuario"
                              className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                            />
                          </div>

                          <div className="space-y-1.5">
                            <label htmlFor="ponto-pass" className="block text-xs font-medium">
                              Senha
                            </label>
                            <div className="relative">
                              <input
                                id="ponto-pass"
                                type={mostrarPontoSenha ? 'text' : 'password'}
                                autoComplete="current-password"
                                value={pontoSenha}
                                onChange={(e) => setPontoSenha(e.target.value)}
                                disabled={submittingPonto}
                                placeholder="••••••••"
                                className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] pl-3 pr-10 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                              />
                              <button
                                type="button"
                                onClick={() => setMostrarPontoSenha((v) => !v)}
                                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)] p-1 cursor-pointer"
                                aria-label={mostrarPontoSenha ? 'Ocultar senha' : 'Mostrar senha'}
                              >
                                {mostrarPontoSenha ? <EyeOff size={16} /> : <Eye size={16} />}
                              </button>
                            </div>
                          </div>
                        </div>

                        <Button
                          type="submit"
                          variant="primary"
                          size="lg"
                          className="w-full"
                          disabled={submittingPonto || !estacaoAtual}
                        >
                          {submittingPonto ? (
                            <>
                              <Loader2 size={16} className="animate-spin" />
                              Registrando marcação...
                            </>
                          ) : (
                            'Confirmar Registro de Ponto'
                          )}
                        </Button>
                      </form>

                      <div className="mt-8 pt-5 border-t border-[var(--border-subtle)] space-y-3">
                        <p className="text-[11px] text-[var(--text-muted)] leading-relaxed">
                          O horário oficial é validado pelo servidor e o comprovante é emitido
                          conforme a Portaria 671/MTP.
                        </p>
                        <div className="flex items-center justify-between text-xs">
                          <span className="text-[var(--text-secondary)]">Espelho, ajustes ou RH?</span>
                          <button
                            type="button"
                            onClick={() => irParaPane('login')}
                            className="font-semibold hover:underline inline-flex items-center gap-1 cursor-pointer transition-transform hover:translate-x-0.5"
                            style={{ color: 'var(--brand-600)' }}
                          >
                            Acessar Portal
                            <ArrowRight size={13} />
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </Card>
              )}
            </div>
          </section>

          {/* ==================== PAINEL 2 (DIREITO - 100vw): LOGIN CORPORATIVO CENTRALIZADO ==================== */}
          <section
            data-active={isLoginPane}
            aria-hidden={!isLoginPane}
            inert={!isLoginPane ? '' : undefined}
            className="tenant-slide-pane px-4 sm:px-6 py-8 flex items-center justify-center"
          >
            <div className="w-full max-w-md mx-auto">
              <Card className="p-6 sm:p-8 shadow-xs">
                <div className="flex items-center gap-3 pb-5 mb-6 border-b border-[var(--border-subtle)]">
                  <div
                    className="h-10 w-10 rounded-xl flex items-center justify-center shrink-0"
                    style={{ backgroundColor: 'var(--brand-50)', color: 'var(--brand-600)' }}
                  >
                    <ShieldCheck size={20} />
                  </div>
                  <div>
                    <h2 className="text-base font-semibold tracking-tight">
                      Portal Corporativo
                    </h2>
                    <p className="text-xs text-[var(--text-secondary)]">
                      Acesso para Colaboradores, Gestores e RH
                    </p>
                  </div>
                </div>

                <form onSubmit={handleLoginTenant} className="space-y-4">
                  <div className="space-y-1.5">
                    <label htmlFor="tenant-login-user" className="block text-xs font-medium">
                      Usuário
                    </label>
                    <input
                      id="tenant-login-user"
                      type="text"
                      autoComplete="username"
                      value={loginUsuario}
                      onChange={(e) => setLoginUsuario(e.target.value)}
                      disabled={submittingLogin}
                      placeholder="seu.usuario"
                      className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label htmlFor="tenant-login-pass" className="block text-xs font-medium">
                      Senha
                    </label>
                    <div className="relative">
                      <input
                        id="tenant-login-pass"
                        type={mostrarLoginSenha ? 'text' : 'password'}
                        autoComplete="current-password"
                        value={loginSenha}
                        onChange={(e) => setLoginSenha(e.target.value)}
                        disabled={submittingLogin}
                        placeholder="••••••••"
                        className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] pl-3 pr-10 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                      />
                      <button
                        type="button"
                        onClick={() => setMostrarLoginSenha((v) => !v)}
                        className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)] p-1 cursor-pointer"
                        aria-label={mostrarLoginSenha ? 'Ocultar senha' : 'Mostrar senha'}
                      >
                        {mostrarLoginSenha ? <EyeOff size={16} /> : <Eye size={16} />}
                      </button>
                    </div>
                  </div>

                  <Button type="submit" variant="primary" size="lg" className="w-full mt-2" disabled={submittingLogin}>
                    {submittingLogin ? (
                      <>
                        <Loader2 size={16} className="animate-spin" />
                        Autenticando...
                      </>
                    ) : (
                      'Entrar no Portal'
                    )}
                  </Button>
                </form>

                <div className="mt-6 pt-4 border-t border-[var(--border-subtle)] flex items-center justify-center">
                  <button
                    type="button"
                    onClick={() => irParaPane('relogio')}
                    className="inline-flex items-center gap-1.5 text-xs font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)] cursor-pointer transition-transform hover:-translate-x-0.5"
                  >
                    <ArrowLeft size={14} />
                    Voltar ao Relógio de Ponto
                  </button>
                </div>
              </Card>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
