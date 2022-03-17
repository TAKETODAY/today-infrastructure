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
package cn.taketoday.framework.availability;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.event.EventListener;
import cn.taketoday.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AvailabilityChangeEvent}.
 *
 * @author Phillip Webb
 */
class AvailabilityChangeEventTests {

  private Object source = new Object();

  @Test
  void createWhenStateIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new AvailabilityChangeEvent<>(this.source, null))
            .withMessage("Payload is required");
  }

  @Test
  void getStateReturnsState() {
    LivenessState state = LivenessState.CORRECT;
    AvailabilityChangeEvent<LivenessState> event = new AvailabilityChangeEvent<>(this.source, state);
    assertThat(event.getState()).isEqualTo(state);
  }

  @Test
  void getResolvableType() {
    LivenessState state = LivenessState.CORRECT;
    AvailabilityChangeEvent<LivenessState> event = new AvailabilityChangeEvent<>(this.source, state);
    ResolvableType type = event.getResolvableType();
    assertThat(type.resolve()).isEqualTo(AvailabilityChangeEvent.class);
    assertThat(type.resolveGeneric()).isEqualTo(LivenessState.class);
  }

  @Test
  void getResolvableTypeWhenSubclassedEnum() {
    SubClassedEnum state = SubClassedEnum.TWO;
    AvailabilityChangeEvent<SubClassedEnum> event = new AvailabilityChangeEvent<>(this.source, state);
    ResolvableType type = event.getResolvableType();
    assertThat(type.resolve()).isEqualTo(AvailabilityChangeEvent.class);
    assertThat(type.resolveGeneric()).isEqualTo(SubClassedEnum.class);
  }

  @Test
  void publishPublishesEvent() {
    ApplicationContext context = mock(ApplicationContext.class);
    AvailabilityState state = LivenessState.CORRECT;
    AvailabilityChangeEvent.publish(context, state);
    ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
    then(context).should().publishEvent(captor.capture());
    AvailabilityChangeEvent<?> event = (AvailabilityChangeEvent<?>) captor.getValue();
    assertThat(event.getSource()).isEqualTo(context);
    assertThat(event.getState()).isEqualTo(state);
  }

  @Test
  void publishEvenToContextConsidersGenericType() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
    AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
    AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
  }

  enum SubClassedEnum implements AvailabilityState {

    ONE {
      @Override
      String getDescription() {
        return "I have been overridden";
      }

    },

    TWO {
      @Override
      String getDescription() {
        return "I have also been overridden";
      }

    };

    abstract String getDescription();

  }

  @Configuration
  static class Config {

    @EventListener
    void onLivenessAvailabilityChange(AvailabilityChangeEvent<LivenessState> event) {
      assertThat(event.getState()).isInstanceOf(LivenessState.class).isEqualTo(LivenessState.CORRECT);
    }

  }

}
