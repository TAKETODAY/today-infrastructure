/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.annotation.Qualifier;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/21 16:18
 */
class AnnotatedBeanDefinitionReaderTests {

  @Test
  void registerBeanWithQualifiers() {
    GenericApplicationContext context = new GenericApplicationContext();
    AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);

    reader.registerBean(TestBean.class, "primary", Primary.class);
    assertThat(context.getBeanDefinition("primary").isPrimary()).isTrue();

    reader.registerBean(TestBean.class, "fallback", Fallback.class);
    assertThat(context.getBeanDefinition("fallback").isFallback()).isTrue();

    reader.registerBean(TestBean.class, "lazy", Lazy.class);
    assertThat(context.getBeanDefinition("lazy").isLazyInit()).isTrue();

    reader.registerBean(TestBean.class, "customQualifier", CustomQualifier.class);
    assertThat(context.getBeanDefinition("customQualifier"))
            .isInstanceOfSatisfying(AbstractBeanDefinition.class, abd ->
                    assertThat(abd.hasQualifier(CustomQualifier.class.getTypeName())).isTrue());
  }

  @Lazy(false)
  static class TestBean {

  }

  @Target({ ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface CustomQualifier {

  }
}