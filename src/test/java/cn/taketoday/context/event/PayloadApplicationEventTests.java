/*
 * Copyright 2002-2022 the original author or authors.
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

package cn.taketoday.context.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.PayloadApplicationEvent;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class PayloadApplicationEventTests {

  @Test
  void payloadApplicationEventWithNoTypeUsesInstance() {
    NumberHolder<Integer> payload = new NumberHolder<>(42);
    PayloadApplicationEvent<NumberHolder<Integer>> event = new PayloadApplicationEvent<>(this, payload);
    assertThat(event.getResolvableType()).satisfies(eventType -> {
      assertThat(eventType.toClass()).isEqualTo(PayloadApplicationEvent.class);
      assertThat(eventType.getGenerics())
              .hasSize(1)
              .allSatisfy(bodyType -> {
                assertThat(bodyType.toClass()).isEqualTo(NumberHolder.class);
                assertThat(bodyType.hasUnresolvableGenerics()).isTrue();
              });
    });
  }

  @Test
  void payloadApplicationEventWithType() {
    NumberHolder<Integer> payload = new NumberHolder<>(42);
    ResolvableType payloadType = ResolvableType.fromClassWithGenerics(NumberHolder.class, Integer.class);
    PayloadApplicationEvent<NumberHolder<Integer>> event = new PayloadApplicationEvent<>(this, payload, payloadType);
    assertThat(event.getResolvableType()).satisfies(eventType -> {
      assertThat(eventType.toClass()).isEqualTo(PayloadApplicationEvent.class);
      assertThat(eventType.getGenerics())
              .hasSize(1)
              .allSatisfy(bodyType -> {
                assertThat(bodyType.toClass()).isEqualTo(NumberHolder.class);
                assertThat(bodyType.hasUnresolvableGenerics()).isFalse();
                assertThat(bodyType.getGenerics()[0].toClass()).isEqualTo(Integer.class);
              });
    });
  }

  @Test
  @SuppressWarnings("resource")
  void testEventClassWithInterface() {
    ApplicationContext ac = new AnnotationConfigApplicationContext(AuditableListener.class);

    AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
    ac.publishEvent(event);
    assertThat(ac.getBean(AuditableListener.class).events.contains(event)).isTrue();
  }

  @Test
  @SuppressWarnings("resource")
  void testProgrammaticEventListener() {
    List<Auditable> events = new ArrayList<>();
    ApplicationListener<AuditablePayloadEvent<String>> listener = events::add;
    ApplicationListener<AuditablePayloadEvent<Integer>> mismatch = (event -> event.getPayload().intValue());

    ConfigurableApplicationContext ac = new GenericApplicationContext();
    ac.addApplicationListener(listener);
    ac.addApplicationListener(mismatch);
    ac.refresh();

    AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
    ac.publishEvent(event);
    assertThat(events.contains(event)).isTrue();
  }

  @Test
  @SuppressWarnings("resource")
  void testProgrammaticPayloadListener() {
    List<String> events = new ArrayList<>();
    ApplicationListener<PayloadApplicationEvent<String>> listener = ApplicationListener.forPayload(events::add);
    ApplicationListener<PayloadApplicationEvent<Integer>> mismatch = ApplicationListener.forPayload(Integer::intValue);

    ConfigurableApplicationContext ac = new GenericApplicationContext();
    ac.addApplicationListener(listener);
    ac.addApplicationListener(mismatch);
    ac.refresh();

    AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
    ac.publishEvent(event);
    assertThat(events.contains(event.getPayload())).isTrue();
  }

  public interface Auditable {
  }

  @SuppressWarnings("serial")
  public static class AuditablePayloadEvent<T> extends PayloadApplicationEvent<T> implements Auditable {

    public AuditablePayloadEvent(Object source, T payload) {
      super(source, payload);
    }
  }

  @Component
  public static class AuditableListener {

    public final List<Auditable> events = new ArrayList<>();

    @EventListener
    public void onEvent(Auditable event) {
      events.add(event);
    }
  }

  static class NumberHolder<T extends Number> {

    public NumberHolder(T number) {
    }

  }

}
