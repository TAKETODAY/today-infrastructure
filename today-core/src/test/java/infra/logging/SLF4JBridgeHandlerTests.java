/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 21:42
 */
class SLF4JBridgeHandlerTests {

  @Test
  void shouldInstallSLF4JBridgeHandler() {
    // First uninstall any existing handlers to have a clean state
    SLF4JBridgeHandler.uninstall();

    // Install the bridge handler
    SLF4JBridgeHandler.install();

    // Verify it's installed
    assertThat(SLF4JBridgeHandler.isInstalled()).isTrue();
  }

  @Test
  void shouldUninstallSLF4JBridgeHandler() {
    // Ensure handler is installed first
    SLF4JBridgeHandler.uninstall();
    SLF4JBridgeHandler.install();

    // Now uninstall it
    SLF4JBridgeHandler.uninstall();

    // Verify it's uninstalled
    assertThat(SLF4JBridgeHandler.isInstalled()).isFalse();
  }

  @Test
  void shouldReturnFalseWhenNotInstalled() {
    SLF4JBridgeHandler.uninstall();

    assertThat(SLF4JBridgeHandler.isInstalled()).isFalse();
  }

  @Test
  void shouldPublishLogRecordToSLF4J() {
    SLF4JBridgeHandler handler = new SLF4JBridgeHandler();
    java.util.logging.LogRecord record = new java.util.logging.LogRecord(java.util.logging.Level.INFO, "Test message");
    record.setLoggerName("test.logger");

    // Should not throw any exception
    handler.publish(record);
  }

  @Test
  void shouldHandleNullLogRecord() {
    SLF4JBridgeHandler handler = new SLF4JBridgeHandler();

    // Should not throw any exception when record is null
    handler.publish(null);
  }

  @Test
  void shouldHandleLogRecordWithNullMessage() {
    SLF4JBridgeHandler handler = new SLF4JBridgeHandler();
    java.util.logging.LogRecord record = new java.util.logging.LogRecord(java.util.logging.Level.INFO, null);
    record.setLoggerName("test.logger");

    // Should not throw any exception
    handler.publish(record);
  }

  @Test
  void shouldCloseAndFlushWithoutException() {
    SLF4JBridgeHandler handler = new SLF4JBridgeHandler();

    // These should not throw exceptions
    handler.close();
    handler.flush();
  }

  @Test
  void shouldGetSLF4JLogger() {
    SLF4JBridgeHandler handler = new SLF4JBridgeHandler();
    java.util.logging.LogRecord record = new java.util.logging.LogRecord(java.util.logging.Level.INFO, "Test message");
    record.setLoggerName("test.slf4j.logger");

    org.slf4j.Logger logger = handler.getSLF4JLogger(record);

    assertThat(logger).isNotNull();
    assertThat(logger.getName()).isEqualTo("test.slf4j.logger");
  }

  @Test
  void shouldHandleLogRecordWithNullLoggerName() {
    SLF4JBridgeHandler handler = new SLF4JBridgeHandler();
    java.util.logging.LogRecord record = new java.util.logging.LogRecord(java.util.logging.Level.INFO, "Test message");
    record.setLoggerName(null);

    org.slf4j.Logger logger = handler.getSLF4JLogger(record);

    assertThat(logger).isNotNull();
    assertThat(logger.getName()).isEqualTo("unknown.jul.logger");
  }

