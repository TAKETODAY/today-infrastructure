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

package infra.context.expression;

import infra.beans.factory.Aware;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.EmbeddedValueResolver;
import infra.core.StringValueResolver;

/**
 * Interface to be implemented by any object that wishes to be notified of a
 * {@code StringValueResolver} for the resolution of embedded definition values.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurableBeanFactory#resolveEmbeddedValue(String)
 * @see ConfigurableBeanFactory#getBeanExpressionResolver()
 * @see EmbeddedValueResolver
 * @since 4.0
 */
public interface EmbeddedValueResolverAware extends Aware {

  /**
   * Set the StringValueResolver to use for resolving embedded definition values.
   */
  void setEmbeddedValueResolver(StringValueResolver resolver);

}
