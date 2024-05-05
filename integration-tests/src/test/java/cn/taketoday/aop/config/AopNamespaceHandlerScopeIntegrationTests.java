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

package cn.taketoday.aop.config;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.annotation.ImportResource;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.session.config.EnableWebSession;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for scoped proxy use in conjunction with aop: namespace.
 * Deemed an integration test because .web mocks and application contexts are required.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see cn.taketoday.aop.config.AopNamespaceHandlerTests
 */
@JUnitWebConfig(classes = AopNamespaceHandlerScopeIntegrationTests.Config.class)
class AopNamespaceHandlerScopeIntegrationTests {

  @Autowired
  ITestBean singletonScoped;

  @Autowired
  ITestBean requestScoped;

  @Autowired
  ITestBean sessionScoped;

  @Autowired
  ITestBean sessionScopedAlias;

  @Autowired
  ITestBean testBean;

  @ImportResource("classpath:cn/taketoday/aop/config/AopNamespaceHandlerScopeIntegrationTests-context.xml")
  @EnableWebSession
  static class Config {

  }

  @Test
  void testSingletonScoping() throws Exception {
    assertThat(AopUtils.isAopProxy(singletonScoped)).as("Should be AOP proxy").isTrue();
    boolean condition = singletonScoped instanceof TestBean;
    assertThat(condition).as("Should be target class proxy").isTrue();
    String rob = "Rob Harrop";
    String bram = "Bram Smeets";
    assertThat(singletonScoped.getName()).isEqualTo(rob);
    singletonScoped.setName(bram);
    assertThat(singletonScoped.getName()).isEqualTo(bram);
    ITestBean deserialized = serializeAndDeserialize(singletonScoped);
    assertThat(deserialized.getName()).isEqualTo(bram);
  }

  @Test
  void testRequestScoping() throws Exception {
    HttpMockRequestImpl oldRequest = new HttpMockRequestImpl();
    HttpMockRequestImpl newRequest = new HttpMockRequestImpl();
    RequestContextHolder.set(new MockRequestContext(null, oldRequest, null));

    assertThat(AopUtils.isAopProxy(requestScoped)).as("Should be AOP proxy").isTrue();
    boolean condition = requestScoped instanceof TestBean;
    assertThat(condition).as("Should be target class proxy").isTrue();

    assertThat(AopUtils.isAopProxy(testBean)).as("Should be AOP proxy").isTrue();
    boolean condition1 = testBean instanceof TestBean;
    assertThat(condition1).as("Regular bean should be JDK proxy").isFalse();

    String rob = "Rob Harrop";
    String bram = "Bram Smeets";

    assertThat(requestScoped.getName()).isEqualTo(rob);
    requestScoped.setName(bram);
    RequestContextHolder.set(new MockRequestContext(null, newRequest, null));
    assertThat(requestScoped.getName()).isEqualTo(rob);
    RequestContextHolder.set(new MockRequestContext(null, oldRequest, null));
    assertThat(requestScoped.getName()).isEqualTo(bram);

    assertThat(((Advised) requestScoped).getAdvisors().length > 0).as("Should have advisors").isTrue();
  }

  @Test
  void testSessionScoping() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    RequestContextHolder.set(new MockRequestContext(null, request, new MockHttpResponseImpl()));

    assertThat(AopUtils.isAopProxy(sessionScoped)).as("Should be AOP proxy").isTrue();
    boolean condition1 = sessionScoped instanceof TestBean;
    assertThat(condition1).as("Should not be target class proxy").isFalse();

    assertThat(sessionScopedAlias).isSameAs(sessionScoped);

    assertThat(AopUtils.isAopProxy(testBean)).as("Should be AOP proxy").isTrue();
    boolean condition = testBean instanceof TestBean;
    assertThat(condition).as("Regular bean should be JDK proxy").isFalse();

    String rob = "Rob Harrop";
    String bram = "Bram Smeets";

    assertThat(sessionScoped.getName()).isEqualTo(rob);
    sessionScoped.setName(bram);

    assertThat(((Advised) sessionScoped).getAdvisors().length > 0).as("Should have advisors").isTrue();
  }

  @SuppressWarnings("unchecked")
  public static <T> T serializeAndDeserialize(T o) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(o);
      oos.flush();
    }
    byte[] bytes = baos.toByteArray();

    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    try (ObjectInputStream ois = new ObjectInputStream(is)) {
      return (T) ois.readObject();
    }
  }

}
