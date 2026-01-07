/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.annotation.config.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import infra.annotation.config.jackson.JacksonProperties.ConstructorDetectorStrategy;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.app.jackson.JacksonComponentModule;
import infra.app.jackson.JacksonMixinModule;
import infra.app.jackson.JacksonMixinModuleEntries;
import infra.beans.BeanUtils;
import infra.beans.factory.ObjectProvider;
import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Primary;
import infra.context.annotation.config.AutoConfigurationPackages;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.Ordered;
import infra.http.ProblemDetail;
import infra.http.converter.json.ProblemDetailJacksonMixin;
import infra.http.converter.json.ProblemDetailJacksonXmlMixin;
import infra.lang.Assert;
import infra.stereotype.Component;
import infra.stereotype.Prototype;
import infra.util.ClassUtils;
import infra.util.ReflectionUtils;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.cbor.CBORMapper;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Auto configuration for Jackson. The following auto-configuration will get applied:
 * <ul>
 * <li>an {@link ObjectMapper} in case none is already configured.</li>
 * <li>auto-registration for all {@link Module} beans with all {@link ObjectMapper} beans
 * (including the defaulted ones).</li>
 * </ul>
 *
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @author Sebastien Deleuze
 * @author Johannes Edmeier
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDIAutoConfiguration
@ConditionalOnClass(JsonMapper.class)
public final class JacksonAutoConfiguration {

  @Component
  public static JacksonComponentModule jsonComponentModule() {
    return new JacksonComponentModule();
  }

  @Prototype
  @ConditionalOnMissingBean
  public static JsonMapper.Builder jsonMapperBuilder(ObjectProvider<JsonFactory> jsonFactory, List<JsonMapperBuilderCustomizer> customizers) {
    JsonMapper.Builder builder = JsonMapper.builder(jsonFactory.getIfAvailable(JsonFactory::new));
    customize(builder, customizers);
    return builder;
  }

  private static void customize(JsonMapper.Builder builder, List<JsonMapperBuilderCustomizer> customizers) {
    for (JsonMapperBuilderCustomizer customizer : customizers) {
      customizer.customize(builder);
    }
  }

  @Component
  @Primary
  @ConditionalOnMissingBean
  public static JsonMapper jacksonJsonMapper(JsonMapper.Builder builder) {
    return builder.build();
  }

  @Configuration(proxyBeanMethods = false)
  static class JacksonMixinConfiguration {

    @Component
    public static JacksonMixinModuleEntries jacksonMixinModuleEntries(ApplicationContext context) {
      List<String> packages = AutoConfigurationPackages.has(context) ? AutoConfigurationPackages.get(context)
              : Collections.emptyList();
      return JacksonMixinModuleEntries.scan(context, packages);
    }

