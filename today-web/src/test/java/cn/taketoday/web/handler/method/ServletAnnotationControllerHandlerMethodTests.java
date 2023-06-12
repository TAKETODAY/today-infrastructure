/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.Test;

import java.beans.ConstructorProperties;
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

import cn.taketoday.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import cn.taketoday.aop.interceptor.SimpleTraceInterceptor;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.propertyeditors.CustomDateEditor;
import cn.taketoday.beans.propertyeditors.StringTrimmerEditor;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.format.annotation.DateTimeFormat;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.format.support.FormattingConversionServiceFactoryBean;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import cn.taketoday.http.converter.xml.MarshallingHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.oxm.jaxb.Jaxb2Marshaller;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.ui.ExtendedModelMap;
import cn.taketoday.ui.Model;
import cn.taketoday.ui.ModelMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.FieldError;
import cn.taketoday.validation.beanvalidation.LocalValidatorFactoryBean;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.SerializationTestUtils;
import cn.taketoday.web.accept.ContentNegotiationManagerFactoryBean;
import cn.taketoday.web.annotation.CookieValue;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.PostMapping;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestHeader;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.annotation.InitBinder;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.annotation.SessionAttributes;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.bind.support.ConfigurableWebBindingInitializer;
import cn.taketoday.web.bind.support.WebBindingInitializer;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.function.RouterFunction;
import cn.taketoday.web.handler.function.RouterFunctions;
import cn.taketoday.web.handler.function.ServerResponse;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.support.StringMultipartFileEditor;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.servlet.view.InternalResourceViewResolver;
import cn.taketoday.web.testfixture.beans.DerivedTestBean;
import cn.taketoday.web.testfixture.beans.GenericBean;
import cn.taketoday.web.testfixture.beans.ITestBean;
import cn.taketoday.web.testfixture.beans.TestBean;
import cn.taketoday.web.testfixture.security.TestPrincipal;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockMultipartFile;
import cn.taketoday.web.testfixture.servlet.MockMultipartHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockPart;
import cn.taketoday.web.testfixture.servlet.MockServletConfig;
import cn.taketoday.web.testfixture.servlet.MockServletContext;
import cn.taketoday.web.view.AbstractView;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.RedirectModel;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.ViewResolver;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
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
class ServletAnnotationControllerHandlerMethodTests extends AbstractServletHandlerMethodTests {

