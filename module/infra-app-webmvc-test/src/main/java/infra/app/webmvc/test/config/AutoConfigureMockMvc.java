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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.app.test.context.PropertyMapping;
import infra.app.test.context.PropertyMapping.Skip;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MvcResult;
import infra.test.web.mock.assertj.MockMvcTester;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of {@link MockMvc}. If AssertJ is available a {@link MockMvcTester}
 * is auto-configured as well.
 *
 * @author Phillip Webb
 * @see MockMvcAutoConfiguration
 * @see InfraMockMvcBuilderCustomizer
 * @since 5.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@PropertyMapping("infra.test.mockmvc")
public @interface AutoConfigureMockMvc {

  /**
   * How {@link MvcResult} information should be printed after each MockMVC invocation.
   *
   * @return how information is printed
   */
  @PropertyMapping(skip = Skip.ON_DEFAULT_VALUE)
  MockMvcPrint print() default MockMvcPrint.DEFAULT;

  /**
   * If {@link MvcResult} information should be printed only if the test fails.
   *
   * @return {@code true} if printing only occurs on failure
   */
  boolean printOnlyOnFailure() default true;

}
