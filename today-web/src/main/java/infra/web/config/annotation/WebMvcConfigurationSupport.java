/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.config.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.BeanInitializationException;
import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.beans.factory.annotation.Qualifier;
import infra.beans.factory.config.BeanDefinition;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Role;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.support.ApplicationObjectSupport;
import infra.core.Ordered;
import infra.core.conversion.Converter;
import infra.format.Formatter;
import infra.format.FormatterRegistry;
import infra.format.support.DefaultFormattingConversionService;
import infra.format.support.FormattingConversionService;
import infra.http.MediaType;
import infra.http.converter.AllEncompassingFormHttpMessageConverter;
import infra.http.converter.ByteArrayHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.ResourceHttpMessageConverter;
import infra.http.converter.ResourceRegionHttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import infra.http.converter.feed.AtomFeedHttpMessageConverter;
import infra.http.converter.feed.RssChannelHttpMessageConverter;
import infra.http.converter.json.GsonHttpMessageConverter;
import infra.http.converter.json.Jackson2ObjectMapperBuilder;
import infra.http.converter.json.JsonbHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import infra.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import infra.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import infra.http.converter.yaml.MappingJackson2YamlHttpMessageConverter;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.session.SessionManager;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.validation.Errors;
import infra.validation.MessageCodesResolver;
import infra.validation.Validator;
import infra.validation.beanvalidation.OptionalValidatorFactoryBean;
import infra.web.ErrorResponse;
import infra.web.HandlerAdapter;
import infra.web.HandlerExceptionHandler;
import infra.web.HandlerMapping;
import infra.web.LocaleResolver;
import infra.web.NotFoundHandler;
import infra.web.RedirectModelManager;
import infra.web.ReturnValueHandler;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.ContentNegotiationManager;
import infra.web.async.WebAsyncManagerFactory;
import infra.web.bind.WebDataBinder;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.bind.resolver.ParameterResolvingStrategy;
import infra.web.bind.support.ConfigurableWebBindingInitializer;
import infra.web.bind.support.WebBindingInitializer;
import infra.web.cors.CorsConfiguration;
import infra.web.handler.AbstractHandlerMapping;
import infra.web.handler.BeanNameUrlHandlerMapping;
import infra.web.handler.CompositeHandlerExceptionHandler;
import infra.web.handler.ResponseStatusExceptionHandler;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.handler.SimpleHandlerExceptionHandler;
import infra.web.handler.SimpleUrlHandlerMapping;
import infra.web.handler.function.support.HandlerFunctionAdapter;
import infra.web.handler.function.support.RouterFunctionMapping;
import infra.web.handler.method.ControllerAdviceBean;
import infra.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import infra.web.handler.method.JsonViewRequestBodyAdvice;
import infra.web.handler.method.JsonViewResponseBodyAdvice;
import infra.web.handler.method.MvcUriComponentsBuilder;
import infra.web.handler.method.RequestBodyAdvice;
import infra.web.handler.method.RequestMappingHandlerAdapter;
import infra.web.handler.method.RequestMappingHandlerMapping;
import infra.web.handler.method.ResponseBodyAdvice;
import infra.web.handler.method.support.CompositeUriComponentsContributor;
import infra.web.i18n.AcceptHeaderLocaleResolver;
import infra.web.util.pattern.PathPatternParser;
import infra.web.view.UrlBasedViewResolver;
import infra.web.view.ViewResolver;
import infra.web.view.ViewResolverComposite;
import infra.web.view.ViewReturnValueHandler;

