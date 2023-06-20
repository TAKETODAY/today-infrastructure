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

package cn.taketoday.aot.agent;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RecordedInvocation}.
 *
 * @author Brian Clozel
 */
class RecordedInvocationTests {

  private RecordedInvocation staticInvocation;

  private RecordedInvocation instanceInvocation;

  @BeforeEach
  void setup() throws Exception {
    staticInvocation = RecordedInvocation.of(InstrumentedMethod.CLASS_FORNAME)
            .withArgument(String.class.getCanonicalName())
            .returnValue(String.class)
            .build();
    instanceInvocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHOD)
            .onInstance(String.class)
            .withArguments("toString", new Class[0])
            .returnValue(String.class.getMethod("toString"))
            .build();
  }

  @Test
  void buildValidStaticInvocation() {
    assertThat(staticInvocation.getHintType()).isEqualTo(HintType.REFLECTION);
    assertThat(staticInvocation.getMethodReference()).isEqualTo(InstrumentedMethod.CLASS_FORNAME.methodReference());
    assertThat(staticInvocation.getArguments()).containsOnly(String.class.getCanonicalName());
    assertThat(staticInvocation.getArgumentTypes()).containsOnly(TypeReference.of(String.class));
    assertThat((Class<?>) staticInvocation.getReturnValue()).isEqualTo(String.class);
    assertThat(staticInvocation.isStatic()).isTrue();
  }

  @Test
  void staticInvocationShouldThrowWhenGetInstance() {
    assertThatThrownBy(staticInvocation::getInstance).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(staticInvocation::getInstanceTypeReference).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void staticInvocationToString() {
    assertThat(staticInvocation.toString()).contains("ReflectionHints", "java.lang.Class#forName", "[java.lang.String]");
  }

  @Test
  void buildValidInstanceInvocation() throws Exception {
    assertThat(instanceInvocation.getHintType()).isEqualTo(HintType.REFLECTION);
    assertThat(instanceInvocation.getMethodReference()).isEqualTo(InstrumentedMethod.CLASS_GETMETHOD.methodReference());
    assertThat(instanceInvocation.getArguments()).containsOnly("toString", new Class[0]);
    assertThat(instanceInvocation.getArgumentTypes()).containsOnly(TypeReference.of(String.class), TypeReference.of(Class[].class));
    Method toString = String.class.getMethod("toString");
    assertThat((Method) instanceInvocation.getReturnValue()).isEqualTo(toString);
    assertThat(instanceInvocation.isStatic()).isFalse();
    assertThat((Class<?>) instanceInvocation.getInstance()).isEqualTo(String.class);
    assertThat(instanceInvocation.getInstanceTypeReference()).isEqualTo(TypeReference.of(Class.class));
  }

  @Test
  void instanceInvocationToString() {
    assertThat(instanceInvocation.toString()).contains("ReflectionHints", "", "java.lang.Class#getMethod",
            "java.lang.String", "[toString, [Ljava.lang.Class;");
  }

}
