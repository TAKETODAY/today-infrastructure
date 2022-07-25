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

package cn.taketoday.context.expression;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 17:15
 */
class MethodBasedEvaluationContextTests {

  private final ParameterNameDiscoverer paramDiscover = new DefaultParameterNameDiscoverer();

  @Test
  public void simpleArguments() {
    Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", String.class, Boolean.class);
    MethodBasedEvaluationContext context = createEvaluationContext(method, "test", true);

    assertThat(context.lookupVariable("a0")).isEqualTo("test");
    assertThat(context.lookupVariable("p0")).isEqualTo("test");
    assertThat(context.lookupVariable("foo")).isEqualTo("test");

    assertThat(context.lookupVariable("a1")).asInstanceOf(BOOLEAN).isTrue();
    assertThat(context.lookupVariable("p1")).asInstanceOf(BOOLEAN).isTrue();
    assertThat(context.lookupVariable("flag")).asInstanceOf(BOOLEAN).isTrue();

    assertThat(context.lookupVariable("a2")).isNull();
    assertThat(context.lookupVariable("p2")).isNull();
  }

  @Test
  public void nullArgument() {
    Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", String.class, Boolean.class);
    MethodBasedEvaluationContext context = createEvaluationContext(method, null, null);

    assertThat(context.lookupVariable("a0")).isNull();
    assertThat(context.lookupVariable("p0")).isNull();
    assertThat(context.lookupVariable("foo")).isNull();

    assertThat(context.lookupVariable("a1")).isNull();
    assertThat(context.lookupVariable("p1")).isNull();
    assertThat(context.lookupVariable("flag")).isNull();
  }

  @Test
  public void varArgEmpty() {
    Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", Boolean.class, String[].class);
    MethodBasedEvaluationContext context = createEvaluationContext(method, new Object[] { null });

    assertThat(context.lookupVariable("a0")).isNull();
    assertThat(context.lookupVariable("p0")).isNull();
    assertThat(context.lookupVariable("flag")).isNull();

    assertThat(context.lookupVariable("a1")).isNull();
    assertThat(context.lookupVariable("p1")).isNull();
    assertThat(context.lookupVariable("vararg")).isNull();
  }

  @Test
  public void varArgNull() {
    Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", Boolean.class, String[].class);
    MethodBasedEvaluationContext context = createEvaluationContext(method, null, null);

    assertThat(context.lookupVariable("a0")).isNull();
    assertThat(context.lookupVariable("p0")).isNull();
    assertThat(context.lookupVariable("flag")).isNull();

    assertThat(context.lookupVariable("a1")).isNull();
    assertThat(context.lookupVariable("p1")).isNull();
    assertThat(context.lookupVariable("vararg")).isNull();
  }

  @Test
  public void varArgSingle() {
    Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", Boolean.class, String[].class);
    MethodBasedEvaluationContext context = createEvaluationContext(method, null, "hello");

    assertThat(context.lookupVariable("a0")).isNull();
    assertThat(context.lookupVariable("p0")).isNull();
    assertThat(context.lookupVariable("flag")).isNull();

    assertThat(context.lookupVariable("a1")).isEqualTo("hello");
    assertThat(context.lookupVariable("p1")).isEqualTo("hello");
    assertThat(context.lookupVariable("vararg")).isEqualTo("hello");
  }

  @Test
  public void varArgMultiple() {
    Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", Boolean.class, String[].class);
    MethodBasedEvaluationContext context = createEvaluationContext(method, null, "hello", "hi");

    assertThat(context.lookupVariable("a0")).isNull();
    assertThat(context.lookupVariable("p0")).isNull();
    assertThat(context.lookupVariable("flag")).isNull();

    assertThat(context.lookupVariable("a1")).isEqualTo(new Object[] { "hello", "hi" });
    assertThat(context.lookupVariable("p1")).isEqualTo(new Object[] { "hello", "hi" });
    assertThat(context.lookupVariable("vararg")).isEqualTo(new Object[] { "hello", "hi" });
  }

  private MethodBasedEvaluationContext createEvaluationContext(Method method, Object... args) {
    return new MethodBasedEvaluationContext(this, method, args, this.paramDiscover);
  }

  @SuppressWarnings("unused")
  private static class SampleMethods {

    private void hello(String foo, Boolean flag) {
    }

    private void hello(Boolean flag, String... vararg) {
    }
  }

}
