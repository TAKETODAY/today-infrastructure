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

package infra.format.support;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.ResolvableType;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalConverter;
import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.core.conversion.ConverterFactory;
import infra.core.conversion.ConverterNotFoundException;
import infra.core.conversion.GenericConverter;
import infra.format.Formatter;
import infra.format.FormatterRegistry;
import infra.format.Parser;
import infra.format.Printer;
import infra.format.support.ApplicationConversionService.ConverterBeanAdapter;
import infra.format.support.ApplicationConversionService.ParserBeanAdapter;
import infra.format.support.ApplicationConversionService.PrinterBeanAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link ApplicationConversionService}.
 *
 * @author Phillip Webb
 */
class ApplicationConversionServiceTests {

  private final FormatterRegistry registry = mock(FormatterRegistry.class,
          withSettings().extraInterfaces(ConversionService.class));

  @Test
  void addBeansWhenHasGenericConverterBeanAddConverter() {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
            ExampleGenericConverter.class)) {
      ApplicationConversionService.addBeans(this.registry, context);
      then(this.registry).should().addConverter(context.getBean(ExampleGenericConverter.class));
      then(this.registry).shouldHaveNoMoreInteractions();
    }
  }

  @Test
  void addBeansWhenHasConverterBeanAddConverter() {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExampleConverter.class)) {
      ApplicationConversionService.addBeans(this.registry, context);
      then(this.registry).should().addConverter(context.getBean(ExampleConverter.class));
      then(this.registry).shouldHaveNoMoreInteractions();
    }
  }

  @Test
  void addBeansWhenHasFormatterBeanAddsOnlyFormatter() {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExampleFormatter.class)) {
      ApplicationConversionService.addBeans(this.registry, context);
      then(this.registry).should().addFormatter(context.getBean(ExampleFormatter.class));
      then(this.registry).shouldHaveNoMoreInteractions();
    }
  }

  @Test
  void addBeansWhenHasPrinterBeanAddPrinter() {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExamplePrinter.class)) {
      ApplicationConversionService.addBeans(this.registry, context);
      then(this.registry).should().addPrinter(context.getBean(ExamplePrinter.class));
      then(this.registry).shouldHaveNoMoreInteractions();
    }
  }

  @Test
  void addBeansWhenHasParserBeanAddParser() {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExampleParser.class)) {
      ApplicationConversionService.addBeans(this.registry, context);
      then(this.registry).should().addParser(context.getBean(ExampleParser.class));
      then(this.registry).shouldHaveNoMoreInteractions();
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void addBeansWhenHasConverterBeanMethodAddConverter() {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
            ConverterBeanMethodConfiguration.class)) {
      Converter<String, Integer> converter = (Converter<String, Integer>) context.getBean("converter");
      willThrow(IllegalArgumentException.class).given(this.registry).addConverter(converter);
      ApplicationConversionService.addBeans(this.registry, context);
      then(this.registry).should().addConverter(any(ConverterBeanAdapter.class));
      then(this.registry).shouldHaveNoMoreInteractions();
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void addBeansWhenHasPrinterBeanMethodAddPrinter() {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
            PrinterBeanMethodConfiguration.class)) {
      Printer<Integer> printer = (Printer<Integer>) context.getBean("printer");
      willThrow(IllegalArgumentException.class).given(this.registry).addPrinter(printer);
      ApplicationConversionService.addBeans(this.registry, context);
      then(this.registry).should(never()).addPrinter(printer);
      then(this.registry).should().addConverter(any(PrinterBeanAdapter.class));
      then(this.registry).shouldHaveNoMoreInteractions();
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void addBeansWhenHasParserBeanMethodAddParser() {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
            ParserBeanMethodConfiguration.class)) {
      Parser<Integer> parser = (Parser<Integer>) context.getBean("parser");
      willThrow(IllegalArgumentException.class).given(this.registry).addParser(parser);
      ApplicationConversionService.addBeans(this.registry, context);
      then(this.registry).should(never()).addParser(parser);
      then(this.registry).should().addConverter(any(ParserBeanAdapter.class));
      then(this.registry).shouldHaveNoMoreInteractions();
    }
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
    ApplicationConversionService instance = (ApplicationConversionService) ApplicationConversionService
            .getSharedInstance();
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

  @Test
  void addPrinterBeanWithTypeConvertsUsingTypeInformation() {
    FormattingConversionService conversionService = new FormattingConversionService();
    Printer<?> printer = (object, locale) -> object.toString().toUpperCase(locale);
    ApplicationConversionService.addBean(conversionService, printer,
            ResolvableType.forClassWithGenerics(Printer.class, ExampleRecord.class));
    assertThat(conversionService.convert(new ExampleRecord("test"), String.class)).isEqualTo("TEST");
    assertThatExceptionOfType(ConverterNotFoundException.class)
            .isThrownBy(() -> conversionService.convert(new OtherRecord("test"), String.class));
    assertThatIllegalArgumentException().isThrownBy(() -> conversionService.addPrinter(printer))
            .withMessageContaining("Unable to extract");
  }

  @Test
  void addParserBeanWithTypeConvertsUsingTypeInformation() {
    FormattingConversionService conversionService = new FormattingConversionService();
    Parser<?> parser = (text, locale) -> new ExampleRecord(text.toString());
    ApplicationConversionService.addBean(conversionService, parser,
            ResolvableType.forClassWithGenerics(Parser.class, ExampleRecord.class));
    assertThat(conversionService.convert("test", ExampleRecord.class)).isEqualTo(new ExampleRecord("test"));
    assertThatExceptionOfType(ConverterNotFoundException.class)
            .isThrownBy(() -> conversionService.convert("test", OtherRecord.class));
    assertThatIllegalArgumentException().isThrownBy(() -> conversionService.addParser(parser))
            .withMessageContaining("Unable to extract");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void addFormatterBeanWithTypeConvertsUsingTypeInformation() {
    FormattingConversionService conversionService = new FormattingConversionService();
    Formatter<?> formatter = new Formatter() {

      @Override
      public String print(Object object, Locale locale) {
        return object.toString().toUpperCase(locale);
      }

      @Override
      public Object parse(String text, Locale locale) throws ParseException {
        return new ExampleRecord(text.toString());
      }

    };
    ApplicationConversionService.addBean(conversionService, formatter,
            ResolvableType.forClassWithGenerics(Formatter.class, ExampleRecord.class));
    assertThat(conversionService.convert(new ExampleRecord("test"), String.class)).isEqualTo("TEST");
    assertThat(conversionService.convert("test", ExampleRecord.class)).isEqualTo(new ExampleRecord("test"));
    assertThatExceptionOfType(ConverterNotFoundException.class)
            .isThrownBy(() -> conversionService.convert(new OtherRecord("test"), String.class));
    assertThatExceptionOfType(ConverterNotFoundException.class)
            .isThrownBy(() -> conversionService.convert("test", OtherRecord.class));
    assertThatIllegalArgumentException().isThrownBy(() -> conversionService.addFormatter(formatter))
            .withMessageContaining("Unable to extract");
  }

  @Test
  void addConverterBeanWithTypeConvertsUsingTypeInformation() {
    FormattingConversionService conversionService = new FormattingConversionService();
    Converter<?, ?> converter = (source) -> new ExampleRecord(source.toString());
    ApplicationConversionService.addBean(conversionService, converter,
            ResolvableType.forClassWithGenerics(Converter.class, CharSequence.class, ExampleRecord.class));
    assertThat(conversionService.convert("test", ExampleRecord.class)).isEqualTo(new ExampleRecord("test"));
    assertThat(conversionService.convert(new StringBuilder("test"), ExampleRecord.class))
            .isEqualTo(new ExampleRecord("test"));
    assertThatExceptionOfType(ConverterNotFoundException.class)
            .isThrownBy(() -> conversionService.convert("test", OtherRecord.class));
    assertThatIllegalArgumentException().isThrownBy(() -> conversionService.addConverter(converter))
            .withMessageContaining("Unable to determine");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void addConverterBeanWithTypeWhenConditionalChecksCondition() {
    FormattingConversionService conversionService = new FormattingConversionService();
    ConditionalConverterConverter<?, ?> converter = new ConditionalConverterConverter() {

      @Override
      public Object convert(Object source) {
        return new ExampleRecord(source.toString());
      }

      @Override
      public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return sourceType.getType() != StringBuilder.class;
      }

    };
    ApplicationConversionService.addBean(conversionService, converter,
            ResolvableType.forClassWithGenerics(Converter.class, CharSequence.class, ExampleRecord.class));
    assertThat(conversionService.convert("test", ExampleRecord.class)).isEqualTo(new ExampleRecord("test"));
    assertThatExceptionOfType(ConverterNotFoundException.class)
            .isThrownBy(() -> conversionService.convert(new StringBuilder("test"), ExampleRecord.class));
    assertThatIllegalArgumentException().isThrownBy(() -> conversionService.addConverter(converter))
            .withMessageContaining("Unable to determine");
  }

  @Test
  @SuppressWarnings("unchecked")
  void addConverterBeanWithTypeWhenNullSourceCanConvertToOptionEmpty() {
    FormattingConversionService conversionService = new FormattingConversionService();
    Converter<?, ?> converter = (source) -> new ExampleRecord(source.toString());
    ApplicationConversionService.addBean(conversionService, converter,
            ResolvableType.forClassWithGenerics(Converter.class, CharSequence.class, ExampleRecord.class));
    assertThat(conversionService.convert(null, ExampleRecord.class)).isNull();
    assertThat(conversionService.convert(null, Optional.class)).isEmpty();
  }

  @Test
  @SuppressWarnings("rawtypes")
  void addConverterFactoryBeanWithTypeConvertsUsingTypeInformation() {
    FormattingConversionService conversionService = new FormattingConversionService();
    Converter converter = (source) -> new ExampleRecord(source.toString());
    ConverterFactory converterFactory  = new ConverterFactory() {
      @Override
      public Converter getConverter(Class targetType) {
        return converter;
      }
    };
    ApplicationConversionService.addBean(conversionService, converterFactory,
            ResolvableType.forClassWithGenerics(ConverterFactory.class, CharSequence.class, ExampleRecord.class));
    assertThat(conversionService.convert("test", ExampleRecord.class)).isEqualTo(new ExampleRecord("test"));
    assertThat(conversionService.convert(new StringBuilder("test"), ExampleRecord.class))
            .isEqualTo(new ExampleRecord("test"));
    assertThatExceptionOfType(ConverterNotFoundException.class)
            .isThrownBy(() -> conversionService.convert("test", OtherRecord.class));
    assertThatIllegalArgumentException().isThrownBy(() -> conversionService.addConverterFactory(converterFactory))
            .withMessageContaining("Unable to determine");
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

  @Configuration
  static class ConverterBeanMethodConfiguration {

    @Bean
    Converter<String, Integer> converter() {
      return Integer::valueOf;
    }

  }

  @Configuration
  static class PrinterBeanMethodConfiguration {

    @Bean
    Printer<Integer> printer() {
      return (object, locale) -> object.toString();
    }

  }

  @Configuration
  static class ParserBeanMethodConfiguration {

    @Bean
    Parser<Integer> parser() {
      return (text, locale) -> Integer.valueOf(text);
    }

  }

  record ExampleRecord(String value) {

    @Override
    public final String toString() {
      return value();
    }

  }

  record OtherRecord(String value) {

  }

  interface ConditionalConverterConverter<S, T> extends Converter<S, T>, ConditionalConverter {

  }

}
