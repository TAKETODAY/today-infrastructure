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

package infra.context.condition;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.ImportSelector;
import infra.context.condition.OnBeanCondition.BeanTypeDeductionException;
import infra.core.type.AnnotationMetadata;
import infra.test.classpath.ClassPathExclusions;

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