  @Test
  void emptyValueMapping() throws Exception {
    initDispatcherServlet(ControllerWithEmptyValueMapping.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
    request.setContextPath("/foo");
    request.setServletPath("");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test");
  }

  @Test
  void errorThrownFromHandlerMethod() throws Exception {
    initDispatcherServlet(ControllerWithErrorThrown.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
    request.setContextPath("/foo");
    request.setServletPath("");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test");
  }

  @Test
  void customAnnotationController() throws Exception {
    initDispatcherServlet(CustomAnnotationController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpServletResponse.SC_OK);
  }

  @Test
  void requiredParamMissing() throws Exception {
    WebApplicationContext webAppContext = initDispatcherServlet(RequiredParamController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    assertThat(webAppContext.isSingleton(RequiredParamController.class.getSimpleName())).isTrue();
  }

  @Test
  void typeConversionError() throws Exception {
    initDispatcherServlet(RequiredParamController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    request.addParameter("id", "foo");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  void optionalParamPresent() throws Exception {
    initDispatcherServlet(OptionalParamController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    request.addParameter("id", "val");
    request.addParameter("flag", "true");
    request.addHeader("header", "otherVal");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("val-true-otherVal");
  }

  @Test
  void optionalParamMissing() throws Exception {
    initDispatcherServlet(OptionalParamController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("null-false-null");
  }

  @Test
  void defaultParameters() throws Exception {
    initDispatcherServlet(DefaultValueParamController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("foo--bar");
  }

  @Test
  void defaultExpressionParameters() throws Exception {
    initDispatcherServlet(DefaultExpressionValueParamController.class, wac -> {
      RootBeanDefinition ppc = new RootBeanDefinition(PropertySourcesPlaceholderConfigurer.class);
      ppc.getPropertyValues().add("properties", "myKey=foo");
      wac.registerBeanDefinition("ppc", ppc);
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myApp/myPath.do");
    request.setContextPath("/myApp");
    MockHttpServletResponse response = new MockHttpServletResponse();
    System.setProperty("myHeader", "bar");
    try {
      getServlet().service(request, response);
    }
    finally {
      System.clearProperty("myHeader");
    }
    assertThat(response.getContentAsString()).isEqualTo("foo-bar-/myApp");
  }

  @Test
  void typeNestedSetBinding() throws Exception {
    initDispatcherServlet(NestedSetController.class, wac -> {
      RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
      csDef.getPropertyValues().add("converters", new TestBeanConverter());
      RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
      wbiDef.getPropertyValues().add("conversionService", csDef);
      RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
      adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
      wac.registerBeanDefinition("handlerAdapter", adapterDef);
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    request.addParameter("testBeanSet", "1", "2");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("[1, 2]-cn.taketoday.web.testfixture.beans.TestBean");
  }

  @Test
  void pathVariableWithCustomConverter() throws Exception {
    initDispatcherServlet(PathVariableWithCustomConverterController.class, wac -> {
      RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
      csDef.getPropertyValues().add("converters", new AnnotatedExceptionRaisingConverter());
      RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
      wbiDef.getPropertyValues().add("conversionService", csDef);
      RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
      adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
      wac.registerBeanDefinition("handlerAdapter", adapterDef);
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath/1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void methodNotAllowed() throws Exception {
    initDispatcherServlet(MethodNotAllowedController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).as("Invalid response status").isEqualTo(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    String allowHeader = response.getHeader("Allow");
    assertThat(allowHeader).as("No Allow header").isNotNull();
    Set<String> allowedMethods = new HashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(allowHeader, ", ")));
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
    initDispatcherServlet(EmptyParameterListHandlerMethodController.class, wac -> {
      RootBeanDefinition vrDef = new RootBeanDefinition(InternalResourceViewResolver.class);
      vrDef.getPropertyValues().add("suffix", ".jsp");
      wac.registerBeanDefinition("viewResolver", vrDef);
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/emptyParameterListHandler");
    MockHttpServletResponse response = new MockHttpServletResponse();

    EmptyParameterListHandlerMethodController.called = false;
    getServlet().service(request, response);
    assertThat(EmptyParameterListHandlerMethodController.called).isTrue();
    assertThat(response.getContentAsString()).isEmpty();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  void sessionAttributeExposure() throws Exception {
    initDispatcherServlet(
            MySessionAttributesController.class,
            wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class))
    );

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page1");
    HttpSession session = request.getSession();
    assertThat(session).isNotNull();
    assertThat(session.getAttribute("object1")).isNotNull();
    assertThat(session.getAttribute("object2")).isNotNull();
    assertThat(((Map) session.getAttribute("model"))).containsKey("object1");
    assertThat(((Map) session.getAttribute("model"))).containsKey("object2");

    request = new MockHttpServletRequest("POST", "/myPage");
    request.setSession(session);
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page2");
    assertThat(session.getAttribute("object1")).isNotNull();
    assertThat(session.getAttribute("object2")).isNotNull();
    assertThat(((Map) session.getAttribute("model"))).containsKey("object1");
    assertThat(((Map) session.getAttribute("model"))).containsKey("object2");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  void sessionAttributeExposureWithInterface() throws Exception {
    initDispatcherServlet(MySessionAttributesControllerImpl.class, wac -> {
      wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class));
      DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
      autoProxyCreator.setBeanFactory(wac.getBeanFactory());
      autoProxyCreator.setProxyTargetClass(true);
      wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
      wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page1");
    HttpSession session = request.getSession();
    assertThat(session).isNotNull();
    assertThat(session.getAttribute("object1")).isNotNull();
    assertThat(session.getAttribute("object2")).isNotNull();
    assertThat(((Map) session.getAttribute("model"))).containsKey("object1");
    assertThat(((Map) session.getAttribute("model"))).containsKey("object2");

    request = new MockHttpServletRequest("POST", "/myPage");
    request.setSession(session);
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page2");
    assertThat(session.getAttribute("object1")).isNotNull();
    assertThat(session.getAttribute("object2")).isNotNull();
    assertThat(((Map) session.getAttribute("model"))).containsKey("object1");
    assertThat(((Map) session.getAttribute("model"))).containsKey("object2");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  void parameterizedAnnotatedInterface() throws Exception {
    initDispatcherServlet(
            MyParameterizedControllerImpl.class,
            wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class))
    );

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page1");
    HttpSession session = request.getSession();
    assertThat(session).isNotNull();
    assertThat(session.getAttribute("object1")).isNotNull();
    assertThat(session.getAttribute("object2")).isNotNull();
    assertThat(((Map) session.getAttribute("model"))).containsKey("object1");
    assertThat(((Map) session.getAttribute("model"))).containsKey("object2");
    assertThat(((Map) session.getAttribute("model"))).containsKey("testBeanList");

    request = new MockHttpServletRequest("POST", "/myPage");
    request.setSession(session);
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page2");
    assertThat(session.getAttribute("object1")).isNotNull();
    assertThat(session.getAttribute("object2")).isNotNull();
    assertThat(((Map) session.getAttribute("model"))).containsKey("object1");
    assertThat(((Map) session.getAttribute("model"))).containsKey("object2");
    assertThat(((Map) session.getAttribute("model"))).containsKey("testBeanList");
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void parameterizedAnnotatedInterfaceWithOverriddenMappingsInImpl() throws Exception {
    initDispatcherServlet(
            MyParameterizedControllerImplWithOverriddenMappings.class,
            wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class))
    );

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page1");
    HttpSession session = request.getSession();
    assertThat(session).isNotNull();
    assertThat(session.getAttribute("object1")).isNotNull();
    assertThat(session.getAttribute("object2")).isNotNull();
    assertThat(((Map) session.getAttribute("model"))).containsKey("object1");
    assertThat(((Map) session.getAttribute("model"))).containsKey("object2");
    assertThat(((Map) session.getAttribute("model"))).containsKey("testBeanList");

    request = new MockHttpServletRequest("POST", "/myPage");
    request.setSession(session);
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(request.getAttribute("viewName")).isEqualTo("page2");
    assertThat(session.getAttribute("object1")).isNotNull();
    assertThat(session.getAttribute("object2")).isNotNull();
    assertThat(((Map) session.getAttribute("model"))).containsKey("object1");
    assertThat(((Map) session.getAttribute("model"))).containsKey("object2");
    assertThat(((Map) session.getAttribute("model"))).containsKey("testBeanList");
  }

  @Test
  void adaptedHandleMethods() throws Exception {
    initDispatcherServlet(MyAdaptedController.class);
    doTestAdaptedHandleMethods();
  }

  @Test
  void adaptedHandleMethods2() throws Exception {
    initDispatcherServlet(MyAdaptedController2.class);
  }

  @Test
  void adaptedHandleMethods3() throws Exception {
    initDispatcherServlet(MyAdaptedController3.class);
    doTestAdaptedHandleMethods();
  }

  private void doTestAdaptedHandleMethods() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath1.do");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addParameter("param1", "value1");
    request.addParameter("param2", "2");
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test");

    request = new MockHttpServletRequest("GET", "/myPath2.do");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "2");
    request.addHeader("header1", "10");
    request.setCookies(new Cookie("cookie1", "3"));
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test-value1-2-10-3");

    request = new MockHttpServletRequest("GET", "/myPath3.do");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "2");
    request.addParameter("name", "name1");
    request.addParameter("age", "2");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);

    request = new MockHttpServletRequest("GET", "/myPath4.do");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "2");
    request.addParameter("name", "name1");
    request.addParameter("age", "value2");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test-name1-typeMismatch");
  }

  @Test
  void formController() throws Exception {
    initDispatcherServlet(
            MyFormController.class,
            wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class))
    );

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    request.addParameter("name", "name1");
    request.addParameter("age", "value2");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-name1-typeMismatch-tb1-myValue");
  }

  @Test
  void modelFormController() throws Exception {
    initDispatcherServlet(
            MyModelFormController.class,
            wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class))
    );

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    request.addParameter("name", "name1");
    request.addParameter("age", "value2");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("view-name-name1-typeMismatch-tb1-myValue");
  }

  @Test
  void lateBindingFormController() throws Exception {
    initDispatcherServlet(
            LateBindingFormController.class,
            wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class))
    );

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    request.addParameter("name", "name1");
    request.addParameter("age", "value2");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-name1-typeMismatch-tb1-myValue");
  }

  @Test
  void proxiedFormController() throws Exception {
    initDispatcherServlet(MyFormController.class, wac -> {
      wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
      DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
      autoProxyCreator.setBeanFactory(wac.getBeanFactory());
      autoProxyCreator.setProxyTargetClass(true);
      wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
      wac.registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    request.addParameter("name", "name1");
    request.addParameter("age", "value2");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-name1-typeMismatch-tb1-myValue");
  }

  @Test
  void commandProvidingFormControllerWithCustomEditor() throws Exception {
    initDispatcherServlet(MyCommandProvidingFormController.class, wac -> {
      wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
      RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
      adapterDef.getPropertyValues().add("webBindingInitializer", new MyWebBindingInitializer());
      wac.registerBeanDefinition("handlerAdapter", adapterDef);
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    request.addParameter("defaultName", "myDefaultName");
    request.addParameter("age", "value2");
    request.addParameter("date", "2007-10-02");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue");
  }

  @Test
  void typedCommandProvidingFormController() throws Exception {
    initDispatcherServlet(MyTypedCommandProvidingFormController.class, wac -> {
      wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));

      wac.registerSingleton(new MyWebBindingInitializer());
      wac.registerSingleton(new MySpecialArgumentResolver());
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
    request.addParameter("defaultName", "10");
    request.addParameter("age", "value2");
    request.addParameter("date", "2007-10-02");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-Integer:10-typeMismatch-tb1-myOriginalValue");

    request = new MockHttpServletRequest("GET", "/myOtherPath.do");
    request.addParameter("defaultName", "10");
    request.addParameter("age", "value2");
    request.addParameter("date", "2007-10-02");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-myName-typeMismatch-tb1-myOriginalValue");

    request = new MockHttpServletRequest("GET", "/myThirdPath.do");
    request.addParameter("defaultName", "10");
    request.addParameter("age", "100");
    request.addParameter("date", "2007-10-02");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView-special-99-special-99");
  }

  @Test
  void binderInitializingCommandProvidingFormController() throws Exception {
    initDispatcherServlet(MyBinderInitializingCommandProvidingFormController.class,
            wac -> wac.registerBean("viewResolver", TestViewResolver.class)
    );

    var request = new MockHttpServletRequest("GET", "/myPath.do");
    request.addParameter("defaultName", "myDefaultName");
    request.addParameter("age", "value2");
    request.addParameter("date", "2007-10-02");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString())
            .isEqualTo("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue");
  }

  @Test
  void specificBinderInitializingCommandProvidingFormController() throws Exception {
    initDispatcherServlet(MySpecificBinderInitializingCommandProvidingFormController.class,
            wac -> wac.registerBeanDefinition("viewResolver",
                    new RootBeanDefinition(TestViewResolver.class))
    );

    var request = new MockHttpServletRequest("GET", "/myPath.do");
    request.addParameter("defaultName", "myDefaultName");
    request.addParameter("age", "value2");
    request.addParameter("date", "2007-10-02");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString())
            .isEqualTo("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue");
  }

  @Test
  void parameterDispatchingController() throws Exception {
    final MockServletContext servletContext = new MockServletContext();
    final MockServletConfig servletConfig = new MockServletConfig(servletContext);

    WebApplicationContext webAppContext =
            initDispatcherServlet(MyParameterDispatchingController.class, wac -> {
              wac.setServletContext(servletContext);
              AnnotationConfigUtils.registerAnnotationConfigProcessors(wac);
              wac.getBeanFactory().registerDependency(ServletConfig.class, servletConfig);
            });

    MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
    MockHttpServletResponse response = new MockHttpServletResponse();
    HttpSession session = request.getSession();
    assertThat(session).isNotNull();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");
    assertThat(request.getAttribute("servletContext")).isSameAs(servletContext);
    assertThat(request.getAttribute("servletConfig")).isSameAs(servletConfig);
    assertThat(request.getAttribute("sessionId")).isSameAs(session.getId());
    assertThat(request.getAttribute("requestUri")).isSameAs(request.getRequestURI());
    assertThat(request.getAttribute("locale")).isSameAs(request.getLocale());

    request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
    response = new MockHttpServletResponse();
    session = request.getSession();
    assertThat(session).isNotNull();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");
    assertThat(request.getAttribute("servletContext")).isSameAs(servletContext);
    assertThat(request.getAttribute("servletConfig")).isSameAs(servletConfig);
    assertThat(request.getAttribute("sessionId")).isSameAs(session.getId());
    assertThat(request.getAttribute("requestUri")).isSameAs(request.getRequestURI());

    request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
    request.addParameter("view", "other");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myOtherView");

    request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
    request.addParameter("view", "my");
    request.addParameter("lang", "de");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myLangView");

    request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
    request.addParameter("surprise", "!");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("mySurpriseView");

    MyParameterDispatchingController deserialized =
            SerializationTestUtils.serializeAndDeserialize(webAppContext.getBean(
                    MyParameterDispatchingController.class.getSimpleName(), MyParameterDispatchingController.class));
    assertThat(deserialized.request).isNotNull();
    assertThat(deserialized.session).isNotNull();
  }

  @Test
  void relativePathDispatchingController() throws Exception {
    initDispatcherServlet(MyRelativePathDispatchingController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myApp/myHandle");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");

    request = new MockHttpServletRequest("GET", "/myApp/myOther");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myOtherView");

    request = new MockHttpServletRequest("GET", "/myApp/myLang");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myLangView");

    request = new MockHttpServletRequest("GET", "/myApp/surprise.do");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");
  }

  @Test
  void relativeMethodPathDispatchingController() throws Exception {
    initDispatcherServlet(MyRelativeMethodPathDispatchingController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myApp/myHandle");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");

    request = new MockHttpServletRequest("GET", "/yourApp/myOther");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myOtherView");

    request = new MockHttpServletRequest("GET", "/hisApp/myLang");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myLangView");

    request = new MockHttpServletRequest("GET", "/herApp/surprise.do");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus())
            .as("Suffixes pattern matching should not work with PathPattern's")
            .isEqualTo(404);
  }

  @Test
  void nullCommandController() throws Exception {
    initDispatcherServlet(MyNullCommandController.class);
    getServlet().init(new MockServletConfig());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath");
    request.setUserPrincipal(new OtherPrincipal());
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myView");
  }

  @Test
  void equivalentMappingsWithSameMethodName() {
    assertThatThrownBy(() -> initDispatcherServlet(ChildController.class))
            .isInstanceOf(BeanCreationException.class)
            .hasCauseInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ambiguous mapping");
  }

  @Test
    // gh-22543
  void unmappedPathMapping() throws Exception {
    initDispatcherServlet(UnmappedPathController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bogus-unmapped");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(404);

    request = new MockHttpServletRequest("GET", "");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("get");
  }

  @Test
  void explicitAndEmptyPathsControllerMapping() throws Exception {
    initDispatcherServlet(ExplicitAndEmptyPathsController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("get");

    request = new MockHttpServletRequest("GET", "");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("get");
  }

  @Test
  void pathOrdering() throws Exception {
    initDispatcherServlet(PathOrderingController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dir/myPath1.do");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("method1");
  }

  @Test
  void requestBodyResponseBody() throws Exception {
    initDispatcherServlet(RequestResponseBodyController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "text/*, */*");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo(requestBody);
  }

  @Test
  void httpPatch() throws Exception {
    initDispatcherServlet(RequestResponseBodyController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/something");
    String requestBody = "Hello world!";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "text/*, */*");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo(requestBody);
  }

  @Test
  void responseBodyNoAcceptableMediaType() throws Exception {
    initDispatcherServlet(RequestResponseBodyProducesController.class, wac -> {
      wac.registerSingleton(new StringHttpMessageConverter());
    });

    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "application/pdf, application/msword");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(406);
  }

  @Test
  void responseBodyWildCardMediaType() throws Exception {
    initDispatcherServlet(RequestResponseBodyController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "*/*");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo(requestBody);
  }

  @Test
  void unsupportedRequestBody() throws Exception {
    initDispatcherServlet(RequestResponseBodyController.class, wac -> {
      StringHttpMessageConverter converter = new StringHttpMessageConverter();
      converter.setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_PLAIN));

      wac.registerSingleton(new HttpMessageConverters(false, List.of(converter)));
    });

    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "application/pdf");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(415);
    assertThat(response.getHeader("Accept")).isEqualTo("text/plain");
  }

  @Test
  void unsupportedPatchBody() throws Exception {
    initDispatcherServlet(RequestResponseBodyController.class, wac -> {
      StringHttpMessageConverter converter = new StringHttpMessageConverter();
      converter.setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_PLAIN));
      wac.registerSingleton(new HttpMessageConverters(false, List.of(converter)));
    });

    MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "application/pdf");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(415);
    assertThat(response.getHeader("Accept-Patch")).isEqualTo("text/plain");
  }

