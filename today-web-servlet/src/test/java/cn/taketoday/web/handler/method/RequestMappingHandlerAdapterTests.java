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

package cn.taketoday.web.handler.method;

import org.apache.groovy.util.Maps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJacksonValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.config.EnableWebSession;
import cn.taketoday.ui.Model;
import cn.taketoday.ui.ModelMap;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RedirectModelManager;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.annotation.SessionAttributes;
import cn.taketoday.web.bind.resolver.HttpEntityMethodProcessor;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.RequestResponseBodyAdviceChain;
import cn.taketoday.web.bind.resolver.RequestResponseBodyMethodProcessor;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.web.testfixture.ReflectionTestUtils;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/11/10 15:24
 */
class RequestMappingHandlerAdapterTests {

  private RequestMappingHandlerAdapter handlerAdapter;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  private StaticWebApplicationContext webAppContext;

  private ServletRequestContext context;

  private final AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext(Config.class);

  @BeforeAll
  public static void setupOnce() {
    RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
    adapter.setApplicationContext(new StaticWebApplicationContext(new MockServletContext()));
    adapter.afterPropertiesSet();
  }

  @BeforeEach
  public void setup() throws Exception {
    this.webAppContext = new StaticWebApplicationContext(new MockServletContext());
    webAppContext.setParent(parent);

    handlerAdapter = new RequestMappingHandlerAdapter();
    handlerAdapter.setApplicationContext(this.webAppContext);
    handlerAdapter.setRedirectModelManager(webAppContext.getBean(RedirectModelManager.class));
    handlerAdapter.setResolvingRegistry(webAppContext.getBean(ParameterResolvingRegistry.class));

    this.request = new MockHttpServletRequest("GET", "/");
    this.response = new MockHttpServletResponse();

    context = new ServletRequestContext(null, request, response);
  }

  @EnableWebMvc
  @EnableWebSession
  static class Config {

  }

  @Test
  public void cacheControlWithoutSessionAttributes() throws Throwable {
    HandlerMethod handlerMethod = handlerMethod(new TestController(), "handle");
    this.handlerAdapter.setCacheSeconds(100);
    this.handlerAdapter.afterPropertiesSet();

    this.handlerAdapter.handle(context, handlerMethod);
    context.flush();
    assertThat(response.getHeader("Cache-Control")).contains("max-age");
  }

  @Test
  public void cacheControlWithSessionAttributes() throws Throwable {
    SessionAttributeController handler = new SessionAttributeController();
    this.handlerAdapter.setCacheSeconds(100);
    this.handlerAdapter.afterPropertiesSet();

    this.handlerAdapter.handle(context, handlerMethod(handler, "handle"));
    context.flush();
    assertThat(this.response.getHeader("Cache-Control")).isEqualTo("no-store");
  }

  @Test
  public void modelAttributeAdvice() throws Throwable {
    this.webAppContext.registerSingleton("maa", ModelAttributeAdvice.class);
    this.webAppContext.refresh();

    HandlerMethod handlerMethod = handlerMethod(new TestController(), "handle");
    this.handlerAdapter.afterPropertiesSet();
    this.handlerAdapter.handle(context, handlerMethod);

    ModelMap model = context.binding().getModel();

    assertThat(model.get("attr1")).isEqualTo("lAttr1");
    assertThat(model.get("attr2")).isEqualTo("gAttr2");
  }

  @Test
  public void prototypeControllerAdvice() throws Throwable {
    this.webAppContext.registerPrototype("maa", ModelAttributeAdvice.class);
    this.webAppContext.refresh();

    HandlerMethod handlerMethod = handlerMethod(new TestController(), "handle");
    this.handlerAdapter.afterPropertiesSet();
    context.setBinding(new BindingContext());
    this.handlerAdapter.handle(context, handlerMethod);
    ModelMap model = context.binding().getModel();

    Object instance = model.get("instance");
    context = new ServletRequestContext(null, request, response);
    context.setBinding(new BindingContext());
    model = context.binding().getModel();
    this.handlerAdapter.handle(context, handlerMethod);
    assertThat(instance).isNotSameAs(model.get("instance"));
  }

