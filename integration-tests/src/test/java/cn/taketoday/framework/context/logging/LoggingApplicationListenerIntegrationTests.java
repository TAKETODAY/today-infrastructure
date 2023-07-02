/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.context.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.builder.ApplicationBuilder;
import cn.taketoday.framework.context.event.ApplicationStartingEvent;
import cn.taketoday.framework.logging.LogFile;
import cn.taketoday.framework.logging.LoggingSystem;
import cn.taketoday.framework.logging.LoggingSystemProperty;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.stereotype.Component;

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
