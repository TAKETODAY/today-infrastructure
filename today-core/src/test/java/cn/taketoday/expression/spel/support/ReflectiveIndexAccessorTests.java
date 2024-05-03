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

package cn.taketoday.expression.spel.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.util.ReflectionUtils;
import example.Color;
import example.FruitMap;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/3 12:58
 */
class ReflectiveIndexAccessorTests {

  @Test
  void nonexistentReadMethod() {
    Class<?> targetType = getClass();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ReflectiveIndexAccessor(targetType, int.class, "bogus"))
            .withMessage("Failed to find public read-method 'bogus(int)' in class '%s'.", targetType.getCanonicalName());
  }

  @Test
  void nonPublicReadMethod() {
    Class<?> targetType = PrivateReadMethod.class;
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ReflectiveIndexAccessor(targetType, int.class, "get"))
            .withMessage("Failed to find public read-method 'get(int)' in class '%s'.", targetType.getCanonicalName());
  }

  @Test
  void nonPublicWriteMethod() {
    Class<?> targetType = PrivateWriteMethod.class;
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ReflectiveIndexAccessor(targetType, int.class, "get", "set"))
            .withMessage("Failed to find public write-method 'set(int, java.lang.Object)' in class '%s'.",
                    targetType.getCanonicalName());
  }

  @Test
  void nonPublicDeclaringClass() {
    Class<?> targetType = NonPublicTargetType.class;
    Method readMethod = ReflectionUtils.findMethod(targetType, "get", int.class);
    ReflectiveIndexAccessor accessor = new ReflectiveIndexAccessor(targetType, int.class, "get");

    assertThatIllegalStateException()
            .isThrownBy(() -> accessor.generateCode(mock(), mock(), mock()))
            .withMessage("Failed to find public declaring class for read-method: %s", readMethod);
  }

  @Test
  void publicReadAndWriteMethods() {
    FruitMap fruitMap = new FruitMap();
    EvaluationContext context = mock();
    ReflectiveIndexAccessor accessor =
            new ReflectiveIndexAccessor(FruitMap.class, Color.class, "getFruit", "setFruit");

    assertThat(accessor.getSpecificTargetClasses()).containsOnly(FruitMap.class);

    assertThat(accessor.canRead(context, this, Color.RED)).isFalse();
    assertThat(accessor.canRead(context, fruitMap, this)).isFalse();
    assertThat(accessor.canRead(context, fruitMap, Color.RED)).isTrue();
    assertThat(accessor.read(context, fruitMap, Color.RED)).extracting(TypedValue::getValue).isEqualTo("cherry");

    assertThat(accessor.canWrite(context, this, Color.RED)).isFalse();
    assertThat(accessor.canWrite(context, fruitMap, this)).isFalse();
    assertThat(accessor.canWrite(context, fruitMap, Color.RED)).isTrue();
    accessor.write(context, fruitMap, Color.RED, "strawberry");
    assertThat(fruitMap.getFruit(Color.RED)).isEqualTo("strawberry");
    assertThat(accessor.read(context, fruitMap, Color.RED)).extracting(TypedValue::getValue).isEqualTo("strawberry");

    assertThat(accessor.isCompilable()).isTrue();
    assertThat(accessor.getIndexedValueType()).isEqualTo(String.class);
    assertThatNoException().isThrownBy(() -> accessor.generateCode(mock(), mock(), mock()));
  }


  public static class PrivateReadMethod {
    Object get(int i) {
      return "foo";
    }
  }

  public static class PrivateWriteMethod {
    public Object get(int i) {
      return "foo";
    }

    void set(int i, String value) {
      // no-op
    }
  }

  static class NonPublicTargetType {
    public Object get(int i) {
      return "foo";
    }
  }

}