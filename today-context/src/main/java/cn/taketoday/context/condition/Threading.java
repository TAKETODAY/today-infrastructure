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

package cn.taketoday.context.condition;

import cn.taketoday.core.JavaVersion;
import cn.taketoday.core.env.Environment;

/**
 * Threading of the application.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public enum Threading {

  /**
   * Platform threads. Active if virtual threads are not active.
   */
  PLATFORM {
    @Override
    public boolean isActive(Environment environment) {
      return !VIRTUAL.isActive(environment);
    }
  },
  /**
   * Virtual threads. Active if {@code spring.threads.virtual.enabled} is {@code true}
   * and running on Java 21 or later.
   */
  VIRTUAL {
    @Override
    public boolean isActive(Environment environment) {
      boolean virtualThreadsEnabled = environment.getProperty("infra.threads.virtual.enabled", boolean.class,
              false);
      return virtualThreadsEnabled && JavaVersion.getJavaVersion().isEqualOrNewerThan(JavaVersion.TWENTY_ONE);
    }
  };

  /**
   * Determines whether the threading is active.
   *
   * @param environment the environment
   * @return whether the threading is active
   */
  public abstract boolean isActive(Environment environment);

}
