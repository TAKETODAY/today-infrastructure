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

package cn.taketoday.context.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.PayloadApplicationEvent;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.context.annotation.ScopedProxyMode;
import cn.taketoday.context.event.test.AbstractIdentifiable;
import cn.taketoday.context.event.test.AnotherTestEvent;
import cn.taketoday.context.event.test.EventCollector;
import cn.taketoday.context.event.test.GenericEventPojo;
import cn.taketoday.context.event.test.Identifiable;
import cn.taketoday.context.event.test.TestEvent;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.lang.Assert;
import cn.taketoday.scheduling.annotation.Async;
import cn.taketoday.scheduling.annotation.EnableAsync;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.concurrent.DefaultFuture;
import cn.taketoday.util.concurrent.SettableFuture;
import cn.taketoday.validation.annotation.Validated;
import cn.taketoday.validation.beanvalidation.MethodValidationPostProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
class AnnotationDrivenEventListenerTests {

  private ConfigurableApplicationContext context;

  private EventCollector eventCollector;

  private CountDownLatch countDownLatch;  // 1 call by default

  @AfterEach
  public void closeContext() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void simpleEventJavaConfig() {
    load(TestEventListener.class);
    TestEvent event = new TestEvent(this, "test");
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(listener, event);
    this.eventCollector.assertTotalEventsCount(1);

    this.eventCollector.clear();
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(listener, event);
    this.eventCollector.assertTotalEventsCount(1);

    context.getBean(ApplicationEventMulticaster.class).removeApplicationListeners(l ->
            l instanceof SmartApplicationListener sal && sal.getListenerId().contains("TestEvent"));
    this.eventCollector.clear();
    this.context.publishEvent(event);
    this.eventCollector.assertNoEventReceived(listener);
  }

