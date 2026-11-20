/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.app.context;

import org.junit.jupiter.api.Test;

import infra.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class InfraTestAnnotationAdditionalTests {

  @Test
  void equalityIncludesArgumentsMainMethodAndWebEnvironment() {
    InfraTestAnnotation annotation = new InfraTestAnnotation(Default.class);
    assertThat(annotation).isEqualTo(annotation).isEqualTo(new InfraTestAnnotation(Default.class))
            .hasSameHashCodeAs(new InfraTestAnnotation(Default.class));
    assertThat(annotation).isNotEqualTo(null).isNotEqualTo("other")
            .isNotEqualTo(new InfraTestAnnotation(Arguments.class))
            .isNotEqualTo(new InfraTestAnnotation(Main.class))
            .isNotEqualTo(new InfraTestAnnotation(Web.class));
    assertThat(annotation.getArgs()).isEmpty();
    assertThat(annotation.getUseMainMethod()).isEqualTo(InfraTest.UseMainMethod.NEVER);
    assertThat(annotation.getWebEnvironment()).isEqualTo(InfraTest.WebEnvironment.NONE);
    var config = new MergedContextConfiguration(getClass(), null, null, null, null);
    assertThat(InfraTestAnnotation.get(config)).isEqualTo(annotation);
  }

  static class Default { }

  @InfraTest(args = "--flag", webEnvironment = InfraTest.WebEnvironment.NONE)
  static class Arguments { }

  @InfraTest(useMainMethod = InfraTest.UseMainMethod.ALWAYS, webEnvironment = InfraTest.WebEnvironment.NONE)
  static class Main { }

  @InfraTest(webEnvironment = InfraTest.WebEnvironment.MOCK)
  static class Web { }

}
