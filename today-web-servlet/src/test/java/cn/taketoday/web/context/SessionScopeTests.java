/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.config.DestructionAwareBeanPostProcessor;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.core.Conventions;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.lang.Assert;
import cn.taketoday.session.CookieSessionIdResolver;
import cn.taketoday.session.MapSession;
import cn.taketoday.session.SessionRepository;
import cn.taketoday.session.WebSession;
import cn.taketoday.session.config.EnableWebSession;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.context.support.SessionScope;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.beans.testfixture.beans.DerivedTestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see RequestScopeTests
 */
public class SessionScopeTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(beanFactory);

  @BeforeEach
  public void setup() throws Exception {
    AnnotatedBeanDefinitionReader annotatedReader = new AnnotatedBeanDefinitionReader(beanFactory);
    annotatedReader.register(
            SessionConfig.class
    );

    this.beanFactory.registerScope("session", new SessionScope(beanFactory));
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
    reader.loadBeanDefinitions(new ClassPathResource("sessionScopeTests.xml", getClass()));

    context.refresh();
  }

  @AfterEach
  public void reset() {
    RequestContextHolder.set(null);
  }

  @Test
  public void getFromScope() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    ServletRequestContext requestAttributes = getContext(request);
    WebSession session = RequestContextUtils.getRequiredSession(requestAttributes);

    String name = "sessionScopedObject";
    assertThat(session.getAttribute(name)).isNull();
    TestBean bean = (TestBean) this.beanFactory.getBean(name);

    assertThat(session.getAttributes().size()).isEqualTo(1);
    assertThat(bean).isEqualTo(session.getAttribute(name));
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);
    assertThat(session.getAttributes().size()).isEqualTo(1);

    // should re-propagate updated attribute
    requestAttributes.requestCompleted();
    assertThat(bean).isEqualTo(session.getAttribute(name));
  }

  @Test
  public void destructionAtSessionTermination() throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();

    ServletRequestContext requestAttributes = getContext(request);
    WebSession session = RequestContextUtils.getSession(requestAttributes);
    String name = "sessionScopedDisposableObject";
    assertThat(session.getAttribute(name)).isNull();
    DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
    assertThat(bean).isEqualTo(session.getAttribute(name));
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

    requestAttributes.requestCompleted();
    session.invalidate();
    assertThat(bean.wasDestroyed()).isTrue();
  }

  private ServletRequestContext getContext(MockHttpServletRequest request) {
    ServletRequestContext requestAttributes = new ServletRequestContext(
            context, request, new MockHttpServletResponse());
    RequestContextHolder.set(requestAttributes);
    return requestAttributes;
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
    MockHttpServletRequest request = new MockHttpServletRequest();

    ServletRequestContext requestAttributes = getContext(request);
    WebSession session = RequestContextUtils.getRequiredSession(requestAttributes);
    String name = "sessionScopedDisposableObject";
    assertThat(session.getAttribute(name)).isNull();
    DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
    assertThat(bean).isEqualTo(session.getAttribute(name));
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

    requestAttributes.requestCompleted();
    serializedState = serializeState(session);
    assertThat(bean.wasDestroyed()).isFalse();

    serializedState = serializeAndDeserialize(serializedState);

    SessionRepository repository = beanFactory.getBean(SessionRepository.class);

    DirectFieldAccessor beanWrapper = BeanWrapper.forDirectFieldAccess(repository);
    ConcurrentHashMap<String, WebSession> sessions = (ConcurrentHashMap<String, WebSession>)
            beanWrapper.getPropertyValue("sessions");

    session = new MapSession();
    deserializeState(session, serializedState);

    WebSession finalSession = session;
    sessions.replaceAll((s, webSession) -> finalSession);
    request = new MockHttpServletRequest();

    requestAttributes = getContext(request);

    requestAttributes.setAttribute(Conventions.getQualifiedAttributeName(
            CookieSessionIdResolver.class, "WRITTEN_SESSION_ID_ATTR"), session.getId());

    name = "sessionScopedDisposableObject";
    assertThat(session.getAttribute(name)).isNotNull();
    bean = (DerivedTestBean) this.beanFactory.getBean(name);
    assertThat(bean).isEqualTo(session.getAttribute(name));
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

    requestAttributes.requestCompleted();
    RequestContextUtils.getRequiredSession(requestAttributes).invalidate();
    assertThat(bean.wasDestroyed()).isTrue();

    if (beanNameReset) {
      assertThat(bean.getBeanName()).isNull();
    }
    else {
      assertThat(bean.getBeanName()).isNotNull();
    }
  }

  private void deserializeState(WebSession session, Serializable state) {
    Assert.isTrue(state instanceof Map, "Serialized state needs to be of type [java.util.Map]");
    session.getAttributes().putAll((Map<String, Object>) state);
  }

  private Serializable serializeState(WebSession session) {
    HashMap<String, Serializable> state = new HashMap<>();
    for (Iterator<Map.Entry<String, Object>> it = session.getAttributes().entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String, Object> entry = it.next();
      String name = entry.getKey();
      Object value = entry.getValue();
      it.remove();
      if (value instanceof Serializable) {
        state.put(name, (Serializable) value);
      }
    }
    return state;
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

  @SuppressWarnings("serial")
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

  @EnableWebSession
  static class SessionConfig {

  }

}
