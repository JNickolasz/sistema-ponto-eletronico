import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Clock, LogOut, Shield, User, Building } from 'lucide-react';
import toast from 'react-hot-toast';



export function Navbar() {
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    toast.success('Sessão encerrada com sucesso!');
    navigate('/login');
  };

  const getRoleBadge = (r) => {
    switch (r) {
      case 'RH':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-purple-500/10 px-3 py-1 text-xs font-semibold text-purple-400 border border-purple-500/30">
            <Shield size={12} /> Recursos Humanos (RH)
          </span>
        );
      case 'GESTOR':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-500/10 px-3 py-1 text-xs font-semibold text-blue-400 border border-blue-500/30">
            <Building size={12} /> Gestor de Equipe
          </span>
        );
      case 'COLABORADOR':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-500/10 px-3 py-1 text-xs font-semibold text-emerald-400 border border-emerald-500/30">
            <User size={12} /> Colaborador
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <header className="sticky top-0 z-40 border-b border-slate-800 bg-slate-900/80 backdrop-blur-xl">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between gap-4">
          {/* Logo & Brand */}
          <div className="flex items-center gap-3">
            <Link to="/" className="flex items-center gap-2.5 group">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/20 group-hover:scale-105 transition-transform">
                <Clock className="h-5 w-5" />
              </div>
              <div className="flex flex-col">
                <span className="text-lg font-bold tracking-tight text-white flex items-center gap-1.5">
                  PontoCerto
                  <span className="rounded bg-indigo-500/20 px-1.5 py-0.5 text-[10px] font-semibold text-indigo-300">
                    v1.0
                  </span>
                </span>
                <span className="text-[11px] text-slate-400 hidden sm:inline">
                  Sistema de Ponto Eletrônico
                </span>
              </div>
            </Link>
          </div>

          {/* Center: Backend connection indicator */}
          <div className="hidden md:flex items-center gap-2 text-xs text-slate-400 bg-slate-950/60 px-3 py-1.5 rounded-full border border-slate-800">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            <span>API Spring Boot: <span className="text-slate-200 font-mono">:8080</span></span>
          </div>

          {/* User Info & Actions */}
          <div className="flex items-center gap-3">
            {getRoleBadge(role)}

            <div className="hidden sm:flex flex-col items-end">
              <span className="text-xs font-medium text-slate-200">{user?.usuario}</span>
              <span className="text-[10px] text-slate-400 font-mono uppercase">{user?.tipo || 'JWT'}</span>
            </div>

            <button
              onClick={handleLogout}
              title="Encerrar Sessão"
              className="flex items-center gap-2 rounded-xl bg-slate-800/80 px-3 py-2 text-xs font-medium text-slate-300 hover:bg-red-500/10 hover:text-red-400 hover:border-red-500/30 border border-slate-700 transition-all cursor-pointer"
            >
              <LogOut size={15} />
              <span className="hidden sm:inline">Sair</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}

export default Navbar;
