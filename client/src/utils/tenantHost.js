/**
 * Resolução de subdomínio estilo 1Doc / Slack:
 * - Em dev: `vertice.localhost:5173` -> "vertice", `localhost:5173` -> null
 * - Em prod: `vertice.pontocerto.com.br` -> "vertice", `pontocerto.com.br` / `www.pontocerto.com.br` -> null
 */
export function getHostSubdomain() {
  if (typeof window === 'undefined') return null;
  const host = window.location.hostname.toLowerCase();
  if (!host || host === 'localhost' || host === '127.0.0.1' || /^\d+\.\d+\.\d+\.\d+$/.test(host)) {
    return null;
  }

  // Suporte nativo a *.localhost em ambiente de desenvolvimento (RFC 6761)
  if (host.endsWith('.localhost')) {
    const sub = host.slice(0, -'.localhost'.length);
    return sub && sub !== 'www' ? sub : null;
  }

  const parts = host.split('.');
  // ex.: vertice.pontocerto.com.br (>= 4 partes) ou vertice.pontocerto.app (>= 3 partes)
  if (parts.length >= 3 && parts[0] !== 'www' && parts[0] !== 'app') {
    // Evita tratar domínio raiz .com.br (3 partes: pontocerto.com.br) como subdomínio
    if (parts.length === 3 && parts[1] === 'com' && parts[2] === 'br') {
      return null;
    }
    return parts[0];
  }

  return null;
}

export function buildTenantUrl(subdominio, path = '/login') {
  const cleanSub = (subdominio || '').trim().toLowerCase().replace(/[^a-z0-9-]/g, '');
  if (!cleanSub) return path;

  const { protocol, hostname, port } = window.location;
  const portPart = port ? `:${port}` : '';
  const cleanPath = path.startsWith('/') ? path : `/${path}`;

  if (hostname === 'localhost' || hostname === '127.0.0.1' || hostname.endsWith('.localhost')) {
    return `${protocol}//${cleanSub}.localhost${portPart}${cleanPath}`;
  }

  const currentSub = getHostSubdomain();
  const rootHost = currentSub ? hostname.slice(currentSub.length + 1) : hostname.replace(/^www\./, '');
  return `${protocol}//${cleanSub}.${rootHost}${portPart}${cleanPath}`;
}

export function buildRootUrl(path = '/') {
  const { protocol, hostname, port } = window.location;
  const portPart = port ? `:${port}` : '';
  const cleanPath = path.startsWith('/') ? path : `/${path}`;

  if (hostname === 'localhost' || hostname === '127.0.0.1' || hostname.endsWith('.localhost')) {
    return `${protocol}//localhost${portPart}${cleanPath}`;
  }

  const currentSub = getHostSubdomain();
  const rootHost = currentSub ? hostname.slice(currentSub.length + 1) : hostname;
  return `${protocol}//${rootHost}${portPart}${cleanPath}`;
}
