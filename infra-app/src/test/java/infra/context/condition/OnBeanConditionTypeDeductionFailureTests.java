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

package infra.context.condition;

import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.ImportSelector;
import infra.context.condition.OnBeanCondition.BeanTypeDeductionException;
import infra.core.type.AnnotationMetadata;
import infra.test.classpath.ClassPathExclusions;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link OnBeanCondition} when deduction of the bean's type fails
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("jackson-core-*.jar")
class OnBeanConditionTypeDeductionFailureTests {

  @Test
  void conditionalOnMissingBeanWithDeducedTypeThatIsPartiallyMissingFromClassPath() {
    assertThatExceptionOfType(Exception.class)
            .isThrownBy(() -> new AnnotationConfigApplicationContext(ImportingConfiguration.class).close())
            .satisfies((ex) -> {
              Throwable beanTypeDeductionException = findNestedCause(ex, BeanTypeDeductionException.class);
              assertThat(beanTypeDeductionException).hasMessage("Failed to deduce bean type for "
                      + OnMissingBeanConfiguration.class.getName() + ".objectMapper");
              assertThat(findNestedCause(beanTypeDeductionException, NoClassDefFoundError.class)).isNotNull();

            });
  }

  private Throwable findNestedCause(Throwable ex, Class<? extends Throwable> target) {
    Throwable candidate = ex;
    while (candidate != null) {
      if (target.isInstance(candidate)) {
        return candidate;
      }
      candidate = candidate.getCause();
    }
    return null;
  }

  @Configuration(proxyBeanMethods = false)
  @Import(OnMissingBeanImportSelector.class)
  static class ImportingConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class OnMissingBeanConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

  }

  static class OnMissingBeanImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importMetadata) {
      return new String[] { OnMissingBeanConfiguration.class.getName() };
    }

  }

}
