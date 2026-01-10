/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.annotation;

import org.assertj.core.api.ThrowableTypeAssert;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import infra.util.ConcurrentReferenceHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MissingMergedAnnotation}.
 *
 * @author Phillip Webb
 */
class MissingMergedAnnotationTests {

  private final MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;

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
    assertThat(this.missing.getValue(Integer.class)).isNull();
  }

  @Test
  void getDefaultValueReturnsEmpty() {
    assertThat(this.missing.getDefaultValue("value", Integer.class)).isNull();
  }

  @Test
  void synthesizeThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(this.missing::synthesize);
  }

  @Test
  void synthesizeWithPredicateWhenPredicateMatchesThrowsNoSuchElementException() {
    assertThatNoSuchElementException().isThrownBy(
            () -> this.missing.synthesize(annotation -> true));
  }

  @Test
  void synthesizeWithPredicateWhenPredicateDoesNotMatchReturnsEmpty() {
    assertThat(this.missing.synthesize(annotation -> false)).isNull();
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

  @Test
  void getInstanceReturnsSameInstance() {
    MergedAnnotation<?> instance1 = MissingMergedAnnotation.INSTANCE;
    MergedAnnotation<?> instance2 = MissingMergedAnnotation.INSTANCE;
    assertThat(instance1).isSameAs(instance2);
  }

  @Test
  void filterAttributesReturnsSameInstance() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    MergedAnnotation<?> filtered = missing.filterAttributes(name -> true);
    assertThat(filtered).isSameAs(missing);
  }

  @Test
  void withNonMergedAttributesReturnsSameInstance() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    MergedAnnotation<?> nonMerged = missing.withNonMergedAttributes();
    assertThat(nonMerged).isSameAs(missing);
  }

  @Test
  void getValueReturnsNull() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    assertThat(missing.getValue("test")).isNull();
    assertThat(missing.getValue("test", String.class)).isNull();
  }

  @Test
  void getValueWithTypeReturnsDefaultValue() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    assertThat(missing.getValue("test", String.class, "default")).isEqualTo("default");
  }

  @Test
  void getDefaultValueReturnsNull() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    assertThat(missing.getDefaultValue("test")).isNull();
    assertThat(missing.getDefaultValue("test", String.class)).isNull();
  }

  @Test
  void asAnnotationAttributesReturnsEmptyAttributes() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    AnnotationAttributes attributes = missing.asAnnotationAttributes();
    assertThat(attributes).isEmpty();
    assertThat(attributes.annotationType()).isNull();
  }

  @Test
  void asMapWithAdaptationsReturnsEmptyMap() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    Map<String, Object> map = missing.asMap(MergedAnnotation.Adapt.CLASS_TO_STRING);
    assertThat(map).isSameAs(Collections.EMPTY_MAP);
  }

  @Test
  void asAnnotationAttributesWithAdaptationsReturnsEmptyAttributes() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    AnnotationAttributes attributes = missing.asAnnotationAttributes(MergedAnnotation.Adapt.CLASS_TO_STRING);
    assertThat(attributes).isEmpty();
  }

  @Test
  void getMetaTypesReturnsEmptyList() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    List<Class<? extends Annotation>> metaTypes = missing.getMetaTypes();
    assertThat(metaTypes).isEmpty();
  }

  @Test
  void getRootReturnsSameInstance() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    MergedAnnotation<?> root = missing.getRoot();
    assertThat(root).isSameAs(missing);
  }

  @Test
  void getAttributeValueThrowsNoSuchElementException() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(() -> missing.getString("test"))
            .withMessageContaining("Unable to get attribute 'test'");
  }

  @Test
  void synthesizeWithPredicateReturnsNullWhenPredicateDoesNotMatch() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    var result = missing.synthesize(a -> false);
    assertThat(result).isNull();
  }

  @Test
  void synthesizeWithPredicateThrowsNoSuchElementExceptionWhenPredicateMatches() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(() -> missing.synthesize(a -> true))
            .withMessage("Unable to synthesize missing annotation");
  }

  @Test
  void toStringReturnsMissing() {
    MergedAnnotation<?> missing = MissingMergedAnnotation.INSTANCE;
    String string = missing.toString();
    assertThat(string).isEqualTo("(missing)");
  }

  @Test
  void createSynthesizedAnnotation() {
    var missing = MissingMergedAnnotation.INSTANCE;
    assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(missing::createSynthesizedAnnotation)
            .withMessage("Unable to synthesize missing annotation");
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
