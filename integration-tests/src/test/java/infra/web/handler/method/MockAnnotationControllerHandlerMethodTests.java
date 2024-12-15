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

package infra.web.handler.method;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import infra.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import infra.aop.interceptor.SimpleTraceInterceptor;
import infra.aop.support.DefaultPointcutAdvisor;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.propertyeditors.CustomDateEditor;
import infra.beans.propertyeditors.StringTrimmerEditor;
import infra.beans.testfixture.beans.DerivedTestBean;
import infra.beans.testfixture.beans.GenericBean;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.annotation.AnnotationConfigUtils;
import infra.context.annotation.Configuration;
import infra.context.support.PropertySourcesPlaceholderConfigurer;
import infra.core.conversion.Converter;
import infra.format.annotation.DateTimeFormat;
import infra.format.support.FormattingConversionServiceFactoryBean;
import infra.http.HttpCookie;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpMethod;
import infra.http.HttpOutputMessage;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.HttpMessageNotWritableException;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import infra.http.converter.xml.MarshallingHttpMessageConverter;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.mock.api.MockConfig;
import infra.mock.api.MockContext;
import infra.mock.api.MockException;
import infra.mock.api.http.Cookie;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.api.http.HttpSession;
import infra.mock.api.http.Part;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.mock.web.MockMockConfig;
import infra.mock.web.MockMultipartHttpMockRequest;
import infra.oxm.jaxb.Jaxb2Marshaller;
import infra.session.SessionManager;
import infra.session.SessionManagerOperations;
import infra.session.WebSession;
import infra.session.config.EnableWebSession;
import infra.stereotype.Controller;
import infra.ui.Model;
import infra.ui.ModelMap;
import infra.util.MultiValueMap;
import infra.util.StreamUtils;
import infra.util.StringUtils;
import infra.validation.BindingResult;
import infra.validation.Errors;
import infra.validation.FieldError;
import infra.validation.beanvalidation.LocalValidatorFactoryBean;
import infra.web.RedirectModel;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;
import infra.web.SerializationTestUtils;
import infra.web.accept.ContentNegotiationManagerFactoryBean;
import infra.web.annotation.CookieValue;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PathVariable;
import infra.web.annotation.PostMapping;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestHeader;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.ResponseBody;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;
import infra.web.bind.WebDataBinder;
import infra.web.bind.annotation.BindParam;
import infra.web.bind.annotation.InitBinder;
import infra.web.bind.annotation.ModelAttribute;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.bind.resolver.ParameterResolvingStrategy;
import infra.web.bind.support.ConfigurableWebBindingInitializer;
import infra.web.bind.support.WebBindingInitializer;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.handler.function.RouterFunction;
import infra.web.handler.function.RouterFunctions;
import infra.web.handler.function.ServerResponse;
import infra.web.mock.MockRequestContext;
import infra.web.mock.WebApplicationContext;
import infra.web.multipart.MultipartFile;
import infra.web.multipart.support.StringMultipartFileEditor;
import infra.web.testfixture.MockMultipartFile;
import infra.web.testfixture.security.TestPrincipal;
import infra.web.view.AbstractView;
import infra.web.view.ModelAndView;
import infra.web.view.View;
import infra.web.view.ViewResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class MockAnnotationControllerHandlerMethodTests extends AbstractMockHandlerMethodTests {

  @Test
  void emptyValueMapping() throws Exception {
    initDispatcher(ControllerWithEmptyValueMapping.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test");

    request = new HttpMockRequestImpl("GET", "/");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getContentAsString()).isEqualTo("test");
  }

  @Test
  void errorThrownFromHandlerMethod() throws Exception {
    initDispatcher(ControllerWithErrorThrown.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test");
  }

  @Test
  void customAnnotationController() throws Exception {
    initDispatcher(CustomAnnotationController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpMockResponse.SC_OK);
  }

  @Test
  void requiredParamMissing() throws Exception {
    WebApplicationContext webAppContext = initDispatcher(RequiredParamController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpMockResponse.SC_BAD_REQUEST);
    assertThat(webAppContext.isSingleton(RequiredParamController.class.getSimpleName())).isTrue();
  }

  @Test
  void typeConversionError() throws Exception {
    initDispatcher(RequiredParamController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    request.addParameter("id", "foo");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpMockResponse.SC_BAD_REQUEST);
  }

  @Test
  void optionalParamPresent() throws Exception {
    initDispatcher(OptionalParamController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    request.addParameter("id", "val");
    request.addParameter("flag", "true");
    request.addHeader("header", "otherVal");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("val-true-otherVal");
  }

  @Test
  void optionalParamMissing() throws Exception {
    initDispatcher(OptionalParamController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("null-false-null");
  }

  @Test
  void defaultParameters() throws Exception {
    initDispatcher(DefaultValueParamController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("foo--bar");
  }

  @Test
  void defaultExpressionParameters() throws Exception {
    initDispatcher(DefaultExpressionValueParamController.class, wac -> {
      RootBeanDefinition ppc = new RootBeanDefinition(PropertySourcesPlaceholderConfigurer.class);
      ppc.getPropertyValues().add("properties", "myKey=foo");
      wac.registerBeanDefinition("ppc", ppc);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    System.setProperty("myHeader", "bar");
    try {
      getMockApi().service(request, response);
    }
    finally {
      System.clearProperty("myHeader");
    }
    assertThat(response.getContentAsString()).isEqualTo("foo-bar-/myPath.do");
  }

  @Test
  void typeNestedSetBinding() throws Exception {
    initDispatcher(NestedSetController.class, wac -> {
      RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
      csDef.getPropertyValues().add("converters", new TestBeanConverter());
      RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
      wbiDef.getPropertyValues().add("conversionService", csDef);
      RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
      adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
      wac.registerBeanDefinition("handlerAdapter", adapterDef);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    request.addParameter("testBeanSet", "1", "2");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("[1, 2]-infra.beans.testfixture.beans.TestBean");
  }

  @Test
  void pathVariableWithCustomConverter() throws Exception {
    initDispatcher(PathVariableWithCustomConverterController.class, wac -> {
      RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
      csDef.getPropertyValues().add("converters", new AnnotatedExceptionRaisingConverter());
      RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
      wbiDef.getPropertyValues().add("conversionService", csDef);
      RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
      adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
      wac.registerBeanDefinition("handlerAdapter", adapterDef);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath/1");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void methodNotAllowed() throws Exception {
    initDispatcher(MethodNotAllowedController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).as("Invalid response status").isEqualTo(HttpMockResponse.SC_METHOD_NOT_ALLOWED);
    String allowHeader = response.getHeader("Allow");

    assertThat(allowHeader).as("No Allow header").isNotNull();
    Set<String> allowedMethods = new HashSet<>(Arrays.asList(StringUtils.commaDelimitedListToStringArray(allowHeader)));
    assertThat(allowedMethods.size()).as("Invalid amount of supported methods").isEqualTo(6);
    assertThat(allowedMethods).as("PUT not allowed").contains("PUT");
    assertThat(allowedMethods).as("DELETE not allowed").contains("DELETE");
    assertThat(allowedMethods).as("HEAD not allowed").contains("HEAD");
    assertThat(allowedMethods).as("TRACE not allowed").contains("TRACE");
    assertThat(allowedMethods).as("OPTIONS not allowed").contains("OPTIONS");
    assertThat(allowedMethods).as("POST not allowed").contains("POST");
  }

  @Test
  void emptyParameterListHandleMethod() throws Exception {
    initDispatcher(EmptyParameterListHandlerMethodController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/emptyParameterListHandler");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    EmptyParameterListHandlerMethodController.called = false;
    getMockApi().service(request, response);
    assertThat(EmptyParameterListHandlerMethodController.called).isTrue();
    assertThat(response.getContentAsString()).isEmpty();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  void sessionAttributeExposure() throws Exception {
    initDispatcher(MySessionAttributesController.class, wac -> {
      wac.registerBean("viewResolver", ModelExposingViewResolver.class);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPage");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page1");
    WebSession session = (WebSession) request.getAttribute("session");

    assertThat(session).isNotNull();

    request = new HttpMockRequestImpl("POST", "/myPage");
    request.setCookies(new Cookie("SESSION", session.getId()));

    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page2");
  }

  @Test
  void sessionAttributeExposureWithInterface() throws Exception {
    initDispatcher(MySessionAttributesControllerImpl.class, wac -> {
      wac.registerBean("viewResolver", ModelExposingViewResolver.class);
      DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
      autoProxyCreator.setBeanFactory(wac.getBeanFactory());
      autoProxyCreator.setProxyTargetClass(true);
      wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
      wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPage");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page1");

    WebSession session = (WebSession) request.getAttribute("session");

    assertThat(session).isNotNull();

    request = new HttpMockRequestImpl("POST", "/myPage");
    request.setCookies(new Cookie("SESSION", session.getId()));

    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page2");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  void parameterizedAnnotatedInterface() throws Exception {
    initDispatcher(MyParameterizedControllerImpl.class, wac -> {
      wac.registerBean("viewResolver", ModelExposingViewResolver.class);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPage");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page1");

    WebSession session = (WebSession) request.getAttribute("session");

    assertThat(session).isNotNull();
    assertThat(((Map) session.getAttribute("model"))).containsKey("testBeanList");

    request = new HttpMockRequestImpl("POST", "/myPage");
    request.setCookies(new Cookie("SESSION", session.getId()));

    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page2");
    assertThat(((Map) session.getAttribute("model"))).containsKey("testBeanList");
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void parameterizedAnnotatedInterfaceWithOverriddenMappingsInImpl() throws Exception {
    initDispatcher(
            MyParameterizedControllerImplWithOverriddenMappings.class,
            wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class))
    );

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPage");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page1");

    WebSession session = (WebSession) request.getAttribute("session");

    assertThat(session).isNotNull();
    assertThat(((Map) session.getAttribute("model"))).containsKey("testBeanList");

    request = new HttpMockRequestImpl("POST", "/myPage");
    request.setCookies(new Cookie("SESSION", session.getId()));

    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page2");
    assertThat(((Map) session.getAttribute("model"))).containsKey("testBeanList");
  }

  @Test
  void adaptedHandleMethods() throws Exception {
    initDispatcher(MyAdaptedController.class);
    doTestAdaptedHandleMethods();
  }

  @Test
  void adaptedHandleMethods2() throws Exception {
    initDispatcher(MyAdaptedController2.class);
  }

  @Test
  void adaptedHandleMethods3() throws Exception {
    initDispatcher(MyAdaptedController3.class);
    doTestAdaptedHandleMethods();
  }

  private void doTestAdaptedHandleMethods() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath1.do");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    request.addParameter("param1", "value1");
    request.addParameter("param2", "2");
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test");

    request = new HttpMockRequestImpl("GET", "/myPath3.do");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "2");
    request.addParameter("name", "name1");
    request.addParameter("age", "2");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    request = new HttpMockRequestImpl("GET", "/myPath4.do");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "2");
    request.addParameter("name", "name1");
    request.addParameter("age", "value2");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test-name1-typeMismatch");

    request = new HttpMockRequestImpl("GET", "/myPath2.do");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "2");
    request.addHeader("header1", "10");
    request.setCookies(new Cookie("cookie1", "3"));
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test-value1-2-10-3");

  }

  @Test
  void formController() throws Exception {
    initDispatcher(
            MyFormController.class,
            wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class))
    );

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    request.addParameter("name", "name1");
    request.addParameter("age", "value2");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-name1-typeMismatch-tb1-myValue");
  }

  @Test
  void modelFormController() throws Exception {
    initDispatcher(
            MyModelFormController.class,
            wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class))
    );

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    request.addParameter("name", "name1");
    request.addParameter("age", "value2");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("view-name-name1-typeMismatch-tb1-myValue");
  }

  @Test
  void lateBindingFormController() throws Exception {
    initDispatcher(
            LateBindingFormController.class,
            wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class))
    );

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    request.addParameter("name", "name1");
    request.addParameter("age", "value2");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-name1-typeMismatch-tb1-myValue");
  }

  @Test
  void proxiedFormController() throws Exception {
    initDispatcher(MyFormController.class, wac -> {
      wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
      DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
      autoProxyCreator.setBeanFactory(wac.getBeanFactory());
      autoProxyCreator.setProxyTargetClass(true);
      wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
      wac.registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    request.addParameter("name", "name1");
    request.addParameter("age", "value2");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-name1-typeMismatch-tb1-myValue");
  }

  @Test
  void commandProvidingFormControllerWithCustomEditor() throws Exception {
    initDispatcher(MyCommandProvidingFormController.class, wac -> {
      wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
      RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
      adapterDef.getPropertyValues().add("webBindingInitializer", new MyWebBindingInitializer());
      wac.registerBeanDefinition("handlerAdapter", adapterDef);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    request.addParameter("defaultName", "myDefaultName");
    request.addParameter("age", "value2");
    request.addParameter("date", "2007-10-02");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue");
  }

  @Test
  void typedCommandProvidingFormController() throws Exception {
    initDispatcher(MyTypedCommandProvidingFormController.class, wac -> {
      wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));

      wac.registerSingleton(new MyWebBindingInitializer());
      wac.registerSingleton(new MySpecialArgumentResolver());
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath.do");
    request.addParameter("defaultName", "10");
    request.addParameter("age", "value2");
    request.addParameter("date", "2007-10-02");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-Integer:10-typeMismatch-tb1-myOriginalValue");

    request = new HttpMockRequestImpl("GET", "/myOtherPath.do");
    request.addParameter("defaultName", "10");
    request.addParameter("age", "value2");
    request.addParameter("date", "2007-10-02");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-myName-typeMismatch-tb1-myOriginalValue");

    request = new HttpMockRequestImpl("GET", "/myThirdPath.do");
    request.addParameter("defaultName", "10");
    request.addParameter("age", "100");
    request.addParameter("date", "2007-10-02");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-special-99-special-99");
  }

  @Test
  void binderInitializingCommandProvidingFormController() throws Exception {
    initDispatcher(MyBinderInitializingCommandProvidingFormController.class,
            wac -> wac.registerBean("viewResolver", TestViewResolver.class)
    );

    var request = new HttpMockRequestImpl("GET", "/myPath.do");
    request.addParameter("defaultName", "myDefaultName");
    request.addParameter("age", "value2");
    request.addParameter("date", "2007-10-02");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString())
            .isEqualTo("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue");
  }

  @Test
  void specificBinderInitializingCommandProvidingFormController() throws Exception {
    initDispatcher(MySpecificBinderInitializingCommandProvidingFormController.class,
            wac -> wac.registerBeanDefinition("viewResolver",
                    new RootBeanDefinition(TestViewResolver.class))
    );

    var request = new HttpMockRequestImpl("GET", "/myPath.do");
    request.addParameter("defaultName", "myDefaultName");
    request.addParameter("age", "value2");
    request.addParameter("date", "2007-10-02");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString())
            .isEqualTo("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue");
  }

  @Test
  void parameterDispatchingController() throws Exception {
    final MockContextImpl mockContext = new MockContextImpl();
    final MockMockConfig servletConfig = new MockMockConfig(mockContext);

    WebApplicationContext webAppContext =
            initDispatcher(MyParameterDispatchingController.class, wac -> {
              wac.setMockContext(mockContext);
              AnnotationConfigUtils.registerAnnotationConfigProcessors(wac);
              wac.getBeanFactory().registerResolvableDependency(MockConfig.class, servletConfig);
            });

    HttpMockRequestImpl request = new HttpMockRequestImpl(mockContext, "GET", "/myPath.do");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    HttpSession session = request.getSession();
    assertThat(session).isNotNull();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");
    assertThat(request.getAttribute("mockContext")).isSameAs(mockContext);
    assertThat(request.getAttribute("servletConfig")).isSameAs(servletConfig);
    assertThat(request.getAttribute("sessionId")).isSameAs(session.getId());
    assertThat(request.getAttribute("requestUri")).isSameAs(request.getRequestURI());
    assertThat(request.getAttribute("locale")).isSameAs(request.getLocale());

    request = new HttpMockRequestImpl(mockContext, "GET", "/myPath.do");
    response = new MockHttpResponseImpl();
    session = request.getSession();
    assertThat(session).isNotNull();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");
    assertThat(request.getAttribute("mockContext")).isSameAs(mockContext);
    assertThat(request.getAttribute("servletConfig")).isSameAs(servletConfig);
    assertThat(request.getAttribute("sessionId")).isSameAs(session.getId());
    assertThat(request.getAttribute("requestUri")).isSameAs(request.getRequestURI());

    request = new HttpMockRequestImpl(mockContext, "GET", "/myPath.do");
    request.addParameter("view", "other");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myOtherView");

    request = new HttpMockRequestImpl(mockContext, "GET", "/myPath.do");
    request.addParameter("view", "my");
    request.addParameter("lang", "de");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myLangView");

    request = new HttpMockRequestImpl(mockContext, "GET", "/myPath.do");
    request.addParameter("surprise", "!");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("mySurpriseView");

    MyParameterDispatchingController deserialized =
            SerializationTestUtils.serializeAndDeserialize(webAppContext.getBean(
                    MyParameterDispatchingController.class.getSimpleName(), MyParameterDispatchingController.class));
    assertThat(deserialized.request).isNull();
    assertThat(deserialized.session).isNotNull();
  }

  @Test
  void relativePathDispatchingController() throws Exception {
    initDispatcher(MyRelativePathDispatchingController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myApp/myHandle");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");

    request = new HttpMockRequestImpl("GET", "/myApp/myOther");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myOtherView");

    request = new HttpMockRequestImpl("GET", "/myApp/myLang");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myLangView");

    request = new HttpMockRequestImpl("GET", "/myApp/surprise.do");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");
  }

  @Test
  void relativeMethodPathDispatchingController() throws Exception {
    initDispatcher(MyRelativeMethodPathDispatchingController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myApp/myHandle");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");

    request = new HttpMockRequestImpl("GET", "/yourApp/myOther");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myOtherView");

    request = new HttpMockRequestImpl("GET", "/hisApp/myLang");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myLangView");

    request = new HttpMockRequestImpl("GET", "/herApp/surprise.do");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus())
            .as("Suffixes pattern matching should not work with PathPattern's")
            .isEqualTo(404);
  }

  @Test
  void nullCommandController() throws Exception {
    initDispatcher(MyNullCommandController.class);
    getMockApi().init(new MockMockConfig());

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/myPath");
    request.setUserPrincipal(new OtherPrincipal());
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");
  }

  @Test
  void equivalentMappingsWithSameMethodName() {
    assertThatThrownBy(() -> initDispatcher(ChildController.class))
            .isInstanceOf(BeanCreationException.class)
            .hasCauseInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ambiguous mapping");
  }

  @Test
    // gh-22543
  void unmappedPathMapping() throws Exception {
    initDispatcher(UnmappedPathController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bogus-unmapped");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(404);

    request = new HttpMockRequestImpl("GET", "");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("get");
  }

  @Test
  void explicitAndEmptyPathsControllerMapping() throws Exception {
    initDispatcher(ExplicitAndEmptyPathsController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("get");

    request = new HttpMockRequestImpl("GET", "");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("get");
  }

  @Test
  void pathOrdering() throws Exception {
    initDispatcher(PathOrderingController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/dir/myPath1.do");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("method1");
  }

  @Test
  void requestBodyResponseBody() throws Exception {
    initDispatcher(RequestResponseBodyController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "text/*, */*");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo(requestBody);
  }

  @Test
  void httpPatch() throws Exception {
    initDispatcher(RequestResponseBodyController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("PATCH", "/something");
    String requestBody = "Hello world!";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "text/*, */*");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo(requestBody);
  }

  @Test
  void responseBodyNoAcceptableMediaType() throws Exception {
    initDispatcher(RequestResponseBodyProducesController.class, wac -> {
      wac.registerSingleton(new StringHttpMessageConverter());
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "application/pdf, application/msword");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(406);
  }

  @Test
  void responseBodyWildCardMediaType() throws Exception {
    initDispatcher(RequestResponseBodyController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "*/*");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo(requestBody);
  }

  @Test
  void unsupportedRequestBody() throws Exception {
    initDispatcher(RequestResponseBodyController.class, wac -> {
      StringHttpMessageConverter converter = new StringHttpMessageConverter();
      converter.setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_PLAIN));

      wac.registerSingleton(new HttpMessageConverters(false, List.of(converter)));
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "application/pdf");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(415);
    assertThat(response.getHeader("Accept")).isEqualTo("text/plain");
  }

  @Test
  void unsupportedPatchBody() throws Exception {
    initDispatcher(RequestResponseBodyController.class, wac -> {
      StringHttpMessageConverter converter = new StringHttpMessageConverter();
      converter.setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_PLAIN));
      wac.registerSingleton(new HttpMessageConverters(false, List.of(converter)));
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("PATCH", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "application/pdf");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(415);
    assertThat(response.getHeader("Accept-Patch")).isEqualTo("text/plain");
  }

  @Test
  void responseBodyNoAcceptHeader() throws Exception {
    initDispatcher(RequestResponseBodyController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo(requestBody);
  }

  @Test
  void badRequestRequestBody() throws Exception {
    initDispatcher(RequestResponseBodyController.class, wac -> {
      RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
      wac.registerBeanDefinition("handlerAdapter", adapterDef);

      register("parameterResolvingRegistry", ParameterResolvingRegistry.class, wac)
              .getPropertyValues().add("messageConverters", new NotReadableMessageConverter());

      register("returnValueHandlerManager", ReturnValueHandlerManager.class, wac)
              .getPropertyValues().add("messageConverters", new NotReadableMessageConverter()); ;
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "application/pdf");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpMockResponse.SC_BAD_REQUEST);
  }

  @Test
  void httpEntity() throws Exception {
    initDispatcher(ResponseEntityController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/foo");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "text/*, */*");
    request.addHeader("MyRequestHeader", "MyValue");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getContentAsString()).isEqualTo(requestBody);
    assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");

    request = new HttpMockRequestImpl("GET", "/bar");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void httpEntityWithContentType() throws Exception {
    initDispatcher(ResponseEntityController.class, wac -> {
      List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
      messageConverters.add(new MappingJackson2HttpMessageConverter());
      messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
      HttpMessageConverters converters = new HttpMessageConverters(messageConverters);
      wac.registerSingleton("messageConverters", converters);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/test-entity");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("Content-Type")).isEqualTo("application/xml");
    assertThat(response.getContentAsString()).isEqualTo(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<testEntity><name>Foo Bar</name></testEntity>");
  }

  @Test
  void overlappingMessageConvertersRequestBody() throws Exception {
    initDispatcher(RequestResponseBodyController.class, wac -> {
      List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
      messageConverters.add(new StringHttpMessageConverter());
      messageConverters.add(new SimpleMessageConverter(new MediaType("application", "json"), MediaType.ALL));

      HttpMessageConverters converters = new HttpMessageConverters(messageConverters);
      wac.registerSingleton("messageConverters", converters);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("PUT", "/something");
    request.setContent("Hello World".getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "application/json, text/javascript, */*");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getHeader("Content-Type")).as("Invalid content-type").isEqualTo("application/json");
  }

  @Test
  void responseBodyVoid() throws Exception {
    initDispatcher(ResponseBodyVoidController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "text/*, */*");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void responseBodyArgMismatch() throws Exception {
    initDispatcher(RequestBodyArgMismatchController.class, wac -> {
      Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
      marshaller.setClassesToBeBound(A.class, B.class);
      try {
        marshaller.afterPropertiesSet();
      }
      catch (Exception ex) {
        throw new BeanCreationException(ex.getMessage(), ex);
      }
      MarshallingHttpMessageConverter messageConverter = new MarshallingHttpMessageConverter(marshaller);

      wac.registerSingleton(messageConverter);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("PUT", "/something");
    String requestBody = "<b/>";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "application/xml; charset=utf-8");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  void contentTypeHeaders() throws Exception {
    initDispatcher(ContentTypeHeadersController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/something");
    request.setContentType("application/pdf");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("pdf");

    request = new HttpMockRequestImpl("POST", "/something");
    request.setContentType("text/html");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("text");

    request = new HttpMockRequestImpl("POST", "/something");
    request.setContentType("application/xml");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(415);
  }

  @Test
  void consumes() throws Exception {
    initDispatcher(ConsumesController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/something");
    request.setContentType("application/pdf");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("pdf");

    request = new HttpMockRequestImpl("POST", "/something");
    request.setContentType("text/html");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("text");

    request = new HttpMockRequestImpl("POST", "/something");
    request.setContentType("application/xml");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(415);
  }

  @Test
  void negatedContentTypeHeaders() throws Exception {
    initDispatcher(NegatedContentTypeHeadersController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/something");
    request.setContentType("application/pdf");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("pdf");

    request = new HttpMockRequestImpl("POST", "/something");
    request.setContentType("text/html");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("non-pdf");
  }

  @Test
  void acceptHeaders() throws Exception {
    initDispatcher(AcceptHeadersController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "text/html");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("html");

    request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "application/xml");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "application/xml, text/html");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "text/html;q=0.9, application/xml");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "application/msword");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(406);
  }

  @Test
  void produces() throws Exception {
    initDispatcher(ProducesController.class, wac -> {
      wac.registerSingleton(new MappingJackson2HttpMessageConverter());
      wac.registerSingleton(new Jaxb2RootElementHttpMessageConverter());
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "text/html");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("html");

    request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "application/xml");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "application/xml, text/html");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "text/html;q=0.9, application/xml");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "application/msword");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(406);

    request = new HttpMockRequestImpl("GET", "/something");
    request.addHeader("Accept", "text/csv,application/problem+json");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getContentAsString()).isEqualTo("{\"reason\":\"error\"}");
  }

  @Test
  void responseStatus() throws Exception {
    initDispatcher(ResponseStatusController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/something");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("something");
    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getErrorMessage()).isEqualTo("It's alive!");
  }

  @Test
  void bindingCookieValue() throws Exception {
    initDispatcher(BindingCookieValueController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/test");
    request.setCookies(new Cookie("date", "2008-11-18"));
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test-2008");
  }

  @Test
  void ambiguousParams() throws Exception {
    initDispatcher(AmbiguousParamsController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/test");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("noParams");

    request = new HttpMockRequestImpl("GET", "/test");
    request.addParameter("myParam", "42");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myParam-42");
  }

  @Test
  void ambiguousPathAndHttpMethod() throws Exception {
    initDispatcher(AmbiguousPathAndHttpMethodController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bug/EXISTING");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("Pattern");
  }

  @Test
  void bridgeMethods() throws Exception {
    initDispatcher(TestControllerImpl.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/method");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
  }

  @Test
  void bridgeMethodsWithMultipleInterfaces() throws Exception {
    initDispatcher(ArticleController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/method");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
  }

  @Test
  void requestParamMap() throws Exception {
    initDispatcher(RequestParamMapController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/map");
    request.addParameter("key1", "value1");
    request.addParameter("key2", "value21", "value22");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("key1=value1,key2=value21");

    request.setRequestURI("/multiValueMap");
    response = new MockHttpResponseImpl();

    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("key1=[value1],key2=[value21,value22]");
  }

  @Test
  void requestHeaderMap() throws Exception {
    initDispatcher(RequestHeaderMapController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/map");
    request.addHeader("Content-Type", "text/html");
    request.addHeader("Custom-Header", new String[] { "value21", "value22" });
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    getMockApi().service(request, response);
    assertThat(response.getContentAsString())
            .isEqualTo("Content-Type=text/html,Custom-Header=value21");

    request.setRequestURI("/multiValueMap");
    response = new MockHttpResponseImpl();

    getMockApi().service(request, response);
    assertThat(response.getContentAsString())
            .isEqualTo("Content-Type=[text/html],Custom-Header=[value21,value22]");

    request.setRequestURI("/httpHeaders");
    response = new MockHttpResponseImpl();

    getMockApi().service(request, response);
    assertThat(response.getContentAsString())
            .isEqualTo("Content-Type=[text/html],Custom-Header=[value21,value22]");
  }

  @Test
  void requestMappingInterface() throws Exception {
    initDispatcher(IMyControllerImpl.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/handle");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("handle null");

    request = new HttpMockRequestImpl("GET", "/handle");
    request.addParameter("p", "value");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("handle value");
  }

  @Test
  void requestMappingInterfaceWithProxy() throws Exception {
    initDispatcher(IMyControllerImpl.class, wac -> {
      DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
      autoProxyCreator.setBeanFactory(wac.getBeanFactory());
      autoProxyCreator.setProxyTargetClass(true);

      wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
      wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/handle");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("handle null");

    request = new HttpMockRequestImpl("GET", "/handle");
    request.addParameter("p", "value");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("handle value");
  }

  @Test
  void requestMappingBaseClass() throws Exception {
    initDispatcher(MyAbstractControllerImpl.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/handle");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("handle");

  }

  @Test
  void trailingSlash() throws Exception {
    initDispatcher(TrailingSlashController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/foo/");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("templatePath");
  }

  @Test
  void customMapEditor() throws Exception {
    initDispatcher(CustomMapEditorController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/handle");
    request.addParameter("map", "bar");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    getMockApi().service(request, response);

    assertThat(response.getContentAsString()).isEqualTo("test-{foo=bar}");
  }

  @Test
  void multipartFileAsSingleString() throws Exception {
    initDispatcher(MultipartController.class);

    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.setRequestURI("/singleString");
    request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen");
  }

  @Test
  void regularParameterAsSingleString() throws Exception {
    initDispatcher(MultipartController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setRequestURI("/singleString");
    request.setMethod("POST");
    request.addParameter("content", "Juergen");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen");
  }

  @Test
  void multipartFileAsStringArray() throws Exception {
    initDispatcher(MultipartController.class);

    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.setRequestURI("/stringArray");
    request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen");
  }

  @Test
  void regularParameterAsStringArray() throws Exception {
    initDispatcher(MultipartController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setRequestURI("/stringArray");
    request.setMethod("POST");
    request.addParameter("content", "Juergen");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen");
  }

  @Test
  void multipartFilesAsStringArray() throws Exception {
    initDispatcher(MultipartController.class);

    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.setRequestURI("/stringArray");
    request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
    request.addFile(new MockMultipartFile("content", "Eva".getBytes()));
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen-Eva");
  }

  @Test
  void regularParametersAsStringArray() throws Exception {
    initDispatcher(MultipartController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setRequestURI("/stringArray");
    request.setMethod("POST");
    request.addParameter("content", "Juergen");
    request.addParameter("content", "Eva");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen-Eva");
  }

  @Test
  void parameterCsvAsStringArray() throws Exception {
    initDispatcher(CsvController.class, wac -> {
      RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
      RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
      wbiDef.getPropertyValues().add("conversionService", csDef);
      RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
      adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
      wac.registerBeanDefinition("handlerAdapter", adapterDef);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setRequestURI("/integerArray");
    request.setMethod("POST");
    request.addParameter("content", "1,2");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("1-2");
  }

  @Test
  void testMatchWithoutMethodLevelPath() throws Exception {
    initDispatcher(NoPathGetAndM2PostController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/t1/m2");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getStatus()).isEqualTo(405);
  }

  @Test
  void testHeadersCondition() throws Exception {
    initDispatcher(HeadersConditionController.class);

    // No "Accept" header
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(500);

    // Accept "*/*"
    request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("Accept", "*/*");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(500);

    // Accept "application/json"
    request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("Accept", "application/json");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
    assertThat(response.getContentAsString()).isEqualTo("homeJson");
  }

  @Test
  void redirectAttribute() throws Exception {
    WebApplicationContext wac = initDispatcher(RedirectAttributesController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/messages");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    WebSession session = (WebSession) request.getAttribute("session");

    MockRequestContext context = new MockRequestContext(wac, request, response);
    // POST -> bind error
//    getServlet().service(request, response);
//
//    assertThat(response.getStatus()).isEqualTo(200);
//    assertThat(response.getForwardedUrl()).isEqualTo("messages/new");
//    assertThat(RequestContextUtils.getOutputRedirectModel(context).isEmpty()).isTrue();

    // POST -> success
    request = new HttpMockRequestImpl("POST", "/messages");
    request.setCookies(new Cookie("SESSION", session.getId()));

    request.addParameter("name", "Jeff");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    context = new MockRequestContext(wac, request, response);

    assertThat(response.getStatus()).isEqualTo(302);
    assertThat(response.getRedirectedUrl()).isEqualTo("/messages/1?name=value");
    assertThat(RequestContextUtils.getOutputRedirectModel(context).get("successMessage")).isEqualTo("yay!");

    // GET after POST
    request = new HttpMockRequestImpl("GET", "/messages/1");
    request.setQueryString("name=value");
    request.setCookies(new Cookie("SESSION", session.getId()));

    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("Got: yay!");
  }

  @Test
  void flashAttributesWithResponseEntity() throws Exception {
    WebApplicationContext wac = initDispatcher(RedirectAttributesController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/messages-response-entity");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    getMockApi().service(request, response);

    WebSession session = (WebSession) request.getAttribute("session");
    MockRequestContext context = new MockRequestContext(wac, request, response);

    assertThat(response.getStatus()).isEqualTo(302);
    assertThat(response.getRedirectedUrl()).isEqualTo("/messages/1?name=value");
    assertThat(RequestContextUtils.getOutputRedirectModel(context).get("successMessage")).isEqualTo("yay!");

    // GET after POST
    request = new HttpMockRequestImpl("GET", "/messages/1");
    request.setQueryString("name=value");
    request.setCookies(new Cookie("SESSION", session.getId()));

    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("Got: yay!");
  }

  @Test
  void prototypeController() throws Exception {
    initDispatcher(null, wac -> {
      RootBeanDefinition beanDef = new RootBeanDefinition(PrototypeController.class);
      beanDef.setScope(BeanDefinition.SCOPE_PROTOTYPE);
      wac.registerBeanDefinition("controller", beanDef);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addParameter("param", "1");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getContentAsString()).isEqualTo("count:3");

    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getContentAsString()).isEqualTo("count:3");
  }

  @Test
  void restController() throws Exception {
    initDispatcher(ThisWillActuallyRun.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Hello World!");
  }

  @Test
  void responseAsHttpHeaders() throws Exception {
    initDispatcher(HttpHeadersResponseController.class);
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(new HttpMockRequestImpl("POST", "/"), response);

    assertThat(response.getStatus()).as("Wrong status code").isEqualTo(MockHttpResponseImpl.SC_CREATED);
    assertThat(response.getHeaderNames().size()).as("Wrong number of headers").isEqualTo(1);
    assertThat(response.getHeader("location")).as("Wrong value for 'location' header").isEqualTo("/test/items/123");
    assertThat(response.getContentLength()).as("Expected an empty content").isEqualTo(0);
  }

  @Test
  void responseAsHttpHeadersNoHeader() throws Exception {
    initDispatcher(HttpHeadersResponseController.class);
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(new HttpMockRequestImpl("POST", "/empty"), response);

    assertThat(response.getStatus()).as("Wrong status code").isEqualTo(MockHttpResponseImpl.SC_CREATED);
    assertThat(response.getHeaderNames().size()).as("Wrong number of headers").isEqualTo(0);
    assertThat(response.getContentLength()).as("Expected an empty content").isEqualTo(0);
  }

  @Test
  void responseBodyAsHtml() throws Exception {
    initDispatcher(TextRestController.class);

    byte[] content = "alert('boo')".getBytes(StandardCharsets.ISO_8859_1);
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/a1.html");
    request.setContent(content);
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    getMockApi().service(request, response);

    assertThat(response.getStatus())
            .as("Suffixes pattern matching should not work with PathPattern's")
            .isEqualTo(404);
  }

  @Test
  void responseBodyAsHtmlWithSuffixPresent() throws Exception {
    initDispatcher(TextRestController.class, wac -> {
      ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();
      factoryBean.setFavorPathExtension(true);
      factoryBean.afterPropertiesSet();

      wac.registerSingleton("mvcContentNegotiationManager", factoryBean.getObject());
    });

    byte[] content = "alert('boo')".getBytes(StandardCharsets.ISO_8859_1);
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/a2.html");
    request.setContent(content);
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
    assertThat(response.getHeader("Content-Disposition")).isNull();
    assertThat(response.getContentAsByteArray()).isEqualTo(content);
  }

  @Test
  void responseBodyAsHtmlWithProducesCondition() throws Exception {
    initDispatcher(TextRestController.class);

    byte[] content = "alert('boo')".getBytes(StandardCharsets.ISO_8859_1);
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/a3.html");
    request.setContent(content);
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    getMockApi().service(request, response);

    assertThat(response.getStatus())
            .as("Suffixes pattern matching should not work with PathPattern's")
            .isEqualTo(404);
  }

  @Test
  void responseBodyAsTextWithCssExtension() throws Exception {
    initDispatcher(TextRestController.class, wac -> {
      ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();
      factoryBean.setFavorParameter(true);
      factoryBean.addMediaType("css", MediaType.parseMediaType("text/css"));
      factoryBean.afterPropertiesSet();

      wac.registerSingleton("mvcContentNegotiationManager", factoryBean.getObject());
    });

    byte[] content = "body".getBytes(StandardCharsets.ISO_8859_1);
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/a4");
    request.addParameter("format", "css");
    request.setContent(content);
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentType()).isEqualTo("text/css;charset=UTF-8");
    assertThat(response.getHeader("Content-Disposition")).isNull();
    assertThat(response.getContentAsByteArray()).isEqualTo(content);
  }

  @Test
  @Disabled
  void modelAndViewWithStatus() throws Exception {
    initDispatcher(ModelAndViewController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(response.getForwardedUrl()).isEqualTo("view");
  }

  @Test
  void modelAndViewWithStatusForRedirect() throws Exception {
    initDispatcher(ModelAndViewController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/redirect");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(307);
    assertThat(response.getRedirectedUrl()).isEqualTo("/path");
  }

  @Test
  @Disabled
  void modelAndViewWithStatusInExceptionHandler() throws Exception {
    initDispatcher(ModelAndViewController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/exception");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(response.getForwardedUrl()).isEqualTo("view");
  }

  @Test
  @Disabled
  void httpHead() throws Exception {
    initDispatcher(ResponseEntityController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("HEAD", "/baz");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");
    assertThat(response.getContentLength()).isEqualTo(4);
    assertThat(response.getContentAsByteArray().length).isEqualTo(0);

    // Now repeat with GET
    request = new HttpMockRequestImpl("GET", "/baz");
    response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");
    assertThat(response.getContentLength()).isEqualTo(4);
    assertThat(response.getContentAsString()).isEqualTo("body");
  }

  @Test
  void httpHeadExplicit() throws Exception {
    initDispatcher(ResponseEntityController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("HEAD", "/stores");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("h1")).isEqualTo("v1");
  }

  @Test
  void httpOptions() throws Exception {
    initDispatcher(ResponseEntityController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("OPTIONS", "/baz");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
    assertThat(response.getContentAsByteArray().length).isEqualTo(0);
  }

  @Test
  void dataClassBinding() throws Exception {
    initDispatcher(DataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithPathVariable() throws Exception {
    initDispatcher(PathVariableDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind/true");
    request.addParameter("param1", "value1");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithMultipartFile() throws Exception {
    initDispatcher(MultipartFileDataClassController.class);

    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.setRequestURI("/bind");
    request.addFile(new MockMultipartFile("param1", "value1".getBytes(StandardCharsets.UTF_8)));
    request.addParameter("param2", "true");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithAdditionalSetter() throws Exception {
    initDispatcher(DataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("param3", "3");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
  }

  @Test
  void dataClassBindingWithAdditionalSetterInDeclarativeBindingMode() throws Exception {
    initDispatcher(DataClassController.class, wac -> {
      ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
      initializer.setDeclarativeBinding(true);

      RootBeanDefinition mappingDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
      mappingDef.getPropertyValues().add("webBindingInitializer", initializer);
      wac.registerBeanDefinition("handlerAdapter", mappingDef);
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("param3", "3");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithResult() throws Exception {
    initDispatcher(ValidatedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", " value1");
    request.addParameter("param2", "true");
    request.addParameter("param3", "3");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
  }

  @Test
  void dataClassBindingWithOptionalParameter() throws Exception {
    initDispatcher(ValidatedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", " value1");
    request.addParameter("param2", "true");
    request.addParameter("optionalParam", "8");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-8");
  }

  @Test
  void dataClassBindingWithMissingParameter() throws Exception {
    initDispatcher(ValidatedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", " value1");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("1:value1-null-null");
  }

  @Test
  void dataClassBindingWithConversionError() throws Exception {
    initDispatcher(ValidatedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", " value1");
    request.addParameter("param2", "x");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("1:value1-x-null");
  }

  @Test
  void dataClassBindingWithValidationError() throws Exception {
    initDispatcher(ValidatedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param2", "true");
    request.addParameter("param3", "0");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("1:-true-0");
  }

  @Test
  void dataClassBindingWithValidationErrorAndConversionError() throws Exception {
    initDispatcher(ValidatedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param2", "x");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("2:null-x-null");
  }

  @Test
  void dataClassBindingWithNullable() throws Exception {
    initDispatcher(NullableDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("param3", "3");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
  }

  @Test
  void dataClassBindingWithNullableAndConversionError() throws Exception {
    initDispatcher(NullableDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "x");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-x-null");
  }

  @Test
  void dataClassBindingWithOptional() throws Exception {
    initDispatcher(OptionalDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("param3", "3");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
  }

  @Test
  void dataClassBindingWithOptionalAndConversionError() throws Exception {
    initDispatcher(OptionalDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "x");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-x-null");
  }

  @Test
  void dataClassBindingWithFieldMarker() throws Exception {
    initDispatcher(DataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("_param2", "on");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithFieldMarkerFallback() throws Exception {
    initDispatcher(DataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("_param2", "on");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-false-0");
  }

  @Test
  void dataClassBindingWithFieldDefault() throws Exception {
    initDispatcher(DataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("!param2", "false");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithFieldDefaultFallback() throws Exception {
    initDispatcher(DataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("!param2", "false");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-false-0");
  }

  @Test
  void dataClassBindingWithLocalDate() throws Exception {
    initDispatcher(DateClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("date", "2010-01-01");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("2010-01-01");
  }

  @Test
  void dataRecordBinding() throws Exception {
    initDispatcher(DataRecordController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("param3", "3");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
  }

  @Test
  void nestedDataClassBinding() throws Exception {
    initDispatcher(NestedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("nestedParam2.param1", "nestedValue1");
    request.addParameter("nestedParam2.param2", "true");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-nestedValue1-true-0");
  }

  @Test
  void nestedDataClassBindingWithAdditionalSetter() throws Exception {
    initDispatcher(NestedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("nestedParam2.param1", "nestedValue1");
    request.addParameter("nestedParam2.param2", "true");
    request.addParameter("nestedParam2.param3", "3");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-nestedValue1-true-3");
  }

  @Test
  void nestedDataClassBindingWithOptionalParameter() throws Exception {
    initDispatcher(NestedValidatedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("nestedParam2.param1", "nestedValue1");
    request.addParameter("nestedParam2.param2", "true");
    request.addParameter("nestedParam2.optionalParam", "8");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-nestedValue1-true-8");
  }

  @Test
  void nestedDataClassBindingWithMissingParameter() throws Exception {
    initDispatcher(NestedValidatedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("nestedParam2.param1", "nestedValue1");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("1:value1-nestedValue1-null-null");
  }

  @Test
  void nestedDataClassBindingWithConversionError() throws Exception {
    initDispatcher(NestedValidatedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("nestedParam2.param1", "nestedValue1");
    request.addParameter("nestedParam2.param2", "x");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("1:value1-nestedValue1-x-null");
  }

  @Test
  void nestedDataClassBindingWithValidationError() throws Exception {
    initDispatcher(NestedValidatedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("nestedParam2.param2", "true");
    request.addParameter("nestedParam2.param3", "0");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("1:value1--true-0");
  }

  @Test
  void nestedDataClassBindingWithValidationErrorAndConversionError() throws Exception {
    initDispatcher(NestedValidatedDataClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("nestedParam2.param2", "x");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("2:value1-null-x-null");
  }

  @Test
  void nestedDataClassBindingWithDataAndLocalDate() throws Exception {
    initDispatcher(NestedDataAndDateClassController.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("nestedParam2.param1", "nestedValue1");
    request.addParameter("nestedParam2.param2", "true");
    request.addParameter("nestedParam2.optionalParam", "8");
    request.addParameter("nestedParam3.date", "2010-01-01");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("2010-01-01");
  }

  @Test
  void routerFunction() throws MockException, IOException {
    initDispatcher(wac -> {
      wac.registerBean(RouterFunction.class, () ->
              RouterFunctions.route()
                      .GET("/foo", request -> ServerResponse.ok().body("foo-body"))
                      .build());

    });

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/foo");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    getMockApi().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("foo-body");
  }

  @Controller
  static class ControllerWithEmptyValueMapping {

    @RequestMapping("")
    public void myPath2(RequestContext response) {
      throw new IllegalStateException("test");
    }

    @RequestMapping("/bar")
    public void myPath3(RequestContext response) throws IOException {
      response.getWriter().write("testX");
    }

    @ExceptionHandler
    public void myPath2(Exception ex, RequestContext response) throws IOException {
      response.getWriter().write(ex.getMessage());
    }
  }

  @Controller
  private static class ControllerWithErrorThrown {

    @RequestMapping("")
    public void myPath2(RequestContext response) {
      throw new AssertionError("test");
    }

    @RequestMapping("/bar")
    public void myPath3(RequestContext response) throws IOException {
      response.getWriter().write("testX");
    }

    @ExceptionHandler
    public void myPath2(Error err, RequestContext response) throws IOException {
      response.getWriter().write(err.getMessage());
    }
  }

  @Controller
  static class MyAdaptedController {

    @RequestMapping("/myPath1.do")
    public void myHandle(RequestContext request, RequestContext response) throws IOException {
      response.getWriter().write("test");
    }

    @RequestMapping("/myPath2.do")
    public void myHandle(@RequestParam("param1") String p1, @RequestParam("param2") int p2,
            @RequestHeader("header1") long h1, @CookieValue(name = "cookie1") HttpCookie c1,
            RequestContext response) throws IOException {
      response.getWriter().write("test-" + p1 + "-" + p2 + "-" + h1 + "-" + c1.getValue());
    }

    @RequestMapping("/myPath3")
    public void myHandle(TestBean tb, RequestContext response) throws IOException {
      response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
    }

    @RequestMapping("/myPath4.do")
    public void myHandle(TestBean tb, Errors errors, RequestContext response) throws IOException {
      response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
    }
  }

  @Controller
  @RequestMapping("/*.do")
  static class MyAdaptedController2 {

    @RequestMapping
    public void myHandle(RequestContext request, RequestContext response) throws IOException {
      response.getWriter().write("test");
    }

    @RequestMapping("/myPath2.do")
    public void myHandle(@RequestParam("param1") String p1, int param2, RequestContext response,
            @RequestHeader("header1") String h1, @CookieValue("cookie1") String c1) throws IOException {
      response.getWriter().write("test-" + p1 + "-" + param2 + "-" + h1 + "-" + c1);
    }

    @RequestMapping("/myPath3")
    public void myHandle(TestBean tb, RequestContext response) throws IOException {
      response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
    }

    @RequestMapping("/myPath4.*")
    public void myHandle(TestBean tb, Errors errors, RequestContext response) throws IOException {
      response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
    }
  }

  @Controller
  static class MyAdaptedControllerBase<T> {

    @RequestMapping("/myPath2.do")
    public void myHandle(@RequestParam("param1") T p1, int param2, @RequestHeader Integer header1,
            @CookieValue int cookie1, RequestContext response) throws IOException {
      response.getWriter().write("test-" + p1 + "-" + param2 + "-" + header1 + "-" + cookie1);
    }

    @InitBinder
    public void initBinder(@RequestParam("param1") String p1,
            @RequestParam(value = "paramX", required = false) String px, int param2) {

      assertThat(px).isNull();
    }

    @ModelAttribute
    public void modelAttribute(@RequestParam("param1") String p1,
            @RequestParam(value = "paramX", required = false) String px, int param2) {

      assertThat(px).isNull();
    }
  }

  @RequestMapping("/*.do")
  static class MyAdaptedController3 extends MyAdaptedControllerBase<String> {

    @RequestMapping
    public void myHandle(RequestContext request, RequestContext response) throws IOException {
      response.getWriter().write("test");
    }

    @Override
    public void myHandle(@RequestParam("param1") String p1, int param2, @RequestHeader Integer header1,
            @CookieValue int cookie1, RequestContext response) throws IOException {
      response.getWriter().write("test-" + p1 + "-" + param2 + "-" + header1 + "-" + cookie1);
    }

    @RequestMapping("/myPath3")
    public void myHandle(TestBean tb, RequestContext response) throws IOException {
      response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
    }

    @RequestMapping("/myPath4.*")
    public void myHandle(TestBean tb, Errors errors, RequestContext response) throws IOException {
      response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
    }

    @Override
    @InitBinder
    public void initBinder(@RequestParam("param1") String p1,
            @RequestParam(value = "paramX", required = false) String px, int param2) {

      assertThat(px).isNull();
    }

    @Override
    @ModelAttribute
    public void modelAttribute(@RequestParam("param1") String p1,
            @RequestParam(value = "paramX", required = false) String px, int param2) {

      assertThat(px).isNull();
    }
  }

  @Controller
  @RequestMapping(method = HttpMethod.GET)
  static class EmptyParameterListHandlerMethodController {

    static boolean called;

    @RequestMapping("/emptyParameterListHandler")
    public void emptyParameterListHandler() {
      EmptyParameterListHandlerMethodController.called = true;
    }

    @RequestMapping("/nonEmptyParameterListHandler")
    public void nonEmptyParameterListHandler(RequestContext response) {
    }
  }

  @Controller
  @EnableWebSession
  @RequestMapping("/myPage")
  static class MySessionAttributesController extends AbstractSessionManagerAutowired {

    public MySessionAttributesController(SessionManager sessionManager) {
      super(sessionManager);
    }

    @RequestMapping(method = HttpMethod.GET)
    public String get(Model model) {
      model.addAttribute("object1", new Object());
      model.addAttribute("object2", new Object());
      saveSession();
      return "page1";
    }

    @RequestMapping(method = HttpMethod.POST)
    public String post(@ModelAttribute("object1") Object object1) {
      //do something with object1
      saveSession();
      return "page2";

    }
  }

  @RequestMapping("/myPage")
  @Controller
  interface MySessionAttributesControllerIfc {

    @RequestMapping(method = HttpMethod.GET)
    String get(Model model);

    @RequestMapping(method = HttpMethod.POST)
    String post(@ModelAttribute("object1") Object object1);
  }

  abstract static class AbstractSessionManagerAutowired extends SessionManagerOperations {

    @Autowired
    RequestContext requestContext;

    @Autowired
    RequestContext request;

    public AbstractSessionManagerAutowired(SessionManager sessionManager) {
      super(sessionManager);
    }

    public void saveSession() {
      request.setAttribute("session", getSession(requestContext));
    }

  }

  @EnableWebSession
  static class MySessionAttributesControllerImpl extends AbstractSessionManagerAutowired
          implements MySessionAttributesControllerIfc {

    public MySessionAttributesControllerImpl(SessionManager sessionManager) {
      super(sessionManager);
    }

    @Override
    public String get(Model model) {
      model.addAttribute("object1", new Object());
      model.addAttribute("object2", new Object());
      saveSession();
      return "page1";
    }

    @Override
    public String post(@ModelAttribute("object1") Object object1) {
      //do something with object1
      saveSession();
      return "page2";
    }
  }

  @RequestMapping("/myPage")
  interface MyParameterizedControllerIfc<T> {

    @ModelAttribute("testBeanList")
    List<TestBean> getTestBeans();

    @RequestMapping(method = HttpMethod.GET)
    String get(Model model);
  }

  interface MyEditableParameterizedControllerIfc<T> extends MyParameterizedControllerIfc<T> {

    @RequestMapping(method = HttpMethod.POST)
    String post(@ModelAttribute("object1") T object);
  }

  @Controller
  @Configuration(proxyBeanMethods = false)
  @EnableWebSession
  static class MyParameterizedControllerImpl extends AbstractSessionManagerAutowired
          implements MyEditableParameterizedControllerIfc<TestBean> {

    public MyParameterizedControllerImpl(SessionManager sessionManager) {
      super(sessionManager);
    }

    @Override
    public List<TestBean> getTestBeans() {
      List<TestBean> list = new ArrayList<>();
      list.add(new TestBean("tb1"));
      list.add(new TestBean("tb2"));
      return list;
    }

    @Override
    public String get(Model model) {
      model.addAttribute("object1", new TestBean());
      model.addAttribute("object2", new TestBean());
      saveSession();
      return "page1";
    }

    @Override
    public String post(TestBean object) {
      //do something with object1
      saveSession();
      return "page2";
    }
  }

  @Controller
  @EnableWebSession
  static class MyParameterizedControllerImplWithOverriddenMappings
          extends AbstractSessionManagerAutowired
          implements MyEditableParameterizedControllerIfc<TestBean> {

    public MyParameterizedControllerImplWithOverriddenMappings(SessionManager sessionManager) {
      super(sessionManager);
    }

    @Override
    @ModelAttribute("testBeanList")
    public List<TestBean> getTestBeans() {
      List<TestBean> list = new ArrayList<>();
      list.add(new TestBean("tb1"));
      list.add(new TestBean("tb2"));
      saveSession();
      return list;
    }

    @Override
    @RequestMapping(method = HttpMethod.GET)
    public String get(Model model) {
      model.addAttribute("object1", new TestBean());
      model.addAttribute("object2", new TestBean());
      return "page1";
    }

    @Override
    @RequestMapping(method = HttpMethod.POST)
    public String post(@ModelAttribute("object1") TestBean object1) {
      //do something with object1
      return "page2";
    }
  }

  @Controller
  static class MyFormController {

    @ModelAttribute("testBeanList")
    public List<TestBean> getTestBeans() {
      List<TestBean> list = new ArrayList<>();
      list.add(new TestBean("tb1"));
      list.add(new TestBean("tb2"));
      return list;
    }

    @RequestMapping("/myPath.do")
    public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, ModelMap model) {
      FieldError error = errors.getFieldError("age");
      assertThat(error).as("Must have field error for age property").isNotNull();
      assertThat(error.getRejectedValue()).isEqualTo("value2");
      if (!model.containsKey("myKey")) {
        model.addAttribute("myKey", "myValue");
      }
      return "myView";
    }
  }

  static class ValidTestBean extends TestBean {

    @NotNull
    private String validCountry;

    public void setValidCountry(String validCountry) {
      this.validCountry = validCountry;
    }

    public String getValidCountry() {
      return this.validCountry;
    }
  }

  @Controller
  static class MyModelFormController {

    @ModelAttribute
    public List<TestBean> getTestBeans() {
      List<TestBean> list = new ArrayList<>();
      list.add(new TestBean("tb1"));
      list.add(new TestBean("tb2"));
      return list;
    }

    @RequestMapping("/myPath.do")
    public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, Model model) {
      if (!model.containsAttribute("myKey")) {
        model.addAttribute("myKey", "myValue");
      }
      return "view-name";
    }
  }

  @Controller
  static class LateBindingFormController {

    @ModelAttribute("testBeanList")
    public List<TestBean> getTestBeans(@ModelAttribute(name = "myCommand", binding = false) TestBean tb) {
      List<TestBean> list = new ArrayList<>();
      list.add(new TestBean("tb1"));
      list.add(new TestBean("tb2"));
      return list;
    }

    @RequestMapping("/myPath.do")
    public String myHandle(@ModelAttribute(name = "myCommand", binding = true) TestBean tb,
            BindingResult errors, ModelMap model) {

      FieldError error = errors.getFieldError("age");
      assertThat(error).as("Must have field error for age property").isNotNull();
      assertThat(error.getRejectedValue()).isEqualTo("value2");
      if (!model.containsKey("myKey")) {
        model.addAttribute("myKey", "myValue");
      }
      return "myView";
    }
  }

  @Controller
  static class MyCommandProvidingFormController<T, TB, TB2> extends MyFormController {

    @ModelAttribute("myCommand")
    public ValidTestBean createTestBean(@RequestParam T defaultName, Map<String, Object> model,
            @RequestParam Date date) {

      model.put("myKey", "myOriginalValue");
      ValidTestBean tb = new ValidTestBean();
      tb.setName(defaultName.getClass().getSimpleName() + ":" + defaultName.toString());
      return tb;
    }

    @Override
    @RequestMapping("/myPath.do")
    public String myHandle(@ModelAttribute("myCommand") @Valid TestBean tb, BindingResult errors, ModelMap model) {
      if (!errors.hasFieldErrors("validCountry")) {
        throw new IllegalStateException("Declarative validation not applied");
      }
      return super.myHandle(tb, errors, model);
    }

    @RequestMapping("/myOtherPath.do")
    public String myOtherHandle(TB tb, BindingResult errors, ModelMap model, MySpecialArg arg) {
      TestBean tbReal = (TestBean) tb;
      tbReal.setName("myName");
      boolean condition = model.get("ITestBean") instanceof DerivedTestBean;
      assertThat(condition).isTrue();
      assertThat(arg).isNotNull();
      return super.myHandle(tbReal, errors, model);
    }

    @RequestMapping("/myThirdPath.do")
    public String myThirdHandle(TB tb, Model model) {
      model.addAttribute("testBean", new TestBean("special", 99));
      return "myView";
    }

    @SuppressWarnings("unchecked")
    @ModelAttribute
    protected TB2 getModelAttr() {
      return (TB2) new DerivedTestBean();
    }
  }

  static class MySpecialArg {

    MySpecialArg(String value) {
    }
  }

  @Controller
  private static class MyTypedCommandProvidingFormController
          extends MyCommandProvidingFormController<Integer, TestBean, ITestBean> {

  }

  @Controller
  static class MyBinderInitializingCommandProvidingFormController
          extends MyCommandProvidingFormController<String, TestBean, ITestBean> {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
      binder.initBeanPropertyAccess();
      binder.setRequiredFields("sex");
      LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
      vf.afterPropertiesSet();
      binder.setValidator(vf);
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      dateFormat.setLenient(false);
      binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
    }

    @Override
    @RequestMapping("/myPath.do")
    public String myHandle(@ModelAttribute("myCommand") @Valid TestBean tb, BindingResult errors, ModelMap model) {
      if (!errors.hasFieldErrors("sex")) {
        throw new IllegalStateException("requiredFields not applied");
      }
      return super.myHandle(tb, errors, model);
    }
  }

  @Controller
  static class MySpecificBinderInitializingCommandProvidingFormController
          extends MyCommandProvidingFormController<String, TestBean, ITestBean> {

    @InitBinder({ "myCommand", "date" })
    public void initBinder(WebDataBinder binder, String date, @RequestParam("date") String[] date2) {
      LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
      vf.afterPropertiesSet();
      binder.setValidator(vf);
      assertThat(date).isEqualTo("2007-10-02");
      assertThat(date2).hasSize(1);
      assertThat(date2[0]).isEqualTo("2007-10-02");
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      dateFormat.setLenient(false);
      binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
    }
  }

  static class MyWebBindingInitializer implements WebBindingInitializer {

    @Override
    public void initBinder(WebDataBinder binder) {
      LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
      vf.afterPropertiesSet();
      binder.setValidator(vf);
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      dateFormat.setLenient(false);
      binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
    }
  }

  static class MySpecialArgumentResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      return resolvable.getParameterType().equals(MySpecialArg.class);
    }

    @Nullable
    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return new MySpecialArg("myValue");
    }
  }

  @Controller
  @RequestMapping("/myPath.do")
  static class MyParameterDispatchingController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Nullable
    @Autowired
    private transient MockContext mockContext;

    @Nullable
    @Autowired
    private transient MockConfig mockConfig;

    @Nullable
    @Autowired
    private HttpSession session;

    @Nullable
    @Autowired
    private transient RequestContext request;

    @Nullable
    @Autowired
    private transient RequestContext webRequest;

    @RequestMapping
    public void myHandle(RequestContext response, RequestContext request) throws IOException {
      if (this.mockContext == null || this.mockConfig == null || this.session == null ||
              this.request == null || this.webRequest == null) {
        throw new IllegalStateException();
      }
      response.getWriter().write("myView");
      request.setAttribute("mockContext", this.mockContext);
      request.setAttribute("servletConfig", this.mockConfig);
      request.setAttribute("sessionId", this.session.getId());
      request.setAttribute("requestUri", this.request.getRequestURI());
      request.setAttribute("locale", this.webRequest.getLocale());
    }

    @RequestMapping(params = { "view", "!lang" })
    public void myOtherHandle(RequestContext response) throws IOException {
      response.getWriter().write("myOtherView");
    }

    @RequestMapping(method = HttpMethod.GET, params = { "view=my", "lang=de" })
    public void myLangHandle(RequestContext response) throws IOException {
      response.getWriter().write("myLangView");
    }

    @RequestMapping(method = { HttpMethod.POST, HttpMethod.GET }, params = "surprise")
    public void mySurpriseHandle(RequestContext response) throws IOException {
      response.getWriter().write("mySurpriseView");
    }
  }

  @Controller
  @RequestMapping(value = "/myPath.do", params = { "active" })
  static class MyConstrainedParameterDispatchingController {

    @RequestMapping(params = { "view", "!lang" })
    public void myOtherHandle(RequestContext response) throws IOException {
      response.getWriter().write("myOtherView");
    }

    @RequestMapping(method = HttpMethod.GET, params = { "view=my", "lang=de" })
    public void myLangHandle(RequestContext response) throws IOException {
      response.getWriter().write("myLangView");
    }
  }

  @Controller
  @RequestMapping("/myApp/*")
  static class MyRelativePathDispatchingController {

    @RequestMapping
    public void myHandle(RequestContext response) throws IOException {
      response.getWriter().write("myView");
    }

    @RequestMapping("*Other")
    public void myOtherHandle(RequestContext response) throws IOException {
      response.getWriter().write("myOtherView");
    }

    @RequestMapping("myLang")
    public void myLangHandle(RequestContext response) throws IOException {
      response.getWriter().write("myLangView");
    }

    @RequestMapping("surprise")
    public void mySurpriseHandle(RequestContext response) throws IOException {
      response.getWriter().write("mySurpriseView");
    }
  }

  @Controller
  static class MyRelativeMethodPathDispatchingController {

    @RequestMapping("*/myHandle") // was **/myHandle
    public void myHandle(RequestContext response) throws IOException {
      response.getWriter().write("myView");
    }

    @RequestMapping("/*/*Other") // was /**/*Other
    public void myOtherHandle(RequestContext response) throws IOException {
      response.getWriter().write("myOtherView");
    }

    @RequestMapping("*/myLang") // was **/myLang
    public void myLangHandle(RequestContext response) throws IOException {
      response.getWriter().write("myLangView");
    }

    @RequestMapping("/*/surprise") // was /**/surprise
    public void mySurpriseHandle(RequestContext response) throws IOException {
      response.getWriter().write("mySurpriseView");
    }
  }

  @Controller
  static class MyNullCommandController {

    @ModelAttribute
    public TestBean getTestBean() {
      return null;
    }

    @ModelAttribute
    public Principal getPrincipal() {
      return new TestPrincipal("test");
    }

    @RequestMapping("/myPath")
    public void handle(@ModelAttribute TestBean testBean,
            Errors errors,
            @ModelAttribute TestPrincipal modelPrinc,
            OtherPrincipal requestPrinc,
            Writer writer) throws IOException {
      assertThat(testBean).isNull();
      assertThat(modelPrinc).isNotNull();
      assertThat(requestPrinc).isNotNull();
      assertThat(errors.hasErrors()).isFalse();
      errors.reject("myCode");
      writer.write("myView");
    }
  }

  static class OtherPrincipal implements Principal {

    @Override
    public String getName() {
      return "other";
    }
  }

  static class TestViewResolver implements ViewResolver {

    @Nullable
    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
      return new AbstractView() {
        @Override
        protected void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws Exception {
          TestBean tb = (TestBean) model.get("testBean");
          if (tb == null) {
            tb = (TestBean) model.get("myCommand");
          }
          if (tb.getName() != null && tb.getName().endsWith("myDefaultName")) {
            assertThat(tb.getDate().getYear()).isEqualTo(107);
          }
          Errors errors = (Errors) model.get(BindingResult.MODEL_KEY_PREFIX + "testBean");
          if (errors == null) {
            errors = (Errors) model.get(BindingResult.MODEL_KEY_PREFIX + "myCommand");
          }
          if (errors.hasFieldErrors("date")) {
            throw new IllegalStateException();
          }
          if (model.containsKey("ITestBean")) {
            boolean condition = model.get(BindingResult.MODEL_KEY_PREFIX + "ITestBean") instanceof Errors;
            assertThat(condition).isTrue();
          }
          List<TestBean> testBeans = (List<TestBean>) model.get("testBeanList");
          if (errors.hasFieldErrors("age")) {
            request.getWriter()
                    .write(viewName + "-" + tb.getName() + "-" + errors.getFieldError("age").getCode() +
                            "-" + testBeans.get(0).getName() + "-" + model.get("myKey") +
                            (model.containsKey("yourKey") ? "-" + model.get("yourKey") : ""));
          }
          else {
            request.getWriter().write(viewName + "-" + tb.getName() + "-" + tb.getAge() + "-" +
                    errors.getFieldValue("name") + "-" + errors.getFieldValue("age"));
          }
        }
      };
    }
  }

  static class ModelExposingViewResolver extends SessionManagerOperations implements ViewResolver {

    public ModelExposingViewResolver(SessionManager sessionManager) {
      super(sessionManager);
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) {
      return new AbstractView() {

        @Override
        protected void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws Exception {
          request.setAttribute("viewName", viewName);
          setAttribute(request, "model", model);
        }
      };
    }
  }

  static class ParentController {

    @RequestMapping(method = HttpMethod.GET)
    public void doGet(RequestContext req, RequestContext resp) {
    }
  }

  @Controller
  @RequestMapping("/child/test")
  static class ChildController extends ParentController {

    @RequestMapping(method = HttpMethod.GET)
    public void doGet(RequestContext req, RequestContext resp, @RequestParam("childId") String id) {
    }
  }

  @Controller
  // @RequestMapping intentionally omitted
  static class UnmappedPathController {

    @GetMapping // path intentionally omitted
    public void get(Writer writer) throws IOException {
      writer.write("get");
    }
  }

  @Controller
  // @RequestMapping intentionally omitted
  static class ExplicitAndEmptyPathsController {

    @GetMapping({ "/", "" })
    public void get(Writer writer) throws IOException {
      writer.write("get");
    }
  }

  @Target({ ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Controller
  public @interface MyControllerAnnotation {
  }

  @MyControllerAnnotation
  static class CustomAnnotationController {

    @RequestMapping("/myPath.do")
    public void myHandle() {
    }
  }

  @Controller
  static class RequiredParamController {

    @RequestMapping("/myPath.do")
    public void myHandle(@RequestParam(value = "id", required = true) int id,
            @RequestHeader(value = "header", required = true) String header) {
    }
  }

  @Controller
  static class OptionalParamController {

    @RequestMapping("/myPath.do")
    public void myHandle(@RequestParam(required = false) String id,
            @RequestParam(required = false) boolean flag,
            @RequestHeader(value = "header", required = false) String header,
            RequestContext response) throws IOException {
      response.getWriter().write(String.valueOf(id) + "-" + flag + "-" + String.valueOf(header));
    }
  }

  @Controller
  static class DefaultValueParamController {

    @RequestMapping("/myPath.do")
    public void myHandle(@RequestParam(value = "id", defaultValue = "foo") String id,
            @RequestParam(value = "otherId", defaultValue = "") String id2,
            @RequestHeader(defaultValue = "bar") String header,
            RequestContext response) throws IOException {
      response.getWriter().write(String.valueOf(id) + "-" + String.valueOf(id2) + "-" + String.valueOf(header));
    }
  }

  @Controller
  static class DefaultExpressionValueParamController {

    @RequestMapping("/myPath.do")
    public void myHandle(@RequestParam(value = "id", defaultValue = "${myKey}") String id,
            @RequestHeader(defaultValue = "#{systemProperties.myHeader}") String header,
            @Value("#{request.requestPath.value()}") String path, RequestContext response) throws IOException {
      response.getWriter().write(id + "-" + header + "-" + path);
    }
  }

  @Controller
  static class NestedSetController {

    @RequestMapping("/myPath.do")
    public void myHandle(GenericBean<?> gb, RequestContext response) throws Exception {
      response.getWriter().write(gb.getTestBeanSet().toString() + "-" +
              gb.getTestBeanSet().iterator().next().getClass().getName());
    }
  }

  static class TestBeanConverter implements Converter<String, ITestBean> {

    @Override
    public ITestBean convert(String source) {
      return new TestBean(source);
    }
  }

  @Controller
  static class PathVariableWithCustomConverterController {

    @RequestMapping("/myPath/{id}")
    public void myHandle(@PathVariable("id") ITestBean bean) throws Exception {
    }
  }

  static class AnnotatedExceptionRaisingConverter implements Converter<String, ITestBean> {

    @Override
    public ITestBean convert(String source) {
      throw new NotFoundException();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @SuppressWarnings("serial")
    private static class NotFoundException extends RuntimeException {
    }
  }

  @Controller
  static class MethodNotAllowedController {

    @RequestMapping(value = "/myPath.do", method = HttpMethod.DELETE)
    public void delete() {
    }

    @RequestMapping(value = "/myPath.do", method = HttpMethod.HEAD)
    public void head() {
    }

    @RequestMapping(value = "/myPath.do", method = HttpMethod.OPTIONS)
    public void options() {
    }

    @RequestMapping(value = "/myPath.do", method = HttpMethod.POST)
    public void post() {
    }

    @RequestMapping(value = "/myPath.do", method = HttpMethod.PUT)
    public void put() {
    }

    @RequestMapping(value = "/myPath.do", method = HttpMethod.TRACE)
    public void trace() {
    }

    @RequestMapping(value = "/otherPath.do", method = HttpMethod.GET)
    public void get() {
    }
  }

  @Controller
  static class PathOrderingController {

    @RequestMapping(value = { "/dir/myPath1.do", "/*/*.do" })
    public void method1(Writer writer) throws IOException {
      writer.write("method1");
    }

    @RequestMapping("/dir/*.do")
    public void method2(Writer writer) throws IOException {
      writer.write("method2");
    }
  }

  @Controller
  static class RequestResponseBodyController {

    @RequestMapping(value = "/something", method = HttpMethod.PUT)
    @ResponseBody
    public String handle(@RequestBody String body) throws IOException {
      return body;
    }

    @RequestMapping(value = "/something", method = HttpMethod.PATCH)
    @ResponseBody
    public String handlePartialUpdate(@RequestBody String content) throws IOException {
      return content;
    }
  }

  @Controller
  static class RequestResponseBodyProducesController {

    @RequestMapping(value = "/something", method = HttpMethod.PUT, produces = "text/plain")
    @ResponseBody
    public String handle(@RequestBody String body) throws IOException {
      return body;
    }
  }

  @Controller
  static class ResponseBodyVoidController {

    @RequestMapping("/something")
    @ResponseBody
    public void handle() throws IOException {
    }
  }

  @Controller
  static class RequestBodyArgMismatchController {

    @RequestMapping(value = "/something", method = HttpMethod.PUT)
    public void handle(@RequestBody A a) throws IOException {
    }
  }

  @XmlRootElement
  static class A {
  }

  @XmlRootElement
  static class B {
  }

  static class NotReadableMessageConverter implements HttpMessageConverter<Object> {

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
      return true;
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
      return true;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
      return Collections.singletonList(new MediaType("application", "pdf"));
    }

    @Override
    public Object read(Class<?> clazz, HttpInputMessage inputMessage) {
      throw new HttpMessageNotReadableException("Could not read", inputMessage);
    }

    @Override
    public void write(Object o, @Nullable MediaType contentType, HttpOutputMessage outputMessage) {
      throw new UnsupportedOperationException("Not implemented");
    }
  }

  static class SimpleMessageConverter implements HttpMessageConverter<Object> {

    private final List<MediaType> supportedMediaTypes;

    public SimpleMessageConverter(MediaType... supportedMediaTypes) {
      this.supportedMediaTypes = Arrays.asList(supportedMediaTypes);
    }

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
      return supportedMediaTypes.contains(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
      return supportedMediaTypes.contains(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
      return supportedMediaTypes;
    }

    @Override
    public Object read(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
      return null;
    }

    @Override
    public void write(Object o, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
      outputMessage.getHeaders().setContentType(contentType);
      outputMessage.getBody(); // force a header write
    }
  }

  @Controller
  static class ContentTypeHeadersController {

    @RequestMapping(value = "/something", headers = "content-type=application/pdf")
    public void handlePdf(Writer writer) throws IOException {
      writer.write("pdf");
    }

    @RequestMapping(value = "/something", headers = "content-type=text/*")
    public void handleHtml(Writer writer) throws IOException {
      writer.write("text");
    }
  }

  @Controller
  static class ConsumesController {

    @RequestMapping(value = "/something", consumes = "application/pdf")
    public void handlePdf(Writer writer) throws IOException {
      writer.write("pdf");
    }

    @RequestMapping(value = "/something", consumes = "text/*")
    public void handleHtml(Writer writer) throws IOException {
      writer.write("text");
    }
  }

  @Controller
  static class NegatedContentTypeHeadersController {

    @RequestMapping(value = "/something", headers = "content-type=application/pdf")
    public void handlePdf(Writer writer) throws IOException {
      writer.write("pdf");
    }

    @RequestMapping(value = "/something", headers = "content-type!=application/pdf")
    public void handleNonPdf(Writer writer) throws IOException {
      writer.write("non-pdf");
    }

  }

  @Controller
  static class AcceptHeadersController {

    @RequestMapping(value = "/something", headers = "accept=text/html")
    public void handleHtml(Writer writer) throws IOException {
      writer.write("html");
    }

    @RequestMapping(value = "/something", headers = "accept=application/xml")
    public void handleXml(Writer writer) throws IOException {
      writer.write("xml");
    }
  }

  @Controller
  static class ProducesController {

    @GetMapping(path = "/something", produces = "text/html")
    public void handleHtml(Writer writer) throws IOException {
      writer.write("html");
    }

    @GetMapping(path = "/something", produces = "application/xml")
    public void handleXml(Writer writer) throws IOException {
      writer.write("xml");
    }

    @GetMapping(path = "/something", produces = "text/csv")
    public String handleCsv() {
      throw new IllegalArgumentException();
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handle(IllegalArgumentException ex) {
      return ResponseEntity.status(500)
              .body(Collections.singletonMap("reason", "error"));
    }
  }

  @Controller
  static class ResponseStatusController {

    @RequestMapping("/something")
    @ResponseStatus(code = HttpStatus.CREATED, reason = "It's alive!")
    public void handle(Writer writer) throws IOException {
      writer.write("something");
    }
  }

  @Controller
  static class ModelAndViewResolverController {

    @RequestMapping("/")
    public MySpecialArg handle() {
      return new MySpecialArg("foo");
    }
  }

  @Controller
  @RequestMapping("/test*")
  static class AmbiguousParamsController {

    @RequestMapping(method = HttpMethod.GET)
    public void noParams(Writer writer) throws IOException {
      writer.write("noParams");
    }

    @RequestMapping(params = "myParam")
    public void param(@RequestParam("myParam") int myParam, Writer writer) throws IOException {
      writer.write("myParam-" + myParam);
    }
  }

  @Controller
  static class AmbiguousPathAndHttpMethodController {

    @RequestMapping(value = "/bug/EXISTING", method = HttpMethod.POST)
    public void directMatch(Writer writer) throws IOException {
      writer.write("Direct");
    }

    @RequestMapping(value = "/bug/{type}", method = HttpMethod.GET)
    public void patternMatch(Writer writer) throws IOException {
      writer.write("Pattern");
    }
  }

  @Controller
  @RequestMapping("/test*")
  static class BindingCookieValueController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
      binder.initBeanPropertyAccess();
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      dateFormat.setLenient(false);
      binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
    }

    @RequestMapping(method = HttpMethod.GET)
    public void handle(@CookieValue("date") Date date, Writer writer) throws IOException {
      assertThat(date).as("Invalid path variable value").isEqualTo(new GregorianCalendar(2008, 10, 18).getTime());
      writer.write("test-" + new SimpleDateFormat("yyyy").format(date));
    }
  }

  interface TestController<T> {

    ModelAndView method(T object);
  }

  static class MyEntity {
  }

  @Controller
  static class TestControllerImpl implements TestController<MyEntity> {

    @Override
    @RequestMapping("/method")
    public ModelAndView method(MyEntity object) {
      return new ModelAndView("/something");
    }
  }

  @RestController
  @RequestMapping(path = ApiConstants.ARTICLES_PATH)
  static class ArticleController implements ApiConstants, ResourceEndpoint<Article, ArticlePredicate> {

    @Override
    @GetMapping(params = "page")
    public Collection<Article> find(String pageable, ArticlePredicate predicate) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    @GetMapping
    public List<Article> find(boolean sort, ArticlePredicate predicate) {
      throw new UnsupportedOperationException("not implemented");
    }
  }

  interface ApiConstants {

    String API_V1 = "/v1";

    String ARTICLES_PATH = API_V1 + "/articles";
  }

  interface ResourceEndpoint<E extends Entity, P extends EntityPredicate<?>> {

    Collection<E> find(String pageable, P predicate) throws IOException;

    List<E> find(boolean sort, P predicate) throws IOException;
  }

  static abstract class Entity {

    public UUID id;

    public String createdBy;

    public Instant createdDate;
  }

  static class Article extends Entity {

    public String slug;

    public String title;

    public String content;
  }

  static abstract class EntityPredicate<E extends Entity> {

    public String createdBy;

    public Instant createdBefore;

    public Instant createdAfter;

    public boolean accept(E entity) {
      return (createdBy == null || createdBy.equals(entity.createdBy)) &&
              (createdBefore == null || createdBefore.compareTo(entity.createdDate) >= 0) &&
              (createdAfter == null || createdAfter.compareTo(entity.createdDate) >= 0);
    }
  }

  static class ArticlePredicate extends EntityPredicate<Article> {

    public String query;

    @Override
    public boolean accept(Article entity) {
      return super.accept(entity) && (query == null || (entity.title.contains(query) || entity.content.contains(query)));
    }
  }

  @Controller
  static class RequestParamMapController {

    @RequestMapping("/map")
    public void map(@RequestParam Map<String, String> params, Writer writer) throws IOException {
      for (Iterator<Map.Entry<String, String>> it = params.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry<String, String> entry = it.next();
        writer.write(entry.getKey() + "=" + entry.getValue());
        if (it.hasNext()) {
          writer.write(',');
        }

      }
    }

    @RequestMapping("/multiValueMap")
    public void multiValueMap(@RequestParam MultiValueMap<String, String> params, Writer writer) throws IOException {
      for (Iterator<Map.Entry<String, List<String>>> it1 = params.entrySet().iterator(); it1.hasNext(); ) {
        Map.Entry<String, List<String>> entry = it1.next();
        writer.write(entry.getKey() + "=[");
        for (Iterator<String> it2 = entry.getValue().iterator(); it2.hasNext(); ) {
          String value = it2.next();
          writer.write(value);
          if (it2.hasNext()) {
            writer.write(',');
          }
        }
        writer.write(']');
        if (it1.hasNext()) {
          writer.write(',');
        }
      }
    }
  }

  @Controller
  static class RequestHeaderMapController {

    @RequestMapping("/map")
    public void map(@RequestHeader Map<String, String> headers, Writer writer) throws IOException {
      for (Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry<String, String> entry = it.next();
        writer.write(entry.getKey() + "=" + entry.getValue());
        if (it.hasNext()) {
          writer.write(',');
        }

      }
    }

    @RequestMapping("/multiValueMap")
    public void multiValueMap(@RequestHeader MultiValueMap<String, String> headers, Writer writer)
            throws IOException {
      for (Iterator<Map.Entry<String, List<String>>> it1 = headers.entrySet().iterator(); it1.hasNext(); ) {
        Map.Entry<String, List<String>> entry = it1.next();
        writer.write(entry.getKey() + "=[");
        for (Iterator<String> it2 = entry.getValue().iterator(); it2.hasNext(); ) {
          String value = it2.next();
          writer.write(value);
          if (it2.hasNext()) {
            writer.write(',');
          }
        }
        writer.write(']');
        if (it1.hasNext()) {
          writer.write(',');
        }
      }
    }

    @RequestMapping("/httpHeaders")
    public void httpHeaders(@RequestHeader HttpHeaders headers, Writer writer) throws IOException {
      assertThat(headers.getContentType()).as("Invalid Content-Type").isEqualTo(new MediaType("text", "html"));
      multiValueMap(headers, writer);
    }
  }

  @Controller
  interface IMyController {

    @RequestMapping("/handle")
    void handle(Writer writer, @RequestParam(value = "p", required = false) String param) throws IOException;
  }

  @Controller
  static class IMyControllerImpl implements IMyController {

    @Override
    public void handle(Writer writer, @RequestParam(value = "p", required = false) String param) throws IOException {
      writer.write("handle " + param);
    }
  }

  static abstract class MyAbstractController {

    @RequestMapping("/handle")
    public abstract void handle(Writer writer) throws IOException;
  }

  @Controller
  static class MyAbstractControllerImpl extends MyAbstractController {

    @Override
    public void handle(Writer writer) throws IOException {
      writer.write("handle");
    }
  }

  @Controller
  static class TrailingSlashController {

    @RequestMapping(value = "/", method = HttpMethod.GET)
    public void root(Writer writer) throws IOException {
      writer.write("root");
    }

    @RequestMapping(value = "/{templatePath}/", method = HttpMethod.GET)
    public void templatePath(Writer writer) throws IOException {
      writer.write("templatePath");
    }
  }

  @Controller
  static class ResponseEntityController {

    @PostMapping("/foo")
    public ResponseEntity<String> foo(HttpEntity<byte[]> requestEntity) {
      assertThat(requestEntity).isNotNull();
      assertThat(requestEntity.getHeaders().getFirst("MyRequestHeader")).isEqualTo("MyValue");

      String body = new String(requestEntity.getBody(), StandardCharsets.UTF_8);
      assertThat(body).isEqualTo("Hello World");

      URI location = URI.create("/foo");
      return ResponseEntity.created(location)
              .header("MyResponseHeader", "MyValue")
              .body(body);
    }

    @GetMapping("/bar")
    public ResponseEntity<Void> bar() {
      return ResponseEntity.notFound()
              .header("MyResponseHeader", "MyValue")
              .build();
    }

    @GetMapping("/baz")
    public ResponseEntity<String> baz() {
      return ResponseEntity.ok()
              .header("MyResponseHeader", "MyValue")
              .body("body");
    }

    @RequestMapping(path = "/stores", method = HttpMethod.HEAD)
    public ResponseEntity<Void> headResource() {
      return ResponseEntity.ok()
              .header("h1", "v1")
              .build();
    }

    @GetMapping("/stores")
    public ResponseEntity<String> getResource() {
      return ResponseEntity.ok().body("body");
    }

    @GetMapping("/test-entity")
    public ResponseEntity<TestEntity> testEntity() {
      TestEntity entity = new TestEntity();
      entity.setName("Foo Bar");
      return ResponseEntity.ok()
              .contentType(MediaType.APPLICATION_XML)
              .body(entity);
    }
  }

  @XmlRootElement
  static class TestEntity {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @Controller
  static class CustomMapEditorController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
      binder.initBeanPropertyAccess();
      binder.registerCustomEditor(Map.class, new CustomMapEditor());
    }

    @SuppressWarnings("rawtypes")
    @RequestMapping("/handle")
    public void handle(@RequestParam("map") Map map, Writer writer) throws IOException {
      writer.write("test-" + map);
    }
  }

  static class CustomMapEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
      if (StringUtils.hasText(text)) {
        setValue(Collections.singletonMap("foo", text));
      }
      else {
        setValue(null);
      }
    }
  }

  @Controller
  static class MultipartController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
      binder.registerCustomEditor(String.class, new StringMultipartFileEditor());
    }

    @RequestMapping("/singleString")
    public void processMultipart(@RequestParam("content") String content, RequestContext response)
            throws IOException {
      response.getWriter().write(content);
    }

    @RequestMapping("/stringArray")
    public void processMultipart(@RequestParam("content") String[] content, RequestContext response)
            throws IOException {
      response.getWriter().write(StringUtils.arrayToDelimitedString(content, "-"));
    }
  }

  @Controller
  static class CsvController {

    @RequestMapping("/singleInteger")
    public void processCsv(@RequestParam("content") Integer content, RequestContext response) throws IOException {
      response.getWriter().write(content.toString());
    }

    @RequestMapping("/integerArray")
    public void processCsv(@RequestParam("content") Integer[] content, RequestContext response) throws IOException {
      response.getWriter().write(StringUtils.arrayToDelimitedString(content, "-"));
    }
  }

  @Controller
  @RequestMapping("/t1")
  protected static class NoPathGetAndM2PostController {

    @RequestMapping(method = HttpMethod.GET)
    public void handle1(Writer writer) throws IOException {
      writer.write("handle1");
    }

    @RequestMapping(value = "/m2", method = HttpMethod.POST)
    public void handle2(Writer writer) throws IOException {
      writer.write("handle2");
    }
  }

  @Controller
  static class HeadersConditionController {

    @RequestMapping(value = "/", method = HttpMethod.GET)
    public String home() {
      return "home";
    }

    @RequestMapping(value = "/", method = HttpMethod.GET, headers = "Accept=application/json")
    @ResponseBody
    public String homeJson() {
      return "homeJson";
    }
  }

  @Controller
  @Configuration(proxyBeanMethods = false)
  @EnableWebSession
  static class RedirectAttributesController extends AbstractSessionManagerAutowired {

    public RedirectAttributesController(SessionManager sessionManager) {
      super(sessionManager);
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
      dataBinder.setRequiredFields("name");
      saveSession();
    }

    @GetMapping("/messages/{id}")
    public void message(ModelMap model, Writer writer) throws IOException {
      writer.write("Got: " + model.get("successMessage"));
      saveSession();
    }

    @PostMapping("/messages")
    public String sendMessage(TestBean testBean, BindingResult result, Model model, RedirectModel attributes) {
      if (result.hasErrors()) {
        return "messages/new";
      }
      model.addAttribute("id", "1")
              .addAttribute("name", "value");
      attributes.addAttribute("successMessage", "yay!");
      return "redirect:/messages/{id}";
    }

    @PostMapping("/messages-response-entity")
    public ResponseEntity<Void> sendMessage(RedirectModel attributes) {
      attributes.addAttribute("successMessage", "yay!");
      URI location = URI.create("/messages/1?name=value");
      saveSession();
      return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }
  }

  @Controller
  static class PrototypeController {

    private int count;

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
      this.count++;
    }

    @ModelAttribute
    public void populate(Model model) {
      this.count++;
    }

    @RequestMapping("/")
    public void message(int param, Writer writer) throws IOException {
      this.count++;
      writer.write("count:" + this.count);
    }
  }

  @RestController
  static class ThisWillActuallyRun {

    @RequestMapping(value = "/", method = HttpMethod.GET)
    public String home() {
      return "Hello World!";
    }
  }

  @Controller
  static class HttpHeadersResponseController {

    @RequestMapping(value = "/", method = HttpMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public HttpHeaders create() {
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setLocation(URI.create("/test/items/123"));
      return headers;
    }

    @RequestMapping(value = "empty", method = HttpMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public HttpHeaders createNoHeader() {
      return HttpHeaders.forWritable();
    }
  }

  @RestController
  static class TextRestController {

    @RequestMapping(path = "/a1", method = HttpMethod.GET)
    public String a1(@RequestBody String body) {
      return body;
    }

    @RequestMapping(path = "/a2.html", method = HttpMethod.GET)
    public String a2(@RequestBody String body) {
      return body;
    }

    @RequestMapping(path = "/a3", method = HttpMethod.GET, produces = "text/html")
    public String a3(@RequestBody String body) throws IOException {
      return body;
    }

    @RequestMapping(path = "/a4", method = HttpMethod.GET)
    public String a4(@RequestBody String body) {
      return body;
    }
  }

  @Controller
  static class ModelAndViewController {

    @RequestMapping("/path")
    public ModelAndView methodWithHttpStatus(MyEntity object) {
      return new ModelAndView("view", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @RequestMapping("/redirect")
    public ModelAndView methodWithHttpStatusForRedirect(MyEntity object) {
      return new ModelAndView("redirect:/path", HttpStatus.TEMPORARY_REDIRECT);
    }

    @RequestMapping("/exception")
    public void raiseException() throws Exception {
      throw new TestException();
    }

    @ExceptionHandler(TestException.class)
    public ModelAndView handleException() {
      return new ModelAndView("view", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @SuppressWarnings("serial")
    private static class TestException extends Exception {
    }
  }

  static class DataClass {

    @NotNull
    private final String param1;

    private final boolean param2;

    private int param3;

    public DataClass(String param1, @BindParam("param2") boolean p2, Optional<Integer> optionalParam) {
      this.param1 = param1;
      this.param2 = p2;
      Assert.notNull(optionalParam, "Optional is required");
      optionalParam.ifPresent(integer -> this.param3 = integer);
    }

    public String param1() {
      return param1;
    }

    public boolean param2() {
      return param2;
    }

    public void setParam3(int param3) {
      this.param3 = param3;
    }

    public int getParam3() {
      return param3;
    }
  }

  @RestController
  static class DataClassController {

    @RequestMapping("/bind")
    public String handle(DataClass data) {
      return data.param1 + "-" + data.param2 + "-" + data.param3;
    }
  }

  @RestController
  static class PathVariableDataClassController {

    @RequestMapping("/bind/{param2}")
    public String handle(DataClass data) {
      return data.param1 + "-" + data.param2 + "-" + data.param3;
    }
  }

  @RestController
  static class ValidatedDataClassController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
      binder.registerCustomEditor(String.class, "param1", new StringTrimmerEditor(true));
      LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
      vf.afterPropertiesSet();
      binder.setValidator(vf);
    }

    @RequestMapping("/bind")
    public BindStatusView handle(@Valid DataClass data, BindingResult result) {
      assertThat(data).isNotNull();
      if (result.hasErrors()) {
        return new BindStatusView(result.getErrorCount() + ":" + result.getFieldValue("param1") + "-" +
                result.getFieldValue("param2") + "-" + result.getFieldValue("param3"));
      }
      return new BindStatusView(data.param1 + "-" + data.param2 + "-" + data.param3);
    }
  }

  static class BindStatusView extends AbstractView {

    private final String content;

    BindStatusView(String content) {
      this.content = content;
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws Exception {
//      rc.getBindStatus("dataClass");
//      rc.getBindStatus("dataClass.param1");
//      rc.getBindStatus("dataClass.param2");
//      rc.getBindStatus("dataClass.param3");
      request.getWriter().write(this.content);
    }
  }

  static class MultipartFileDataClass {

    @NotNull
    public final MultipartFile param1;

    public final boolean param2;

    public int param3;

    public MultipartFileDataClass(MultipartFile param1,
            @BindParam("param2") boolean p2, Optional<Integer> optionalParam) {
      this.param1 = param1;
      this.param2 = p2;
      Assert.notNull(optionalParam, "Optional is required");
      optionalParam.ifPresent(integer -> this.param3 = integer);
    }

    public void setParam3(int param3) {
      this.param3 = param3;
    }
  }

  @RestController
  static class MultipartFileDataClassController {

    @RequestMapping("/bind")
    public String handle(MultipartFileDataClass data) throws IOException {
      return StreamUtils.copyToString(data.param1.getInputStream(), StandardCharsets.UTF_8) +
              "-" + data.param2 + "-" + data.param3;
    }
  }

  static class PartDataClass {

    @NotNull
    public final Part param1;

    public final boolean param2;

    public int param3;

    public PartDataClass(Part param1,
            @BindParam("param2") boolean p2, Optional<Integer> optionalParam) {
      this.param1 = param1;
      this.param2 = p2;
      Assert.notNull(optionalParam, "Optional is required");
      optionalParam.ifPresent(integer -> this.param3 = integer);
    }

    public void setParam3(int param3) {
      this.param3 = param3;
    }
  }

  @RestController
  static class MockPartDataClassController {

    @RequestMapping("/bind")
    public String handle(PartDataClass data) throws IOException {
      return StreamUtils.copyToString(data.param1.getInputStream(), StandardCharsets.UTF_8) +
              "-" + data.param2 + "-" + data.param3;
    }
  }

  @RestController
  static class NullableDataClassController {

    @RequestMapping("/bind")
    public String handle(@Nullable DataClass data, BindingResult result) {
      if (result.hasErrors()) {
        assertThat(data).isNull();
        return result.getFieldValue("param1") + "-" + result.getFieldValue("param2") + "-" +
                result.getFieldValue("param3");
      }
      assertThat(data).isNotNull();
      return data.param1 + "-" + data.param2 + "-" + data.param3;
    }
  }

  @RestController
  static class OptionalDataClassController {

    @RequestMapping("/bind")
    public String handle(Optional<DataClass> optionalData, BindingResult result) {
      if (result.hasErrors()) {
        assertThat(optionalData).isNotNull();
        assertThat(optionalData).isNotPresent();
        return result.getFieldValue("param1") + "-" + result.getFieldValue("param2") + "-" +
                result.getFieldValue("param3");
      }
      return optionalData.map(data -> data.param1 + "-" + data.param2 + "-" + data.param3).orElse("");
    }
  }

  static class DateClass {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    public LocalDate date;

    public DateClass(LocalDate date) {
      this.date = date;
    }
  }

  @RestController
  static class DateClassController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
      binder.initDirectFieldAccess();
    }

    @RequestMapping("/bind")
    public String handle(DateClass data, BindingResult result) {
      if (result.hasErrors()) {
        return result.getFieldError().toString();
      }
      assertThat(data).isNotNull();
      assertThat(data.date).isNotNull();
      assertThat(data.date.getYear()).isEqualTo(2010);
      assertThat(data.date.getMonthValue()).isEqualTo(1);
      assertThat(data.date.getDayOfMonth()).isEqualTo(1);
      return result.getFieldValue("date").toString();
    }
  }

  static record DataRecord(String param1, boolean param2, int param3) {
  }

  @RestController
  static class DataRecordController {

    @RequestMapping("/bind")
    public String handle(DataRecord data) {
      return data.param1 + "-" + data.param2 + "-" + data.param3;
    }
  }

  static class NestedDataClass {

    @NotNull
    private final String param1;

    @Valid
    private final DataClass nestedParam2;

    public NestedDataClass(@NotNull String param1, DataClass nestedParam2) {
      this.param1 = param1;
      this.nestedParam2 = nestedParam2;
    }

    public String getParam1() {
      return this.param1;
    }

    public DataClass getNestedParam2() {
      return this.nestedParam2;
    }
  }

  @RestController
  static class NestedDataClassController {

    @RequestMapping("/bind")
    public String handle(NestedDataClass data) {
      DataClass nestedParam2 = data.nestedParam2;
      return (data.param1 + "-" + nestedParam2.param1 + "-" + nestedParam2.param2 + "-" + nestedParam2.param3);
    }
  }

  @RestController
  static class NestedValidatedDataClassController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
//      binder.setConversionService(new DefaultFormattingConversionService());
      binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
      LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
      vf.afterPropertiesSet();
      binder.setValidator(vf);
    }

    @RequestMapping("/bind")
    public NestedBindStatusView handle(@Valid NestedDataClass data, BindingResult result) {
      assertThat(data).isNotNull();
      if (result.hasErrors()) {
        String content = result.getErrorCount() + ":" + result.getFieldValue("param1");
        content += "-" + result.getFieldValue("nestedParam2.param1");
        content += "-" + result.getFieldValue("nestedParam2.param2");
        content += "-" + result.getFieldValue("nestedParam2.param3");
        return new NestedBindStatusView(content);
      }
      DataClass nested = data.nestedParam2;
      return new NestedBindStatusView(
              data.param1 + "-" + nested.param1 + "-" + nested.param2 + "-" + nested.param3);
    }
  }

  static class NestedBindStatusView extends AbstractView {

    private final String content;

    NestedBindStatusView(String content) {
      this.content = content;
    }

    @Override
    protected void renderMergedOutputModel(
            Map<String, Object> model, RequestContext request) throws Exception {
//      RequestContext rc = new RequestContext(request, model);
//      rc.getBindStatus("nestedDataClass");
//      rc.getBindStatus("nestedDataClass.param1");
//      rc.getBindStatus("nestedDataClass.nestedParam2");
//      rc.getBindStatus("nestedDataClass.nestedParam2.param1");
//      rc.getBindStatus("nestedDataClass.nestedParam2.param2");
//      rc.getBindStatus("nestedDataClass.nestedParam2.param3");
      request.getWriter().write(this.content);
    }
  }

  static class NestedDataAndDateClass {

    @NotNull
    private final String param1;

    @Valid
    private final DataClass nestedParam2;

    @Valid
    private final DateClass nestedParam3;

    public NestedDataAndDateClass(
            @NotNull String param1, DataClass nestedParam2, DateClass nestedParam3) {

      this.param1 = param1;
      this.nestedParam2 = nestedParam2;
      this.nestedParam3 = nestedParam3;
    }

    public String getParam1() {
      return this.param1;
    }

    public DataClass getNestedParam2() {
      return this.nestedParam2;
    }

    public DateClass getNestedParam3() {
      return this.nestedParam3;
    }
  }

  @RestController
  static class NestedDataAndDateClassController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
      binder.initDirectFieldAccess();
    }

    @RequestMapping("/bind")
    public String handle(NestedDataAndDateClass data, BindingResult result) {
      if (result.hasErrors()) {
        return result.getFieldError().toString();
      }
      assertThat(data).isNotNull();
      assertThat(data.getParam1()).isEqualTo("value1");
      assertThat(data.getNestedParam2().param1).isEqualTo("nestedValue1");
      assertThat(data.getNestedParam2().param2).isTrue();
      assertThat(data.getNestedParam2().param3).isEqualTo(8);
      assertThat(data.getNestedParam3().date).isNotNull();
      assertThat(data.getNestedParam3().date.getYear()).isEqualTo(2010);
      assertThat(data.getNestedParam3().date.getMonthValue()).isEqualTo(1);
      assertThat(data.getNestedParam3().date.getDayOfMonth()).isEqualTo(1);
      return result.getFieldValue("nestedParam3.date").toString();
    }
  }

}
