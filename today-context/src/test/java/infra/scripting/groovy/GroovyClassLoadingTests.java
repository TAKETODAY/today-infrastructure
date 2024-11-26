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
package infra.scripting.groovy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import groovy.lang.GroovyClassLoader;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.support.StaticApplicationContext;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
public class GroovyClassLoadingTests {

  @Test
  @SuppressWarnings("resource")
  public void classLoading() throws Exception {
    StaticApplicationContext context = new StaticApplicationContext();

    GroovyClassLoader gcl = new GroovyClassLoader();
    Class<?> class1 = gcl.parseClass("class TestBean { def myMethod() { \"foo\" } }");
    Class<?> class2 = gcl.parseClass("class TestBean { def myMethod() { \"bar\" } }");

    context.registerBeanDefinition("testBean", new RootBeanDefinition(class1));
    Object testBean1 = context.getBean("testBean");
    Method method1 = class1.getDeclaredMethod("myMethod", new Class<?>[0]);
    Object result1 = ReflectionUtils.invokeMethod(method1, testBean1);
    assertThat(result1).isEqualTo("foo");

    context.removeBeanDefinition("testBean");
    context.registerBeanDefinition("testBean", new RootBeanDefinition(class2));
    Object testBean2 = context.getBean("testBean");
    Method method2 = class2.getDeclaredMethod("myMethod", new Class<?>[0]);
    Object result2 = ReflectionUtils.invokeMethod(method2, testBean2);
    assertThat(result2).isEqualTo("bar");
  }

}
