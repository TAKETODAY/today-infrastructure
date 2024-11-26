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

package infra.web.config;

import java.util.List;

import infra.core.conversion.Converter;
import infra.core.io.Resource;
import infra.format.Formatter;
import infra.format.FormatterRegistry;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.lang.Nullable;
import infra.validation.Validator;
import infra.validation.beanvalidation.OptionalValidatorFactoryBean;
import infra.web.ErrorResponse;
import infra.web.HandlerExceptionHandler;
import infra.web.HandlerMapping;
import infra.web.ReturnValueHandler;
import infra.web.annotation.CrossOrigin;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.bind.resolver.ParameterResolvingStrategies;
import infra.web.bind.resolver.ParameterResolvingStrategy;
import infra.web.cors.CorsConfiguration;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.view.View;

/**
 * Defines callback methods to customize the Java-based configuration for
 * framework enabled via {@code @EnableWebMvc}.
 *
 * <p>{@code @EnableWebMvc}-annotated configuration classes may implement
 * this interface to be called back and given a chance to customize the
 * default configuration.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-05-17 17:46
 */
public interface WebMvcConfigurer {

  /**
   * Configure {@link ParameterResolvingStrategy}
   *
   * @param customizedStrategies {@link ParameterResolvingStrategy} registry
   */
  default void configureParameterResolving(ParameterResolvingStrategies customizedStrategies) {

  }

  /**
   * Configure {@link ParameterResolvingStrategy}
   * <p>
   * user can add {@link ParameterResolvingStrategy} to {@code resolvingStrategies} or
   * use {@link ParameterResolvingRegistry#getCustomizedStrategies()} or
   * use {@link ParameterResolvingRegistry#getDefaultStrategies()}
   * </p>
   *
   * @param customizedStrategies {@link ParameterResolvingStrategy} registry
   * @see ParameterResolvingRegistry#getCustomizedStrategies()
   * @see ParameterResolvingRegistry#getDefaultStrategies()
   * @since 4.0
   */
  default void configureParameterResolving(ParameterResolvingRegistry registry, ParameterResolvingStrategies customizedStrategies) {
    configureParameterResolving(customizedStrategies);
  }

  /**
   * Configure {@link ReturnValueHandler}
   *
   * @param manager {@link ReturnValueHandler} registry
   * @see ReturnValueHandlerManager
   */
  default void modifyReturnValueHandlerManager(ReturnValueHandlerManager manager) {

  }

  /**
   * Configure static {@link Resource}
   *
   * @param registry {@link ResourceHandlerRegistry}
   */
  default void addResourceHandlers(ResourceHandlerRegistry registry) {

  }

  /**
   * Configure {@link HandlerMapping}
   *
   * @param handlerRegistries {@link HandlerMapping}s
   * @since 2.3.7
   */
  default void configureHandlerRegistry(List<HandlerMapping> handlerRegistries) {

  }

  /**
   * Configure {@link HandlerExceptionHandler}
   * <p>
   * Override this method to configure the list of
   * {@link HandlerExceptionHandler HandlerExceptionHandlers} to use.
   * <p>Adding handlers to the list turns off the default resolvers that would otherwise
   * be registered by default.
   *
   * @param handlers a list to add exception handlers to (initially an empty list)
   * @since 3.0
   */
  default void configureExceptionHandlers(List<HandlerExceptionHandler> handlers) {

  }

  /**
   * Override this method to extend or modify the list of
   * {@link HandlerExceptionHandler HandlerExceptionHandlers} after it has been configured.
   * <p>This may be useful for example to allow default handlers to be registered
   * and then insert a custom one through this method.
   *
   * @param handlers the list of configured resolvers to extend.
   * @since 4.0
   */
  default void extendExceptionHandlers(List<HandlerExceptionHandler> handlers) {

  }

  /**
   * Configure content negotiation options.
   *
   * @since 4.0
   */
  default void configureContentNegotiation(ContentNegotiationConfigurer configurer) {

  }

  /**
   * Configure view resolvers to translate String-based view names returned from
   * controllers into concrete {@link View} implementations to perform rendering with.
   *
   * @since 4.0
   */
  default void configureViewResolvers(ViewResolverRegistry registry) {

  }

