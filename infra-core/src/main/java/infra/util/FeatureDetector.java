/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.util;

import org.jspecify.annotations.Nullable;

/**
 * A common delegate for detecting the presence of Infra framework modules and
 * commonly used third-party libraries on the classpath.
 * <p>
 * Detection results are cached, so repeated checks for the same indicator class are
 * cheap.
 *
 * <pre>{@code
 * if (FeatureDetector.isPresent(Feature.APP)) {
 *     // infra-app is on the classpath
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public abstract class FeatureDetector {

  /**
   * Determine whether the given feature is present on the classpath.
   *
   * @param feature the feature to check
   * @return whether the feature is present
   */
  public static boolean isPresent(Feature feature) {
    Boolean present = feature.present;
    if (present == null) {
      present = ClassUtils.isPresent(feature.indicatorClassName());
      feature.present = present;
    }
    return present;
  }

  /**
   * Determine whether the given feature is present when loaded with the supplied
   * class loader.
   * <p>
   * Unlike {@link #isPresent(Feature)}, this variant performs a dedicated lookup
   * with the given class loader and does <strong>not</strong> consult or update the
   * default-class-loader cache held on the {@link Feature} instance. Class-loading
   * outcomes may differ between class loaders, so the cached result would not be
   * reliable here.
   *
   * @param feature the feature to check
   * @param classLoader the class loader to use
   * (may be {@code null} which indicates the default class loader)
   * @return whether the feature is present with the supplied class loader
   */
  public static boolean isPresent(Feature feature, @Nullable ClassLoader classLoader) {
    return ClassUtils.isPresent(feature.indicatorClassName(), classLoader);
  }

  /**
   * Determine whether the given feature is missing from the classpath.
   *
   * @param feature the feature to check
   * @return whether the feature is missing
   */
  public static boolean isMissing(Feature feature) {
    return !isPresent(feature);
  }

}
