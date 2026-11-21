import React, { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowRight,
  Building2,
  CheckCircle2,
  Clock,
  FileCheck2,
  FileSpreadsheet,
  Layers,
  Lock,
  Scale,
  ShieldCheck,
  SlidersHorizontal,
  Users,
} from 'lucide-react';
import BrandLogo from '../components/ui/BrandLogo';
import ThemeToggle from '../components/ui/ThemeToggle';
import { Button, Badge, Card } from '../components/ui/Primitives';

const NAV_SECTIONS = [
  { id: 'recursos', label: 'Recursos' },
  { id: 'como-funciona', label: 'Como funciona' },
  { id: 'whitelabel', label: 'White-Label' },
  { id: 'seguranca', label: 'Segurança & LGPD' },
];

const FEATURES = [
  {
    icon: Clock,
    title: 'Apuração Diária Automática',
    description:
      'Cálculo automático de horas normais, extras por faixa, adicional noturno, atrasos e tolerâncias conforme a CLT.',
  },
  {
    icon: FileCheck2,
    title: 'Conformidade Portaria 671/MTP',
    description:
      'Comprovantes imutáveis com NSR sequencial, trilha de auditoria em ajustes, preservação de histórico e bloqueio de mês fechado.',
  },
  {
    icon: Users,
    title: 'Fluxo por Hierarquia Real',
    description:
      'Colaboradores solicitam ajustes, gestores analisam apenas sua equipe e o RH consolida ocorrências com privacidade LGPD.',
  },
  {
    icon: Layers,
    title: 'Banco de Horas e Ocorrências',
    description:
      'Gestão completa de créditos, débitos, compensações, férias e atestados médicos anexados com acesso restrito ao colaborador e RH.',
  },
  {
    icon: FileSpreadsheet,
    title: 'Fechamento e Folha de Pagamento',
    description:
      'Checklist automático de pendências antes do fechamento mensal, reabertura auditada e exportação em CSV com integridade SHA-256.',
  },
  {
    icon: SlidersHorizontal,
    title: 'Portal White-Label por Empresa',
    description:
      'Cada empresa contratante acessa seu subdomínio exclusivo com logotipo próprio, paleta de cores institucional e dados isolados.',
  },
];

const WORKFLOW_STEPS = [
  {
    step: '01',
    title: 'Coleta confiável',
    text: 'Registro na estação web com foto ou importação de arquivos AFD/posicionais sem duplicidade e vínculo auditado de batidas órfãs.',
  },
  {
    step: '02',
    title: 'Tratamento ágil',
    text: 'Fila unificada de inconsistências, solicitações de ajuste pelo colaborador e aprovação direta pelo gestor da equipe.',
  },
  {
    step: '03',
    title: 'Fechamento seguro',
    text: 'Bloqueio da competência mensal contra alterações retroativas e geração do arquivo final pronto para a contabilidade.',
  },
];

