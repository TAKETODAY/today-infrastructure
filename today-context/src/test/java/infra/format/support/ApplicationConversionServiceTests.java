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

package infra.format.support;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.core.TypeDescriptor;
import infra.core.conversion.Converter;
import infra.core.conversion.GenericConverter;
import infra.format.Formatter;
import infra.format.FormatterRegistry;
import infra.format.Parser;
import infra.format.Printer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link ApplicationConversionService}.
 *
 * @author Phillip Webb
 */
class ApplicationConversionServiceTests {

  private final FormatterRegistry registry = mock(FormatterRegistry.class);

  @Test
  void addBeansWhenHasGenericConverterBeanAddConverter() {
    var context = new AnnotationConfigApplicationContext(ExampleGenericConverter.class);
    ApplicationConversionService.addBeans(this.registry, context);
    verify(this.registry).addConverter(context.getBean(ExampleGenericConverter.class));
    verifyNoMoreInteractions(this.registry);
  }

  @Test
  void addBeansWhenHasConverterBeanAddConverter() {
    var context = new AnnotationConfigApplicationContext(ExampleConverter.class);
    ApplicationConversionService.addBeans(this.registry, context);
    verify(this.registry).addConverter(context.getBean(ExampleConverter.class));
    verifyNoMoreInteractions(this.registry);
  }

  @Test
  void addBeansWhenHasFormatterBeanAddsOnlyFormatter() {
    var context = new AnnotationConfigApplicationContext(ExampleFormatter.class);
    ApplicationConversionService.addBeans(this.registry, context);
    verify(this.registry).addFormatter(context.getBean(ExampleFormatter.class));
    verifyNoMoreInteractions(this.registry);

  }

  @Test
  void addBeansWhenHasPrinterBeanAddPrinter() {
    var context = new AnnotationConfigApplicationContext(ExamplePrinter.class);
    ApplicationConversionService.addBeans(this.registry, context);
    verify(this.registry).addPrinter(context.getBean(ExamplePrinter.class));
    verifyNoMoreInteractions(this.registry);
  }

  @Test
  void addBeansWhenHasParserBeanAddParser() {
    ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExampleParser.class);
    ApplicationConversionService.addBeans(this.registry, context);
    verify(this.registry).addParser(context.getBean(ExampleParser.class));
    verifyNoMoreInteractions(this.registry);

  }

  @Test
  void isConvertViaObjectSourceTypeWhenObjectSourceReturnsTrue() {
    // Uses ObjectToCollectionConverter
    ApplicationConversionService conversionService = new ApplicationConversionService();
    TypeDescriptor sourceType = TypeDescriptor.valueOf(Long.class);
    TypeDescriptor targetType = TypeDescriptor.valueOf(List.class);
    assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
    assertThat(conversionService.isConvertViaObjectSourceType(sourceType, targetType)).isTrue();
  }

  @Test
  void isConvertViaObjectSourceTypeWhenNotObjectSourceReturnsFalse() {
    // Uses StringToCollectionConverter
    ApplicationConversionService conversionService = new ApplicationConversionService();
    TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
    TypeDescriptor targetType = TypeDescriptor.valueOf(List.class);
    assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
    assertThat(conversionService.isConvertViaObjectSourceType(sourceType, targetType)).isFalse();
  }

  @Test
  void sharedInstanceCannotBeModified() {
    ApplicationConversionService instance = ApplicationConversionService.getSharedInstance();
    assertUnmodifiableExceptionThrown(() -> instance.addPrinter(null));
    assertUnmodifiableExceptionThrown(() -> instance.addParser(null));
    assertUnmodifiableExceptionThrown(() -> instance.addFormatter(null));
    assertUnmodifiableExceptionThrown(() -> instance.addFormatterForFieldType(null, null));
    assertUnmodifiableExceptionThrown(() -> instance.addConverter((Converter<?, ?>) null));
    assertUnmodifiableExceptionThrown(() -> instance.addFormatterForFieldType(null, null, null));
    assertUnmodifiableExceptionThrown(() -> instance.addFormatterForFieldAnnotation(null));
    assertUnmodifiableExceptionThrown(() -> instance.addConverter(null, null, null));
    assertUnmodifiableExceptionThrown(() -> instance.addConverter((GenericConverter) null));
    assertUnmodifiableExceptionThrown(() -> instance.addConverterFactory(null));
    assertUnmodifiableExceptionThrown(() -> instance.removeConvertible(null, null));
  }

  private void assertUnmodifiableExceptionThrown(ThrowingCallable throwingCallable) {
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(throwingCallable)
            .withMessage("This ApplicationConversionService cannot be modified");
  }

  static class ExampleGenericConverter implements GenericConverter {

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return null;
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      return null;
    }

  }

  static class ExampleConverter implements Converter<String, Integer> {

    @Override
    public Integer convert(String source) {
      return null;
    }

  }

  static class ExampleFormatter implements Formatter<Integer> {

    @Override
    public String print(Integer object, Locale locale) {
      return null;
    }

    @Override
    public Integer parse(String text, Locale locale) throws ParseException {
      return null;
    }

  }

  static class ExampleParser implements Parser<Integer> {

    @Override
    public Integer parse(String text, Locale locale) throws ParseException {
      return null;
    }

  }

  static class ExamplePrinter implements Printer<Integer> {

    @Override
    public String print(Integer object, Locale locale) {
      return null;
    }

  }

}
