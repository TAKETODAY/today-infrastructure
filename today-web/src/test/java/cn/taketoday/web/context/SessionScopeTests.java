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

package cn.taketoday.web.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.config.DestructionAwareBeanPostProcessor;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.context.support.SessionScope;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.beans.DerivedTestBean;
import cn.taketoday.web.testfixture.beans.TestBean;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see RequestScopeTests
 */
public class SessionScopeTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @BeforeEach
  public void setup() throws Exception {
    this.beanFactory.registerScope("session", new SessionScope(beanFactory));
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
    reader.loadBeanDefinitions(new ClassPathResource("sessionScopeTests.xml", getClass()));
  }

  @AfterEach
  public void reset() {
    RequestContextHolder.set(null);
  }

  @Test
  public void getFromScope() throws Exception {
    AtomicInteger count = new AtomicInteger();
    MockHttpSession session = new MockHttpSession() {
      @Override
      public void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        count.incrementAndGet();
      }
    };
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    ServletRequestContext requestAttributes = new ServletRequestContext(null, request, null);

    RequestContextHolder.set(requestAttributes);
    String name = "sessionScopedObject";
    assertThat(session.getAttribute(name)).isNull();
    TestBean bean = (TestBean) this.beanFactory.getBean(name);
    assertThat(count.intValue()).isEqualTo(1);
    assertThat(bean).isEqualTo(session.getAttribute(name));
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);
    assertThat(count.intValue()).isEqualTo(1);

    // should re-propagate updated attribute
    requestAttributes.requestCompleted();
    assertThat(bean).isEqualTo(session.getAttribute(name));
    assertThat(count.intValue()).isEqualTo(2);
  }

  @Test
  public void getFromScopeWithSingleAccess() throws Exception {
    AtomicInteger count = new AtomicInteger();
    MockHttpSession session = new MockHttpSession() {
      @Override
      public void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        count.incrementAndGet();
      }
    };
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    ServletRequestContext requestAttributes = new ServletRequestContext(null, request, null);

    RequestContextHolder.set(requestAttributes);
    String name = "sessionScopedObject";
    assertThat(session.getAttribute(name)).isNull();
    TestBean bean = (TestBean) this.beanFactory.getBean(name);
    assertThat(count.intValue()).isEqualTo(1);

    // should re-propagate updated attribute
    requestAttributes.requestCompleted();
    assertThat(bean).isEqualTo(session.getAttribute(name));
    assertThat(count.intValue()).isEqualTo(2);
  }

  @Test
  public void destructionAtSessionTermination() throws Exception {
    MockHttpSession session = new MockHttpSession();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    ServletRequestContext requestAttributes = new ServletRequestContext(null, request, null);

    RequestContextHolder.set(requestAttributes);
    String name = "sessionScopedDisposableObject";
    assertThat(session.getAttribute(name)).isNull();
    DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
    assertThat(bean).isEqualTo(session.getAttribute(name));
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

    requestAttributes.requestCompleted();
    session.invalidate();
    assertThat(bean.wasDestroyed()).isTrue();
  }

  @Test
  public void destructionWithSessionSerialization() throws Exception {
    doTestDestructionWithSessionSerialization(false);
  }

  @Test
  public void destructionWithSessionSerializationAndBeanPostProcessor() throws Exception {
    this.beanFactory.addBeanPostProcessor(new CustomDestructionAwareBeanPostProcessor());
    doTestDestructionWithSessionSerialization(false);
  }

  @Test
  public void destructionWithSessionSerializationAndSerializableBeanPostProcessor() throws Exception {
    this.beanFactory.addBeanPostProcessor(new CustomSerializableDestructionAwareBeanPostProcessor());
    doTestDestructionWithSessionSerialization(true);
  }

  private void doTestDestructionWithSessionSerialization(boolean beanNameReset) throws Exception {
    Serializable serializedState = null;

    MockHttpSession session = new MockHttpSession();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    ServletRequestContext requestAttributes = new ServletRequestContext(null, request, null);

    RequestContextHolder.set(requestAttributes);
    String name = "sessionScopedDisposableObject";
    assertThat(session.getAttribute(name)).isNull();
    DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
    assertThat(bean).isEqualTo(session.getAttribute(name));
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

    requestAttributes.requestCompleted();
    serializedState = session.serializeState();
    assertThat(bean.wasDestroyed()).isFalse();

    serializedState = serializeAndDeserialize(serializedState);

    session = new MockHttpSession();
    session.deserializeState(serializedState);
    request = new MockHttpServletRequest();
    request.setSession(session);
    requestAttributes = new ServletRequestContext(null, request, null);

    RequestContextHolder.set(requestAttributes);
    name = "sessionScopedDisposableObject";
    assertThat(session.getAttribute(name)).isNotNull();
    bean = (DerivedTestBean) this.beanFactory.getBean(name);
    assertThat(bean).isEqualTo(session.getAttribute(name));
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

    requestAttributes.requestCompleted();
    session.invalidate();
    assertThat(bean.wasDestroyed()).isTrue();

    if (beanNameReset) {
      assertThat(bean.getBeanName()).isNull();
    }
    else {
      assertThat(bean.getBeanName()).isNotNull();
    }
  }

  private static class CustomDestructionAwareBeanPostProcessor implements DestructionAwareBeanPostProcessor {

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
    }

    @Override
    public boolean requiresDestruction(Object bean) {
      return true;
    }
  }

  private static class CustomSerializableDestructionAwareBeanPostProcessor
          implements DestructionAwareBeanPostProcessor, Serializable {

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
      if (bean instanceof BeanNameAware) {
        ((BeanNameAware) bean).setBeanName(null);
      }
    }

    @Override
    public boolean requiresDestruction(Object bean) {
      return true;
    }
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
