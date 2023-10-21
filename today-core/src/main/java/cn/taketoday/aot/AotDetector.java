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

package cn.taketoday.aot;

import cn.taketoday.core.NativeDetector;
import cn.taketoday.lang.TodayStrategies;

import static cn.taketoday.core.NativeDetector.Context;

/**
 * Utility for determining if AOT-processed optimizations must be used rather
 * than the regular runtime. Strictly for internal use within the framework.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AotDetector {

  /**
   * System property that indicates the application should run with AOT
   * generated artifacts. If such optimizations are not available, it is
   * recommended to throw an exception rather than fall back to the regular
   * runtime behavior.
   */
  public static final String AOT_ENABLED = "infra.aot.enabled";

  private static final boolean inNativeImage = NativeDetector.inNativeImage(Context.RUN, Context.BUILD);

  /**
   * Determine whether AOT optimizations must be considered at runtime. This
   * is mandatory in a native image but can be triggered on the JVM using
   * the {@value #AOT_ENABLED} Infra property.
   *
   * @return whether AOT optimizations must be considered
   */
  public static boolean useGeneratedArtifacts() {
    return inNativeImage || TodayStrategies.getFlag(AOT_ENABLED);
  }

}
