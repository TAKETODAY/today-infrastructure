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

package infra.context.support;

import org.jspecify.annotations.Nullable;

import infra.context.expression.EmbeddedValueResolverAware;
import infra.core.StringValueResolver;

/**
 * Convenient base class for components with a need for embedded value resolution
 * (i.e. {@link EmbeddedValueResolverAware} consumers).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 16:34
 */
public class EmbeddedValueResolutionSupport implements EmbeddedValueResolverAware {

  @Nullable
  private StringValueResolver embeddedValueResolver;

  @Override
  public void setEmbeddedValueResolver(@Nullable StringValueResolver resolver) {
    this.embeddedValueResolver = resolver;
  }

  /**
   * Resolve the given embedded value through this instance's {@link StringValueResolver}.
   *
   * @param value the value to resolve
   * @return the resolved value, or always the original value if no resolver is available
   * @see #setEmbeddedValueResolver
   */
  @Nullable
  protected String resolveEmbeddedValue(String value) {
    return embeddedValueResolver != null ? embeddedValueResolver.resolveStringValue(value) : value;
  }

}
