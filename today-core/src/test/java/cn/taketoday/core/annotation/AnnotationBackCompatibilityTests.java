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

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 * @since 4.0
 */
class AnnotationBackCompatibilityTests {

  @Test
  void multiplRoutesToMetaAnnotation() {
    Class<WithMetaMetaTestAnnotation1AndMetaTestAnnotation2> source = WithMetaMetaTestAnnotation1AndMetaTestAnnotation2.class;
    // Merged annotation chooses lowest depth
    MergedAnnotation<TestAnnotation> mergedAnnotation = MergedAnnotations.from(source).get(TestAnnotation.class);
    assertThat(mergedAnnotation.getStringValue()).isEqualTo("testAndMetaTest");
    // AnnotatedElementUtils finds first
    TestAnnotation previousVersion = AnnotatedElementUtils.getMergedAnnotation(source, TestAnnotation.class);
    assertThat(previousVersion.value()).isEqualTo("metaTest");
  }

  @Test
  void defaultValue() {
    DefaultValueAnnotation synthesized = MergedAnnotations.from(WithDefaultValue.class).get(DefaultValueAnnotation.class).synthesize();
    assertThat(AnnotationUtils.isSynthesizedAnnotation(synthesized)).as("synthesized annotation").isTrue();
    Object defaultValue = AnnotationUtils.getDefaultValue(synthesized, "enumValue");
    assertThat(defaultValue).isEqualTo(TestEnum.ONE);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface TestAnnotation {

    String value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @TestAnnotation("metaTest")
  @interface MetaTestAnnotation {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @TestAnnotation("testAndMetaTest")
  @MetaTestAnnotation
  @interface TestAndMetaTestAnnotation {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @MetaTestAnnotation
  @interface MetaMetaTestAnnotation {
  }

  @MetaMetaTestAnnotation
  @TestAndMetaTestAnnotation
  static class WithMetaMetaTestAnnotation1AndMetaTestAnnotation2 {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface DefaultValueAnnotation {

    @AliasFor("enumAlias")
    TestEnum enumValue() default TestEnum.ONE;

    @AliasFor("enumValue")
    TestEnum enumAlias() default TestEnum.ONE;

  }

  @DefaultValueAnnotation
  static class WithDefaultValue {

  }

  static enum TestEnum {

    ONE,

    TWO

  }

}
