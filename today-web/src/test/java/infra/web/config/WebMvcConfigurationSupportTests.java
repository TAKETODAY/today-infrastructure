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

package infra.web.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.beans.DirectFieldAccessor;
import infra.beans.testfixture.beans.TestBean;
import infra.core.Ordered;
import infra.core.io.FileSystemResourceLoader;
import infra.format.FormatterRegistry;
import infra.http.HttpStatus;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.lang.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.scheduling.concurrent.ConcurrentTaskExecutor;
import infra.stereotype.Controller;
import infra.validation.BeanPropertyBindingResult;
import infra.validation.DefaultMessageCodesResolver;
import infra.validation.Errors;
import infra.validation.MessageCodesResolver;
import infra.validation.Validator;
import infra.web.ErrorResponse;
import infra.web.HandlerExceptionHandler;
import infra.web.HandlerInterceptor;
import infra.web.HandlerMapping;
import infra.web.accept.ContentNegotiationManager;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.async.CallableProcessingInterceptor;
import infra.web.async.DeferredResultProcessingInterceptor;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.bind.resolver.ParameterResolvingStrategies;
import infra.web.bind.support.ConfigurableWebBindingInitializer;
import infra.web.config.annotation.ApiVersionConfigurer;
import infra.web.cors.CorsConfiguration;
import infra.web.handler.AbstractHandlerMapping;
import infra.web.handler.CompositeHandlerExceptionHandler;
import infra.web.handler.HandlerExecutionChain;
import infra.web.handler.ResponseStatusExceptionHandler;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.handler.SimpleMappingExceptionHandler;
import infra.web.handler.SimpleUrlHandlerMapping;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.HandlerTypePredicate;
import infra.web.handler.method.ModelAttributeMethodProcessor;
import infra.web.handler.method.RequestMappingHandlerAdapter;
import infra.web.handler.method.RequestMappingHandlerMapping;
import infra.web.handler.method.RequestMappingInfo;
import infra.web.i18n.LocaleChangeInterceptor;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;
import infra.web.view.ContentNegotiatingViewResolver;
import infra.web.view.View;
import infra.web.view.ViewResolver;
import infra.web.view.ViewResolverComposite;
import infra.web.view.json.MappingJackson2JsonView;

import static infra.http.MediaType.APPLICATION_JSON;
import static infra.http.MediaType.APPLICATION_XML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/13 11:44
 */
class WebMvcConfigurationSupportTests {

  private TestWebMvcConfigurationSupport config;

  private StaticWebApplicationContext context;

  @BeforeEach
  void setUp() {
    this.context = new StaticWebApplicationContext();
    this.context.setMockContext(new MockContextImpl(new FileSystemResourceLoader()));
    this.context.registerSingleton("controller", TestController.class);
    this.context.registerSingleton("userController", UserController.class);

    this.config = new TestWebMvcConfigurationSupport();
    this.config.setApplicationContext(this.context);
  }

  @Test
  void handlerMappings() throws Exception {
    RequestMappingHandlerMapping rmHandlerMapping = this.config.requestMappingHandlerMapping(
            this.config.mvcContentNegotiationManager(), this.config.mvcApiVersionStrategy(),
            ParameterResolvingRegistry.get(context));
    rmHandlerMapping.setApplicationContext(this.context);
    rmHandlerMapping.afterPropertiesSet();

    HandlerExecutionChain chain = (HandlerExecutionChain) rmHandlerMapping.getHandler(
            new MockRequestContext(new HttpMockRequestImpl("GET", "/")));

    assertThat(chain).isNotNull();
    HandlerInterceptor[] interceptors = chain.getInterceptors();
    assertThat(interceptors).isNotNull();
    assertThat(interceptors).hasSize(2);
    assertThat(interceptors[0].getClass().getSimpleName()).isEqualTo("CorsInterceptor");
    assertThat(interceptors[1].getClass()).isEqualTo(LocaleChangeInterceptor.class);

    Map<RequestMappingInfo, HandlerMethod> map = rmHandlerMapping.getHandlerMethods();
    assertThat(map).hasSize(2);
    RequestMappingInfo info = map.entrySet().stream()
            .filter(entry -> entry.getValue().getBeanType().equals(UserController.class))
            .findFirst()
            .orElseThrow(() -> new AssertionError("UserController bean not found"))
            .getKey();
    assertThat(info.getPatternValues()).isEqualTo(Collections.singleton("/api/user/{id}"));

    AbstractHandlerMapping handlerMapping = (AbstractHandlerMapping) this.config.viewControllerHandlerMapping();
    handlerMapping.setApplicationContext(this.context);
    assertThat(handlerMapping).isNotNull();
    assertThat(handlerMapping.getOrder()).isEqualTo(1);
    chain = (HandlerExecutionChain) handlerMapping.getHandler(
            new MockRequestContext(new HttpMockRequestImpl("GET", "/path")));
    assertThat(chain).isNotNull();
    assertThat(chain.getRawHandler()).isNotNull();
    chain = (HandlerExecutionChain) handlerMapping.getHandler(new MockRequestContext(new HttpMockRequestImpl("GET", "/bad")));
    assertThat(chain).isNotNull();
    assertThat(chain.getRawHandler()).isNotNull();
    chain = (HandlerExecutionChain) handlerMapping.getHandler(new MockRequestContext(new HttpMockRequestImpl("GET", "/old")));
    assertThat(chain).isNotNull();
    assertThat(chain.getRawHandler()).isNotNull();

    handlerMapping = (AbstractHandlerMapping) this.config.resourceHandlerMapping(null,
            this.config.mvcContentNegotiationManager());
    handlerMapping.setApplicationContext(this.context);
    assertThat(handlerMapping).isNotNull();
    assertThat(handlerMapping.getOrder()).isEqualTo((Integer.MAX_VALUE - 1));
    chain = (HandlerExecutionChain) handlerMapping.getHandler(
            new MockRequestContext(new HttpMockRequestImpl("GET", "/resources/foo.gif")));
    assertThat(chain).isNotNull();
    assertThat(chain.getRawHandler()).isNotNull();
    interceptors = chain.getInterceptors();
    assertThat(interceptors.length).as(Arrays.toString(interceptors)).isEqualTo(2);
    assertThat(interceptors[0].getClass().getSimpleName()).isEqualTo("CorsInterceptor");
    // PathExposingHandlerInterceptor at interceptors[1]
    assertThat(interceptors[1].getClass()).isEqualTo(LocaleChangeInterceptor.class);

  }

