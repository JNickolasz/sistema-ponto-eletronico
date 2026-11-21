package com.example.projetoPBD.pontoCerto.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class AnexoStorageService {

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of(".pdf", ".png", ".jpg", ".jpeg", ".webp");

    private final Path diretorioUploads = Paths.get("uploads/ocorrencias").toAbsolutePath().normalize();
    private final Path diretorioFotosBatidas = Paths.get("uploads/batidas").toAbsolutePath().normalize();

    public AnexoStorageService() {
        try {
            Files.createDirectories(diretorioUploads);
            Files.createDirectories(diretorioFotosBatidas);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório para armazenamento de anexos.", e);
        }
    }

    public String salvarAnexo(MultipartFile arquivo) {
        return salvarEmDiretorio(arquivo, diretorioUploads, "anexo");
    }

    public String salvarFotoBatida(MultipartFile foto) {
        return salvarEmDiretorio(foto, diretorioFotosBatidas, "foto_batida.jpg");
    }

    private String salvarEmDiretorio(MultipartFile arquivo, Path diretorio, String nomePadrao) {
        if (arquivo == null || arquivo.isEmpty()) {
            return null;
        }

        String nomeOriginal = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : nomePadrao;
        String extensao = "";
        int pontoIndex = nomeOriginal.lastIndexOf('.');
        if (pontoIndex > 0 && pontoIndex < nomeOriginal.length() - 1) {
            String extBruta = nomeOriginal.substring(pontoIndex).toLowerCase();
            String extLimpa = extBruta.replaceAll("[^a-z0-9.]", "");
            if (!EXTENSOES_PERMITIDAS.contains(extLimpa)) {
                throw new IllegalArgumentException("Tipo de arquivo não permitido. Envie apenas PDF ou imagem (PNG, JPG, JPEG, WEBP).");
            }
            extensao = extLimpa;
        } else {
            throw new IllegalArgumentException("Arquivo sem extensão válida. Envie apenas PDF ou imagem (PNG, JPG, JPEG, WEBP).");
        }

        String nomeArquivoArmazenado = UUID.randomUUID() + extensao;
        Path caminhoAlvo = diretorio.resolve(nomeArquivoArmazenado);

        try {
            Files.copy(arquivo.getInputStream(), caminhoAlvo, StandardCopyOption.REPLACE_EXISTING);
            return caminhoAlvo.toString();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar arquivo no servidor.", e);
        }
    }

    public Resource carregarComoRecurso(String caminhoArquivo) {
        if (caminhoArquivo == null || caminhoArquivo.isBlank()) {
            return null;
        }
        try {
            Path caminho = Paths.get(caminhoArquivo);
            Resource recurso = new UrlResource(caminho.toUri());
            if (recurso.exists() && recurso.isReadable()) {
                return recurso;
            }
            return null;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public void excluirAnexo(String caminhoArquivo) {
        if (caminhoArquivo == null || caminhoArquivo.isBlank()) return;
        try {
            Path caminho = Paths.get(caminhoArquivo);
            Files.deleteIfExists(caminho);
        } catch (IOException ignored) {}
    }
}