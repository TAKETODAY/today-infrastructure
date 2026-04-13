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

package infra.app.test.context;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

import infra.app.test.context.InfraTest.UseMainMethod;
import infra.app.test.context.InfraTest.WebEnvironment;
import infra.context.ConfigurableApplicationContext;
import infra.test.context.ContextCustomizer;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizer} to track attributes of {@link InfraTest @InfraTest}
 * that are taken into account when evaluating a {@link MergedContextConfiguration} to
 * determine if a context can be shared between tests.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
class InfraTestAnnotation implements ContextCustomizer {

  private static final String[] NO_ARGS = new String[0];

  private static final InfraTestAnnotation DEFAULT = new InfraTestAnnotation((InfraTest) null);

  private final String[] args;

  private final WebEnvironment webEnvironment;

  private final UseMainMethod useMainMethod;

  InfraTestAnnotation(Class<?> testClass) {
    this(TestContextAnnotationUtils.findMergedAnnotation(testClass, InfraTest.class));
  }

  private InfraTestAnnotation(@Nullable InfraTest annotation) {
    this.args = annotation != null ? annotation.args() : NO_ARGS;
    this.webEnvironment = annotation != null ? annotation.webEnvironment() : WebEnvironment.NONE;
    this.useMainMethod = annotation != null ? annotation.useMainMethod() : UseMainMethod.NEVER;
  }

  @Override
  public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    InfraTestAnnotation other = (InfraTestAnnotation) obj;
    boolean result = Arrays.equals(this.args, other.args);
    result = result && this.useMainMethod == other.useMainMethod;
    result = result && this.webEnvironment == other.webEnvironment;
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(this.args);
    result = prime * result + Objects.hash(this.useMainMethod, this.webEnvironment);
    return result;
  }

  String[] getArgs() {
    return this.args;
  }

  WebEnvironment getWebEnvironment() {
    return this.webEnvironment;
  }

  UseMainMethod getUseMainMethod() {
    return this.useMainMethod;
  }

  /**
   * Return the application arguments from the given {@link MergedContextConfiguration}.
   *
   * @param mergedConfig the merged config to check
   * @return a {@link InfraTestAnnotation} instance
   */
  static InfraTestAnnotation get(MergedContextConfiguration mergedConfig) {
    for (ContextCustomizer customizer : mergedConfig.getContextCustomizers()) {
      if (customizer instanceof InfraTestAnnotation annotation) {
        return annotation;
      }
    }
    return DEFAULT;
  }

}
