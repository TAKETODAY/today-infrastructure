/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Import;
import infra.core.annotation.AliasFor;
import infra.stereotype.Component;

/**
 * Configures the base packages used when scanning for
 * {@link ConfigurationProperties @ConfigurationProperties} classes. One of
 * {@link #basePackageClasses()}, {@link #basePackages()} or its alias {@link #value()}
 * may be specified to define specific packages to scan. If specific packages are not
 * defined scanning will occur from the package of the class with this annotation.
 * <p>
 * Note: Classes annotated or meta-annotated with {@link Component @Component} will not be
 * picked up by this annotation.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableConfigurationProperties
@Import(ConfigurationPropertiesScanRegistrar.class)
public @interface ConfigurationPropertiesScan {

  /**
   * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
   * declarations e.g.: {@code @ConfigurationPropertiesScan("org.my.pkg")} instead of
   * {@code @ConfigurationPropertiesScan(basePackages="org.my.pkg")}.
   *
   * @return the base packages to scan
   */
  @AliasFor("basePackages")
  String[] value() default {};

  /**
   * Base packages to scan for configuration properties. {@link #value()} is an alias
   * for (and mutually exclusive with) this attribute.
   * <p>
   * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
   * package names.
   *
   * @return the base packages to scan
   */
  @AliasFor("value")
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages()} for specifying the packages to
   * scan for configuration properties. The package of each class specified will be
   * scanned.
   * <p>
   * Consider creating a special no-op marker class or interface in each package that
   * serves no purpose other than being referenced by this attribute.
   *
   * @return classes from the base packages to scan
   */
  Class<?>[] basePackageClasses() default {};

}
