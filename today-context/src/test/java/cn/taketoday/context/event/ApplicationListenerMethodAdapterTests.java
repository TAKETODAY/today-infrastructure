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

package cn.taketoday.context.event;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.PayloadApplicationEvent;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.ResolvableTypeProvider;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 */
public class ApplicationListenerMethodAdapterTests extends AbstractApplicationEventListenerTests {

  private final SampleEvents sampleEvents = spy(new SampleEvents());

  private final ApplicationContext context = mock();

  @Test
  public void rawListener() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleRaw", ApplicationEvent.class);
    supportsEventType(true, method, ResolvableType.fromClass(ApplicationEvent.class));
  }

  @Test
  public void rawListenerWithGenericEvent() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleRaw", ApplicationEvent.class);
    supportsEventType(true, method, ResolvableType.fromClassWithGenerics(GenericTestEvent.class, String.class));
  }

  @Test
  public void genericListener() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "handleGenericString", GenericTestEvent.class);
    supportsEventType(true, method, ResolvableType.fromClassWithGenerics(GenericTestEvent.class, String.class));
  }

  @Test
  public void genericListenerWrongParameterizedType() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "handleGenericString", GenericTestEvent.class);
    supportsEventType(false, method, ResolvableType.fromClassWithGenerics(GenericTestEvent.class, Long.class));
  }

  @Test
  public void listenerWithPayloadAndGenericInformation() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleString", String.class);
    supportsEventType(true, method, createGenericEventType(String.class));
  }

  @Test
  public void listenerWithInvalidPayloadAndGenericInformation() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleString", String.class);
    supportsEventType(false, method, createGenericEventType(Integer.class));
  }

  @Test
  public void listenerWithPayloadTypeErasure() {  // Always accept such event when the type is unknown
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleString", String.class);
    supportsEventType(true, method, ResolvableType.fromClass(PayloadApplicationEvent.class));
  }

  @Test
  public void listenerWithSubTypeSeveralGenerics() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleString", String.class);
    supportsEventType(true, method, ResolvableType.fromClass(PayloadTestEvent.class));
  }

  @Test
  public void listenerWithSubTypeSeveralGenericsResolved() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleString", String.class);
    supportsEventType(true, method, ResolvableType.fromClass(PayloadStringTestEvent.class));
  }

  @Test
  public void listenerWithAnnotationValue() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleStringAnnotationValue");
    supportsEventType(true, method, createGenericEventType(String.class));
  }

  @Test
  public void listenerWithAnnotationClasses() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleStringAnnotationClasses");
    supportsEventType(true, method, createGenericEventType(String.class));
  }

  @Test
  public void listenerWithAnnotationValueAndParameter() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "handleStringAnnotationValueAndParameter", String.class);
    supportsEventType(true, method, createGenericEventType(String.class));
  }

  @Test
  public void listenerWithSeveralTypes() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleStringOrInteger");
    supportsEventType(true, method, createGenericEventType(String.class));
    supportsEventType(true, method, createGenericEventType(Integer.class));
    supportsEventType(false, method, createGenericEventType(Double.class));
  }

  @Test
  public void listenerWithTooManyParameters() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "tooManyParameters", String.class, String.class);
    assertThatIllegalStateException().isThrownBy(() -> createTestInstance(method));
  }

  @Test
  public void listenerWithNoParameter() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "noParameter");
    assertThatIllegalStateException().isThrownBy(() -> createTestInstance(method));
  }

  @Test
  public void listenerWithMoreThanOneParameter() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "moreThanOneParameter", String.class, Integer.class);
    assertThatIllegalStateException().isThrownBy(() -> createTestInstance(method));
  }

  @Test
  public void defaultOrder() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "handleGenericString", GenericTestEvent.class);
    ApplicationListenerMethodAdapter adapter = createTestInstance(method);
    assertThat(adapter.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
  }

  @Test
  public void specifiedOrder() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "handleRaw", ApplicationEvent.class);
    ApplicationListenerMethodAdapter adapter = createTestInstance(method);
    assertThat(adapter.getOrder()).isEqualTo(42);
  }

  @Test
  public void invokeListener() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "handleGenericString", GenericTestEvent.class);
    GenericTestEvent<String> event = createGenericTestEvent("test");
    invokeListener(method, event);
    verify(this.sampleEvents, times(1)).handleGenericString(event);
  }

  @Test
  public void invokeListenerWithGenericEvent() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "handleGenericString", GenericTestEvent.class);
    GenericTestEvent<String> event = new SmartGenericTestEvent<>(this, "test");
    invokeListener(method, event);
    verify(this.sampleEvents, times(1)).handleGenericString(event);
  }

  @Test
  public void invokeListenerWithGenericPayload() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "handleGenericStringPayload", EntityWrapper.class);
    EntityWrapper<String> payload = new EntityWrapper<>("test");
    invokeListener(method, new PayloadApplicationEvent<>(this, payload));
    verify(this.sampleEvents, times(1)).handleGenericStringPayload(payload);
  }

  @Test
  public void invokeListenerWithWrongGenericPayload() {
    Method method = ReflectionUtils.findMethod
            (SampleEvents.class, "handleGenericStringPayload", EntityWrapper.class);
    EntityWrapper<Integer> payload = new EntityWrapper<>(123);
    invokeListener(method, new PayloadApplicationEvent<>(this, payload));
    verify(this.sampleEvents, times(0)).handleGenericStringPayload(any());
  }

  @Test
  public void invokeListenerWithAnyGenericPayload() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "handleGenericAnyPayload", EntityWrapper.class);
    EntityWrapper<String> payload = new EntityWrapper<>("test");
    invokeListener(method, new PayloadApplicationEvent<>(this, payload));
    verify(this.sampleEvents, times(1)).handleGenericAnyPayload(payload);
  }

  @Test
  public void invokeListenerRuntimeException() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "generateRuntimeException", GenericTestEvent.class);
    GenericTestEvent<String> event = createGenericTestEvent("fail");

    assertThatIllegalStateException().isThrownBy(() ->
                    invokeListener(method, event))
            .withMessageContaining("Test exception")
            .withNoCause();
  }

  @Test
  public void invokeListenerCheckedException() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "generateCheckedException", GenericTestEvent.class);
    GenericTestEvent<String> event = createGenericTestEvent("fail");

    assertThatExceptionOfType(UndeclaredThrowableException.class).isThrownBy(() ->
                    invokeListener(method, event))
            .withCauseInstanceOf(IOException.class);
  }

  @Test
  public void invokeListenerInvalidProxy() {
    Object target = new InvalidProxyTestBean();
    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.setTarget(target);
    proxyFactory.addInterface(SimpleService.class);
    Object bean = proxyFactory.getProxy(getClass().getClassLoader());

    Method method = ReflectionUtils.findMethod(
            InvalidProxyTestBean.class, "handleIt2", ApplicationEvent.class);
    StaticApplicationListenerMethodAdapter listener =
            new StaticApplicationListenerMethodAdapter(method, bean);
    assertThatIllegalStateException().isThrownBy(() ->
                    listener.onApplicationEvent(createGenericTestEvent("test")))
            .withMessageContaining("handleIt2");
  }

  @Test
  public void invokeListenerWithPayload() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleString", String.class);
    PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "test");
    invokeListener(method, event);
    verify(this.sampleEvents, times(1)).handleString("test");
  }

  @Test
  public void invokeListenerWithPayloadWrongType() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleString", String.class);
    PayloadApplicationEvent<Long> event = new PayloadApplicationEvent<>(this, 123L);
    invokeListener(method, event);
    verify(this.sampleEvents, never()).handleString(anyString());
  }

  @Test
  public void invokeListenerWithAnnotationValue() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleStringAnnotationClasses");
    PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "test");
    invokeListener(method, event);
    verify(this.sampleEvents, times(1)).handleStringAnnotationClasses();
  }

  @Test
  public void invokeListenerWithAnnotationValueAndParameter() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "handleStringAnnotationValueAndParameter", String.class);
    PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "test");
    invokeListener(method, event);
    verify(this.sampleEvents, times(1)).handleStringAnnotationValueAndParameter("test");
  }

  @Test
  public void invokeListenerWithSeveralTypes() {
    Method method = ReflectionUtils.findMethod(SampleEvents.class, "handleStringOrInteger");
    PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "test");
    invokeListener(method, event);
    verify(this.sampleEvents, times(1)).handleStringOrInteger();
    PayloadApplicationEvent<Integer> event2 = new PayloadApplicationEvent<>(this, 123);
    invokeListener(method, event2);
    verify(this.sampleEvents, times(2)).handleStringOrInteger();
    PayloadApplicationEvent<Double> event3 = new PayloadApplicationEvent<>(this, 23.2);
    invokeListener(method, event3);
    verify(this.sampleEvents, times(2)).handleStringOrInteger();
  }

  @Test
  public void beanInstanceRetrievedAtEveryInvocation() {
    Method method = ReflectionUtils.findMethod(
            SampleEvents.class, "handleGenericString", GenericTestEvent.class);
    given(this.context.getBean("testBean")).willReturn(this.sampleEvents);
    ApplicationListenerMethodAdapter listener = new ApplicationListenerMethodAdapter(
            "testBean", GenericTestEvent.class, method);
    listener.init(this.context, new EventExpressionEvaluator());
    GenericTestEvent<String> event = createGenericTestEvent("test");

    listener.onApplicationEvent(event);
    verify(this.sampleEvents, times(1)).handleGenericString(event);
    verify(this.context, times(1)).getBean("testBean");

    listener.onApplicationEvent(event);
    verify(this.sampleEvents, times(2)).handleGenericString(event);
    verify(this.context, times(2)).getBean("testBean");
  }

  // see https://github.com/spring-projects/spring-framework/issues/30399
  @Test
  void simplePayloadDoesNotSupportArbitraryGenericEventType() throws Exception {
    var method = SampleEvents.class.getDeclaredMethod("handleString", String.class);
    var adapter = new ApplicationListenerMethodAdapter(null, ApplicationListenerMethodAdapterTests.class, method);

    assertThat(adapter.supportsEventType(ResolvableType.fromClassWithGenerics(EntityWrapper.class, Integer.class)))
            .as("handleString(String) with EntityWrapper<Integer>").isFalse();
    assertThat(adapter.supportsEventType(ResolvableType.fromClass(EntityWrapper.class)))
            .as("handleString(String) with EntityWrapper<?>").isFalse();
    assertThat(adapter.supportsEventType(ResolvableType.fromClass(String.class)))
            .as("handleString(String) with String").isTrue();
  }

  // see https://github.com/spring-projects/spring-framework/issues/30399
  @Test
  void genericPayloadDoesNotSupportArbitraryGenericEventType() throws Exception {
    var method = SampleEvents.class.getDeclaredMethod("handleGenericStringPayload", EntityWrapper.class);
    var adapter = new ApplicationListenerMethodAdapter(null, ApplicationListenerMethodAdapterTests.class, method);

    assertThat(adapter.supportsEventType(ResolvableType.fromClass(EntityWrapper.class)))
            .as("handleGenericStringPayload(EntityWrapper<String>) with EntityWrapper<?>").isFalse();
    assertThat(adapter.supportsEventType(ResolvableType.fromClassWithGenerics(EntityWrapper.class, Integer.class)))
            .as("handleGenericStringPayload(EntityWrapper<String>) with EntityWrapper<Integer>").isFalse();
    assertThat(adapter.supportsEventType(ResolvableType.fromClassWithGenerics(EntityWrapper.class, String.class)))
            .as("handleGenericStringPayload(EntityWrapper<String>) with EntityWrapper<String>").isTrue();
  }

  // see https://github.com/spring-projects/spring-framework/issues/30399
  @Test
  void rawGenericPayloadDoesNotSupportArbitraryGenericEventType() throws Exception {
    var method = SampleEvents.class.getDeclaredMethod("handleGenericAnyPayload", EntityWrapper.class);
    var adapter = new ApplicationListenerMethodAdapter(null, ApplicationListenerMethodAdapterTests.class, method);

    assertThat(adapter.supportsEventType(ResolvableType.fromClass(EntityWrapper.class)))
            .as("handleGenericAnyPayload(EntityWrapper<?>) with EntityWrapper<?>").isTrue();
    assertThat(adapter.supportsEventType(ResolvableType.fromClassWithGenerics(EntityWrapper.class, Integer.class)))
            .as("handleGenericAnyPayload(EntityWrapper<?>) with EntityWrapper<Integer>").isTrue();
    assertThat(adapter.supportsEventType(ResolvableType.fromClassWithGenerics(EntityWrapper.class, String.class)))
            .as("handleGenericAnyPayload(EntityWrapper<?>) with EntityWrapper<String>").isTrue();
    assertThat(adapter.supportsEventType(ResolvableType.fromClass(List.class)))
            .as("handleGenericAnyPayload(EntityWrapper<?>) with List<?>").isFalse();
    assertThat(adapter.supportsEventType(ResolvableType.fromClassWithGenerics(List.class, String.class)))
            .as("handleGenericAnyPayload(EntityWrapper<?>) with List<String>").isFalse();
  }

  @Test
  void genericApplicationEventSupportsSpecificType() throws Exception {
    var method = SampleEvents.class.getDeclaredMethod("handleGenericString", GenericTestEvent.class);
    var adapter = new ApplicationListenerMethodAdapter(null, ApplicationListenerMethodAdapterTests.class, method);

    assertThat(adapter.supportsEventType(ResolvableType.fromClass(GenericTestEvent.class)))
            .as("handleGenericString(GenericTestEvent<String>) with GenericTestEvent<?>").isFalse();
    assertThat(adapter.supportsEventType(ResolvableType.fromClassWithGenerics(GenericTestEvent.class, Integer.class)))
            .as("handleGenericString(GenericTestEvent<String>) with GenericTestEvent<Integer>").isFalse();
    assertThat(adapter.supportsEventType(ResolvableType.fromClassWithGenerics(GenericTestEvent.class, String.class)))
            .as("handleGenericString(GenericTestEvent<String>) with GenericTestEvent<String>").isTrue();
  }

  @Test
  void genericRawApplicationEventSupportsRawTypeAndAnySpecificType() throws Exception {
    var method = SampleEvents.class.getDeclaredMethod("handleGenericRaw", GenericTestEvent.class);
    var adapter = new ApplicationListenerMethodAdapter(null, ApplicationListenerMethodAdapterTests.class, method);

    assertThat(adapter.supportsEventType(ResolvableType.fromClass(GenericTestEvent.class)))
            .as("handleGenericRaw(GenericTestEvent<?>) with GenericTestEvent<?>").isTrue();
    assertThat(adapter.supportsEventType(ResolvableType.fromClassWithGenerics(GenericTestEvent.class, String.class)))
            .as("handleGenericRaw(GenericTestEvent<?>) with GenericTestEvent<String>").isTrue();
    assertThat(adapter.supportsEventType(ResolvableType.fromClassWithGenerics(GenericTestEvent.class, Integer.class)))
            .as("handleGenericRaw(GenericTestEvent<?>) with GenericTestEvent<Integer>").isTrue();
  }

  private void supportsEventType(boolean match, Method method, ResolvableType eventType) {
    ApplicationListenerMethodAdapter adapter = createTestInstance(method);
    assertThat(adapter.supportsEventType(eventType))
            .as("Wrong match for event '" + eventType + "' on " + method).isEqualTo(match);
  }

  private void invokeListener(Method method, ApplicationEvent event) {
    ApplicationListenerMethodAdapter adapter = createTestInstance(method);
    adapter.onApplicationEvent(event);
  }

  private ApplicationListenerMethodAdapter createTestInstance(Method method) {
    return new StaticApplicationListenerMethodAdapter(method, this.sampleEvents);
  }

  private ResolvableType createGenericEventType(Class<?> payloadType) {
    return ResolvableType.fromClassWithGenerics(PayloadApplicationEvent.class, payloadType);
  }

  private static class StaticApplicationListenerMethodAdapter extends ApplicationListenerMethodAdapter {

    private final Object targetBean;

    public StaticApplicationListenerMethodAdapter(Method method, Object targetBean) {
      super("unused", targetBean.getClass(), method);
      this.targetBean = targetBean;
    }

    @Override
    public Object getTargetBean() {
      return this.targetBean;
    }
  }

  private static class SampleEvents {

    @EventListener
    @Order(42)
    public void handleRaw(ApplicationEvent event) {
    }

    @EventListener
    public void handleGenericString(GenericTestEvent<String> event) {
    }

    @EventListener
    public void handleGenericRaw(GenericTestEvent<?> event) {
    }

    @EventListener
    public void handleString(String payload) {
    }

    @EventListener(String.class)
    public void handleStringAnnotationValue() {
    }

    @EventListener(event = String.class)
    public void handleStringAnnotationClasses() {
    }

    @EventListener(String.class)
    public void handleStringAnnotationValueAndParameter(String payload) {
    }

    @EventListener({ String.class, Integer.class })
    public void handleStringOrInteger() {
    }

    @EventListener({ String.class, Integer.class })
    public void handleStringOrIntegerWithParam(String invalid) {
    }

    @EventListener
    public void handleGenericStringPayload(EntityWrapper<String> event) {
    }

    @EventListener
    public void handleGenericAnyPayload(EntityWrapper<?> event) {
    }

    @EventListener
    public void tooManyParameters(String event, String whatIsThis) {
    }

    @EventListener
    public void noParameter() {
    }

    @EventListener
    public void moreThanOneParameter(String foo, Integer bar) {
    }

    @EventListener
    public void generateRuntimeException(GenericTestEvent<String> event) {
      if ("fail".equals(event.getPayload())) {
        throw new IllegalStateException("Test exception");
      }
    }

    @EventListener
    public void generateCheckedException(GenericTestEvent<String> event) throws IOException {
      if ("fail".equals(event.getPayload())) {
        throw new IOException("Test exception");
      }
    }
  }

  interface SimpleService {

    void handleIt(ApplicationEvent event);
  }

  private static class EntityWrapper<T> implements ResolvableTypeProvider {

    private final T entity;

    public EntityWrapper(T entity) {
      this.entity = entity;
    }

    @Override
    public ResolvableType getResolvableType() {
      return ResolvableType.fromClassWithGenerics(getClass(), this.entity.getClass());
    }
  }

  static class InvalidProxyTestBean implements SimpleService {

    @Override
    public void handleIt(ApplicationEvent event) {
    }

    @EventListener
    public void handleIt2(ApplicationEvent event) {
    }
  }

  @SuppressWarnings({ "unused", "serial" })
  static class PayloadTestEvent<V, T> extends PayloadApplicationEvent<T> {

    private final V something;

    public PayloadTestEvent(Object source, T payload, V something) {
      super(source, payload);
      this.something = something;
    }
  }

  @SuppressWarnings({ "serial" })
  static class PayloadStringTestEvent extends PayloadTestEvent<Long, String> {

    public PayloadStringTestEvent(Object source, String payload, Long something) {
      super(source, payload, something);
    }
  }

}
