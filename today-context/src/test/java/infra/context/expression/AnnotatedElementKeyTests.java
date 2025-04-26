/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;

import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 21:35
 */
class AnnotatedElementKeyTests {

  private Method method;

  @BeforeEach
  void setUpMethod(TestInfo testInfo) {
    this.method = ReflectionUtils.findMethod(getClass(), testInfo.getTestMethod().get().getName());
  }

  @Test
  void sameInstanceEquals() {
    AnnotatedElementKey instance = new AnnotatedElementKey(this.method, getClass());

    assertKeyEquals(instance, instance);
  }

  @Test
  void equals() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, getClass());
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, getClass());

    assertKeyEquals(first, second);
  }

  @Test
  void equalsNoTarget() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, null);
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, null);

    assertKeyEquals(first, second);
  }

  @Test
  void noTargetClassNotEquals() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, getClass());
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, null);

    assertThat(first.equals(second)).isFalse();
  }

  @Test
  void differentMethodNotEquals() throws NoSuchMethodException {
    Method otherMethod = ReflectionUtils.findMethod(getClass(), "noTargetClassNotEquals");
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, getClass());
    AnnotatedElementKey second = new AnnotatedElementKey(otherMethod, getClass());

    assertThat(first.equals(second)).isFalse();
    assertThat(first.hashCode()).isNotEqualTo(second.hashCode());
  }

  @Test
  void differentTargetClassNotEquals() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, String.class);
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, Integer.class);

    assertThat(first.equals(second)).isFalse();
    assertThat(first.hashCode()).isNotEqualTo(second.hashCode());
  }

  @Test
  void compareToWithSameElement() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, getClass());
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, getClass());

    assertThat(first.compareTo(second)).isZero();
  }

  @Test
  void compareToWithDifferentElement() throws NoSuchMethodException {
    Method otherMethod = ReflectionUtils.findMethod(getClass(), "noTargetClassNotEquals");
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, getClass());
    AnnotatedElementKey second = new AnnotatedElementKey(otherMethod, getClass());

    assertThat(first.compareTo(second)).isNotZero();
  }

  @Test
  void compareToWithNullAndNonNullTarget() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, null);
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, getClass());

    assertThat(second.compareTo(first)).isGreaterThan(0);
    assertThat(first.compareTo(second)).isEqualTo(0);
  }

  @Test
  void compareToWithDifferentTargetClass() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, String.class);
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, Integer.class);

    assertThat(first.compareTo(second)).isNotZero();
  }

  @Test
  void toStringWithTargetClass() {
    AnnotatedElementKey key = new AnnotatedElementKey(this.method, getClass());
    assertThat(key.toString()).contains(method.toString(), getClass().toString());
  }

  @Test
  void toStringWithoutTargetClass() {
    AnnotatedElementKey key = new AnnotatedElementKey(this.method, null);
    assertThat(key.toString()).contains(method.toString())
            .doesNotContain("on null");
  }

  @Test
  void nullAnnotatedElementThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new AnnotatedElementKey(null, getClass()));
  }

  @Test
  void compareToWithSameElementAndDifferentTargetClass() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, String.class);
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, Integer.class);

    int result = first.compareTo(second);
    assertThat(result).isNotZero();
    assertThat(second.compareTo(first)).isEqualTo(-result);
  }

  @Test
  void compareToWithBothNullTargets() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, null);
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, null);

    assertThat(first.compareTo(second)).isZero();
  }

  @Test
  void equalsWithDifferentTypes() {
    AnnotatedElementKey key = new AnnotatedElementKey(this.method, getClass());
    assertThat(key).isNotEqualTo("different type");
  }

  @Test
  void equalsWithNull() {
    AnnotatedElementKey key = new AnnotatedElementKey(this.method, getClass());
    assertThat(key).isNotEqualTo(null);
  }

  @Test
  void hashCodeConsistentWithEquals() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, getClass());
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, getClass());
    AnnotatedElementKey third = new AnnotatedElementKey(this.method, String.class);

    assertThat(first.hashCode()).isEqualTo(second.hashCode());
    assertThat(first.hashCode()).isNotEqualTo(third.hashCode());
  }

  @Test
  void toStringWithNullTargetClass() {
    AnnotatedElementKey key = new AnnotatedElementKey(this.method, null);
    String toString = key.toString();
    assertThat(toString).contains(this.method.toString());
    assertThat(toString).doesNotContain("null");
  }

  private void assertKeyEquals(AnnotatedElementKey first, AnnotatedElementKey second) {
    assertThat(second).isEqualTo(first);
    assertThat(second.hashCode()).isEqualTo(first.hashCode());
  }

}
