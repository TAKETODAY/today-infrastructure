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

package infra.webmvc.config;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import infra.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import infra.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import infra.annotation.config.task.TaskExecutionAutoConfiguration;
import infra.annotation.config.validation.ValidationAutoConfiguration;
import infra.annotation.config.validation.ValidatorAdapter;
import infra.app.test.context.assertj.AssertableApplicationContext;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.context.runner.ContextConsumer;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.config.AutoConfigurations;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.core.conversion.ConversionService;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.core.task.AsyncTaskExecutor;
import infra.format.Parser;
import infra.format.Printer;
import infra.format.support.FormattingConversionService;
import infra.http.CacheControl;
import infra.http.HttpHeaders;
import infra.http.converter.HttpMessageConverter;
import infra.mock.web.HttpMockRequestImpl;
import infra.test.util.ReflectionTestUtils;
import infra.util.StringUtils;
import infra.validation.Validator;
import infra.validation.beanvalidation.LocalValidatorFactoryBean;
import infra.web.AbstractRedirectModelManager;
import infra.web.HandlerAdapter;
import infra.web.HandlerExceptionHandler;
import infra.web.HandlerMapping;
import infra.web.LocaleResolver;
import infra.web.RedirectModel;
import infra.web.RedirectModelManager;
import infra.web.RequestContext;
import infra.web.accept.ApiVersionDeprecationHandler;
import infra.web.accept.ApiVersionParser;
import infra.web.accept.ApiVersionResolver;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.ContentNegotiationManager;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.accept.InvalidApiVersionException;
import infra.web.accept.MissingApiVersionException;
import infra.web.accept.StandardApiVersionDeprecationHandler;
import infra.web.async.WebAsyncManagerFactory;
import infra.web.bind.support.ConfigurableWebBindingInitializer;
import infra.web.config.annotation.AsyncSupportConfigurer;
import infra.web.config.annotation.CorsRegistry;
import infra.web.config.annotation.ResourceHandlerRegistry;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.handler.AbstractHandlerExceptionHandler;
import infra.web.handler.CompositeHandlerExceptionHandler;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.handler.SimpleHandlerExceptionHandler;
import infra.web.handler.SimpleUrlHandlerMapping;
import infra.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import infra.web.handler.method.RequestMappingHandlerAdapter;
import infra.web.handler.method.RequestMappingHandlerMapping;
import infra.web.i18n.AcceptHeaderLocaleResolver;
import infra.web.i18n.FixedLocaleResolver;
import infra.web.mock.MockRequestContext;
import infra.web.resource.CachingResourceResolver;
import infra.web.resource.CachingResourceTransformer;
import infra.web.resource.ContentVersionStrategy;
import infra.web.resource.CssLinkResourceTransformer;
import infra.web.resource.EncodedResourceResolver;
import infra.web.resource.FixedVersionStrategy;
import infra.web.resource.PathResourceResolver;
import infra.web.resource.ResourceHttpRequestHandler;
import infra.web.resource.ResourceResolver;
import infra.web.resource.ResourceTransformer;
import infra.web.resource.VersionResourceResolver;
import infra.web.resource.VersionStrategy;
import infra.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import infra.web.context.StandardWebEnvironment;
import infra.web.view.AbstractView;
import infra.web.view.ContentNegotiatingViewResolver;
import infra.web.view.View;
import infra.web.view.ViewResolver;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/23 15:07
 */
class WebMvcAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = ApplicationContextRunner.forProvider(() -> {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
            context.setEnvironment(new StandardWebEnvironment());
            return context;
          })
          .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class,
                  HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class))
          .withUserConfiguration(Config.class);

  @Test
  void handlerAdaptersCreated() {
    this.contextRunner.run((context) -> {
      assertThat(context).getBeans(HandlerAdapter.class).hasSize(2);
      assertThat(context.getBean(ReturnValueHandlerManager.class).getMessageConverters()).isNotEmpty();
    });
  }

  @Test
  void handlerMappingsCreated() {
    this.contextRunner.run(context -> assertThat(context).getBeans(HandlerMapping.class).isNotEmpty());
  }

  @Test
  void resourceHandlerMapping() {
    this.contextRunner.run(context -> {
      Map<String, List<Resource>> locations = getResourceMappingLocations(context);
      assertThat(locations.get("/**")).hasSize(4);
      assertThat(locations.get("/webjars/**")).hasSize(1);
      assertThat(locations.get("/webjars/**").get(0))
              .isEqualTo(new ClassPathResource("/META-INF/resources/webjars/"));
      assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(1);
      assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(0);
      assertThat(getResourceResolvers(context, "/**")).hasSize(1);
      assertThat(getResourceTransformers(context, "/**")).hasSize(0);
    });
  }

  @Test
  void customResourceHandlerMapping() {
    this.contextRunner.withPropertyValues("web.resources.static-path-pattern:/static/**").run((context) -> {
      Map<String, List<Resource>> locations = getResourceMappingLocations(context);
      assertThat(locations.get("/static/**")).hasSize(4);
      assertThat(getResourceResolvers(context, "/static/**")).hasSize(1);
    });
  }

  @Test
  void customWebjarsHandlerMapping() {
    this.contextRunner.withPropertyValues("web.resources.webjars-path-pattern:/assets/**").run((context) -> {
      Map<String, List<Resource>> locations = getResourceMappingLocations(context);
      assertThat(locations.get("/assets/**")).hasSize(1);
      assertThat(locations.get("/assets/**").get(0))
              .isEqualTo(new ClassPathResource("/META-INF/resources/webjars/"));
      assertThat(getResourceResolvers(context, "/assets/**")).hasSize(1);
    });
  }

  @Test
  void resourceHandlerMappingOverrideWebjars() {
    this.contextRunner.withUserConfiguration(WebJars.class).run((context) -> {
      Map<String, List<Resource>> locations = getResourceMappingLocations(context);
      assertThat(locations.get("/webjars/**")).hasSize(1);
      assertThat(locations.get("/webjars/**").get(0)).isEqualTo(new ClassPathResource("/foo/"));
    });
  }

  @Test
  void resourceHandlerMappingOverrideAll() {
    this.contextRunner.withUserConfiguration(AllResources.class).run((context) -> {
      Map<String, List<Resource>> locations = getResourceMappingLocations(context);
      assertThat(locations.get("/**")).hasSize(1);
      assertThat(locations.get("/**").get(0)).isEqualTo(new ClassPathResource("/foo/"));
    });
  }

  @Test
  void resourceHandlerMappingDisabled() {
    this.contextRunner.withPropertyValues("web.resources.add-default-mappings:false")
            .run((context) -> assertThat(getResourceMappingLocations(context)).hasSize(0));
  }

  @Test
  void resourceHandlerChainEnabled() {
    this.contextRunner.withPropertyValues("web.resources.chain.enabled:true").run((context) -> {
      assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(2);
      assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(1);
      assertThat(getResourceResolvers(context, "/**")).extractingResultOf("getClass")
              .containsOnly(CachingResourceResolver.class, PathResourceResolver.class);
      assertThat(getResourceTransformers(context, "/**")).extractingResultOf("getClass")
              .containsOnly(CachingResourceTransformer.class);
    });
  }

  @Test
  void resourceHandlerFixedStrategyEnabled() {
    this.contextRunner.withPropertyValues("web.resources.chain.strategy.fixed.enabled:true",
            "web.resources.chain.strategy.fixed.version:test",
            "web.resources.chain.strategy.fixed.paths:/**/*.js").run((context) -> {
      assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(3);
      assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(2);
      assertThat(getResourceResolvers(context, "/**")).extractingResultOf("getClass").containsOnly(
              CachingResourceResolver.class, VersionResourceResolver.class, PathResourceResolver.class);
      assertThat(getResourceTransformers(context, "/**")).extractingResultOf("getClass")
              .containsOnly(CachingResourceTransformer.class, CssLinkResourceTransformer.class);
      VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(context, "/**")
              .get(1);
      assertThat(resolver.getStrategyMap().get("/**/*.js")).isInstanceOf(FixedVersionStrategy.class);
    });
  }

  @Test
  void resourceHandlerContentStrategyEnabled() {
    this.contextRunner.withPropertyValues("web.resources.chain.strategy.content.enabled:true",
            "web.resources.chain.strategy.content.paths:/**,/*.png").run((context) -> {
      assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(3);
      assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(2);
      assertThat(getResourceResolvers(context, "/**")).extractingResultOf("getClass").containsOnly(
              CachingResourceResolver.class, VersionResourceResolver.class, PathResourceResolver.class);
      assertThat(getResourceTransformers(context, "/**")).extractingResultOf("getClass")
              .containsOnly(CachingResourceTransformer.class, CssLinkResourceTransformer.class);
      VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(context, "/**")
              .get(1);
      assertThat(resolver.getStrategyMap().get("/*.png")).isInstanceOf(ContentVersionStrategy.class);
    });
  }

  @Test
  void resourceHandlerChainCustomized() {
    this.contextRunner.withPropertyValues("web.resources.chain.enabled:true",
                    "web.resources.chain.cache:false", "web.resources.chain.strategy.content.enabled:true",
                    "web.resources.chain.strategy.content.paths:/**,/*.png",
                    "web.resources.chain.strategy.fixed.enabled:true",
                    "web.resources.chain.strategy.fixed.version:test",
                    "web.resources.chain.strategy.fixed.paths:/**/*.js",
                    "web.resources.chain.html-application-cache:true", "web.resources.chain.compressed:true")
            .run((context) -> {
              assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(3);
              assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(1);
              assertThat(getResourceResolvers(context, "/**")).extractingResultOf("getClass").containsOnly(
                      EncodedResourceResolver.class, VersionResourceResolver.class, PathResourceResolver.class);
              assertThat(getResourceTransformers(context, "/**")).extractingResultOf("getClass")
                      .containsOnly(CssLinkResourceTransformer.class);
              VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(context, "/**")
                      .get(1);
              Map<String, VersionStrategy> strategyMap = resolver.getStrategyMap();
              assertThat(strategyMap.get("/*.png")).isInstanceOf(ContentVersionStrategy.class);
              assertThat(strategyMap.get("/**/*.js")).isInstanceOf(FixedVersionStrategy.class);
            });
  }

  @Test
  void defaultLocaleResolver() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(LocaleResolver.class);
      LocaleResolver localeResolver = context.getBean(LocaleResolver.class);
      assertThat(localeResolver).hasFieldOrPropertyWithValue("defaultLocale", null);
    });
  }

  @Test
  void overrideLocale() {
    this.contextRunner.withPropertyValues("web.locale:en_UK", "web.locale-resolver=fixed")
            .run((loader) -> {
              // mock request and set user preferred locale
              HttpMockRequestImpl request = new HttpMockRequestImpl();
              request.addPreferredLocale(StringUtils.parseLocaleString("nl_NL"));
              request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "nl_NL");
              LocaleResolver localeResolver = loader.getBean(LocaleResolver.class);
              assertThat(localeResolver).isInstanceOf(FixedLocaleResolver.class);
              Locale locale = localeResolver.resolveLocale(new MockRequestContext(null, request, null));
              // test locale resolver uses fixed locale and not user preferred
              // locale
              assertThat(locale.toString()).isEqualTo("en_UK");
            });
  }

  @Test
  void useAcceptHeaderLocale() {
    this.contextRunner.withPropertyValues("web.locale:en_UK").run((loader) -> {
      // mock request and set user preferred locale
      HttpMockRequestImpl request = new HttpMockRequestImpl();
      request.addPreferredLocale(StringUtils.parseLocaleString("nl_NL"));
      request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "nl_NL");
      LocaleResolver localeResolver = loader.getBean(LocaleResolver.class);
      assertThat(localeResolver).isInstanceOf(AcceptHeaderLocaleResolver.class);
      Locale locale = localeResolver.resolveLocale(new MockRequestContext(null, request, null));
      // test locale resolver uses user preferred locale
      assertThat(locale.toString()).isEqualTo("nl_NL");
    });
  }

  @Test
  void useDefaultLocaleIfAcceptHeaderNoSet() {
    this.contextRunner.withPropertyValues("web.locale:en_UK").run((context) -> {
      // mock request and set user preferred locale
      HttpMockRequestImpl request = new HttpMockRequestImpl();
      LocaleResolver localeResolver = context.getBean(LocaleResolver.class);
      assertThat(localeResolver).isInstanceOf(AcceptHeaderLocaleResolver.class);
      Locale locale = localeResolver.resolveLocale(new MockRequestContext(null, request, null));
      // test locale resolver uses default locale if no header is set
      assertThat(locale.toString()).isEqualTo("en_UK");
    });
  }

  @Test
  void customLocaleResolverWithMatchingNameReplacesAutoConfiguredLocaleResolver() {
    this.contextRunner.withBean("webLocaleResolver", CustomLocaleResolver.class, CustomLocaleResolver::new)
            .run((context) -> {
              assertThat(context).hasSingleBean(LocaleResolver.class);
              assertThat(context.getBean("webLocaleResolver")).isInstanceOf(CustomLocaleResolver.class);
            });
  }

  @Test
  void customLocaleResolverWithDifferentNameDoesNotReplaceAutoConfiguredLocaleResolver() {
    this.contextRunner.withBean("customLocaleResolver", CustomLocaleResolver.class, CustomLocaleResolver::new)
            .run((context) -> {
              assertThat(context.getBean("customLocaleResolver")).isInstanceOf(CustomLocaleResolver.class);
              assertThat(context.getBean("webLocaleResolver")).isInstanceOf(AcceptHeaderLocaleResolver.class);
            });
  }

  @Test
  void customFlashMapManagerWithMatchingNameReplacesDefaultFlashMapManager() {
    this.contextRunner.withBean("sessionRedirectModelManager", CustomFlashMapManager.class, CustomFlashMapManager::new)
            .run((context) -> {
              assertThat(context).hasSingleBean(RedirectModelManager.class);
              assertThat(context.getBean("sessionRedirectModelManager")).isInstanceOf(CustomFlashMapManager.class);
            });
  }

  @Test
  void customFlashMapManagerWithDifferentNameDoesNotReplaceDefaultFlashMapManager() {
    this.contextRunner.withBean("sessionRedirectModelManager", CustomFlashMapManager.class, CustomFlashMapManager::new)
            .run((context) -> {
              assertThat(context.getBean("sessionRedirectModelManager")).isInstanceOf(CustomFlashMapManager.class);
              assertThat(context.getBean("sessionRedirectModelManager")).isInstanceOf(RedirectModelManager.class);
            });
  }

  @Test
  void defaultDateFormat() {
    this.contextRunner.run((context) -> {
      FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
      Date date = Date.from(ZonedDateTime.of(1988, 6, 25, 20, 30, 0, 0, ZoneId.systemDefault()).toInstant());
      // formatting conversion service should use simple toString()
      assertThat(conversionService.convert(date, String.class)).isEqualTo(date.toString());
    });
  }

  @Test
  void customDateFormat() {
    this.contextRunner.withPropertyValues("web.mvc.format.date:dd*MM*yyyy").run((context) -> {
      FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
      Date date = Date.from(ZonedDateTime.of(1988, 6, 25, 20, 30, 0, 0, ZoneId.systemDefault()).toInstant());
      assertThat(conversionService.convert(date, String.class)).isEqualTo("25*06*1988");
    });
  }

  @Test
  void defaultTimeFormat() {
    this.contextRunner.run((context) -> {
      FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
      LocalTime time = LocalTime.of(11, 43, 10);
      assertThat(conversionService.convert(time, String.class))
              .isEqualTo(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time));
    });
  }

  @Test
  void customTimeFormat() {
    this.contextRunner.withPropertyValues("web.mvc.format.time=HH:mm:ss").run((context) -> {
      FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
      LocalTime time = LocalTime.of(11, 43, 10);
      assertThat(conversionService.convert(time, String.class)).isEqualTo("11:43:10");
    });
  }

  @Test
  void defaultDateTimeFormat() {
    this.contextRunner.run((context) -> {
      FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
      LocalDateTime dateTime = LocalDateTime.of(2020, 4, 28, 11, 43, 10);
      assertThat(conversionService.convert(dateTime, String.class))
              .isEqualTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(dateTime));
    });
  }

  @Test
  void customDateTimeTimeFormat() {
    this.contextRunner.withPropertyValues("web.mvc.format.date-time=yyyy-MM-dd HH:mm:ss").run((context) -> {
      FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
      LocalDateTime dateTime = LocalDateTime.of(2020, 4, 28, 11, 43, 10);
      assertThat(conversionService.convert(dateTime, String.class)).isEqualTo("2020-04-28 11:43:10");
    });
  }

  @Test
  void noMessageCodesResolver() {
    this.contextRunner.run(
            (context) -> assertThat(context.getBean(WebMvcAutoConfiguration.class).getMessageCodesResolver())
                    .isNull());
  }

  @Test
  void overrideMessageCodesFormat() {
    this.contextRunner.withPropertyValues("web.mvc.messageCodesResolverFormat:POSTFIX_ERROR_CODE")
            .run(context -> assertThat(context.getBean(WebMvcAutoConfiguration.class).getMessageCodesResolver())
                    .isNotNull());
  }

  @Test
  void customViewResolver() {
    this.contextRunner.withUserConfiguration(CustomViewResolver.class)
            .run((context) -> assertThat(context.getBean("viewResolver")).isInstanceOf(MyViewResolver.class));
  }

  @Test
  void customContentNegotiatingViewResolver() {
    this.contextRunner.withUserConfiguration(CustomContentNegotiatingViewResolver.class)
            .run((context) -> assertThat(context).getBeanNames(ContentNegotiatingViewResolver.class)
                    .containsOnly("myViewResolver"));
  }

  @Test
  void defaultAsyncRequestTimeout() {
    this.contextRunner.run((context) -> assertThat(context.getBean(WebAsyncManagerFactory.class))
            .extracting("asyncRequestTimeout").isNull());
  }

  @Test
  void customAsyncRequestTimeout() {
    this.contextRunner.withPropertyValues("web.mvc.async.request-timeout:12345")
            .run((context) -> assertThat(context.getBean(WebAsyncManagerFactory.class))
                    .extracting("asyncRequestTimeout").isEqualTo(12345L));
  }

  @Test
  void asyncTaskExecutorWithApplicationTaskExecutor() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
            .run((context) -> {
              assertThat(context).hasSingleBean(AsyncTaskExecutor.class);
              assertThat(context.getBean(WebAsyncManagerFactory.class)).extracting("taskExecutor")
                      .isSameAs(context.getBean("applicationTaskExecutor"));
            });
  }

  @Test
  void asyncTaskExecutorWithNonMatchApplicationTaskExecutorBean() {
    this.contextRunner.withUserConfiguration(CustomApplicationTaskExecutorConfig.class)
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class)).run((context) -> {
              assertThat(context).doesNotHaveBean(AsyncTaskExecutor.class);
              assertThat(context.getBean(WebAsyncManagerFactory.class)).extracting("taskExecutor")
                      .isNotSameAs(context.getBean("applicationTaskExecutor"));
            });
  }

  @Test
  void asyncTaskExecutorWithMvcConfigurerCanOverrideExecutor() {
    this.contextRunner.withUserConfiguration(CustomAsyncTaskExecutorConfigurer.class)
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
            .run((context) -> {
              assertThat(context.getBean(WebAsyncManagerFactory.class))
                      .extracting("taskExecutor")
                      .isSameAs(context.getBean(CustomAsyncTaskExecutorConfigurer.class).taskExecutor);
            });
  }

  @Test
  void asyncTaskExecutorWithCustomNonApplicationTaskExecutor() {
    this.contextRunner.withUserConfiguration(CustomAsyncTaskExecutorConfig.class)
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class)).run((context) -> {
              assertThat(context).hasSingleBean(AsyncTaskExecutor.class);
              assertThat(context.getBean(WebAsyncManagerFactory.class)).extracting("taskExecutor")
                      .isNotSameAs(context.getBean("customTaskExecutor"));
            });
  }

  @Test
  void customConfigurableWebBindingInitializer() {
    this.contextRunner.withUserConfiguration(CustomConfigurableWebBindingInitializer.class)
            .run(context ->
                    assertThat(context.getBean(RequestMappingHandlerAdapter.class).getWebBindingInitializer())
                            .isInstanceOf(CustomWebBindingInitializer.class));
  }

  @Test
  void customRequestMappingHandlerMapping() {
    this.contextRunner.withUserConfiguration(CustomRequestMappingHandlerMapping.class).run((context) -> {
      assertThat(context).getBean(RequestMappingHandlerMapping.class)
              .isInstanceOf(MyRequestMappingHandlerMapping.class);
      assertThat(context.getBean(CustomRequestMappingHandlerMapping.class).handlerMappings).isEqualTo(1);
    });
  }

  @Test
  void customRequestMappingHandlerAdapter() {
    this.contextRunner.withUserConfiguration(CustomRequestMappingHandlerAdapter.class).run((context) -> {
      assertThat(context).getBean(RequestMappingHandlerAdapter.class)
              .isInstanceOf(MyRequestMappingHandlerAdapter.class);
      assertThat(context.getBean(CustomRequestMappingHandlerAdapter.class).handlerAdapters).isEqualTo(1);
    });
  }

  @Test
  void customExceptionHandlerExceptionHandler() {
    this.contextRunner.withUserConfiguration(CustomExceptionHandlerExceptionHandler.class)
            .run((context) -> assertThat(
                    context.getBean(CustomExceptionHandlerExceptionHandler.class).exceptionResolvers)
                    .isEqualTo(1));
  }

  @Test
  void multipleWebMvcRegistrations() {
    this.contextRunner.withUserConfiguration(MultipleWebMvcRegistrations.class).run((context) -> {
      assertThat(context.getBean(RequestMappingHandlerMapping.class))
              .isNotInstanceOf(MyRequestMappingHandlerMapping.class);
      assertThat(context.getBean(RequestMappingHandlerAdapter.class))
              .isNotInstanceOf(MyRequestMappingHandlerAdapter.class);
    });
  }

  @Test
  void defaultLogResolvedException() {
    this.contextRunner.run(assertExceptionResolverWarnLoggers((logger) -> assertThat(logger).isNull()));
  }

  @Test
  void customLogResolvedException() {
    this.contextRunner.withPropertyValues("web.mvc.log-resolved-exception:true")
            .run(assertExceptionResolverWarnLoggers((logger) -> assertThat(logger).isNotNull()));
  }

  private ContextConsumer<AssertableApplicationContext> assertExceptionResolverWarnLoggers(Consumer<Object> consumer) {
    return (context) -> {
      HandlerExceptionHandler resolver = context.getBean(HandlerExceptionHandler.class);
      assertThat(resolver).isInstanceOf(CompositeHandlerExceptionHandler.class);
      List<HandlerExceptionHandler> delegates = ((CompositeHandlerExceptionHandler) resolver)
              .getExceptionHandlers();
      for (HandlerExceptionHandler delegate : delegates) {
        if (delegate instanceof AbstractHandlerExceptionHandler
                && !(delegate instanceof SimpleHandlerExceptionHandler)) {
          consumer.accept(ReflectionTestUtils.getField(delegate, "warnLogger"));
        }
      }
    };
  }

  @Test
  void validatorWhenNoValidatorShouldUseDefault() {
    this.contextRunner.run((context) -> {
      assertThat(context).doesNotHaveBean(ValidatorFactory.class);
      assertThat(context).doesNotHaveBean(jakarta.validation.Validator.class);
      assertThat(context).getBeanNames(Validator.class).containsOnly("mvcValidator");
    });
  }

  @Test
  void validatorWhenNoCustomizationShouldUseAutoConfigured() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .run((context) -> {
              assertThat(context).getBeanNames(jakarta.validation.Validator.class)
                      .containsOnly("defaultValidator");
              assertThat(context).getBeanNames(Validator.class).containsOnly("defaultValidator", "mvcValidator");
              Validator validator = context.getBean("mvcValidator", Validator.class);
              assertThat(validator).isInstanceOf(ValidatorAdapter.class);
              Object defaultValidator = context.getBean("defaultValidator");
              assertThat(((ValidatorAdapter) validator).getTarget()).isSameAs(defaultValidator);
              // Primary Infra validator is the one used by MVC behind the scenes
              assertThat(context.getBean(Validator.class)).isEqualTo(defaultValidator);
            });
  }

  @Test
  void validatorWithConfigurerAloneShouldUseInfraValidator() {
    this.contextRunner.withUserConfiguration(MvcValidator.class).run((context) -> {
      assertThat(context).doesNotHaveBean(ValidatorFactory.class);
      assertThat(context).doesNotHaveBean(jakarta.validation.Validator.class);
      assertThat(context).getBeanNames(Validator.class).containsOnly("mvcValidator");
      Validator expectedValidator = context.getBean(MvcValidator.class).validator;
      assertThat(context.getBean("mvcValidator")).isSameAs(expectedValidator);
      assertThat(context.getBean(RequestMappingHandlerAdapter.class).getWebBindingInitializer())
              .hasFieldOrPropertyWithValue("validator", expectedValidator);
    });
  }

  @Test
  void validatorWithConfigurerShouldUseInfraValidator() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(MvcValidator.class).run((context) -> {
              assertThat(context).getBeanNames(jakarta.validation.Validator.class)
                      .containsOnly("defaultValidator");
              assertThat(context).getBeanNames(Validator.class).containsOnly("defaultValidator", "mvcValidator");
              Validator expectedValidator = context.getBean(MvcValidator.class).validator;
              assertThat(context.getBean("mvcValidator")).isSameAs(expectedValidator);
              assertThat(context.getBean(RequestMappingHandlerAdapter.class).getWebBindingInitializer())
                      .hasFieldOrPropertyWithValue("validator", expectedValidator);
            });
  }

  @Test
  void validatorWithConfigurerDoesNotExposeJsr303() {
    this.contextRunner.withUserConfiguration(MvcJsr303Validator.class).run((context) -> {
      assertThat(context).doesNotHaveBean(ValidatorFactory.class);
      assertThat(context).doesNotHaveBean(jakarta.validation.Validator.class);
      assertThat(context).getBeanNames(Validator.class).containsOnly("mvcValidator");
      Validator validator = context.getBean("mvcValidator", Validator.class);
      assertThat(validator).isInstanceOf(ValidatorAdapter.class);
      assertThat(((ValidatorAdapter) validator).getTarget())
              .isSameAs(context.getBean(MvcJsr303Validator.class).validator);
    });
  }

  @Test
  void validatorWithConfigurerTakesPrecedence() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(MvcValidator.class).run((context) -> {
              assertThat(context).hasSingleBean(ValidatorFactory.class);
              assertThat(context).hasSingleBean(jakarta.validation.Validator.class);
              assertThat(context).getBeanNames(Validator.class).containsOnly("defaultValidator", "mvcValidator");
              assertThat(context.getBean("mvcValidator")).isSameAs(context.getBean(MvcValidator.class).validator);
              // Primary Infra validator is the auto-configured one as the MVC one
              // has been customized via a WebMvcConfiguration
              assertThat(context.getBean(Validator.class)).isEqualTo(context.getBean("defaultValidator"));
            });
  }

  @Test
  void validatorWithCustomInfraValidatorIgnored() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(CustomInfraValidator.class).run((context) -> {
              assertThat(context).getBeanNames(jakarta.validation.Validator.class)
                      .containsOnly("defaultValidator");
              assertThat(context).getBeanNames(Validator.class).containsOnly("customInfraValidator",
                      "defaultValidator", "mvcValidator");
              Validator validator = context.getBean("mvcValidator", Validator.class);
              assertThat(validator).isInstanceOf(ValidatorAdapter.class);
              Object defaultValidator = context.getBean("defaultValidator");
              assertThat(((ValidatorAdapter) validator).getTarget()).isSameAs(defaultValidator);
              // Primary Infra validator is the one used by MVC behind the scenes
              assertThat(context.getBean(Validator.class)).isEqualTo(defaultValidator);
            });
  }

  @Test
  void validatorWithCustomJsr303ValidatorExposedAsInfraValidator() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(CustomJsr303Validator.class).run((context) -> {
              assertThat(context).doesNotHaveBean(ValidatorFactory.class);
              assertThat(context).getBeanNames(jakarta.validation.Validator.class)
                      .containsOnly("customJsr303Validator");
              assertThat(context).getBeanNames(Validator.class).containsOnly("mvcValidator");
              Validator validator = context.getBean(Validator.class);
              assertThat(validator).isInstanceOf(ValidatorAdapter.class);
              Validator target = ((ValidatorAdapter) validator).getTarget();
              assertThat(target).extracting("targetValidator").isSameAs(context.getBean("customJsr303Validator"));
            });
  }

  @Test
  void httpMessageConverterThatUsesConversionServiceDoesNotCreateACycle() {
    this.contextRunner.withUserConfiguration(CustomHttpMessageConverter.class)
            .run((context) -> assertThat(context).hasNotFailed());
  }

  @Test
  void cachePeriod() {
    this.contextRunner.withPropertyValues("web.resources.cache.period:5").run((context) -> {
      assertResourceHttpRequestHandler((context), (handler) -> {
        assertThat(handler.getCacheSeconds()).isEqualTo(5);
        assertThat(handler.getCacheControl()).isNull();
      });
    });
  }

  @Test
  void cacheControl() {
    this.contextRunner
            .withPropertyValues("web.resources.cache.cachecontrol.max-age:5",
                    "web.resources.cache.cachecontrol.proxy-revalidate:true")
            .run((context) -> assertResourceHttpRequestHandler(context, (handler) -> {
              assertThat(handler.getCacheSeconds()).isEqualTo(-1);
              assertThat(handler.getCacheControl()).usingRecursiveComparison()
                      .isEqualTo(CacheControl.maxAge(5, TimeUnit.SECONDS).proxyRevalidate());
            }));
  }

  @Test
  void defaultContentNegotiation() {
    this.contextRunner.run((context) -> {
      RequestMappingHandlerMapping handlerMapping = context.getBean(RequestMappingHandlerMapping.class);
      ContentNegotiationManager contentNegotiationManager = handlerMapping.getContentNegotiationManager();
      assertThat(contentNegotiationManager.getStrategies()).doesNotHaveAnyElementsOfTypes(
              ContentNegotiationManager.class);
    });
  }

  @Test
  void customPrinterAndParserShouldBeRegisteredAsConverters() {
    this.contextRunner.withUserConfiguration(ParserConfiguration.class, PrinterConfiguration.class)
            .run((context) -> {
              ConversionService service = context.getBean(ConversionService.class);
              assertThat(service.convert(new Example("infra", new Date()), String.class)).isEqualTo("infra");
              assertThat(service.convert("boot", Example.class)).extracting(Example::getName).isEqualTo("boot");
            });
  }

  @Test
  void lastModifiedNotUsedIfDisabled() {
    this.contextRunner.withPropertyValues("web.resources.cache.use-last-modified=false")
            .run((context) -> assertResourceHttpRequestHandler(context,
                    (handler) -> assertThat(handler.isUseLastModified()).isFalse()));
  }

  @Test
  void apiVersionPropertiesAreApplied() {
    this.contextRunner
            .withPropertyValues("web.mvc.apiVersion.use.header=version", "web.mvc.apiVersion.required=true",
                    "web.mvc.apiVersion.supported=123,456", "web.mvc.apiVersion.detect-supported=false")
            .run((context) -> {
              DefaultApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy",
                      DefaultApiVersionStrategy.class);
              assertThatExceptionOfType(MissingApiVersionException.class)
                      .isThrownBy(() -> versionStrategy.validateVersion(null, new MockRequestContext(new HttpMockRequestImpl())));
              assertThatExceptionOfType(InvalidApiVersionException.class).isThrownBy(() -> versionStrategy
                      .validateVersion(versionStrategy.parseVersion("789"), new MockRequestContext(new HttpMockRequestImpl())));
              assertThat(versionStrategy.detectSupportedVersions()).isFalse();
            });
  }

  @Test
  void apiVersionDefaultVersionPropertyIsApplied() {
    this.contextRunner
            .withPropertyValues("web.mvc.apiVersion.use.header=version", "web.mvc.apiVersion.default=1.0.0")
            .run((context) -> {
              DefaultApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy",
                      DefaultApiVersionStrategy.class);
              versionStrategy.addSupportedVersion("1.0.0");
              Comparable<?> version = versionStrategy.parseVersion("1.0.0");
              assertThat(versionStrategy.getDefaultVersion()).isEqualTo(version);
              versionStrategy.validateVersion(version, new MockRequestContext(new HttpMockRequestImpl()));
              versionStrategy.validateVersion(null, new MockRequestContext(new HttpMockRequestImpl()));
            });
  }

  @Test
  void apiVersionUseHeaderPropertyIsApplied() {
    this.contextRunner.withPropertyValues("web.mvc.apiVersion.use.header=hv").run((context) -> {
      ApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy", ApiVersionStrategy.class);
      HttpMockRequestImpl request = new HttpMockRequestImpl();
      request.addHeader("hv", "123");
      assertThat(versionStrategy.resolveVersion(new MockRequestContext(request))).isEqualTo("123");
    });
  }

  @Test
  void apiVersionUseQueryParameterPropertyIsApplied() {
    this.contextRunner.withPropertyValues("web.mvc.apiVersion.use.request-parameter=rpv").run((context) -> {
      ApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy", ApiVersionStrategy.class);
      HttpMockRequestImpl request = new HttpMockRequestImpl();
      request.setParameter("rpv", "123");
      assertThat(versionStrategy.resolveVersion(new MockRequestContext(request))).isEqualTo("123");
    });
  }

  @Test
  void apiVersionUsePathSegmentPropertyIsApplied() {
    this.contextRunner.withPropertyValues("web.mvc.apiVersion.use.path-segment=1").run((context) -> {
      ApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy", ApiVersionStrategy.class);
      HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/test/123");
      assertThat(versionStrategy.resolveVersion(new MockRequestContext(request))).isEqualTo("123");
    });
  }

  @Test
  void apiVersionUseMediaTypeParameterPropertyIsApplied() {
    this.contextRunner.withPropertyValues("web.mvc.apiVersion.use.media-type-parameter[application/json]=mtpv")
            .run((context) -> {
              ApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy", ApiVersionStrategy.class);
              HttpMockRequestImpl request = new HttpMockRequestImpl();
              request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json;mtpv=123");
              assertThat(versionStrategy.resolveVersion(new MockRequestContext(request))).isEqualTo("123");
            });
  }

  @Test
  void apiVersionBeansAreInjected() {
    this.contextRunner.withUserConfiguration(ApiVersionConfiguration.class).run((context) -> {
      DefaultApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy",
              DefaultApiVersionStrategy.class);
      assertThat(versionStrategy).extracting("versionResolvers")
              .asInstanceOf(InstanceOfAssertFactories.LIST)
              .containsExactly(context.getBean(ApiVersionResolver.class));
      assertThat(versionStrategy).extracting("deprecationHandler")
              .isEqualTo(context.getBean(ApiVersionDeprecationHandler.class));
      assertThat(versionStrategy).extracting("versionParser").isEqualTo(context.getBean(ApiVersionParser.class));
    });
  }

  private void assertResourceHttpRequestHandler(AssertableApplicationContext context,
          Consumer<ResourceHttpRequestHandler> handlerConsumer) {
    Map<String, Object> handlerMap = getHandlerMap(context.getBean("resourceHandlerMapping", HandlerMapping.class));
    assertThat(handlerMap).hasSize(2);
    for (Object handler : handlerMap.values()) {
      if (handler instanceof ResourceHttpRequestHandler resourceHandler) {
        handlerConsumer.accept(resourceHandler);
      }
    }
  }

  protected Map<String, List<Resource>> getResourceMappingLocations(ApplicationContext context) {
    Object bean = context.getBean("resourceHandlerMapping");
    if (bean instanceof HandlerMapping handlerMapping) {
      return getMappingLocations(context, handlerMapping);
    }
    assertThat(bean).isNull();
    return Collections.emptyMap();
  }

  protected List<ResourceResolver> getResourceResolvers(ApplicationContext context, String mapping) {
    ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) context
            .getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class).getHandlerMap().get(mapping);
    return resourceHandler.getResourceResolvers();
  }

  protected List<ResourceTransformer> getResourceTransformers(ApplicationContext context, String mapping) {
    SimpleUrlHandlerMapping handler = context.getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class);
    ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler.getHandlerMap().get(mapping);
    return resourceHandler.getResourceTransformers();
  }

  private Map<String, List<Resource>> getMappingLocations(ApplicationContext context, HandlerMapping mapping) {
    Map<String, List<Resource>> mappingLocations = new LinkedHashMap<>();
    getHandlerMap(mapping).forEach((key, value) -> {
      List<String> locationValues = ReflectionTestUtils.getField(value, "locationValues");
      List<Resource> locationResources = ReflectionTestUtils.getField(value, "locationResources");
      List<Resource> resources = new ArrayList<>();
      for (String locationValue : locationValues) {
        resources.add(context.getResource(locationValue));
      }
      resources.addAll(locationResources);
      mappingLocations.put(key, resources);
    });
    return mappingLocations;
  }

  protected Map<String, Object> getHandlerMap(HandlerMapping mapping) {
    if (mapping instanceof SimpleUrlHandlerMapping handlerMapping) {
      return handlerMapping.getHandlerMap();
    }
    return Collections.emptyMap();
  }

  @Configuration(proxyBeanMethods = false)
  static class ViewConfig {

    @Bean
    View jsonView() {
      return new AbstractView() {

        @Override
        protected void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws Exception {
          request.getOutputStream().write("Hello World".getBytes());
        }

      };
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class WebJars implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/foo/");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class AllResources implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/**").addResourceLocations("classpath:/foo/");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

//    @Bean
//    MockWebServerFactory webServerFactory() {
//      return webServerFactory;
//    }

    @Bean
    WebServerFactoryCustomizerBeanPostProcessor webServerCustomizerBeanPostProcessor() {
      return new WebServerFactoryCustomizerBeanPostProcessor();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomViewResolver {

    @Bean
    ViewResolver viewResolver() {
      return new MyViewResolver();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomContentNegotiatingViewResolver {

    @Bean
    ContentNegotiatingViewResolver myViewResolver() {
      return new ContentNegotiatingViewResolver();
    }

  }

  static class MyViewResolver implements ViewResolver {

    @Override
    public View resolveViewName(String viewName, Locale locale) {
      return null;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomConfigurableWebBindingInitializer {

    @Bean
    ConfigurableWebBindingInitializer customConfigurableWebBindingInitializer() {
      return new CustomWebBindingInitializer();
    }

  }

  static class CustomWebBindingInitializer extends ConfigurableWebBindingInitializer {

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomRequestMappingHandlerMapping {

    private int handlerMappings;

    @Bean
    WebMvcRegistrations webMvcRegistrationsHandlerMapping() {
      return new WebMvcRegistrations() {

        @Override
        public RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
          CustomRequestMappingHandlerMapping.this.handlerMappings++;
          return new MyRequestMappingHandlerMapping();
        }

      };
    }

  }

  static class MyRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomRequestMappingHandlerAdapter {

    private int handlerAdapters = 0;

    @Bean
    WebMvcRegistrations webMvcRegistrationsHandlerAdapter() {
      return new WebMvcRegistrations() {

        @Override
        public RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
          CustomRequestMappingHandlerAdapter.this.handlerAdapters++;
          return new MyRequestMappingHandlerAdapter();
        }

      };
    }

  }

  static class MyRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomExceptionHandlerExceptionHandler {

    private int exceptionResolvers = 0;

    @Bean
    WebMvcRegistrations webMvcRegistrationsExceptionResolver() {
      return new WebMvcRegistrations() {

        @Override
        public ExceptionHandlerAnnotationExceptionHandler createAnnotationExceptionHandler() {
          CustomExceptionHandlerExceptionHandler.this.exceptionResolvers++;
          return new MyExceptionHandlerExceptionHandler();
        }

      };
    }

  }

  static class MyExceptionHandlerExceptionHandler extends ExceptionHandlerAnnotationExceptionHandler {

  }

  @Configuration(proxyBeanMethods = false)
  @Import({ CustomRequestMappingHandlerMapping.class, CustomRequestMappingHandlerAdapter.class })
  static class MultipleWebMvcRegistrations {

  }

  @Configuration(proxyBeanMethods = false)
  static class MvcValidator implements WebMvcConfigurer {

    private final Validator validator = mock(Validator.class);

    @Override
    public Validator getValidator() {
      return this.validator;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class MvcJsr303Validator implements WebMvcConfigurer {

    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

    @Override
    public Validator getValidator() {
      return this.validator;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomJsr303Validator {

    @Bean
    jakarta.validation.Validator customJsr303Validator() {
      return mock(jakarta.validation.Validator.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomInfraValidator {

    @Bean
    Validator customInfraValidator() {
      return mock(Validator.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomHttpMessageConverter {

    @Bean
    HttpMessageConverter<?> customHttpMessageConverter() {
      return mock(HttpMessageConverter.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomApplicationTaskExecutorConfig {

    @Bean
    Executor applicationTaskExecutor() {
      return mock(Executor.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomAsyncTaskExecutorConfig {

    @Bean
    AsyncTaskExecutor customTaskExecutor() {
      return mock(AsyncTaskExecutor.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomAsyncTaskExecutorConfigurer implements WebMvcConfigurer {

    private final AsyncTaskExecutor taskExecutor = mock(AsyncTaskExecutor.class);

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
      configurer.setTaskExecutor(this.taskExecutor);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class PrinterConfiguration {

    @Bean
    Printer<Example> examplePrinter() {
      return new ExamplePrinter();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ParserConfiguration {

    @Bean
    Parser<Example> exampleParser() {
      return new ExampleParser();
    }

  }

  static final class Example {

    private final String name;

    private Example(String name, Date date) {
      this.name = name;
    }

    String getName() {
      return this.name;
    }

  }

  static class ExamplePrinter implements Printer<Example> {

    @Override
    public String print(Example example, Locale locale) {
      return example.getName();
    }

  }

  static class ExampleParser implements Parser<Example> {

    @Override
    public Example parse(String source, Locale locale) {
      return new Example(source, new Date());
    }

  }

  @Configuration
  static class CorsConfigurer implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
      registry.addMapping("/**").allowedMethods("GET");
    }

  }

  static class CustomLocaleResolver implements LocaleResolver {
    @Override
    public Locale resolveLocale(RequestContext request) {
      return Locale.ENGLISH;
    }

    @Override
    public void setLocale(RequestContext request, @Nullable Locale locale) {

    }

  }

  static class CustomFlashMapManager extends AbstractRedirectModelManager {

    @Nullable
    @Override
    protected List<RedirectModel> retrieveRedirectModel(RequestContext request) {
      return null;
    }

    @Override
    protected void updateRedirectModel(List<RedirectModel> redirectModels, RequestContext request) {

    }

  }

  @Configuration(proxyBeanMethods = false)
//  @EnableWebMvc
  @ImportAutoConfiguration(WebMvcAutoConfiguration.class)
  static class ResourceHandlersWithChildAndParentContextChildConfiguration {

    @Bean
    WebMvcConfigurer myConfigurer() {
      return new WebMvcConfigurer() {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
          registry.addResourceHandler("/testtesttest");
        }

      };
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ApiVersionConfiguration {

    @Bean
    ApiVersionResolver apiVersionResolver() {
      return (request) -> "latest";
    }

    @Bean
    ApiVersionDeprecationHandler apiVersionDeprecationHandler(ApiVersionParser<?> apiVersionParser) {
      return new StandardApiVersionDeprecationHandler(apiVersionParser);
    }

    @Bean
    ApiVersionParser<String> apiVersionParser() {
      return (version) -> String.valueOf(version);
    }

  }
}
