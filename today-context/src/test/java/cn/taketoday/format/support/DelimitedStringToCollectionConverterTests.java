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

package cn.taketoday.format.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.annotation.Delimiter;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DelimitedStringToCollectionConverter}.
 *
 * @author Phillip Webb
 */
class DelimitedStringToCollectionConverterTests {

  @ConversionServiceTest
  void canConvertFromStringToCollectionShouldReturnTrue(ConversionService conversionService) {
    assertThat(conversionService.canConvert(String.class, Collection.class)).isTrue();
  }

  @ConversionServiceTest
  void matchesWhenTargetIsNotAnnotatedShouldReturnTrue(ConversionService conversionService) {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "noAnnotation"), 0);
    assertThat(new DelimitedStringToCollectionConverter(conversionService).matches(sourceType, targetType))
            .isTrue();
  }

  @ConversionServiceTest
  void matchesWhenHasAnnotationAndNoElementTypeShouldReturnTrue(ConversionService conversionService) {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "noElementType"), 0);
    assertThat(new DelimitedStringToCollectionConverter(conversionService).matches(sourceType, targetType))
            .isTrue();
  }

  @ConversionServiceTest
  void matchesWhenHasAnnotationAndNonConvertibleElementTypeShouldReturnFalse(ConversionService conversionService) {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor
            .nested(ReflectionUtils.findField(Values.class, "nonConvertibleElementType"), 0);
    assertThat(new DelimitedStringToCollectionConverter(conversionService).matches(sourceType, targetType))
            .isFalse();
  }

  @ConversionServiceTest
  @SuppressWarnings("unchecked")
  void convertWhenHasNoElementTypeShouldReturnTrimmedString(ConversionService conversionService) {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "noElementType"), 0);
    Collection<String> converted = (Collection<String>) conversionService.convert(" a |  b| c  ", sourceType,
            targetType);
    assertThat(converted).containsExactly("a", "b", "c");
  }

  @ConversionServiceTest
  @SuppressWarnings("unchecked")
  void convertWhenHasDelimiterOfNoneShouldReturnWholeString(ConversionService conversionService) {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "delimiterNone"), 0);
    List<String> converted = (List<String>) conversionService.convert("a,b,c", sourceType, targetType);
    assertThat(converted).containsExactly("a,b,c");
  }

  @SuppressWarnings("unchecked")
  @ConversionServiceTest
  void convertWhenHasCollectionObjectTypeShouldUseCollectionObjectType(ConversionService conversionService) {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "specificType"), 0);
    MyCustomList<String> converted = (MyCustomList<String>) conversionService.convert("a*b", sourceType,
            targetType);
    assertThat(converted).containsExactly("a", "b");
  }

  @Test
  void matchesWhenHasAnnotationAndConvertibleElementTypeShouldReturnTrue() {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor
            .nested(ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
    assertThat(new DelimitedStringToCollectionConverter(new ApplicationConversionService()).matches(sourceType,
            targetType)).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  void convertWhenHasConvertibleElementTypeShouldReturnConvertedType() {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor
            .nested(ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
    List<Integer> converted = (List<Integer>) new ApplicationConversionService().convert(" 1 |  2| 3  ", sourceType,
            targetType);
    assertThat(converted).containsExactly(1, 2, 3);
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments
            .with((service) -> service.addConverter(new DelimitedStringToCollectionConverter(service)));
  }

  static class Values {

    List<String> noAnnotation;

    @SuppressWarnings("rawtypes")
    @Delimiter("|")
    List noElementType;

    @Delimiter("|")
    List<Integer> convertibleElementType;

    @Delimiter("|")
    List<NonConvertible> nonConvertibleElementType;

    @Delimiter(Delimiter.NONE)
    List<String> delimiterNone;

    @Delimiter("*")
    MyCustomList<String> specificType;

  }

  static class NonConvertible {

  }

  @SuppressWarnings("serial")
  static class MyCustomList<E> extends LinkedList<E> {

  }

}
