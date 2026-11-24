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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(MessageSourceProperties.class)
@ImportRuntimeHints(MessageSourceAutoConfiguration.Hints.class)
public final class MessageSourceAutoConfiguration {

  @Component
  @ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
  public static ResourceBasedMessageSourceConfigurer resourceBasedMessageSourceConfigurer(
          MessageSourceProperties properties) {
    return new ResourceBasedMessageSourceConfigurer(properties);
  }

  @Component
  @ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME, search = SearchStrategy.CURRENT)
  @Conditional(MessageSourceAutoConfiguration.ResourceBundleCondition.class)
  public static MessageSource messageSource(ResourceBasedMessageSourceConfigurer configurer) {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    configurer.configure(messageSource);
    return messageSource;
  }

  protected static class ResourceBundleCondition extends InfraCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
      String basename = context.getEnvironment().getProperty("messages.basename", "messages");
      ClassLoader classLoader = context.getClassLoader();
      return Cache.forClassLoader(classLoader)
              .computeIfAbsent(basename, () -> getMatchOutcomeForBasename(classLoader, basename));
    }

    private ConditionOutcome getMatchOutcomeForBasename(@Nullable ClassLoader classLoader, String basename) {
      ConditionMessage.Builder message = ConditionMessage.forCondition("ResourceBundle");
      for (String name : StringUtils.tokenizeToStringArray(basename, ",")) {
        for (Resource resource : getResources(classLoader, name)) {
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

  static final class Cache {

    private static final Cache none = new Cache(null);

    private static final Map<ClassLoader, Cache> classLoaderCaches = new ConcurrentReferenceHashMap<>(16,
            ConcurrentReferenceHashMap.ReferenceType.SOFT);

    private final @Nullable Map<String, ConditionOutcome> outcomes;

    private Cache(@Nullable Map<String, ConditionOutcome> outcomes) {
      this.outcomes = outcomes;
    }

    ConditionOutcome computeIfAbsent(String basename, Supplier<ConditionOutcome> action) {
      return (this.outcomes != null) ? this.outcomes.computeIfAbsent(basename, (key) -> action.get()) : action.get();
    }

    static Cache forClassLoader(@Nullable ClassLoader classLoader) {
      return (classLoader != null) ? classLoaderCaches.computeIfAbsent(classLoader,
              (key) -> new Cache(new ConcurrentReferenceHashMap<>())) : none;
    }

  }

  static class Hints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      hints.resources().registerPattern("messages.properties").registerPattern("messages_*.properties");
    }

  }

}
