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

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.annotation.Value;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.MethodParameter;
import infra.web.RequestContext;
import infra.web.bind.WebDataBinder;
import infra.web.handler.method.NamedValueInfo;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves method arguments annotated with {@code @Value}.
 *
 * <p>An {@code @Value} does not have a name but gets resolved from the default
 * value string, which may contain ${...} placeholder or Infra Expression
 * Language #{...} expressions.
 *
 * <p>A {@link WebDataBinder} may be invoked to apply type conversion to
 * resolved argument value.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/27 13:37
 */
public class ExpressionValueMethodArgumentResolver extends AbstractNamedValueResolvingStrategy {

  /**
   * Create a new {@link ExpressionValueMethodArgumentResolver} instance.
   *
   * @param beanFactory a bean factory to use for resolving  ${...}
   * placeholder and #{...} SpEL expressions in default values;
   * or {@code null} if default values are not expected to contain expressions
   */
  public ExpressionValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
    super(beanFactory);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.hasParameterAnnotation(Value.class);
  }

  @Override
  @SuppressWarnings("NullAway")
  protected NamedValueInfo getNamedValueInfo(ResolvableMethodParameter resolvable) {
    if (resolvable.hasNamedValueInfo()) {
      return resolvable.getNamedValueInfo();
    }
    Value annotation = resolvable.getParameterAnnotation(Value.class);
    NamedValueInfo namedValueInfo = resolvable.getNamedValueInfo();
    resolvable.withNamedValueInfo(new NamedValueInfo(namedValueInfo, annotation.value()));
    return resolvable.getNamedValueInfo();
  }

  @Nullable
  @Override
  protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
    // No name to resolve
    return null;
  }

  @Override
  protected void handleMissingValue(String name, MethodParameter parameter) {
    throw new UnsupportedOperationException("@Value is never required: " + parameter.getMethod());
  }

}
