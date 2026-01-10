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

import java.util.Map;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.MethodParameter;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.util.StringUtils;
import infra.web.HandlerMatchingMetadata;
import infra.web.RequestContext;
import infra.web.annotation.PathVariable;
import infra.web.bind.MissingPathVariableException;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.handler.method.support.UriComponentsContributor;
import infra.web.util.UriComponentsBuilder;

/**
 * Resolves method arguments annotated with an @{@link PathVariable}.
 *
 * <p>An @{@link PathVariable} is a named value that gets resolved from a URI template variable.
 * It is always required and does not have a default value to fall back on. See the base class
 * {@link AbstractNamedValueResolvingStrategy}
 * for more information on how named values are processed.
 *
 * <p>If the method parameter type is {@link Map}, the name specified in the annotation is used
 * to resolve the URI variable String value. The value is then converted to a {@link Map} via
 * type conversion, assuming a suitable {@link Converter} has been registered.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 16:32
 */
public class PathVariableMethodArgumentResolver extends AbstractNamedValueResolvingStrategy
        implements UriComponentsContributor {

  private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);

  public PathVariableMethodArgumentResolver() {
  }

  public PathVariableMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory) {
    super(factory);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    if (resolvable.hasParameterAnnotation(PathVariable.class)) {
      if (Map.class.isAssignableFrom(resolvable.getParameter().getParameterType())) {
        PathVariable pathVariable = resolvable.getParameterAnnotation(PathVariable.class);
        return pathVariable != null && StringUtils.hasText(pathVariable.value());
      }
      return true;
    }
    return false;
  }

  @Nullable
  @Override
  protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    if (matchingMetadata != null) {
      return matchingMetadata.getUriVariable(name);
    }
    return null;
  }

  @Override
  protected void handleMissingValue(String name, MethodParameter parameter) {
    throw new MissingPathVariableException(name, parameter);
  }

  @Override
  protected void handleMissingValueAfterConversion(String name, MethodParameter parameter, RequestContext request) {
    throw new MissingPathVariableException(name, parameter, true);
  }

  @Override
  protected void handleResolvedValue(@Nullable Object arg, String name, ResolvableMethodParameter resolvable, RequestContext request) {
    request.matchingMetadata().getPathVariables().put(name, arg);
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return supportsParameter(new ResolvableMethodParameter(parameter));
  }

  @Override
  public void contributeMethodArgument(MethodParameter parameter, @Nullable Object value,
          UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

    if (Map.class.isAssignableFrom(parameter.getParameterType())) {
      return;
    }

    PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
    String name = (ann != null && StringUtils.isNotEmpty(ann.value()) ? ann.value() : parameter.getParameterName());
    String formatted = formatUriValue(conversionService, new TypeDescriptor(parameter), value);
    uriVariables.put(name, formatted);
  }

  @Nullable
  protected String formatUriValue(@Nullable ConversionService cs, @Nullable TypeDescriptor sourceType, @Nullable Object value) {
    if (value instanceof String) {
      return (String) value;
    }
    else if (cs != null) {
      return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
    }
    else {
      return String.valueOf(value);
    }
  }

}

