/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core;

import cn.taketoday.lang.Nullable;

/**
 * A common delegate for detecting a GraalVM native image environment.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class NativeDetector {

  // See https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java
  @Nullable
  private static final String imageCode = System.getProperty("org.graalvm.nativeimage.imagecode");

  private static final boolean inNativeImage = (imageCode != null);

  /**
   * Returns {@code true} if running in a native image context (for example
   * {@code buildtime}, {@code runtime}, or {@code agent}) expressed by setting the
   * {@code org.graalvm.nativeimage.imagecode} system property to any value.
   */
  public static boolean inNativeImage() {
    return inNativeImage;
  }

  /**
   * Returns {@code true} if running in any of the specified native image context(s).
   *
   * @param contexts the native image context(s)
   */
  public static boolean inNativeImage(Context... contexts) {
    for (Context context : contexts) {
      if (context.key.equals(imageCode)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Native image context as defined in GraalVM's
   * <a href="https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java">ImageInfo</a>.
   */
  public enum Context {

    /**
     * The code is executing in the context of image building.
     */
    BUILD("buildtime"),

    /**
     * The code is executing at image runtime.
     */
    RUN("runtime");

    private final String key;

    Context(final String key) {
      this.key = key;
    }

    @Override
    public String toString() {
      return this.key;
    }
  }

}
