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
package cn.taketoday.context.autowire;

import cn.taketoday.beans.factory.DependencySetter;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.lang.Autowired;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Today <br>
 *
 * 2018-08-04 15:56
 */
class AutowiredPropertyResolverTests {

  @Autowired
  private String name;

  @Named
  private String name1;

  @SuppressWarnings("unused")
  @Inject
  private String name2;

  @Test
  void autowiredPropertyResolver() throws Throwable {

    try (ConfigurableApplicationContext context = new StandardApplicationContext()) {
      context.refresh();

      AutowiredPropertyResolver resolver = new AutowiredPropertyResolver();
      PropsReader propsReader = new PropsReader(context.getEnvironment());
      PropertyResolvingContext resolvingContext = new PropertyResolvingContext(context, propsReader);
      DependencySetter resolveProperty = resolver.resolveProperty(
              resolvingContext,
              BeanProperty.valueOf(getClass(), "name")//
      );

      assertThat(resolveProperty).isNotNull();

//      PropertySetter name1 = resolver.resolveProperty(resolvingContext, BeanProperty.valueOf(getClass(), "name1"));
//      PropertySetter name2 = resolver.resolveProperty(resolvingContext, BeanProperty.valueOf(getClass(), "name2"));
//
//      assertThat(name1).isNotNull();
//      assertThat(name1.getName()).isNotNull().isEqualTo("name1");
//
//      assertThat(name2).isNotNull();
//      assertThat(name2.getName()).isNotNull().isEqualTo("name2");

    }
  }

}
