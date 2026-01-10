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

import org.jspecify.annotations.Nullable;

import java.io.Serial;

import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.origin.Origin;
import infra.origin.OriginProvider;

/**
 * Exception thrown when binding fails.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BindException extends RuntimeException implements OriginProvider {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Bindable<?> target;

  @Nullable
  private final ConfigurationProperty property;

  private final ConfigurationPropertyName name;

  BindException(ConfigurationPropertyName name, Bindable<?> target, @Nullable ConfigurationProperty property, Throwable cause) {
    super(buildMessage(name, target), cause);
    this.name = name;
    this.target = target;
    this.property = property;
  }

  /**
   * Return the name of the configuration property being bound.
   *
   * @return the configuration property name
   */
  public ConfigurationPropertyName getName() {
    return this.name;
  }

  /**
   * Return the target being bound.
   *
   * @return the bind target
   */
  public Bindable<?> getTarget() {
    return this.target;
  }

  /**
   * Return the configuration property name of the item that was being bound.
   *
   * @return the configuration property name
   */
  @Nullable
  public ConfigurationProperty getProperty() {
    return this.property;
  }

  @Nullable
  @Override
  public Origin getOrigin() {
    return Origin.from(this.name);
  }

  private static String buildMessage(@Nullable ConfigurationPropertyName name, Bindable<?> target) {
    StringBuilder message = new StringBuilder();
    message.append("Failed to bind properties");
    message.append((name != null) ? " under '" + name + "'" : "");
    message.append(" to ").append(target.getType());
    return message.toString();
  }

}