  @Test
  public void modelAttributeAdviceInParentContext() throws Throwable {
    parent.registerSingleton("maa", ModelAttributeAdvice.class);
    this.webAppContext.refresh();

    HandlerMethod handlerMethod = handlerMethod(new TestController(), "handle");
    this.handlerAdapter.afterPropertiesSet();

    context.setBinding(new BindingContext());
    this.handlerAdapter.handle(context, handlerMethod);
    ModelMap model = context.binding().getModel();

    assertThat(model.get("attr1")).isEqualTo("lAttr1");
    assertThat(model.get("attr2")).isEqualTo("gAttr2");
  }

  @Test
  public void modelAttributePackageNameAdvice() throws Throwable {
    this.webAppContext.registerSingleton("mapa", ModelAttributePackageAdvice.class);
    this.webAppContext.registerSingleton("manupa", ModelAttributeNotUsedPackageAdvice.class);
    this.webAppContext.refresh();

    HandlerMethod handlerMethod = handlerMethod(new TestController(), "handle");
    this.handlerAdapter.afterPropertiesSet();

    context.setBinding(new BindingContext());
    this.handlerAdapter.handle(context, handlerMethod);
    ModelMap model = context.binding().getModel();

    assertThat(model.get("attr1")).isEqualTo("lAttr1");
    assertThat(model.get("attr2")).isEqualTo("gAttr2");
    assertThat(model.get("attr3")).isNull();
  }

