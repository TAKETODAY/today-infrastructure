/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.annotation.config.context;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.context.MessageSource;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.ImportRuntimeHints;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.AutoConfigureOrder;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionMessage;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.InfraCondition;
import infra.context.condition.SearchStrategy;
import infra.context.properties.EnableConfigurationProperties;
import infra.context.support.AbstractApplicationContext;
import infra.context.support.ResourceBundleMessageSource;
import infra.core.Ordered;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.Resource;
import infra.core.type.AnnotatedTypeMetadata;
import infra.lang.Nullable;
import infra.stereotype.Component;
import infra.util.ConcurrentReferenceHashMap;
import infra.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link MessageSource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDIAutoConfiguration
@ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME, search = SearchStrategy.CURRENT)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Conditional(MessageSourceAutoConfiguration.ResourceBundleCondition.class)
@EnableConfigurationProperties(MessageSourceProperties.class)
@ImportRuntimeHints(MessageSourceAutoConfiguration.Hints.class)
public class MessageSourceAutoConfiguration {

  @Component
  public static MessageSource messageSource(MessageSourceProperties properties) {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    if (StringUtils.hasText(properties.getBasename())) {
      messageSource.setBasenames(
              StringUtils.commaDelimitedListToStringArray(
                      StringUtils.trimAllWhitespace(properties.getBasename())
              )
      );
    }
    if (properties.getEncoding() != null) {
      messageSource.setDefaultEncoding(properties.getEncoding().name());
    }
    messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
    Duration cacheDuration = properties.getCacheDuration();
    if (cacheDuration != null) {
      messageSource.setCacheMillis(cacheDuration.toMillis());
    }
    messageSource.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());
    messageSource.setUseCodeAsDefaultMessage(properties.isUseCodeAsDefaultMessage());
    return messageSource;
  }

  protected static class ResourceBundleCondition extends InfraCondition {

    private static final ConcurrentReferenceHashMap<String, ConditionOutcome>
            cache = new ConcurrentReferenceHashMap<>();

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
      String basename = context.getEnvironment().getProperty("infra.messages.basename", "messages");
      ConditionOutcome outcome = cache.get(basename);
      if (outcome == null) {
        outcome = getMatchOutcomeForBasename(context, basename);
        cache.put(basename, outcome);
      }
      return outcome;
    }

    private ConditionOutcome getMatchOutcomeForBasename(ConditionContext context, String basename) {
      ConditionMessage.Builder message = ConditionMessage.forCondition("ResourceBundle");
      for (String name : StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(basename))) {
        for (Resource resource : getResources(context.getClassLoader(), name)) {
          if (resource.exists()) {
            return ConditionOutcome.match(message.found("bundle").items(resource));
          }
        }
      }
      return ConditionOutcome.noMatch(message.didNotFind("bundle with basename " + basename).atAll());
    }

    private Set<Resource> getResources(@Nullable ClassLoader classLoader, String name) {
      String target = name.replace('.', '/');
      try {
        return new PathMatchingPatternResourceLoader(classLoader)
                .getResources("classpath*:%s.properties".formatted(target));
      }
      catch (Exception ex) {
        return Collections.emptySet();
      }
    }

  }

  static class Hints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      hints.resources().registerPattern("messages.properties").registerPattern("messages_*.properties");
    }

  }

}