  @Test
  void responseBodyNoAcceptHeader() throws Exception {
    initDispatcherServlet(RequestResponseBodyController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo(requestBody);
  }

  @Test
  void badRequestRequestBody() throws Exception {
    initDispatcherServlet(RequestResponseBodyController.class, wac -> {
      RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
      wac.registerBeanDefinition("handlerAdapter", adapterDef);

      register("parameterResolvingRegistry", ParameterResolvingRegistry.class, wac)
              .getPropertyValues().add("messageConverters", new NotReadableMessageConverter());

      register("returnValueHandlerManager", ReturnValueHandlerManager.class, wac)
              .getPropertyValues().add("messageConverters", new NotReadableMessageConverter()); ;
    });

    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "application/pdf");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  void httpEntity() throws Exception {
    initDispatcherServlet(ResponseEntityController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/foo");
    String requestBody = "Hello World";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "text/*, */*");
    request.addHeader("MyRequestHeader", "MyValue");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getContentAsString()).isEqualTo(requestBody);
    assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");

    request = new MockHttpServletRequest("GET", "/bar");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void httpEntityWithContentType() throws Exception {
    initDispatcherServlet(ResponseEntityController.class, wac -> {
      List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
      messageConverters.add(new MappingJackson2HttpMessageConverter());
      messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
      HttpMessageConverters converters = new HttpMessageConverters(messageConverters);
      wac.registerSingleton("messageConverters", converters);
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test-entity");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("Content-Type")).isEqualTo("application/xml");
    assertThat(response.getContentAsString()).isEqualTo(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<testEntity><name>Foo Bar</name></testEntity>");
  }

  @Test
  void overlappingMessageConvertersRequestBody() throws Exception {
    initDispatcherServlet(RequestResponseBodyController.class, wac -> {
      List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
      messageConverters.add(new StringHttpMessageConverter());
      messageConverters.add(new SimpleMessageConverter(new MediaType("application", "json"), MediaType.ALL));

      HttpMessageConverters converters = new HttpMessageConverters(messageConverters);
      wac.registerSingleton("messageConverters", converters);
    });

    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
    request.setContent("Hello World".getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "text/plain; charset=utf-8");
    request.addHeader("Accept", "application/json, text/javascript, */*");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getHeader("Content-Type")).as("Invalid content-type").isEqualTo("application/json");
  }

  @Test
  void responseBodyVoid() throws Exception {
    initDispatcherServlet(ResponseBodyVoidController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "text/*, */*");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void responseBodyArgMismatch() throws Exception {
    initDispatcherServlet(RequestBodyArgMismatchController.class, wac -> {
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

    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
    String requestBody = "<b/>";
    request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Type", "application/xml; charset=utf-8");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  void contentTypeHeaders() throws Exception {
    initDispatcherServlet(ContentTypeHeadersController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/something");
    request.setContentType("application/pdf");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("pdf");

    request = new MockHttpServletRequest("POST", "/something");
    request.setContentType("text/html");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("text");

    request = new MockHttpServletRequest("POST", "/something");
    request.setContentType("application/xml");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(415);
  }

  @Test
  void consumes() throws Exception {
    initDispatcherServlet(ConsumesController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/something");
    request.setContentType("application/pdf");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("pdf");

    request = new MockHttpServletRequest("POST", "/something");
    request.setContentType("text/html");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("text");

    request = new MockHttpServletRequest("POST", "/something");
    request.setContentType("application/xml");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(415);
  }

  @Test
  void negatedContentTypeHeaders() throws Exception {
    initDispatcherServlet(NegatedContentTypeHeadersController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/something");
    request.setContentType("application/pdf");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("pdf");

    request = new MockHttpServletRequest("POST", "/something");
    request.setContentType("text/html");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("non-pdf");
  }

  @Test
  void acceptHeaders() throws Exception {
    initDispatcherServlet(AcceptHeadersController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "text/html");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("html");

    request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "application/xml");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "application/xml, text/html");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "text/html;q=0.9, application/xml");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "application/msword");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(406);
  }

  @Test
  void produces() throws Exception {
    initDispatcherServlet(ProducesController.class, wac -> {
      wac.registerSingleton(new MappingJackson2HttpMessageConverter());
      wac.registerSingleton(new Jaxb2RootElementHttpMessageConverter());
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "text/html");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("html");

    request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "application/xml");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "application/xml, text/html");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "text/html;q=0.9, application/xml");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("xml");

    request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "application/msword");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(406);

    request = new MockHttpServletRequest("GET", "/something");
    request.addHeader("Accept", "text/csv,application/problem+json");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getContentAsString()).isEqualTo("{\"reason\":\"error\"}");
  }

  @Test
  void responseStatus() throws Exception {
    initDispatcherServlet(ResponseStatusController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/something");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("something");
    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getErrorMessage()).isEqualTo("It's alive!");
  }

  @Test
  void bindingCookieValue() throws Exception {
    initDispatcherServlet(BindingCookieValueController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
    request.setCookies(new Cookie("date", "2008-11-18"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test-2008");
  }

  @Test
  void ambiguousParams() throws Exception {
    initDispatcherServlet(AmbiguousParamsController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("noParams");

    request = new MockHttpServletRequest("GET", "/test");
    request.addParameter("myParam", "42");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("myParam-42");
  }

  @Test
  void ambiguousPathAndHttpMethod() throws Exception {
    initDispatcherServlet(AmbiguousPathAndHttpMethodController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bug/EXISTING");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("Pattern");
  }

  @Test
  void bridgeMethods() throws Exception {
    initDispatcherServlet(TestControllerImpl.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/method");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
  }

  @Test
  void bridgeMethodsWithMultipleInterfaces() throws Exception {
    initDispatcherServlet(ArticleController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/method");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
  }

  @Test
  void requestParamMap() throws Exception {
    initDispatcherServlet(RequestParamMapController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/map");
    request.addParameter("key1", "value1");
    request.addParameter("key2", "value21", "value22");
    MockHttpServletResponse response = new MockHttpServletResponse();

    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("key1=value1,key2=value21");

    request.setRequestURI("/multiValueMap");
    response = new MockHttpServletResponse();

    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("key1=[value1],key2=[value21,value22]");
  }

  @Test
  void requestHeaderMap() throws Exception {
    initDispatcherServlet(RequestHeaderMapController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/map");
    request.addHeader("Content-Type", "text/html");
    request.addHeader("Custom-Header", new String[] { "value21", "value22" });
    MockHttpServletResponse response = new MockHttpServletResponse();

    getServlet().service(request, response);
    assertThat(response.getContentAsString())
            .isEqualTo("Content-Type=text/html,Custom-Header=value21");

    request.setRequestURI("/multiValueMap");
    response = new MockHttpServletResponse();

    getServlet().service(request, response);
    assertThat(response.getContentAsString())
            .isEqualTo("Content-Type=[text/html],Custom-Header=[value21,value22]");

    request.setRequestURI("/httpHeaders");
    response = new MockHttpServletResponse();

    getServlet().service(request, response);
    assertThat(response.getContentAsString())
            .isEqualTo("Content-Type=[text/html],Custom-Header=[value21,value22]");
  }

  @Test
  void requestMappingInterface() throws Exception {
    initDispatcherServlet(IMyControllerImpl.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("handle null");

    request = new MockHttpServletRequest("GET", "/handle");
    request.addParameter("p", "value");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("handle value");
  }

  @Test
  void requestMappingInterfaceWithProxy() throws Exception {
    initDispatcherServlet(IMyControllerImpl.class, wac -> {
      DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
      autoProxyCreator.setBeanFactory(wac.getBeanFactory());
      autoProxyCreator.setProxyTargetClass(true);

      wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
      wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("handle null");

    request = new MockHttpServletRequest("GET", "/handle");
    request.addParameter("p", "value");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("handle value");
  }

  @Test
  void requestMappingBaseClass() throws Exception {
    initDispatcherServlet(MyAbstractControllerImpl.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("handle");

  }

  @Test
  void trailingSlash() throws Exception {
    initDispatcherServlet(TrailingSlashController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo/");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("templatePath");
  }

  /*
   * See SPR-6021
   */
  @Test
  void customMapEditor() throws Exception {
    initDispatcherServlet(CustomMapEditorController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
    request.addParameter("map", "bar");
    MockHttpServletResponse response = new MockHttpServletResponse();

    getServlet().service(request, response);

    assertThat(response.getContentAsString()).isEqualTo("test-{foo=bar}");
  }

  @Test
  void multipartFileAsSingleString() throws Exception {
    initDispatcherServlet(MultipartController.class);

    MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
    request.setRequestURI("/singleString");
    request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen");
  }

  @Test
  void regularParameterAsSingleString() throws Exception {
    initDispatcherServlet(MultipartController.class);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/singleString");
    request.setMethod("POST");
    request.addParameter("content", "Juergen");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen");
  }

  @Test
  void multipartFileAsStringArray() throws Exception {
    initDispatcherServlet(MultipartController.class);

    MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
    request.setRequestURI("/stringArray");
    request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen");
  }

  @Test
  void regularParameterAsStringArray() throws Exception {
    initDispatcherServlet(MultipartController.class);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/stringArray");
    request.setMethod("POST");
    request.addParameter("content", "Juergen");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen");
  }

  @Test
  void multipartFilesAsStringArray() throws Exception {
    initDispatcherServlet(MultipartController.class);

    MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
    request.setRequestURI("/stringArray");
    request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
    request.addFile(new MockMultipartFile("content", "Eva".getBytes()));
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen-Eva");
  }

  @Test
  void regularParametersAsStringArray() throws Exception {
    initDispatcherServlet(MultipartController.class);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/stringArray");
    request.setMethod("POST");
    request.addParameter("content", "Juergen");
    request.addParameter("content", "Eva");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Juergen-Eva");
  }

  @Test
  void parameterCsvAsStringArray() throws Exception {
    initDispatcherServlet(CsvController.class, wac -> {
      RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
      RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
      wbiDef.getPropertyValues().add("conversionService", csDef);
      RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
      adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
      wac.registerBeanDefinition("handlerAdapter", adapterDef);
    });

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/integerArray");
    request.setMethod("POST");
    request.addParameter("content", "1,2");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("1-2");
  }

  @Test
  void testMatchWithoutMethodLevelPath() throws Exception {
    initDispatcherServlet(NoPathGetAndM2PostController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/t1/m2");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(405);
  }

  @Test
  void testHeadersCondition() throws Exception {
    initDispatcherServlet(HeadersConditionController.class);

    // No "Accept" header
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getForwardedUrl()).isEqualTo("home");

    // Accept "*/*"
    request = new MockHttpServletRequest("GET", "/");
    request.addHeader("Accept", "*/*");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getForwardedUrl()).isEqualTo("home");

    // Accept "application/json"
    request = new MockHttpServletRequest("GET", "/");
    request.addHeader("Accept", "application/json");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
    assertThat(response.getContentAsString()).isEqualTo("homeJson");
  }

  @Test
  void redirectAttribute() throws Exception {
    WebApplicationContext wac = initDispatcherServlet(RedirectAttributesController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/messages");
    HttpSession session = request.getSession();
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);

    ServletRequestContext context = new ServletRequestContext(wac, request, response);
    // POST -> bind error
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getForwardedUrl()).isEqualTo("messages/new");
    assertThat(RequestContextUtils.getOutputRedirectModel(context).isEmpty()).isTrue();

    // POST -> success
    request = new MockHttpServletRequest("POST", "/messages");
    request.setSession(session);
    request.addParameter("name", "Jeff");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(302);
    assertThat(response.getRedirectedUrl()).isEqualTo("/messages/1?name=value");
    assertThat(RequestContextUtils.getOutputRedirectModel(context).get("successMessage")).isEqualTo("yay!");

    // GET after POST
    request = new MockHttpServletRequest("GET", "/messages/1");
    request.setQueryString("name=value");
    request.setSession(session);
    response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("Got: yay!");
    assertThat(RequestContextUtils.getOutputRedirectModel(context).isEmpty()).isTrue();
  }

  @Test
  void flashAttributesWithResponseEntity() throws Exception {
    WebApplicationContext wac = initDispatcherServlet(RedirectAttributesController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/messages-response-entity");
    MockHttpServletResponse response = new MockHttpServletResponse();
    HttpSession session = request.getSession();

    getServlet().service(request, response);
    ServletRequestContext context = new ServletRequestContext(wac, request, response);

    assertThat(response.getStatus()).isEqualTo(302);
    assertThat(response.getRedirectedUrl()).isEqualTo("/messages/1?name=value");
    assertThat(RequestContextUtils.getOutputRedirectModel(context).get("successMessage")).isEqualTo("yay!");

    // GET after POST
    request = new MockHttpServletRequest("GET", "/messages/1");
    request.setQueryString("name=value");
    request.setSession(session);
    response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("Got: yay!");
    assertThat(RequestContextUtils.getOutputRedirectModel(context).isEmpty()).isTrue();
  }

  @Test
  void prototypeController() throws Exception {
    initDispatcherServlet(null, wac -> {
      RootBeanDefinition beanDef = new RootBeanDefinition(PrototypeController.class);
      beanDef.setScope(BeanDefinition.SCOPE_PROTOTYPE);
      wac.registerBeanDefinition("controller", beanDef);
    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    request.addParameter("param", "1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getContentAsString()).isEqualTo("count:3");

    response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getContentAsString()).isEqualTo("count:3");
  }

  @Test
  void restController() throws Exception {
    initDispatcherServlet(ThisWillActuallyRun.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("Hello World!");
  }

  @Test
  void responseAsHttpHeaders() throws Exception {
    initDispatcherServlet(HttpHeadersResponseController.class);
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(new MockHttpServletRequest("POST", "/"), response);

    assertThat(response.getStatus()).as("Wrong status code").isEqualTo(MockHttpServletResponse.SC_CREATED);
    assertThat(response.getHeaderNames().size()).as("Wrong number of headers").isEqualTo(1);
    assertThat(response.getHeader("location")).as("Wrong value for 'location' header").isEqualTo("/test/items/123");
    assertThat(response.getContentLength()).as("Expected an empty content").isEqualTo(0);
  }

  @Test
  void responseAsHttpHeadersNoHeader() throws Exception {
    initDispatcherServlet(HttpHeadersResponseController.class);
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(new MockHttpServletRequest("POST", "/empty"), response);

    assertThat(response.getStatus()).as("Wrong status code").isEqualTo(MockHttpServletResponse.SC_CREATED);
    assertThat(response.getHeaderNames().size()).as("Wrong number of headers").isEqualTo(0);
    assertThat(response.getContentLength()).as("Expected an empty content").isEqualTo(0);
  }

  @Test
  void responseBodyAsHtml() throws Exception {
    initDispatcherServlet(TextRestController.class);

    byte[] content = "alert('boo')".getBytes(StandardCharsets.ISO_8859_1);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a1.html");
    request.setContent(content);
    MockHttpServletResponse response = new MockHttpServletResponse();

    getServlet().service(request, response);

    assertThat(response.getStatus())
            .as("Suffixes pattern matching should not work with PathPattern's")
            .isEqualTo(404);
  }

  @Test
  void responseBodyAsHtmlWithSuffixPresent() throws Exception {
    initDispatcherServlet(TextRestController.class, wac -> {
      ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();
      factoryBean.setFavorPathExtension(true);
      factoryBean.afterPropertiesSet();

      wac.registerSingleton("mvcContentNegotiationManager", factoryBean.getObject());
    });

    byte[] content = "alert('boo')".getBytes(StandardCharsets.ISO_8859_1);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a2.html");
    request.setContent(content);
    MockHttpServletResponse response = new MockHttpServletResponse();

    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
    assertThat(response.getHeader("Content-Disposition")).isNull();
    assertThat(response.getContentAsByteArray()).isEqualTo(content);
  }

  @Test
  void responseBodyAsHtmlWithProducesCondition() throws Exception {
    initDispatcherServlet(TextRestController.class);

    byte[] content = "alert('boo')".getBytes(StandardCharsets.ISO_8859_1);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a3.html");
    request.setContent(content);
    MockHttpServletResponse response = new MockHttpServletResponse();

    getServlet().service(request, response);

    assertThat(response.getStatus())
            .as("Suffixes pattern matching should not work with PathPattern's")
            .isEqualTo(404);
  }

  @Test
  void responseBodyAsTextWithCssExtension() throws Exception {
    initDispatcherServlet(TextRestController.class, wac -> {
      ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();
      factoryBean.setFavorParameter(true);
      factoryBean.addMediaType("css", MediaType.parseMediaType("text/css"));
      factoryBean.afterPropertiesSet();

      wac.registerSingleton("mvcContentNegotiationManager", factoryBean.getObject());
    });

    byte[] content = "body".getBytes(StandardCharsets.ISO_8859_1);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a4");
    request.addParameter("format", "css");
    request.setContent(content);
    MockHttpServletResponse response = new MockHttpServletResponse();

    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentType()).isEqualTo("text/css;charset=UTF-8");
    assertThat(response.getHeader("Content-Disposition")).isNull();
    assertThat(response.getContentAsByteArray()).isEqualTo(content);
  }

  @Test
  void modelAndViewWithStatus() throws Exception {
    initDispatcherServlet(ModelAndViewController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(response.getForwardedUrl()).isEqualTo("view");
  }

  @Test
  void modelAndViewWithStatusForRedirect() throws Exception {
    initDispatcherServlet(ModelAndViewController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/redirect");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(307);
    assertThat(response.getRedirectedUrl()).isEqualTo("/path");
  }

  @Test
  void modelAndViewWithStatusInExceptionHandler() throws Exception {
    initDispatcherServlet(ModelAndViewController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/exception");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(response.getForwardedUrl()).isEqualTo("view");
  }

  @Test
  void httpHead() throws Exception {
    initDispatcherServlet(ResponseEntityController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/baz");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");
    assertThat(response.getContentLength()).isEqualTo(4);
    assertThat(response.getContentAsByteArray().length).isEqualTo(0);

    // Now repeat with GET
    request = new MockHttpServletRequest("GET", "/baz");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");
    assertThat(response.getContentLength()).isEqualTo(4);
    assertThat(response.getContentAsString()).isEqualTo("body");
  }

  @Test
  void httpHeadExplicit() throws Exception {
    initDispatcherServlet(ResponseEntityController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/stores");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("h1")).isEqualTo("v1");
  }

  @Test
  void httpOptions() throws Exception {
    initDispatcherServlet(ResponseEntityController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/baz");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
    assertThat(response.getContentAsByteArray().length).isEqualTo(0);
  }

  @Test
  void dataClassBinding() throws Exception {
    initDispatcherServlet(DataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithPathVariable() throws Exception {
    initDispatcherServlet(PathVariableDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind/true");
    request.addParameter("param1", "value1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithMultipartFile() throws Exception {
    initDispatcherServlet(MultipartFileDataClassController.class);

    MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
    request.setRequestURI("/bind");
    request.addFile(new MockMultipartFile("param1", "value1".getBytes(StandardCharsets.UTF_8)));
    request.addParameter("param2", "true");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithServletPart() throws Exception {
    initDispatcherServlet(ServletPartDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/bind");
    request.setContentType("multipart/form-data");
    request.addPart(new MockPart("param1", "value1".getBytes(StandardCharsets.UTF_8)));
    request.addParameter("param2", "true");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithAdditionalSetter() throws Exception {
    initDispatcherServlet(DataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("param3", "3");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
  }

  @Test
  void dataClassBindingWithResult() throws Exception {
    initDispatcherServlet(ValidatedDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", " value1");
    request.addParameter("param2", "true");
    request.addParameter("param3", "3");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
  }

  @Test
  void dataClassBindingWithOptionalParameter() throws Exception {
    initDispatcherServlet(ValidatedDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", " value1");
    request.addParameter("param2", "true");
    request.addParameter("optionalParam", "8");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-8");
  }

  @Test
  void dataClassBindingWithMissingParameter() throws Exception {
    initDispatcherServlet(ValidatedDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", " value1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("1:value1-null-null");
  }

  @Test
  void dataClassBindingWithConversionError() throws Exception {
    initDispatcherServlet(ValidatedDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", " value1");
    request.addParameter("param2", "x");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("1:value1-x-null");
  }

  @Test
  void dataClassBindingWithValidationError() throws Exception {
    initDispatcherServlet(ValidatedDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param2", "true");
    request.addParameter("param3", "0");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("1:-true-0");
  }

  @Test
  void dataClassBindingWithValidationErrorAndConversionError() throws Exception {
    initDispatcherServlet(ValidatedDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param2", "x");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("2:null-x-null");
  }

  @Test
  void dataClassBindingWithNullable() throws Exception {
    initDispatcherServlet(NullableDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("param3", "3");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
  }

  @Test
  void dataClassBindingWithNullableAndConversionError() throws Exception {
    initDispatcherServlet(NullableDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "x");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-x-null");
  }

  @Test
  void dataClassBindingWithOptional() throws Exception {
    initDispatcherServlet(OptionalDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("param3", "3");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
  }

  @Test
  void dataClassBindingWithOptionalAndConversionError() throws Exception {
    initDispatcherServlet(OptionalDataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "x");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-x-null");
  }

  @Test
  void dataClassBindingWithFieldMarker() throws Exception {
    initDispatcherServlet(DataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("_param2", "on");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithFieldMarkerFallback() throws Exception {
    initDispatcherServlet(DataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("_param2", "on");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-false-0");
  }

  @Test
  void dataClassBindingWithFieldDefault() throws Exception {
    initDispatcherServlet(DataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("!param2", "false");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
  }

  @Test
  void dataClassBindingWithFieldDefaultFallback() throws Exception {
    initDispatcherServlet(DataClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("!param2", "false");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-false-0");
  }

  @Test
  void dataClassBindingWithLocalDate() throws Exception {
    initDispatcherServlet(DateClassController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("date", "2010-01-01");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("2010-01-01");
  }

  @Test
  void dataRecordBinding() throws Exception {
    initDispatcherServlet(DataRecordController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
    request.addParameter("param1", "value1");
    request.addParameter("param2", "true");
    request.addParameter("param3", "3");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
  }

  @Test
  void routerFunction() throws ServletException, IOException {
    initDispatcherServlet(wac -> {
      wac.registerBean(RouterFunction.class, () ->
              RouterFunctions.route()
                      .GET("/foo", request -> ServerResponse.ok().body("foo-body"))
                      .build());

    });

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("foo-body");
  }

  @Controller
  static class ControllerWithEmptyValueMapping {

    @RequestMapping("")
    public void myPath2(HttpServletResponse response) {
      throw new IllegalStateException("test");
    }

    @RequestMapping("/bar")
    public void myPath3(HttpServletResponse response) throws IOException {
      response.getWriter().write("testX");
    }

    @ExceptionHandler
    public void myPath2(Exception ex, HttpServletResponse response) throws IOException {
      response.getWriter().write(ex.getMessage());
    }
  }

  @Controller
  private static class ControllerWithErrorThrown {

    @RequestMapping("")
    public void myPath2(HttpServletResponse response) {
      throw new AssertionError("test");
    }

    @RequestMapping("/bar")
    public void myPath3(HttpServletResponse response) throws IOException {
      response.getWriter().write("testX");
    }

    @ExceptionHandler
    public void myPath2(Error err, HttpServletResponse response) throws IOException {
      response.getWriter().write(err.getMessage());
    }
  }

  @Controller
  static class MyAdaptedController {

    @RequestMapping("/myPath1.do")
    public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException {
      response.getWriter().write("test");
    }

    @RequestMapping("/myPath2.do")
    public void myHandle(@RequestParam("param1") String p1, @RequestParam("param2") int p2,
            @RequestHeader("header1") long h1, @CookieValue(name = "cookie1") Cookie c1,
            HttpServletResponse response) throws IOException {
      response.getWriter().write("test-" + p1 + "-" + p2 + "-" + h1 + "-" + c1.getValue());
    }

    @RequestMapping("/myPath3")
    public void myHandle(TestBean tb, HttpServletResponse response) throws IOException {
      response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
    }

    @RequestMapping("/myPath4.do")
    public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException {
      response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
    }
  }

  @Controller
  @RequestMapping("/*.do")
  static class MyAdaptedController2 {

    @RequestMapping
    public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException {
      response.getWriter().write("test");
    }

    @RequestMapping("/myPath2.do")
    public void myHandle(@RequestParam("param1") String p1, int param2, HttpServletResponse response,
            @RequestHeader("header1") String h1, @CookieValue("cookie1") String c1) throws IOException {
      response.getWriter().write("test-" + p1 + "-" + param2 + "-" + h1 + "-" + c1);
    }

    @RequestMapping("/myPath3")
    public void myHandle(TestBean tb, HttpServletResponse response) throws IOException {
      response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
    }

    @RequestMapping("/myPath4.*")
    public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException {
      response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
    }
  }

  @Controller
  static class MyAdaptedControllerBase<T> {

    @RequestMapping("/myPath2.do")
    public void myHandle(@RequestParam("param1") T p1, int param2, @RequestHeader Integer header1,
            @CookieValue int cookie1, HttpServletResponse response) throws IOException {
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
    public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException {
      response.getWriter().write("test");
    }

    @Override
    public void myHandle(@RequestParam("param1") String p1, int param2, @RequestHeader Integer header1,
            @CookieValue int cookie1, HttpServletResponse response) throws IOException {
      response.getWriter().write("test-" + p1 + "-" + param2 + "-" + header1 + "-" + cookie1);
    }

    @RequestMapping("/myPath3")
    public void myHandle(TestBean tb, HttpServletResponse response) throws IOException {
      response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
    }

    @RequestMapping("/myPath4.*")
    public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException {
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
    public void nonEmptyParameterListHandler(HttpServletResponse response) {
    }
  }

  @Controller
  @RequestMapping("/myPage")
  @SessionAttributes(names = { "object1", "object2" })
  static class MySessionAttributesController {

    @RequestMapping(method = HttpMethod.GET)
    public String get(Model model) {
      model.addAttribute("object1", new Object());
      model.addAttribute("object2", new Object());
      return "page1";
    }

    @RequestMapping(method = HttpMethod.POST)
    public String post(@ModelAttribute("object1") Object object1) {
      //do something with object1
      return "page2";

    }
  }

  @RequestMapping("/myPage")
  @SessionAttributes({ "object1", "object2" })
  @Controller
  interface MySessionAttributesControllerIfc {

    @RequestMapping(method = HttpMethod.GET)
    String get(Model model);

    @RequestMapping(method = HttpMethod.POST)
    String post(@ModelAttribute("object1") Object object1);
  }

  static class MySessionAttributesControllerImpl implements MySessionAttributesControllerIfc {

    @Override
    public String get(Model model) {
      model.addAttribute("object1", new Object());
      model.addAttribute("object2", new Object());
      return "page1";
    }

    @Override
    public String post(@ModelAttribute("object1") Object object1) {
      //do something with object1
      return "page2";
    }
  }

  @RequestMapping("/myPage")
  @SessionAttributes({ "object1", "object2" })
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
  static class MyParameterizedControllerImpl implements MyEditableParameterizedControllerIfc<TestBean> {

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
      return "page1";
    }

    @Override
    public String post(TestBean object) {
      //do something with object1
      return "page2";
    }
  }

  @Controller
  static class MyParameterizedControllerImplWithOverriddenMappings
          implements MyEditableParameterizedControllerIfc<TestBean> {

    @Override
    @ModelAttribute("testBeanList")
    public List<TestBean> getTestBeans() {
      List<TestBean> list = new ArrayList<>();
      list.add(new TestBean("tb1"));
      list.add(new TestBean("tb2"));
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
    public String myOtherHandle(TB tb, BindingResult errors, ExtendedModelMap model, MySpecialArg arg) {
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

    @Autowired
    private transient ServletContext servletContext;

    @Autowired
    private transient ServletConfig servletConfig;

    @Autowired
    private HttpSession session;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private RequestContext webRequest;

    @RequestMapping
    public void myHandle(HttpServletResponse response, HttpServletRequest request) throws IOException {
      if (this.servletContext == null || this.servletConfig == null || this.session == null ||
              this.request == null || this.webRequest == null) {
        throw new IllegalStateException();
      }
      response.getWriter().write("myView");
      request.setAttribute("servletContext", this.servletContext);
      request.setAttribute("servletConfig", this.servletConfig);
      request.setAttribute("sessionId", this.session.getId());
      request.setAttribute("requestUri", this.request.getRequestURI());
      request.setAttribute("locale", this.webRequest.getLocale());
    }

    @RequestMapping(params = { "view", "!lang" })
    public void myOtherHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("myOtherView");
    }

    @RequestMapping(method = HttpMethod.GET, params = { "view=my", "lang=de" })
    public void myLangHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("myLangView");
    }

    @RequestMapping(method = { HttpMethod.POST, HttpMethod.GET }, params = "surprise")
    public void mySurpriseHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("mySurpriseView");
    }
  }

  @Controller
  @RequestMapping(value = "/myPath.do", params = { "active" })
  static class MyConstrainedParameterDispatchingController {

    @RequestMapping(params = { "view", "!lang" })
    public void myOtherHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("myOtherView");
    }

    @RequestMapping(method = HttpMethod.GET, params = { "view=my", "lang=de" })
    public void myLangHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("myLangView");
    }
  }

  @Controller
  @RequestMapping("/myApp/*")
  static class MyRelativePathDispatchingController {

    @RequestMapping
    public void myHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("myView");
    }

    @RequestMapping("*Other")
    public void myOtherHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("myOtherView");
    }

    @RequestMapping("myLang")
    public void myLangHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("myLangView");
    }

    @RequestMapping("surprise")
    public void mySurpriseHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("mySurpriseView");
    }
  }

  @Controller
  static class MyRelativeMethodPathDispatchingController {

    @RequestMapping("*/myHandle") // was **/myHandle
    public void myHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("myView");
    }

    @RequestMapping("/*/*Other") // was /**/*Other
    public void myOtherHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("myOtherView");
    }

    @RequestMapping("*/myLang") // was **/myLang
    public void myLangHandle(HttpServletResponse response) throws IOException {
      response.getWriter().write("myLangView");
    }

    @RequestMapping("/*/surprise") // was /**/surprise
    public void mySurpriseHandle(HttpServletResponse response) throws IOException {
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

    @Override
    public View resolveViewName(final String viewName, Locale locale) throws Exception {
      return (model, context) -> {
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
          context.getWriter()
                  .write(viewName + "-" + tb.getName() + "-" + errors.getFieldError("age").getCode() +
                          "-" + testBeans.get(0).getName() + "-" + model.get("myKey") +
                          (model.containsKey("yourKey") ? "-" + model.get("yourKey") : ""));
        }
        else {
          context.getWriter().write(viewName + "-" + tb.getName() + "-" + tb.getAge() + "-" +
                  errors.getFieldValue("name") + "-" + errors.getFieldValue("age"));
        }
      };
    }
  }

  static class ModelExposingViewResolver implements ViewResolver {

    @Override
    public View resolveViewName(String viewName, Locale locale) {
      return (model, request) -> {
        request.setAttribute("viewName", viewName);
//        request.getSession().setAttribute("model", model);
      };
    }
  }

  static class ParentController {

    @RequestMapping(method = HttpMethod.GET)
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
    }
  }

  @Controller
  @RequestMapping("/child/test")
  static class ChildController extends ParentController {

    @RequestMapping(method = HttpMethod.GET)
    public void doGet(HttpServletRequest req, HttpServletResponse resp, @RequestParam("childId") String id) {
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
            HttpServletResponse response) throws IOException {
      response.getWriter().write(String.valueOf(id) + "-" + flag + "-" + String.valueOf(header));
    }
  }

  @Controller
  static class DefaultValueParamController {

    @RequestMapping("/myPath.do")
    public void myHandle(@RequestParam(value = "id", defaultValue = "foo") String id,
            @RequestParam(value = "otherId", defaultValue = "") String id2,
            @RequestHeader(defaultValue = "bar") String header,
            HttpServletResponse response) throws IOException {
      response.getWriter().write(String.valueOf(id) + "-" + String.valueOf(id2) + "-" + String.valueOf(header));
    }
  }

  @Controller
  static class DefaultExpressionValueParamController {

    @RequestMapping("/myPath.do")
    public void myHandle(@RequestParam(value = "id", defaultValue = "${myKey}") String id,
            @RequestHeader(defaultValue = "#{systemProperties.myHeader}") String header,
            @Value("#{request.contextPath}") String contextPath,
            HttpServletResponse response) throws IOException {
      response.getWriter().write(String.valueOf(id) + "-" + String.valueOf(header) + "-" + contextPath);
    }
  }

  @Controller
  static class NestedSetController {

    @RequestMapping("/myPath.do")
    public void myHandle(GenericBean<?> gb, HttpServletResponse response) throws Exception {
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
    public void processMultipart(@RequestParam("content") String content, HttpServletResponse response)
            throws IOException {
      response.getWriter().write(content);
    }

    @RequestMapping("/stringArray")
    public void processMultipart(@RequestParam("content") String[] content, HttpServletResponse response)
            throws IOException {
      response.getWriter().write(StringUtils.arrayToDelimitedString(content, "-"));
    }
  }

  @Controller
  static class CsvController {

    @RequestMapping("/singleInteger")
    public void processCsv(@RequestParam("content") Integer content, HttpServletResponse response) throws IOException {
      response.getWriter().write(content.toString());
    }

    @RequestMapping("/integerArray")
    public void processCsv(@RequestParam("content") Integer[] content, HttpServletResponse response) throws IOException {
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
  static class RedirectAttributesController {

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
      dataBinder.setRequiredFields("name");
    }

    @GetMapping("/messages/{id}")
    public void message(ModelMap model, Writer writer) throws IOException {
      writer.write("Got: " + model.get("successMessage"));
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
      HttpHeaders headers = HttpHeaders.create();
      headers.setLocation(URI.create("/test/items/123"));
      return headers;
    }

    @RequestMapping(value = "empty", method = HttpMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public HttpHeaders createNoHeader() {
      return HttpHeaders.create();
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

    @ConstructorProperties({ "param1", "param2", "optionalParam" })
    public DataClass(String param1, boolean p2, Optional<Integer> optionalParam) {
      this.param1 = param1;
      this.param2 = p2;
      Assert.notNull(optionalParam, "Optional must not be null");
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
      binder.setConversionService(new DefaultFormattingConversionService());
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

    @ConstructorProperties({ "param1", "param2", "optionalParam" })
    public MultipartFileDataClass(MultipartFile param1, boolean p2, Optional<Integer> optionalParam) {
      this.param1 = param1;
      this.param2 = p2;
      Assert.notNull(optionalParam, "Optional must not be null");
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

  static class ServletPartDataClass {

    @NotNull
    public final Part param1;

    public final boolean param2;

    public int param3;

    @ConstructorProperties({ "param1", "param2", "optionalParam" })
    public ServletPartDataClass(Part param1, boolean p2, Optional<Integer> optionalParam) {
      this.param1 = param1;
      this.param2 = p2;
      Assert.notNull(optionalParam, "Optional must not be null");
      optionalParam.ifPresent(integer -> this.param3 = integer);
    }

    public void setParam3(int param3) {
      this.param3 = param3;
    }
  }

  @RestController
  static class ServletPartDataClassController {

    @RequestMapping("/bind")
    public String handle(ServletPartDataClass data) throws IOException {
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
      binder.setConversionService(new DefaultFormattingConversionService());
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

}
