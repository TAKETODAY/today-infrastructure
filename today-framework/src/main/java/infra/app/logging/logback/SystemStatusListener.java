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

import java.io.PrintStream;
import java.util.List;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.BasicStatusManager;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.util.StatusListenerConfigHelper;

/**
 * {@link StatusListener} used to print appropriate status messages to {@link System#out}
 * or {@link System#err}. Note that this class extends {@link OnConsoleStatusListener} so
 * that {@link BasicStatusManager#add(StatusListener)} does not add the same listener
 * twice. It also implements a version of retrospectivePrint that can filter status
 * messages by level.
 *
 * @author Dmytro Nosan
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class SystemStatusListener extends OnConsoleStatusListener {

  private static final long RETROSPECTIVE_THRESHOLD = 300;

  private final boolean debug;

  private SystemStatusListener(boolean debug) {
    this.debug = debug;
    setResetResistant(false);
    setRetrospective(0);
  }

  @Override
  public void start() {
    super.start();
    retrospectivePrint();
  }

  private void retrospectivePrint() {
    if (this.context == null) {
      return;
    }
    long now = System.currentTimeMillis();
    List<Status> statusList = this.context.getStatusManager().getCopyOfStatusList();
    statusList.stream()
            .filter((status) -> getElapsedTime(status, now) < RETROSPECTIVE_THRESHOLD)
            .forEach(this::addStatusEvent);
  }

  @Override
  public void addStatusEvent(Status status) {
    if (this.debug || status.getLevel() >= Status.WARN) {
      super.addStatusEvent(status);
    }
  }

  @Override
  protected PrintStream getPrintStream() {
    return (!this.debug) ? System.err : System.out;
  }

  private static long getElapsedTime(Status status, long now) {
    return now - status.getTimestamp();
  }

  static void addTo(LoggerContext loggerContext) {
    addTo(loggerContext, false);
  }

  static void addTo(LoggerContext loggerContext, boolean debug) {
    StatusListenerConfigHelper.addOnConsoleListenerInstance(loggerContext, new SystemStatusListener(debug));
  }

}
