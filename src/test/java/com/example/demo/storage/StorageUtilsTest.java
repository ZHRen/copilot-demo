package com.example.demo.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageUtilsTest {

    @Test
    void sanitizeNormalFilenameUnchanged() {
        assertEquals("test.txt", StorageUtils.sanitizeFilename("test.txt"));
    }

    @Test
    void sanitizeCollapsesConsecutiveSpecialChars() {
        assertEquals("my_file.txt", StorageUtils.sanitizeFilename("my///file.txt"));
    }

    @Test
    void sanitizeReplacesSpecialCharsWithUnderscore() {
        assertEquals("hello_world.txt", StorageUtils.sanitizeFilename("hello world.txt"));
    }

    @Test
    void sanitizeAllSpecialCharBaseNameBecomesUpload() {
        // "!@#$.txt" → base "!@#$" sanitizes to "____", replaced with "upload"
        assertEquals("upload.txt", StorageUtils.sanitizeFilename("!@#$.txt"));
    }

    @Test
    void sanitizeAllSpecialCharsNoExtensionBecomesUpload() {
        assertEquals("upload", StorageUtils.sanitizeFilename("!@#!"));
    }

    @Test
    void sanitizeNullByteThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> StorageUtils.sanitizeFilename("bad\0name.txt"));
    }

    @Test
    void sanitizePathSeparatorsCollapsed() {
        // on Linux, Paths.get already strips directory; here we test raw input
        assertEquals("a_b_c", StorageUtils.sanitizeFilename("a/b/c"));
    }

    @Test
    void sanitizeDotsAndHyphensAllowed() {
        assertEquals("my-file.v1.0.txt", StorageUtils.sanitizeFilename("my-file.v1.0.txt"));
    }
}