  @Test
  void simpleEventXmlConfig() {
    this.context = new ClassPathXmlApplicationContext(
            "cn/taketoday/context/event/simple-event-configuration.xml");

    TestEvent event = new TestEvent(this, "test");
    TestEventListener listener = this.context.getBean(TestEventListener.class);
    this.eventCollector = getEventCollector(this.context);

    this.eventCollector.assertNoEventReceived(listener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(listener, event);
    this.eventCollector.assertTotalEventsCount(1);

    context.getBean(ApplicationEventMulticaster.class).removeApplicationListeners(l ->
            l instanceof SmartApplicationListener sal && sal.getListenerId().contains("TestEvent"));
    this.eventCollector.clear();
    this.context.publishEvent(event);
    this.eventCollector.assertNoEventReceived(listener);
  }

  @Test
  void metaAnnotationIsDiscovered() {
    load(MetaAnnotationListenerTestBean.class);
    MetaAnnotationListenerTestBean bean = this.context.getBean(MetaAnnotationListenerTestBean.class);
    this.eventCollector.assertNoEventReceived(bean);

    TestEvent event = new TestEvent();
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(bean, event);
    this.eventCollector.assertTotalEventsCount(1);

    context.getBean(ApplicationEventMulticaster.class).removeApplicationListeners(l ->
            l instanceof SmartApplicationListener sal && sal.getListenerId().equals("foo"));
    this.eventCollector.clear();
    this.context.publishEvent(event);
    this.eventCollector.assertNoEventReceived(bean);
  }

  @Test
  void contextEventsAreReceived() {
    load(ContextEventListener.class);
    ContextEventListener listener = this.context.getBean(ContextEventListener.class);

    List<Object> events = this.eventCollector.getEvents(listener);
    assertThat(events).as("Wrong number of initial context events").hasSize(1);
    assertThat(events.get(0).getClass()).isEqualTo(ContextRefreshedEvent.class);

    this.context.start();
    List<Object> eventsAfterStart = this.eventCollector.getEvents(listener);
    assertThat(eventsAfterStart).as("Wrong number of context events on start").hasSize(2);
    assertThat(eventsAfterStart.get(1).getClass()).isEqualTo(ContextStartedEvent.class);
    this.eventCollector.assertTotalEventsCount(2);

    this.context.stop();
    List<Object> eventsAfterStop = this.eventCollector.getEvents(listener);
    assertThat(eventsAfterStop).as("Wrong number of context events on stop").hasSize(3);
    assertThat(eventsAfterStop.get(2).getClass()).isEqualTo(ContextStoppedEvent.class);
    this.eventCollector.assertTotalEventsCount(3);

    this.context.close();
    List<Object> eventsAfterClose = this.eventCollector.getEvents(listener);
    assertThat(eventsAfterClose).as("Wrong number of context events on close").hasSize(4);
    assertThat(eventsAfterClose.get(3).getClass()).isEqualTo(ContextClosedEvent.class);
    this.eventCollector.assertTotalEventsCount(4);

    // Further events are supposed to be ignored after context close
    this.context.publishEvent(new ContextClosedEvent(this.context));
    this.eventCollector.assertTotalEventsCount(4);
  }

  @Test
  void methodSignatureNoEvent() {
    @SuppressWarnings("resource")
    AnnotationConfigApplicationContext failingContext =
            new AnnotationConfigApplicationContext();
    failingContext.register(BasicConfiguration.class,
            InvalidMethodSignatureEventListener.class);

    assertThatExceptionOfType(BeanInitializationException.class).isThrownBy(() ->
                    failingContext.refresh())
            .withMessageContaining(InvalidMethodSignatureEventListener.class.getName())
            .withMessageContaining("cannotBeCalled");
  }

  @Test
  void simpleReply() {
    load(TestEventListener.class, ReplyEventListener.class);
    AnotherTestEvent event = new AnotherTestEvent(this, "dummy");
    ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertNoEventReceived(replyEventListener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(replyEventListener, event);
    this.eventCollector.assertEvent(listener, new TestEvent(replyEventListener, event.getId(), "dummy")); // reply
    this.eventCollector.assertTotalEventsCount(2);
  }

  @Test
  void nullReplyIgnored() {
    load(TestEventListener.class, ReplyEventListener.class);
    AnotherTestEvent event = new AnotherTestEvent(this, null); // No response
    ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertNoEventReceived(replyEventListener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(replyEventListener, event);
    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void arrayReply() {
    load(TestEventListener.class, ReplyEventListener.class);
    AnotherTestEvent event = new AnotherTestEvent(this, new String[] { "first", "second" });
    ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertNoEventReceived(replyEventListener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(replyEventListener, event);
    this.eventCollector.assertEvent(listener, "first", "second"); // reply
    this.eventCollector.assertTotalEventsCount(3);
  }

  @Test
  void collectionReply() {
    load(TestEventListener.class, ReplyEventListener.class);
    Set<Object> replies = new LinkedHashSet<>();
    replies.add("first");
    replies.add(4L);
    replies.add("third");
    AnotherTestEvent event = new AnotherTestEvent(this, replies);
    ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertNoEventReceived(replyEventListener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(replyEventListener, event);
    this.eventCollector.assertEvent(listener, "first", "third"); // reply (no listener for 4L)
    this.eventCollector.assertTotalEventsCount(3);
  }

  @Test
  void collectionReplyNullValue() {
    load(TestEventListener.class, ReplyEventListener.class);
    AnotherTestEvent event = new AnotherTestEvent(this, Arrays.asList(null, "test"));
    ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertNoEventReceived(replyEventListener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(replyEventListener, event);
    this.eventCollector.assertEvent(listener, "test");
    this.eventCollector.assertTotalEventsCount(2);
  }

  @Test
  void listenableFutureReply() {
    load(TestEventListener.class, ReplyEventListener.class);
    SettableFuture<String> future = new DefaultFuture<>();
    future.setSuccess("dummy");
    AnotherTestEvent event = new AnotherTestEvent(this, future);
    ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertNoEventReceived(replyEventListener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(replyEventListener, event);
    this.eventCollector.assertEvent(listener, "dummy"); // reply
    this.eventCollector.assertTotalEventsCount(2);
  }

  @Test
  void completableFutureReply() {
    load(TestEventListener.class, ReplyEventListener.class);
    AnotherTestEvent event = new AnotherTestEvent(this, CompletableFuture.completedFuture("dummy"));
    ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertNoEventReceived(replyEventListener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(replyEventListener, event);
    this.eventCollector.assertEvent(listener, "dummy"); // reply
    this.eventCollector.assertTotalEventsCount(2);
  }

  @Test
  void monoReply() {
    load(TestEventListener.class, ReplyEventListener.class);
    AnotherTestEvent event = new AnotherTestEvent(this, Mono.just("dummy"));
    ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertNoEventReceived(replyEventListener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(replyEventListener, event);
    this.eventCollector.assertEvent(listener, "dummy"); // reply
    this.eventCollector.assertTotalEventsCount(2);
  }

  @Test
  void fluxReply() {
    load(TestEventListener.class, ReplyEventListener.class);
    AnotherTestEvent event = new AnotherTestEvent(this, Flux.just("dummy1", "dummy2"));
    ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertNoEventReceived(replyEventListener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(replyEventListener, event);
    this.eventCollector.assertEvent(listener, "dummy1", "dummy2"); // reply
    this.eventCollector.assertTotalEventsCount(3);
  }

  @Test
  void eventListenerWorksWithSimpleInterfaceProxy() {
    load(ScopedProxyTestBean.class);

    SimpleService proxy = this.context.getBean(SimpleService.class);
    assertThat(proxy).as("bean should be a proxy").isInstanceOf(Advised.class);
    this.eventCollector.assertNoEventReceived(proxy.getId());

    this.context.publishEvent(new ContextRefreshedEvent(this.context));
    this.eventCollector.assertNoEventReceived(proxy.getId());

    TestEvent event = new TestEvent();
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(proxy.getId(), event);
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void eventListenerWorksWithAnnotatedInterfaceProxy() {
    load(AnnotatedProxyTestBean.class);

    AnnotatedSimpleService proxy = this.context.getBean(AnnotatedSimpleService.class);
    assertThat(proxy).as("bean should be a proxy").isInstanceOf(Advised.class);
    this.eventCollector.assertNoEventReceived(proxy.getId());

    this.context.publishEvent(new ContextRefreshedEvent(this.context));
    this.eventCollector.assertNoEventReceived(proxy.getId());

    TestEvent event = new TestEvent();
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(proxy.getId(), event);
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void eventListenerWorksWithCglibProxy() {
    load(CglibProxyTestBean.class);

    CglibProxyTestBean proxy = this.context.getBean(CglibProxyTestBean.class);
    assertThat(AopUtils.isCglibProxy(proxy)).as("bean should be a cglib proxy").isTrue();
    this.eventCollector.assertNoEventReceived(proxy.getId());

    this.context.publishEvent(new ContextRefreshedEvent(this.context));
    this.eventCollector.assertNoEventReceived(proxy.getId());

    TestEvent event = new TestEvent();
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(proxy.getId(), event);
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void privateMethodOnCglibProxyFails() {
    assertThatExceptionOfType(BeanInitializationException.class).isThrownBy(() ->
                    load(CglibProxyWithPrivateMethod.class))
            .withCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void eventListenerWorksWithCustomScope() {
    load(CustomScopeTestBean.class);
    CustomScope customScope = new CustomScope();
    this.context.getBeanFactory().registerScope("custom", customScope);

    CustomScopeTestBean proxy = this.context.getBean(CustomScopeTestBean.class);
    assertThat(AopUtils.isCglibProxy(proxy)).as("bean should be a cglib proxy").isTrue();
    this.eventCollector.assertNoEventReceived(proxy.getId());

    this.context.publishEvent(new ContextRefreshedEvent(this.context));
    this.eventCollector.assertNoEventReceived(proxy.getId());

    customScope.active = false;
    this.context.publishEvent(new ContextRefreshedEvent(this.context));
    customScope.active = true;
    this.eventCollector.assertNoEventReceived(proxy.getId());

    TestEvent event = new TestEvent();
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(proxy.getId(), event);
    this.eventCollector.assertTotalEventsCount(1);

    customScope.active = false;
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    this.context.publishEvent(new TestEvent()))
            .withCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void asyncProcessingApplied() throws InterruptedException {
    loadAsync(AsyncEventListener.class);

    String threadName = Thread.currentThread().getName();
    AnotherTestEvent event = new AnotherTestEvent(this, threadName);
    AsyncEventListener listener = this.context.getBean(AsyncEventListener.class);
    this.eventCollector.assertNoEventReceived(listener);

    this.context.publishEvent(event);
    this.countDownLatch.await(2, TimeUnit.SECONDS);
    this.eventCollector.assertEvent(listener, event);
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void asyncProcessingAppliedWithInterfaceProxy() throws InterruptedException {
    doLoad(AsyncConfigurationWithInterfaces.class, SimpleProxyTestBean.class);

    String threadName = Thread.currentThread().getName();
    AnotherTestEvent event = new AnotherTestEvent(this, threadName);
    SimpleService listener = this.context.getBean(SimpleService.class);
    this.eventCollector.assertNoEventReceived(listener);

    this.context.publishEvent(event);
    this.countDownLatch.await(2, TimeUnit.SECONDS);
    this.eventCollector.assertEvent(listener, event);
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void asyncProcessingAppliedWithScopedProxy() throws InterruptedException {
    doLoad(AsyncConfigurationWithInterfaces.class, ScopedProxyTestBean.class);

    String threadName = Thread.currentThread().getName();
    AnotherTestEvent event = new AnotherTestEvent(this, threadName);
    SimpleService listener = this.context.getBean(SimpleService.class);
    this.eventCollector.assertNoEventReceived(listener);

    this.context.publishEvent(event);
    this.countDownLatch.await(2, TimeUnit.SECONDS);
    this.eventCollector.assertEvent(listener, event);
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void exceptionPropagated() {
    load(ExceptionEventListener.class);
    TestEvent event = new TestEvent(this, "fail");
    ExceptionEventListener listener = this.context.getBean(ExceptionEventListener.class);
    this.eventCollector.assertNoEventReceived(listener);
    assertThatIllegalStateException().isThrownBy(() ->
                    this.context.publishEvent(event))
            .withMessage("Test exception");
    this.eventCollector.assertEvent(listener, event);
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void exceptionNotPropagatedWithAsync() throws InterruptedException {
    loadAsync(ExceptionEventListener.class);
    AnotherTestEvent event = new AnotherTestEvent(this, "fail");
    ExceptionEventListener listener = this.context.getBean(ExceptionEventListener.class);
    this.eventCollector.assertNoEventReceived(listener);

    this.context.publishEvent(event);
    this.countDownLatch.await(2, TimeUnit.SECONDS);

    this.eventCollector.assertEvent(listener, event);
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void listenerWithSimplePayload() {
    load(TestEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.context.publishEvent("test");
    this.eventCollector.assertEvent(listener, "test");
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void listenerWithNonMatchingPayload() {
    load(TestEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.context.publishEvent(123L);
    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertTotalEventsCount(0);
  }

  @Test
  void replyWithPayload() {
    load(TestEventListener.class, ReplyEventListener.class);
    AnotherTestEvent event = new AnotherTestEvent(this, "String");
    ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
    TestEventListener listener = this.context.getBean(TestEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertNoEventReceived(replyEventListener);
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(replyEventListener, event);
    this.eventCollector.assertEvent(listener, "String"); // reply
    this.eventCollector.assertTotalEventsCount(2);
  }

  @Test
  void listenerWithGenericApplicationEvent() {
    load(GenericEventListener.class);
    GenericEventListener listener = this.context.getBean(GenericEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.context.publishEvent("TEST");
    this.eventCollector.assertEvent(listener, "TEST");
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void listenerWithResolvableTypeEvent() {
    load(ResolvableTypeEventListener.class);
    ResolvableTypeEventListener listener = this.context.getBean(ResolvableTypeEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    GenericEventPojo<String> event = new GenericEventPojo<>("TEST");
    this.context.publishEvent(event);
    this.eventCollector.assertEvent(listener, event);
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  void listenerWithResolvableTypeEventWrongGeneric() {
    load(ResolvableTypeEventListener.class);
    ResolvableTypeEventListener listener = this.context.getBean(ResolvableTypeEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    GenericEventPojo<Long> event = new GenericEventPojo<>(123L);
    this.context.publishEvent(event);
    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertTotalEventsCount(0);
  }

  @Test
  void conditionMatch() {
    validateConditionMatch(ConditionalEventListener.class);
  }

  @Test
  void conditionMatchWithProxy() {
    validateConditionMatch(ConditionalEventListener.class, MethodValidationPostProcessor.class);
  }

  private void validateConditionMatch(Class<?>... classes) {
    long timestamp = System.currentTimeMillis();
    load(classes);
    TestEvent event = new TestEvent(this, "OK");

    ConditionalEventInterface listener = this.context.getBean(ConditionalEventInterface.class);
    this.eventCollector.assertNoEventReceived(listener);

    this.context.publishEvent(event);
    this.eventCollector.assertEvent(listener, event);
    this.eventCollector.assertTotalEventsCount(1);

    this.context.publishEvent("OK");
    this.eventCollector.assertEvent(listener, event, "OK");
    this.eventCollector.assertTotalEventsCount(2);

    this.context.publishEvent("NOT OK");
    this.eventCollector.assertTotalEventsCount(2);

    this.context.publishEvent(timestamp);
    this.eventCollector.assertEvent(listener, event, "OK", timestamp);
    this.eventCollector.assertTotalEventsCount(3);

    this.context.publishEvent(42d);
    this.eventCollector.assertEvent(listener, event, "OK", timestamp, 42d);
    this.eventCollector.assertTotalEventsCount(4);
  }

  @Test
  void conditionDoesNotMatch() {
    long maxLong = Long.MAX_VALUE;
    load(ConditionalEventListener.class);
    TestEvent event = new TestEvent(this, "KO");
    TestEventListener listener = this.context.getBean(ConditionalEventListener.class);
    this.eventCollector.assertNoEventReceived(listener);

    this.context.publishEvent(event);
    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertTotalEventsCount(0);

    this.context.publishEvent("KO");
    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertTotalEventsCount(0);

    this.context.publishEvent(maxLong);
    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertTotalEventsCount(0);

    this.context.publishEvent(24d);
    this.eventCollector.assertNoEventReceived(listener);
    this.eventCollector.assertTotalEventsCount(0);
  }

  @Test
  void orderedListeners() {
    load(OrderedTestListener.class);
    OrderedTestListener listener = this.context.getBean(OrderedTestListener.class);

    assertThat(listener.order.isEmpty()).isTrue();
    this.context.publishEvent("whatever");
    assertThat(listener.order).contains("first", "second", "third");
  }

  @Test
  void publicSubclassWithInheritedEventListener() {
    load(PublicSubclassWithInheritedEventListener.class);
    TestEventListener listener = this.context.getBean(PublicSubclassWithInheritedEventListener.class);

    this.eventCollector.assertNoEventReceived(listener);
    this.context.publishEvent("test");
    this.eventCollector.assertEvent(listener, "test");
    this.eventCollector.assertTotalEventsCount(1);
  }

  @Test
  @Disabled
  void listenersReceiveEarlyEvents() {
    load(EventOnPostConstruct.class, OrderedTestListener.class);
    OrderedTestListener listener = this.context.getBean(OrderedTestListener.class);

    assertThat(listener.order).contains("first", "second", "third");
  }

  @Test
  void missingListenerBeanIgnored() {
    load(MissingEventListener.class);
    context.getBean(UseMissingEventListener.class);
    context.publishEvent(new TestEvent(this));
  }

  private void load(Class<?>... classes) {
    List<Class<?>> allClasses = new ArrayList<>();
    allClasses.add(BasicConfiguration.class);
    allClasses.addAll(Arrays.asList(classes));
    doLoad(allClasses.toArray(new Class<?>[allClasses.size()]));
  }

  private void loadAsync(Class<?>... classes) {
    List<Class<?>> allClasses = new ArrayList<>();
    allClasses.add(AsyncConfiguration.class);
    allClasses.addAll(Arrays.asList(classes));
    doLoad(allClasses.toArray(new Class<?>[allClasses.size()]));
  }

  private void doLoad(Class<?>... classes) {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(classes);
    this.eventCollector = ctx.getBean(EventCollector.class);
    this.countDownLatch = ctx.getBean(CountDownLatch.class);
    this.context = ctx;
  }

  private EventCollector getEventCollector(ConfigurableApplicationContext context) {
    return context.getBean(EventCollector.class);
  }

  @Configuration
  static class BasicConfiguration {

    @Bean
    public EventCollector eventCollector() {
      return new EventCollector();
    }

    @Bean
    public CountDownLatch testCountDownLatch() {
      return new CountDownLatch(1);
    }

    @Bean
    public TestConditionEvaluator conditionEvaluator() {
      return new TestConditionEvaluator();
    }

    static class TestConditionEvaluator {

      public boolean valid(Double ratio) {
        return Double.valueOf(42).equals(ratio);
      }
    }
  }

  static abstract class AbstractTestEventListener extends AbstractIdentifiable {

    @Autowired
    private EventCollector eventCollector;

    protected void collectEvent(Object content) {
      this.eventCollector.addEvent(this, content);
    }
  }

  @Component
  static class TestEventListener extends AbstractTestEventListener {

    @EventListener
    public void handle(TestEvent event) {
      collectEvent(event);
    }

    @EventListener
    public void handleString(String content) {
      collectEvent(content);
    }
  }

  @EventListener(id = "foo")
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface FooListener {
  }

  @Component
  static class MetaAnnotationListenerTestBean extends AbstractTestEventListener {

    @FooListener
    public void handleIt(TestEvent event) {
      collectEvent(event);
    }
  }

  @Component
  static class ContextEventListener extends AbstractTestEventListener {

    @EventListener
    public void handleContextEvent(ApplicationContextEvent event) {
      collectEvent(event);
    }

  }

  @Component
  static class InvalidMethodSignatureEventListener {

    @EventListener
    public void cannotBeCalled(String s, Integer what) {
    }
  }

  @Component
  static class ReplyEventListener extends AbstractTestEventListener {

    @EventListener
    public Object handle(AnotherTestEvent event) {
      collectEvent(event);
      if (event.content == null) {
        return null;
      }
      else if (event.content instanceof String s) {
        if (s.equals("String")) {
          return event.content;
        }
        else {
          return new TestEvent(this, event.getId(), s);
        }
      }
      return event.content;
    }
  }

  @Component
  static class ExceptionEventListener extends AbstractTestEventListener {

    @Autowired
    private CountDownLatch countDownLatch;

    @EventListener
    public void handle(TestEvent event) {
      collectEvent(event);
      if ("fail".equals(event.msg)) {
        throw new IllegalStateException("Test exception");
      }
    }

    @EventListener
    @Async
    public void handleAsync(AnotherTestEvent event) {
      collectEvent(event);
      if ("fail".equals(event.content)) {
        this.countDownLatch.countDown();
        throw new IllegalStateException("Test exception");
      }
    }
  }

  @Component
  static class AsyncEventListener extends AbstractTestEventListener {

    @Autowired
    private CountDownLatch countDownLatch;

    @EventListener
    @Async
    public void handleAsync(AnotherTestEvent event) {
      assertThat(Thread.currentThread().getName()).isNotEqualTo(event.content);
      collectEvent(event);
      this.countDownLatch.countDown();
    }
  }

  @Configuration
  @Import(BasicConfiguration.class)
  @EnableAsync(proxyTargetClass = true)
  static class AsyncConfiguration {
  }

  @Configuration
  @Import(BasicConfiguration.class)
  @EnableAsync(proxyTargetClass = false)
  static class AsyncConfigurationWithInterfaces {
  }

  interface SimpleService extends Identifiable {

    void handleIt(TestEvent event);

    void handleAsync(AnotherTestEvent event);
  }

  @Component
  static class SimpleProxyTestBean extends AbstractIdentifiable implements SimpleService {

    @Autowired
    private EventCollector eventCollector;

    @Autowired
    private CountDownLatch countDownLatch;

    @EventListener
    @Override
    public void handleIt(TestEvent event) {
      this.eventCollector.addEvent(this, event);
    }

    @EventListener
    @Async
    @Override
    public void handleAsync(AnotherTestEvent event) {
      assertThat(Thread.currentThread().getName()).isNotEqualTo(event.content);
      this.eventCollector.addEvent(this, event);
      this.countDownLatch.countDown();
    }
  }

  @Component
  @Scope(proxyMode = ScopedProxyMode.INTERFACES)
  static class ScopedProxyTestBean extends AbstractIdentifiable implements SimpleService {

    @Autowired
    private EventCollector eventCollector;

    @Autowired
    private CountDownLatch countDownLatch;

    @EventListener
    @Override
    public void handleIt(TestEvent event) {
      this.eventCollector.addEvent(this, event);
    }

    @EventListener
    @Async
    @Override
    public void handleAsync(AnotherTestEvent event) {
      assertThat(Thread.currentThread().getName()).isNotEqualTo(event.content);
      this.eventCollector.addEvent(this, event);
      this.countDownLatch.countDown();
    }
  }

  interface AnnotatedSimpleService extends Identifiable {

    @EventListener
    void handleIt(TestEvent event);
  }

  @Component
  @Scope(proxyMode = ScopedProxyMode.INTERFACES)
  static class AnnotatedProxyTestBean extends AbstractIdentifiable implements AnnotatedSimpleService {

    @Autowired
    private EventCollector eventCollector;

    @Override
    public void handleIt(TestEvent event) {
      this.eventCollector.addEvent(this, event);
    }
  }

  @Component
  @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
  static class CglibProxyTestBean extends AbstractTestEventListener {

    @EventListener
    public void handleIt(TestEvent event) {
      collectEvent(event);
    }
  }

  @Component
  @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
  static class CglibProxyWithPrivateMethod extends AbstractTestEventListener {

    @EventListener
    private void handleIt(TestEvent event) {
      collectEvent(event);
    }
  }

  @Component
  @Scope(scopeName = "custom", proxyMode = ScopedProxyMode.TARGET_CLASS)
  static class CustomScopeTestBean extends AbstractTestEventListener {

    @EventListener
    public void handleIt(TestEvent event) {
      collectEvent(event);
    }
  }

  @Component
  static class GenericEventListener extends AbstractTestEventListener {

    @EventListener
    public void handleString(PayloadApplicationEvent<String> event) {
      collectEvent(event.getPayload());
    }
  }

  @Component
  static class ResolvableTypeEventListener extends AbstractTestEventListener {

    @EventListener
    public void handleString(GenericEventPojo<String> value) {
      collectEvent(value);
    }
  }

  @EventListener
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ConditionalEvent {

    @AliasFor(annotation = EventListener.class, attribute = "condition")
    String value();
  }

  interface ConditionalEventInterface extends Identifiable {

    void handle(TestEvent event);

    void handleString(String payload);

    void handleTimestamp(Long timestamp);

    void handleRatio(Double ratio);
  }

  @Component
  @Validated
  static class ConditionalEventListener extends TestEventListener implements ConditionalEventInterface {

    // language=SpEL
    @EventListener(condition = "'OK'.equals(#root.event.msg)")
    @Override
    public void handle(TestEvent event) {
      super.handle(event);
    }

    // language=SpEL
    @EventListener(condition = "#payload.startsWith('OK')")
    @Override
    public void handleString(String payload) {
      super.handleString(payload);
    }

    // language=SpEL
    @ConditionalEvent("#root.event.timestamp > #p0")
    @Override
    public void handleTimestamp(Long timestamp) {
      collectEvent(timestamp);
    }

    // language=SpEL
    @ConditionalEvent("@conditionEvaluator.valid(#p0)")
    @Override
    public void handleRatio(Double ratio) {
      collectEvent(ratio);
    }
  }

  @Component
  static class OrderedTestListener extends TestEventListener {

    public final List<String> order = new ArrayList<>();

    @EventListener
    @Order(50)
    public void handleThird(String payload) {
      this.order.add("third");
    }

    @EventListener
    @Order(-50)
    public void handleFirst(String payload) {
      this.order.add("first");
    }

    @EventListener
    public void handleSecond(String payload) {
      this.order.add("second");
    }
  }

  @Component
  public static class PublicSubclassWithInheritedEventListener extends TestEventListener {
  }

  static class EventOnPostConstruct {

    @Autowired
    ApplicationEventPublisher publisher;

    @PostConstruct
    public void init() {
      this.publisher.publishEvent("earlyEvent");
    }
  }

  private static class CustomScope implements cn.taketoday.beans.factory.config.Scope {

    public boolean active = true;

    private Object instance = null;

    @Override
    public Object get(String name, Supplier<?> objectFactory) {
      Assert.state(this.active, "Not active");
      if (this.instance == null) {
        this.instance = objectFactory.get();
      }
      return this.instance;
    }

    @Override
    public Object remove(String name) {
      return null;
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
    }

    @Override
    public Object resolveContextualObject(String key) {
      return null;
    }

    @Override
    public String getConversationId() {
      return null;
    }
  }

  @Configuration
  @Import(UseMissingEventListener.class)
  public static class MissingEventListener {

    @Bean
    public MyEventListener missing() {
      return null;
    }
  }

  @Component
  public static class MyEventListener {

    @EventListener
    public void hear(TestEvent e) {
      throw new AssertionError();
    }
  }

  public static class UseMissingEventListener {

    @Inject
    public UseMissingEventListener(Optional<MyEventListener> notHere) {
      if (notHere.isPresent()) {
        throw new AssertionError();
      }
    }
  }

}
