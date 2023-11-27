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
package cn.taketoday.framework.availability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serial;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.bytecode.proxy.Enhancer;
import cn.taketoday.bytecode.proxy.MethodInterceptor;
import cn.taketoday.bytecode.proxy.MethodProxy;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.logging.Logger;
import cn.taketoday.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationAvailabilityBean}
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class ApplicationAvailabilityBeanTests {

  private AnnotationConfigApplicationContext context;

  private ApplicationAvailabilityBean availability;

  private MockLog log;

  @BeforeEach
  void setup() {
    this.context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    this.availability = this.context.getBean(ApplicationAvailabilityBean.class);
    this.log = this.context.getBean(MockLog.class);
  }

  @Test
  void getLivenessStateWhenNoEventHasBeenPublishedReturnsDefaultState() {
    assertThat(this.availability.getLivenessState()).isEqualTo(LivenessState.BROKEN);
  }

  @Test
  void getLivenessStateWhenEventHasBeenPublishedReturnsPublishedState() {
    AvailabilityChangeEvent.publish(this.context, LivenessState.CORRECT);
    assertThat(this.availability.getLivenessState()).isEqualTo(LivenessState.CORRECT);
  }

  @Test
  void getReadinessStateWhenNoEventHasBeenPublishedReturnsDefaultState() {
    assertThat(this.availability.getReadinessState()).isEqualTo(ReadinessState.REFUSING_TRAFFIC);
  }

  @Test
  void getReadinessStateWhenEventHasBeenPublishedReturnsPublishedState() {
    AvailabilityChangeEvent.publish(this.context, ReadinessState.ACCEPTING_TRAFFIC);
    assertThat(this.availability.getReadinessState()).isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);
  }

  @Test
  void getStateWhenNoEventHasBeenPublishedReturnsDefaultState() {
    assertThat(this.availability.getState(TestState.class)).isNull();
    assertThat(this.availability.getState(TestState.class, TestState.ONE)).isEqualTo(TestState.ONE);
  }

  @Test
  void getStateWhenEventHasBeenPublishedReturnsPublishedState() {
    AvailabilityChangeEvent.publish(this.context, TestState.TWO);
    assertThat(this.availability.getState(TestState.class)).isEqualTo(TestState.TWO);
    assertThat(this.availability.getState(TestState.class, TestState.ONE)).isEqualTo(TestState.TWO);
  }

  @Test
  void getLastChangeEventWhenNoEventHasBeenPublishedReturnsDefaultState() {
    assertThat(this.availability.getLastChangeEvent(TestState.class)).isNull();
  }

  @Test
  void getLastChangeEventWhenEventHasBeenPublishedReturnsPublishedState() {
    AvailabilityChangeEvent.publish(this.context, TestState.TWO);
    assertThat(this.availability.getLastChangeEvent(TestState.class)).isNotNull();
  }

  @Test
  void stateChangesAreLogged() {
    AvailabilityChangeEvent.publish(this.context, LivenessState.CORRECT);
    assertThat(this.log.getLogged()).contains("Application availability state LivenessState changed to CORRECT");
    AvailabilityChangeEvent.publish(this.context, LivenessState.BROKEN);
    assertThat(this.log.getLogged())
            .contains("Application availability state LivenessState changed from CORRECT to BROKEN");
  }

  @Test
  void stateChangesAreLoggedWithExceptionSource() {
    AvailabilityChangeEvent.publish(this.context, new IOException("connection error"), LivenessState.BROKEN);
    List<String> logged = this.log.getLogged();
    assertThat(logged).contains("Application availability state LivenessState changed to BROKEN: "
            + "java.io.IOException: connection error");
  }

  @Test
  void stateChangesAreLoggedWithOtherSource() {
    AvailabilityChangeEvent.publish(this.context, new CustomEventSource(), LivenessState.BROKEN);
    assertThat(this.log.getLogged()).contains(
            "Application availability state LivenessState changed to BROKEN: " + CustomEventSource.class.getName());
  }

  enum TestState implements AvailabilityState {

    ONE {
      @Override
      public String test() {
        return "spring";
      }
    },

    TWO {
      @Override
      public String test() {
        return "boot";
      }
    };

    abstract String test();

  }

  static class CustomEventSource {

  }

  @Configuration
  static class TestConfiguration {

    @Component
    MockLog mockLog() {
      return (MockLog) Enhancer.create(MockLog.class, new MethodInterceptor() {
        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
          if (method.getName().equals("isDebugEnabled")) {
            return true;
          }
          if (!Modifier.isAbstract(method.getModifiers())) {
            return proxy.invokeSuper(obj, args);
          }
          return null;
        }
      });
    }

    @Component
    ApplicationAvailabilityBean applicationAvailabilityBean(MockLog log) {
      return new ApplicationAvailabilityBean(log);
    }

  }

  static abstract class MockLog extends Logger {
    @Serial
    private static final long serialVersionUID = 1L;

    List<String> logged = new ArrayList<>();

    MockLog() { super(true); }

    @Override
    public void debug(String msg) {
      super.debug(msg);
      logged.add(msg);
    }

    @Override
    public void debug(Object message) {
      super.debug(message);
      logged.add(message.toString());
    }

    public List<String> getLogged() {
      return logged;
    }

  }

}
