/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.converter.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * A builder used to create {@link ObjectMapper} instances with a fluent API.
 *
 * <p>It customizes Jackson's default properties with the following ones:
 * <ul>
 * <li>{@link MapperFeature#DEFAULT_VIEW_INCLUSION} is disabled</li>
 * <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is disabled</li>
 * </ul>
 *
 * <p>It also automatically registers the following well-known modules if they are
 * detected on the classpath:
 * <ul>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jdk8">jackson-datatype-jdk8</a>:
 * support for other Java 8 types like {@link java.util.Optional}</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jsr310">jackson-datatype-jsr310</a>:
 * support for Java 8 Date &amp; Time API types</li>
 * <li><a href="https://github.com/FasterXML/jackson-module-kotlin">jackson-module-kotlin</a>:
 * support for Kotlin classes and data classes</li>
 * </ul>
 *
 * <p>Compatible with Jackson 2.9 to 2.12.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Tadaya Tsuyukubo
 * @author Eddú Meléndez
 * @see #build()
 * @see #configure(ObjectMapper)
 * @see Jackson2ObjectMapperFactoryBean
 * @since 4.0
 */
public class Jackson2ObjectMapperBuilder {

  private final LinkedHashMap<Object, Boolean> features = new LinkedHashMap<>();
  private final LinkedHashMap<Class<?>, Class<?>> mixIns = new LinkedHashMap<>();
  private final LinkedHashMap<Class<?>, JsonSerializer<?>> serializers = new LinkedHashMap<>();
  private final LinkedHashMap<Class<?>, JsonDeserializer<?>> deserializers = new LinkedHashMap<>();
  private final LinkedHashMap<PropertyAccessor, JsonAutoDetect.Visibility> visibilities = new LinkedHashMap<>();

  private boolean createXmlMapper = false;

  @Nullable
  private JsonFactory factory;

  @Nullable
  private DateFormat dateFormat;

  @Nullable
  private Locale locale;

  @Nullable
  private TimeZone timeZone;

  @Nullable
  private AnnotationIntrospector annotationIntrospector;

  @Nullable
  private PropertyNamingStrategy propertyNamingStrategy;

  @Nullable
  private TypeResolverBuilder<?> defaultTyping;

  @Nullable
  private JsonInclude.Value serializationInclusion;

  @Nullable
  private FilterProvider filters;

  @Nullable
  private List<Module> modules;

  @Nullable
  private Class<? extends Module>[] moduleClasses;

  private boolean findModulesViaServiceLoader = false;

  private boolean findWellKnownModules = true;

  private ClassLoader moduleClassLoader = getClass().getClassLoader();

  @Nullable
  private HandlerInstantiator handlerInstantiator;

  @Nullable
  private ApplicationContext applicationContext;

  @Nullable
  private Boolean defaultUseWrapper;

  @Nullable
  private Consumer<ObjectMapper> configurer;

  /**
   * If set to {@code true}, an {@link XmlMapper} will be created using its
   * default constructor. This is only applicable to {@link #build()} calls,
   * not to {@link #configure} calls.
   */
  public Jackson2ObjectMapperBuilder createXmlMapper(boolean createXmlMapper) {
    this.createXmlMapper = createXmlMapper;
    return this;
  }

  /**
   * Define the {@link JsonFactory} to be used to create the {@link ObjectMapper}
   * instance.
   */
  public Jackson2ObjectMapperBuilder factory(JsonFactory factory) {
    this.factory = factory;
    return this;
  }

  /**
   * Define the format for date/time with the given {@link DateFormat}.
   * <p>Note: Setting this property makes the exposed {@link ObjectMapper}
   * non-thread-safe, according to Jackson's thread safety rules.
   *
   * @see #simpleDateFormat(String)
   */
  public Jackson2ObjectMapperBuilder dateFormat(DateFormat dateFormat) {
    this.dateFormat = dateFormat;
    return this;
  }

  /**
   * Define the date/time format with a {@link SimpleDateFormat}.
   * <p>Note: Setting this property makes the exposed {@link ObjectMapper}
   * non-thread-safe, according to Jackson's thread safety rules.
   *
   * @see #dateFormat(DateFormat)
   */
  public Jackson2ObjectMapperBuilder simpleDateFormat(String format) {
    this.dateFormat = new SimpleDateFormat(format);
    return this;
  }

  /**
   * Override the default {@link Locale} to use for formatting.
   * Default value used is {@link Locale#getDefault()}.
   */
  public Jackson2ObjectMapperBuilder locale(Locale locale) {
    this.locale = locale;
    return this;
  }

  /**
   * Override the default {@link Locale} to use for formatting.
   * Default value used is {@link Locale#getDefault()}.
   *
   * @param localeString the locale ID as a String representation
   */
  public Jackson2ObjectMapperBuilder locale(String localeString) {
    this.locale = StringUtils.parseLocale(localeString);
    return this;
  }

  /**
   * Override the default {@link TimeZone} to use for formatting.
   * Default value used is UTC (NOT local timezone).
   */
  public Jackson2ObjectMapperBuilder timeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
    return this;
  }

  /**
   * Override the default {@link TimeZone} to use for formatting.
   * Default value used is UTC (NOT local timezone).
   *
   * @param timeZoneString the zone ID as a String representation
   */
  public Jackson2ObjectMapperBuilder timeZone(String timeZoneString) {
    this.timeZone = StringUtils.parseTimeZoneString(timeZoneString);
    return this;
  }

  /**
   * Set an {@link AnnotationIntrospector} for both serialization and deserialization.
   */
  public Jackson2ObjectMapperBuilder annotationIntrospector(AnnotationIntrospector annotationIntrospector) {
    this.annotationIntrospector = annotationIntrospector;
    return this;
  }

  /**
   * Alternative to {@link #annotationIntrospector(AnnotationIntrospector)}
   * that allows combining with rather than replacing the currently set
   * introspector, e.g. via
   * {@link AnnotationIntrospectorPair#pair(AnnotationIntrospector, AnnotationIntrospector)}.
   *
   * @param pairingFunction a function to apply to the currently set
   * introspector (possibly {@code null}); the result of the function becomes
   * the new introspector.
   */
  public Jackson2ObjectMapperBuilder annotationIntrospector(
          Function<AnnotationIntrospector, AnnotationIntrospector> pairingFunction) {
    this.annotationIntrospector = pairingFunction.apply(this.annotationIntrospector);
    return this;
  }

  /**
   * Specify a {@link PropertyNamingStrategy} to
   * configure the {@link ObjectMapper} with.
   */
  public Jackson2ObjectMapperBuilder propertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
    this.propertyNamingStrategy = propertyNamingStrategy;
    return this;
  }

  /**
   * Specify a {@link TypeResolverBuilder} to use for Jackson's default typing.
   */
  public Jackson2ObjectMapperBuilder defaultTyping(TypeResolverBuilder<?> typeResolverBuilder) {
    this.defaultTyping = typeResolverBuilder;
    return this;
  }

  /**
   * Set a custom inclusion strategy for serialization.
   *
   * @see JsonInclude.Include
   */
  public Jackson2ObjectMapperBuilder serializationInclusion(JsonInclude.Include inclusion) {
    return serializationInclusion(JsonInclude.Value.construct(inclusion, inclusion));
  }

  /**
   * Set a custom inclusion strategy for serialization.
   *
   * @see JsonInclude.Value
   */
  public Jackson2ObjectMapperBuilder serializationInclusion(JsonInclude.Value serializationInclusion) {
    this.serializationInclusion = serializationInclusion;
    return this;
  }

  /**
   * Set the global filters to use in order to support {@link JsonFilter @JsonFilter} annotated POJO.
   *
   * @see MappingJacksonValue#setFilters(FilterProvider)
   */
  public Jackson2ObjectMapperBuilder filters(FilterProvider filters) {
    this.filters = filters;
    return this;
  }

  /**
   * Add mix-in annotations to use for augmenting specified class or interface.
   *
   * @param target class (or interface) whose annotations to effectively override
   * @param mixinSource class (or interface) whose annotations are to be "added"
   * to target's annotations as value
   * @see ObjectMapper#addMixIn(Class, Class)
   */
  public Jackson2ObjectMapperBuilder mixIn(Class<?> target, Class<?> mixinSource) {
    this.mixIns.put(target, mixinSource);
    return this;
  }

  /**
   * Add mix-in annotations to use for augmenting specified class or interface.
   *
   * @param mixIns a Map of entries with target classes (or interface) whose annotations
   * to effectively override as key and mix-in classes (or interface) whose
   * annotations are to be "added" to target's annotations as value.
   * @see ObjectMapper#addMixIn(Class, Class)
   */
  public Jackson2ObjectMapperBuilder mixIns(Map<Class<?>, Class<?>> mixIns) {
    this.mixIns.putAll(mixIns);
    return this;
  }

  /**
   * Configure custom serializers. Each serializer is registered for the type
   * returned by {@link JsonSerializer#handledType()}, which must not be {@code null}.
   *
   * @see #serializersByType(Map)
   */
  public Jackson2ObjectMapperBuilder serializers(JsonSerializer<?>... serializers) {
    for (JsonSerializer<?> serializer : serializers) {
      Class<?> handledType = serializer.handledType();
      if (handledType == null || handledType == Object.class) {
        throw new IllegalArgumentException("Unknown handled type in " + serializer.getClass().getName());
      }
      this.serializers.put(serializer.handledType(), serializer);
    }
    return this;
  }

  /**
   * Configure a custom serializer for the given type.
   *
   * @see #serializers(JsonSerializer...)
   */
  public Jackson2ObjectMapperBuilder serializerByType(Class<?> type, JsonSerializer<?> serializer) {
    this.serializers.put(type, serializer);
    return this;
  }

  /**
   * Configure custom serializers for the given types.
   *
   * @see #serializers(JsonSerializer...)
   */
  public Jackson2ObjectMapperBuilder serializersByType(Map<Class<?>, JsonSerializer<?>> serializers) {
    this.serializers.putAll(serializers);
    return this;
  }

  /**
   * Configure custom deserializers. Each deserializer is registered for the type
   * returned by {@link JsonDeserializer#handledType()}, which must not be {@code null}.
   *
   * @see #deserializersByType(Map)
   */
  public Jackson2ObjectMapperBuilder deserializers(JsonDeserializer<?>... deserializers) {
    for (JsonDeserializer<?> deserializer : deserializers) {
      Class<?> handledType = deserializer.handledType();
      if (handledType == null || handledType == Object.class) {
        throw new IllegalArgumentException("Unknown handled type in " + deserializer.getClass().getName());
      }
      this.deserializers.put(deserializer.handledType(), deserializer);
    }
    return this;
  }

  /**
   * Configure a custom deserializer for the given type.
   */
  public Jackson2ObjectMapperBuilder deserializerByType(Class<?> type, JsonDeserializer<?> deserializer) {
    this.deserializers.put(type, deserializer);
    return this;
  }

  /**
   * Configure custom deserializers for the given types.
   */
  public Jackson2ObjectMapperBuilder deserializersByType(Map<Class<?>, JsonDeserializer<?>> deserializers) {
    this.deserializers.putAll(deserializers);
    return this;
  }

  /**
   * Shortcut for {@link MapperFeature#AUTO_DETECT_FIELDS} option.
   */
  public Jackson2ObjectMapperBuilder autoDetectFields(boolean autoDetectFields) {
    this.features.put(MapperFeature.AUTO_DETECT_FIELDS, autoDetectFields);
    return this;
  }

  /**
   * Shortcut for {@link MapperFeature#AUTO_DETECT_SETTERS}/
   * {@link MapperFeature#AUTO_DETECT_GETTERS}/{@link MapperFeature#AUTO_DETECT_IS_GETTERS}
   * options.
   */
  public Jackson2ObjectMapperBuilder autoDetectGettersSetters(boolean autoDetectGettersSetters) {
    this.features.put(MapperFeature.AUTO_DETECT_GETTERS, autoDetectGettersSetters);
    this.features.put(MapperFeature.AUTO_DETECT_SETTERS, autoDetectGettersSetters);
    this.features.put(MapperFeature.AUTO_DETECT_IS_GETTERS, autoDetectGettersSetters);
    return this;
  }

  /**
   * Shortcut for {@link MapperFeature#DEFAULT_VIEW_INCLUSION} option.
   */
  public Jackson2ObjectMapperBuilder defaultViewInclusion(boolean defaultViewInclusion) {
    this.features.put(MapperFeature.DEFAULT_VIEW_INCLUSION, defaultViewInclusion);
    return this;
  }

  /**
   * Shortcut for {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} option.
   */
  public Jackson2ObjectMapperBuilder failOnUnknownProperties(boolean failOnUnknownProperties) {
    this.features.put(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
    return this;
  }

  /**
   * Shortcut for {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} option.
   */
  public Jackson2ObjectMapperBuilder failOnEmptyBeans(boolean failOnEmptyBeans) {
    this.features.put(SerializationFeature.FAIL_ON_EMPTY_BEANS, failOnEmptyBeans);
    return this;
  }

  /**
   * Shortcut for {@link SerializationFeature#INDENT_OUTPUT} option.
   */
  public Jackson2ObjectMapperBuilder indentOutput(boolean indentOutput) {
    this.features.put(SerializationFeature.INDENT_OUTPUT, indentOutput);
    return this;
  }

  /**
   * Define if a wrapper will be used for indexed (List, array) properties or not by
   * default (only applies to {@link XmlMapper}).
   */
  public Jackson2ObjectMapperBuilder defaultUseWrapper(boolean defaultUseWrapper) {
    this.defaultUseWrapper = defaultUseWrapper;
    return this;
  }

  /**
   * Specify visibility to limit what kind of properties are auto-detected.
   *
   * @see PropertyAccessor
   * @see JsonAutoDetect.Visibility
   */
  public Jackson2ObjectMapperBuilder visibility(PropertyAccessor accessor, JsonAutoDetect.Visibility visibility) {
    this.visibilities.put(accessor, visibility);
    return this;
  }

  /**
   * Specify features to enable.
   *
   * @see JsonParser.Feature
   * @see JsonGenerator.Feature
   * @see SerializationFeature
   * @see DeserializationFeature
   * @see MapperFeature
   */
  public Jackson2ObjectMapperBuilder featuresToEnable(Object... featuresToEnable) {
    for (Object feature : featuresToEnable) {
      this.features.put(feature, Boolean.TRUE);
    }
    return this;
  }

  /**
   * Specify features to disable.
   *
   * @see JsonParser.Feature
   * @see JsonGenerator.Feature
   * @see SerializationFeature
   * @see DeserializationFeature
   * @see MapperFeature
   */
  public Jackson2ObjectMapperBuilder featuresToDisable(Object... featuresToDisable) {
    for (Object feature : featuresToDisable) {
      this.features.put(feature, Boolean.FALSE);
    }
    return this;
  }

  /**
   * Specify one or more modules to be registered with the {@link ObjectMapper}.
   * <p>Multiple invocations are not additive, the last one defines the modules to
   * register.
   * <p>Note: If this is set, no finding of modules is going to happen - not by
   * Jackson, and not by today either (see {@link #findModulesViaServiceLoader}).
   * As a consequence, specifying an empty list here will suppress any kind of
   * module detection.
   * <p>Specify either this or {@link #modulesToInstall}, not both.
   *
   * @see #modules(List)
   * @see Module
   */
  public Jackson2ObjectMapperBuilder modules(Module... modules) {
    return modules(Arrays.asList(modules));
  }

  /**
   * Variant of {@link #modules(Module...)} with a {@link List}.
   *
   * @see #modules(Module...)
   * @see #modules(Consumer)
   * @see com.fasterxml.jackson.databind.Module
   */
  public Jackson2ObjectMapperBuilder modules(List<Module> modules) {
    this.modules = new ArrayList<>(modules);
    this.findModulesViaServiceLoader = false;
    this.findWellKnownModules = false;
    return this;
  }

  /**
   * Variant of {@link #modules(Module...)} with a {@link Consumer} for full
   * control over the underlying list of modules.
   *
   * @see #modules(Module...)
   * @see #modules(List)
   * @see com.fasterxml.jackson.databind.Module
   */
  public Jackson2ObjectMapperBuilder modules(Consumer<List<Module>> consumer) {
    this.modules = (this.modules != null ? this.modules : new ArrayList<>());
    this.findModulesViaServiceLoader = false;
    this.findWellKnownModules = false;
    consumer.accept(this.modules);
    return this;
  }

  /**
   * Specify one or more modules to be registered with the {@link ObjectMapper}.
   * <p>Multiple invocations are not additive, the last one defines the modules
   * to register.
   * <p>Modules specified here will be registered after
   * autodetection of JSR-310 and Joda-Time, or Jackson's
   * finding of modules (see {@link #findModulesViaServiceLoader}),
   * allowing to eventually override their configuration.
   * <p>Specify either this or {@link #modules(Module...)}, not both.
   *
   * @see #modulesToInstall(Consumer)
   * @see #modulesToInstall(Class...)
   * @see Module
   */
  public Jackson2ObjectMapperBuilder modulesToInstall(Module... modules) {
    this.modules = Arrays.asList(modules);
    this.findWellKnownModules = true;
    return this;
  }

  /**
   * Variant of {@link #modulesToInstall(Module...)} with a {@link Consumer}
   * for full control over the underlying list of modules.
   *
   * @see #modulesToInstall(Module...)
   * @see #modulesToInstall(Class...)
   * @see com.fasterxml.jackson.databind.Module
   */
  public Jackson2ObjectMapperBuilder modulesToInstall(Consumer<List<Module>> consumer) {
    this.modules = (this.modules != null ? this.modules : new ArrayList<>());
    this.findWellKnownModules = true;
    consumer.accept(this.modules);
    return this;
  }

  /**
   * Specify one or more modules by class to be registered with
   * the {@link ObjectMapper}.
   * <p>Multiple invocations are not additive, the last one defines the modules
   * to register.
   * <p>Modules specified here will be registered after
   * autodetection of JSR-310 and Joda-Time, or Jackson's
   * finding of modules (see {@link #findModulesViaServiceLoader}),
   * allowing to eventually override their configuration.
   * <p>Specify either this or {@link #modules(Module...)}, not both.
   *
   * @see #modulesToInstall(Module...)
   * @see #modulesToInstall(Consumer)
   * @see Module
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public final Jackson2ObjectMapperBuilder modulesToInstall(Class<? extends Module>... modules) {
    this.moduleClasses = modules;
    this.findWellKnownModules = true;
    return this;
  }

  /**
   * Set whether to let Jackson find available modules via the JDK ServiceLoader,
   * based on META-INF metadata in the classpath.
   * <p>If this mode is not set,  Jackson2ObjectMapperBuilder itself
   * will try to find the JSR-310 and Joda-Time support modules on the classpath -
   * provided that Java 8 and Joda-Time themselves are available, respectively.
   *
   * @see ObjectMapper#findModules()
   */
  public Jackson2ObjectMapperBuilder findModulesViaServiceLoader(boolean findModules) {
    this.findModulesViaServiceLoader = findModules;
    return this;
  }

  /**
   * Set the ClassLoader to use for loading Jackson extension modules.
   */
  public Jackson2ObjectMapperBuilder moduleClassLoader(ClassLoader moduleClassLoader) {
    this.moduleClassLoader = moduleClassLoader;
    return this;
  }

  /**
   * Customize the construction of Jackson handlers ({@link JsonSerializer}, {@link JsonDeserializer},
   * {@link KeyDeserializer}, {@code TypeResolverBuilder} and {@code TypeIdResolver}).
   *
   * @see Jackson2ObjectMapperBuilder#applicationContext(ApplicationContext)
   */
  public Jackson2ObjectMapperBuilder handlerInstantiator(HandlerInstantiator handlerInstantiator) {
    this.handlerInstantiator = handlerInstantiator;
    return this;
  }

  /**
   * Set the {@link ApplicationContext} in order to autowire Jackson handlers ({@link JsonSerializer},
   * {@link JsonDeserializer}, {@link KeyDeserializer}, {@code TypeResolverBuilder} and {@code TypeIdResolver}).
   *
   * @see BeanFactoryHandlerInstantiator
   */
  public Jackson2ObjectMapperBuilder applicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    return this;
  }

  /**
   * An option to apply additional customizations directly to the
   * {@code ObjectMapper} instances at the end, after all other config
   * properties of the builder have been applied.
   *
   * @param configurer a configurer to apply. If several configurers are
   * registered, they will get applied in their registration order.
   */
  public Jackson2ObjectMapperBuilder postConfigurer(Consumer<ObjectMapper> configurer) {
    this.configurer = (this.configurer != null ? this.configurer.andThen(configurer) : configurer);
    return this;
  }

  /**
   * Build a new {@link ObjectMapper} instance.
   * <p>Each build operation produces an independent {@link ObjectMapper} instance.
   * The builder's settings can get modified, with a subsequent build operation
   * then producing a new {@link ObjectMapper} based on the most recent settings.
   *
   * @return the newly built ObjectMapper
   */
  @SuppressWarnings("unchecked")
  public <T extends ObjectMapper> T build() {
    ObjectMapper mapper;
    if (this.createXmlMapper) {
      mapper = (this.defaultUseWrapper != null ?
                new XmlObjectMapperInitializer().create(this.defaultUseWrapper, this.factory) :
                new XmlObjectMapperInitializer().create(this.factory));
    }
    else {
      mapper = (this.factory != null ? new ObjectMapper(this.factory) : new ObjectMapper());
    }
    configure(mapper);
    return (T) mapper;
  }

  /**
   * Configure an existing {@link ObjectMapper} instance with this builder's
   * settings. This can be applied to any number of {@code ObjectMappers}.
   *
   * @param objectMapper the ObjectMapper to configure
   */
  public void configure(ObjectMapper objectMapper) {
    Assert.notNull(objectMapper, "ObjectMapper must not be null");
    DefaultMultiValueMap<Object, Module> modulesToRegister = MultiValueMap.fromLinkedHashMap();
    if (this.findModulesViaServiceLoader) {
      for (Module module : ObjectMapper.findModules(this.moduleClassLoader)) {
        registerModule(module, modulesToRegister);
      }
    }
    else if (this.findWellKnownModules) {
      registerWellKnownModulesIfAvailable(modulesToRegister);
    }

    if (this.modules != null) {
      for (Module module : modules) {
        registerModule(module, modulesToRegister);
      }
    }
    if (this.moduleClasses != null) {
      for (Class<? extends Module> moduleClass : this.moduleClasses) {
        registerModule(BeanUtils.newInstance(moduleClass), modulesToRegister);
      }
    }
    List<Module> modules = new ArrayList<>();
    for (List<Module> nestedModules : modulesToRegister.values()) {
      modules.addAll(nestedModules);
    }
    objectMapper.registerModules(modules);

    if (this.dateFormat != null) {
      objectMapper.setDateFormat(this.dateFormat);
    }
    if (this.locale != null) {
      objectMapper.setLocale(this.locale);
    }
    if (this.timeZone != null) {
      objectMapper.setTimeZone(this.timeZone);
    }

    if (this.annotationIntrospector != null) {
      objectMapper.setAnnotationIntrospector(this.annotationIntrospector);
    }
    if (this.propertyNamingStrategy != null) {
      objectMapper.setPropertyNamingStrategy(this.propertyNamingStrategy);
    }
    if (this.defaultTyping != null) {
      objectMapper.setDefaultTyping(this.defaultTyping);
    }
    if (this.serializationInclusion != null) {
      objectMapper.setDefaultPropertyInclusion(this.serializationInclusion);
    }

    if (this.filters != null) {
      objectMapper.setFilterProvider(this.filters);
    }

    objectMapper.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
    this.mixIns.forEach(objectMapper::addMixIn);

    if (!this.serializers.isEmpty() || !this.deserializers.isEmpty()) {
      SimpleModule module = new SimpleModule();
      addSerializers(module);
      addDeserializers(module);
      objectMapper.registerModule(module);
    }

    this.visibilities.forEach(objectMapper::setVisibility);

    for (Map.Entry<Object, Boolean> entry : features.entrySet()) {
      Object feature = entry.getKey();
      Boolean enabled = entry.getValue();
      configureFeature(objectMapper, feature, enabled);
    }

    customizeDefaultFeatures(objectMapper);

    if (this.handlerInstantiator != null) {
      objectMapper.setHandlerInstantiator(this.handlerInstantiator);
    }
    else if (this.applicationContext != null) {
      objectMapper.setHandlerInstantiator(
              new BeanFactoryHandlerInstantiator(this.applicationContext.getAutowireCapableBeanFactory()));
    }

    if (this.configurer != null) {
      this.configurer.accept(objectMapper);
    }
  }

  private void registerModule(Module module, MultiValueMap<Object, Module> modulesToRegister) {
    if (module.getTypeId() == null) {
      modulesToRegister.add(SimpleModule.class.getName(), module);
    }
    else {
      modulesToRegister.set(module.getTypeId(), module);
    }
  }

  // MappingJackson2MessageConverter default constructors
  private void customizeDefaultFeatures(ObjectMapper objectMapper) {
    if (!this.features.containsKey(MapperFeature.DEFAULT_VIEW_INCLUSION)) {
      configureFeature(objectMapper, MapperFeature.DEFAULT_VIEW_INCLUSION, false);
    }
    if (!this.features.containsKey(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
      configureFeature(objectMapper, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void addSerializers(SimpleModule module) {
    for (Map.Entry<Class<?>, JsonSerializer<?>> entry : serializers.entrySet()) {
      Class<?> type = entry.getKey();
      JsonSerializer<?> serializer = entry.getValue();
      module.addSerializer((Class<? extends T>) type, (JsonSerializer<T>) serializer);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void addDeserializers(SimpleModule module) {
    for (Map.Entry<Class<?>, JsonDeserializer<?>> entry : deserializers.entrySet()) {
      Class<?> type = entry.getKey();
      JsonDeserializer<?> deserializer = entry.getValue();
      module.addDeserializer((Class<T>) type, (JsonDeserializer<? extends T>) deserializer);
    }
  }

  @SuppressWarnings("deprecation")  // on Jackson 2.13: configure(MapperFeature, boolean)
  private void configureFeature(ObjectMapper objectMapper, Object feature, boolean enabled) {
    if (feature instanceof JsonParser.Feature) {
      objectMapper.configure((JsonParser.Feature) feature, enabled);
    }
    else if (feature instanceof JsonGenerator.Feature) {
      objectMapper.configure((JsonGenerator.Feature) feature, enabled);
    }
    else if (feature instanceof SerializationFeature) {
      objectMapper.configure((SerializationFeature) feature, enabled);
    }
    else if (feature instanceof DeserializationFeature) {
      objectMapper.configure((DeserializationFeature) feature, enabled);
    }
    else if (feature instanceof MapperFeature) {
      objectMapper.configure((MapperFeature) feature, enabled);
    }
    else {
      throw new IllegalArgumentException("Unknown feature class: " + feature.getClass().getName());
    }
  }

  private void registerWellKnownModulesIfAvailable(MultiValueMap<Object, Module> modulesToRegister) {
    try {
      Class<? extends Module> jdk8ModuleClass = ClassUtils.forName(
              "com.fasterxml.jackson.datatype.jdk8.Jdk8Module", this.moduleClassLoader);
      Module jdk8Module = BeanUtils.newInstance(jdk8ModuleClass);
      modulesToRegister.set(jdk8Module.getTypeId(), jdk8Module);
    }
    catch (ClassNotFoundException ex) {
      // jackson-datatype-jdk8 not available
    }

    try {
      Class<? extends Module> javaTimeModuleClass = ClassUtils.forName(
              "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", this.moduleClassLoader);
      Module javaTimeModule = BeanUtils.newInstance(javaTimeModuleClass);
      modulesToRegister.set(javaTimeModule.getTypeId(), javaTimeModule);
    }
    catch (ClassNotFoundException ex) {
      // jackson-datatype-jsr310 not available
    }

  }

  // Convenience factory methods

  /**
   * Obtain a {@link Jackson2ObjectMapperBuilder} instance in order to
   * build a regular JSON {@link ObjectMapper} instance.
   */
  public static Jackson2ObjectMapperBuilder json() {
    return new Jackson2ObjectMapperBuilder();
  }

  /**
   * Obtain a {@link Jackson2ObjectMapperBuilder} instance in order to
   * build an {@link XmlMapper} instance.
   */
  public static Jackson2ObjectMapperBuilder xml() {
    return new Jackson2ObjectMapperBuilder().createXmlMapper(true);
  }

  /**
   * Obtain a {@link Jackson2ObjectMapperBuilder} instance in order to
   * build a Smile data format {@link ObjectMapper} instance.
   */
  public static Jackson2ObjectMapperBuilder smile() {
    return new Jackson2ObjectMapperBuilder().factory(new SmileFactoryInitializer().create());
  }

  /**
   * Obtain a {@link Jackson2ObjectMapperBuilder} instance in order to
   * build a CBOR data format {@link ObjectMapper} instance.
   */
  public static Jackson2ObjectMapperBuilder cbor() {
    return new Jackson2ObjectMapperBuilder().factory(new CborFactoryInitializer().create());
  }

  private static class XmlObjectMapperInitializer {

    private static final XMLResolver NO_OP_XML_RESOLVER =
            (publicID, systemID, base, ns) -> InputStream.nullInputStream();

    public ObjectMapper create(@Nullable JsonFactory factory) {
      if (factory != null) {
        return new XmlMapper((XmlFactory) factory);
      }
      else {
        return new XmlMapper(createDefensiveInputFactory());
      }
    }

    public ObjectMapper create(boolean defaultUseWrapper, @Nullable JsonFactory factory) {
      JacksonXmlModule module = new JacksonXmlModule();
      module.setDefaultUseWrapper(defaultUseWrapper);
      if (factory != null) {
        return new XmlMapper((XmlFactory) factory, module);
      }
      else {
        return new XmlMapper(new XmlFactory(createDefensiveInputFactory()), module);
      }
    }

    /**
     * Create an {@link XMLInputFactory} with  defensive setup,
     * i.e. no support for the resolution of DTDs and external entities.
     *
     * @return a new defensively initialized input factory instance to use
     */
    public static XMLInputFactory createDefensiveInputFactory() {
      return createDefensiveInputFactory(XMLInputFactory::newInstance);
    }

    /**
     * Variant of {@link #createDefensiveInputFactory()} with a custom instance.
     *
     * @param instanceSupplier supplier for the input factory instance
     * @return a new defensively initialized input factory instance to use
     */
    public static <T extends XMLInputFactory> T createDefensiveInputFactory(Supplier<T> instanceSupplier) {
      T inputFactory = instanceSupplier.get();
      inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      inputFactory.setXMLResolver(NO_OP_XML_RESOLVER);
      return inputFactory;
    }
  }

  private static class SmileFactoryInitializer {

    public JsonFactory create() {
      return new SmileFactory();
    }
  }

  private static class CborFactoryInitializer {

    public JsonFactory create() {
      return new CBORFactory();
    }
  }

}
