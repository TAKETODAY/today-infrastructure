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

package infra.core.testfixture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;

import infra.logging.Logger;
import infra.util.ReflectionUtils;

import static infra.core.testfixture.TestGroup.FULL_LOG;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A smoke test that verifies full framework log output is enabled. It only runs when the
 * {@link TestGroup#FULL_LOG FULL_LOG} test group is active, i.e. when the build is
 * invoked with {@code -PtestGroups=FULL_LOG}. The root logger is raised to {@code TRACE}
 * at JVM startup by {@code TestConventions}, so no per-test bootstrap is needed here.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
@EnabledForTestGroups(FULL_LOG)
class FullLogTests {

  private static final Logger log = infra.logging.LoggerFactory.getLogger(FullLogTests.class);

  @Test
  void rootLoggerIsAtTraceWhenFullLogEnabled() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Level rootLevel = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getLevel();
    assertThat(rootLevel).isEqualTo(Level.TRACE);
  }

  @Test
  void dumpFullFrameworkLogs() {
    log.trace("trace level message");
    log.debug("debug level message");
    log.info("info level message");
    log.warn("warn level message");

    Method[] methods = ReflectionUtils.getAllDeclaredMethods(FullLogTests.class);
    assertThat(methods).isNotEmpty();
    log.info("Discovered {} methods on {}", methods.length, FullLogTests.class.getName());
    log.debug("Methods: {}", Arrays.toString(methods));
  }

}
