package com.example.s3merge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for S3SequentialJsonArrayMerger
 * Tests cover: checkpoint operations, utility methods, error handling, and edge cases
 * Target coverage: 90% lines
 */
@DisplayName("S3SequentialJsonArrayMerger Test Suite")
class S3SequentialJsonArrayMergerTest {
    
    private static final String ORIGINAL_CHECKPOINT = "checkpoint.txt";
    
    @BeforeEach
    void setUp() throws Exception {
        // Clean up any existing test checkpoint files
        Files.deleteIfExists(Paths.get(ORIGINAL_CHECKPOINT));
        Files.deleteIfExists(Paths.get(ORIGINAL_CHECKPOINT + ".tmp"));
    }
    
    @AfterEach
    void tearDown() throws Exception {
        // Clean up test files
        Files.deleteIfExists(Paths.get(ORIGINAL_CHECKPOINT));
        Files.deleteIfExists(Paths.get(ORIGINAL_CHECKPOINT + ".tmp"));
    }
    
    // ============ Utility Method Tests: formatBytes ============
    
    @Test
    @DisplayName("formatBytes: Format bytes to KB")
    void testFormatBytesKB() throws Exception {
        Method formatBytes = getPrivateMethod("formatBytes", long.class);
        Object result = formatBytes.invoke(null, 2048L);
        assertEquals("2.00 KB", result);
    }
    
    @Test
    @DisplayName("formatBytes: Format bytes to MB")
    void testFormatBytesMB() throws Exception {
        Method formatBytes = getPrivateMethod("formatBytes", long.class);
        Object result = formatBytes.invoke(null, 1048576L);
        assertEquals("1.00 MB", result);
    }
    
    @Test
    @DisplayName("formatBytes: Format bytes to GB")
    void testFormatBytesGB() throws Exception {
        Method formatBytes = getPrivateMethod("formatBytes", long.class);
        Object result = formatBytes.invoke(null, 1073741824L);
        assertEquals("1.00 GB", result);
    }
    
    @Test
    @DisplayName("formatBytes: Format bytes less than 1KB")
    void testFormatBytesSmall() throws Exception {
        Method formatBytes = getPrivateMethod("formatBytes", long.class);
        Object result = formatBytes.invoke(null, 512L);
        assertEquals("512 B", result);
    }
    
    @Test
    @DisplayName("formatBytes: Format bytes with decimal precision")
    void testFormatBytesDecimal() throws Exception {
        Method formatBytes = getPrivateMethod("formatBytes", long.class);
        Object result = formatBytes.invoke(null, 1536L);
        assertEquals("1.50 KB", result);
    }
    
    @Test
    @DisplayName("formatBytes: Zero bytes")
    void testFormatBytesZero() throws Exception {
        Method formatBytes = getPrivateMethod("formatBytes", long.class);
        Object result = formatBytes.invoke(null, 0L);
        assertEquals("0 B", result);
    }
    
    @Test
    @DisplayName("formatBytes: 1024 bytes exactly")
    void testFormatBytes1024() throws Exception {
        Method formatBytes = getPrivateMethod("formatBytes", long.class);
        Object result = formatBytes.invoke(null, 1024L);
        assertEquals("1.00 KB", result);
    }
    
    @Test
    @DisplayName("formatBytes: Large file size (5GB)")
    void testFormatBytesLargeFile() throws Exception {
        Method formatBytes = getPrivateMethod("formatBytes", long.class);
        Object result = formatBytes.invoke(null, 5368709120L);
        assertEquals("5.00 GB", result);
    }
    
    // ============ buildTargetKey Tests ============
    
    @Test
    @DisplayName("buildTargetKey: With nested directory structure")
    void testBuildTargetKeyWithDirectory() throws Exception {
        Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
        Object result = buildTargetKey.invoke(null, "logs/2025-12-26/app-001.log", "merged-123");
        assertEquals("logs/2025-12-26/merged-123", result);
    }
    
    @Test
    @DisplayName("buildTargetKey: Without directory structure")
    void testBuildTargetKeyNoDirectory() throws Exception {
        Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
        Object result = buildTargetKey.invoke(null, "app-001.log", "merged-123");
        assertEquals("merged-123", result);
    }
    