  /**
   * Help with configuring {@link HandlerMapping} path matching options such as
   * whether to use parsed {@code PathPatterns} or String pattern matching
   * with {@code PathMatcher}, whether to match trailing slashes, and more.
   *
   * @see PathMatchConfigurer
   * @since 4.0
   */
  default void configurePathMatch(PathMatchConfigurer configurer) {

  }

  /**
   * Configure the {@link HttpMessageConverter HttpMessageConverter}s for
   * reading from the request body and for writing to the response body.
   * <p>By default, all built-in converters are configured as long as the
   * corresponding 3rd party libraries such Jackson JSON, JAXB2, and others
   * are present on the classpath.
   * <p>Note that use of this method turns off default converter
   * registration. However, in a Infra application the
   * {@code WebMvcAutoConfiguration} adds any {@code HttpMessageConverter}
   * beans as well as default converters. Hence, in a Infra application use
   * {@link HttpMessageConverters} Alternatively, for any scenario, use
   * {@link #extendMessageConverters(java.util.List)} to modify the configured
   * list of message converters.
   *
   * @param converters initially an empty list of converters
   * @since 4.0
   */
  default void configureMessageConverters(List<HttpMessageConverter<?>> converters) {

  }

  /**
   * Extend or modify the list of converters after it has been, either
   * {@link #configureMessageConverters(List) configured} or initialized with
   * a default list.
   * <p>Note that the order of converter registration is important. Especially
   * in cases where clients accept {@link infra.http.MediaType#ALL}
   * the converters configured earlier will be preferred.
   *
   * @param converters the list of configured converters to be extended
   * @since 4.0
   */
  default void extendMessageConverters(List<HttpMessageConverter<?>> converters) {

  }

  /**
   * Add {@link Converter Converters} and {@link Formatter Formatters} in addition to the ones
   * registered by default.
   *
   * @since 4.0
   */
  default void addFormatters(FormatterRegistry registry) {

  }

  /**
   * Add MVC lifecycle interceptors for pre- and post-processing of
   * controller method invocations and resource handler requests.
   * Interceptors can be registered to apply to all requests or be limited
   * to a subset of URL patterns.
   *
   * @since 4.0
   */
  default void addInterceptors(InterceptorRegistry registry) {

  }

  /**
   * Configure "global" cross-origin request processing. The configured CORS
   * mappings apply to annotated controllers, functional endpoints, and static
   * resources.
   * <p>Annotated controllers can further declare more fine-grained config via
   * {@link CrossOrigin @CrossOrigin}. In such cases "global" CORS configuration
   * declared here is {@link CorsConfiguration#combine(CorsConfiguration) combined}
   * with local CORS configuration defined on a controller method.
   *
   * @see CorsRegistry
   * @see CorsConfiguration#combine(CorsConfiguration)
   * @since 4.0
   */
  default void addCorsMappings(CorsRegistry registry) {

  }

  /**
   * Configure simple automated controllers pre-configured with the response
   * status code and/or a view to render the response body. This is useful in
   * cases where there is no need for custom controller logic -- e.g. render a
   * home page, perform simple site URL redirects, return a 404 status with
   * HTML content, a 204 with no content, and more.
   *
   * @see ViewControllerRegistry
   * @since 4.0
   */
  default void addViewControllers(ViewControllerRegistry registry) {

  }

  /**
   * Add to the list of {@link ErrorResponse.Interceptor}'s to apply when
   * rendering an RFC 7807 {@link infra.http.ProblemDetail}
   * error response.
   *
   * @param interceptors the interceptors to use
   * @since 5.0
   */
  default void addErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {

  }

  /**
   * Configure asynchronous request handling options.
   *
   * @since 4.0
   */
  default void configureAsyncSupport(AsyncSupportConfigurer configurer) {

  }

  /**
   * Provide a custom {@link Validator} instead of the one created by default.
   * The default implementation, assuming JSR-303 is on the classpath, is:
   * {@link OptionalValidatorFactoryBean}.
   * Leave the return value as {@code null} to keep the default.
   *
   * @since 4.0
   */
  @Nullable
  default Validator getValidator() {
    return null;
  }

}
