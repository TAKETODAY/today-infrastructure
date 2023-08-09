/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.event;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.context.ApplicationEventPublisherAware;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.PayloadApplicationEvent;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.context.support.StaticMessageSource;
import cn.taketoday.context.testfixture.beans.BeanThatBroadcasts;
import cn.taketoday.context.testfixture.beans.BeanThatListens;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.scheduling.support.TaskUtils;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.context.support.AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit and integration tests for the ApplicationContext event support.
 *
 * @author Alef Arendsen
 * @author Rick Evans
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class ApplicationContextEventTests extends AbstractApplicationEventListenerTests {

  @Test
  public void multicastSimpleEvent() {
    multicastEvent(true, ApplicationListener.class,
        new ContextRefreshedEvent(new StaticApplicationContext()), null);
    multicastEvent(true, ApplicationListener.class,
        new ContextClosedEvent(new StaticApplicationContext()), null);
  }

  @Test
  public void multicastGenericEvent() {
    multicastEvent(true, StringEventListener.class, createGenericTestEvent("test"),
        ResolvableType.forClassWithGenerics(GenericTestEvent.class, String.class));
  }

  @Test
  public void multicastGenericEventWrongType() {
    multicastEvent(false, StringEventListener.class, createGenericTestEvent(123L),
        ResolvableType.forClassWithGenerics(GenericTestEvent.class, Long.class));
  }

  @Test
  public void multicastGenericEventWildcardSubType() {
    multicastEvent(false, StringEventListener.class, createGenericTestEvent("test"),
        getGenericApplicationEventType("wildcardEvent"));
  }

  @Test
  public void multicastConcreteTypeGenericListener() {
    multicastEvent(true, StringEventListener.class, new StringEvent(this, "test"), null);
  }

  @Test
  public void multicastConcreteWrongTypeGenericListener() {
    multicastEvent(false, StringEventListener.class, new LongEvent(this, 123L), null);
  }

  @Test
  public void multicastSmartGenericTypeGenericListener() {
    multicastEvent(true, StringEventListener.class, new SmartGenericTestEvent<>(this, "test"), null);
  }

  @Test
  public void multicastSmartGenericWrongTypeGenericListener() {
    multicastEvent(false, StringEventListener.class, new SmartGenericTestEvent<>(this, 123L), null);
  }

  private void multicastEvent(boolean match, Class<?> listenerType, ApplicationEvent event, ResolvableType eventType) {
    @SuppressWarnings("unchecked")
    ApplicationListener<ApplicationEvent> listener =
        (ApplicationListener<ApplicationEvent>) mock(listenerType);
    SimpleApplicationEventMulticaster smc = new SimpleApplicationEventMulticaster();
    smc.addApplicationListener(listener);

    if (eventType != null) {
      smc.multicastEvent(event, eventType);
    }
    else {
      smc.multicastEvent(event);
    }
    int invocation = match ? 1 : 0;
    verify(listener, times(invocation)).onApplicationEvent(event);
  }

  @Test
  public void simpleApplicationEventMulticasterWithTaskExecutor() {
    ApplicationListener<ApplicationEvent> listener = mock();
    willReturn(true).given(listener).supportsAsyncExecution();
    ApplicationEvent evt = new ContextClosedEvent(new StaticApplicationContext());

    SimpleApplicationEventMulticaster smc = new SimpleApplicationEventMulticaster();
    AtomicBoolean invoked = new AtomicBoolean();
    smc.setTaskExecutor(command -> {
      invoked.set(true);
      command.run();
      command.run();
    });
    smc.addApplicationListener(listener);

    smc.multicastEvent(evt);
    assertThat(invoked.get()).isTrue();
    verify(listener, times(2)).onApplicationEvent(evt);
  }

  @Test
  public void simpleApplicationEventMulticasterWithTaskExecutorAndNonAsyncListener() {
    ApplicationListener<ApplicationEvent> listener = mock();
    willReturn(false).given(listener).supportsAsyncExecution();
    ApplicationEvent evt = new ContextClosedEvent(new StaticApplicationContext());

    SimpleApplicationEventMulticaster smc = new SimpleApplicationEventMulticaster();
    AtomicBoolean invoked = new AtomicBoolean();
    smc.setTaskExecutor(command -> {
      invoked.set(true);
      command.run();
      command.run();
    });
    smc.addApplicationListener(listener);

    smc.multicastEvent(evt);
    assertThat(invoked.get()).isFalse();
    verify(listener, times(1)).onApplicationEvent(evt);
  }

  @Test
  public void simpleApplicationEventMulticasterWithException() {
    ApplicationListener<ApplicationEvent> listener = mock();
    ApplicationEvent evt = new ContextClosedEvent(new StaticApplicationContext());

    SimpleApplicationEventMulticaster smc = new SimpleApplicationEventMulticaster();
    smc.addApplicationListener(listener);

    RuntimeException thrown = new RuntimeException();
    willThrow(thrown).given(listener).onApplicationEvent(evt);
    assertThatRuntimeException()
        .isThrownBy(() -> smc.multicastEvent(evt))
        .isSameAs(thrown);
  }

  @Test
  public void simpleApplicationEventMulticasterWithErrorHandler() {
    ApplicationListener<ApplicationEvent> listener = mock();
    ApplicationEvent evt = new ContextClosedEvent(new StaticApplicationContext());

    SimpleApplicationEventMulticaster smc = new SimpleApplicationEventMulticaster();
    smc.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER);
    smc.addApplicationListener(listener);

    willThrow(new RuntimeException()).given(listener).onApplicationEvent(evt);
    smc.multicastEvent(evt);
  }

  @Test
  public void orderedListeners() {
    MyOrderedListener1 listener1 = new MyOrderedListener1();
    MyOrderedListener2 listener2 = new MyOrderedListener2(listener1);

    SimpleApplicationEventMulticaster smc = new SimpleApplicationEventMulticaster();
    smc.addApplicationListener(listener2);
    smc.addApplicationListener(listener1);

    smc.multicastEvent(new MyEvent(this));
    smc.multicastEvent(new MyOtherEvent(this));
    assertThat(listener1.seenEvents).hasSize(2);
  }

  @Test
  public void orderedListenersWithAnnotation() {
    MyOrderedListener3 listener1 = new MyOrderedListener3();
    MyOrderedListener4 listener2 = new MyOrderedListener4(listener1);

    SimpleApplicationEventMulticaster smc = new SimpleApplicationEventMulticaster();
    smc.addApplicationListener(listener2);
    smc.addApplicationListener(listener1);

    smc.multicastEvent(new MyEvent(this));
    smc.multicastEvent(new MyOtherEvent(this));
    assertThat(listener1.seenEvents).hasSize(2);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void proxiedListeners() {
    MyOrderedListener1 listener1 = new MyOrderedListener1();
    MyOrderedListener2 listener2 = new MyOrderedListener2(listener1);
    ApplicationListener<ApplicationEvent> proxy1 = (ApplicationListener<ApplicationEvent>) new ProxyFactory(listener1).getProxy();
    ApplicationListener<ApplicationEvent> proxy2 = (ApplicationListener<ApplicationEvent>) new ProxyFactory(listener2).getProxy();

    SimpleApplicationEventMulticaster smc = new SimpleApplicationEventMulticaster();
    smc.addApplicationListener(proxy1);
    smc.addApplicationListener(proxy2);

    smc.multicastEvent(new MyEvent(this));
    smc.multicastEvent(new MyOtherEvent(this));
    assertThat(listener1.seenEvents).hasSize(2);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void proxiedListenersMixedWithTargetListeners() {
    MyOrderedListener1 listener1 = new MyOrderedListener1();
    MyOrderedListener2 listener2 = new MyOrderedListener2(listener1);
    ApplicationListener<ApplicationEvent> proxy1 = (ApplicationListener<ApplicationEvent>) new ProxyFactory(listener1).getProxy();
    ApplicationListener<ApplicationEvent> proxy2 = (ApplicationListener<ApplicationEvent>) new ProxyFactory(listener2).getProxy();

    SimpleApplicationEventMulticaster smc = new SimpleApplicationEventMulticaster();
    smc.addApplicationListener(listener1);
    smc.addApplicationListener(listener2);
    smc.addApplicationListener(proxy1);
    smc.addApplicationListener(proxy2);

    smc.multicastEvent(new MyEvent(this));
    smc.multicastEvent(new MyOtherEvent(this));
    assertThat(listener1.seenEvents).hasSize(2);
  }

  @Test
  public void testEventPublicationInterceptor() throws Throwable {
    MethodInvocation invocation = mock();
    ApplicationContext ctx = mock();

    EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
    interceptor.setApplicationEventClass(MyEvent.class);
    interceptor.setApplicationEventPublisher(ctx);
    interceptor.afterPropertiesSet();

    given(invocation.proceed()).willReturn(new Object());
    given(invocation.getThis()).willReturn(new Object());
    interceptor.invoke(invocation);
    verify(ctx).publishEvent(isA(MyEvent.class));
  }

  @Test
  public void listenersInApplicationContext() {
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerBeanDefinition("listener1", new RootBeanDefinition(MyOrderedListener1.class));
    RootBeanDefinition listener2 = new RootBeanDefinition(MyOrderedListener2.class);
    listener2.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("listener1"));
    listener2.setLazyInit(true);
    context.registerBeanDefinition("listener2", listener2);
    context.refresh();
    assertThat(context.getBeanFactory().containsSingleton("listener2")).isFalse();

    MyOrderedListener1 listener1 = context.getBean("listener1", MyOrderedListener1.class);
    MyOtherEvent event1 = new MyOtherEvent(context);
    context.publishEvent(event1);
    assertThat(context.getBeanFactory().containsSingleton("listener2")).isFalse();
    MyEvent event2 = new MyEvent(context);
    context.publishEvent(event2);
    assertThat(context.getBeanFactory().containsSingleton("listener2")).isTrue();
    MyEvent event3 = new MyEvent(context);
    context.publishEvent(event3);
    MyOtherEvent event4 = new MyOtherEvent(context);
    context.publishEvent(event4);
    assertThat(listener1.seenEvents).contains(event1, event2, event3, event4);

    listener1.seenEvents.clear();
    context.publishEvent(event1);
    context.publishEvent(event2);
    context.publishEvent(event3);
    context.publishEvent(event4);
    assertThat(listener1.seenEvents).contains(event1, event2, event3, event4);

    AbstractApplicationEventMulticaster multicaster = context.getBean(AbstractApplicationEventMulticaster.class);
    assertThat(multicaster.retrieverCache).hasSize(2);

    context.close();
  }

  @Test
  public void listenersInApplicationContextWithPayloadEvents() {
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerBeanDefinition("listener", new RootBeanDefinition(MyPayloadListener.class));
    context.refresh();

    MyPayloadListener listener = context.getBean("listener", MyPayloadListener.class);
    context.publishEvent("event1");
    context.publishEvent("event2");
    context.publishEvent("event3");
    context.publishEvent("event4");
    assertThat(listener.seenPayloads).contains("event1", "event2", "event3", "event4");

    AbstractApplicationEventMulticaster multicaster = context.getBean(AbstractApplicationEventMulticaster.class);
    assertThat(multicaster.retrieverCache).hasSize(2);

    context.close();
  }

  @Test
  public void listenersInApplicationContextWithNestedChild() {
    StaticApplicationContext context = new StaticApplicationContext();
    RootBeanDefinition nestedChild = new RootBeanDefinition(StaticApplicationContext.class);
    nestedChild.getPropertyValues().add("parent", context);
    nestedChild.setInitMethodName("refresh");
    context.registerBeanDefinition("nestedChild", nestedChild);
    RootBeanDefinition listener1Def = new RootBeanDefinition(MyOrderedListener1.class);
    listener1Def.setDependsOn("nestedChild");
    context.registerBeanDefinition("listener1", listener1Def);
    context.refresh();

    MyOrderedListener1 listener1 = context.getBean("listener1", MyOrderedListener1.class);
    MyEvent event1 = new MyEvent(context);
    context.publishEvent(event1);
    assertThat(listener1.seenEvents).contains(event1);

    SimpleApplicationEventMulticaster multicaster = context.getBean(SimpleApplicationEventMulticaster.class);
    assertThat(multicaster.getApplicationListeners()).isNotEmpty();

    context.close();
    assertThat(multicaster.getApplicationListeners()).isEmpty();
  }

  @Test
  public void nonSingletonListenerInApplicationContext() {
    StaticApplicationContext context = new StaticApplicationContext();
    RootBeanDefinition listener = new RootBeanDefinition(MyNonSingletonListener.class);
    listener.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    context.registerBeanDefinition("listener", listener);
    context.refresh();

    MyEvent event1 = new MyEvent(context);
    context.publishEvent(event1);
    MyOtherEvent event2 = new MyOtherEvent(context);
    context.publishEvent(event2);
    MyEvent event3 = new MyEvent(context);
    context.publishEvent(event3);
    MyOtherEvent event4 = new MyOtherEvent(context);
    context.publishEvent(event4);
    assertThat(MyNonSingletonListener.seenEvents).contains(event1, event2, event3, event4);
    MyNonSingletonListener.seenEvents.clear();

    context.publishEvent(event1);
    context.publishEvent(event2);
    context.publishEvent(event3);
    context.publishEvent(event4);
    assertThat(MyNonSingletonListener.seenEvents).contains(event1, event2, event3, event4);
    MyNonSingletonListener.seenEvents.clear();

    AbstractApplicationEventMulticaster multicaster = context.getBean(AbstractApplicationEventMulticaster.class);
    assertThat(multicaster.retrieverCache).hasSize(3);

    context.close();
  }

  @Test
  public void listenerAndBroadcasterWithCircularReference() {
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerBeanDefinition("broadcaster", new RootBeanDefinition(BeanThatBroadcasts.class));
    RootBeanDefinition listenerDef = new RootBeanDefinition(BeanThatListens.class);
    listenerDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("broadcaster"));
    context.registerBeanDefinition("listener", listenerDef);
    context.refresh();

    BeanThatBroadcasts broadcaster = context.getBean("broadcaster", BeanThatBroadcasts.class);
    context.publishEvent(new MyEvent(context));
    assertThat(broadcaster.receivedCount).as("The event was not received by the listener").isEqualTo(2);

    context.close();
  }

  @Test
  public void innerBeanAsListener() {
    StaticApplicationContext context = new StaticApplicationContext();
    RootBeanDefinition listenerDef = new RootBeanDefinition(TestBean.class);
    listenerDef.getPropertyValues().add("friends", new RootBeanDefinition(BeanThatListens.class));
    context.registerBeanDefinition("listener", listenerDef);
    context.refresh();

    context.publishEvent(new MyEvent(this));
    context.publishEvent(new MyEvent(this));
    TestBean listener = context.getBean(TestBean.class);
    assertThat(((BeanThatListens) listener.getFriends().iterator().next()).getEventCount()).isEqualTo(3);

    context.close();
  }

  @Test
  public void anonymousClassAsListener() {
    final Set<MyEvent> seenEvents = new HashSet<>();
    StaticApplicationContext context = new StaticApplicationContext();
    context.addApplicationListener((MyEvent event) -> seenEvents.add(event));
    context.refresh();

    MyEvent event1 = new MyEvent(context);
    context.publishEvent(event1);
    context.publishEvent(new MyOtherEvent(context));
    MyEvent event2 = new MyEvent(context);
    context.publishEvent(event2);
    assertThat(seenEvents).contains(event1, event2);

    context.close();
  }

  @Test
  public void lambdaAsListener() {
    final Set<MyEvent> seenEvents = new HashSet<>();
    StaticApplicationContext context = new StaticApplicationContext();
    ApplicationListener<MyEvent> listener = seenEvents::add;
    context.addApplicationListener(listener);
    context.refresh();

    MyEvent event1 = new MyEvent(context);
    context.publishEvent(event1);
    context.publishEvent(new MyOtherEvent(context));
    MyEvent event2 = new MyEvent(context);
    context.publishEvent(event2);
    assertThat(seenEvents).contains(event1, event2);

    context.close();
  }

  @Test
  public void lambdaAsListenerWithErrorHandler() {
    final Set<MyEvent> seenEvents = new HashSet<>();
    StaticApplicationContext context = new StaticApplicationContext();
    SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
    multicaster.setErrorHandler(ReflectionUtils::rethrowRuntimeException);
    context.getBeanFactory().registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, multicaster);
    ApplicationListener<MyEvent> listener = seenEvents::add;
    context.addApplicationListener(listener);
    context.refresh();

    MyEvent event1 = new MyEvent(context);
    context.publishEvent(event1);
    context.publishEvent(new MyOtherEvent(context));
    MyEvent event2 = new MyEvent(context);
    context.publishEvent(event2);
    assertThat(seenEvents).containsExactlyInAnyOrder(event1, event2);

    context.close();
  }

  @Test
  public void lambdaAsListenerWithJava8StyleClassCastMessage() {
    StaticApplicationContext context = new StaticApplicationContext();
    ApplicationListener<ApplicationEvent> listener =
        event -> { throw new ClassCastException(event.getClass().getName()); };
    context.addApplicationListener(listener);
    context.refresh();

    context.publishEvent(new MyEvent(context));
    context.close();
  }

  @Test
  public void lambdaAsListenerWithJava9StyleClassCastMessage() {
    StaticApplicationContext context = new StaticApplicationContext();
    ApplicationListener<ApplicationEvent> listener =
        event -> { throw new ClassCastException("spring.context/" + event.getClass().getName()); };
    context.addApplicationListener(listener);
    context.refresh();

    context.publishEvent(new MyEvent(context));
    context.close();
  }

  @Test
  public void beanPostProcessorPublishesEvents() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBeanDefinition("listener", new RootBeanDefinition(BeanThatListens.class));
    context.registerBeanDefinition("messageSource", new RootBeanDefinition(StaticMessageSource.class));
    context.registerBeanDefinition("postProcessor", new RootBeanDefinition(EventPublishingBeanPostProcessor.class));
    context.refresh();

    context.publishEvent(new MyEvent(this));
    BeanThatListens listener = context.getBean(BeanThatListens.class);
    assertThat(listener.getEventCount()).isEqualTo(4);

    context.close();
  }

  @Test
  public void initMethodPublishesEvent() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBeanDefinition("listener", new RootBeanDefinition(BeanThatListens.class));
    context.registerBeanDefinition("messageSource", new RootBeanDefinition(StaticMessageSource.class));
    context.registerBeanDefinition("initMethod", new RootBeanDefinition(EventPublishingInitMethod.class));
    context.refresh();

    context.publishEvent(new MyEvent(this));
    BeanThatListens listener = context.getBean(BeanThatListens.class);
    assertThat(listener.getEventCount()).isEqualTo(3);

    context.close();
  }

  @Test
  public void initMethodPublishesAsyncEvent() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBeanDefinition("listener", new RootBeanDefinition(BeanThatListens.class));
    context.registerBeanDefinition("messageSource", new RootBeanDefinition(StaticMessageSource.class));
    context.registerBeanDefinition("initMethod", new RootBeanDefinition(AsyncEventPublishingInitMethod.class));
    context.refresh();

    context.publishEvent(new MyEvent(this));
    BeanThatListens listener = context.getBean(BeanThatListens.class);
    assertThat(listener.getEventCount()).isEqualTo(3);

    context.close();
  }

  @SuppressWarnings("serial")
  public static class MyEvent extends ApplicationEvent {

    public MyEvent(Object source) {
      super(source);
    }
  }

  @SuppressWarnings("serial")
  public static class MyOtherEvent extends ApplicationEvent {

    public MyOtherEvent(Object source) {
      super(source);
    }
  }

  public static class MyOrderedListener1 implements ApplicationListener<ApplicationEvent>, Ordered {

    public final List<ApplicationEvent> seenEvents = new ArrayList<>();

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
      this.seenEvents.add(event);
    }

    @Override
    public int getOrder() {
      return 0;
    }
  }

  public interface MyOrderedListenerIfc<E extends ApplicationEvent> extends ApplicationListener<E>, Ordered {
  }

  public static abstract class MyOrderedListenerBase implements MyOrderedListenerIfc<MyEvent> {

    @Override
    public int getOrder() {
      return 1;
    }
  }

  public static class MyOrderedListener2 extends MyOrderedListenerBase {

    private final MyOrderedListener1 otherListener;

    public MyOrderedListener2(MyOrderedListener1 otherListener) {
      this.otherListener = otherListener;
    }

    @Override
    public void onApplicationEvent(MyEvent event) {
      assertThat(this.otherListener.seenEvents.contains(event)).isTrue();
    }
  }

  @SuppressWarnings("rawtypes")
  public static class MyPayloadListener implements ApplicationListener<PayloadApplicationEvent> {

    public final Set<Object> seenPayloads = new HashSet<>();

    @Override
    public void onApplicationEvent(PayloadApplicationEvent event) {
      this.seenPayloads.add(event.getPayload());
    }
  }

  public static class MyNonSingletonListener implements ApplicationListener<ApplicationEvent> {

    public static final Set<ApplicationEvent> seenEvents = new HashSet<>();

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
      seenEvents.add(event);
    }
  }

  @Order(5)
  public static class MyOrderedListener3 implements ApplicationListener<ApplicationEvent> {

    public final Set<ApplicationEvent> seenEvents = new HashSet<>();

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
      this.seenEvents.add(event);
    }

  }

  @Order(50)
  public static class MyOrderedListener4 implements ApplicationListener<MyEvent> {

    private final MyOrderedListener3 otherListener;

    public MyOrderedListener4(MyOrderedListener3 otherListener) {
      this.otherListener = otherListener;
    }

    @Override
    public void onApplicationEvent(MyEvent event) {
      assertThat(this.otherListener.seenEvents.contains(event)).isTrue();
    }
  }

  public static class EventPublishingBeanPostProcessor implements InitializationBeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      this.applicationContext.publishEvent(new MyEvent(this));
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      return bean;
    }
  }

  public static class EventPublishingInitMethod implements ApplicationEventPublisherAware, InitializingBean {

    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
      this.publisher = applicationEventPublisher;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
      this.publisher.publishEvent(new MyEvent(this));
    }
  }

  public static class AsyncEventPublishingInitMethod implements ApplicationEventPublisherAware, InitializingBean {

    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
      this.publisher = applicationEventPublisher;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
      Thread thread = new Thread(() -> this.publisher.publishEvent(new MyEvent(this)));
      thread.start();
      thread.join();
    }
  }

}
