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

package infra.aop.framework;

import org.junit.jupiter.api.Test;

import infra.aop.interceptor.DebugInterceptor;
import infra.context.ApplicationContext;
import infra.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Objenesis proxy creation.
 *
 * @author Oliver Gierke
 */
public class ObjenesisProxyTests {

  @Test
  public void appliesAspectToClassWithComplexConstructor() {
    @SuppressWarnings("resource")
    ApplicationContext context = new ClassPathXmlApplicationContext("ObjenesisProxyTests-context.xml", getClass());

    ClassWithComplexConstructor bean = context.getBean(ClassWithComplexConstructor.class);
    bean.method();

    DebugInterceptor interceptor = context.getBean(DebugInterceptor.class);
    assertThat(interceptor.getCount()).isEqualTo(1L);
    assertThat(bean.getDependency().getValue()).isEqualTo(1);
  }

}
