package com.mini.credit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployePhotoStorageServiceTest {

    @TempDir
    Path tempDirectory;

    private EmployePhotoStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new EmployePhotoStorageService();
        ReflectionTestUtils.setField(storage, "uploadDirectory", tempDirectory.toString());
        ReflectionTestUtils.setField(storage, "maxFileSize", 2L * 1024L * 1024L);
        ReflectionTestUtils.setField(storage, "backendPublicUrl", "http://localhost:8080");
    }

    @Test
    void accepteJpegPngEtWebpEtGenereUnCheminPublicSansNomOriginal() throws Exception {
        for (String contentType : new String[]{"image/jpeg", "image/png", "image/webp"}) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "../../secret.exe", contentType, new byte[]{1, 2, 3});

            String publicUrl = storage.store(7L, file);

            assertThat(publicUrl).startsWith("http://localhost:8080/uploads/employes/7-");
            assertThat(publicUrl).doesNotContain("secret.exe");
            assertThat(Files.exists(tempDirectory.resolve(publicUrl.substring("http://localhost:8080/uploads/".length())))).isTrue();
        }
    }

    @Test
    void refuseUnFichierNonImage() {
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> storage.store(7L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPEG");
    }

    @Test
    void refuseUnFichierTropVolumineux() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[(2 * 1024 * 1024) + 1]);

        assertThatThrownBy(() -> storage.store(7L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 Mo");
    }

    @Test
    void supprimeUniquementUnePhotoGereeParLApplication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1});
        String publicUrl = storage.store(7L, file);
        Path stored = tempDirectory.resolve(java.net.URI.create(publicUrl).getPath().substring("/uploads/".length()));

        storage.deleteIfManaged(publicUrl);
        assertThat(Files.exists(stored)).isFalse();

        storage.deleteIfManaged("https://cdn.example/photo.png");
        storage.deleteIfManaged("/uploads/autres/photo.png");
    }
}
