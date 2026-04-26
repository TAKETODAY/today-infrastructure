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

package infra.app.webmvc.test.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import infra.app.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;
import infra.context.annotation.config.TypeExcludeFilter;
import infra.core.conversion.Converter;
import infra.core.conversion.GenericConverter;
import infra.http.converter.HttpMessageConverter;
import infra.stereotype.Controller;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;
import infra.web.HandlerInterceptor;
import infra.web.annotation.ControllerAdvice;
import infra.web.bind.resolver.ParameterResolvingStrategy;
import infra.web.config.WebMvcRegistrations;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.server.error.ErrorAttributes;

/**
 * {@link TypeExcludeFilter} for {@link WebMvcTest @WebMvcTest}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Yanming Zhou
 */
class WebMvcTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<WebMvcTest> {

  private static final Class<?>[] NO_CONTROLLERS = {};

  private static final String[] OPTIONAL_INCLUDES = { "tools.jackson.databind.JacksonModule",
          "infra.jackson.JacksonComponent", "com.fasterxml.jackson.databind.Module" };

  private static final Set<Class<?>> KNOWN_INCLUDES;

  static {
    Set<Class<?>> includes = new LinkedHashSet<>();
    includes.add(ControllerAdvice.class);
    includes.add(WebMvcConfigurer.class);
    includes.add(WebMvcRegistrations.class);
    includes.add(infra.mock.api.Filter.class);
    includes.add(ParameterResolvingStrategy.class);
    includes.add(HttpMessageConverter.class);
    includes.add(ErrorAttributes.class);
    includes.add(Converter.class);
    includes.add(GenericConverter.class);
    includes.add(HandlerInterceptor.class);
    for (String optionalInclude : OPTIONAL_INCLUDES) {
      try {
        includes.add(ClassUtils.forName(optionalInclude, null));
      }
      catch (Exception ex) {
        // Ignore
      }
    }
    KNOWN_INCLUDES = Collections.unmodifiableSet(includes);
  }

  private static final Set<Class<?>> KNOWN_INCLUDES_AND_CONTROLLER;

  static {
    Set<Class<?>> includes = new LinkedHashSet<>(KNOWN_INCLUDES);
    includes.add(Controller.class);
    KNOWN_INCLUDES_AND_CONTROLLER = Collections.unmodifiableSet(includes);
  }

  private final Class<?>[] controllers;

  WebMvcTypeExcludeFilter(Class<?> testClass) {
    super(testClass);
    this.controllers = Optional.ofNullable(getAnnotation().getValue("controllers", Class[].class)).orElse(NO_CONTROLLERS);
  }

  @Override
  protected Set<Class<?>> getKnownIncludes() {
    if (ObjectUtils.isEmpty(this.controllers)) {
      return KNOWN_INCLUDES_AND_CONTROLLER;
    }
    return KNOWN_INCLUDES;
  }

  @Override
  protected Set<Class<?>> getComponentIncludes() {
    return new LinkedHashSet<>(Arrays.asList(this.controllers));
  }

}