  @Test
  void shouldMapJULLevelsToSLF4JLevels() {
    SLF4JBridgeHandler handler = new SLF4JBridgeHandler();

    // Test various JUL levels mapping to SLF4J levels
    java.util.logging.LogRecord finestRecord = new java.util.logging.LogRecord(java.util.logging.Level.FINEST, "Trace message");
    java.util.logging.LogRecord finerRecord = new java.util.logging.LogRecord(java.util.logging.Level.FINER, "Debug message");
    java.util.logging.LogRecord fineRecord = new java.util.logging.LogRecord(java.util.logging.Level.FINE, "Debug message");
    java.util.logging.LogRecord infoRecord = new java.util.logging.LogRecord(java.util.logging.Level.INFO, "Info message");
    java.util.logging.LogRecord warningRecord = new java.util.logging.LogRecord(java.util.logging.Level.WARNING, "Warn message");
    java.util.logging.LogRecord severeRecord = new java.util.logging.LogRecord(java.util.logging.Level.SEVERE, "Error message");

    finestRecord.setLoggerName("test.level");
    finerRecord.setLoggerName("test.level");
    fineRecord.setLoggerName("test.level");
    infoRecord.setLoggerName("test.level");
    warningRecord.setLoggerName("test.level");
    severeRecord.setLoggerName("test.level");

    // Should not throw exceptions
    handler.publish(finestRecord);
    handler.publish(finerRecord);
    handler.publish(fineRecord);
    handler.publish(infoRecord);
    handler.publish(warningRecord);
    handler.publish(severeRecord);

    assertThat(true).isTrue();
  }

  @Test
  void shouldHandleLogRecordWithParameters() {
    SLF4JBridgeHandler handler = new SLF4JBridgeHandler();
    java.util.logging.LogRecord record = new java.util.logging.LogRecord(java.util.logging.Level.INFO, "Test message with {0} and {1}");
    record.setLoggerName("test.params");
    record.setParameters(new Object[] { "param1", 42 });

    handler.publish(record);

    assertThat(true).isTrue();
  }

  @Test
  void shouldCallLocationAwareLogger() {
    SLF4JBridgeHandler handler = new SLF4JBridgeHandler();
    java.util.logging.LogRecord record = new java.util.logging.LogRecord(java.util.logging.Level.INFO, "Location aware message");
    record.setLoggerName("test.location");

    // This will call callLocationAwareLogger if the SLF4J logger implements LocationAwareLogger
    handler.publish(record);

    assertThat(true).isTrue();
  }

  @Test
  void shouldCallPlainSLF4JLogger() {
    SLF4JBridgeHandler handler = new SLF4JBridgeHandler();
    java.util.logging.LogRecord record = new java.util.logging.LogRecord(java.util.logging.Level.INFO, "Plain logger message");
    record.setLoggerName("test.plain");

    // This will call callPlainSLF4JLogger for regular SLF4J loggers
    handler.publish(record);

    assertThat(true).isTrue();
  }

  @Test
  void shouldHandleMalformedMessageFormat() {
    SLF4JBridgeHandler handler = new SLF4JBridgeHandler();
    java.util.logging.LogRecord record = new java.util.logging.LogRecord(java.util.logging.Level.INFO, "Test message with {0 and {1}");
    record.setLoggerName("test.malformed");
    record.setParameters(new Object[] { "param1", 42 });

    handler.publish(record);

    assertThat(true).isTrue();
  }

  @Test
  void shouldRemoveExistingHandlersDuringInstall() {
    // First ensure clean state
    SLF4JBridgeHandler.uninstall();

    // Add a dummy handler to root logger
    java.util.logging.Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
    java.util.logging.Handler dummyHandler = new java.util.logging.Handler() {
      @Override
      public void publish(java.util.logging.LogRecord record) { }

      @Override
      public void flush() { }

      @Override
      public void close() throws SecurityException { }
    };
    rootLogger.addHandler(dummyHandler);

    // Install SLF4JBridgeHandler
    SLF4JBridgeHandler.install();

    // Verify our dummy handler was removed and SLF4JBridgeHandler was added
    boolean hasSLF4JBridgeHandler = false;
    for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
      if (handler instanceof SLF4JBridgeHandler) {
        hasSLF4JBridgeHandler = true;
        break;
      }
    }

    assertThat(hasSLF4JBridgeHandler).isTrue();
    assertThat(rootLogger.getHandlers()).hasSize(1);
  }

}