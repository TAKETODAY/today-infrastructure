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

package infra.app.health;

import java.io.File;

import infra.app.health.contributor.AbstractHealthIndicator;
import infra.app.health.contributor.Health;
import infra.app.health.contributor.HealthIndicator;
import infra.app.health.contributor.Status;
import infra.lang.Assert;
import infra.logging.LogMessage;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.DataSize;

/**
 * A {@link HealthIndicator} that checks available disk space and reports a status of
 * {@link Status#DOWN} when it drops below a configurable threshold.
 *
 * @author Mattias Severson
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class DiskSpaceHealthIndicator extends AbstractHealthIndicator {

  private static final Logger logger = LoggerFactory.getLogger(DiskSpaceHealthIndicator.class);

  private final File path;

  private final DataSize threshold;

  /**
   * Create a new {@code DiskSpaceHealthIndicator} instance.
   *
   * @param path the Path used to compute the available disk space
   * @param threshold the minimum disk space that should be available
   */
  public DiskSpaceHealthIndicator(File path, DataSize threshold) {
    super("DiskSpace health check failed");
    Assert.isTrue(!threshold.isNegative(), "'threshold' must be greater than or equal to 0");
    this.path = path;
    this.threshold = threshold;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) throws Exception {
    long diskFreeInBytes = this.path.getUsableSpace();
    if (diskFreeInBytes >= this.threshold.toBytes()) {
      builder.up();
    }
    else {
      logger.warn(LogMessage.format(
              "Free disk space at path '%s' below threshold. Available: %d bytes (threshold: %s)",
              this.path.getAbsolutePath(), diskFreeInBytes, this.threshold));
      builder.down();
    }
    builder.withDetail("total", this.path.getTotalSpace())
            .withDetail("free", diskFreeInBytes)
            .withDetail("threshold", this.threshold.toBytes())
            .withDetail("path", this.path.getAbsolutePath())
            .withDetail("exists", this.path.exists());
  }

}