export default function LandingPage() {
  const navRef = useRef(null);

  // Scrollspy native (:target-current) + aria-current sync + IntersectionObserver fallback
  useEffect(() => {
    const navEl = navRef.current;
    if (!navEl) return;

    const links = Array.from(navEl.querySelectorAll('a[href^="#"]'));
    const supportsNative = typeof CSS !== 'undefined' && CSS.supports?.('scroll-target-group: auto');

    if (supportsNative) {
      const syncAria = () => {
        const current = navEl.querySelector('a:target-current');
        links.forEach((link) => {
          link.setAttribute('aria-current', link === current ? 'true' : 'false');
        });
      };
      syncAria();
      document.addEventListener('scrollend', syncAria);
      return () => document.removeEventListener('scrollend', syncAria);
    }

    const sections = NAV_SECTIONS.map((s) => document.getElementById(s.id)).filter(Boolean);
    const setActiveId = (activeId) => {
      links.forEach((link) => {
        const active = link.getAttribute('href') === `#${activeId}`;
        link.classList.toggle('target-current', active);
        link.setAttribute('aria-current', active ? 'true' : 'false');
      });
    };

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio);
        if (visible.length > 0) {
          setActiveId(visible[0].target.id);
        }
      },
      { rootMargin: '-35% 0px -45% 0px', threshold: [0, 0.25, 0.5] }
    );

    sections.forEach((sec) => observer.observe(sec));
    return () => observer.disconnect();
  }, []);

  const handleSmoothAnchor = (e, id) => {
    e.preventDefault();
    const target = document.getElementById(id);
    if (!target) return;
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    if (window.history?.replaceState) {
      window.history.replaceState(null, '', `#${id}`);
    }
  };

  return (
    <div className="min-h-screen bg-[var(--surface-page)] text-[var(--text-primary)] flex flex-col">
      {/* Header */}
      <header className="sticky top-0 z-30 border-b border-[var(--border-subtle)] bg-[var(--surface-card)]/95 backdrop-blur-xs">
        <div className="w-full px-6 sm:px-10 lg:px-14 min-h-20 flex items-center justify-between gap-4">
          <a
            href="#topo"
            onClick={(e) => {
              e.preventDefault();
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
          >
            <BrandLogo name="Ponto Certo" subtitle="Plataforma de Ponto Eletrônico" size="md" />
          </a>

          <nav
            ref={navRef}
            aria-label="Navegação principal"
            className="landing-scrollspy-nav hidden md:flex items-center gap-8 text-sm font-semibold"
          >
            {NAV_SECTIONS.map((item) => (
              <a
                key={item.id}
                href={`#${item.id}`}
                onClick={(e) => handleSmoothAnchor(e, item.id)}
              >
                {item.label}
              </a>
            ))}
          </nav>

          <div className="flex items-center gap-3">
            <ThemeToggle />
            <Link to="/login">
              <Button variant="primary" size="md">
                Acessar Portal
                <ArrowRight size={16} />
              </Button>
            </Link>
          </div>
        </div>
      </header>

      <main id="topo">
        {/* Hero */}
        <section className="border-b border-[var(--border-subtle)] bg-[var(--surface-card)]">
          <div className="max-w-6xl mx-auto px-4 sm:px-6 py-16 lg:py-24 grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            <div className="lg:col-span-6 space-y-6">
              <div>
                <Badge tone="brand">
                  <Scale size={12} />
                  Portaria 671/MTP · LGPD · Multi-Tenant
                </Badge>
              </div>

              <h1 className="text-3xl sm:text-4xl lg:text-[44px] font-bold tracking-tight text-[var(--text-primary)] leading-[1.12]">
                O controle de ponto que o seu RH fecha em minutos, não em dias.
              </h1>

              <p className="text-base text-[var(--text-secondary)] leading-relaxed max-w-xl">
                Centralize marcações, apuração diária, banco de horas, ocorrências e fechamento de
                folha em um portal exclusivo com o subdomínio e a identidade visual da sua empresa.
              </p>

              <div className="flex flex-wrap items-center gap-3 pt-1">
                <Link to="/login">
                  <Button variant="primary" size="lg">
                    Acessar minha empresa
                    <ArrowRight size={16} />
                  </Button>
                </Link>
                <a href="#como-funciona" onClick={(e) => handleSmoothAnchor(e, 'como-funciona')}>
                  <Button variant="secondary" size="lg">
                    Ver como funciona
                  </Button>
                </a>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 pt-2">
                {[
                  'Subdomínio dedicado por cliente',
                  'Isolamento estrito de dados (tenant)',
                  'Atestados protegidos pela LGPD',
                  'Tema claro, escuro e cor da marca',
                ].map((item) => (
                  <div key={item} className="flex items-center gap-2 text-xs text-[var(--text-secondary)]">
                    <CheckCircle2 size={15} className="shrink-0" style={{ color: 'var(--brand-600)' }} />
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Product Preview */}
            <div className="lg:col-span-6">
              <Card className="overflow-hidden shadow-sm transition-transform duration-300 hover:-translate-y-0.5">
                <div className="px-4 py-2.5 border-b border-[var(--border-subtle)] bg-[var(--surface-sidebar)] flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <span className="h-2.5 w-2.5 rounded-full bg-slate-300 dark:bg-zinc-700" />
                    <span className="h-2.5 w-2.5 rounded-full bg-slate-300 dark:bg-zinc-700" />
                    <span className="h-2.5 w-2.5 rounded-full bg-slate-300 dark:bg-zinc-700" />
                  </div>
                  <span className="text-[11px] font-mono-tabular text-[var(--text-muted)] px-2.5 py-0.5 rounded-md bg-[var(--surface-card)] border border-[var(--border-subtle)]">
                    suaempresa.pontocerto.com.br
                  </span>
                  <Badge tone="success">Online</Badge>
                </div>

                <div className="p-5 sm:p-6 space-y-5">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="text-xs text-[var(--text-muted)]">Apuração Mensal</div>
                      <div className="text-sm font-semibold mt-0.5">Visão Consolidada do RH</div>
                    </div>
                    <Badge tone="brand">Competência Aberta</Badge>
                  </div>

                  <div className="grid grid-cols-3 gap-3">
                    <div className="rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3">
                      <div className="text-[11px] text-[var(--text-muted)]">Colaboradores</div>
                      <div className="text-xl font-bold font-mono-tabular mt-1">148</div>
                      <div className="text-[11px] text-emerald-600 dark:text-emerald-400 mt-0.5">98,6% ativos</div>
                    </div>
                    <div className="rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3">
                      <div className="text-[11px] text-[var(--text-muted)]">Marcações</div>
                      <div className="text-xl font-bold font-mono-tabular mt-1">6.420</div>
                      <div className="text-[11px] text-[var(--text-secondary)] mt-0.5">100% auditadas</div>
                    </div>
                    <div className="rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] p-3">
                      <div className="text-[11px] text-[var(--text-muted)]">Pendências</div>
                      <div className="text-xl font-bold font-mono-tabular mt-1">2</div>
                      <div className="text-[11px] text-amber-600 dark:text-amber-400 mt-0.5">Para revisão</div>
                    </div>
                  </div>

                  <div className="rounded-lg border border-[var(--border-subtle)] overflow-hidden">
                    <div className="bg-[var(--surface-subtle)] px-3.5 py-2 text-[11px] font-semibold uppercase tracking-wider text-[var(--text-muted)] grid grid-cols-12">
                      <span className="col-span-5">Colaborador</span>
                      <span className="col-span-4">Jornada do Dia</span>
                      <span className="col-span-3 text-right">Saldo</span>
                    </div>
                    {[
                      { nome: 'Ana Paula Costa', horas: '08:00 · 12:00 · 13:00 · 17:02', saldo: '+00:02', ok: true },
                      { nome: 'Lucas Andrade', horas: '08:01 · 12:00 · 13:02 · 17:15', saldo: '+00:12', ok: true },
                      { nome: 'Marina Oliveira', horas: '08:00 · 12:00 · 13:00 · —', saldo: 'Ajuste', ok: false },
                    ].map((row) => (
                      <div
                        key={row.nome}
                        className="px-3.5 py-2.5 border-t border-[var(--border-subtle)] grid grid-cols-12 items-center text-xs"
                      >
                        <span className="col-span-5 font-medium truncate">{row.nome}</span>
                        <span className="col-span-4 font-mono-tabular text-[11px] text-[var(--text-secondary)] truncate">
                          {row.horas}
                        </span>
                        <span
                          className={`col-span-3 text-right font-mono-tabular font-medium ${
                            row.ok ? 'text-emerald-600 dark:text-emerald-400' : 'text-amber-600 dark:text-amber-400'
                          }`}
                        >
                          {row.saldo}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </Card>
            </div>
          </div>
        </section>

        {/* Compliance Strip */}
        <div className="border-b border-[var(--border-subtle)] bg-[var(--surface-sidebar)]">
          <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 grid grid-cols-2 lg:grid-cols-4 gap-6">
            {[
              { label: 'Conformidade Legal', value: 'Portaria 671/MTP' },
              { label: 'Privacidade', value: 'LGPD por Padrão' },
              { label: 'Arquitetura', value: 'Multi-Tenant Isolado' },
              { label: 'Integridade da Folha', value: 'Hash SHA-256' },
            ].map((m) => (
              <div key={m.label} className="space-y-0.5">
                <div className="text-[11px] uppercase tracking-wider text-[var(--text-muted)] font-semibold">
                  {m.label}
                </div>
                <div className="text-sm font-semibold text-[var(--text-primary)]">{m.value}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Recursos */}
        <section id="recursos" tabIndex={-1} className="scroll-mt-20 max-w-6xl mx-auto px-4 sm:px-6 py-20 outline-none">
          <div className="max-w-2xl">
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">
              Do registro à folha de pagamento, sem retrabalho
            </h2>
            <p className="mt-2.5 text-sm sm:text-base text-[var(--text-secondary)]">
              Todos os módulos conversam entre si: cada marcação atualiza automaticamente espelho,
              banco de horas, pendências e exportação mensal.
            </p>
          </div>

          <div className="mt-10 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {FEATURES.map(({ icon: Icon, title, description }) => (
              <Card
                key={title}
                className="p-6 hover:border-[var(--border-strong)] transition-all duration-200 hover:-translate-y-0.5"
              >
                <div
                  className="h-10 w-10 rounded-lg flex items-center justify-center mb-4"
                  style={{ backgroundColor: 'var(--brand-50)', color: 'var(--brand-600)' }}
                >
                  <Icon size={20} />
                </div>
                <h3 className="text-base font-semibold text-[var(--text-primary)]">{title}</h3>
                <p className="mt-2 text-sm text-[var(--text-secondary)] leading-relaxed">
                  {description}
                </p>
              </Card>
            ))}
          </div>
        </section>

        {/* Como funciona */}
        <section
          id="como-funciona"
          tabIndex={-1}
          className="scroll-mt-20 border-t border-[var(--border-subtle)] bg-[var(--surface-card)] outline-none"
        >
          <div className="max-w-6xl mx-auto px-4 sm:px-6 py-20">
            <div className="max-w-2xl mb-10">
              <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">
                Fluxo mensal sem gargalos
              </h2>
              <p className="mt-2 text-sm sm:text-base text-[var(--text-secondary)]">
                Três etapas integradas para eliminar conferências manuais no fechamento do ponto.
              </p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {WORKFLOW_STEPS.map((s) => (
                <div
                  key={s.step}
                  className="rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-page)] p-6 space-y-3 transition-all duration-200 hover:border-[var(--border-strong)] hover:-translate-y-0.5"
                >
                  <span
                    className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-xs font-bold font-mono-tabular"
                    style={{ backgroundColor: 'var(--brand-600)', color: 'var(--brand-fg)' }}
                  >
                    {s.step}
                  </span>
                  <h3 className="text-base font-semibold">{s.title}</h3>
                  <p className="text-xs text-[var(--text-secondary)] leading-relaxed">{s.text}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* White-Label */}
        <section
          id="whitelabel"
          tabIndex={-1}
          className="scroll-mt-20 border-t border-[var(--border-subtle)] bg-[var(--surface-page)] outline-none"
        >
          <div className="max-w-6xl mx-auto px-4 sm:px-6 py-20 grid grid-cols-1 lg:grid-cols-12 gap-10 items-center">
            <div className="lg:col-span-6 space-y-4">
              <div>
                <Badge tone="neutral">
                  <Building2 size={12} />
                  Arquitetura SaaS White-Label
                </Badge>
              </div>
              <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">
                Sua marca, seu subdomínio, seus dados isolados.
              </h2>
              <p className="text-sm sm:text-base text-[var(--text-secondary)] leading-relaxed">
                Cada cliente recebe um endereço exclusivo (ex.:{' '}
                <span className="font-mono-tabular text-[var(--text-primary)]">
                  suaempresa.pontocerto.com.br
                </span>
                ) personalizado com logotipo, paleta institucional e estação de ponto dedicada —
                mantendo segregação completa no banco de dados.
              </p>
              <div className="pt-2">
                <Link to="/login">
                  <Button variant="primary">
                    Acessar portal da minha empresa
                    <ArrowRight size={15} />
                  </Button>
                </Link>
              </div>
            </div>

            <div className="lg:col-span-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
              {[
                { title: 'Endereço Próprio', desc: 'Subdomínio exclusivo pronto para relógio de recepção e portal do RH.' },
                { title: 'Cores e Logotipo', desc: 'Identidade visual configurada por empresa direto no painel administrativo.' },
                { title: 'Calendário Próprio', desc: 'Feriados nacionais automáticos + feriados estaduais, municipais e facultativos internos.' },
                { title: 'Tema Claro e Escuro', desc: 'Conforto visual adaptado à preferência do sistema operacional de cada usuário.' },
              ].map((card) => (
                <Card key={card.title} className="p-5">
                  <div className="text-sm font-semibold">{card.title}</div>
                  <p className="mt-1.5 text-xs text-[var(--text-secondary)] leading-relaxed">
                    {card.desc}
                  </p>
                </Card>
              ))}
            </div>
          </div>
        </section>

        {/* Segurança & LGPD */}
        <section
          id="seguranca"
          tabIndex={-1}
          className="scroll-mt-20 border-y border-[var(--border-subtle)] bg-[var(--surface-card)] outline-none"
        >
          <div className="max-w-6xl mx-auto px-4 sm:px-6 py-20 space-y-10">
            <div className="max-w-2xl">
              <Badge tone="brand">
                <ShieldCheck size={12} />
                Segurança &amp; Governança
              </Badge>
              <h2 className="mt-3 text-2xl sm:text-3xl font-bold tracking-tight">
                Proteção jurídica, privacidade LGPD e auditoria ponta a ponta
              </h2>
              <p className="mt-2 text-sm sm:text-base text-[var(--text-secondary)]">
                Projetado desde o banco de dados para evitar vazamentos entre empresas e garantir
                integridade trabalhista em cada fechamento.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {[
                {
                  icon: Lock,
                  title: 'Isolamento Multi-Tenant',
                  text: 'Todas as consultas e operações são validadas pelo identificador da empresa no token JWT.',
                },
                {
                  icon: ShieldCheck,
                  title: 'Privacidade LGPD',
                  text: 'Atestados médicos ficam restritos ao próprio colaborador e ao departamento pessoal.',
                },
                {
                  icon: Scale,
                  title: 'Imutabilidade Trabalhista',
                  text: 'Competências fechadas bloqueiam alterações retroativas e preservam histórico de marcações.',
                },
                {
                  icon: Clock,
                  title: 'Trilha de Auditoria',
                  text: 'Cada vínculo de batida órfã, abono ou reabertura registra autor, horário e justificativa.',
                },
              ].map(({ icon: Icon, title, text }) => (
                <div
                  key={title}
                  className="rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-page)] p-5 transition-all duration-200 hover:border-[var(--border-strong)]"
                >
                  <Icon size={18} style={{ color: 'var(--brand-600)' }} className="mb-3" />
                  <div className="text-sm font-semibold">{title}</div>
                  <p className="mt-1.5 text-xs text-[var(--text-secondary)] leading-relaxed">{text}</p>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="mt-auto bg-[var(--surface-card)]">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-[var(--text-muted)]">
          <span>© {new Date().getFullYear()} Ponto Certo. Controle de Ponto Eletrônico.</span>
          <Link to="/login" className="hover:text-[var(--text-primary)] font-medium">
            Acessar Portal →
          </Link>
        </div>
      </footer>
    </div>
  );
}
