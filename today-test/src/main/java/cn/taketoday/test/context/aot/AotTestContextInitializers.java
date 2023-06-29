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

package cn.taketoday.test.context.aot;

import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.lang.Nullable;

/**
 * {@code AotTestContextInitializers} provides mappings from test classes to
 * AOT-optimized context initializers.
 *
 * <p>Intended solely for internal use within the framework.
 *
 * <p>If we are not running in {@linkplain AotDetector#useGeneratedArtifacts()
 * AOT mode} or if a test class is not {@linkplain #isSupportedTestClass(Class)
 * supported} in AOT mode, {@link #getContextInitializer(Class)} and
 * {@link #getContextInitializerClass(Class)} will return {@code null}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class AotTestContextInitializers {

  private final Map<String, Supplier<ApplicationContextInitializer>> contextInitializers;

  private final Map<String, Class<ApplicationContextInitializer>> contextInitializerClasses;

  public AotTestContextInitializers() {
    this(AotTestContextInitializersFactory.getContextInitializers(),
            AotTestContextInitializersFactory.getContextInitializerClasses());
  }

  AotTestContextInitializers(
          Map<String, Supplier<ApplicationContextInitializer>> contextInitializers,
          Map<String, Class<ApplicationContextInitializer>> contextInitializerClasses) {

    this.contextInitializers = contextInitializers;
    this.contextInitializerClasses = contextInitializerClasses;
  }

  /**
   * Determine if the specified test class has an AOT-optimized application context
   * initializer.
   * <p>If this method returns {@code true}, {@link #getContextInitializer(Class)}
   * should not return {@code null}.
   */
  public boolean isSupportedTestClass(Class<?> testClass) {
    return this.contextInitializers.containsKey(testClass.getName());
  }

  /**
   * Get the AOT {@link ApplicationContextInitializer} for the specified test class.
   *
   * @return the AOT context initializer, or {@code null} if there is no AOT context
   * initializer for the specified test class
   * @see #isSupportedTestClass(Class)
   * @see #getContextInitializerClass(Class)
   */
  @Nullable
  public ApplicationContextInitializer getContextInitializer(Class<?> testClass) {
    var supplier = contextInitializers.get(testClass.getName());
    return supplier != null ? supplier.get() : null;
  }

  /**
   * Get the AOT {@link ApplicationContextInitializer} {@link Class} for the
   * specified test class.
   *
   * @return the AOT context initializer class, or {@code null} if there is no
   * AOT context initializer for the specified test class
   * @see #isSupportedTestClass(Class)
   * @see #getContextInitializer(Class)
   */
  @Nullable
  public Class<ApplicationContextInitializer> getContextInitializerClass(Class<?> testClass) {
    return contextInitializerClasses.get(testClass.getName());
  }

}
