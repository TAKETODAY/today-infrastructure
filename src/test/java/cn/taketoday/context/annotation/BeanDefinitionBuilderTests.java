/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import cn.taketoday.beans.factory.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/10/3 22:35
 */
class BeanDefinitionBuilderTests {

  public void destroy() {

  }

  public void init(BeanDefinitionBuilderTests _this) {

  }

  @Test
  void testBuildBeanDefinitions() throws Exception {

    BeanDefinition definition =
            new BeanDefinitionBuilder()
                    .name("testBean")
                    .beanClass(getClass())
                    .initMethods("init")
                    .destroyMethod("destroy")
                    .primary(true)
                    .singleton()
                    .synthetic(true)
                    .build();

    assertThat(definition)
            .isNotNull()
            .isInstanceOf(BeanDefinition.class)
            .isInstanceOf(AnnotatedBeanDefinition.class);

    assertThat(definition.getBeanClass()).isEqualTo(getClass());
    assertThat(definition.getScope()).isNotNull().isEqualTo("singleton");
    assertThat(definition.getPropertyValues()).isNull();

    assertThat(definition.getName()).isEqualTo("testBean");

    assertThat(definition.isSingleton()).isTrue();
    assertThat(definition.isPrototype()).isFalse();
    assertThat(definition.getBeanClassName()).isEqualTo(getClass().getName());
    assertThat(definition.isInitialized()).isFalse();
    assertThat(definition.isSynthetic()).isTrue();
  }

}