/**
 * This is the main class providing the configuration behind the MVC Java config.
 * It is typically imported by adding {@link EnableWebMvc @EnableWebMvc} to an
 * application {@link Configuration @Configuration} class. An alternative more
 * advanced option is to extend directly from this class and override methods as
 * necessary, remembering to add {@link Configuration @Configuration} to the
 * subclass and {@link Bean @Bean} to overridden {@link Bean @Bean} methods.
 * For more details see the javadoc of {@link EnableWebMvc @EnableWebMvc}.
 *
 * <p>This class registers the following {@link HandlerMapping HandlerMappings}:</p>
 * <ul>
 * <li>{@link RequestMappingHandlerMapping}
 * ordered at 0 for mapping requests to annotated controller methods.
 * <li>{@link HandlerMapping}
 * ordered at 1 to map URL paths directly to view names.
 * <li>{@link BeanNameUrlHandlerMapping}
 * ordered at 2 to map URL paths to controller bean names.
 * <li>{@link RouterFunctionMapping}
 * ordered at 3 to map {@linkplain infra.web.handler.function.RouterFunction router functions}.
 * <li>{@link HandlerMapping}
 * ordered at {@code Integer.MAX_VALUE-1} to serve static resource requests.
 * <li>{@link HandlerMapping}
 * ordered at {@code Integer.MAX_VALUE} to forward requests to the default handler.
 * </ul>
 *
 * <p>Registers these {@link HandlerAdapter HandlerAdapters}:
 * <ul>
 * <li>{@link RequestMappingHandlerAdapter}
 * for processing requests with annotated controller methods.
 * <li>{@link HandlerFunctionAdapter}
 * for processing requests with {@linkplain
 * infra.web.handler.function.RouterFunction router functions}.
 * </ul>
 *
 * <p>
 *   HttpRequestHandler is default handler to handle HTTP request
 *
 * <p>Registers a {@link CompositeHandlerExceptionHandler} with this chain of
 * exception handlers:
 * <ul>
 * <li>{@link ExceptionHandlerAnnotationExceptionHandler} for handling exceptions through
 * {@link infra.web.annotation.ExceptionHandler} methods.
 * <li>{@link ResponseStatusExceptionHandler} for exceptions annotated with
 * {@link infra.web.annotation.ResponseStatus}.
 * <li>{@link SimpleHandlerExceptionHandler} for resolving known Infra
 * exception types
 * </ul>
 *
 * Note that those beans can be configured with a {@link PathMatchConfigurer}.
 *
 * <p>Both the {@link RequestMappingHandlerAdapter} and the
 * {@link ExceptionHandlerAnnotationExceptionHandler} are configured with default
 * instances of the following by default:
 * <ul>
 * <li>a {@link ContentNegotiationManager}
 * <li>a {@link DefaultFormattingConversionService}
 * <li>an {@link OptionalValidatorFactoryBean}
 * if a JSR-303 implementation is available on the classpath
 * <li>a range of {@link HttpMessageConverter HttpMessageConverters} depending on the third-party
 * libraries available on the classpath.
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebMvcConfigurer
 * @since 4.0 2022/1/27 23:43
 */
@DisableAllDependencyInjection
public class WebMvcConfigurationSupport extends ApplicationObjectSupport {

  private static final boolean gsonPresent = isPresent("com.google.gson.Gson");
  private static final boolean jsonbPresent = isPresent("jakarta.json.bind.Jsonb");
  private static final boolean jaxb2Present = isPresent("jakarta.xml.bind.Binder");
  private static final boolean romePresent = isPresent("com.rometools.rome.feed.WireFeed");
  private static final boolean jackson2Present = isPresent("com.fasterxml.jackson.databind.ObjectMapper")
          && isPresent("com.fasterxml.jackson.core.JsonGenerator");
  private static final boolean jackson2XmlPresent = isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper");
  private static final boolean jackson2CborPresent = isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory");
  private static final boolean jackson2YamlPresent = isPresent("com.fasterxml.jackson.dataformat.yaml.YAMLFactory");
  private static final boolean jackson2SmilePresent = isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory");

  private final List<Object> requestResponseBodyAdvice = new ArrayList<>();

  @Nullable
  private ContentNegotiationManager contentNegotiationManager;

  @Nullable
  private PathMatchConfigurer pathMatchConfigurer;

  @Nullable
  private List<HttpMessageConverter<?>> messageConverters;

  @Nullable
  private Map<String, CorsConfiguration> corsConfigurations;

  @Nullable
  private List<Object> interceptors;

  @Nullable
  private AsyncSupportConfigurer asyncSupportConfigurer;

  @Nullable
  private List<ErrorResponse.Interceptor> errorResponseInterceptors;

  @Nullable
  private ApiVersionStrategy apiVersionStrategy;

  @Override
  protected void initApplicationContext() {
    initControllerAdviceCache();
  }

  private void initControllerAdviceCache() {
    List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(
            obtainApplicationContext(), RequestBodyAdvice.class, ResponseBodyAdvice.class);

    if (!adviceBeans.isEmpty()) {
      requestResponseBodyAdvice.addAll(0, adviceBeans);
    }

    if (jackson2Present) {
      requestResponseBodyAdvice.add(new JsonViewRequestBodyAdvice());
      requestResponseBodyAdvice.add(new JsonViewResponseBodyAdvice());
    }

  }

  //---------------------------------------------------------------------
  // HttpMessageConverter
  //---------------------------------------------------------------------

  /**
   * Provides access to the shared {@link HttpMessageConverter HttpMessageConverters}
   * used by the {@link ParameterResolvingStrategy} and the
   * {@link ReturnValueHandlerManager}.
   * <p>This method cannot be overridden; use {@link #configureMessageConverters} instead.
   * Also see {@link #addDefaultHttpMessageConverters} for adding default message converters.
   */
  public final List<HttpMessageConverter<?>> getMessageConverters() {
    if (this.messageConverters == null) {
      this.messageConverters = new ArrayList<>();
      configureMessageConverters(this.messageConverters);
      if (this.messageConverters.isEmpty()) {
        addDefaultHttpMessageConverters(this.messageConverters);
      }
      extendMessageConverters(this.messageConverters);
    }
    return this.messageConverters;
  }

