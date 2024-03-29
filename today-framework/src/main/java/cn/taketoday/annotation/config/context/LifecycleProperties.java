/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.context;

import java.time.Duration;

import cn.taketoday.context.properties.ConfigurationProperties;

/**
 * Configuration properties for lifecycle processing.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "infra.lifecycle")
public class LifecycleProperties {

  /**
   * Timeout for the shutdown of any phase (group of SmartLifecycle beans with the same
   * 'phase' value).
   */
  private Duration timeoutPerShutdownPhase = Duration.ofSeconds(30);

  public Duration getTimeoutPerShutdownPhase() {
    return this.timeoutPerShutdownPhase;
  }

  public void setTimeoutPerShutdownPhase(Duration timeoutPerShutdownPhase) {
    this.timeoutPerShutdownPhase = timeoutPerShutdownPhase;
  }

}
