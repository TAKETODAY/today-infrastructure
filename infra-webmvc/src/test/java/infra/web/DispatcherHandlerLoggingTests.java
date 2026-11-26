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

package infra.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import infra.context.support.GenericApplicationContext;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;

class DispatcherHandlerLoggingTests {

  private final Logger logger = (Logger) LoggerFactory.getLogger(DispatcherHandler.class);

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  private Level previousLevel;

  private boolean previousDebugEnabled;

  private Field debugEnabled;

  @BeforeEach
  void captureLogs() throws Exception {
    previousLevel = logger.getLevel();
    logger.setLevel(Level.TRACE);
    // The framework logger caches its debug flag at initialization time.
    debugEnabled = infra.logging.Logger.class.getDeclaredField("debugEnabled");
    debugEnabled.setAccessible(true);
    previousDebugEnabled = debugEnabled.getBoolean(WebLifecycleManager.log);
    debugEnabled.setBoolean(WebLifecycleManager.log, true);
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void restoreLogger() throws Exception {
    logger.detachAppender(appender);
    logger.setLevel(previousLevel);
    appender.stop();
    debugEnabled.setBoolean(WebLifecycleManager.log, previousDebugEnabled);
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void traceLogsRespectRequestDetailsSetting(boolean enableDetails) throws Exception {
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      application.refresh();
      DispatcherHandler dispatcher = new DispatcherHandler(application);
      dispatcher.setEnableLoggingRequestDetails(enableDetails);
      dispatcher.setHandlerMapping(context -> (HttpRequestHandler) request -> {
        request.setHeader("X-Result", "sample-result");
        return HttpRequestHandler.NONE_RETURN_VALUE;
      });
      dispatcher.start();
      MockHttpContext request = new MockHttpContext(application);
      request.request.setRequestURI("/sample");
      request.request.setQueryString("page=1");
      request.request.addParameter("name", "sample-param");
      request.request.addHeader("X-First", "sample-first");
      request.request.addHeader("X-Second", "sample-second");

      dispatcher.handleRequest(request);

      String messages = messages();
      assertThat(messages).contains("/sample?page=1", "Completed", "Detected");
      if (enableDetails) {
        assertThat(messages).contains("sample-param", "sample-first", "sample-second", "sample-result");
      }
      else {
        assertThat(messages).contains("parameters={masked}", "headers={masked}")
                .doesNotContain("sample-param", "sample-first", "sample-second", "sample-result");
      }
    }
  }

  @Test
  void debugLogsMultipartRequestWithoutParsingParameters() throws Exception {
    logger.setLevel(Level.DEBUG);
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      application.refresh();
      DispatcherHandler dispatcher = new DispatcherHandler(application);
      dispatcher.setHandlerMapping(context -> (HttpRequestHandler) request -> HttpRequestHandler.NONE_RETURN_VALUE);
      dispatcher.start();
      MockHttpContext request = new MockHttpContext(application) {
        @Override
        public boolean isMultipart() {
          return true;
        }
      };

      dispatcher.handleRequest(request);

      assertThat(messages()).contains("parameters={multipart}", "Completed");
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void logsUnresolvedDispatchFailure(boolean trace) throws Exception {
    logger.setLevel(trace ? Level.TRACE : Level.DEBUG);
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      application.refresh();
      DispatcherHandler dispatcher = new DispatcherHandler(application);
      IllegalStateException failure = new IllegalStateException("sample failure");
      dispatcher.setHandlerMapping(context -> { throw failure; });
      dispatcher.setExceptionHandler((context, exception, handler) -> null);
      dispatcher.start();
      MockHttpContext request = new MockHttpContext(application);

      dispatcher.handleRequest(request);

      assertThat(request.getStatus()).isEqualTo(500);
      assertThat(appender.list).anySatisfy(event -> {
        assertThat(event.getFormattedMessage()).isEqualTo("Failed to complete request");
        assertThat(event.getLevel()).isEqualTo(trace ? Level.TRACE : Level.DEBUG);
      });
    }
  }

  private String messages() {
    return String.join("\n", appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
  }

}