  @Test
  void requestMappingHandlerAdapter() {
    RequestMappingHandlerAdapter adapter = this.config.requestMappingHandlerAdapter(
            null, null, null,
            ParameterResolvingRegistry.get(context), this.config.mvcValidator(), this.config.mvcConversionService());

    // ConversionService
    String actual = this.config.mvcConversionService().convert(new TestBean(), String.class);
    assertThat(actual).isEqualTo("converted");

    DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(adapter);

    // Custom argument resolvers and return value handlers
    ParameterResolvingRegistry registry =
            (ParameterResolvingRegistry) fieldAccessor.getPropertyValue("resolvingRegistry");
    assertThat(registry.getMessageConverters()).hasSize(3);

  }

  @Test
  void webBindingInitializer() {
    RequestMappingHandlerAdapter adapter = this.config.requestMappingHandlerAdapter(null, null,
            null, ParameterResolvingRegistry.get(context),
            this.config.mvcValidator(), this.config.mvcConversionService());

    ConfigurableWebBindingInitializer initializer =
            (ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();

    assertThat(initializer).isNotNull();

    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(null, "");
    initializer.getValidator().validate(null, bindingResult);
    assertThat(bindingResult.getAllErrors().get(0).getCode()).isEqualTo("invalid");

    String[] codes = initializer.getMessageCodesResolver().resolveMessageCodes("invalid", null);
    assertThat(codes[0]).isEqualTo("custom.invalid");
  }

  @Test
  public void contentNegotiation() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/foo");

    RequestMappingHandlerMapping mapping = this.config.requestMappingHandlerMapping(
            this.config.mvcContentNegotiationManager(), this.config.mvcApiVersionStrategy(),
            ParameterResolvingRegistry.get(context));

    request.setParameter("f", "json");
    ContentNegotiationManager manager = mapping.getContentNegotiationManager();
    assertThat(manager.resolveMediaTypes(new MockRequestContext(request))).isEqualTo(Collections.singletonList(APPLICATION_JSON));

    request.setParameter("f", "xml");
    assertThat(manager.resolveMediaTypes(new MockRequestContext(request))).isEqualTo(Collections.singletonList(APPLICATION_XML));

    SimpleUrlHandlerMapping handlerMapping = (SimpleUrlHandlerMapping) this.config.resourceHandlerMapping(
            null, this.config.mvcContentNegotiationManager());
    handlerMapping.setApplicationContext(this.context);

    request = new HttpMockRequestImpl("GET", "/resources/foo.gif");
    HandlerExecutionChain chain = (HandlerExecutionChain) handlerMapping.getHandler(new MockRequestContext(request));
    assertThat(chain).isNotNull();
  }

  @Test
  void exceptionHandlers() {
    List<HandlerExceptionHandler> resolvers = ((CompositeHandlerExceptionHandler)
            this.config.handlerExceptionHandler(ParameterResolvingRegistry.get(context), null)).getExceptionHandlers();

    assertThat(resolvers).hasSize(2);
    assertThat(resolvers.get(0).getClass()).isEqualTo(ResponseStatusExceptionHandler.class);
    assertThat(resolvers.get(1).getClass()).isEqualTo(SimpleMappingExceptionHandler.class);
  }

