/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

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
