/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.annotation;

import org.assertj.core.api.ThrowableTypeAssert;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

import cn.taketoday.util.ConcurrentReferenceHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MissingMergedAnnotation}.
 *
 * @author Phillip Webb
 */
class MissingMergedAnnotationTests {

  private final MergedAnnotation<?> missing = MissingMergedAnnotation.getInstance();

  @Test
  void getTypeThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(this.missing::getType);
  }

  @Test
  void metaTypesReturnsEmptyList() {
    assertThat(this.missing.getMetaTypes()).isEmpty();
  }

  @Test
  void isPresentReturnsFalse() {
    assertThat(this.missing.isPresent()).isFalse();
  }

  @Test
  void isDirectlyPresentReturnsFalse() {
    assertThat(this.missing.isDirectlyPresent()).isFalse();
  }

  @Test
  void isMetaPresentReturnsFalse() {
    assertThat(this.missing.isMetaPresent()).isFalse();
  }

  @Test
  void getDistanceReturnsMinusOne() {
    assertThat(this.missing.getDistance()).isEqualTo(-1);
  }

  @Test
  void getAggregateIndexReturnsMinusOne() {
    assertThat(this.missing.getAggregateIndex()).isEqualTo(-1);
  }

  @Test
  void getSourceReturnsNull() {
    assertThat(this.missing.getSource()).isNull();
  }

  @Test
  void getMetaSourceReturnsNull() {
    assertThat(this.missing.getMetaSource()).isNull();
  }

  @Test
  void getRootReturnsEmptyAnnotation() {
    assertThat(this.missing.getRoot()).isSameAs(this.missing);
  }

  @Test
  void hasNonDefaultValueThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.hasNonDefaultValue("value"));
  }

  @Test
  void hasDefaultValueThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.hasDefaultValue("value"));
  }

  @Test
  void getByteThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getByte("value"));
  }

  @Test
  void getByteArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getByteArray("value"));
  }

  @Test
  void getBooleanThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getBoolean("value"));
  }

  @Test
  void getBooleanArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getBooleanArray("value"));
  }

  @Test
  void getCharThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getChar("value"));
  }

  @Test
  void getCharArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getCharArray("value"));
  }

  @Test
  void getShortThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getShort("value"));
  }

  @Test
  void getShortArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getShortArray("value"));
  }

  @Test
  void getIntThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(() -> this.missing.getInt("value"));
  }

  @Test
  void getIntArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getIntArray("value"));
  }

  @Test
  void getLongThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getLong("value"));
  }

  @Test
  void getLongArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getLongArray("value"));
  }

  @Test
  void getDoubleThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getDouble("value"));
  }

  @Test
  void getDoubleArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getDoubleArray("value"));
  }

  @Test
  void getFloatThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getFloat("value"));
  }

  @Test
  void getFloatArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getFloatArray("value"));
  }

  @Test
  void getStringThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getString("value"));
  }

  @Test
  void getStringArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getStringArray("value"));
  }

  @Test
  void getClassThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getClass("value"));
  }

  @Test
  void getClassArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getClassArray("value"));
  }

  @Test
  void getEnumThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getEnum("value", TestEnum.class));
  }

  @Test
  void getEnumArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getEnumArray("value", TestEnum.class));
  }

  @Test
  void getAnnotationThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getAnnotation("value", TestAnnotation.class));
  }

  @Test
  void getAnnotationArrayThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.getAnnotationArray("value", TestAnnotation.class));
  }

  @Test
  void getValueReturnsEmpty() {
    assertThat(this.missing.getValue(Integer.class)).isEmpty();
  }

  @Test
  void getDefaultValueReturnsEmpty() {
    assertThat(this.missing.getDefaultValue("value", Integer.class)).isEmpty();
  }

  @Test
  void isSynthesizableReturnsFalse() {
    assertThat(this.missing.isSynthesizable()).isFalse();
  }

  @Test
  void synthesizeThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(() -> this.missing.synthesize());
  }

  @Test
  void synthesizeWithPredicateWhenPredicateMatchesThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.synthesize(annotation -> true));
  }

  @Test
  void synthesizeWithPredicateWhenPredicateDoesNotMatchReturnsEmpty() {
    assertThat(this.missing.synthesize(annotation -> false)).isEmpty();
  }

  @Test
  void toStringReturnsString() {
    assertThat(this.missing.toString()).isEqualTo("(missing)");
  }

  @Test
  void asAnnotationAttributesReturnsNewAnnotationAttributes() {
    AnnotationAttributes attributes = this.missing.asAnnotationAttributes();
    assertThat(attributes).isEmpty();
    assertThat(this.missing.asAnnotationAttributes()).isNotSameAs(attributes);
  }

  @Test
  void asMapReturnsEmptyMap() {
    Map<String, Object> map = this.missing.asMap();
    assertThat(map).isSameAs(Collections.EMPTY_MAP);
  }

  @Test
  void asMapWithFactoryReturnsNewMapFromFactory() {
    Map<String, Object> map = this.missing.asMap(annotation -> new ConcurrentReferenceHashMap<>());
    assertThat(map).isInstanceOf(ConcurrentReferenceHashMap.class);
  }

  private static ThrowableTypeAssert<NoSuchElementException> assertThatNoSuchElementException() {
    return assertThatExceptionOfType(NoSuchElementException.class);
  }

  private enum TestEnum {
    ONE, TWO, THREE
  }

  @Retention(RetentionPolicy.RUNTIME)
  private @interface TestAnnotation {
  }

}
