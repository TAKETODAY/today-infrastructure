/*
 * Copyright 2002-present the original author or authors.
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

package infra.aop.config;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import infra.aop.framework.Advised;
import infra.aop.support.AopUtils;
import infra.beans.factory.annotation.Autowired;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.annotation.ImportResource;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.session.config.EnableSession;
import infra.test.context.junit.jupiter.web.JUnitWebConfig;
import infra.web.RequestContextHolder;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for scoped proxy use in conjunction with aop: namespace.
 * Deemed an integration test because .web mocks and application contexts are required.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see infra.aop.config.AopNamespaceHandlerTests
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

  @ImportResource("classpath:infra/aop/config/AopNamespaceHandlerScopeIntegrationTests-context.xml")
  @EnableSession
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
