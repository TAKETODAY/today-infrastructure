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

package cn.taketoday.annotation.config.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.util.PropertyMapper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Gson.
 *
 * @author David Liu
 * @author Ivan Golovko
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@AutoConfiguration
@ConditionalOnClass(Gson.class)
@EnableConfigurationProperties(GsonProperties.class)
public class GsonAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public GsonBuilder gsonBuilder(List<GsonBuilderCustomizer> customizers) {
    GsonBuilder builder = new GsonBuilder();
    customizers.forEach((c) -> c.customize(builder));
    return builder;
  }

  @Bean
  @ConditionalOnMissingBean
  public Gson gson(GsonBuilder gsonBuilder) {
    return gsonBuilder.create();
  }

  @Bean
  public StandardGsonBuilderCustomizer standardGsonBuilderCustomizer(GsonProperties gsonProperties) {
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
      map.from(properties::getGenerateNonExecutableJson).toCall(builder::generateNonExecutableJson);
      map.from(properties::getExcludeFieldsWithoutExposeAnnotation)
              .toCall(builder::excludeFieldsWithoutExposeAnnotation);
      map.from(properties::getSerializeNulls).whenTrue().toCall(builder::serializeNulls);
      map.from(properties::getEnableComplexMapKeySerialization).toCall(builder::enableComplexMapKeySerialization);
      map.from(properties::getDisableInnerClassSerialization).toCall(builder::disableInnerClassSerialization);
      map.from(properties::getLongSerializationPolicy).to(builder::setLongSerializationPolicy);
      map.from(properties::getFieldNamingPolicy).to(builder::setFieldNamingPolicy);
      map.from(properties::getPrettyPrinting).toCall(builder::setPrettyPrinting);
      map.from(properties::getLenient).toCall(builder::setLenient);
      map.from(properties::getDisableHtmlEscaping).toCall(builder::disableHtmlEscaping);
      map.from(properties::getDateFormat).to(builder::setDateFormat);
    }

  }

}
