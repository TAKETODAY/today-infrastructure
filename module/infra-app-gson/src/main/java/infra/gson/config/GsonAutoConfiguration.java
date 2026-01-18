/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.gson.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import infra.context.annotation.Lazy;
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
@Lazy
@DisableDIAutoConfiguration
@ConditionalOnClass(Gson.class)
@EnableConfigurationProperties(GsonProperties.class)
public final class GsonAutoConfiguration {

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
    @SuppressWarnings("NullAway")
    public void customize(GsonBuilder builder) {
      GsonProperties properties = this.properties;
      PropertyMapper map = PropertyMapper.get();
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
