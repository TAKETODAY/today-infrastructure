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

import java.util.function.Consumer;

import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.lang.Assert;

/**
 * {@link BindHandler} that can be used to track bound configuration properties.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BoundPropertiesTrackingBindHandler extends AbstractBindHandler {

  private final Consumer<ConfigurationProperty> consumer;

  public BoundPropertiesTrackingBindHandler(Consumer<ConfigurationProperty> consumer) {
    Assert.notNull(consumer, "Consumer is required");
    this.consumer = consumer;
  }

  @Nullable
  @Override
  public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
    if (context.getConfigurationProperty() != null && name.equals(context.getConfigurationProperty().getName())) {
      this.consumer.accept(context.getConfigurationProperty());
    }
    return super.onSuccess(name, target, context, result);
  }

}