  @SuppressWarnings("unchecked")
  @Test
  void viewResolvers() {
    ViewResolverComposite viewResolver = (ViewResolverComposite) this.config.mvcViewResolver(
            this.config.mvcContentNegotiationManager());
    assertThat(viewResolver.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    List<ViewResolver> viewResolvers = viewResolver.getViewResolvers();

    DirectFieldAccessor accessor = new DirectFieldAccessor(viewResolvers.get(0));
    assertThat(viewResolvers).hasSize(1);
    assertThat(viewResolvers.get(0).getClass()).isEqualTo(ContentNegotiatingViewResolver.class);
    assertThat((Boolean) accessor.getPropertyValue("useNotAcceptableStatusCode")).isFalse();
    assertThat(accessor.getPropertyValue("contentNegotiationManager")).isNotNull();

    List<View> defaultViews = (List<View>) accessor.getPropertyValue("defaultViews");
    assertThat(defaultViews).isNotNull();
    assertThat(defaultViews).hasSize(1);
    assertThat(defaultViews.get(0).getClass()).isEqualTo(MappingJackson2JsonView.class);

    viewResolvers = (List<ViewResolver>) accessor.getPropertyValue("viewResolvers");
    assertThat(viewResolvers).isNotNull();
    assertThat(viewResolvers).hasSize(0);
  }

  @Test
  void crossOrigin() {
    Map<String, CorsConfiguration> configs = this.config.getCorsConfigurations();
    assertThat(configs).hasSize(1);
    assertThat(configs.get("/resources/**").getAllowedOrigins()).containsExactly("*");
  }

  @Controller
  private static class TestController {

    @RequestMapping("/")
    public void handle() {
    }
  }

  /**
   * Since WebMvcConfigurationSupport does not implement WebMvcConfigurer, the purpose
   * of this test class is also to ensure the two are in sync with each other. Effectively
   * that ensures that application config classes that use the combo {@code @EnableWebMvc}
   * plus WebMvcConfigurer can switch to extending WebMvcConfigurationSupport directly for
   * more advanced configuration needs.
   */
  private static class TestWebMvcConfigurationSupport extends WebMvcConfigurationSupport implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
      registry.addConverter(TestBean.class, String.class, testBean -> "converted");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
      converters.add(new MappingJackson2HttpMessageConverter());
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
      converters.add(0, new StringHttpMessageConverter());
    }

    @Override
    public Validator getValidator() {
      return new Validator() {
        @Override
        public void validate(@Nullable Object target, Errors errors) {
          errors.reject("invalid");
        }

        @Override
        public boolean supports(Class<?> clazz) {
          return true;
        }
      };
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
      configurer.favorParameter(true).parameterName("f");
    }

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
      configurer.useRequestHeader("X-API-Version");
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
      configurer.setDefaultTimeout(2500).setTaskExecutor(new ConcurrentTaskExecutor())
              .registerCallableInterceptors(new CallableProcessingInterceptor() { })
              .registerDeferredResultInterceptors(new DeferredResultProcessingInterceptor() { });
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
      configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(new LocaleChangeInterceptor());
    }

    @Override
    public MessageCodesResolver getMessageCodesResolver() {
      return new DefaultMessageCodesResolver() {
        @Override
        public String[] resolveMessageCodes(String errorCode, String objectName) {
          return new String[] { "custom." + errorCode };
        }
      };
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
      registry.addViewController("/path").setViewName("view");
      registry.addRedirectViewController("/old", "/new").setStatusCode(HttpStatus.PERMANENT_REDIRECT);
      registry.addStatusController("/bad", HttpStatus.NOT_FOUND);
    }

    @Override
    public void addErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
      WebMvcConfigurer.super.addErrorResponseInterceptors(interceptors);
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
      registry.enableContentNegotiation(new MappingJackson2JsonView());
    }

    @Override
    public void configureParameterResolving(ParameterResolvingRegistry registry, ParameterResolvingStrategies customizedStrategies) {
      customizedStrategies.add(new ModelAttributeMethodProcessor(true));
    }

    @Override
    public void modifyReturnValueHandlerManager(ReturnValueHandlerManager manager) {
      manager.addHandlers(new ModelAttributeMethodProcessor(true));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/resources/**").addResourceLocations("src/test/java/");
    }

    @Override
    public void configureHandlerRegistry(List<HandlerMapping> handlerRegistries) {
      WebMvcConfigurer.super.configureHandlerRegistry(handlerRegistries);
    }

    @Override
    public void configureExceptionHandlers(List<HandlerExceptionHandler> handlers) {
      handlers.add(new SimpleMappingExceptionHandler());
    }

    @Override
    public void extendExceptionHandlers(List<HandlerExceptionHandler> handlers) {
      handlers.add(0, new ResponseStatusExceptionHandler());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
      registry.addMapping("/resources/**");
    }

  }

  @RestController
  @RequestMapping("/user")
  static class UserController {

    @GetMapping("/{id}")
    public Principal getUser() {
      return mock();
    }
  }

}