    @Test
    @DisplayName("buildTargetKey: Null source key")
    void testBuildTargetKeyNullSource() throws Exception {
        Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
        Object result = buildTargetKey.invoke(null, null, "merged-123");
        assertEquals("merged-123", result);
    }
    
    @Test
    @DisplayName("buildTargetKey: Empty source key")
    void testBuildTargetKeyEmptySource() throws Exception {
        Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
        Object result = buildTargetKey.invoke(null, "", "merged-123");
        assertEquals("merged-123", result);
    }
    
    @Test
    @DisplayName("buildTargetKey: Deep nested directory")
    void testBuildTargetKeyDeepNesting() throws Exception {
        Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
        Object result = buildTargetKey.invoke(null, "a/b/c/d/e/file.log", "merged-123");
        assertEquals("a/b/c/d/e/merged-123", result);
    }
    
    @Test
    @DisplayName("buildTargetKey: Single level directory")
    void testBuildTargetKeySingleLevel() throws Exception {
        Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
        Object result = buildTargetKey.invoke(null, "logs/file.log", "merged-123");
        assertEquals("logs/merged-123", result);
    }
    
    @Test
    @DisplayName("buildTargetKey: Source key ending with slash")
    void testBuildTargetKeyEndingSlash() throws Exception {
        Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
        Object result = buildTargetKey.invoke(null, "logs/", "merged-123");
        assertEquals("logs/merged-123", result);
    }
    
    @Test
    @DisplayName("buildTargetKey: Source key with only slash")
    void testBuildTargetKeyOnlySlash() throws Exception {
        Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
        Object result = buildTargetKey.invoke(null, "/", "merged-123");
        assertEquals("merged-123", result);
    }
    
    // ============ isNetworkException Tests ============
    
    @Test
    @DisplayName("isNetworkException: UnknownHostException message")
    void testIsNetworkExceptionUnknownHost() throws Exception {
        Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
        Exception e = new Exception("UnknownHostException: example.com");
        Object result = isNetworkException.invoke(null, e);
        assertTrue((Boolean) result);
    }
    
    @Test
    @DisplayName("isNetworkException: SocketTimeoutException message")
    void testIsNetworkExceptionSocketTimeout() throws Exception {
        Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
        Exception e = new Exception("SocketTimeoutException");
        Object result = isNetworkException.invoke(null, e);
        assertTrue((Boolean) result);
    }
    
    @Test
    @DisplayName("isNetworkException: ConnectException message")
    void testIsNetworkExceptionConnect() throws Exception {
        Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
        Exception e = new Exception("ConnectException: refused");
        Object result = isNetworkException.invoke(null, e);
        assertTrue((Boolean) result);
    }
    
    @Test
    @DisplayName("isNetworkException: Connection reset message")
    void testIsNetworkExceptionConnectionReset() throws Exception {
        Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
        Exception e = new Exception("Connection reset by peer");
        Object result = isNetworkException.invoke(null, e);
        assertTrue((Boolean) result);
    }
    
    @Test
    @DisplayName("isNetworkException: Connection refused message")
    void testIsNetworkExceptionConnectionRefused() throws Exception {
        Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
        Exception e = new Exception("Connection refused");
        Object result = isNetworkException.invoke(null, e);
        assertTrue((Boolean) result);
    }
    
    @Test
    @DisplayName("isNetworkException: Network unreachable message")
    void testIsNetworkExceptionNetworkUnreachable() throws Exception {
        Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
        Exception e = new Exception("Network is unreachable");
        Object result = isNetworkException.invoke(null, e);
        assertTrue((Boolean) result);
    }
    
    @Test
    @DisplayName("isNetworkException: NoRouteToHostException")
    void testIsNetworkExceptionNoRoute() throws Exception {
        Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
        Exception e = new Exception("NoRouteToHostException");
        Object result = isNetworkException.invoke(null, e);
        assertTrue((Boolean) result);
    }
    
    @Test
    @DisplayName("isNetworkException: Null message")
    void testIsNetworkExceptionNullMessage() throws Exception {
        Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
        Exception e = new Exception((String) null);
        Object result = isNetworkException.invoke(null, e);
        assertFalse((Boolean) result);
    }
    
