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

package infra.annotation.config.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.Ordered;
import infra.stereotype.Component;
import infra.util.PropertyMapper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Gson.
 *
 * @author David Liu
 * @author Ivan Golovko
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDIAutoConfiguration
@ConditionalOnClass(Gson.class)
@EnableConfigurationProperties(GsonProperties.class)
public class GsonAutoConfiguration {

  private GsonAutoConfiguration() {
  }

  @Component
  @ConditionalOnMissingBean
  public static GsonBuilder gsonBuilder(List<GsonBuilderCustomizer> customizers) {
    GsonBuilder builder = new GsonBuilder();
    customizers.forEach((c) -> c.customize(builder));
    return builder;
  }

  @Component
  @ConditionalOnMissingBean
  public static Gson gson(GsonBuilder gsonBuilder) {
    return gsonBuilder.create();
  }

  @Component
  public static StandardGsonBuilderCustomizer standardGsonBuilderCustomizer(GsonProperties gsonProperties) {
    return new StandardGsonBuilderCustomizer(gsonProperties);
  }

  static final class StandardGsonBuilderCustomizer implements GsonBuilderCustomizer, Ordered {

    private final GsonProperties properties;

    StandardGsonBuilderCustomizer(GsonProperties properties) {
      this.properties = properties;
    }

    @Override
    public int getOrder() {
      return 0;
    }

    @Override
    public void customize(GsonBuilder builder) {
      GsonProperties properties = this.properties;
      PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
      map.from(properties::getGenerateNonExecutableJson).whenTrue().toCall(builder::generateNonExecutableJson);
      map.from(properties::getExcludeFieldsWithoutExposeAnnotation)
              .whenTrue()
              .toCall(builder::excludeFieldsWithoutExposeAnnotation);
      map.from(properties::getSerializeNulls).whenTrue().toCall(builder::serializeNulls);
      map.from(properties::getEnableComplexMapKeySerialization)
              .whenTrue()
              .toCall(builder::enableComplexMapKeySerialization);
      map.from(properties::getDisableInnerClassSerialization)
              .whenTrue()
              .toCall(builder::disableInnerClassSerialization);
      map.from(properties::getLongSerializationPolicy).to(builder::setLongSerializationPolicy);
      map.from(properties::getFieldNamingPolicy).to(builder::setFieldNamingPolicy);
      map.from(properties::getPrettyPrinting).whenTrue().toCall(builder::setPrettyPrinting);
      map.from(properties::getLenient).whenTrue().toCall(builder::setLenient);
      map.from(properties::getDisableHtmlEscaping).whenTrue().toCall(builder::disableHtmlEscaping);
      map.from(properties::getDateFormat).to(builder::setDateFormat);
    }

  }

}
