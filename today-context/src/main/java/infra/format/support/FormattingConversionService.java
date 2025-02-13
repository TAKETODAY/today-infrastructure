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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import infra.context.expression.EmbeddedValueResolverAware;
import infra.core.DecoratingProxy;
import infra.core.GenericTypeResolver;
import infra.core.StringValueResolver;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.core.conversion.GenericConverter;
import infra.core.conversion.support.GenericConversionService;
import infra.core.i18n.LocaleContextHolder;
import infra.format.AnnotationFormatterFactory;
import infra.format.Formatter;
import infra.format.FormatterRegistry;
import infra.format.Parser;
import infra.format.Printer;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * A {@link ConversionService} implementation
 * designed to be configured as a {@link FormatterRegistry}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class FormattingConversionService extends GenericConversionService
        implements FormatterRegistry, EmbeddedValueResolverAware {

  @Nullable
  private StringValueResolver embeddedValueResolver;

  private final ConcurrentHashMap<ConverterKey, GenericConverter> cachedParsers
          = new ConcurrentHashMap<>(64);

  private final ConcurrentHashMap<ConverterKey, GenericConverter> cachedPrinters
          = new ConcurrentHashMap<>(64);

  @Override
  public void setEmbeddedValueResolver(@Nullable StringValueResolver resolver) {
    this.embeddedValueResolver = resolver;
  }

  @Override
  public void addPrinter(Printer<?> printer) {
    Class<?> fieldType = getFieldType(printer, Printer.class);
    addConverter(new PrinterConverter(fieldType, printer, this));
  }

  @Override
  public void addParser(Parser<?> parser) {
    Class<?> fieldType = getFieldType(parser, Parser.class);
    addConverter(new ParserConverter(fieldType, parser, this));
  }

  @Override
  public void addFormatter(Formatter<?> formatter) {
    addFormatterForFieldType(getFieldType(formatter), formatter);
  }

  @Override
  public void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter) {
    addConverter(new PrinterConverter(fieldType, formatter, this));
    addConverter(new ParserConverter(fieldType, formatter, this));
  }

  @Override
  public void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser) {
    addConverter(new PrinterConverter(fieldType, printer, this));
    addConverter(new ParserConverter(fieldType, parser, this));
  }

  @Override
  public <T extends Annotation> void addFormatterForFieldAnnotation(AnnotationFormatterFactory<T> factory) {
    Class<T> annotationType = getAnnotationType(factory);
    if (embeddedValueResolver != null && factory instanceof EmbeddedValueResolverAware) {
      ((EmbeddedValueResolverAware) factory).setEmbeddedValueResolver(embeddedValueResolver);
    }
    Set<Class<?>> fieldTypes = factory.getFieldTypes();
    for (Class<?> fieldType : fieldTypes) {
      addConverter(new AnnotationPrinterConverter(annotationType, factory, fieldType));
      addConverter(new AnnotationParserConverter(annotationType, factory, fieldType));
    }
  }

  static Class<?> getFieldType(Formatter<?> formatter) {
    return getFieldType(formatter, Formatter.class);
  }

  private static <T> Class<?> getFieldType(T instance, Class<T> genericInterface) {
    Class<?> fieldType = GenericTypeResolver.resolveTypeArgument(instance.getClass(), genericInterface);
    if (fieldType == null && instance instanceof DecoratingProxy decoratingProxy) {
      fieldType = GenericTypeResolver.resolveTypeArgument(
              decoratingProxy.getDecoratedClass(), genericInterface);
    }
    if (fieldType == null) {
      throw new IllegalArgumentException(
              "Unable to extract the parameterized field type from %s [%s]; does the class parameterize the <T> generic type?"
                      .formatted(ClassUtils.getShortName(genericInterface), instance.getClass().getName()));
    }
    return fieldType;
  }

  static <T extends Annotation> Class<T> getAnnotationType(AnnotationFormatterFactory<T> factory) {
    Class<T> annotationType = GenericTypeResolver.resolveTypeArgument(factory.getClass(), AnnotationFormatterFactory.class);
    if (annotationType == null) {
      throw new IllegalArgumentException(
              "Unable to extract parameterized Annotation type argument from AnnotationFormatterFactory [%s]; does the factory parameterize the <A extends Annotation> generic type?"
                      .formatted(factory.getClass().getName()));
    }
    return annotationType;
  }

  private static Annotation getAnnotation(TypeDescriptor sourceType, Class<? extends Annotation> annotationType) {
    Annotation ann = sourceType.getAnnotation(annotationType);
    if (ann == null) {
      throw new IllegalStateException(
              "Expected [%s] to be present on %s".formatted(annotationType.getName(), sourceType));
    }
    return ann;
  }

  private static final class PrinterConverter implements GenericConverter {

    private final Class<?> fieldType;

    private final TypeDescriptor printerObjectType;

    @SuppressWarnings("rawtypes")
    private final Printer printer;

    private final ConversionService conversionService;

    public PrinterConverter(Class<?> fieldType, Printer<?> printer, ConversionService conversionService) {
      this.fieldType = fieldType;
      this.printerObjectType = TypeDescriptor.valueOf(resolvePrinterObjectType(printer));
      this.printer = printer;
      this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(this.fieldType, String.class));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (!sourceType.isAssignableTo(this.printerObjectType)) {
        source = this.conversionService.convert(source, sourceType, this.printerObjectType);
      }
      if (source == null) {
        return "";
      }
      return this.printer.print(source, LocaleContextHolder.getLocale());
    }

    @Nullable
    private Class<?> resolvePrinterObjectType(Printer<?> printer) {
      return GenericTypeResolver.resolveTypeArgument(printer.getClass(), Printer.class);
    }

    @Override
    public String toString() {
      return (this.fieldType.getName() + " -> " + String.class.getName() + " : " + this.printer);
    }
  }

  private record ParserConverter(Class<?> fieldType, Parser<?> parser, ConversionService conversionService)
          implements GenericConverter {

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(String.class, this.fieldType));
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      String text = (String) source;
      if (StringUtils.isBlank(text)) {
        return null;
      }
      Object result;
      try {
        result = this.parser.parse(text, LocaleContextHolder.getLocale());
      }
      catch (IllegalArgumentException ex) {
        throw ex;
      }
      catch (Throwable ex) {
        throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
      }
      TypeDescriptor resultType = TypeDescriptor.valueOf(result.getClass());
      if (!resultType.isAssignableTo(targetType)) {
        result = this.conversionService.convert(result, resultType, targetType);
      }
      return result;
    }

    @Override
    public String toString() {
      return (String.class.getName() + " -> " + this.fieldType.getName() + ": " + this.parser);
    }
  }

  private final class AnnotationPrinterConverter implements ConditionalGenericConverter {

    private final Class<? extends Annotation> annotationType;

    @SuppressWarnings("rawtypes")
    private final AnnotationFormatterFactory formatterFactory;

    private final Class<?> fieldType;

    public AnnotationPrinterConverter(
            Class<? extends Annotation> annotationType,
            AnnotationFormatterFactory<?> formatterFactory, Class<?> fieldType) {

      this.fieldType = fieldType;
      this.annotationType = annotationType;
      this.formatterFactory = formatterFactory;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(this.fieldType, String.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return sourceType.hasAnnotation(this.annotationType);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      Annotation ann = getAnnotation(sourceType, annotationType);
      ConverterKey key = new ConverterKey(ann, sourceType.getObjectType());
      GenericConverter converter = cachedPrinters.get(key);
      if (converter == null) {
        Printer<?> printer = formatterFactory.getPrinter(key.annotation, key.fieldType);
        converter = new PrinterConverter(this.fieldType, printer, FormattingConversionService.this);
        cachedPrinters.put(key, converter);
      }
      return converter.convert(source, sourceType, targetType);
    }

    @Override
    public String toString() {
      return ("@" + this.annotationType.getName() + " " + this.fieldType.getName() + " -> " +
              String.class.getName() + ": " + this.formatterFactory);
    }
  }

  private final class AnnotationParserConverter implements ConditionalGenericConverter {

    private final Class<? extends Annotation> annotationType;

    @SuppressWarnings("rawtypes")
    private final AnnotationFormatterFactory formatterFactory;

    private final Class<?> fieldType;

    public AnnotationParserConverter(
            Class<? extends Annotation> annotationType,
            AnnotationFormatterFactory<?> formatterFactory, Class<?> fieldType) {
      this.fieldType = fieldType;
      this.annotationType = annotationType;
      this.formatterFactory = formatterFactory;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(String.class, this.fieldType));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return targetType.hasAnnotation(this.annotationType);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      Annotation ann = getAnnotation(targetType, annotationType);
      ConverterKey key = new ConverterKey(ann, targetType.getObjectType());
      GenericConverter converter = cachedParsers.get(key);
      if (converter == null) {
        Parser<?> parser = formatterFactory.getParser(key.annotation, key.fieldType);
        converter = new ParserConverter(fieldType, parser, FormattingConversionService.this);
        cachedParsers.put(key, converter);
      }
      return converter.convert(source, sourceType, targetType);
    }

    @Override
    public String toString() {
      return (String.class.getName() + " -> @" + this.annotationType.getName() + " " +
              this.fieldType.getName() + ": " + this.formatterFactory);
    }
  }

  private static final class ConverterKey {
    public final Annotation annotation;
    public final Class<?> fieldType;

    private ConverterKey(Annotation annotation, Class<?> fieldType) {
      this.annotation = annotation;
      this.fieldType = fieldType;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof ConverterKey otherKey)) {
        return false;
      }
      return (this.fieldType == otherKey.fieldType && this.annotation.equals(otherKey.annotation));
    }

    @Override
    public int hashCode() {
      return (this.fieldType.hashCode() * 29 + this.annotation.hashCode());
    }
  }

}
