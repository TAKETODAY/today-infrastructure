/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.context.junit.jupiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @InfraExtensionConfig} is a type-level annotation that can be used to
 * configure the behavior of the {@link InfraExtension}.
 *
 * <p>This annotation is only applicable to {@link org.junit.jupiter.api.Nested @Nested}
 * test class hierarchies and should be applied to the top-level enclosing class
 * of a {@code @Nested} test class hierarchy. Consequently, there is no need to
 * declare this annotation on a test class that does not contain {@code @Nested}
 * test classes.
 *
 * <p>Note that
 * {@link infra.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * does not apply to this annotation: {@code @InfraExtensionConfig} will always be
 * detected within a {@code @Nested} test class hierarchy, effectively disregarding
 * any {@code @NestedTestConfiguration(OVERRIDE)} declarations.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see infra.test.context.junit.jupiter.InfraExtension InfraExtension
 * @see infra.test.context.junit.jupiter.JUnitConfig @JUnitConfig
 * @see infra.test.context.junit.jupiter.web.JUnitWebConfig @JUnitWebConfig
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface InfraExtensionConfig {

  /**
   * Specify whether the {@link InfraExtension} should use a test-class scoped
   * {@link org.junit.jupiter.api.extension.ExtensionContext ExtensionContext}
   * within {@link org.junit.jupiter.api.Nested @Nested} test class hierarchies.
   *
   * <p>By default, the {@code InfraExtension} uses a test-method scoped
   * {@code ExtensionContext}. Thus, there is no need to declare this annotation
   * attribute with a value of {@code false}.
   *
   * <p>Similarly, if your top-level test class is configured to use JUnit Jupiter’s
   * {@code @TestInstance(Lifecycle.PER_CLASS)} semantics, the {@code InfraExtension}
   * will always use a test-class scoped {@code ExtensionContext}, and there is no need
   * to declare {@code @InfraExtensionConfig(useTestClassScopedExtensionContext = true)}.
   *
   * <p>Furthermore, this attribute takes precedence over global configuration
   * of the {@code infra.test.extension.context.scope} property.
   *
   * @see InfraExtension
   * @see InfraExtension#EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME
   * @see InfraExtension.ExtensionContextScope
   */
  boolean useTestClassScopedExtensionContext();

}