    @Component
    public static JacksonMixinModule jacksonMixinModule(ApplicationContext context, JacksonMixinModuleEntries entries) {
      JacksonMixinModule jacksonMixinModule = new JacksonMixinModule();
      jacksonMixinModule.registerEntries(entries, context.getClassLoader());
      return jacksonMixinModule;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(JacksonProperties.class)
  static class JacksonJsonMapperBuilderCustomizerConfiguration {

    @Component
    public static StandardJsonMapperBuilderCustomizer standardJsonMapperBuilderCustomizer(JacksonProperties jacksonProperties,
            ObjectProvider<JacksonModule> modules) {
      return new StandardJsonMapperBuilderCustomizer(jacksonProperties, modules.stream().toList());
    }

    static final class StandardJsonMapperBuilderCustomizer
            extends AbstractMapperBuilderCustomizer<JsonMapper.Builder> implements JsonMapperBuilderCustomizer {

      StandardJsonMapperBuilderCustomizer(JacksonProperties jacksonProperties,
              Collection<JacksonModule> modules) {
        super(jacksonProperties, modules);
      }

      @Override
      public void customize(JsonMapper.Builder builder) {
        super.customize(builder);
        configureFeatures(builder, properties().getJson().getRead(), builder::configure);
        configureFeatures(builder, properties().getJson().getWrite(), builder::configure);
      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(ProblemDetail.class)
  public static class JsonProblemDetailsConfiguration {

    @Component
    public static ProblemDetailJsonMapperBuilderCustomizer problemDetailJsonMapperBuilderCustomizer() {
      return new ProblemDetailJsonMapperBuilderCustomizer();
    }

    static final class ProblemDetailJsonMapperBuilderCustomizer implements JsonMapperBuilderCustomizer {

      @Override
      public void customize(JsonMapper.Builder builder) {
        builder.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(CBORMapper.class)
  @EnableConfigurationProperties(JacksonCborProperties.class)
  public static class CborConfiguration {

    @Component
    @ConditionalOnMissingBean
    public static CBORMapper cborMapper(CBORMapper.Builder builder) {
      return builder.build();
    }

    @Prototype
    @ConditionalOnMissingBean
    public static CBORMapper.Builder cborMapperBuilder(List<CborMapperBuilderCustomizer> customizers) {
      CBORMapper.Builder builder = CBORMapper.builder();
      customize(builder, customizers);
      return builder;
    }

    private static void customize(CBORMapper.Builder builder, List<CborMapperBuilderCustomizer> customizers) {
      for (CborMapperBuilderCustomizer customizer : customizers) {
        customizer.customize(builder);
      }
    }

    @Component
    public static StandardCborMapperBuilderCustomizer standardCborMapperBuilderCustomizer(JacksonProperties jacksonProperties,
            ObjectProvider<JacksonModule> modules, JacksonCborProperties cborProperties) {
      return new StandardCborMapperBuilderCustomizer(jacksonProperties, modules.stream().toList(),
              cborProperties);
    }

    static class StandardCborMapperBuilderCustomizer extends AbstractMapperBuilderCustomizer<CBORMapper.Builder>
            implements CborMapperBuilderCustomizer {

      private final JacksonCborProperties cborProperties;

      StandardCborMapperBuilderCustomizer(JacksonProperties jacksonProperties, Collection<JacksonModule> modules,
              JacksonCborProperties cborProperties) {
        super(jacksonProperties, modules);
        this.cborProperties = cborProperties;
      }

      @Override
      public void customize(CBORMapper.Builder builder) {
        super.customize(builder);
        configureFeatures(builder, this.cborProperties.getRead(), builder::configure);
        configureFeatures(builder, this.cborProperties.getWrite(), builder::configure);
      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(XmlMapper.class)
  @EnableConfigurationProperties(JacksonXmlProperties.class)
  public static class XmlConfiguration {

    @Component
    @ConditionalOnMissingBean
    public static XmlMapper xmlMapper(XmlMapper.Builder builder) {
      return builder.build();
    }

    @Prototype
    @ConditionalOnMissingBean
    public static XmlMapper.Builder xmlMapperBuilder(List<XmlMapperBuilderCustomizer> customizers) {
      XmlMapper.Builder builder = XmlMapper.builder();
      customize(builder, customizers);
      return builder;
    }

    private static void customize(XmlMapper.Builder builder, List<XmlMapperBuilderCustomizer> customizers) {
      for (XmlMapperBuilderCustomizer customizer : customizers) {
        customizer.customize(builder);
      }
    }

    @Component
    public static StandardXmlMapperBuilderCustomizer standardXmlMapperBuilderCustomizer(JacksonProperties jacksonProperties,
            ObjectProvider<JacksonModule> modules, JacksonXmlProperties xmlProperties) {
      return new StandardXmlMapperBuilderCustomizer(jacksonProperties, modules.stream().toList(), xmlProperties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ProblemDetail.class)
    static class XmlProblemDetailsConfiguration {

      @Component
      public static ProblemDetailXmlMapperBuilderCustomizer problemDetailXmlMapperBuilderCustomizer() {
        return new ProblemDetailXmlMapperBuilderCustomizer();
      }

      static final class ProblemDetailXmlMapperBuilderCustomizer implements XmlMapperBuilderCustomizer {

        @Override
        public void customize(XmlMapper.Builder builder) {
          builder.addMixIn(ProblemDetail.class, ProblemDetailJacksonXmlMixin.class);
        }

      }

    }

    static class StandardXmlMapperBuilderCustomizer extends AbstractMapperBuilderCustomizer<XmlMapper.Builder>
            implements XmlMapperBuilderCustomizer {

      private final JacksonXmlProperties xmlProperties;

      StandardXmlMapperBuilderCustomizer(JacksonProperties jacksonProperties, Collection<JacksonModule> modules,
              JacksonXmlProperties xmlProperties) {
        super(jacksonProperties, modules);
        this.xmlProperties = xmlProperties;
      }

      @Override
      public void customize(XmlMapper.Builder builder) {
        super.customize(builder);
        configureFeatures(builder, this.xmlProperties.getRead(), builder::configure);
        configureFeatures(builder, this.xmlProperties.getWrite(), builder::configure);
      }

    }

  }

  static class JacksonAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      if (ClassUtils.isPresent("tools.jackson.databind.PropertyNamingStrategy", classLoader)) {
        registerPropertyNamingStrategyHints(hints.reflection());
      }
    }

    /**
     * Register hints for the {@code configurePropertyNamingStrategyField} method to
     * use.
     *
     * @param hints reflection hints
     */
    private void registerPropertyNamingStrategyHints(ReflectionHints hints) {
      registerPropertyNamingStrategyHints(hints, PropertyNamingStrategies.class);
    }

    private void registerPropertyNamingStrategyHints(ReflectionHints hints, Class<?> type) {
      Stream.of(type.getDeclaredFields())
              .filter(this::isPropertyNamingStrategyField)
              .forEach(hints::registerField);
    }

    private boolean isPropertyNamingStrategyField(Field candidate) {
      return ReflectionUtils.isPublicStaticFinal(candidate)
              && candidate.getType().isAssignableFrom(PropertyNamingStrategy.class);
    }

  }

  abstract static class AbstractMapperBuilderCustomizer<B extends MapperBuilder<?, ?>> implements Ordered {

    private final JacksonProperties jacksonProperties;

    private final Collection<JacksonModule> modules;

    AbstractMapperBuilderCustomizer(JacksonProperties jacksonProperties, Collection<JacksonModule> modules) {
      this.jacksonProperties = jacksonProperties;
      this.modules = modules;
    }

    @Override
    public int getOrder() {
      return 0;
    }

    protected JacksonProperties properties() {
      return this.jacksonProperties;
    }

    protected void customize(B builder) {
      if (this.jacksonProperties.isFindAndAddModules()) {
        builder.findAndAddModules(getClass().getClassLoader());
      }
      Include propertyInclusion = this.jacksonProperties.getDefaultPropertyInclusion();
      if (propertyInclusion != null) {
        builder.changeDefaultPropertyInclusion((handler) -> handler.withValueInclusion(propertyInclusion)
                .withContentInclusion(propertyInclusion));
      }
      if (this.jacksonProperties.getTimeZone() != null) {
        builder.defaultTimeZone(this.jacksonProperties.getTimeZone());
      }
      configureVisibility(builder, this.jacksonProperties.getVisibility());
      configureFeatures(builder, this.jacksonProperties.getDeserialization(), builder::configure);
      configureFeatures(builder, this.jacksonProperties.getSerialization(), builder::configure);
      configureFeatures(builder, this.jacksonProperties.getMapper(), builder::configure);
      configureFeatures(builder, this.jacksonProperties.getDatatype().getDatetime(), builder::configure);
      configureFeatures(builder, this.jacksonProperties.getDatatype().getEnum(), builder::configure);
      configureFeatures(builder, this.jacksonProperties.getDatatype().getJsonNode(), builder::configure);
      configureDateFormat(builder);
      configurePropertyNamingStrategy(builder);
      configureModules(builder);
      configureLocale(builder);
      configureDefaultLeniency(builder);
      configureConstructorDetector(builder);
    }

    protected <T> void configureFeatures(B builder, Map<T, Boolean> features, BiConsumer<T, Boolean> configure) {
      features.forEach((feature, value) -> {
        if (value != null) {
          configure.accept(feature, value);
        }
      });
    }

    private void configureVisibility(MapperBuilder<?, ?> builder, Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities) {
      builder.changeDefaultVisibility((visibilityChecker) -> {
        for (Map.Entry<PropertyAccessor, JsonAutoDetect.Visibility> entry : visibilities.entrySet()) {
          visibilityChecker = visibilityChecker.withVisibility(entry.getKey(), entry.getValue());
        }
        return visibilityChecker;
      });
    }

    private void configureDateFormat(MapperBuilder<?, ?> builder) {
      // We support a fully qualified class name extending DateFormat or a date
      // pattern string value
      String dateFormat = this.jacksonProperties.getDateFormat();
      if (dateFormat != null) {
        try {
          Class<?> dateFormatClass = ClassUtils.forName(dateFormat, null);
          builder.defaultDateFormat((DateFormat) BeanUtils.newInstance(dateFormatClass));
        }
        catch (ClassNotFoundException ex) {
          SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
          // Since Jackson 2.6.3 we always need to set a TimeZone (see
          // gh-4170). If none in our properties fallback to Jackson's
          // default
          TimeZone timeZone = this.jacksonProperties.getTimeZone();
          if (timeZone == null) {
            timeZone = new ObjectMapper().serializationConfig().getTimeZone();
          }
          simpleDateFormat.setTimeZone(timeZone);
          builder.defaultDateFormat(simpleDateFormat);
        }
      }
    }

    private void configurePropertyNamingStrategy(MapperBuilder<?, ?> builder) {
      // We support a fully qualified class name extending Jackson's
      // PropertyNamingStrategy or a string value corresponding to the constant
      // names in PropertyNamingStrategy which hold default provided
      // implementations
      String strategy = this.jacksonProperties.getPropertyNamingStrategy();
      if (strategy != null) {
        try {
          configurePropertyNamingStrategyClass(builder, ClassUtils.forName(strategy, null));
        }
        catch (ClassNotFoundException ex) {
          configurePropertyNamingStrategyField(builder, strategy);
        }
      }
    }

    private void configurePropertyNamingStrategyClass(MapperBuilder<?, ?> builder, Class<?> propertyNamingStrategyClass) {
      builder.propertyNamingStrategy((PropertyNamingStrategy) BeanUtils.newInstance(propertyNamingStrategyClass));
    }

    private void configurePropertyNamingStrategyField(MapperBuilder<?, ?> builder, String fieldName) {
      // Find the field (this way we automatically support new constants
      // that may be added by Jackson in the future)
      Field field = findPropertyNamingStrategyField(fieldName);
      try {
        builder.propertyNamingStrategy((PropertyNamingStrategy) field.get(null));
      }
      catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    }

    private Field findPropertyNamingStrategyField(String fieldName) {
      Field field = ReflectionUtils.findField(PropertyNamingStrategies.class, fieldName,
              PropertyNamingStrategy.class);
      Assert.state(field != null, () -> "Constant named '" + fieldName + "' not found");
      return field;
    }

    private void configureModules(MapperBuilder<?, ?> builder) {
      builder.addModules(this.modules);
    }

    private void configureLocale(MapperBuilder<?, ?> builder) {
      Locale locale = this.jacksonProperties.getLocale();
      if (locale != null) {
        builder.defaultLocale(locale);
      }
    }

    private void configureDefaultLeniency(MapperBuilder<?, ?> builder) {
      Boolean defaultLeniency = this.jacksonProperties.getDefaultLeniency();
      if (defaultLeniency != null) {
        builder.defaultLeniency(defaultLeniency);
      }
    }

    private void configureConstructorDetector(MapperBuilder<?, ?> builder) {
      ConstructorDetectorStrategy strategy = this.jacksonProperties.getConstructorDetector();
      if (strategy != null) {
        switch (strategy) {
          case USE_PROPERTIES_BASED -> builder.constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED);
          case USE_DELEGATING -> builder.constructorDetector(ConstructorDetector.USE_DELEGATING);
          case EXPLICIT_ONLY -> builder.constructorDetector(ConstructorDetector.EXPLICIT_ONLY);
          default -> builder.constructorDetector(ConstructorDetector.DEFAULT);
        }
      }
    }

  }

}
