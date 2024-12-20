/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.beans.factory.BeanFactory;
import infra.core.StringValueResolver;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.core.conversion.ConverterFactory;
import infra.core.conversion.ConverterRegistry;
import infra.core.conversion.GenericConverter;
import infra.core.conversion.GenericConverter.ConvertiblePair;
import infra.core.conversion.support.ConfigurableConversionService;
import infra.core.conversion.support.DefaultConversionService;
import infra.format.AnnotationFormatterFactory;
import infra.format.Formatter;
import infra.format.FormatterRegistry;
import infra.format.Parser;
import infra.format.Printer;
import infra.lang.Nullable;

/**
 * A specialization of {@link FormattingConversionService} configured by default with
 * converters and formatters appropriate for most applications.
 * <p>
 * Designed for direct instantiation but also exposes the static
 * {@link #addApplicationConverters} and
 * {@link #addApplicationFormatters(FormatterRegistry)} utility methods for ad-hoc use
 * against registry instance.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ApplicationConversionService extends FormattingConversionService {

  @Nullable
  private static volatile ApplicationConversionService sharedInstance;

  private final boolean unmodifiable;

  public ApplicationConversionService() {
    this(null);
  }

  public ApplicationConversionService(@Nullable StringValueResolver embeddedValueResolver) {
    this(embeddedValueResolver, false);
  }

  private ApplicationConversionService(@Nullable StringValueResolver embeddedValueResolver, boolean unmodifiable) {
    if (embeddedValueResolver != null) {
      setEmbeddedValueResolver(embeddedValueResolver);
    }
    configure(this);
    this.unmodifiable = unmodifiable;
  }

  @Override
  public void addPrinter(Printer<?> printer) {
    assertModifiable();
    super.addPrinter(printer);
  }

  @Override
  public void addParser(Parser<?> parser) {
    assertModifiable();
    super.addParser(parser);
  }

  @Override
  public void addFormatter(Formatter<?> formatter) {
    assertModifiable();
    super.addFormatter(formatter);
  }

  @Override
  public void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter) {
    assertModifiable();
    super.addFormatterForFieldType(fieldType, formatter);
  }

  @Override
  public void addConverter(Converter<?, ?> converter) {
    assertModifiable();
    super.addConverter(converter);
  }

  @Override
  public void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser) {
    assertModifiable();
    super.addFormatterForFieldType(fieldType, printer, parser);
  }

  @Override
  public <T extends Annotation> void addFormatterForFieldAnnotation(AnnotationFormatterFactory<T> factory) {
    assertModifiable();
    super.addFormatterForFieldAnnotation(factory);
  }

  @Override
  public <S, T> void addConverter(
          Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter) {
    assertModifiable();
    super.addConverter(sourceType, targetType, converter);
  }

  @Override
  public void addConverter(GenericConverter converter) {
    assertModifiable();
    super.addConverter(converter);
  }

  @Override
  public void addConverterFactory(ConverterFactory<?, ?> factory) {
    assertModifiable();
    super.addConverterFactory(factory);
  }

  @Override
  public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
    assertModifiable();
    super.removeConvertible(sourceType, targetType);
  }

  private void assertModifiable() {
    if (this.unmodifiable) {
      throw new UnsupportedOperationException("This ApplicationConversionService cannot be modified");
    }
  }

  /**
   * Return {@code true} if objects of {@code sourceType} can be converted to the
   * {@code targetType} and the converter has {@code Object.class} as a supported source
   * type.
   *
   * @param sourceType the source type to test
   * @param targetType the target type to test
   * @return if conversion happens via an {@code ObjectTo...} converter
   */
  public boolean isConvertViaObjectSourceType(TypeDescriptor sourceType, TypeDescriptor targetType) {
    GenericConverter converter = getConverter(sourceType, targetType);
    Set<ConvertiblePair> pairs = converter != null ? converter.getConvertibleTypes() : null;
    if (pairs != null) {
      for (ConvertiblePair pair : pairs) {
        if (Object.class.equals(pair.sourceType)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Return a shared default application {@code ConversionService} instance, lazily
   * building it once needed.
   * <p>
   * Note: This method actually returns an {@link ApplicationConversionService}
   * instance. However, the {@code ConversionService} signature has been preserved for
   * binary compatibility.
   *
   * @return the shared {@code ApplicationConversionService} instance (never
   * {@code null})
   */
  public static ApplicationConversionService getSharedInstance() {
    ApplicationConversionService sharedInstance = ApplicationConversionService.sharedInstance;
    if (sharedInstance == null) {
      synchronized(ApplicationConversionService.class) {
        sharedInstance = ApplicationConversionService.sharedInstance;
        if (sharedInstance == null) {
          sharedInstance = new ApplicationConversionService(null, true);
          ApplicationConversionService.sharedInstance = sharedInstance;
        }
      }
    }
    return sharedInstance;
  }

  /**
   * Configure the given {@link FormatterRegistry} with formatters and converters
   * appropriate for most applications.
   *
   * @param registry the registry of converters to add to (must also be castable to
   * ConversionService, e.g. being a {@link ConfigurableConversionService})
   * @throws ClassCastException if the given FormatterRegistry could not be cast to a
   * ConversionService
   */
  public static void configure(FormatterRegistry registry) {
    DefaultConversionService.addDefaultConverters(registry);
    DefaultFormattingConversionService.addDefaultFormatters(registry);
    addApplicationFormatters(registry);
    addApplicationConverters(registry);
  }

  /**
   * Add converters useful for most applications.
   *
   * @param registry the registry of converters to add to (must also be castable to
   * ConversionService, e.g. being a {@link ConfigurableConversionService})
   * @throws ClassCastException if the given ConverterRegistry could not be cast to a
   * ConversionService
   */
  public static void addApplicationConverters(ConverterRegistry registry) {
    addDelimitedStringConverters(registry);
    registry.addConverter(new StringToDurationConverter());
    registry.addConverter(new DurationToStringConverter());
    registry.addConverter(new NumberToDurationConverter());
    registry.addConverter(new DurationToNumberConverter());
    registry.addConverter(new StringToPeriodConverter());
    registry.addConverter(new PeriodToStringConverter());
    registry.addConverter(new NumberToPeriodConverter());
    registry.addConverter(new StringToDataSizeConverter());
    registry.addConverter(new NumberToDataSizeConverter());
    registry.addConverter(new StringToFileConverter());
    registry.addConverter(new InputStreamSourceToByteArrayConverter());
    registry.addConverterFactory(new LenientStringToEnumConverterFactory());
    registry.addConverterFactory(new LenientBooleanToEnumConverterFactory());
    if (registry instanceof ConversionService) {
      addApplicationConverters(registry, (ConversionService) registry);
    }
  }

  private static void addApplicationConverters(ConverterRegistry registry, ConversionService conversionService) {
    registry.addConverter(new CharSequenceToObjectConverter(conversionService));
  }

  /**
   * Add converters to support delimited strings.
   *
   * @param registry the registry of converters to add to (must also be castable to
   * ConversionService, e.g. being a {@link ConfigurableConversionService})
   * @throws ClassCastException if the given ConverterRegistry could not be cast to a
   * ConversionService
   */
  public static void addDelimitedStringConverters(ConverterRegistry registry) {
    ConversionService service = (ConversionService) registry;
    registry.addConverter(new ArrayToDelimitedStringConverter(service));
    registry.addConverter(new CollectionToDelimitedStringConverter(service));
    registry.addConverter(new DelimitedStringToArrayConverter(service));
    registry.addConverter(new DelimitedStringToCollectionConverter(service));
  }

  /**
   * Add formatters useful for most applications.
   *
   * @param registry the service to register default formatters with
   */
  public static void addApplicationFormatters(FormatterRegistry registry) {
    registry.addFormatter(new CharArrayFormatter());
    registry.addFormatter(new InetAddressFormatter());
    registry.addFormatter(new IsoOffsetFormatter());
  }

  /**
   * Add {@link GenericConverter}, {@link Converter}, {@link Printer}, {@link Parser}
   * and {@link Formatter} beans from the specified context.
   *
   * @param registry the service to register beans with
   * @param beanFactory the bean factory to get the beans from
   */
  public static void addBeans(FormatterRegistry registry, BeanFactory beanFactory) {
    Set<Object> beans = new LinkedHashSet<>();
    beans.addAll(beanFactory.getBeansOfType(GenericConverter.class).values());
    beans.addAll(beanFactory.getBeansOfType(Converter.class).values());
    beans.addAll(beanFactory.getBeansOfType(Printer.class).values());
    beans.addAll(beanFactory.getBeansOfType(Parser.class).values());
    for (Object bean : beans) {
      if (bean instanceof GenericConverter) {
        registry.addConverter((GenericConverter) bean);
      }
      else if (bean instanceof Converter) {
        registry.addConverter((Converter<?, ?>) bean);
      }
      else if (bean instanceof Formatter) {
        registry.addFormatter((Formatter<?>) bean);
      }
      else if (bean instanceof Printer) {
        registry.addPrinter((Printer<?>) bean);
      }
      else if (bean instanceof Parser) {
        registry.addParser((Parser<?>) bean);
      }
    }
  }

}
