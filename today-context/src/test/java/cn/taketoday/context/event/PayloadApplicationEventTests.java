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

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.BeansException;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.PayloadApplicationEvent;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.stereotype.Component;

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
  void testEventClassWithPayloadType() {
    ConfigurableApplicationContext ac = new AnnotationConfigApplicationContext(NumberHolderListener.class);

    PayloadApplicationEvent<NumberHolder<Integer>> event = new PayloadApplicationEvent<>(this,
            new NumberHolder<>(42), ResolvableType.fromClassWithGenerics(NumberHolder.class, Integer.class));
    ac.publishEvent(event);
    assertThat(ac.getBean(NumberHolderListener.class).events.contains(event.getPayload())).isTrue();
    ac.close();
  }

  @Test
  void testEventClassWithPayloadTypeOnParentContext() {
    ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext(NumberHolderListener.class);
    ConfigurableApplicationContext ac = new GenericApplicationContext(parent);
    ac.refresh();

    PayloadApplicationEvent<NumberHolder<Integer>> event = new PayloadApplicationEvent<>(this,
            new NumberHolder<>(42), ResolvableType.fromClassWithGenerics(NumberHolder.class, Integer.class));
    ac.publishEvent(event);
    assertThat(parent.getBean(NumberHolderListener.class).events.contains(event.getPayload())).isTrue();
    ac.close();
    parent.close();
  }

  @Test
  void testPayloadObjectWithPayloadType() {
    final Object payload = new NumberHolder<>(42);

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(NumberHolderListener.class) {
      @Override
      protected void finishRefresh() throws BeansException {
        super.finishRefresh();
        // This is not recommended: use publishEvent(new PayloadApplicationEvent(...)) instead
        publishEvent(payload, ResolvableType.fromClassWithGenerics(NumberHolder.class, Integer.class));
      }
    };

    assertThat(ac.getBean(NumberHolderListener.class).events.contains(payload)).isTrue();
    ac.close();
  }

  @Test
  void testPayloadObjectWithPayloadTypeOnParentContext() {
    final Object payload = new NumberHolder<>(42);

    ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext(NumberHolderListener.class);
    ConfigurableApplicationContext ac = new GenericApplicationContext(parent) {
      @Override
      protected void finishRefresh() throws BeansException {
        super.finishRefresh();
        // This is not recommended: use publishEvent(new PayloadApplicationEvent(...)) instead
        publishEvent(payload, ResolvableType.fromClassWithGenerics(NumberHolder.class, Integer.class));
      }
    };
    ac.refresh();

    assertThat(parent.getBean(NumberHolderListener.class).events.contains(payload)).isTrue();
    ac.close();
    parent.close();
  }

  @Test
  void testEventClassWithInterface() {
    ConfigurableApplicationContext ac = new AnnotationConfigApplicationContext(AuditableListener.class);

    AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
    ac.publishEvent(event);
    assertThat(ac.getBean(AuditableListener.class).events.contains(event)).isTrue();
    ac.close();
  }

  @Test
  void testEventClassWithInterfaceOnParentContext() {
    ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext(AuditableListener.class);
    ConfigurableApplicationContext ac = new GenericApplicationContext(parent);
    ac.refresh();

    AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
    ac.publishEvent(event);
    assertThat(parent.getBean(AuditableListener.class).events.contains(event)).isTrue();
    ac.close();
    parent.close();
  }

  @Test
  void testProgrammaticEventListener() {
    List<Auditable> events = new ArrayList<>();
    ApplicationListener<AuditablePayloadEvent<String>> listener = events::add;
    ApplicationListener<AuditablePayloadEvent<Integer>> mismatch = (event -> event.getPayload());

    ConfigurableApplicationContext ac = new GenericApplicationContext();
    ac.addApplicationListener(listener);
    ac.addApplicationListener(mismatch);
    ac.refresh();

    AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
    ac.publishEvent(event);
    assertThat(events.contains(event)).isTrue();
    ac.close();
  }

  @Test
  void testProgrammaticEventListenerOnParentContext() {
    List<Auditable> events = new ArrayList<>();
    ApplicationListener<AuditablePayloadEvent<String>> listener = events::add;
    ApplicationListener<AuditablePayloadEvent<Integer>> mismatch = (event -> event.getPayload());

    ConfigurableApplicationContext parent = new GenericApplicationContext();
    parent.addApplicationListener(listener);
    parent.addApplicationListener(mismatch);
    parent.refresh();
    ConfigurableApplicationContext ac = new GenericApplicationContext(parent);
    ac.refresh();

    AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
    ac.publishEvent(event);
    assertThat(events.contains(event)).isTrue();
    ac.close();
    parent.close();
  }

  @Test
  void testProgrammaticPayloadListener() {
    List<String> events = new ArrayList<>();
    ApplicationListener<PayloadApplicationEvent<String>> listener = ApplicationListener.forPayload(events::add);
    ApplicationListener<PayloadApplicationEvent<Integer>> mismatch = ApplicationListener.forPayload(Integer::intValue);

    ConfigurableApplicationContext ac = new GenericApplicationContext();
    ac.addApplicationListener(listener);
    ac.addApplicationListener(mismatch);
    ac.refresh();

    String payload = "xyz";
    ac.publishEvent(payload);
    assertThat(events.contains(payload)).isTrue();
    ac.close();
  }

  @Test
  void testProgrammaticPayloadListenerOnParentContext() {
    List<String> events = new ArrayList<>();
    ApplicationListener<PayloadApplicationEvent<String>> listener = ApplicationListener.forPayload(events::add);
    ApplicationListener<PayloadApplicationEvent<Integer>> mismatch = ApplicationListener.forPayload(Integer::intValue);

    ConfigurableApplicationContext parent = new GenericApplicationContext();
    parent.addApplicationListener(listener);
    parent.addApplicationListener(mismatch);
    parent.refresh();
    ConfigurableApplicationContext ac = new GenericApplicationContext(parent);
    ac.refresh();

    String payload = "xyz";
    ac.publishEvent(payload);
    assertThat(events.contains(payload)).isTrue();
    ac.close();
    parent.close();
  }

  @Test
  void testPlainPayloadListener() {
    ConfigurableApplicationContext ac = new AnnotationConfigApplicationContext(PlainPayloadListener.class);

    String payload = "xyz";
    ac.publishEvent(payload);
    assertThat(ac.getBean(PlainPayloadListener.class).events.contains(payload)).isTrue();
    ac.close();
  }

  @Test
  void testPlainPayloadListenerOnParentContext() {
    ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext(PlainPayloadListener.class);
    ConfigurableApplicationContext ac = new GenericApplicationContext(parent);
    ac.refresh();

    String payload = "xyz";
    ac.publishEvent(payload);
    assertThat(parent.getBean(PlainPayloadListener.class).events.contains(payload)).isTrue();
    ac.close();
    parent.close();
  }

  static class NumberHolder<T extends Number> {

    public NumberHolder(T number) {
    }
  }

  @Component
  public static class NumberHolderListener {

    public final List<NumberHolder<Integer>> events = new ArrayList<>();

    @EventListener
    public void onEvent(NumberHolder<Integer> event) {
      events.add(event);
    }
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

  @Component
  public static class PlainPayloadListener {

    public final List<String> events = new ArrayList<>();

    @EventListener
    public void onEvent(String event) {
      events.add(event);
    }
  }

}
