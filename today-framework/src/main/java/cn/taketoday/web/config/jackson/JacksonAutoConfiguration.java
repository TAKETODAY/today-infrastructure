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

package cn.taketoday.web.config.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Prototype;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.config.jackson.JacksonProperties.ConstructorDetectorStrategy;

/**
 * Auto configuration for Jackson. The following auto-configuration will get applied:
 * <ul>
 * <li>an {@link ObjectMapper} in case none is already configured.</li>
 * <li>a {@link Jackson2ObjectMapperBuilder} in case none is already configured.</li>
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
@DisableAllDependencyInjection
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ObjectMapper.class)
@EnableConfigurationProperties(JacksonProperties.class)
public class JacksonAutoConfiguration {

  private static final Map<?, Boolean> FEATURE_DEFAULTS = Map.of(
          SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false,
          SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false
  );

  @Component
  public JsonComponentModule jsonComponentModule() {
    return new JsonComponentModule();
  }

  @Primary
  @Component
  @ConditionalOnMissingBean
  ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
    return builder.createXmlMapper(false).build();
  }

  @Prototype
  @ConditionalOnMissingBean
  Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder(
          ApplicationContext applicationContext, List<Jackson2ObjectMapperBuilderCustomizer> customizers) {
    Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
    builder.applicationContext(applicationContext);
    customize(builder, customizers);
    return builder;
  }

  private void customize(
          Jackson2ObjectMapperBuilder builder, List<Jackson2ObjectMapperBuilderCustomizer> customizers) {
    for (Jackson2ObjectMapperBuilderCustomizer customizer : customizers) {
      customizer.customize(builder);
    }
  }

  @Component
  StandardJackson2ObjectMapperBuilderCustomizer standardJacksonObjectMapperBuilderCustomizer(
          ApplicationContext applicationContext, JacksonProperties jacksonProperties) {
    return new StandardJackson2ObjectMapperBuilderCustomizer(applicationContext, jacksonProperties);
  }

  static final class StandardJackson2ObjectMapperBuilderCustomizer
          implements Jackson2ObjectMapperBuilderCustomizer, Ordered {

    private final JacksonProperties jacksonProperties;
    private final ApplicationContext applicationContext;

    StandardJackson2ObjectMapperBuilderCustomizer(
            ApplicationContext applicationContext, JacksonProperties jacksonProperties) {
      this.applicationContext = applicationContext;
      this.jacksonProperties = jacksonProperties;
    }

    @Override
    public int getOrder() {
      return 0;
    }

    @Override
    public void customize(Jackson2ObjectMapperBuilder builder) {
      if (jacksonProperties.getDefaultPropertyInclusion() != null) {
        builder.serializationInclusion(jacksonProperties.getDefaultPropertyInclusion());
      }
      if (jacksonProperties.getTimeZone() != null) {
        builder.timeZone(jacksonProperties.getTimeZone());
      }
      configureFeatures(builder, FEATURE_DEFAULTS);
      configureVisibility(builder, jacksonProperties.getVisibility());
      configureFeatures(builder, jacksonProperties.getDeserialization());
      configureFeatures(builder, jacksonProperties.getSerialization());
      configureFeatures(builder, jacksonProperties.getMapper());
      configureFeatures(builder, jacksonProperties.getParser());
      configureFeatures(builder, jacksonProperties.getGenerator());
      configureDateFormat(builder);
      configurePropertyNamingStrategy(builder);
      configureModules(builder);
      configureLocale(builder);
      configureDefaultLeniency(builder);
      configureConstructorDetector(builder);
    }

    private void configureFeatures(Jackson2ObjectMapperBuilder builder, Map<?, Boolean> features) {
      for (Map.Entry<?, Boolean> entry : features.entrySet()) {
        Boolean value = entry.getValue();
        Object feature = entry.getKey();
        if (value != null) {
          if (value) {
            builder.featuresToEnable(feature);
          }
          else {
            builder.featuresToDisable(feature);
          }
        }
      }
    }

    private void configureVisibility(Jackson2ObjectMapperBuilder builder,
            Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities) {

      for (Map.Entry<PropertyAccessor, JsonAutoDetect.Visibility> entry : visibilities.entrySet()) {
        builder.visibility(entry.getKey(), entry.getValue());
      }
    }

    private void configureDateFormat(Jackson2ObjectMapperBuilder builder) {
      // We support a fully qualified class name extending DateFormat or a date
      // pattern string value
      String dateFormat = jacksonProperties.getDateFormat();
      if (dateFormat != null) {
        try {
          Class<DateFormat> dateFormatClass = ClassUtils.forName(dateFormat, null);
          builder.dateFormat(BeanUtils.newInstance(dateFormatClass));
        }
        catch (ClassNotFoundException ex) {
          SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
          // Since Jackson 2.6.3 we always need to set a TimeZone (see
          // gh-4170). If none in our properties fallback to the Jackson's
          // default
          TimeZone timeZone = jacksonProperties.getTimeZone();
          if (timeZone == null) {
            timeZone = new ObjectMapper().getSerializationConfig().getTimeZone();
          }
          simpleDateFormat.setTimeZone(timeZone);
          builder.dateFormat(simpleDateFormat);
        }
      }
    }

    private void configurePropertyNamingStrategy(Jackson2ObjectMapperBuilder builder) {
      // We support a fully qualified class name extending Jackson's
      // PropertyNamingStrategy or a string value corresponding to the constant
      // names in PropertyNamingStrategy which hold default provided
      // implementations
      String strategy = jacksonProperties.getPropertyNamingStrategy();
      if (strategy != null) {
        try {
          configurePropertyNamingStrategyClass(builder, ClassUtils.forName(strategy, null));
        }
        catch (ClassNotFoundException ex) {
          configurePropertyNamingStrategyField(builder, strategy);
        }
      }
    }

    private void configurePropertyNamingStrategyClass(
            Jackson2ObjectMapperBuilder builder, Class<PropertyNamingStrategy> propertyNamingStrategyClass) {
      builder.propertyNamingStrategy(BeanUtils.newInstance(propertyNamingStrategyClass));
    }

    private void configurePropertyNamingStrategyField(Jackson2ObjectMapperBuilder builder, String fieldName) {
      // Find the field (this way we automatically support new constants
      // that may be added by Jackson in the future)
      Field field = findPropertyNamingStrategyField(fieldName);
      if (field == null) {
        throw new IllegalArgumentException("Constant named '" + fieldName + "' not found");
      }
      try {
        builder.propertyNamingStrategy((PropertyNamingStrategy) field.get(null));
      }
      catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    }

    private Field findPropertyNamingStrategyField(String fieldName) {
      try {
        return ReflectionUtils.findField(PropertyNamingStrategies.class, fieldName, PropertyNamingStrategy.class);
      }
      catch (NoClassDefFoundError ex) { // Fallback pre Jackson 2.12
        return ReflectionUtils.findField(PropertyNamingStrategy.class, fieldName, PropertyNamingStrategy.class);
      }
    }

    private void configureModules(Jackson2ObjectMapperBuilder builder) {
      Collection<Module> moduleBeans = getBeans(applicationContext, Module.class);
      builder.modulesToInstall(moduleBeans.toArray(new Module[0]));
    }

    private void configureLocale(Jackson2ObjectMapperBuilder builder) {
      Locale locale = jacksonProperties.getLocale();
      if (locale != null) {
        builder.locale(locale);
      }
    }

    private void configureDefaultLeniency(Jackson2ObjectMapperBuilder builder) {
      Boolean defaultLeniency = jacksonProperties.getDefaultLeniency();
      if (defaultLeniency != null) {
        builder.postConfigurer(mapper -> mapper.setDefaultLeniency(defaultLeniency));
      }
    }

    private void configureConstructorDetector(Jackson2ObjectMapperBuilder builder) {
      ConstructorDetectorStrategy strategy = jacksonProperties.getConstructorDetector();
      if (strategy != null) {
        builder.postConfigurer(mapper -> {
          switch (strategy) {
            default -> mapper.setConstructorDetector(ConstructorDetector.DEFAULT);
            case EXPLICIT_ONLY -> mapper.setConstructorDetector(ConstructorDetector.EXPLICIT_ONLY);
            case USE_DELEGATING -> mapper.setConstructorDetector(ConstructorDetector.USE_DELEGATING);
            case USE_PROPERTIES_BASED -> mapper.setConstructorDetector(ConstructorDetector.USE_PROPERTIES_BASED);
          }
        });
      }
    }

    private static <T> Collection<T> getBeans(BeanFactory beanFactory, Class<T> type) {
      return BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, type).values();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(ParameterNamesModule.class)
  static class ParameterNamesModuleConfiguration {

    @Component
    @ConditionalOnMissingBean
    ParameterNamesModule parameterNamesModule() {
      return new ParameterNamesModule(JsonCreator.Mode.DEFAULT);
    }

  }

}
