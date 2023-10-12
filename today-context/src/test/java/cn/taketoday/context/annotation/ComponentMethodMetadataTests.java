/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
public class ComponentMethodMetadataTests {

  @Test
  public void providesBeanMethodBeanDefinition() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Conf.class);
    BeanDefinition beanDefinition = context.getBeanDefinition("myBean");
    assertThat(beanDefinition).as("should provide AnnotatedBeanDefinition").isInstanceOf(AnnotatedBeanDefinition.class);
    Map<String, Object> annotationAttributes =
            ((AnnotatedBeanDefinition) beanDefinition).getFactoryMethodMetadata().getAnnotationAttributes(MyAnnotation.class.getName());
    assertThat(annotationAttributes.get("value")).isEqualTo("test");
    context.close();
  }

  @Configuration
  static class Conf {

    @Bean
    @MyAnnotation("test")
    public MyBean myBean() {
      return new MyBean();
    }
  }

  static class MyBean {
  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface MyAnnotation {

    String value();
  }

}
