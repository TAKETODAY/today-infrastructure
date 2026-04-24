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

package infra.app.test.config.json;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.app.InfraApplication;
import infra.app.test.config.OverrideAutoConfiguration;
import infra.app.test.context.filter.annotation.TypeExcludeFilters;
import infra.app.test.json.GsonTester;
import infra.app.test.json.JacksonTester;
import infra.app.test.json.JsonbTester;
import infra.context.annotation.ComponentScan.Filter;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.core.annotation.AliasFor;
import infra.core.env.Environment;
import infra.test.context.BootstrapWith;
import infra.test.context.junit.jupiter.InfraExtension;

/**
 * Annotation for a JSON test that focuses <strong>only</strong> on JSON serialization.
 * <p>
 * Using this annotation only enables auto-configuration that is relevant to JSON tests.
 * Similarly, component scanning is limited to beans annotated with:
 * <ul>
 * <li>{@code @JacksonComponent}</li>
 * </ul>
 * <p>
 * as well as beans that implement:
 * <ul>
 * <li>{@code JacksonModule}, if Jackson is available</li>
 * </ul>
 * <p>
 * By default, tests annotated with {@code JsonTest} will also initialize
 * {@link JacksonTester}, {@link JsonbTester} and {@link GsonTester} fields. More
 * fine-grained control can be provided through the
 * {@link AutoConfigureJsonTesters @AutoConfigureJsonTesters} annotation.
 * <p>
 * When using JUnit 4, this annotation should be used in combination with
 * {@code @RunWith(SpringRunner.class)}.
 *
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @see AutoConfigureJson
 * @see AutoConfigureJsonTesters
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(JsonTestContextBootstrapper.class)
@ExtendWith(InfraExtension.class)
@OverrideAutoConfiguration(enabled = false)
@TypeExcludeFilters(JsonTypeExcludeFilter.class)
@AutoConfigureJsonTesters
@ImportAutoConfiguration
public @interface JsonTest {

  /**
   * Properties in form {@literal key=value} that should be added to the Spring
   * {@link Environment} before the test runs.
   *
   * @return the properties to add
   */
  String[] properties() default {};

  /**
   * Determines if default filtering should be used with
   * {@link InfraApplication @InfraApplication}. By default, only
   * {@code @JacksonComponent} and {@code JacksonModule} beans are included.
   *
   * @return if default filters should be used
   * @see #includeFilters()
   * @see #excludeFilters()
   */
  boolean useDefaultFilters() default true;

  /**
   * A set of include filters which can be used to add otherwise filtered beans to the
   * application context.
   *
   * @return include filters to apply
   */
  Filter[] includeFilters() default {};

  /**
   * A set of exclude filters which can be used to filter beans that would otherwise be
   * added to the application context.
   *
   * @return exclude filters to apply
   */
  Filter[] excludeFilters() default {};

  /**
   * Auto-configuration exclusions that should be applied for this test.
   *
   * @return auto-configuration exclusions to apply
   */
  @AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
  Class<?>[] excludeAutoConfiguration() default {};

}
