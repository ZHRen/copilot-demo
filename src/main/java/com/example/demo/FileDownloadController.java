package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * REST controller that serves files stored in the application's upload directory.
 *
 * <p>Endpoint: {@code GET /api/files/download?filename=<name>}
 *
 * <p>Security: filenames are validated against the pattern {@code [\w.\-]+} to block
 * path-traversal characters such as {@code /}, {@code \}, and {@code ..}. An additional
 * check confirms that the resolved path still begins with the configured upload directory,
 * providing a second layer of defence against traversal attacks.
 *
 * <p>The upload directory is configured via the property {@code app.upload-dir}
 * (defaults to {@code uploads} relative to the working directory).
 */
@RestController
@RequestMapping("/api")
public class FileDownloadController {

    /** Absolute, normalised path to the directory from which files are served. */
    private final Path uploadDir;

    /**
     * Creates the controller and resolves the upload directory to an absolute path.
     *
     * @param uploadDirPath value of {@code app.upload-dir}, defaults to {@code "uploads"}
     */
    public FileDownloadController(@Value("${app.upload-dir:uploads}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
    }

    /**
     * Downloads a file from the upload directory.
     *
     * <p>The method performs the following steps:
     * <ol>
     *   <li>Validate the filename against {@code [\w.\-]+} – reject invalid names with 400.</li>
     *   <li>Resolve the filename against the upload directory and normalise the path.</li>
     *   <li>Confirm the resolved path is still inside the upload directory – reject with 400 if not.</li>
     *   <li>Check that the file exists and is readable – return 404 if not.</li>
     *   <li>Return the file as an {@code application/octet-stream} download with a
     *       {@code Content-Disposition: attachment} header.</li>
     * </ol>
     *
     * @param filename the name of the file to download (must match {@code [\w.\-]+})
     * @return {@code 200 OK} with the file body, {@code 400 Bad Request} for invalid filenames,
     *         or {@code 404 Not Found} if the file does not exist
     */
    @GetMapping("/files/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename) {
        // Reject blank or unsafe filenames before touching the filesystem
        if (filename == null || filename.isBlank() || !filename.matches("[\\w.\\-]+")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path filePath = uploadDir.resolve(filename).normalize();

            // Path-traversal guard: the resolved path must remain inside uploadDir
            if (!filePath.startsWith(uploadDir)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Build a Content-Disposition header that correctly encodes non-ASCII filenames
            ContentDisposition contentDisposition = ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