  /**
   * Override this method to add custom {@link HttpMessageConverter HttpMessageConverters}
   * to use with the {@link ParameterResolvingStrategy} and the
   * {@link ReturnValueHandlerManager}.
   * <p>Adding converters to the list turns off the default converters that would
   * otherwise be registered by default. Also see {@link #addDefaultHttpMessageConverters}
   * for adding default message converters.
   *
   * @param converters a list to add message converters to (initially an empty list)
   * @since 4.0
   */
  protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
  }

  /**
   * Override this method to extend or modify the list of converters after it has
   * been configured. This may be useful for example to allow default converters
   * to be registered and then insert a custom converter through this method.
   *
   * @param converters the list of configured converters to extend
   * @since 4.0
   */
  protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
  }

  /**
   * Adds a set of default HttpMessageConverter instances to the given list.
   * Subclasses can call this method from {@link #configureMessageConverters}.
   *
   * @param messageConverters the list to add the default message converters to
   * @since 4.0
   */
  protected final void addDefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    messageConverters.add(new ByteArrayHttpMessageConverter());
    messageConverters.add(new StringHttpMessageConverter());
    messageConverters.add(new ResourceHttpMessageConverter());
    messageConverters.add(new ResourceRegionHttpMessageConverter());

    messageConverters.add(new AllEncompassingFormHttpMessageConverter());

    if (romePresent) {
      messageConverters.add(new AtomFeedHttpMessageConverter());
      messageConverters.add(new RssChannelHttpMessageConverter());
    }
    ApplicationContext applicationContext = getApplicationContext();

    if (jackson2XmlPresent) {
      Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.xml();
      if (applicationContext != null) {
        builder.applicationContext(applicationContext);
      }
      messageConverters.add(new MappingJackson2XmlHttpMessageConverter(builder.build()));
    }
    else if (jaxb2Present) {
      messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
    }

    if (jackson2Present) {
      Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
      if (applicationContext != null) {
        builder.applicationContext(applicationContext);
      }
      messageConverters.add(new MappingJackson2HttpMessageConverter(builder.build()));
    }
    else if (gsonPresent) {
      messageConverters.add(new GsonHttpMessageConverter());
    }
    else if (jsonbPresent) {
      messageConverters.add(new JsonbHttpMessageConverter());
    }

    if (jackson2SmilePresent) {
      Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.smile();
      if (applicationContext != null) {
        builder.applicationContext(applicationContext);
      }
      messageConverters.add(new MappingJackson2SmileHttpMessageConverter(builder.build()));
    }
    if (jackson2CborPresent) {
      Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.cbor();
      if (applicationContext != null) {
        builder.applicationContext(applicationContext);
      }
      messageConverters.add(new MappingJackson2CborHttpMessageConverter(builder.build()));
    }

    if (jackson2YamlPresent) {
      Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.yaml();
      if (this.applicationContext != null) {
        builder.applicationContext(this.applicationContext);
      }
      messageConverters.add(new MappingJackson2YamlHttpMessageConverter(builder.build()));
    }
  }

  // Async

  /**
   * WebAsyncManager Factory
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public WebAsyncManagerFactory webAsyncManagerFactory() {
    WebAsyncManagerFactory factory = new WebAsyncManagerFactory();
    AsyncSupportConfigurer configurer = getAsyncSupportConfigurer();
    if (configurer.taskExecutor != null) {
      factory.setTaskExecutor(configurer.taskExecutor);
    }

    factory.setAsyncRequestTimeout(configurer.timeout);
    factory.setCallableInterceptors(configurer.callableInterceptors);
    factory.setDeferredResultInterceptors(configurer.deferredResultInterceptors);

    return factory;
  }

  //---------------------------------------------------------------------
  // ContentNegotiation
  //---------------------------------------------------------------------

  /**
   * Return a {@link ContentNegotiationManager} instance to use to determine
   * requested {@linkplain MediaType media types} in a given request.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(ContentNegotiationManager.class)
  public ContentNegotiationManager mvcContentNegotiationManager() {
    if (contentNegotiationManager == null) {
      ContentNegotiationConfigurer configurer = createNegotiationConfigurer();
      configurer.mediaTypes(getDefaultMediaTypes());
      configureContentNegotiation(configurer);
      this.contentNegotiationManager = configurer.buildContentNegotiationManager();
    }
    return contentNegotiationManager;
  }

  private ContentNegotiationConfigurer createNegotiationConfigurer() {
    return new ContentNegotiationConfigurer();
  }

  protected Map<String, MediaType> getDefaultMediaTypes() {
    Map<String, MediaType> map = new HashMap<>(4);
    if (romePresent) {
      map.put("atom", MediaType.APPLICATION_ATOM_XML);
      map.put("rss", MediaType.APPLICATION_RSS_XML);
    }
    if (jaxb2Present || jackson2XmlPresent) {
      map.put("xml", MediaType.APPLICATION_XML);
    }
    if (jackson2Present || gsonPresent || jsonbPresent) {
      map.put("json", MediaType.APPLICATION_JSON);
    }
    if (jackson2SmilePresent) {
      map.put("smile", MediaType.valueOf("application/x-jackson-smile"));
    }
    if (jackson2CborPresent) {
      map.put("cbor", MediaType.APPLICATION_CBOR);
    }
    if (jackson2YamlPresent) {
      map.put("yaml", MediaType.APPLICATION_YAML);
    }
    return map;
  }

  /**
   * Override this method to configure content negotiation.
   */
  protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
  }

  //---------------------------------------------------------------------
  // PathMatchConfigurer
  //---------------------------------------------------------------------

  /**
   * Callback for building the {@link PathMatchConfigurer}.
   * Delegates to {@link #configurePathMatch}.
   */
  protected PathMatchConfigurer getPathMatchConfigurer() {
    if (this.pathMatchConfigurer == null) {
      this.pathMatchConfigurer = new PathMatchConfigurer();
      configurePathMatch(this.pathMatchConfigurer);
    }
    return this.pathMatchConfigurer;
  }

  /**
   * Override this method to configure path matching options.
   *
   * @see PathMatchConfigurer
   */
  protected void configurePathMatch(PathMatchConfigurer configurer) {
  }

  /**
   * Register a {@link ViewResolverComposite} that contains a chain of view resolvers
   * to use for view resolution.
   * By default this resolver is ordered at 0 unless content negotiation view
   * resolution is used in which case the order is raised to
   * {@link Ordered#HIGHEST_PRECEDENCE Ordered.HIGHEST_PRECEDENCE}.
   * <p>If no other resolvers are configured,
   * {@link ViewResolverComposite#resolveViewName(String, Locale)} returns null in order
   * to allow other potential {@link ViewResolver} beans to resolve views.
   */
  @Nullable
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public ViewResolver mvcViewResolver(@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {
    var registry = new ViewResolverRegistry(contentNegotiationManager, applicationContext);
    configureViewResolvers(registry);
    var viewResolvers = new ArrayList<>(registry.getViewResolvers());
    if (applicationContext != null) {
      Set<String> names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
              applicationContext, ViewResolver.class, true, false);
      if (names.size() == 1) {
        // add default
        viewResolvers.add(new UrlBasedViewResolver());
        configureDefaultViewResolvers(viewResolvers);
      }
    }

    if (viewResolvers.isEmpty()) {
      return null;
    }

    ViewResolverComposite composite = new ViewResolverComposite();

    composite.setOrder(registry.getOrder());
    composite.setViewResolvers(viewResolvers);
    return composite;
  }

  /**
   * Override this method to configure view resolution.
   *
   * @see ViewResolverRegistry
   */
  protected void configureDefaultViewResolvers(List<ViewResolver> viewResolvers) {
  }

  /**
   * Override this method to configure view resolution.
   *
   * @see ViewResolverRegistry
   */
  protected void configureViewResolvers(ViewResolverRegistry registry) {
  }

  @Component
  @ConditionalOnMissingBean(name = LocaleResolver.BEAN_NAME)
  public LocaleResolver webLocaleResolver() {
    return new AcceptHeaderLocaleResolver();
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(ViewReturnValueHandler.class)
  public ViewReturnValueHandler viewReturnValueHandler(
          @Qualifier(LocaleResolver.BEAN_NAME) LocaleResolver localeResolver, List<ViewResolver> viewResolvers) {
    ViewResolver viewResolver;
    if (viewResolvers.size() == 1) {
      viewResolver = CollectionUtils.firstElement(viewResolvers);
      Assert.state(viewResolver != null, "No ViewResolver");
    }
    else {
      ViewResolverComposite composite = new ViewResolverComposite();
      composite.setViewResolvers(viewResolvers);
      viewResolver = composite;
    }

    return new ViewReturnValueHandler(viewResolver, localeResolver);
  }

  /**
   * default {@link ReturnValueHandler} registry
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(ReturnValueHandlerManager.class)
  public ReturnValueHandlerManager returnValueHandlerManager(
          ViewReturnValueHandler viewHandler, @Nullable RedirectModelManager redirectModelManager,
          @Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {

    var manager = new ReturnValueHandlerManager(getMessageConverters());

    manager.setApplicationContext(applicationContext);
    manager.setRedirectModelManager(redirectModelManager);

    AsyncSupportConfigurer configurer = getAsyncSupportConfigurer();
    if (configurer.taskExecutor != null) {
      manager.setTaskExecutor(configurer.taskExecutor);
    }

    manager.setContentNegotiationManager(contentNegotiationManager);
    manager.setViewReturnValueHandler(viewHandler);
    manager.addRequestResponseBodyAdvice(requestResponseBodyAdvice);
    manager.setErrorResponseInterceptors(getErrorResponseInterceptors());
    manager.registerDefaultHandlers();

    modifyReturnValueHandlerManager(manager);

    return manager;
  }

  protected void modifyReturnValueHandlerManager(ReturnValueHandlerManager manager) {
  }

  /**
   * default {@link ParameterResolvingStrategy} registry
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public ParameterResolvingRegistry parameterResolvingRegistry(
          @Nullable ParameterResolvingStrategy[] strategies, @Nullable RedirectModelManager redirectModelManager,
          @Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {

    var registry = new ParameterResolvingRegistry(getMessageConverters());
    registry.setApplicationContext(getApplicationContext());
    registry.setRedirectModelManager(redirectModelManager);
    registry.addRequestResponseBodyAdvice(requestResponseBodyAdvice);
    registry.setContentNegotiationManager(contentNegotiationManager);

    registry.registerDefaultStrategies();
    registry.addCustomizedStrategies(strategies);

    modifyParameterResolvingRegistry(registry);
    return registry;
  }

  protected void modifyParameterResolvingRegistry(ParameterResolvingRegistry registry) {

  }

  // HandlerExceptionHandler

  /**
   * Returns a {@link CompositeHandlerExceptionHandler} containing a list of exception
   * resolvers obtained either through {@link #configureExceptionHandlers} or
   * through {@link #addDefaultHandlerExceptionHandlers}.
   * <p><strong>Note:</strong> This method cannot be made final due to CGLIB constraints.
   * Rather than overriding it, consider overriding {@link #configureExceptionHandlers}
   * which allows for providing a list of resolvers.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public HandlerExceptionHandler handlerExceptionHandler(ParameterResolvingRegistry registry,
          @Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {
    var handlers = new ArrayList<HandlerExceptionHandler>();
    configureExceptionHandlers(handlers);
    if (handlers.isEmpty()) {
      addDefaultHandlerExceptionHandlers(handlers, registry, contentNegotiationManager);
    }
    extendExceptionHandlers(handlers);
    if (handlers.size() == 1) {
      return handlers.get(0);
    }
    CompositeHandlerExceptionHandler composite = new CompositeHandlerExceptionHandler(handlers);
    composite.setOrder(0);
    return composite;
  }

  /**
   * Override this method to configure the list of
   * {@link HandlerExceptionHandler HandlerExceptionHandlers} to use.
   * <p>Adding resolvers to the list turns off the default resolvers that would otherwise
   * be registered by default. Also see {@link #addDefaultHandlerExceptionHandlers}
   * that can be used to add the default exception resolvers.
   *
   * @param handlers a list to add exception handlers to (initially an empty list)
   */
  protected void configureExceptionHandlers(List<HandlerExceptionHandler> handlers) {
  }

  /**
   * Override this method to extend or modify the list of
   * {@link HandlerExceptionHandler HandlerExceptionHandlers} after it has been configured.
   * <p>This may be useful for example to allow default resolvers to be registered
   * and then insert a custom one through this method.
   *
   * @param handlers the list of configured resolvers to extend.
   */
  protected void extendExceptionHandlers(List<HandlerExceptionHandler> handlers) {
  }

  /**
   * A method available to subclasses for adding default
   * {@link HandlerExceptionHandler HandlerExceptionHandlers}.
   * <p>Adds the following exception resolvers:
   * <ul>
   * <li>{@link ExceptionHandlerAnnotationExceptionHandler} for handling exceptions through
   * {@link infra.web.annotation.ExceptionHandler} methods.
   * <li>{@link ResponseStatusExceptionHandler} for exceptions annotated with
   * {@link infra.web.annotation.ResponseStatus}.
   * <li>{@link SimpleHandlerExceptionHandler} for resolving known Framework exception types
   * </ul>
   */
  protected final void addDefaultHandlerExceptionHandlers(List<HandlerExceptionHandler> handlers,
          ParameterResolvingRegistry registry, ContentNegotiationManager contentNegotiationManager) {
    var handler = createAnnotationExceptionHandler();

    if (this.applicationContext != null) {
      handler.setApplicationContext(this.applicationContext);
    }

    handler.setContentNegotiationManager(contentNegotiationManager);
    handler.setParameterResolvingRegistry(registry);
    handler.afterPropertiesSet();
    handlers.add(handler);

    var statusExceptionHandler = new ResponseStatusExceptionHandler();
    statusExceptionHandler.setMessageSource(this.applicationContext);
    handlers.add(statusExceptionHandler);

    handlers.add(new SimpleHandlerExceptionHandler());
  }

  /**
   * Protected method for plugging in a custom subclass of
   * {@link ExceptionHandlerAnnotationExceptionHandler}.
   */
  protected ExceptionHandlerAnnotationExceptionHandler createAnnotationExceptionHandler() {
    return new ExceptionHandlerAnnotationExceptionHandler();
  }

  // HandlerRegistry

  /**
   * core {@link HandlerMapping} to register handler
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(RequestMappingHandlerMapping.class)
  public RequestMappingHandlerMapping requestMappingHandlerMapping(
          @Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
          @Qualifier("mvcApiVersionStrategy") @Nullable ApiVersionStrategy apiVersionStrategy,
          ParameterResolvingRegistry parameterResolvingRegistry) {

    var mapping = createRequestMappingHandlerMapping();
    mapping.setOrder(0);
    mapping.setContentNegotiationManager(contentNegotiationManager);
    mapping.setResolvingRegistry(parameterResolvingRegistry);
    mapping.setApiVersionStrategy(apiVersionStrategy);

    initHandlerMapping(mapping);

    PathMatchConfigurer pathConfig = getPathMatchConfigurer();
    if (pathConfig.getPathPrefixes() != null) {
      mapping.setPathPrefixes(pathConfig.getPathPrefixes());
    }
    return mapping;
  }

  /**
   * Protected method for plugging in a custom subclass of
   * {@link RequestMappingHandlerMapping}.
   */
  protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
    return new RequestMappingHandlerMapping();
  }

  /**
   * Return a {@link BeanNameUrlHandlerMapping} ordered at 2 to map URL
   * paths to controller bean names.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public BeanNameUrlHandlerMapping beanNameHandlerMapping() {
    BeanNameUrlHandlerMapping mapping = new BeanNameUrlHandlerMapping();
    mapping.setOrder(2);

    initHandlerMapping(mapping);
    return mapping;
  }

  /**
   * Return a {@link RouterFunctionMapping} ordered at 3 to map
   * {@linkplain infra.web.handler.function.RouterFunction router functions}.
   * Consider overriding one of these other more fine-grained methods:
   * <ul>
   * <li>{@link #addInterceptors} for adding handler interceptors.
   * <li>{@link #addCorsMappings} to configure cross origin requests processing.
   * <li>{@link #configureMessageConverters} for adding custom message converters.
   * <li>{@link #configurePathMatch(PathMatchConfigurer)} for customizing the {@link PathPatternParser}.
   * </ul>
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public RouterFunctionMapping routerFunctionMapping(@Qualifier("mvcApiVersionStrategy") @Nullable ApiVersionStrategy versionStrategy) {

    RouterFunctionMapping mapping = new RouterFunctionMapping();
    mapping.setOrder(3);
    mapping.setMessageConverters(getMessageConverters());
    mapping.setApiVersionStrategy(versionStrategy);

    initHandlerMapping(mapping);
    return mapping;
  }

  public void initHandlerMapping(@Nullable AbstractHandlerMapping mapping) {
    if (mapping != null) {
      mapping.setInterceptors(getInterceptors());
      mapping.setCorsConfigurations(getCorsConfigurations());

      PathMatchConfigurer configurer = getPathMatchConfigurer();

      Boolean useTrailingSlashMatch = configurer.isUseTrailingSlashMatch();
      if (useTrailingSlashMatch != null) {
        mapping.setUseTrailingSlashMatch(useTrailingSlashMatch);
      }

      Boolean useCaseSensitiveMatch = configurer.isUseCaseSensitiveMatch();
      if (useCaseSensitiveMatch != null) {
        mapping.setUseCaseSensitiveMatch(useCaseSensitiveMatch);
      }
    }
  }

  /**
   * Return a handler mapping ordered at Integer.MAX_VALUE-1 with mapped
   * resource handlers. To configure resource handling, override
   * {@link #addResourceHandlers}.
   */
  @Nullable
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public HandlerMapping resourceHandlerMapping(@Nullable NotFoundHandler notFoundHandler,
          @Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {
    var context = obtainApplicationContext();
    var registry = new ResourceHandlerRegistry(context, contentNegotiationManager);

    registry.setNotFoundHandler(notFoundHandler);
    addResourceHandlers(registry);

    SimpleUrlHandlerMapping handlerMapping = registry.getHandlerMapping();
    initHandlerMapping(handlerMapping);
    return handlerMapping;
  }

  /**
   * Return a handler mapping ordered at 1 to map URL paths directly to
   * view names. To configure view controllers, override
   * {@link #addViewControllers}.
   */
  @Component
  @Nullable
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public HandlerMapping viewControllerHandlerMapping() {
    var context = obtainApplicationContext();
    ViewControllerRegistry registry = new ViewControllerRegistry(context);
    addViewControllers(registry);

    AbstractHandlerMapping mapping = registry.buildHandlerMapping();
    initHandlerMapping(mapping);
    return mapping;
  }

  /**
   * Override this method to add view controllers.
   *
   * @see ViewControllerRegistry
   */
  protected void addViewControllers(ViewControllerRegistry registry) {
  }

  /**
   * Override this method to add resource handlers for serving static resources.
   *
   * @see ResourceHandlerRegistry
   */
  protected void addResourceHandlers(ResourceHandlerRegistry registry) {
  }

  /**
   * Return the registered {@link CorsConfiguration} objects,
   * keyed by path pattern.
   */
  protected final Map<String, CorsConfiguration> getCorsConfigurations() {
    if (this.corsConfigurations == null) {
      CorsRegistry registry = new CorsRegistry();
      addCorsMappings(registry);
      this.corsConfigurations = registry.getCorsConfigurations();
    }
    return this.corsConfigurations;
  }

  /**
   * Override this method to configure cross origin requests processing.
   *
   * @see CorsRegistry
   */
  protected void addCorsMappings(CorsRegistry registry) {
  }

  /**
   * Provide access to the shared handler interceptors used to configure
   * {@link HandlerMapping} instances with.
   * <p>This method cannot be overridden; use {@link #addInterceptors} instead.
   */
  protected final Object[] getInterceptors() {
    if (this.interceptors == null) {
      InterceptorRegistry registry = new InterceptorRegistry();
      addInterceptors(registry);
      this.interceptors = registry.getInterceptors();
    }
    return this.interceptors.toArray();
  }

  /**
   * Override this method to add Framework MVC interceptors for
   * pre- and post-processing of controller invocation.
   *
   * @see InterceptorRegistry
   */
  protected void addInterceptors(InterceptorRegistry registry) {
  }

  /**
   * Return a {@link FormattingConversionService} for use with annotated controllers.
   * <p>See {@link #addFormatters} as an alternative to overriding this method.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public FormattingConversionService mvcConversionService() {
    var conversionService = new DefaultFormattingConversionService();
    addFormatters(conversionService);
    return conversionService;
  }

  /**
   * Override this method to add custom {@link Converter} and/or {@link Formatter}
   * delegates to the common {@link FormattingConversionService}.
   *
   * @see #mvcConversionService()
   */
  protected void addFormatters(FormatterRegistry registry) {
  }

  /**
   * Returns a {@link RequestMappingHandlerAdapter} for processing requests
   * through annotated controller methods. Consider overriding one of these
   * other more fine-grained methods:
   * <ul>
   * <li>{@link #modifyParameterResolvingRegistry(ParameterResolvingRegistry)} for adding custom argument resolvers.
   * <li>{@link #modifyReturnValueHandlerManager(ReturnValueHandlerManager)} for adding custom return value handlers.
   * <li>{@link #configureMessageConverters} for adding custom message converters.
   * </ul>
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public RequestMappingHandlerAdapter requestMappingHandlerAdapter(
          @Nullable SessionManager sessionManager,
          @Nullable RedirectModelManager redirectModelManager,
          @Nullable WebBindingInitializer webBindingInitializer,
          ParameterResolvingRegistry parameterResolvingRegistry,
          @Qualifier("mvcValidator") Validator validator,
          @Qualifier("mvcConversionService") FormattingConversionService conversionService) {

    var adapter = createRequestMappingHandlerAdapter();

    adapter.setSessionManager(sessionManager);
    adapter.setRedirectModelManager(redirectModelManager);
    adapter.setResolvingRegistry(parameterResolvingRegistry);
    if (webBindingInitializer == null) {
      webBindingInitializer = createWebBindingInitializer(conversionService, validator);
    }
    adapter.setWebBindingInitializer(webBindingInitializer);

    return adapter;
  }

  /**
   * Protected method for plugging in a custom subclass of
   * {@link RequestMappingHandlerAdapter}.
   */
  protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
    return new RequestMappingHandlerAdapter();
  }

  /**
   * Returns a {@link HandlerFunctionAdapter} for processing requests through
   * {@linkplain infra.web.handler.function.HandlerFunction handler functions}.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public HandlerFunctionAdapter handlerFunctionAdapter() {
    return new HandlerFunctionAdapter();
  }

  /**
   * Return the {@link WebBindingInitializer} to use for
   * initializing all {@link WebDataBinder} instances.
   */
  protected WebBindingInitializer createWebBindingInitializer(FormattingConversionService mvcConversionService, Validator mvcValidator) {
    var initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(mvcConversionService);
    initializer.setValidator(mvcValidator);
    MessageCodesResolver messageCodesResolver = getMessageCodesResolver();
    if (messageCodesResolver != null) {
      initializer.setMessageCodesResolver(messageCodesResolver);
    }
    return initializer;
  }

  /**
   * Return a global {@link Validator} instance for example for validating
   * {@code @ModelAttribute} and {@code @RequestBody} method arguments.
   * Delegates to {@link #getValidator()} first and if that returns {@code null}
   * checks the classpath for the presence of a JSR-303 implementations
   * before creating a {@code OptionalValidatorFactoryBean}.If a JSR-303
   * implementation is not available, a no-op {@link Validator} is returned.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public Validator mvcValidator() {
    Validator validator = getValidator();
    if (validator == null) {
      if (ClassUtils.isPresent("jakarta.validation.Validator", getClass().getClassLoader())) {
        try {
          validator = new OptionalValidatorFactoryBean();
        }
        catch (Throwable ex) {
          throw new BeanInitializationException("Failed to create default validator", ex);
        }
      }
      else {
        validator = new NoOpValidator();
      }
    }
    return validator;
  }

  /**
   * Provide a custom {@link MessageCodesResolver} for building message codes
   * from data binding and validation error codes. Leave the return value as
   * {@code null} to keep the default.
   */
  @Nullable
  protected MessageCodesResolver getMessageCodesResolver() {
    return null;
  }

  /**
   * Override this method to provide a custom {@link Validator}.
   */
  @Nullable
  protected Validator getValidator() {
    return null;
  }

  /**
   * Callback for building the {@link AsyncSupportConfigurer}.
   * Delegates to {@link #configureAsyncSupport(AsyncSupportConfigurer)}.
   */
  protected AsyncSupportConfigurer getAsyncSupportConfigurer() {
    if (asyncSupportConfigurer == null) {
      this.asyncSupportConfigurer = new AsyncSupportConfigurer();
      configureAsyncSupport(asyncSupportConfigurer);
    }
    return asyncSupportConfigurer;
  }

  /**
   * Override this method to configure asynchronous request processing options.
   *
   * @see AsyncSupportConfigurer
   */
  protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
  }

  /**
   * Return an instance of {@link CompositeUriComponentsContributor} for use with
   * {@link MvcUriComponentsBuilder}.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public CompositeUriComponentsContributor mvcUriComponentsContributor(ParameterResolvingRegistry registry,
          @Qualifier("mvcConversionService") FormattingConversionService conversionService) {
    var strategies = new ArrayList<>(registry.getDefaultStrategies().getStrategies());
    strategies.addAll(registry.getCustomizedStrategies().getStrategies());
    return new CompositeUriComponentsContributor(strategies, conversionService);
  }

  /**
   * Provide access to the list of {@link ErrorResponse.Interceptor}'s to apply
   * when rendering error responses.
   * <p>This method cannot be overridden; use {@link #configureErrorResponseInterceptors(List)} instead.
   *
   * @since 5.0
   */
  protected final List<ErrorResponse.Interceptor> getErrorResponseInterceptors() {
    if (this.errorResponseInterceptors == null) {
      this.errorResponseInterceptors = new ArrayList<>();
      configureErrorResponseInterceptors(this.errorResponseInterceptors);
    }
    return this.errorResponseInterceptors;
  }

  /**
   * Override this method for control over the {@link ErrorResponse.Interceptor}'s
   * to apply when rendering error responses.
   *
   * @param interceptors the list to add handlers to
   * @since 5.0
   */
  protected void configureErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {

  }

  /**
   * Return the central strategy to manage API versioning with, or {@code null}
   * if the application does not use versioning.
   *
   * @since 5.0
   */
  @Nullable
  @Component
  public ApiVersionStrategy mvcApiVersionStrategy() {
    if (this.apiVersionStrategy == null) {
      ApiVersionConfigurer configurer = new ApiVersionConfigurer();
      configureApiVersioning(configurer);
      ApiVersionStrategy strategy = configurer.getApiVersionStrategy();
      if (strategy != null) {
        this.apiVersionStrategy = strategy;
      }
    }
    return this.apiVersionStrategy;
  }

  /**
   * Override this method to configure API versioning.
   *
   * @since 5.0
   */
  protected void configureApiVersioning(ApiVersionConfigurer configurer) {
  }

  static boolean isPresent(String name) {
    ClassLoader classLoader = WebMvcConfigurationSupport.class.getClassLoader();
    return ClassUtils.isPresent(name, classLoader);
  }

  private static final class NoOpValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
      return false;
    }

    @Override
    public void validate(@Nullable Object target, Errors errors) { }

  }

}
