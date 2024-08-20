/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.logging.logback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.qos.logback.classic.LoggerContext;
import cn.taketoday.framework.logging.LoggingSystem;
import cn.taketoday.test.classpath.ForkedClassPath;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for parallel initialization of {@link LogbackLoggingSystem} that are separate
 * from {@link LogbackLoggingSystemTests}. This isolation allows them to have complete
 * control over how and when the logging system is initialized.
 *
 * @author Andy Wilkinson
 */
class LogbackLoggingSystemParallelInitializationTests {

  private final LoggingSystem loggingSystem = LoggingSystem
          .get(LogbackLoggingSystemParallelInitializationTests.class.getClassLoader());

  @AfterEach
  void cleanUp() {
    this.loggingSystem.cleanUp();
    ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
  }

  @Test
  @ForkedClassPath
  void noExceptionsAreThrownWhenBeforeInitializeIsCalledInParallel() {
    List<Thread> threads = new ArrayList<>();
    List<Throwable> exceptions = new CopyOnWriteArrayList<>();
    for (int i = 0; i < 10; i++) {
      Thread thread = new Thread(this.loggingSystem::beforeInitialize);
      thread.setUncaughtExceptionHandler((t, ex) -> exceptions.add(ex));
      threads.add(thread);
    }
    threads.forEach(Thread::start);
    threads.forEach(this::join);
    assertThat(exceptions).isEmpty();
  }

  private void join(Thread thread) {
    try {
      thread.join();
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ex);
    }
  }

}
