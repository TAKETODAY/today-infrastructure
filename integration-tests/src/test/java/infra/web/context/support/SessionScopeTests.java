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

package infra.web.context.support;

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
import java.util.function.Supplier;

import infra.beans.BeanWrapper;
import infra.beans.BeansException;
import infra.beans.DirectFieldAccessor;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanNameAware;
import infra.beans.factory.config.DestructionAwareBeanPostProcessor;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.DerivedTestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.core.Conventions;
import infra.core.io.ClassPathResource;
import infra.lang.Assert;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.session.CookieSessionIdResolver;
import infra.session.MapSession;
import infra.session.Session;
import infra.session.SessionAttributeListener;
import infra.session.SessionRepository;
import infra.session.config.EnableSession;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.RequestContextUtils;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = getContext(request);
    Session session = RequestContextUtils.getRequiredSession(requestAttributes);

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

    HttpMockRequestImpl request = new HttpMockRequestImpl();

    MockRequestContext requestAttributes = getContext(request);
    Session session = RequestContextUtils.getSession(requestAttributes);
    String name = "sessionScopedDisposableObject";
    assertThat(session.getAttribute(name)).isNull();
    DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
    assertThat(bean).isEqualTo(session.getAttribute(name));
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

    requestAttributes.requestCompleted();
    session.invalidate();
    assertThat(bean.wasDestroyed()).isTrue();
  }

  private MockRequestContext getContext(HttpMockRequestImpl request) {
    MockRequestContext requestAttributes = new MockRequestContext(
            context, request, new MockHttpResponseImpl());
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

  @Test
  void getConversationIdReturnsNullWhenNoSession() {
    RequestContextHolder.set(new MockRequestContext(context, new HttpMockRequestImpl(), new MockHttpResponseImpl()));

    SessionScope sessionScope = new SessionScope(beanFactory);
    String conversationId = sessionScope.getConversationId();

    assertThat(conversationId).isNull();
  }

  @Test
  void resolveContextualObjectWithRequestKey() {
    MockRequestContext requestContext = new MockRequestContext(context, new HttpMockRequestImpl(), new MockHttpResponseImpl());
    RequestContextHolder.set(requestContext);

    SessionScope sessionScope = new SessionScope(beanFactory);
    Object result = sessionScope.resolveContextualObject(RequestContext.SCOPE_REQUEST);

    assertThat(result).isSameAs(requestContext);
  }

  @Test
  void resolveContextualObjectWithSessionKey() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = getContext(request);
    Session session = RequestContextUtils.getRequiredSession(requestAttributes);

    SessionScope sessionScope = new SessionScope(beanFactory);
    Object result = sessionScope.resolveContextualObject(RequestContext.SCOPE_SESSION);

    assertThat(result).isSameAs(session);
  }

  @Test
  void resolveContextualObjectWithInvalidKey() {
    RequestContextHolder.set(new MockRequestContext(context, new HttpMockRequestImpl(), new MockHttpResponseImpl()));

    SessionScope sessionScope = new SessionScope(beanFactory);
    Object result = sessionScope.resolveContextualObject("invalidKey");

    assertThat(result).isNull();
  }

  @Test
  void resolveContextualObjectWithSessionKeyAndNoRequestContext() {
    RequestContextHolder.set(null);

    SessionScope sessionScope = new SessionScope(beanFactory);
    Object result = sessionScope.resolveContextualObject(RequestContext.SCOPE_SESSION);

    assertThat(result).isNull();
  }

  @Test
  void registerDestructionCallbackStoresCallback() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = getContext(request);
    Session session = RequestContextUtils.getRequiredSession(requestAttributes);

    SessionScope sessionScope = new SessionScope(beanFactory);
    Runnable callback = mock(Runnable.class);

    sessionScope.registerDestructionCallback("testBean", callback);

    String destructionCallbackName = SessionScope.DESTRUCTION_CALLBACK_NAME_PREFIX + "testBean";
    assertThat(session.getAttribute(destructionCallbackName)).isNotNull();
  }

  @Test
  void createDestructionCallbackReturnsSessionAttributeListener() {
    SessionAttributeListener listener = SessionScope.createDestructionCallback();

    assertThat(listener).isInstanceOf(SessionScope.DestructionCallback.class);
  }

  @Test
  void destructionCallbackAttributeRemoved() {
    Session session = mock(Session.class);
    String attributeName = "testAttribute";
    Object attributeValue = new Object();

    SessionScope.DestructionCallback destructionCallback = new SessionScope.DestructionCallback();
    destructionCallback.attributeRemoved(session, attributeName, attributeValue);

    // Verification would require checking interactions, but the method mainly handles cleanup logic
    assertThatCode(() -> destructionCallback.attributeRemoved(session, attributeName, attributeValue))
            .doesNotThrowAnyException();
  }

  @Test
  void constructorInitializesSessionManagerDiscover() {
    SessionScope sessionScope = new SessionScope(mock(BeanFactory.class));

    assertThat(sessionScope).isNotNull();
  }

  @Test
  void getWithObjectFactoryCreatesAndStoresBean() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = getContext(request);
    RequestContextHolder.set(requestAttributes);

    SessionScope sessionScope = new SessionScope(beanFactory);
    String beanName = "testBean";
    Supplier<Object> objectFactory = mock(Supplier.class);
    Object beanInstance = new Object();
    given(objectFactory.get()).willReturn(beanInstance);

    Object result = sessionScope.get(beanName, objectFactory);

    assertThat(result).isSameAs(beanInstance);
    verify(objectFactory).get();
  }

  @Test
  void getReturnsExistingBeanFromSession() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = getContext(request);
    RequestContextHolder.set(requestAttributes);

    SessionScope sessionScope = new SessionScope(beanFactory);
    String beanName = "testBean";
    Object beanInstance = new Object();

    // Put bean in session manually
    Session session = RequestContextUtils.getRequiredSession(requestAttributes);
    session.setAttribute(beanName, beanInstance);

    Supplier<Object> objectFactory = mock(Supplier.class);
    Object result = sessionScope.get(beanName, objectFactory);

    assertThat(result).isSameAs(beanInstance);
    verify(objectFactory, never()).get();
  }

  @Test
  void removeReturnsAndRemovesBeanFromSession() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = getContext(request);
    RequestContextHolder.set(requestAttributes);

    SessionScope sessionScope = new SessionScope(beanFactory);
    String beanName = "testBean";
    Object beanInstance = new Object();

    // Put bean in session manually
    Session session = RequestContextUtils.getRequiredSession(requestAttributes);
    session.setAttribute(beanName, beanInstance);

    Object result = sessionScope.remove(beanName);

    assertThat(result).isSameAs(beanInstance);
    assertThat(session.getAttribute(beanName)).isNull();
  }

  @Test
  void removeReturnsNullWhenNoSession() {
    RequestContextHolder.set(null);

    SessionScope sessionScope = new SessionScope(beanFactory);
    Object result = sessionScope.remove("testBean");

    assertThat(result).isNull();
  }

  @Test
  void setAttributeStoresAttributeInSession() {
    Session session = mock(Session.class);
    String attributeName = "testAttribute";
    Object attributeValue = new Object();

    SessionScope sessionScope = new SessionScope(beanFactory);
    sessionScope.setAttribute(session, attributeName, attributeValue);

    verify(session).setAttribute(attributeName, attributeValue);
  }

  @Test
  void getAttributeReturnsAttributeFromSession() {
    Session session = mock(Session.class);
    String attributeName = "testAttribute";
    Object attributeValue = new Object();
    given(session.getAttribute(attributeName)).willReturn(attributeValue);

    SessionScope sessionScope = new SessionScope(beanFactory);
    Object result = sessionScope.getAttribute(session, attributeName);

    assertThat(result).isSameAs(attributeValue);
    verify(session).getAttribute(attributeName);
  }

  @Test
  void removeAttributeRemovesAttributeFromSession() {
    Session session = mock(Session.class);
    String attributeName = "testAttribute";

    SessionScope sessionScope = new SessionScope(beanFactory);
    sessionScope.removeAttribute(session, attributeName);

    verify(session).removeAttribute(attributeName);
  }

  @Test
  void getDestructionCallbackNameReturnsCorrectName() {
    String beanName = "testBean";
    String expectedName = SessionScope.DESTRUCTION_CALLBACK_NAME_PREFIX + beanName;

    String result = SessionScope.getDestructionCallbackName(beanName);

    assertThat(result).isEqualTo(expectedName);
  }

  private void doTestDestructionWithSessionSerialization(boolean beanNameReset) throws Exception {
    Serializable serializedState = null;
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    MockRequestContext requestAttributes = getContext(request);
    Session session = RequestContextUtils.getRequiredSession(requestAttributes);
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
    ConcurrentHashMap<String, Session> sessions = (ConcurrentHashMap<String, Session>)
            beanWrapper.getPropertyValue("sessions");

    session = new MapSession();
    deserializeState(session, serializedState);

    Session finalSession = session;
    sessions.replaceAll((s, webSession) -> finalSession);
    request = new HttpMockRequestImpl();

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

  private void deserializeState(Session session, Serializable state) {
    Assert.isTrue(state instanceof Map, "Serialized state needs to be of type [java.util.Map]");
    session.getAttributes().putAll((Map<String, Object>) state);
  }

  private Serializable serializeState(Session session) {
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

  @EnableSession
  static class SessionConfig {

  }

}