  @Test
  public void responseBodyAdvice() throws Throwable {
    parent.registerSingleton("rba", ResponseCodeSuppressingAdvice.class);
    this.webAppContext.refresh();

    this.request.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
    this.request.setParameter("c", "callback");

    RequestResponseBodyMethodProcessor processor = webAppContext.getBean(ParameterResolvingRegistry.class)
            .getDefaultStrategies().get(RequestResponseBodyMethodProcessor.class);
    RequestResponseBodyAdviceChain advice = (RequestResponseBodyAdviceChain) ReflectionTestUtils.getField(processor, "advice");
    List requestBodyAdvice = (List) ReflectionTestUtils.getField(advice, "requestBodyAdvice");
    requestBodyAdvice.add(webAppContext.getBean("rba"));

    HandlerMethod handlerMethod = handlerMethod(new TestController(), "handleBadRequest");
    this.handlerAdapter.afterPropertiesSet();

    Object handle = this.handlerAdapter.handle(context, handlerMethod);

    new HttpEntityMethodProcessor(List.of(new MappingJackson2HttpMessageConverter()),
            List.of(webAppContext.getBean("rba")), null)
            .handleReturnValue(context, handlerMethod, handle);

    assertThat(this.response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("{\"status\":400,\"message\":\"body\"}");
  }

  @Test
  public void responseBodyAdviceWithEmptyBody() throws Throwable {
    webAppContext.registerBean("rba", EmptyBodyAdvice.class);
    this.webAppContext.refresh();
    RequestResponseBodyMethodProcessor processor = webAppContext.getBean(ParameterResolvingRegistry.class)
            .getDefaultStrategies().get(RequestResponseBodyMethodProcessor.class);
    RequestResponseBodyAdviceChain advice = (RequestResponseBodyAdviceChain) ReflectionTestUtils.getField(processor, "advice");
    List requestBodyAdvice = (List) ReflectionTestUtils.getField(advice, "requestBodyAdvice");
    requestBodyAdvice.add(webAppContext.getBean("rba"));

    HandlerMethod handlerMethod = handlerMethod(new TestController(), "handleBody", Map.class);
    this.handlerAdapter.afterPropertiesSet();
    Object handle = this.handlerAdapter.handle(context, handlerMethod);

    assertThat(this.response.getStatus()).isEqualTo(200);
    assertThat(handle).isEqualTo("Body: {foo=bar}");
  }

  private HandlerMethod handlerMethod(Object handler, String methodName, Class<?>... paramTypes) throws Exception {
    Method method = handler.getClass().getDeclaredMethod(methodName, paramTypes);
    return new HandlerMethod(handler, method);
  }

  @SuppressWarnings("unused")
  private static class TestController {

    @ModelAttribute
    public void addAttributes(Model model) {
      model.addAttribute("attr1", "lAttr1");
    }

    public String handle() {
      return null;
    }

    public ResponseEntity<Map<String, String>> handleWithResponseEntity() {
      return new ResponseEntity<>(Collections.singletonMap("foo", "bar"), HttpStatus.OK);
    }

    public ResponseEntity<String> handleBadRequest() {
      return new ResponseEntity<>("body", HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    public String handleBody(@Nullable @RequestBody Map<String, String> body) {
      return "Body: " + body;
    }
  }

  @SessionAttributes("attr1")
  private static class SessionAttributeController {

    @SuppressWarnings("unused")
    public void handle() {
    }
  }

  @SuppressWarnings("unused")
  private static class RedirectAttributeController {

    public String handle(Model model) {
      model.addAttribute("someAttr", "someAttrValue");
      return "redirect:/path";
    }
  }

  @ControllerAdvice
  private static class ModelAttributeAdvice {

    @SuppressWarnings("unused")
    @ModelAttribute
    public void addAttributes(Model model) {
      model.addAttribute("attr1", "gAttr1");
      model.addAttribute("attr2", "gAttr2");
      model.addAttribute("instance", this);
    }
  }

  @ControllerAdvice({ "cn.taketoday.web.handler.method", "java.lang" })
  private static class ModelAttributePackageAdvice {

    @SuppressWarnings("unused")
    @ModelAttribute
    public void addAttributes(Model model) {
      model.addAttribute("attr2", "gAttr2");
    }
  }

  @ControllerAdvice("java.lang")
  private static class ModelAttributeNotUsedPackageAdvice {

    @SuppressWarnings("unused")
    @ModelAttribute
    public void addAttributes(Model model) {
      model.addAttribute("attr3", "gAttr3");
    }
  }

  /**
   * This class additionally implements {@link RequestBodyAdvice} solely for the purpose
   * of verifying that controller advice implementing both {@link ResponseBodyAdvice}
   * and {@link RequestBodyAdvice} does not get registered twice.
   */
  @ControllerAdvice
  private static class ResponseCodeSuppressingAdvice
          extends AbstractMappingJacksonResponseBodyAdvice implements RequestBodyAdvice {

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
            @Nullable MethodParameter returnType, RequestContext request) {

      int status = request.getStatus();
      request.setStatus(HttpStatus.OK);

      Map<String, Object> map = new LinkedHashMap<>();
      map.put("status", status);
      map.put("message", bodyContainer.getValue());
      bodyContainer.setValue(map);
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, HttpMessageConverter<?> converter) {
      return converter instanceof StringHttpMessageConverter;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
            Type targetType, HttpMessageConverter<?> converter) throws IOException {

      return inputMessage;
    }

    @Nullable
    @Override
    public Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage,
            MethodParameter parameter, Type targetType, HttpMessageConverter<?> converter) {

      return "default value for empty body";
    }
  }

  @ControllerAdvice
  private static class EmptyBodyAdvice implements RequestBodyAdvice {

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, HttpMessageConverter<?> converter) {
      return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
            Type targetType, HttpMessageConverter<?> converter) throws IOException {

      throw new UnsupportedOperationException();
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage,
            MethodParameter parameter, Type targetType, HttpMessageConverter<?> converter) {

      throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage,
            MethodParameter parameter, Type targetType, HttpMessageConverter<?> converter) {

      return Maps.of("foo", "bar");
    }
  }

}