package de.spukhaus.backend.storage;

import de.spukhaus.backend.config.MinioProperties;
import de.spukhaus.backend.web.exception.ApiException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);
    private static final int THUMBNAIL_MAX_DIMENSION = 480;

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public ImageStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
                log.info("MinIO-Bucket '{}' wurde angelegt.", properties.getBucket());
            }
        } catch (Exception e) {
            log.warn("Konnte MinIO-Bucket nicht prüfen/anlegen: {}", e.getMessage());
        }
    }

    /**
     * Lädt ein Bild hoch. Validiert, dass es sich tatsächlich um eine dekodierbare Bilddatei handelt
     * (nicht nur anhand des angegebenen Content-Type, der gefälscht sein könnte).
     */
    public StoredImage upload(MultipartFile file, String keyPrefix) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new StorageException("Datei konnte nicht gelesen werden.", e);
        }
        String detectedFormat = detectImageFormat(bytes);
        if (detectedFormat == null) {
            throw ApiException.badRequest("Nur Bilddateien sind erlaubt.");
        }
        String contentType = "image/" + detectedFormat;
        String key = keyPrefix + "/" + UUID.randomUUID() + "." + detectedFormat;
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .stream(in, bytes.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Bild konnte nicht gespeichert werden.", e);
        }
        return new StoredImage(key, contentType, bytes.length);
    }

    public InputStream download(String storageKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(storageKey)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Bild konnte nicht geladen werden.", e);
        }
    }

    /**
     * Liefert ein komprimiertes Thumbnail für die Übersicht. Wird beim ersten Zugriff erzeugt
     * und im Object-Storage unter einem abgeleiteten Key zwischengespeichert.
     */
    public byte[] getOrCreateThumbnail(String storageKey) {
        String thumbKey = "thumbnails/" + storageKey;
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(thumbKey)
                    .build());
            try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(thumbKey)
                    .build())) {
                return in.readAllBytes();
            }
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return generateAndStoreThumbnail(storageKey, thumbKey);
            }
            throw new StorageException("Thumbnail konnte nicht geladen werden.", e);
        } catch (Exception e) {
            throw new StorageException("Thumbnail konnte nicht geladen werden.", e);
        }
    }

    private byte[] generateAndStoreThumbnail(String storageKey, String thumbKey) {
        try (InputStream original = download(storageKey)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(original)
                    .size(THUMBNAIL_MAX_DIMENSION, THUMBNAIL_MAX_DIMENSION)
                    .outputFormat("jpg")
                    .outputQuality(0.8)
                    .toOutputStream(out);
            byte[] thumbBytes = out.toByteArray();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(thumbKey)
                    .stream(new ByteArrayInputStream(thumbBytes), thumbBytes.length, -1)
                    .contentType("image/jpeg")
                    .build());
            return thumbBytes;
        } catch (Exception e) {
            throw new StorageException("Thumbnail konnte nicht erzeugt werden.", e);
        }
    }

    private String detectImageFormat(byte[] bytes) {
        try (var iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                return readers.next().getFormatName().toLowerCase();
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
