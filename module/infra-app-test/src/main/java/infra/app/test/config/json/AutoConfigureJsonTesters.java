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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.app.test.context.PropertyMapping;
import infra.app.test.json.BasicJsonTester;
import infra.app.test.json.GsonTester;
import infra.app.test.json.JacksonTester;
import infra.app.test.json.JsonbTester;
import infra.context.annotation.config.ImportAutoConfiguration;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of JSON testers.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureJson
@ImportAutoConfiguration
@PropertyMapping("infra.test.jsontesters")
public @interface AutoConfigureJsonTesters {

  /**
   * If {@link BasicJsonTester}, {@link JacksonTester}, {@link JsonbTester} and
   * {@link GsonTester} beans should be registered. Defaults to {@code true}.
   *
   * @return if tester support is enabled
   */
  boolean enabled() default true;

}
