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

import org.jspecify.annotations.Nullable;

import infra.beans.factory.BeanCreationException;
import infra.util.ClassUtils;

/**
 * Exception thrown when {@link ConfigurationProperties @ConfigurationProperties} binding
 * fails.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ConfigurationPropertiesBindException extends BeanCreationException {

  private final ConfigurationPropertiesBean bean;

  ConfigurationPropertiesBindException(ConfigurationPropertiesBean bean, Exception cause) {
    super(bean.getName(), getMessage(bean), cause);
    this.bean = bean;
  }

  /**
   * Return the bean type that was being bound.
   *
   * @return the bean type
   */
  @Nullable
  public Class<?> getBeanType() {
    return this.bean.getType();
  }

  /**
   * Return the configuration properties annotation that triggered the binding.
   *
   * @return the configuration properties annotation
   */
  public ConfigurationProperties getAnnotation() {
    return this.bean.getAnnotation();
  }

  @SuppressWarnings("NullAway")
  private static String getMessage(ConfigurationPropertiesBean bean) {
    ConfigurationProperties annotation = bean.getAnnotation();
    return "Could not bind properties to '%s' : prefix=%s, ignoreInvalidFields=%s, ignoreUnknownFields=%s"
            .formatted(ClassUtils.getShortName(bean.getType()), annotation.prefix(), annotation.ignoreInvalidFields(), annotation.ignoreUnknownFields());
  }

}
