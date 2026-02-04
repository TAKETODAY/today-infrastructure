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

package infra.groovy.template.config;

import org.jspecify.annotations.Nullable;

import groovy.text.markup.MarkupTemplateEngine;
import infra.app.config.ConditionalOnWebApplication;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.core.env.ConfigurableEnvironment;
import infra.core.i18n.LocaleContextHolder;
import infra.stereotype.Component;
import infra.util.PropertyMapper;
import infra.web.view.UrlBasedViewResolver;
import infra.web.view.groovy.GroovyMarkupConfig;
import infra.web.view.groovy.GroovyMarkupConfigurer;
import infra.web.view.groovy.GroovyMarkupViewResolver;

/**
 * Auto-configuration support for Groovy templates in MVC. By default creates a
 * {@link MarkupTemplateEngine} configured from {@link GroovyTemplateProperties}, but you
 * can override that by providing your own {@link GroovyMarkupConfig} or even a
 * {@link MarkupTemplateEngine} of a different type.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@AutoConfiguration
@ConditionalOnClass(MarkupTemplateEngine.class)
@EnableConfigurationProperties(GroovyTemplateProperties.class)
public final class GroovyTemplateAutoConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(GroovyMarkupConfigurer.class)
  static class GroovyMarkupConfiguration {

    @Component
    @ConditionalOnMissingBean(GroovyMarkupConfig.class)
    static GroovyMarkupConfigurer groovyMarkupConfigurer(ConfigurableEnvironment environment,
            @Nullable MarkupTemplateEngine templateEngine, GroovyTemplateProperties properties) {
      GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
      PropertyMapper map = PropertyMapper.get();
      map.from(properties::isAutoEscape).to(configurer::setAutoEscape);
      map.from(properties::isAutoIndent).to(configurer::setAutoIndent);
      map.from(properties::getAutoIndentString).to(configurer::setAutoIndentString);
      map.from(properties::isAutoNewLine).to(configurer::setAutoNewLine);
      map.from(properties::getBaseTemplateClass).to(configurer::setBaseTemplateClass);
      map.from(properties::isCache).to(configurer::setCacheTemplates);
      map.from(properties::getDeclarationEncoding).to(configurer::setDeclarationEncoding);
      map.from(properties::isExpandEmptyElements).to(configurer::setExpandEmptyElements);
      map.from(properties::getLocale).to(configurer::setLocale);
      map.from(properties::getNewLineString).to(configurer::setNewLineString);
      map.from(properties::getResourceLoaderPath).to(configurer::setResourceLoaderPath);
      map.from(properties::isUseDoubleQuotes).to(configurer::setUseDoubleQuotes);
      Binder.get(environment).bind("groovy.template.configuration", Bindable.ofInstance(configurer));
      if (templateEngine != null) {
        configurer.setTemplateEngine(templateEngine);
      }
      return configurer;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ LocaleContextHolder.class, UrlBasedViewResolver.class })
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.MVC)
  static class GroovyWebConfiguration {

    @Component
    @ConditionalOnMissingBean(name = "groovyMarkupViewResolver")
    static GroovyMarkupViewResolver groovyMarkupViewResolver(GroovyTemplateProperties properties) {
      GroovyMarkupViewResolver resolver = new GroovyMarkupViewResolver();
      properties.applyToMvcViewResolver(resolver);
      return resolver;
    }

  }

}
