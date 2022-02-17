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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import cn.taketoday.aop.proxy.ProxyFactory;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterFactory;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.number.NumberStyleFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Kazuki Shimizu
 * @author Sam Brannen
 */
public class FormattingConversionServiceTests {

  private FormattingConversionService formattingService;

  @BeforeEach
  public void setUp() {
    formattingService = new FormattingConversionService();
    DefaultConversionService.addDefaultConverters(formattingService);
    LocaleContextHolder.setLocale(Locale.US);
  }

  @AfterEach
  public void tearDown() {
    LocaleContextHolder.setLocale(null);
  }

  @Test
  public void formatFieldForTypeWithFormatter() {
    formattingService.addFormatterForFieldType(Number.class, new NumberStyleFormatter());
    String formatted = formattingService.convert(3, String.class);
    assertThat(formatted).isEqualTo("3");
    Integer i = formattingService.convert("3", Integer.class);
    assertThat(i).isEqualTo(3);
  }

  @Test
  public void printNull() {
    formattingService.addFormatterForFieldType(Number.class, new NumberStyleFormatter());
    assertThat(formattingService.convert(null, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class))).isEqualTo("");
  }

  @Test
  public void parseNull() {
    formattingService.addFormatterForFieldType(Number.class, new NumberStyleFormatter());
    assertThat(formattingService
            .convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
  }

  @Test
  public void parseEmptyString() {
    formattingService.addFormatterForFieldType(Number.class, new NumberStyleFormatter());
    assertThat(formattingService.convert("", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
  }

  @Test
  public void parseBlankString() {
    formattingService.addFormatterForFieldType(Number.class, new NumberStyleFormatter());
    assertThat(formattingService.convert("     ", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
  }

  @Test
  public void parseParserReturnsNull() {
    formattingService.addFormatterForFieldType(Integer.class, new NullReturningFormatter());
    assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
            formattingService.convert("1", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
  }

  @Test
  public void parseNullPrimitiveProperty() {
    formattingService.addFormatterForFieldType(Integer.class, new NumberStyleFormatter());
    assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
            formattingService.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(int.class)));
  }

  @Test
  public void printNullDefault() {
    assertThat(formattingService
            .convert(null, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class))).isNull();
  }

  @Test
  public void parseNullDefault() {
    assertThat(formattingService
            .convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
  }

  @Test
  public void parseEmptyStringDefault() {
    assertThat(formattingService.convert("", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
  }

  @Test
  public void introspectedFormatter() {
    formattingService.addFormatter(new NumberStyleFormatter("#,#00.0#"));
    assertThat(formattingService.convert(123, String.class)).isEqualTo("123.0");
    assertThat(formattingService.convert("123.0", Integer.class)).isEqualTo(123);
  }

  @Test
  public void introspectedPrinter() {
    formattingService.addPrinter(new NumberStyleFormatter("#,#00.0#"));
    assertThat(formattingService.convert(123, String.class)).isEqualTo("123.0");
    assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
                    formattingService.convert("123.0", Integer.class))
            .withCauseInstanceOf(NumberFormatException.class);
  }

  @Test
  public void introspectedParser() {
    formattingService.addParser(new NumberStyleFormatter("#,#00.0#"));
    assertThat(formattingService.convert("123.0", Integer.class)).isEqualTo(123);
    assertThat(formattingService.convert(123, String.class)).isEqualTo("123");
  }

  @Test
  public void proxiedFormatter() {
    Formatter<?> formatter = new NumberStyleFormatter();
    formattingService.addFormatter((Formatter<?>) new ProxyFactory(formatter).getProxy());
    assertThat(formattingService.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
  }

  @Test
  public void introspectedConverter() {
    formattingService.addConverter(new IntegerConverter());
    assertThat(formattingService.convert("1", Integer.class)).isEqualTo(Integer.valueOf(1));
  }

  @Test
  public void proxiedConverter() {
    Converter<?, ?> converter = new IntegerConverter();
    formattingService.addConverter((Converter<?, ?>) new ProxyFactory(converter).getProxy());
    assertThat(formattingService.convert("1", Integer.class)).isEqualTo(Integer.valueOf(1));
  }

  @Test
  public void introspectedConverterFactory() {
    formattingService.addConverterFactory(new IntegerConverterFactory());
    assertThat(formattingService.convert("1", Integer.class)).isEqualTo(Integer.valueOf(1));
  }

  @Test
  public void proxiedConverterFactory() {
    ConverterFactory<?, ?> converterFactory = new IntegerConverterFactory();
    formattingService.addConverterFactory((ConverterFactory<?, ?>) new ProxyFactory(converterFactory).getProxy());
    assertThat(formattingService.convert("1", Integer.class)).isEqualTo(Integer.valueOf(1));
  }

  public static class NullReturningFormatter implements Formatter<Integer> {

    @Override
    public String print(Integer object, Locale locale) {
      return null;
    }

    @Override
    public Integer parse(String text, Locale locale) {
      return null;
    }
  }

  private static class IntegerConverter implements Converter<String, Integer> {

    @Override
    public Integer convert(String source) {
      return Integer.parseInt(source);
    }
  }

  private static class IntegerConverterFactory implements ConverterFactory<String, Number> {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
      if (Integer.class == targetType) {
        return (Converter<String, T>) new IntegerConverter();
      }
      else {
        throw new IllegalStateException();
      }
    }
  }

}
