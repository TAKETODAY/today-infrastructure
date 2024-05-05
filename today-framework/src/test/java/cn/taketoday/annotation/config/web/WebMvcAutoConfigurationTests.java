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

package cn.taketoday.annotation.config.web;

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

import cn.taketoday.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import cn.taketoday.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.annotation.config.task.TaskExecutionAutoConfiguration;
import cn.taketoday.annotation.config.validation.ValidationAutoConfiguration;
import cn.taketoday.annotation.config.validation.ValidatorAdapter;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.context.annotation.config.ImportAutoConfiguration;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.format.Parser;
import cn.taketoday.format.Printer;
import cn.taketoday.format.support.FormattingConversionService;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.ContextConsumer;
import cn.taketoday.web.server.context.AnnotationConfigWebServerApplicationContext;
import cn.taketoday.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import cn.taketoday.http.CacheControl;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.Validator;
import cn.taketoday.validation.beanvalidation.LocalValidatorFactoryBean;
import cn.taketoday.web.AbstractRedirectModelManager;
import cn.taketoday.web.HandlerAdapter;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.RedirectModel;
import cn.taketoday.web.RedirectModelManager;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.bind.support.ConfigurableWebBindingInitializer;
import cn.taketoday.web.config.AsyncSupportConfigurer;
import cn.taketoday.web.config.CorsRegistry;
import cn.taketoday.web.config.ResourceHandlerRegistry;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.async.WebAsyncManagerFactory;
import cn.taketoday.web.handler.AbstractHandlerExceptionHandler;
import cn.taketoday.web.handler.CompositeHandlerExceptionHandler;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.SimpleHandlerExceptionHandler;
import cn.taketoday.web.handler.SimpleUrlHandlerMapping;
import cn.taketoday.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import cn.taketoday.web.handler.method.RequestMappingHandlerAdapter;
import cn.taketoday.web.handler.method.RequestMappingHandlerMapping;
import cn.taketoday.web.i18n.AcceptHeaderLocaleResolver;
import cn.taketoday.web.i18n.FixedLocaleResolver;
import cn.taketoday.web.resource.CachingResourceResolver;
import cn.taketoday.web.resource.CachingResourceTransformer;
import cn.taketoday.web.resource.ContentVersionStrategy;
import cn.taketoday.web.resource.CssLinkResourceTransformer;
import cn.taketoday.web.resource.EncodedResourceResolver;
import cn.taketoday.web.resource.FixedVersionStrategy;
import cn.taketoday.web.resource.PathResourceResolver;
import cn.taketoday.web.resource.ResourceHttpRequestHandler;
import cn.taketoday.web.resource.ResourceResolver;
import cn.taketoday.web.resource.ResourceTransformer;
import cn.taketoday.web.resource.VersionResourceResolver;
import cn.taketoday.web.resource.VersionStrategy;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.view.AbstractView;
import cn.taketoday.web.view.ContentNegotiatingViewResolver;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.ViewResolver;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/23 15:07
 */
public class WebMvcAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner =
          ApplicationContextRunner.forProvider(AnnotationConfigWebServerApplicationContext::new)
                  .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class, RandomPortWebServerConfig.class,
                          HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class))
                  .withUserConfiguration(Config.class);

  @Test
  void handlerAdaptersCreated() {
    this.contextRunner.run((context) -> {
      assertThat(context).getBeans(HandlerAdapter.class).hasSize(2);
      assertThat(context.getBean(ReturnValueHandlerManager.class).getMessageConverters()).isNotEmpty()
              .isEqualTo(context.getBean(HttpMessageConverters.class).getConverters());
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
    this.contextRunner.withPropertyValues("web.mvc.static-path-pattern:/static/**").run((context) -> {
      Map<String, List<Resource>> locations = getResourceMappingLocations(context);
      assertThat(locations.get("/static/**")).hasSize(4);
      assertThat(getResourceResolvers(context, "/static/**")).hasSize(1);
    });
  }

  @Test
  void customWebjarsHandlerMapping() {
    this.contextRunner.withPropertyValues("web.mvc.webjars-path-pattern:/assets/**").run((context) -> {
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
    WebServerFactoryCustomizerBeanPostProcessor ServletWebServerCustomizerBeanPostProcessor() {
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

}
