/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.client;

import java.util.Arrays;
import java.util.function.Supplier;

import cn.taketoday.format.support.FormattingConversionService;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.test.web.servlet.setup.StandaloneMockMvcBuilder;
import cn.taketoday.validation.Validator;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.RedirectModelManager;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.handler.method.RequestMappingHandlerMapping;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.ViewResolver;

/**
 * Simple wrapper around a {@link StandaloneMockMvcBuilder} that implements
 * {@link MockMvcWebTestClient.ControllerSpec}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class StandaloneMockMvcSpec extends AbstractMockMvcServerSpec<MockMvcWebTestClient.ControllerSpec>
        implements MockMvcWebTestClient.ControllerSpec {

  private final StandaloneMockMvcBuilder mockMvcBuilder;

  StandaloneMockMvcSpec(Object... controllers) {
    this.mockMvcBuilder = MockMvcBuilders.standaloneSetup(controllers);
  }

  @Override
  public StandaloneMockMvcSpec controllerAdvice(Object... controllerAdvice) {
    this.mockMvcBuilder.setControllerAdvice(controllerAdvice);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec messageConverters(HttpMessageConverter<?>... messageConverters) {
    this.mockMvcBuilder.setMessageConverters(messageConverters);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec validator(Validator validator) {
    this.mockMvcBuilder.setValidator(validator);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec conversionService(FormattingConversionService conversionService) {
    this.mockMvcBuilder.setConversionService(conversionService);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec interceptors(HandlerInterceptor... interceptors) {
    mappedInterceptors(null, interceptors);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec mappedInterceptors(
          @Nullable String[] pathPatterns, HandlerInterceptor... interceptors) {

    this.mockMvcBuilder.addMappedInterceptors(pathPatterns, interceptors);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec contentNegotiationManager(ContentNegotiationManager manager) {
    this.mockMvcBuilder.setContentNegotiationManager(manager);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec asyncRequestTimeout(long timeout) {
    this.mockMvcBuilder.setAsyncRequestTimeout(timeout);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec customArgumentResolvers(ParameterResolvingStrategy... argumentResolvers) {
    this.mockMvcBuilder.setCustomArgumentResolvers(argumentResolvers);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec customReturnValueHandlers(HandlerMethodReturnValueHandler... handlers) {
    this.mockMvcBuilder.setCustomReturnValueHandlers(handlers);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec handlerExceptionHandlers(HandlerExceptionHandler... exceptionResolvers) {
    this.mockMvcBuilder.setHandlerExceptionHandlers(Arrays.asList(exceptionResolvers));
    return this;
  }

  @Override
  public StandaloneMockMvcSpec viewResolvers(ViewResolver... resolvers) {
    this.mockMvcBuilder.setViewResolvers(resolvers);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec singleView(View view) {
    this.mockMvcBuilder.setSingleView(view);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec localeResolver(LocaleResolver localeResolver) {
    this.mockMvcBuilder.setLocaleResolver(localeResolver);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec flashMapManager(RedirectModelManager flashMapManager) {
    this.mockMvcBuilder.setFlashMapManager(flashMapManager);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec useTrailingSlashPatternMatch(boolean useTrailingSlashPatternMatch) {
    this.mockMvcBuilder.setUseTrailingSlashPatternMatch(useTrailingSlashPatternMatch);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec placeholderValue(String name, Object value) {
    this.mockMvcBuilder.addPlaceholderValue(name, value);
    return this;
  }

  @Override
  public StandaloneMockMvcSpec customHandlerMapping(Supplier<RequestMappingHandlerMapping> factory) {
    this.mockMvcBuilder.setCustomHandlerMapping(factory);
    return this;
  }

  @Override
  public ConfigurableMockMvcBuilder<?> getMockMvcBuilder() {
    return this.mockMvcBuilder;
  }
}
