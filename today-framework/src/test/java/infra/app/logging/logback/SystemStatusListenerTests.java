/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.app.logging.logback;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.Supplier;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.BasicStatusManager;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/23 17:49
 */
@ExtendWith(OutputCaptureExtension.class)
class SystemStatusListenerTests {

  private static final String TEST_MESSAGE = "testtesttest";

  private final StatusManager statusManager = new BasicStatusManager();

  private final LoggerContext loggerContext = new LoggerContext();

  SystemStatusListenerTests() {
    this.loggerContext.setStatusManager(this.statusManager);
  }

  @Test
  void addStatusWithInfoLevelWhenNoDebugDoesNotPrint(CapturedOutput output) {
    addStatus(false, () -> new InfoStatus(TEST_MESSAGE, null));
    assertThat(output.getOut()).doesNotContain(TEST_MESSAGE);
    assertThat(output.getErr()).doesNotContain(TEST_MESSAGE);
  }

  @Test
  void addStatusWithWarningLevelWhenNoDebugPrintsToSystemErr(CapturedOutput output) {
    addStatus(false, () -> new WarnStatus(TEST_MESSAGE, null));
    assertThat(output.getOut()).doesNotContain(TEST_MESSAGE);
    assertThat(output.getErr()).contains(TEST_MESSAGE);
  }

  @Test
  void addStatusWithErrorLevelWhenNoDebugPrintsToSystemErr(CapturedOutput output) {
    addStatus(false, () -> new ErrorStatus(TEST_MESSAGE, null));
    assertThat(output.getOut()).doesNotContain(TEST_MESSAGE);
    assertThat(output.getErr()).contains(TEST_MESSAGE);
  }

  @Test
  void addStatusWithInfoLevelWhenDebugPrintsToSystemOut(CapturedOutput output) {
    addStatus(true, () -> new InfoStatus(TEST_MESSAGE, null));
    assertThat(output.getOut()).contains(TEST_MESSAGE);
    assertThat(output.getErr()).doesNotContain(TEST_MESSAGE);
  }

  @Test
  void addStatusWithWarningLevelWhenDebugPrintsToSystemOut(CapturedOutput output) {
    addStatus(true, () -> new WarnStatus(TEST_MESSAGE, null));
    assertThat(output.getOut()).contains(TEST_MESSAGE);
    assertThat(output.getErr()).doesNotContain(TEST_MESSAGE);
  }

  @Test
  void addStatusWithErrorLevelWhenDebugPrintsToSystemOut(CapturedOutput output) {
    addStatus(true, () -> new ErrorStatus(TEST_MESSAGE, null));
    assertThat(output.getOut()).contains(TEST_MESSAGE);
    assertThat(output.getErr()).doesNotContain(TEST_MESSAGE);
  }

  @Test
  void shouldRetrospectivePrintStatusOnStartAndDebugIsDisabled(CapturedOutput output) {
    this.statusManager.add(new ErrorStatus(TEST_MESSAGE, null));
    this.statusManager.add(new WarnStatus(TEST_MESSAGE, null));
    this.statusManager.add(new InfoStatus(TEST_MESSAGE, null));
    addStatus(false, () -> new InfoStatus(TEST_MESSAGE, null));
    assertThat(output.getErr()).contains("WARN " + TEST_MESSAGE);
    assertThat(output.getErr()).contains("ERROR " + TEST_MESSAGE);
    assertThat(output.getErr()).doesNotContain("INFO");
    assertThat(output.getOut()).isEmpty();
  }

  @Test
  void shouldRetrospectivePrintStatusOnStartAndDebugIsEnabled(CapturedOutput output) {
    this.statusManager.add(new ErrorStatus(TEST_MESSAGE, null));
    this.statusManager.add(new WarnStatus(TEST_MESSAGE, null));
    this.statusManager.add(new InfoStatus(TEST_MESSAGE, null));
    addStatus(true, () -> new InfoStatus(TEST_MESSAGE, null));
    assertThat(output.getErr()).isEmpty();
    assertThat(output.getOut()).contains("WARN " + TEST_MESSAGE);
    assertThat(output.getOut()).contains("ERROR " + TEST_MESSAGE);
    assertThat(output.getOut()).contains("INFO " + TEST_MESSAGE);
  }

  @Test
  void shouldNotRetrospectivePrintWhenStatusIsOutdated(CapturedOutput output) {
    ErrorStatus outdatedStatus = new ErrorStatus(TEST_MESSAGE, null);
    ReflectionTestUtils.setField(outdatedStatus, "timestamp", System.currentTimeMillis() - 300);
    this.statusManager.add(outdatedStatus);
    addStatus(false, () -> new InfoStatus(TEST_MESSAGE, null));
    assertThat(output.getOut()).isEmpty();
    assertThat(output.getErr()).isEmpty();
  }

  private void addStatus(boolean debug, Supplier<Status> statusFactory) {
    SystemStatusListener.addTo(this.loggerContext, debug);
    StatusListener listener = this.statusManager.getCopyOfStatusListenerList().get(0);
    assertThat(listener).extracting("context").isSameAs(this.loggerContext);
    listener.addStatusEvent(statusFactory.get());
  }

}