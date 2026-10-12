/**
 * valida PIS/PASEP seguindo a regra do FuncionarioService.java
 * @param {string} pis
 * @returns {boolean}
**/
export function validarPis(pis) {
  if (!pis) return false;
  const limpo = String(pis).replace(/\D/g, '');
  if (limpo.length !== 11) return false;

  if (/^(\d)\1{10}$/.test(limpo)) return false;

  const multiplicadores = [3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  let soma = 0;

  for (let i = 0; i < 10; i++) {
    soma += parseInt(limpo.charAt(i), 10) * multiplicadores[i];
  }

  const resto = soma % 11;
  const digitoEsperado = resto < 2 ? 0 : 11 - resto;

  return digitoEsperado === parseInt(limpo.charAt(10), 10);
}

/**
 * Gera um PIS/PASEP válido com dígito 
 * @returns {string} PIS de 11 dígitos
**/
export function gerarPisValido() {
  const multiplicadores = [3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  let base = '';
  // Gerar os primeiros 10 dígitos (garantindo que não seja tudo igual)
  for (let i = 0; i < 10; i++) {
    base += Math.floor(Math.random() * 10);
  }

  let soma = 0;
  for (let i = 0; i < 10; i++) {
    soma += parseInt(base.charAt(i), 10) * multiplicadores[i];
  }

  const resto = soma % 11;
  const digito = resto < 2 ? 0 : 11 - resto;

  return `${base}${digito}`;
}

/**
 * Valida CPF conforme a regra / Hibernate @CPF
 * @param {string} cpf
 * @returns {boolean}
**/
export function validarCpf(cpf) {
  if (!cpf) return false;
  const limpo = String(cpf).replace(/\D/g, '');
  if (limpo.length !== 11) return false;

  // Rejeita sequências repetidas
  if (/^(\d)\1{10}$/.test(limpo)) return false;

  // Primeiro dígito verificador
  let soma = 0;
  for (let i = 0; i < 9; i++) {
    soma += parseInt(limpo.charAt(i), 10) * (10 - i);
  }
  let resto = (soma * 10) % 11;
  if (resto === 10 || resto === 11) resto = 0;
  if (resto !== parseInt(limpo.charAt(9), 10)) return false;

  // Segundo dígito verificador
  soma = 0;
  for (let i = 0; i < 10; i++) {
    soma += parseInt(limpo.charAt(i), 10) * (11 - i);
  }
  resto = (soma * 10) % 11;
  if (resto === 10 || resto === 11) resto = 0;
  if (resto !== parseInt(limpo.charAt(10), 10)) return false;

  return true;
}

/**
 * Gera um CPF válido
 * @returns {string} CPF de 11 dígitos
**/
export function gerarCpfValido() {
  const digitos = [];
  for (let i = 0; i < 9; i++) {
    digitos.push(Math.floor(Math.random() * 10));
  }

  // Primeiro dígito
  let soma = 0;
  for (let i = 0; i < 9; i++) {
    soma += digitos[i] * (10 - i);
  }
  let resto = (soma * 10) % 11;
  if (resto === 10 || resto === 11) resto = 0;
  digitos.push(resto);

  // Segundo dígito
  soma = 0;
  for (let i = 0; i < 10; i++) {
    soma += digitos[i] * (11 - i);
  }
  resto = (soma * 10) % 11;
  if (resto === 10 || resto === 11) resto = 0;
  digitos.push(resto);

  return digitos.join('');
}

/**
 * Formata CPF: 000.000.000-00
 * @param {string} cpf
**/
export function formatarCpf(cpf) {
  if (!cpf) return '';
  const limpo = String(cpf).replace(/\D/g, '').slice(0, 11);
  return limpo
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
}

/**
 * Formata PIS para o formato correto: 000.00000.00-0
 * @param {string} pis
**/
export function formatarPis(pis) {
  if (!pis) return '';
  const limpo = String(pis).replace(/\D/g, '').slice(0, 11);
  return limpo
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{5})(\d)/, '$1.$2')
    .replace(/(\d{2})(\d{1})$/, '$1-$2');
}
