/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.context.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import infra.app.ApplicationType;
import infra.app.builder.ApplicationBuilder;
import infra.app.context.event.ApplicationStartingEvent;
import infra.app.logging.LogFile;
import infra.app.logging.LoggingSystem;
import infra.app.logging.LoggingSystemProperty;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.beans.factory.ObjectProvider;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LoggingApplicationListener}.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
class LoggingApplicationListenerIntegrationTests {

  @Test
  void loggingSystemRegisteredInTheContext() {
    try (ConfigurableApplicationContext context = new ApplicationBuilder(SampleService.class)
            .type(ApplicationType.NORMAL)
            .run()) {
      SampleService service = context.getBean(SampleService.class);
      assertThat(service.loggingSystem).isNotNull();
    }
  }

  @Test
  void logFileRegisteredInTheContextWhenApplicable(@TempDir File tempDir) {
    String logFile = new File(tempDir, "test.log").getAbsolutePath();
    try (ConfigurableApplicationContext context = new ApplicationBuilder(SampleService.class)
            .type(ApplicationType.NORMAL)
            .properties("logging.file.name=" + logFile)
            .run()) {
      SampleService service = context.getBean(SampleService.class);
      assertThat(service.logFile).isNotNull();
      assertThat(service.logFile).hasToString(logFile);
    }
    finally {
      System.clearProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName());
    }
  }

  @Test
  void loggingPerformedDuringChildApplicationStartIsNotLost(CapturedOutput output) {
    new ApplicationBuilder(Config.class).type(ApplicationType.NORMAL)
            .child(Config.class)
            .type(ApplicationType.NORMAL)
            .listeners(new ApplicationListener<ApplicationStartingEvent>() {

              private final Logger logger = LoggerFactory.getLogger(getClass());

              @Override
              public void onApplicationEvent(ApplicationStartingEvent event) {
                this.logger.info("Child application starting");
              }

            })
            .run();
    assertThat(output).contains("Child application starting");
  }

  @Component
  static class SampleService {

    private final LoggingSystem loggingSystem;

    private final LogFile logFile;

    SampleService(LoggingSystem loggingSystem, ObjectProvider<LogFile> logFile) {
      this.loggingSystem = loggingSystem;
      this.logFile = logFile.getIfAvailable();
    }

  }

  static class Config {

  }

}
