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

package cn.taketoday.format.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.annotation.Delimiter;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DelimitedStringToArrayConverter}.
 *
 * @author Phillip Webb
 */
class DelimitedStringToArrayConverterTests {

  @ConversionServiceTest
  void canConvertFromStringToArrayShouldReturnTrue(ConversionService conversionService) {
    assertThat(conversionService.canConvert(String.class, String[].class)).isTrue();
  }

  @ConversionServiceTest
  void matchesWhenTargetIsNotAnnotatedShouldReturnTrue(ConversionService conversionService) {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "noAnnotation"), 0);
    assertThat(new DelimitedStringToArrayConverter(conversionService).matches(sourceType, targetType)).isTrue();
  }

  @ConversionServiceTest
  void matchesWhenHasAnnotationAndNonConvertibleElementTypeShouldReturnFalse(ConversionService conversionService) {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor
            .nested(ReflectionUtils.findField(Values.class, "nonConvertibleElementType"), 0);
    assertThat(new DelimitedStringToArrayConverter(conversionService).matches(sourceType, targetType)).isFalse();
  }

  @ConversionServiceTest
  void convertWhenHasDelimiterOfNoneShouldReturnWholeString(ConversionService conversionService) {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "delimiterNone"), 0);
    String[] converted = (String[]) conversionService.convert("a,b,c", sourceType, targetType);
    assertThat(converted).containsExactly("a,b,c");
  }

  @Test
  void matchesWhenHasAnnotationAndConvertibleElementTypeShouldReturnTrue() {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor
            .nested(ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
    assertThat(
            new DelimitedStringToArrayConverter(new ApplicationConversionService()).matches(sourceType, targetType))
            .isTrue();
  }

  @Test
  void convertWhenHasConvertibleElementTypeShouldReturnConvertedType() {
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor
            .nested(ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
    Integer[] converted = (Integer[]) new ApplicationConversionService().convert(" 1 |  2| 3  ", sourceType,
            targetType);
    assertThat(converted).containsExactly(1, 2, 3);
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments
            .with((service) -> service.addConverter(new DelimitedStringToArrayConverter(service)));
  }

  static class Values {

    List<String> noAnnotation;

    @Delimiter("|")
    Integer[] convertibleElementType;

    @Delimiter("|")
    NonConvertible[] nonConvertibleElementType;

    @Delimiter(Delimiter.NONE)
    String[] delimiterNone;

  }

  static class NonConvertible {

  }

  static class MyCustomList<E> extends LinkedList<E> {

  }

}