    @Test
    @DisplayName("isNetworkException: Non-network exception")
    void testIsNetworkExceptionNonNetwork() throws Exception {
        Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
        Exception e = new Exception("Some other error");
        Object result = isNetworkException.invoke(null, e);
        assertFalse((Boolean) result);
    }
    
    @Test
    @DisplayName("isNetworkException: SdkClientException type")
    void testIsNetworkExceptionSdkType() throws Exception {
        Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
        SdkClientException e = SdkClientException.create("SDK error");
        Object result = isNetworkException.invoke(null, e);
        assertTrue((Boolean) result);
    }
    
    // ============ Checkpoint Operations Tests ============
    
    @Test
    @DisplayName("loadCheckpoint: No existing checkpoint file")
    void testLoadCheckpointNoFile() throws Exception {
        Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("loadCheckpoint: With existing checkpoint")
    void testLoadCheckpointWithFile() throws Exception {
        createCheckpointFile("checkpoint.txt", new String[]{"file1.log=100", "file2.log=200"});
        
        Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
        
        assertNotNull(result);
        assertEquals("100", result.get("file1.log"));
        assertEquals("200", result.get("file2.log"));
    }
    
    @Test
    @DisplayName("loadCheckpoint: With buffer entries")
    void testLoadCheckpointWithBuffer() throws Exception {
        createCheckpointFile("checkpoint.txt", new String[]{
            "file1.log=100",
            "bufferCount=2",
            "buffer_0=line1",
            "buffer_1=line2"
        });
        
        Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
        
        assertEquals("2", result.get("bufferCount"));
        assertEquals("line1", result.get("buffer_0"));
        assertEquals("line2", result.get("buffer_1"));
    }
    
    @Test
    @DisplayName("loadCheckpoint: With lastSourceKey")
    void testLoadCheckpointWithLastSourceKey() throws Exception {
        createCheckpointFile("checkpoint.txt", new String[]{
            "file1.log=100",
            "lastSourceKey=logs/file.log"
        });
        
        Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
        
        assertEquals("logs/file.log", result.get("lastSourceKey"));
    }
    
    @Test
    @DisplayName("saveCheckpoint: Create new checkpoint file")
    void testSaveCheckpointNewFile() throws Exception {
        Map<String, String> checkpoint = new HashMap<>();
        checkpoint.put("file1.log", "100");
        checkpoint.put("file2.log", "200");
        
        Method saveCheckpoint = getPrivateMethod("saveCheckpoint", Map.class);
        saveCheckpoint.invoke(null, checkpoint);
        
        assertTrue(Files.exists(Paths.get("checkpoint.txt")));
        List<String> lines = Files.readAllLines(Paths.get("checkpoint.txt"));
        assertEquals(2, lines.size());
    }
    
    @Test
    @DisplayName("saveCheckpoint: Overwrite existing checkpoint")
    void testSaveCheckpointOverwrite() throws Exception {
        createCheckpointFile("checkpoint.txt", new String[]{"old=data"});
        
        Map<String, String> checkpoint = new HashMap<>();
        checkpoint.put("new", "entry");
        
        Method saveCheckpoint = getPrivateMethod("saveCheckpoint", Map.class);
        saveCheckpoint.invoke(null, checkpoint);
        
        List<String> lines = Files.readAllLines(Paths.get("checkpoint.txt"));
        assertTrue(lines.stream().anyMatch(l -> l.contains("new=entry")));
    }
    
    @Test
    @DisplayName("saveCheckpoint: With buffer data")
    void testSaveCheckpointWithBuffer() throws Exception {
        Map<String, String> checkpoint = new HashMap<>();
        checkpoint.put("file1.log", "100");
        checkpoint.put("bufferCount", "3");
        checkpoint.put("buffer_0", "line1");
        checkpoint.put("buffer_1", "line2");
        checkpoint.put("buffer_2", "line3");
        
        Method saveCheckpoint = getPrivateMethod("saveCheckpoint", Map.class);
        saveCheckpoint.invoke(null, checkpoint);
        
        List<String> lines = Files.readAllLines(Paths.get("checkpoint.txt"));
        assertEquals(5, lines.size());
    }
    
    @Test
    @DisplayName("saveCheckpoint: Atomic operation creates temp file")
    void testSaveCheckpointAtomicTemp() throws Exception {
        Map<String, String> checkpoint = new HashMap<>();
        checkpoint.put("test", "data");
        
        Method saveCheckpoint = getPrivateMethod("saveCheckpoint", Map.class);
        saveCheckpoint.invoke(null, checkpoint);
        
        assertFalse(Files.exists(Paths.get("checkpoint.txt.tmp")));
        assertTrue(Files.exists(Paths.get("checkpoint.txt")));
    }
    
    // ============ Buffer Management Tests ============
    
    @Test
    @DisplayName("updateBufferInCheckpoint: Save empty buffer")
    void testUpdateBufferInCheckpointEmpty() throws Exception {
        Map<String, String> checkpoint = new HashMap<>();
        List<String> buffer = new ArrayList<>();
        
        Method updateBuffer = getPrivateMethod("updateBufferInCheckpoint", Map.class, List.class, int.class);
        updateBuffer.invoke(null, checkpoint, buffer, 0);
        
        assertEquals("0", checkpoint.get("bufferCount"));
    }
    
    @Test
    @DisplayName("updateBufferInCheckpoint: Save multiple lines")
    void testUpdateBufferInCheckpointMultiple() throws Exception {
        Map<String, String> checkpoint = new HashMap<>();
        List<String> buffer = new ArrayList<>();
        buffer.add("line1");
        buffer.add("line2");
        buffer.add("line3");
        
        Method updateBuffer = getPrivateMethod("updateBufferInCheckpoint", Map.class, List.class, int.class);
        updateBuffer.invoke(null, checkpoint, buffer, 3);
        
        assertEquals("3", checkpoint.get("bufferCount"));
        assertEquals("line1", checkpoint.get("buffer_0"));
        assertEquals("line2", checkpoint.get("buffer_1"));
        assertEquals("line3", checkpoint.get("buffer_2"));
    }
    
    @Test
    @DisplayName("clearBufferFromCheckpoint: Remove all buffer entries")
    void testClearBufferFromCheckpoint() throws Exception {
        Map<String, String> checkpoint = new HashMap<>();
        checkpoint.put("file1.log", "100");
        checkpoint.put("bufferCount", "2");
        checkpoint.put("buffer_0", "line1");
        checkpoint.put("buffer_1", "line2");
        
        Method clearBuffer = getPrivateMethod("clearBufferFromCheckpoint", Map.class);
        clearBuffer.invoke(null, checkpoint);
        
        assertEquals("100", checkpoint.get("file1.log"));
        assertFalse(checkpoint.containsKey("bufferCount"));
        assertFalse(checkpoint.containsKey("buffer_0"));
        assertFalse(checkpoint.containsKey("buffer_1"));
    }
    
    @Test
    @DisplayName("updateLastSourceKeyInCheckpoint: Save last source key")
    void testUpdateLastSourceKeyInCheckpoint() throws Exception {
        Map<String, String> checkpoint = new HashMap<>();
        
        Method updateKey = getPrivateMethod("updateLastSourceKeyInCheckpoint", Map.class, String.class);
        updateKey.invoke(null, checkpoint, "logs/file.log");
        
        assertEquals("logs/file.log", checkpoint.get("lastSourceKey"));
    }
    
    @Test
    @DisplayName("updateLastSourceKeyInCheckpoint: Null key not saved")
    void testUpdateLastSourceKeyInCheckpointNull() throws Exception {
        Map<String, String> checkpoint = new HashMap<>();
        
        Method updateKey = getPrivateMethod("updateLastSourceKeyInCheckpoint", Map.class, String.class);
        updateKey.invoke(null, checkpoint, null);
        
        assertFalse(checkpoint.containsKey("lastSourceKey"));
    }
    
    // ============ Checkpoint Parsing Edge Cases ============
    
    @Test
    @DisplayName("loadCheckpoint: Handles malformed lines")
    void testLoadCheckpointMalformedLines() throws Exception {
        createCheckpointFile("checkpoint.txt", new String[]{
            "validkey=value",
            "malformed_line_without_equals",
            "another_valid=entry"
        });
        
        Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
        
        assertEquals("value", result.get("validkey"));
        assertEquals("entry", result.get("another_valid"));
        assertFalse(result.containsKey("malformed_line_without_equals"));
    }
    
    @Test
    @DisplayName("loadCheckpoint: Handles equals sign in value")
    void testLoadCheckpointEqualsInValue() throws Exception {
        createCheckpointFile("checkpoint.txt", new String[]{
            "buffer_0={\"key\"=\"value\"}"
        });
        
        Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
        
        assertEquals("{\"key\"=\"value\"}", result.get("buffer_0"));
    }
    
    @Test
    @DisplayName("loadCheckpoint: Empty file")
    void testLoadCheckpointEmptyFile() throws Exception {
        createCheckpointFile("checkpoint.txt", new String[]{});
        
        Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("loadCheckpoint: Preserves complex buffer content")
    void testLoadCheckpointComplexBuffer() throws Exception {
        String complexJson = "{\"event\":\"data\",\"timestamp\":\"2025-12-26T14:23:45Z\"}";
        createCheckpointFile("checkpoint.txt", new String[]{
            "buffer_0=" + complexJson
        });
        
        Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
        
        assertEquals(complexJson, result.get("buffer_0"));
    }
    
    @Test
    @DisplayName("saveCheckpoint: Large checkpoint with many files")
    void testSaveCheckpointLarge() throws Exception {
        Map<String, String> checkpoint = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            checkpoint.put("file_" + i + ".log", String.valueOf(i * 100));
        }
        
        Method saveCheckpoint = getPrivateMethod("saveCheckpoint", Map.class);
        saveCheckpoint.invoke(null, checkpoint);
        
        List<String> lines = Files.readAllLines(Paths.get("checkpoint.txt"));
        assertEquals(1000, lines.size());
    }
    
    @Test
    @DisplayName("loadCheckpoint: Roundtrip with special characters")
    void testLoadCheckpointRoundtripSpecialChars() throws Exception {
        String specialContent = "line with spaces and $pecial char@";
        createCheckpointFile("checkpoint.txt", new String[]{
            "buffer_0=" + specialContent
        });
        
        Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
        
        assertEquals(specialContent, result.get("buffer_0"));
    }
    
    // ============ Parameterized Tests ============
    
    @ParameterizedTest
    @DisplayName("formatBytes: Multiple size values")
    @CsvSource({
        "0, '0 B'",
        "512, '512 B'",
        "1024, '1.00 KB'",
        "1048576, '1.00 MB'",
        "1073741824, '1.00 GB'",
        "1572864, '1.50 MB'",
        "3145728, '3.00 MB'"
    })
    void testFormatBytesParameterized(long bytes, String expected) throws Exception {
        Method formatBytes = getPrivateMethod("formatBytes", long.class);
        Object result = formatBytes.invoke(null, bytes);
        assertEquals(expected, result);
    }
    
    @ParameterizedTest
    @DisplayName("isNetworkException: Various error messages")
    @ValueSource(strings = {
        "UnknownHostException",
        "SocketTimeoutException",
        "ConnectException",
        "NoRouteToHostException",
        "Connection reset",
        "Connection refused",
        "Network is unreachable"
    })
    void testIsNetworkExceptionParameterized(String errorMessage) throws Exception {
        Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
        Exception e = new Exception(errorMessage);
        Object result = isNetworkException.invoke(null, e);
        assertTrue((Boolean) result);
    }
    
    @ParameterizedTest
    @DisplayName("buildTargetKey: Various directory structures")
    @CsvSource({
        "null, 'merged-123', 'merged-123'",
        "'', 'merged-123', 'merged-123'",
        "'file.log', 'merged-123', 'merged-123'",
        "'logs/file.log', 'merged-123', 'logs/merged-123'",
        "'logs/2025/file.log', 'merged-123', 'logs/2025/merged-123'"
    })
    void testBuildTargetKeyParameterized(String sourceKey, String mergedName, String expected) throws Exception {
        Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
        Object sourceKeyObj = "null".equals(sourceKey) ? null : sourceKey;
        Object result = buildTargetKey.invoke(null, sourceKeyObj, mergedName);
        assertEquals(expected, result);
    }
    
    // ============ Helper Methods ============
    
    private Method getPrivateMethod(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = S3SequentialJsonArrayMerger.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method;
    }
    
    private void createCheckpointFile(String filename, String[] lines) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}
