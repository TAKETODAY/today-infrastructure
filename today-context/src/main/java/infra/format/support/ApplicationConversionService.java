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

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.annotation.BeanFactoryAnnotationUtils;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ConfigurableApplicationContext;
import infra.core.ResolvableType;
import infra.core.StringValueResolver;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalConverter;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.core.conversion.ConverterFactory;
import infra.core.conversion.ConverterRegistry;
import infra.core.conversion.GenericConverter;
import infra.core.conversion.GenericConverter.ConvertiblePair;
import infra.core.conversion.support.ConfigurableConversionService;
import infra.core.conversion.support.DefaultConversionService;
import infra.core.i18n.LocaleContextHolder;
import infra.format.AnnotationFormatterFactory;
import infra.format.Formatter;
import infra.format.FormatterRegistry;
import infra.format.Parser;
import infra.format.Printer;
import infra.lang.Assert;
import infra.util.StringUtils;

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

  private static final ResolvableType STRING = ResolvableType.forClass(String.class);

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
  public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter) {
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
   * @return if conversion happens through an {@code ObjectTo...} converter
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
   * appropriate for most infra applications.
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
   * Add converters useful for most infra applications.
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
    if (registry instanceof ConversionService conversionService) {
      addApplicationConverters(registry, conversionService);
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
   * Add formatters useful for most Infra applications.
   *
   * @param registry the service to register default formatters with
   */
  public static void addApplicationFormatters(FormatterRegistry registry) {
    registry.addFormatter(new CharArrayFormatter());
    registry.addFormatter(new InetAddressFormatter());
    registry.addFormatter(new IsoOffsetFormatter());
  }

  /**
   * Add {@link Printer}, {@link Parser}, {@link Formatter}, {@link Converter},
   * {@link ConverterFactory}, {@link GenericConverter}, and beans from the specified
   * bean factory.
   *
   * @param registry the service to register beans with
   * @param beanFactory the bean factory to get the beans from
   */
  public static void addBeans(FormatterRegistry registry, BeanFactory beanFactory) {
    addBeans(registry, beanFactory, null);
  }

  /**
   * Add {@link Printer}, {@link Parser}, {@link Formatter}, {@link Converter},
   * {@link ConverterFactory}, {@link GenericConverter}, and beans from the specified
   * bean factory.
   *
   * @param registry the service to register beans with
   * @param beanFactory the bean factory to get the beans from
   * @param qualifier the qualifier required on the beans or {@code null}
   * @return the beans that were added
   * @since 5.0
   */
  public static Map<String, Object> addBeans(FormatterRegistry registry, BeanFactory beanFactory, @Nullable String qualifier) {
    ConfigurableBeanFactory configurableBeanFactory = getConfigurableListableBeanFactory(beanFactory);
    Map<String, Object> beans = getBeans(beanFactory, qualifier);
    beans.forEach((beanName, bean) -> {
      BeanDefinition beanDefinition = (configurableBeanFactory != null)
              ? configurableBeanFactory.getMergedBeanDefinition(beanName) : null;
      ResolvableType type = (beanDefinition != null) ? beanDefinition.getResolvableType() : null;
      addBean(registry, bean, type);
    });
    return beans;
  }

  @Nullable
  private static ConfigurableBeanFactory getConfigurableListableBeanFactory(BeanFactory beanFactory) {
    if (beanFactory instanceof ConfigurableApplicationContext applicationContext) {
      return applicationContext.getBeanFactory();
    }
    if (beanFactory instanceof ConfigurableBeanFactory configurableListableBeanFactory) {
      return configurableListableBeanFactory;
    }
    return null;
  }

  private static Map<String, Object> getBeans(BeanFactory beanFactory, @Nullable String qualifier) {
    Map<String, Object> beans = new LinkedHashMap<>();
    beans.putAll(getBeans(beanFactory, Printer.class, qualifier));
    beans.putAll(getBeans(beanFactory, Parser.class, qualifier));
    beans.putAll(getBeans(beanFactory, Formatter.class, qualifier));
    beans.putAll(getBeans(beanFactory, Converter.class, qualifier));
    beans.putAll(getBeans(beanFactory, ConverterFactory.class, qualifier));
    beans.putAll(getBeans(beanFactory, GenericConverter.class, qualifier));
    return beans;
  }

  private static <T> Map<String, T> getBeans(BeanFactory beanFactory, Class<T> type, @Nullable String qualifier) {
    return StringUtils.isEmpty(qualifier) ? beanFactory.getBeansOfType(type)
            : BeanFactoryAnnotationUtils.qualifiedBeansOfType(beanFactory, type, qualifier);
  }

  static void addBean(FormatterRegistry registry, Object bean, @Nullable ResolvableType beanType) {
    if (bean instanceof GenericConverter converterBean) {
      addBean(registry, converterBean, beanType, GenericConverter.class, registry::addConverter, (Runnable) null);
    }
    else if (bean instanceof Converter<?, ?> converterBean) {
      Assert.state(beanType != null, "beanType is missing");
      addBean(registry, converterBean, beanType, Converter.class, registry::addConverter,
              ConverterBeanAdapter::new);
    }
    else if (bean instanceof ConverterFactory<?, ?> converterBean) {
      Assert.state(beanType != null, "beanType is missing");
      addBean(registry, converterBean, beanType, ConverterFactory.class, registry::addConverterFactory,
              ConverterFactoryBeanAdapter::new);
    }
    else if (bean instanceof Formatter<?> formatterBean) {
      Assert.state(beanType != null, "beanType is missing");
      addBean(registry, formatterBean, beanType, Formatter.class, registry::addFormatter, () -> {
        registry.addConverter(new PrinterBeanAdapter(formatterBean, beanType));
        registry.addConverter(new ParserBeanAdapter(formatterBean, beanType));
      });
    }
    else if (bean instanceof Printer<?> printerBean) {
      Assert.state(beanType != null, "beanType is missing");
      addBean(registry, printerBean, beanType, Printer.class, registry::addPrinter, PrinterBeanAdapter::new);
    }
    else if (bean instanceof Parser<?> parserBean) {
      Assert.state(beanType != null, "beanType is missing");
      addBean(registry, parserBean, beanType, Parser.class, registry::addParser, ParserBeanAdapter::new);
    }
  }

  private static <B, T> void addBean(FormatterRegistry registry, B bean, ResolvableType beanType, Class<T> type,
          Consumer<B> standardRegistrar, BiFunction<B, ResolvableType, BeanAdapter<?>> beanAdapterFactory) {
    addBean(registry, bean, beanType, type, standardRegistrar,
            () -> registry.addConverter(beanAdapterFactory.apply(bean, beanType)));
  }

  private static <B, T> void addBean(FormatterRegistry registry, B bean, @Nullable ResolvableType beanType, Class<T> type,
          Consumer<B> standardRegistrar, @Nullable Runnable beanAdapterRegistrar) {
    if (beanType != null && beanAdapterRegistrar != null
            && ResolvableType.forInstance(bean).as(type).hasUnresolvableGenerics()) {
      beanAdapterRegistrar.run();
      return;
    }
    standardRegistrar.accept(bean);
  }

  /**
   * Base class for adapters that adapt a bean to a {@link GenericConverter}.
   *
   * @param <B> the base type of the bean
   */
  abstract static class BeanAdapter<B> implements ConditionalGenericConverter {

    private final B bean;

    private final ResolvableTypePair types;

    BeanAdapter(B bean, ResolvableType beanType) {
      Assert.isInstanceOf(beanType.toClass(), bean);
      ResolvableType type = ResolvableType.forClass(getClass()).as(BeanAdapter.class).getGeneric();
      ResolvableType[] generics = beanType.as(type.toClass()).getGenerics();
      this.bean = bean;
      this.types = getResolvableTypePair(generics);
    }

    protected ResolvableTypePair getResolvableTypePair(ResolvableType[] generics) {
      return new ResolvableTypePair(generics[0], generics[1]);
    }

    protected B bean() {
      return this.bean;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return Set.of(new ConvertiblePair(this.types.source().toClass(), this.types.target().toClass()));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return (this.types.target().toClass() == targetType.getObjectType()
              && matchesTargetType(targetType.getResolvableType()));
    }

    private boolean matchesTargetType(ResolvableType targetType) {
      ResolvableType ours = this.types.target();
      return targetType.getType() instanceof Class || targetType.isAssignableFrom(ours)
              || this.types.target().hasUnresolvableGenerics();
    }

    protected final boolean conditionalConverterCandidateMatches(Object candidate, TypeDescriptor sourceType, TypeDescriptor targetType) {
      return !(candidate instanceof ConditionalConverter conditionalConverter)
              || conditionalConverter.matches(sourceType, targetType);
    }

    @Nullable
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected final Object convert(@Nullable Object source, TypeDescriptor targetType, Converter<?, ?> converter) {
      return (source != null) ? ((Converter) converter).convert(source) : convertNull(targetType);
    }

    @Nullable
    private Object convertNull(TypeDescriptor targetType) {
      return (targetType.getObjectType() != Optional.class) ? null : Optional.empty();
    }

    @Override
    public String toString() {
      return this.types + " : " + this.bean;
    }

  }

  /**
   * Adapts a {@link Printer} bean to a {@link GenericConverter}.
   */
  static class PrinterBeanAdapter extends BeanAdapter<Printer<?>> {

    PrinterBeanAdapter(Printer<?> bean, ResolvableType beanType) {
      super(bean, beanType);
    }

    @Override
    protected ResolvableTypePair getResolvableTypePair(ResolvableType[] generics) {
      return new ResolvableTypePair(generics[0], STRING);
    }

    @Override
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      return (source != null) ? print(source) : "";
    }

    @SuppressWarnings("unchecked")
    private String print(Object object) {
      return ((Printer<Object>) bean()).print(object, LocaleContextHolder.getLocale());
    }

  }

  /**
   * Adapts a {@link Parser} bean to a {@link GenericConverter}.
   */
  static class ParserBeanAdapter extends BeanAdapter<Parser<?>> {

    ParserBeanAdapter(Parser<?> bean, ResolvableType beanType) {
      super(bean, beanType);
    }

    @Override
    protected ResolvableTypePair getResolvableTypePair(ResolvableType[] generics) {
      return new ResolvableTypePair(STRING, generics[0]);
    }

    @Nullable
    @Override
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      String text = (String) source;
      return (!StringUtils.hasText(text)) ? null : parse(text);
    }

    private Object parse(String text) {
      try {
        return bean().parse(text, LocaleContextHolder.getLocale());
      }
      catch (IllegalArgumentException ex) {
        throw ex;
      }
      catch (Throwable ex) {
        throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
      }
    }

  }

  /**
   * Adapts a {@link Converter} bean to a {@link GenericConverter}.
   */
  static final class ConverterBeanAdapter extends BeanAdapter<Converter<?, ?>> {

    ConverterBeanAdapter(Converter<?, ?> bean, ResolvableType beanType) {
      super(bean, beanType);
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return super.matches(sourceType, targetType)
              && conditionalConverterCandidateMatches(bean(), sourceType, targetType);
    }

    @Nullable
    @Override
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      return convert(source, targetType, bean());
    }

  }

  /**
   * Adapts a {@link ConverterFactory} bean to a {@link GenericConverter}.
   */
  private static final class ConverterFactoryBeanAdapter extends BeanAdapter<ConverterFactory<?, ?>> {

    ConverterFactoryBeanAdapter(ConverterFactory<?, ?> bean, ResolvableType beanType) {
      super(bean, beanType);
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return super.matches(sourceType, targetType)
              && conditionalConverterCandidateMatches(bean(), sourceType, targetType)
              && conditionalConverterCandidateMatches(getConverter(targetType::getType), sourceType, targetType);
    }

    @Nullable
    @Override
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      return convert(source, targetType, getConverter(targetType::getObjectType));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Converter<Object, ?> getConverter(Supplier<Class<?>> typeSupplier) {
      return ((ConverterFactory) bean()).getConverter(typeSupplier.get());
    }

  }

  /**
   * Convertible type information as extracted from bean generics.
   *
   * @param source the source type
   * @param target the target type
   */
  record ResolvableTypePair(ResolvableType source, ResolvableType target) {

    ResolvableTypePair {
      Assert.notNull(source.resolve(), "'source' cannot be resolved");
      Assert.notNull(target.resolve(), "'target' cannot be resolved");
    }

    @Override
    public final String toString() {
      return source() + " -> " + target();
    }

  }

}
