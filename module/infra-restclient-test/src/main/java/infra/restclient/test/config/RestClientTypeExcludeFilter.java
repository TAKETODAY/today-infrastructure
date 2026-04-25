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

package infra.restclient.test.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import infra.app.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;
import infra.context.annotation.config.TypeExcludeFilter;
import infra.util.ClassUtils;

/**
 * {@link TypeExcludeFilter} for {@link RestClientTest @RestClientTest}.
 *
 * @author Stephane Nicoll
 */
class RestClientTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<RestClientTest> {

  private static final Class<?>[] NO_COMPONENTS = {};

  private static final String[] OPTIONAL_INCLUDES = {
          "tools.jackson.databind.JacksonModule",
          "infra.jackson.JacksonComponent",
          "com.fasterxml.jackson.databind.Module"
  };

  private static final Set<Class<?>> KNOWN_INCLUDES;

  static {
    Set<Class<?>> includes = new LinkedHashSet<>();
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

  private final Class<?>[] components;

  RestClientTypeExcludeFilter(Class<?> testClass) {
    super(testClass);
    this.components = Optional.ofNullable(getAnnotation().getValue("components", Class[].class)).orElse(NO_COMPONENTS);
  }

  @Override
  protected Set<Class<?>> getKnownIncludes() {
    return KNOWN_INCLUDES;
  }

  @Override
  protected Set<Class<?>> getComponentIncludes() {
    return new LinkedHashSet<>(Arrays.asList(this.components));
  }

}
