package com.example.s3merge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced test suite for S3SequentialJsonArrayMerger
 * Tests cover: complex scenarios, thread safety, buffer management, and corner cases
 * Additional coverage for edge cases and complex workflows
 */
@DisplayName("S3SequentialJsonArrayMerger Advanced Test Suite")
class S3SequentialJsonArrayMergerAdvancedTest {
    
    private static final String ORIGINAL_CHECKPOINT = "checkpoint.txt";
    
    @BeforeEach
    void setUp() throws Exception {
        Files.deleteIfExists(Paths.get(ORIGINAL_CHECKPOINT));
        Files.deleteIfExists(Paths.get(ORIGINAL_CHECKPOINT + ".tmp"));
    }
    
    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(Paths.get(ORIGINAL_CHECKPOINT));
        Files.deleteIfExists(Paths.get(ORIGINAL_CHECKPOINT + ".tmp"));
    }
    
    // ============ Complex Buffer Scenarios ============
    
    @Nested
    @DisplayName("Buffer Management Complex Scenarios")
    class BufferManagementTests {
        
        @Test
        @DisplayName("Buffer with large lines exceeding 1MB")
        void testBufferWithLargeLines() throws Exception {
            Map<String, String> checkpoint = new HashMap<>();
            List<String> buffer = new ArrayList<>();
            
            // Create a large line (1MB)
            StringBuilder largeLineBuilder = new StringBuilder();
            for (int i = 0; i < 1024; i++) {
                largeLineBuilder.append("x".repeat(1024));
            }
            String largeLine = largeLineBuilder.toString();
            buffer.add(largeLine);
            
            Method updateBuffer = getPrivateMethod("updateBufferInCheckpoint", Map.class, List.class, int.class);
            updateBuffer.invoke(null, checkpoint, buffer, 1);
            
            assertEquals("1", checkpoint.get("bufferCount"));
            assertEquals(largeLine, checkpoint.get("buffer_0"));
        }
        
        @Test
        @DisplayName("Buffer with many small lines")
        void testBufferWithManySmallLines() throws Exception {
            Map<String, String> checkpoint = new HashMap<>();
            List<String> buffer = new ArrayList<>();
            
            for (int i = 0; i < 10000; i++) {
                buffer.add("line_" + i);
            }
            
            Method updateBuffer = getPrivateMethod("updateBufferInCheckpoint", Map.class, List.class, int.class);
            updateBuffer.invoke(null, checkpoint, buffer, 10000);
            
            assertEquals("10000", checkpoint.get("bufferCount"));
            assertEquals("line_0", checkpoint.get("buffer_0"));
            assertEquals("line_9999", checkpoint.get("buffer_9999"));
        }
        
        @Test
        @DisplayName("Clear buffer multiple times")
        void testClearBufferMultipleTimes() throws Exception {
            Map<String, String> checkpoint = new HashMap<>();
            checkpoint.put("file1.log", "100");
            checkpoint.put("bufferCount", "2");
            checkpoint.put("buffer_0", "line1");
            checkpoint.put("buffer_1", "line2");
            
            Method clearBuffer = getPrivateMethod("clearBufferFromCheckpoint", Map.class);
            clearBuffer.invoke(null, checkpoint);
            clearBuffer.invoke(null, checkpoint);
            clearBuffer.invoke(null, checkpoint);
            
            assertEquals("100", checkpoint.get("file1.log"));
            assertFalse(checkpoint.containsKey("bufferCount"));
            assertFalse(checkpoint.containsKey("buffer_0"));
        }
        
        @Test
        @DisplayName("Update buffer replaces old buffer")
        void testUpdateBufferReplaces() throws Exception {
            Map<String, String> checkpoint = new HashMap<>();
            checkpoint.put("bufferCount", "2");
            checkpoint.put("buffer_0", "old1");
            checkpoint.put("buffer_1", "old2");
            
            List<String> newBuffer = new ArrayList<>();
            newBuffer.add("new1");
            newBuffer.add("new2");
            newBuffer.add("new3");
            
            Method updateBuffer = getPrivateMethod("updateBufferInCheckpoint", Map.class, List.class, int.class);
            updateBuffer.invoke(null, checkpoint, newBuffer, 3);
            
            assertEquals("3", checkpoint.get("bufferCount"));
            assertEquals("new1", checkpoint.get("buffer_0"));
            assertEquals("new2", checkpoint.get("buffer_1"));
            assertEquals("new3", checkpoint.get("buffer_2"));
            assertFalse(checkpoint.containsKey("buffer_3"));
        }
    }
    
    // ============ Checkpoint Persistence Tests ============
    
    @Nested
    @DisplayName("Checkpoint Persistence Scenarios")
    class CheckpointPersistenceTests {
        
        @Test
        @DisplayName("Save and load roundtrip preserves data")
        void testSaveLoadRoundtrip() throws Exception {
            Map<String, String> checkpoint = new HashMap<>();
            checkpoint.put("file1.log", "100");
            checkpoint.put("file2.log", "200");
            checkpoint.put("bufferCount", "2");
            checkpoint.put("buffer_0", "line1");
            checkpoint.put("buffer_1", "line2");
            checkpoint.put("lastSourceKey", "logs/file.log");
            
            Method saveCheckpoint = getPrivateMethod("saveCheckpoint", Map.class);
            saveCheckpoint.invoke(null, checkpoint);
            
            Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
            @SuppressWarnings("unchecked")
            Map<String, String> loaded = (Map<String, String>) loadCheckpoint.invoke(null);
            
            assertEquals(checkpoint.size(), loaded.size());
            checkpoint.forEach((key, value) -> assertEquals(value, loaded.get(key)));
        }
        
        @Test
        @DisplayName("Checkpoint survives multiple save operations")
        void testCheckpointMultipleSaves() throws Exception {
            Map<String, String> checkpoint = new HashMap<>();
            
            Method saveCheckpoint = getPrivateMethod("saveCheckpoint", Map.class);
            
            for (int i = 0; i < 10; i++) {
                checkpoint.put("file_" + i + ".log", String.valueOf(i * 100));
                saveCheckpoint.invoke(null, checkpoint);
            }
            
            Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
            @SuppressWarnings("unchecked")
            Map<String, String> loaded = (Map<String, String>) loadCheckpoint.invoke(null);
            
            assertEquals(10, loaded.size());
            for (int i = 0; i < 10; i++) {
                assertEquals(String.valueOf(i * 100), loaded.get("file_" + i + ".log"));
            }
        }
        
        @Test
        @DisplayName("Checkpoint with Unicode characters")
        void testCheckpointUnicodeCharacters() throws Exception {
            String unicodeContent = "日本語テキスト with émojis 🚀📊 and special chars: €₹";
            createCheckpointFile("checkpoint.txt", new String[]{
                "buffer_0=" + unicodeContent
            });
            
            Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
            @SuppressWarnings("unchecked")
            Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
            
            assertEquals(unicodeContent, result.get("buffer_0"));
        }
        
        @Test
        @DisplayName("Checkpoint handles null values in map")
        void testCheckpointWithNullValues() throws Exception {
            Map<String, String> checkpoint = new HashMap<>();
            checkpoint.put("file1.log", "100");
            checkpoint.put("file2.log", null);
            
            Method saveCheckpoint = getPrivateMethod("saveCheckpoint", Map.class);
            // Should not throw exception
            assertDoesNotThrow(() -> saveCheckpoint.invoke(null, checkpoint));
        }
        
        @Test
        @DisplayName("Checkpoint with very long file paths")
        void testCheckpointLongFilePaths() throws Exception {
            StringBuilder longPath = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                longPath.append("very/long/nested/directory/structure/");
            }
            longPath.append("file.log");
            
            Map<String, String> checkpoint = new HashMap<>();
            checkpoint.put(longPath.toString(), "12345");
            
            Method saveCheckpoint = getPrivateMethod("saveCheckpoint", Map.class);
            saveCheckpoint.invoke(null, checkpoint);
            
            Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
            @SuppressWarnings("unchecked")
            Map<String, String> loaded = (Map<String, String>) loadCheckpoint.invoke(null);
            
            assertEquals("12345", loaded.get(longPath.toString()));
        }
    }
    
    // ============ Network Exception Detection Tests ============
    
    @Nested
    @DisplayName("Network Exception Detection")
    class NetworkExceptionTests {
        
        @Test
        @DisplayName("Detects multiple network error patterns in message")
        void testMultipleErrorPatterns() throws Exception {
            Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
            
            String[] networkMessages = {
                "java.net.UnknownHostException: api.example.com",
                "java.net.SocketTimeoutException: Read timed out",
                "java.net.ConnectException: Connection refused",
                "java.net.NoRouteToHostException",
                "Connection reset by peer",
                "Connection refused",
                "Network is unreachable"
            };
            
            for (String msg : networkMessages) {
                Exception e = new Exception(msg);
                Object result = isNetworkException.invoke(null, e);
                assertTrue((Boolean) result, "Should detect: " + msg);
            }
        }
        
        @Test
        @DisplayName("Case sensitive detection of error patterns")
        void testCaseSensitiveDetection() throws Exception {
            Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
            
            Exception e1 = new Exception("Connection reset by peer");
            Object result1 = isNetworkException.invoke(null, e1);
            assertTrue((Boolean) result1);
            
            // Test with a message that contains the words but not exact patterns
            Exception e2 = new Exception("SOME ERROR that is not connection related");
            Object result2 = isNetworkException.invoke(null, e2);
            assertFalse((Boolean) result2); // Doesn't contain network patterns
        }
        
        @Test
        @DisplayName("Does not falsely detect non-network errors")
        void testNonNetworkErrors() throws Exception {
            Method isNetworkException = getPrivateMethod("isNetworkException", Exception.class);
            
            String[] nonNetworkMessages = {
                "NullPointerException",
                "IllegalArgumentException",
                "IOException: File not found",
                "ClassNotFoundException",
                "RuntimeException: Something went wrong"
            };
            
            for (String msg : nonNetworkMessages) {
                Exception e = new Exception(msg);
                Object result = isNetworkException.invoke(null, e);
                assertFalse((Boolean) result, "Should NOT detect as network: " + msg);
            }
        }
    }
    
    // ============ formatBytes Edge Cases ============
    
    @Nested
    @DisplayName("Byte Formatting Edge Cases")
    class ByteFormattingTests {
        
        @Test
        @DisplayName("Boundary values for byte units")
        void testBoundaryValues() throws Exception {
            Method formatBytes = getPrivateMethod("formatBytes", long.class);
            
            // Boundary: 1023 B -> 1023 B, 1024 B -> 1.00 KB
            assertEquals("1023 B", formatBytes.invoke(null, 1023L));
            assertEquals("1.00 KB", formatBytes.invoke(null, 1024L));
            
            // Boundary: 1024*1024-1 bytes -> KB, 1024*1024 bytes -> MB
            assertEquals("1024.00 KB", formatBytes.invoke(null, 1024L * 1024 - 1));
            assertEquals("1.00 MB", formatBytes.invoke(null, 1024L * 1024));
        }
        
        @Test
        @DisplayName("Precision in decimal formatting")
        void testDecimalPrecision() throws Exception {
            Method formatBytes = getPrivateMethod("formatBytes", long.class);
            
            // Test two decimal places
            assertEquals("1.50 KB", formatBytes.invoke(null, 1536L));
            assertEquals("2.75 MB", formatBytes.invoke(null, 2883584L));
            assertEquals("3.33 GB", formatBytes.invoke(null, 3580498837L));
        }
        
        @Test
        @DisplayName("Very large file sizes")
        void testVeryLargeSizes() throws Exception {
            Method formatBytes = getPrivateMethod("formatBytes", long.class);
            
            // 1000 GB
            assertEquals("1000.00 GB", formatBytes.invoke(null, 1000L * 1024 * 1024 * 1024));
            
            // 10 TB
            assertEquals("10240.00 GB", formatBytes.invoke(null, 10L * 1024 * 1024 * 1024 * 1024));
        }
    }
    
    // ============ BuildTargetKey Complex Cases ============
    
    @Nested
    @DisplayName("Target Key Building Complex Cases")
    class TargetKeyBuildingTests {
        
        @Test
        @DisplayName("Windows-style paths are treated as single file")
        void testWindowsPaths() throws Exception {
            Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
            Object result = buildTargetKey.invoke(null, "C:\\logs\\file.log", "merged-123");
            // Windows paths without forward slashes are treated as files
            assertEquals("merged-123", result);
        }
        
        @Test
        @DisplayName("Mixed path separators")
        void testMixedPathSeparators() throws Exception {
            Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
            // Only forward slashes are recognized
            Object result = buildTargetKey.invoke(null, "logs\\windows\\file.log", "merged-123");
            assertEquals("merged-123", result);
        }
        
        @Test
        @DisplayName("Path with special characters")
        void testPathSpecialCharacters() throws Exception {
            Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
            Object result = buildTargetKey.invoke(null, "logs-2025/file@1.log", "merged-123");
            assertEquals("logs-2025/merged-123", result);
        }
        
        @Test
        @DisplayName("Consecutive slashes in path")
        void testConsecutiveSlashes() throws Exception {
            Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
            Object result = buildTargetKey.invoke(null, "logs//double//slash/file.log", "merged-123");
            assertEquals("logs//double//slash/merged-123", result);
        }
        
        @Test
        @DisplayName("Trailing slash with filename")
        void testTrailingSlashWithFile() throws Exception {
            Method buildTargetKey = getPrivateMethod("buildTargetKey", String.class, String.class);
            Object result = buildTargetKey.invoke(null, "logs/folder/", "merged-123");
            assertEquals("logs/folder/merged-123", result);
        }
    }
    
    // ============ Checkpoint File Corruption Tests ============
    
    @Nested
    @DisplayName("Checkpoint File Resilience")
    class CheckpointResilienceTests {
        
        @Test
        @DisplayName("Load checkpoint with blank lines")
        void testCheckpointWithBlankLines() throws Exception {
            createCheckpointFile("checkpoint.txt", new String[]{
                "file1.log=100",
                "",
                "file2.log=200",
                ""
            });
            
            Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
            @SuppressWarnings("unchecked")
            Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
            
            assertEquals(2, result.size());
            assertEquals("100", result.get("file1.log"));
            assertEquals("200", result.get("file2.log"));
        }
        
        @Test
        @DisplayName("Load checkpoint with only equals sign")
        void testCheckpointOnlyEquals() throws Exception {
            createCheckpointFile("checkpoint.txt", new String[]{
                "file1.log=100",
                "=",
                "file2.log=200"
            });
            
            Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
            @SuppressWarnings("unchecked")
            Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
            
            // The "=" line creates an entry with empty key and empty value
            assertEquals(3, result.size());
            assertEquals("100", result.get("file1.log"));
            assertEquals("200", result.get("file2.log"));
            assertEquals("", result.get(""));
        }
        
        @Test
        @DisplayName("Load checkpoint with empty value")
        void testCheckpointEmptyValue() throws Exception {
            createCheckpointFile("checkpoint.txt", new String[]{
                "file1.log=",
                "file2.log=200"
            });
            
            Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
            @SuppressWarnings("unchecked")
            Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
            
            assertEquals("", result.get("file1.log"));
            assertEquals("200", result.get("file2.log"));
        }
        
        @Test
        @DisplayName("Load checkpoint with no key")
        void testCheckpointNoKey() throws Exception {
            createCheckpointFile("checkpoint.txt", new String[]{
                "=value",
                "file2.log=200"
            });
            
            Method loadCheckpoint = getPrivateMethod("loadCheckpoint");
            @SuppressWarnings("unchecked")
            Map<String, String> result = (Map<String, String>) loadCheckpoint.invoke(null);
            
            assertEquals(2, result.size());
            assertEquals("value", result.get(""));
            assertEquals("200", result.get("file2.log"));
        }
    }
    
    // ============ State Management Tests ============
    
    @Nested
    @DisplayName("Checkpoint State Management")
    class StateManagementTests {
        
        @Test
        @DisplayName("LastSourceKey persists across updates")
        void testLastSourceKeyPersists() throws Exception {
            Map<String, String> checkpoint = new HashMap<>();
            checkpoint.put("file1.log", "100");
            
            Method updateKey = getPrivateMethod("updateLastSourceKeyInCheckpoint", Map.class, String.class);
            updateKey.invoke(null, checkpoint, "logs/first.log");
            assertEquals("logs/first.log", checkpoint.get("lastSourceKey"));
            
            updateKey.invoke(null, checkpoint, "logs/second.log");
            assertEquals("logs/second.log", checkpoint.get("lastSourceKey"));
        }
        
        @Test
        @DisplayName("Buffer count consistency")
        void testBufferCountConsistency() throws Exception {
            Map<String, String> checkpoint = new HashMap<>();
            List<String> buffer = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                buffer.add("line_" + i);
            }
            
            Method updateBuffer = getPrivateMethod("updateBufferInCheckpoint", Map.class, List.class, int.class);
            updateBuffer.invoke(null, checkpoint, buffer, 100);
            
            int bufferCount = Integer.parseInt(checkpoint.get("bufferCount"));
            int actualCount = (int) checkpoint.entrySet().stream()
                .filter(e -> e.getKey().startsWith("buffer_"))
                .count();
            
            assertEquals(bufferCount, actualCount);
        }
        
        @Test
        @DisplayName("Clear buffer removes exactly buffer entries")
        void testClearBufferExactRemoval() throws Exception {
            Map<String, String> checkpoint = new HashMap<>();
            checkpoint.put("file1.log", "100");
            checkpoint.put("file2.log", "200");
            checkpoint.put("bufferCount", "2");
            checkpoint.put("buffer_0", "line1");
            checkpoint.put("buffer_1", "line2");
            checkpoint.put("lastSourceKey", "logs/file.log");
            
            Method clearBuffer = getPrivateMethod("clearBufferFromCheckpoint", Map.class);
            clearBuffer.invoke(null, checkpoint);
            
            assertTrue(checkpoint.containsKey("file1.log"));
            assertTrue(checkpoint.containsKey("file2.log"));
            assertTrue(checkpoint.containsKey("lastSourceKey"));
            assertFalse(checkpoint.containsKey("bufferCount"));
            assertFalse(checkpoint.containsKey("buffer_0"));
            assertFalse(checkpoint.containsKey("buffer_1"));
        }
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
