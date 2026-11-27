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

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LoggingAdapterTests {

  @Test
  void slf4jAdapterPreservesLogLevelsAndFormatsArguments() {
    var target = mock(org.slf4j.Logger.class);
    given(target.getName()).willReturn("test.adapter");
    given(target.isDebugEnabled()).willReturn(true);
    given(target.isTraceEnabled()).willReturn(true);
    var logger = new Slf4jLogger(target);

    logger.trace("trace {}", 1);
    logger.debug("debug {}", 2);
    logger.info("info {}", 3);
    var failure = new IllegalStateException("failed");
    logger.warn("warn {}", 4, failure);
    logger.error("error {}", 5, failure);

    assertThat(logger.isTraceEnabled()).isTrue();
    verify(target).trace("trace 1", (Throwable) null);
    verify(target).debug("debug 2", (Throwable) null);
    verify(target).info("info 3", (Throwable) null);
    verify(target).warn("warn 4", failure);
    verify(target).error("error 5", failure);
    assertThat(logger.readResolve()).isInstanceOfSatisfying(Logger.class,
            restored -> assertThat(restored.getName()).isEqualTo("test.adapter"));
  }

  @Test
  void log4jAdapterPreservesLiteralStringsAndObjectMessages() {
    var target = mock(ExtendedLogger.class);
    var logger = new Log4j2Logger(target);
    var failure = new IllegalStateException("failed");
    Object message = new Object();

    logger.warn((Object) "literal {}");
    logger.error((Object) "literal {}", failure);
    logger.info(message, failure);

    verify(target).logIfEnabled(Logger.FQCN, org.apache.logging.log4j.Level.WARN, null, "literal {}");
    verify(target).logIfEnabled(Logger.FQCN, org.apache.logging.log4j.Level.ERROR, null, "literal {}", failure);
    verify(target).logIfEnabled(Logger.FQCN, org.apache.logging.log4j.Level.INFO, null, message, failure);
  }

  @Test
  void log4jAdapterExposesArgumentsAndCauseForFormattedMessages() {
    var target = mock(ExtendedLogger.class);
    var logger = new Log4j2Logger(target);
    var failure = new IllegalStateException("failed");

    logger.error("Could not load {}", "settings", failure);

    var captor = ArgumentCaptor.forClass(Message.class);
    verify(target).logIfEnabled(eq(Logger.FQCN), eq(org.apache.logging.log4j.Level.ERROR),
            isNull(), captor.capture(), eq(failure));
    Message message = captor.getValue();
    assertThat(message.getParameters()).containsExactly("settings");
    assertThat(message.getThrowable()).isSameAs(failure);
    assertThat(message.getFormattedMessage()).isEqualTo("Could not load settings");
    assertThat(message.getFormattedMessage()).isSameAs(message.getFormat());
  }

  @Test
  void formatsMultiElementPrimitiveArraysAndCycles() {
    Object[] cycle = new Object[1];
    cycle[0] = cycle;
    assertThat(MessageFormatter.format("{} {} {} {} {} {} {} {} {} {}", new Object[] {
            new boolean[] { true, false }, new byte[] { 1, 2 }, new char[] { 'a', 'b' }, new short[] { 3, 4 },
            new int[] { 5, 6 }, new long[] { 7, 8 }, new float[] { 1.5f, 2.5f }, new double[] { 3.5, 4.5 },
            new String[] { "left", "right" }, cycle
    })).isEqualTo("[true, false] [1, 2] [a, b] [3, 4] [5, 6] [7, 8] [1.5, 2.5] [3.5, 4.5] [left, right] [[...]]");
  }

}
