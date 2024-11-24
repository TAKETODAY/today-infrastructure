/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.retry.stats;

import java.util.concurrent.atomic.AtomicInteger;

import infra.core.AttributeAccessorSupport;
import infra.retry.RetryStatistics;

/**
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultRetryStatistics extends AttributeAccessorSupport implements RetryStatistics, MutableRetryStatistics {

  private String name;

  private final AtomicInteger startedCount = new AtomicInteger();

  private final AtomicInteger completeCount = new AtomicInteger();

  private final AtomicInteger recoveryCount = new AtomicInteger();

  private final AtomicInteger errorCount = new AtomicInteger();

  private final AtomicInteger abortCount = new AtomicInteger();

  public DefaultRetryStatistics(String name) {
    this.name = name;
  }

  @Override
  public int getCompleteCount() {
    return completeCount.get();
  }

  @Override
  public int getStartedCount() {
    return startedCount.get();
  }

  @Override
  public int getErrorCount() {
    return errorCount.get();
  }

  @Override
  public int getAbortCount() {
    return abortCount.get();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getRecoveryCount() {
    return recoveryCount.get();
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void incrementStartedCount() {
    this.startedCount.incrementAndGet();
  }

  @Override
  public void incrementCompleteCount() {
    this.completeCount.incrementAndGet();
  }

  @Override
  public void incrementRecoveryCount() {
    this.recoveryCount.incrementAndGet();
  }

  @Override
  public void incrementErrorCount() {
    this.errorCount.incrementAndGet();
  }

  @Override
  public void incrementAbortCount() {
    this.abortCount.incrementAndGet();
  }

  @Override
  public String toString() {
    return "DefaultRetryStatistics [name=%s, startedCount=%s, completeCount=%s, recoveryCount=%s, errorCount=%s, abortCount=%s]"
            .formatted(name, startedCount, completeCount, recoveryCount, errorCount, abortCount);
  }

}
