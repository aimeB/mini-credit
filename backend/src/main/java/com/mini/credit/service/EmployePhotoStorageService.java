package com.mini.credit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployePhotoStorageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    @Value("${app.upload.dir:uploads}")
    private String uploadDirectory;

    @Value("${app.upload.employe-max-size:2097152}")
    private long maxFileSize;

    @Value("${app.backend-public-url:http://localhost:8080}")
    private String backendPublicUrl;

    public String store(Long employeId, MultipartFile file) {
        validate(file);

        String contentType = file.getContentType().toLowerCase();
        Path employeDirectory = getRoot().resolve("employes").normalize();
        if (!employeDirectory.startsWith(getRoot())) {
            throw new IllegalStateException("Répertoire de stockage invalide");
        }

        try {
            Files.createDirectories(employeDirectory);
            String filename = employeId + "-" + UUID.randomUUID() + EXTENSIONS.get(contentType);
            Path target = employeDirectory.resolve(filename).normalize();
            if (!target.startsWith(employeDirectory)) {
                throw new IllegalStateException("Nom de fichier généré invalide");
            }
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return backendPublicUrl.replaceAll("/$", "") + "/uploads/employes/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible d'enregistrer la photo employé", ex);
        }
    }

    public void deleteIfManaged(String photoUrl) {
        String photoPath = extractPhotoPath(photoUrl);
        if (photoPath == null || !photoPath.startsWith("/uploads/employes/")) {
            return;
        }

        String filename = photoPath.substring("/uploads/employes/".length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return;
        }

        Path root = getRoot();
        Path target = root.resolve("employes").resolve(filename).normalize();
        if (!target.startsWith(root.resolve("employes").normalize())) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("Impossible de supprimer l'ancienne photo employé: {}", photoUrl, ex);
        }
    }

    private String extractPhotoPath(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return null;
        }
        if (photoUrl.startsWith("/")) {
            return photoUrl;
        }
        try {
            URI uri = new URI(photoUrl);
            URI configuredBackend = new URI(backendPublicUrl);
            if (!java.util.Objects.equals(uri.getScheme(), configuredBackend.getScheme())
                    || !java.util.Objects.equals(uri.getAuthority(), configuredBackend.getAuthority())) {
                return null;
            }
            return uri.getPath();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Une photo est obligatoire");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("La photo ne doit pas dépasser 2 Mo");
        }
        if (file.getContentType() == null || !EXTENSIONS.containsKey(file.getContentType().toLowerCase())) {
            throw new IllegalArgumentException("Format accepté : JPEG, PNG ou WebP");
        }
    }

    private Path getRoot() {
        return Path.of(uploadDirectory).toAbsolutePath().normalize();
    }
}
