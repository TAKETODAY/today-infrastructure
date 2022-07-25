/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.bytecode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Provides utility methods for the asm.test package.
 *
 * @author Eric Bruneton
 */
final class Util {

  private Util() { }

  static int getMajorJavaVersion() {
    String javaVersion = System.getProperty("java.version");
    StringTokenizer tokenizer = new StringTokenizer(javaVersion, "._");
    String javaMajorVersionText = tokenizer.nextToken();
    int majorVersion = Integer.parseInt(javaMajorVersionText);
    if (majorVersion != 1) {
      return majorVersion;
    }
    javaMajorVersionText = tokenizer.nextToken();
    return Integer.parseInt(javaMajorVersionText);
  }

  static boolean previewFeatureEnabled() {
    try {
      Class<?> managementFactoryClass = Class.forName("java.lang.management.ManagementFactory");
      Method getRuntimeMxBean = managementFactoryClass.getMethod("getRuntimeMXBean");
      Object runtimeMxBean = getRuntimeMxBean.invoke(null);
      Class<?> runtimeMxBeanClass = Class.forName("java.lang.management.RuntimeMXBean");
      Method getInputArguments = runtimeMxBeanClass.getMethod("getInputArguments");
      List<?> argumentList = (List<?>) getInputArguments.invoke(runtimeMxBean);
      return argumentList.contains("--enable-preview");
    }
    catch (ClassNotFoundException e) { // JMX may be not available
      return false;
    }
    catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
    catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw new AssertionError(cause); // NOPMD
    }
  }
}
