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

package infra.context.properties.bind;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.properties.ConfigurationProperties;
import infra.core.env.Environment;

/**
 * Annotation that can be used to specify the default value when binding to an immutable
 * property. This annotation can also be used with nested properties to indicate that a
 * value should always be bound (rather than binding {@code null}). The value from this
 * annotation will only be used if the property is not found in the property sources used
 * by the {@link Binder}. For example, if the property is present in the
 * {@link Environment} when binding to
 * {@link ConfigurationProperties @ConfigurationProperties},
 * the default value for the property will not be used even if the property value is
 * empty.
 *
 * @author Madhura Bhave
 * @author Pavel Anisimov
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface DefaultValue {

  /**
   * The default value of the property. Can be an array of values for collection or
   * array-based properties.
   *
   * @return the default value of the property.
   */
  String[] value() default {};

}
