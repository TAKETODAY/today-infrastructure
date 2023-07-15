/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.junit.jupiter.params.provider.Arguments;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.annotation.DurationStyle;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link StringToDurationConverter}.
 *
 * @author Phillip Webb
 */
class StringToDurationConverterTests {

  @ConversionServiceTest
  void convertWhenIso8601ShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, "pt20.345s")).isEqualTo(Duration.parse("pt20.345s"));
    assertThat(convert(conversionService, "PT20.345S")).isEqualTo(Duration.parse("PT20.345S"));
    assertThat(convert(conversionService, "PT15M")).isEqualTo(Duration.parse("PT15M"));
    assertThat(convert(conversionService, "+PT15M")).isEqualTo(Duration.parse("PT15M"));
    assertThat(convert(conversionService, "PT10H")).isEqualTo(Duration.parse("PT10H"));
    assertThat(convert(conversionService, "P2D")).isEqualTo(Duration.parse("P2D"));
    assertThat(convert(conversionService, "P2DT3H4M")).isEqualTo(Duration.parse("P2DT3H4M"));
    assertThat(convert(conversionService, "-PT6H3M")).isEqualTo(Duration.parse("-PT6H3M"));
    assertThat(convert(conversionService, "-PT-6H+3M")).isEqualTo(Duration.parse("-PT-6H+3M"));
  }

  @ConversionServiceTest
  void convertWhenSimpleNanosShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, "10ns")).hasNanos(10);
    assertThat(convert(conversionService, "10NS")).hasNanos(10);
    assertThat(convert(conversionService, "+10ns")).hasNanos(10);
    assertThat(convert(conversionService, "-10ns")).hasNanos(-10);
  }

  @ConversionServiceTest
  void convertWhenSimpleMicrosShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, "10us")).hasNanos(10000);
    assertThat(convert(conversionService, "10US")).hasNanos(10000);
    assertThat(convert(conversionService, "+10us")).hasNanos(10000);
    assertThat(convert(conversionService, "-10us")).hasNanos(-10000);
  }

  @ConversionServiceTest
  void convertWhenSimpleMillisShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, "10ms")).hasMillis(10);
    assertThat(convert(conversionService, "10MS")).hasMillis(10);
    assertThat(convert(conversionService, "+10ms")).hasMillis(10);
    assertThat(convert(conversionService, "-10ms")).hasMillis(-10);
  }

  @ConversionServiceTest
  void convertWhenSimpleSecondsShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, "10s")).hasSeconds(10);
    assertThat(convert(conversionService, "10S")).hasSeconds(10);
    assertThat(convert(conversionService, "+10s")).hasSeconds(10);
    assertThat(convert(conversionService, "-10s")).hasSeconds(-10);
  }

  @ConversionServiceTest
  void convertWhenSimpleMinutesShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, "10m")).hasMinutes(10);
    assertThat(convert(conversionService, "10M")).hasMinutes(10);
    assertThat(convert(conversionService, "+10m")).hasMinutes(10);
    assertThat(convert(conversionService, "-10m")).hasMinutes(-10);
  }

  @ConversionServiceTest
  void convertWhenSimpleHoursShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, "10h")).hasHours(10);
    assertThat(convert(conversionService, "10H")).hasHours(10);
    assertThat(convert(conversionService, "+10h")).hasHours(10);
    assertThat(convert(conversionService, "-10h")).hasHours(-10);
  }

  @ConversionServiceTest
  void convertWhenSimpleDaysShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, "10d")).hasDays(10);
    assertThat(convert(conversionService, "10D")).hasDays(10);
    assertThat(convert(conversionService, "+10d")).hasDays(10);
    assertThat(convert(conversionService, "-10d")).hasDays(-10);
  }

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, "10")).hasMillis(10);
    assertThat(convert(conversionService, "+10")).hasMillis(10);
    assertThat(convert(conversionService, "-10")).hasMillis(-10);
  }

  @ConversionServiceTest
  void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDuration(ConversionService conversionService) {
    assertThat(convert(conversionService, "10", ChronoUnit.SECONDS, null)).hasSeconds(10);
    assertThat(convert(conversionService, "+10", ChronoUnit.SECONDS, null)).hasSeconds(10);
    assertThat(convert(conversionService, "-10", ChronoUnit.SECONDS, null)).hasSeconds(-10);
  }

  @ConversionServiceTest
  void convertWhenBadFormatShouldThrowException(ConversionService conversionService) {
    assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() -> convert(conversionService, "10foo"))
            .havingRootCause().withMessageContaining("'10foo' is not a valid duration");
  }

  @ConversionServiceTest
  void convertWhenStyleMismatchShouldThrowException(ConversionService conversionService) {
    assertThatExceptionOfType(ConversionFailedException.class)
            .isThrownBy(() -> convert(conversionService, "10s", null, DurationStyle.ISO8601));
  }

  @ConversionServiceTest
  void convertWhenEmptyShouldReturnNull(ConversionService conversionService) {
    assertThat(convert(conversionService, "")).isNull();
  }

  @Nullable
  private Duration convert(ConversionService conversionService, String source) {
    return conversionService.convert(source, Duration.class);
  }

  private Duration convert(
          ConversionService conversionService,
          @Nullable String source, @Nullable ChronoUnit unit, @Nullable DurationStyle style) {
    return (Duration) conversionService.convert(source, TypeDescriptor.forObject(source),
            MockDurationTypeDescriptor.get(unit, style));
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with(new StringToDurationConverter());
  }

}
