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

package infra.context.config;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import infra.context.support.AbstractResourceBasedMessageSource;
import infra.core.io.PropertiesUtils;
import infra.core.io.Resource;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * Configure an {@link AbstractResourceBasedMessageSource} with sensible defaults tuned
 * using configuration properties.
 *
 * <p>Can be injected into application code and used to define a custom message source whose
 * configuration is based upon that produced by auto-configuration.
 *
 * @author Henrique (henriquejsza)
 * @since 5.0
 */
public class ResourceBasedMessageSourceConfigurer {

  private final MessageSourceProperties properties;

  /**
   * Creates a new configurer that will use the given {@code properties}.
   *
   * @param properties properties to use
   */
  public ResourceBasedMessageSourceConfigurer(MessageSourceProperties properties) {
    Assert.notNull(properties, "'properties' is required");
    this.properties = properties;
  }

  /**
   * Configure the specified message source. The message source can be further tuned and
   * default settings can be overridden.
   *
   * @param messageSource the {@link AbstractResourceBasedMessageSource} instance to
   * configure
   */
  public void configure(AbstractResourceBasedMessageSource messageSource) {
    Assert.notNull(messageSource, "'messageSource' is required");
    if (StringUtils.hasText(this.properties.getBasename())) {
      messageSource.setBasenames(StringUtils.commaDelimitedListToStringArray(
              StringUtils.trimAllWhitespace(this.properties.getBasename())));
    }
    if (this.properties.getEncoding() != null) {
      messageSource.setDefaultCharset(this.properties.getEncoding());
    }
    messageSource.setFallbackToSystemLocale(this.properties.isFallbackToSystemLocale());
    Duration cacheDuration = this.properties.getCacheDuration();
    if (cacheDuration != null) {
      messageSource.setCacheMillis(cacheDuration.toMillis());
    }
    messageSource.setAlwaysUseMessageFormat(this.properties.isAlwaysUseMessageFormat());
    messageSource.setUseCodeAsDefaultMessage(this.properties.isUseCodeAsDefaultMessage());
    messageSource.setCommonMessages(loadCommonMessages(this.properties.getCommonMessages()));
  }

  private @Nullable Properties loadCommonMessages(@Nullable List<Resource> resources) {
    if (CollectionUtils.isEmpty(resources)) {
      return null;
    }
    Properties properties = CollectionUtils.createSortedProperties(false);
    for (Resource resource : resources) {
      try {
        PropertiesUtils.fillProperties(properties, resource);
      }
      catch (IOException ex) {
        throw new UncheckedIOException("Failed to load common messages from '%s'".formatted(resource), ex);
      }
    }
    return properties;
  }

}
