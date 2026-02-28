package com.example.demo.storage;

import com.obs.services.ObsClient;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.S3Object;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.UncheckedIOException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ObsStorageServiceTest {

    private ObsClient mockObsClient;
    private ObsStorageService service;

    @BeforeEach
    void setUp() {
        mockObsClient = mock(ObsClient.class);
        service = new ObsStorageService(mockObsClient, "test-bucket");
    }

    @Test
    void constructorRejectsBlankEndpoint() {
        assertThrows(IllegalArgumentException.class,
                () -> new ObsStorageService("", "ak", "sk", "bucket"));
    }

    @Test
    void constructorRejectsBlankAccessKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new ObsStorageService("https://obs.example.com", "", "sk", "bucket"));
    }

    @Test
    void constructorRejectsBlankSecretKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new ObsStorageService("https://obs.example.com", "ak", "", "bucket"));
    }

    @Test
    void constructorRejectsBlankBucketName() {
        assertThrows(IllegalArgumentException.class,
                () -> new ObsStorageService("https://obs.example.com", "ak", "sk", ""));
    }

    @Test
    void storeCallsPutObjectAndReturnsKey() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        String result = service.store(file);

        verify(mockObsClient, times(1)).putObject(any(PutObjectRequest.class));
        assertTrue(result.endsWith("_test.txt"), "Returned key should end with _test.txt but was: " + result);
    }

    @Test
    void storeSetsContentLengthAndContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", "a,b,c".getBytes());
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);

        service.store(file);

        verify(mockObsClient).putObject(captor.capture());
        PutObjectRequest req = captor.getValue();
        assertEquals(5L, req.getMetadata().getContentLength());
        assertEquals("text/csv", req.getMetadata().getContentType());
    }

    @Test
    void storeWrapsObsExceptionAsUncheckedIO() {
        doThrow(new RuntimeException("OBS service unavailable"))
                .when(mockObsClient).putObject(any());
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        assertThrows(UncheckedIOException.class, () -> service.store(file));
    }

    @Test
    void storeWithNullByteFilenameThrowsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile("file", "bad\0name.txt", "text/plain", "content".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.store(file));
    }

    @Test
    void storeWithAllSpecialCharsUsesUploadExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "!@#$.txt", "text/plain", "content".getBytes());

        String result = service.store(file);

        assertTrue(result.endsWith("_upload.txt"), "Should replace all-special base with 'upload'");
    }

    @Test
    void destroyClosesObsClient() throws Exception {
        service.destroy();

        verify(mockObsClient, times(1)).close();
    }

    @Test
    void searchReturnsAllObjectsWhenKeywordIsNull() {
        ObjectListing listing = mock(ObjectListing.class);
        when(listing.getObjectSummaries()).thenReturn(List.of(
                buildS3Object("uuid1_alpha.txt", 100L),
                buildS3Object("uuid2_beta.txt", 200L)));
        when(mockObsClient.listObjects(any(com.obs.services.model.ListObjectsRequest.class))).thenReturn(listing);

        List<FileInfo> results = service.search(null);

        assertEquals(2, results.size());
    }

    @Test
    void searchFiltersByKeywordCaseInsensitive() {
        ObjectListing listing = mock(ObjectListing.class);
        when(listing.getObjectSummaries()).thenReturn(List.of(
                buildS3Object("uuid1_Report.csv", 512L),
                buildS3Object("uuid2_image.png", 1024L)));
        when(mockObsClient.listObjects(any(com.obs.services.model.ListObjectsRequest.class))).thenReturn(listing);

        List<FileInfo> results = service.search("report");

        assertEquals(1, results.size());
        assertEquals("uuid1_Report.csv", results.get(0).filename());
        assertEquals(512L, results.get(0).sizeBytes());
    }

    @Test
    void searchReturnsEmptyListWhenNoMatch() {
        ObjectListing listing = mock(ObjectListing.class);
        when(listing.getObjectSummaries()).thenReturn(List.of(
                buildS3Object("uuid1_document.pdf", 300L)));
        when(mockObsClient.listObjects(any(com.obs.services.model.ListObjectsRequest.class))).thenReturn(listing);

        List<FileInfo> results = service.search("nonexistent");

        assertTrue(results.isEmpty());
    }

    @Test
    void searchWrapsObsExceptionAsUncheckedIO() {
        doThrow(new RuntimeException("OBS unavailable"))
                .when(mockObsClient).listObjects(any(com.obs.services.model.ListObjectsRequest.class));

        assertThrows(UncheckedIOException.class, () -> service.search(null));
    }

    private static S3Object buildS3Object(String key, long size) {
        S3Object obj = new S3Object();
        obj.setObjectKey(key);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(size);
        meta.setLastModified(new Date());
        obj.setMetadata(meta);
        return obj;
    }
}
