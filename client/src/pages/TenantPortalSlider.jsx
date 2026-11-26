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
import {
  formatarCnpj,
  formatarCpf,
  formatarDataHora,
  formatarNsr,
} from '../utils/brazilianDocuments';
import BrandLogo from '../components/ui/BrandLogo';
import ThemeToggle from '../components/ui/ThemeToggle';
import { Badge, Button, Card } from '../components/ui/Primitives';

const API_ORIGIN = API_BASE.replace(/\/api\/v1\/?$/, '');
const TEMPO_EXIBICAO_COMPROVANTE_SEG = 7;

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
  const [comprovanteFoto, setComprovanteFoto] = useState(null);
  const [segundosRestantes, setSegundosRestantes] = useState(TEMPO_EXIBICAO_COMPROVANTE_SEG);
  const [timerPausado, setTimerPausado] = useState(false);

  const fecharComprovante = () => {
    setComprovante(null);
    setComprovanteFoto(null);
    setTimerPausado(false);
    setSegundosRestantes(TEMPO_EXIBICAO_COMPROVANTE_SEG);
  };

  // Auto-fechamento da tela de confirmação para liberar o terminal para o próximo colaborador (T07)
  useEffect(() => {
    if (!comprovante || timerPausado) return;
    if (segundosRestantes <= 0) {
      fecharComprovante();
      return;
    }
    const timer = setTimeout(() => {
      setSegundosRestantes((prev) => prev - 1);
    }, 1000);
    return () => clearTimeout(timer);
  }, [comprovante, segundosRestantes, timerPausado]);

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
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setCameraAtiva(false);
  };

  useEffect(() => {
    if (cameraAtiva && videoRef.current && streamRef.current) {
      if (videoRef.current.srcObject !== streamRef.current) {
        videoRef.current.srcObject = streamRef.current;
      }
    }
  }, [cameraAtiva, fotoPreview, comprovante]);

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

      const previewConfirmada = fotoPreview || URL.createObjectURL(blobParaEnviar);
      pararCamera();
      setComprovanteFoto(previewConfirmada);
      setComprovante(data);
      setSegundosRestantes(TEMPO_EXIBICAO_COMPROVANTE_SEG);
      setTimerPausado(false);
      setPontoUsuario('');
      setPontoSenha('');
      setFotoBlob(null);
      setFotoPreview(null);
      toast.success(data?.mensagem || 'Ponto registrado com sucesso!');
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
      {/* Header fixo compartilhado de ponta a ponta (justify-between) com logo maior e pill deslizante */}
      <header className="min-h-20 border-b border-[var(--border-subtle)] bg-[var(--surface-card)]/95 backdrop-blur-xs px-6 sm:px-10 lg:px-14 sticky top-0 z-30 no-print flex items-center">
        <div className="w-full flex items-center justify-between gap-4">
          <BrandLogo
            name={tenantBranding.nome}
            subtitle={isLoginPane ? 'Acesso ao Portal Corporativo' : 'Estação & Relógio de Ponto'}
            logoUrl={tenantBranding.logoUrl}
            size="md"
          />

          <div className="flex items-center gap-3">
            {/* Segmented Pill Switcher com indicador deslizante */}
            <div
              role="tablist"
              aria-label="Alternar entre Relógio de Ponto e Login"
              className="relative grid grid-cols-2 p-1.5 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-subtle)] min-w-[270px] sm:min-w-[330px]"
            >
              {/* Pílula de fundo que desliza junto com a tela */}
              <span
                aria-hidden="true"
                className="pointer-events-none absolute top-1.5 bottom-1.5 left-1.5 w-[calc(50%-6px)] rounded-lg shadow-xs transition-transform duration-500 ease-[cubic-bezier(0.22,1,0.36,1)]"
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
                className={`relative z-10 min-h-9 px-3.5 py-1.5 rounded-lg text-sm inline-flex items-center justify-center gap-2 transition-colors duration-200 cursor-pointer ${
                  !isLoginPane
                    ? 'text-[var(--brand-fg)] font-semibold'
                    : 'text-[var(--text-secondary)] font-medium hover:text-[var(--text-primary)]'
                }`}
              >
                <Clock size={16} />
                <span>Relógio de Ponto</span>
              </button>

              <button
                type="button"
                role="tab"
                aria-selected={isLoginPane}
                onClick={() => irParaPane('login')}
                className={`relative z-10 min-h-9 px-3.5 py-1.5 rounded-lg text-sm inline-flex items-center justify-center gap-2 transition-colors duration-200 cursor-pointer ${
                  isLoginPane
                    ? 'text-[var(--brand-fg)] font-semibold'
                    : 'text-[var(--text-secondary)] font-medium hover:text-[var(--text-primary)]'
                }`}
              >
                <LogIn size={16} />
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
            className="tenant-slide-pane px-4 sm:px-8 py-8 flex items-center justify-center"
          >
            <div className="w-full max-w-6xl mx-auto">
              {comprovante ? (
                <Card
                  id="comprovante-print-card"
                  className="max-w-xl mx-auto p-7 sm:p-8 space-y-6 shadow-sm animate-fadeIn relative overflow-hidden"
                >
                  {/* Barra visual de contagem regressiva para auto-fechamento (T07) */}
                  <div className="no-print absolute top-0 left-0 right-0 h-1.5 bg-[var(--surface-subtle)]">
                    <div
                      className="h-full transition-all duration-1000 ease-linear"
                      style={{
                        backgroundColor: 'var(--brand-600)',
                        width: `${Math.max(0, (segundosRestantes / TEMPO_EXIBICAO_COMPROVANTE_SEG) * 100)}%`,
                      }}
                    />
                  </div>

                  <div className="flex items-start justify-between gap-4 pb-5 border-b border-[var(--border-subtle)]">
                    <div className="flex items-center gap-3.5">
                      <div className="h-12 w-12 rounded-2xl bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
                        <CheckCircle2 size={26} />
                      </div>
                      <div>
                        <h2 className="text-xl font-bold tracking-tight">Ponto Registrado com Sucesso</h2>
                        <p className="text-xs font-medium text-[var(--text-secondary)]">
                          Comprovante Oficial Portaria 671/MTP · {comprovante.nomeEmpresa || tenantBranding.nome}
                        </p>
                      </div>
                    </div>

                    <div className="text-right shrink-0">
                      <span className="text-[11px] font-bold uppercase tracking-wider text-[var(--text-muted)] block">
                        NSR Oficial
                      </span>
                      <span
                        className="font-mono-tabular text-lg font-extrabold"
                        style={{ color: 'var(--brand-600)' }}
                      >
                        #{formatarNsr(comprovante.nsr)}
                      </span>
                    </div>
                  </div>

                  {/* Identificação do Colaborador + Foto Capturada no Momento da Batida */}
                  <div className="flex flex-col sm:flex-row items-center gap-4 p-4 rounded-2xl border border-[var(--border-subtle)] bg-[var(--surface-subtle)]/60">
                    {comprovanteFoto && (
                      <img
                        src={comprovanteFoto}
                        alt="Foto registrada na batida"
                        className="h-20 w-20 rounded-xl object-cover border border-[var(--border-subtle)] shrink-0 shadow-xs no-print"
                      />
                    )}
                    <div className="min-w-0 flex-1 text-center sm:text-left">
                      <span className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
                        Colaborador Identificado
                      </span>
                      <div className="text-lg font-bold text-[var(--text-primary)] truncate mt-0.5">
                        {comprovante.nomeFuncionario ||
                          comprovante.nomeColaborador ||
                          comprovante.colaborador ||
                          'Colaborador'}
                      </div>
                      <div className="mt-1 flex flex-wrap items-center justify-center sm:justify-start gap-2 text-xs text-[var(--text-secondary)]">
                        {comprovante.matricula && (
                          <span className="font-mono-tabular font-semibold px-2 py-0.5 rounded-md bg-[var(--surface-card)] border border-[var(--border-subtle)]">
                            Matrícula: {comprovante.matricula}
                          </span>
                        )}
                        {comprovante.cpfFuncionario && (
                          <span className="font-mono-tabular font-semibold px-2 py-0.5 rounded-md bg-[var(--surface-card)] border border-[var(--border-subtle)]">
                            CPF: {formatarCpf(comprovante.cpfFuncionario)}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                    <div className="p-3.5 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)]">
                      <dt className="text-xs font-semibold text-[var(--text-muted)]">
                        Data e Hora Oficial do Servidor
                      </dt>
                      <dd className="font-mono-tabular text-base font-bold mt-1 text-[var(--text-primary)]">
                        {formatarDataHora(
                          comprovante.dataHoraServidor ||
                            comprovante.dataHoraBatida ||
                            comprovante.dataHora ||
                            horaAtual.toISOString()
                        )}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)]">
                      <dt className="text-xs font-semibold text-[var(--text-muted)]">
                        Registro & Origem
                      </dt>
                      <dd className="mt-1.5 flex flex-wrap items-center gap-2">
                        <Badge
                          tone={
                            comprovante.tipoRegistro === 'SAIDA'
                              ? 'brand'
                              : 'success'
                          }
                        >
                          {comprovante.tipoRegistro || 'CONFIRMADO'}
                        </Badge>
                        <span className="text-xs font-semibold text-[var(--text-secondary)]">
                          {comprovante.estacaoIdentificacao || estacaoAtual?.identificacao || 'Estação Web'}
                        </span>
                      </dd>
                    </div>

                    <div className="sm:col-span-2 p-3.5 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] flex flex-wrap items-center justify-between gap-2 text-xs">
                      <div>
                        <span className="text-[var(--text-muted)] font-medium">Empregador: </span>
                        <span className="font-bold text-[var(--text-primary)]">
                          {comprovante.nomeEmpresa || tenantBranding.nome}
                        </span>
                      </div>
                      {(comprovante.cnpjEmpresa || tenantBranding.cnpj) && (
                        <div className="font-mono-tabular font-semibold text-[var(--text-secondary)]">
                          CNPJ: {formatarCnpj(comprovante.cnpjEmpresa || tenantBranding.cnpj)}
                        </div>
                      )}
                    </div>
                  </dl>

                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pt-4 border-t border-[var(--border-subtle)] no-print">
                    <span className="text-xs font-medium text-[var(--text-muted)]">
                      {timerPausado
                        ? 'Fechamento automático pausado'
                        : `Liberando terminal para o próximo colaborador em ${segundosRestantes}s...`}
                    </span>

                    <div className="flex items-center justify-end gap-2.5">
                      <Button
                        variant="secondary"
                        size="md"
                        onClick={() => {
                          setTimerPausado(true);
                          window.print();
                        }}
                      >
                        <Printer size={16} />
                        Imprimir
                      </Button>
                      <Button variant="primary" size="md" onClick={fecharComprovante}>
                        Novo Registro Agora
                      </Button>
                    </div>
                  </div>
                </Card>
              ) : (
                <Card className="overflow-hidden shadow-sm">
                  <div className="grid grid-cols-1 lg:grid-cols-12">
                    {/* Relógio + Câmera */}
                    <div className="lg:col-span-7 p-7 sm:p-10 border-b lg:border-b-0 lg:border-r border-[var(--border-subtle)] bg-[var(--surface-sidebar)]/55 flex flex-col justify-between space-y-6">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <div className="inline-flex items-center gap-2 text-sm font-semibold text-[var(--text-secondary)]">
                          <span className="h-2.5 w-2.5 rounded-full bg-emerald-500" />
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
                        <span className="text-sm font-medium text-[var(--text-muted)] capitalize">
                          {horaAtual.toLocaleDateString('pt-BR', {
                            weekday: 'long',
                            day: '2-digit',
                            month: 'long',
                          })}
                        </span>
                      </div>

                      <div className="py-2 text-center">
                        <div className="text-5xl sm:text-6xl lg:text-7xl font-bold font-mono-tabular tracking-tight text-[var(--text-primary)]">
                          {horaAtual.toLocaleTimeString('pt-BR')}
                        </div>
                        {estacaoAtual?.local && (
                          <div className="mt-2.5 inline-flex items-center gap-1.5 text-sm font-medium text-[var(--text-secondary)]">
                            <MapPin size={15} />
                            {estacaoAtual.local}
                          </div>
                        )}
                      </div>

                      <div className="rounded-2xl border border-[var(--border-subtle)] bg-[var(--surface-card)] overflow-hidden aspect-[16/10] relative flex items-center justify-center">
                        {fotoPreview && (
                          <img src={fotoPreview} alt="Foto capturada" className="w-full h-full object-cover" />
                        )}
                        <video
                          ref={videoRef}
                          autoPlay
                          playsInline
                          muted
                          className={`w-full h-full object-cover ${cameraAtiva && !fotoPreview ? 'block' : 'hidden'}`}
                        />

                        {!cameraAtiva && !fotoPreview && (
                          <div className="text-center p-6 space-y-3.5">
                            <div
                              className="mx-auto h-13 w-13 rounded-2xl flex items-center justify-center"
                              style={{ backgroundColor: 'var(--brand-50)', color: 'var(--brand-600)' }}
                            >
                              <Camera size={26} />
                            </div>
                            <p className="text-sm font-medium text-[var(--text-secondary)] max-w-sm mx-auto">
                              {cameraErro || 'Ative a câmera para registrar sua foto no momento da marcação.'}
                            </p>
                            <div className="flex flex-wrap items-center justify-center gap-2.5 pt-1">
                              <Button variant="primary" size="md" onClick={iniciarCamera}>
                                <Camera size={16} />
                                Abrir Câmera
                              </Button>
                              <label className="inline-flex items-center gap-2 min-h-10 px-4 py-2 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] hover:bg-[var(--surface-hover)] text-sm font-semibold cursor-pointer transition-colors">
                                <Upload size={16} />
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
                              <Button variant="secondary" size="md" onClick={handleCapturarFoto}>
                                <Camera size={16} />
                                Capturar agora
                              </Button>
                            )}
                            {fotoPreview && (
                              <Button
                                variant="secondary"
                                size="md"
                                onClick={() => {
                                  setFotoBlob(null);
                                  setFotoPreview(null);
                                }}
                              >
                                <RefreshCw size={15} />
                                Refazer foto
                              </Button>
                            )}
                          </div>
                          {cameraAtiva && (
                            <Button variant="ghost" size="md" onClick={pararCamera}>
                              Desligar câmera
                            </Button>
                          )}
                        </div>
                      )}
                    </div>

                    {/* Formulário Bater Ponto — centralizado vertical e horizontalmente */}
                    <div className="lg:col-span-5 p-7 sm:p-10 flex flex-col justify-center bg-[var(--surface-card)]">
                      <div className="w-full max-w-sm mx-auto space-y-6">
                        <div className="text-center space-y-1.5">
                          <div
                            className="mx-auto h-12 w-12 rounded-2xl flex items-center justify-center mb-3"
                            style={{ backgroundColor: 'var(--brand-50)', color: 'var(--brand-600)' }}
                          >
                            <Clock size={24} />
                          </div>
                          <h1 className="text-2xl font-bold tracking-tight">Registrar Ponto</h1>
                          <p className="text-sm text-[var(--text-secondary)]">
                            Informe suas credenciais corporativas para registrar entrada ou saída.
                          </p>
                        </div>

                        <form onSubmit={handleBaterPonto} className="space-y-4">
                          <div className="space-y-1.5">
                            <label htmlFor="ponto-user" className="block text-sm font-semibold text-[var(--text-primary)]">
                              Usuário
                            </label>
                            <input
                              id="ponto-user"
                              name="usuario"
                              type="text"
                              autoComplete="username"
                              autoCorrect="off"
                              autoCapitalize="none"
                              spellCheck={false}
                              value={pontoUsuario}
                              onChange={(e) => setPontoUsuario(e.target.value)}
                              disabled={submittingPonto}
                              placeholder="seu.usuario"
                              className="w-full min-h-12 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] px-4 text-base focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                            />
                          </div>

                          <div className="space-y-1.5">
                            <label htmlFor="ponto-pass" className="block text-sm font-semibold text-[var(--text-primary)]">
                              Senha
                            </label>
                            <div className="relative">
                              <input
                                id="ponto-pass"
                                name="senha"
                                type={mostrarPontoSenha ? 'text' : 'password'}
                                autoComplete="current-password"
                                value={pontoSenha}
                                onChange={(e) => setPontoSenha(e.target.value)}
                                disabled={submittingPonto}
                                placeholder="••••••••"
                                className="w-full min-h-12 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] pl-4 pr-11 text-base focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                              />
                              <button
                                type="button"
                                onClick={() => setMostrarPontoSenha((v) => !v)}
                                aria-pressed={mostrarPontoSenha}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)] p-1.5 cursor-pointer"
                                aria-label={mostrarPontoSenha ? 'Ocultar senha' : 'Mostrar senha'}
                              >
                                {mostrarPontoSenha ? <EyeOff size={18} /> : <Eye size={18} />}
                              </button>
                            </div>
                          </div>

                          <Button
                            type="submit"
                            variant="primary"
                            size="lg"
                            className="w-full mt-2"
                            disabled={submittingPonto || !estacaoAtual}
                          >
                            {submittingPonto ? (
                              <>
                                <Loader2 size={18} className="animate-spin" />
                                Registrando marcação...
                              </>
                            ) : (
                              'Confirmar Registro de Ponto'
                            )}
                          </Button>
                        </form>

                        <div className="pt-5 border-t border-[var(--border-subtle)] space-y-3 text-center">
                          <p className="text-xs text-[var(--text-muted)] leading-relaxed">
                            O horário oficial é validado pelo servidor e o comprovante é emitido
                            conforme a Portaria 671/MTP.
                          </p>
                          <div className="flex items-center justify-center gap-2 text-sm">
                            <span className="text-[var(--text-secondary)]">Espelho, ajustes ou RH?</span>
                            <button
                              type="button"
                              onClick={() => irParaPane('login')}
                              className="font-bold hover:underline inline-flex items-center gap-1 cursor-pointer transition-transform hover:translate-x-0.5"
                              style={{ color: 'var(--brand-600)' }}
                            >
                              Acessar Portal
                              <ArrowRight size={15} />
                            </button>
                          </div>
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
            className="tenant-slide-pane px-4 sm:px-8 py-8 flex items-center justify-center"
          >
            <div className="w-full max-w-[460px] mx-auto">
              <Card className="p-8 sm:p-10 shadow-sm">
                <div className="flex flex-col items-center text-center pb-6 mb-6 border-b border-[var(--border-subtle)]">
                  <div
                    className="h-13 w-13 rounded-2xl flex items-center justify-center shrink-0 mb-3"
                    style={{ backgroundColor: 'var(--brand-50)', color: 'var(--brand-600)' }}
                  >
                    <ShieldCheck size={26} />
                  </div>
                  <h2 className="text-2xl font-bold tracking-tight">
                    Portal Corporativo
                  </h2>
                  <p className="mt-1 text-sm text-[var(--text-secondary)]">
                    Acesso para Colaboradores, Gestores e RH
                  </p>
                </div>

                <form onSubmit={handleLoginTenant} className="space-y-4">
                  <div className="space-y-1.5">
                    <label htmlFor="tenant-login-user" className="block text-sm font-semibold text-[var(--text-primary)]">
                      Usuário
                    </label>
                    <input
                      id="tenant-login-user"
                      name="usuario"
                      type="text"
                      autoComplete="username"
                      autoCorrect="off"
                      autoCapitalize="none"
                      spellCheck={false}
                      value={loginUsuario}
                      onChange={(e) => setLoginUsuario(e.target.value)}
                      disabled={submittingLogin}
                      placeholder="seu.usuario"
                      className="w-full min-h-12 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] px-4 text-base focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label htmlFor="tenant-login-pass" className="block text-sm font-semibold text-[var(--text-primary)]">
                      Senha
                    </label>
                    <div className="relative">
                      <input
                        id="tenant-login-pass"
                        name="senha"
                        type={mostrarLoginSenha ? 'text' : 'password'}
                        autoComplete="current-password"
                        value={loginSenha}
                        onChange={(e) => setLoginSenha(e.target.value)}
                        disabled={submittingLogin}
                        placeholder="••••••••"
                        className="w-full min-h-12 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-card)] pl-4 pr-11 text-base focus:outline-none focus:ring-2 focus:ring-[var(--brand-600)]/25 focus:border-[var(--brand-600)]"
                      />
                      <button
                        type="button"
                        onClick={() => setMostrarLoginSenha((v) => !v)}
                        aria-pressed={mostrarLoginSenha}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)] p-1.5 cursor-pointer"
                        aria-label={mostrarLoginSenha ? 'Ocultar senha' : 'Mostrar senha'}
                      >
                        {mostrarLoginSenha ? <EyeOff size={18} /> : <Eye size={18} />}
                      </button>
                    </div>
                  </div>

                  <Button type="submit" variant="primary" size="lg" className="w-full mt-3" disabled={submittingLogin}>
                    {submittingLogin ? (
                      <>
                        <Loader2 size={18} className="animate-spin" />
                        Autenticando...
                      </>
                    ) : (
                      'Entrar no Portal'
                    )}
                  </Button>
                </form>

                <div className="mt-6 pt-5 border-t border-[var(--border-subtle)] flex items-center justify-center">
                  <button
                    type="button"
                    onClick={() => irParaPane('relogio')}
                    className="inline-flex items-center gap-2 text-sm font-semibold text-[var(--text-secondary)] hover:text-[var(--text-primary)] cursor-pointer transition-transform hover:-translate-x-0.5"
                  >
                    <ArrowLeft size={16} />
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
