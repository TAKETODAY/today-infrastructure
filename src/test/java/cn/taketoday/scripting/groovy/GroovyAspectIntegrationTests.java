/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.scripting.groovy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 */
public class GroovyAspectIntegrationTests {

  private GenericXmlApplicationContext context;

  @Test
  public void testJavaBean() {
    context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName() + "-java-context.xml");
    TestService bean = context.getBean("javaBean", TestService.class);
    LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

    assertThat(logAdvice.getCountThrows()).isEqualTo(0);
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
                    bean::sayHello)
            .withMessage("TestServiceImpl");
    assertThat(logAdvice.getCountThrows()).isEqualTo(1);
  }

  @Test
  public void testGroovyBeanInterface() {
    context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName() + "-groovy-interface-context.xml");
    TestService bean = context.getBean("groovyBean", TestService.class);
    LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

    assertThat(logAdvice.getCountThrows()).isEqualTo(0);
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
                    bean::sayHello)
            .withMessage("GroovyServiceImpl");
    assertThat(logAdvice.getCountThrows()).isEqualTo(1);
  }

  @Test
  public void testGroovyBeanDynamic() {
    context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName() + "-groovy-dynamic-context.xml");
    TestService bean = context.getBean("groovyBean", TestService.class);
    LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

    assertThat(logAdvice.getCountThrows()).isEqualTo(0);
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
                    bean::sayHello)
            .withMessage("GroovyServiceImpl");
    // No proxy here because the pointcut only applies to the concrete class, not the interface
    assertThat(logAdvice.getCountThrows()).isEqualTo(0);
    assertThat(logAdvice.getCountBefore()).isEqualTo(0);
  }

  @Test
  public void testGroovyBeanProxyTargetClass() {
    context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName() + "-groovy-proxy-target-class-context.xml");
    TestService bean = context.getBean("groovyBean", TestService.class);
    LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

    assertThat(logAdvice.getCountThrows()).isEqualTo(0);
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
                    bean::sayHello)
            .withMessage("GroovyServiceImpl");
    assertThat(logAdvice.getCountBefore()).isEqualTo(1);
    assertThat(logAdvice.getCountThrows()).isEqualTo(1);
  }

  @AfterEach
  public void close() {
    if (context != null) {
      context.close();
    }
  }

}
