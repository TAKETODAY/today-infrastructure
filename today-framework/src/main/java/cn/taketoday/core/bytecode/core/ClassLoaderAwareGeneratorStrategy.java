/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.bytecode.core;

/**
 * GeneratorStrategy variant which exposes the application ClassLoader
 * as current thread context ClassLoader for the time of class generation.
 * The ASM ClassWriter in ASM variant will pick it up when doing
 * common superclass resolution.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/10/30 17:59
 * @since 4.0
 */
public class ClassLoaderAwareGeneratorStrategy extends DefaultGeneratorStrategy {

  private final ClassLoader classLoader;

  public ClassLoaderAwareGeneratorStrategy(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public byte[] generate(ClassGenerator cg) throws Exception {
    if (this.classLoader == null) {
      return super.generate(cg);
    }

    Thread currentThread = Thread.currentThread();
    ClassLoader threadContextClassLoader;
    try {
      threadContextClassLoader = currentThread.getContextClassLoader();
    }
    catch (Throwable ex) {
      // Cannot access thread context ClassLoader - falling back...
      return super.generate(cg);
    }

    boolean overrideClassLoader = !this.classLoader.equals(threadContextClassLoader);
    if (overrideClassLoader) {
      currentThread.setContextClassLoader(this.classLoader);
    }
    try {
      return super.generate(cg);
    }
    finally {
      if (overrideClassLoader) {
        // Reset original thread context ClassLoader.
        currentThread.setContextClassLoader(threadContextClassLoader);
      }
    }
  }

}
