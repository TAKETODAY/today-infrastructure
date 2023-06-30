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
 * Factory for {@link AotTestContextInitializers}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class AotTestContextInitializersFactory {

  @Nullable
  private static volatile Map<String, Supplier<ApplicationContextInitializer>> contextInitializers;

  @Nullable
  private static volatile Map<String, Class<ApplicationContextInitializer>> contextInitializerClasses;

  private AotTestContextInitializersFactory() { }

  /**
   * Get the underlying map.
   * <p>If the map is not already loaded, this method loads the map from the
   * generated class when running in {@linkplain AotDetector#useGeneratedArtifacts()
   * AOT execution mode} and otherwise creates an immutable, empty map.
   */
  static Map<String, Supplier<ApplicationContextInitializer>> getContextInitializers() {
    Map<String, Supplier<ApplicationContextInitializer>> initializers = contextInitializers;
    if (initializers == null) {
      synchronized(AotTestContextInitializersFactory.class) {
        initializers = contextInitializers;
        if (initializers == null) {
          initializers = (AotDetector.useGeneratedArtifacts() ? loadContextInitializersMap() : Map.of());
          contextInitializers = initializers;
        }
      }
    }
    return initializers;
  }

  static Map<String, Class<ApplicationContextInitializer>> getContextInitializerClasses() {
    Map<String, Class<ApplicationContextInitializer>> initializerClasses = contextInitializerClasses;
    if (initializerClasses == null) {
      synchronized(AotTestContextInitializersFactory.class) {
        initializerClasses = contextInitializerClasses;
        if (initializerClasses == null) {
          initializerClasses = (AotDetector.useGeneratedArtifacts() ? loadContextInitializerClassesMap() : Map.of());
          contextInitializerClasses = initializerClasses;
        }
      }
    }
    return initializerClasses;
  }

  /**
   * Reset the factory.
   * <p>Only for internal use.
   */
  static void reset() {
    synchronized(AotTestContextInitializersFactory.class) {
      contextInitializers = null;
      contextInitializerClasses = null;
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Supplier<ApplicationContextInitializer>> loadContextInitializersMap() {
    String className = AotTestContextInitializersCodeGenerator.GENERATED_MAPPINGS_CLASS_NAME;
    String methodName = AotTestContextInitializersCodeGenerator.GET_CONTEXT_INITIALIZERS_METHOD_NAME;
    return GeneratedMapUtils.loadMap(className, methodName);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Class<ApplicationContextInitializer>> loadContextInitializerClassesMap() {
    String className = AotTestContextInitializersCodeGenerator.GENERATED_MAPPINGS_CLASS_NAME;
    String methodName = AotTestContextInitializersCodeGenerator.GET_CONTEXT_INITIALIZER_CLASSES_METHOD_NAME;
    return GeneratedMapUtils.loadMap(className, methodName);
  }

}
