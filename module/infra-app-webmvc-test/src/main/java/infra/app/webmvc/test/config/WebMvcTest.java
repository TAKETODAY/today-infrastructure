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

package infra.app.webmvc.test.config;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.app.InfraApplication;
import infra.app.test.config.OverrideAutoConfiguration;
import infra.app.test.context.InfraTest;
import infra.app.test.context.filter.annotation.TypeExcludeFilters;
import infra.context.annotation.ComponentScan.Filter;
import infra.context.annotation.Import;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.core.annotation.AliasFor;
import infra.core.env.Environment;
import infra.test.context.BootstrapWith;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.web.mock.MockMvc;

/**
 * Annotation that can be used for a Web MVC test that focuses <strong>only</strong> on
 * Web MVC components.
 * <p>
 * Using this annotation only enables auto-configuration that is relevant to MVC tests.
 * Similarly, component scanning is limited to beans annotated with:
 * <ul>
 * <li>{@code @Controller}</li>
 * <li>{@code @ControllerAdvice}</li>
 * <li>{@code @JacksonComponent}</li>
 * </ul>
 * <p>
 * as well as beans that implement:
 * <ul>
 * <li>{@code Converter}</li>
 * <li>{@code ErrorAttributes}</li>
 * <li>{@code Filter}</li>
 * <li>{@code GenericConverter}</li>
 * <li>{@code HandlerInterceptor}</li>
 * <li>{@code ParameterResolvingStrategy}</li>
 * <li>{@code HttpMessageConverter}</li>
 * <li>{@code JacksonModule}, if Jackson is available</li>
 * <li>{@code WebMvcConfigurer}</li>
 * <li>{@code WebMvcRegistrations}</li>
 * </ul>
 * <p>
 * By default, tests annotated with {@code @WebMvcTest} will also auto-configure Infra
 * Security and {@link MockMvc}. For more fine-grained control of MockMVC the
 * {@link AutoConfigureMockMvc @AutoConfigureMockMvc} annotation can be used.
 * <p>
 * Typically {@code @WebMvcTest} is used in combination with
 * {@link infra.test.context.bean.override.mockito.MockitoBean @MockitoBean}
 * or {@link Import @Import} to create any collaborators required by your
 * {@code @Controller} beans.
 * <p>
 * If you are looking to load your full application configuration and use MockMVC, you
 * should consider {@link InfraTest @InfraTest} combined with
 * {@link AutoConfigureMockMvc @AutoConfigureMockMvc} rather than this annotation.
 *
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @see AutoConfigureWebMvc
 * @see AutoConfigureMockMvc
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(WebMvcTestContextBootstrapper.class)
@ExtendWith(InfraExtension.class)
@OverrideAutoConfiguration(enabled = false)
@TypeExcludeFilters(WebMvcTypeExcludeFilter.class)
@AutoConfigureWebMvc
@AutoConfigureMockMvc
@ImportAutoConfiguration
public @interface WebMvcTest {

  /**
   * Properties in form {@literal key=value} that should be added to the Infra
   * {@link Environment} before the test runs.
   *
   * @return the properties to add
   */
  String[] properties() default {};

  /**
   * Specifies the controllers to test. This is an alias of {@link #controllers()} which
   * can be used for brevity if no other attributes are defined. See
   * {@link #controllers()} for details.
   *
   * @return the controllers to test
   * @see #controllers()
   */
  @AliasFor("controllers")
  Class<?>[] value() default {};

  /**
   * Specifies the controllers to test. May be left blank if all {@code @Controller}
   * beans should be added to the application context.
   *
   * @return the controllers to test
   * @see #value()
   */
  @AliasFor("value")
  Class<?>[] controllers() default {};

  /**
   * Determines if default filtering should be used with
   * {@link InfraApplication @InfraApplication}. By default only
   * {@code @Controller} (when no explicit {@link #controllers() controllers} are
   * defined), {@code @ControllerAdvice} and {@code WebMvcConfigurer} beans are
   * included.
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
