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

package infra.validation.beanvalidation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/19 21:50
 */
class MethodValidationExcludeFilterTests {

  @Test
  void byAnnotationWhenClassIsAnnotatedExcludes() {
    MethodValidationExcludeFilter filter = MethodValidationExcludeFilter.byAnnotation(Indicator.class);
    assertThat(filter.isExcluded(Annotated.class)).isTrue();
  }

  @Test
  void byAnnotationWhenClassIsNotAnnotatedIncludes() {
    MethodValidationExcludeFilter filter = MethodValidationExcludeFilter.byAnnotation(Indicator.class);
    assertThat(filter.isExcluded(Plain.class)).isFalse();
  }

  static class Plain {

  }

  @Indicator
  static class Annotated {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Indicator {

  }

}
