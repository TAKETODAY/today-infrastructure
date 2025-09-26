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

package infra.web.handler.method;

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

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.core.MethodParameter;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.http.converter.json.MappingJacksonValue;
import org.jspecify.annotations.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.session.SessionManager;
import infra.session.WebSession;
import infra.session.config.EnableWebSession;
import infra.ui.Model;
import infra.ui.ModelMap;
import infra.web.BindingContext;
import infra.web.RedirectModel;
import infra.web.RedirectModelManager;
import infra.web.RequestContext;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestParam;
import infra.web.annotation.ResponseBody;
import infra.web.bind.annotation.ModelAttribute;
import infra.web.bind.resolver.HttpEntityMethodProcessor;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.bind.resolver.RequestResponseBodyAdviceChain;
import infra.web.bind.resolver.RequestResponseBodyMethodProcessor;
import infra.web.bind.support.WebBindingInitializer;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.handler.result.ResponseBodyEmitter;
import infra.web.handler.result.SseEmitter;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;
import infra.web.testfixture.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/11/10 15:24
 */
class RequestMappingHandlerAdapterTests {

  private RequestMappingHandlerAdapter handlerAdapter;

  private HttpMockRequestImpl request;

  private MockHttpResponseImpl response;

  private StaticWebApplicationContext webAppContext;

  private MockRequestContext context;

  private final AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext(Config.class);

  @BeforeAll
  public static void setupOnce() {
    RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
    adapter.setApplicationContext(new StaticWebApplicationContext(new MockContextImpl()));
    adapter.afterPropertiesSet();
  }

  @BeforeEach
  public void setup() throws Exception {
    this.webAppContext = new StaticWebApplicationContext(new MockContextImpl());
    webAppContext.setParent(parent);

    handlerAdapter = new RequestMappingHandlerAdapter();
    handlerAdapter.setApplicationContext(this.webAppContext);
    handlerAdapter.setRedirectModelManager(webAppContext.getBean(RedirectModelManager.class));
    handlerAdapter.setResolvingRegistry(webAppContext.getBean(ParameterResolvingRegistry.class));

    this.request = new HttpMockRequestImpl("GET", "/");
    this.response = new MockHttpResponseImpl();

    context = new MockRequestContext(webAppContext, request, response);
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
  void responseEntityWithWildCardAndConditionalStream() throws Throwable {
    HandlerMethod handlerMethod = handlerMethod(new SseController(), "handle", String.class);
    this.handlerAdapter.afterPropertiesSet();

    this.request.setAsyncSupported(true);
    this.request.addParameter("q", "sse");

    Object result = this.handlerAdapter.handle(context, handlerMethod);

    webAppContext.getBean(ReturnValueHandlerManager.class)
            .handleReturnValue(context, handlerMethod, result);

    assertThat(this.response.getStatus()).isEqualTo(200);
    assertThat(this.response.getHeader("Content-Type")).isEqualTo("text/event-stream");
    assertThat(this.response.getContentAsString()).isEqualTo("data:event 1\n\ndata:event 2\n\n");
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
    context = new MockRequestContext(null, request, response);
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

  @Test
  void handleMethodWithoutSessionSynchronization() throws Throwable {
    this.handlerAdapter.setSynchronizeOnSession(false);
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    HandlerMethod handlerMethod = new HandlerMethod(new TestController(), "handle");

    handlerAdapter.afterPropertiesSet();
    Object result = this.handlerAdapter.handleInternal(new MockRequestContext(request), handlerMethod);

    assertThat(result).isNull();
  }

  @Test
  void handleMethodWithSessionSynchronizationNoSession() throws Throwable {
    this.handlerAdapter.setSynchronizeOnSession(true);
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    HandlerMethod handlerMethod = new HandlerMethod(new TestController(), "handle");
    handlerAdapter.afterPropertiesSet();
    Object result = this.handlerAdapter.handleInternal(new MockRequestContext(request), handlerMethod);
    assertThat(result).isNull();
  }

  @Test
  void handleMethodWithRedirectAttributes() throws Throwable {
    RedirectModelManager redirectModelManager = mock(RedirectModelManager.class);
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.addAttribute("attr", "value");
    when(redirectModelManager.retrieveAndUpdate(any())).thenReturn(redirectModel);

    this.handlerAdapter.setRedirectModelManager(redirectModelManager);
    handlerAdapter.afterPropertiesSet();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    HandlerMethod handlerMethod = new HandlerMethod(new TestController(), "handle");

    MockRequestContext request1 = new MockRequestContext(request);
    Object result = this.handlerAdapter.handleInternal(request1, handlerMethod);

    assertThat(request1.getBinding().getModel()).containsEntry("attr", "value");
  }

  @Test
  void handleMethodWithCustomWebBindingInitializer() throws Throwable {
    WebBindingInitializer initializer = mock(WebBindingInitializer.class);
    this.handlerAdapter.setWebBindingInitializer(initializer);
    handlerAdapter.afterPropertiesSet();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    HandlerMethod handlerMethod = new HandlerMethod(new TestController(), "handle");

    this.handlerAdapter.handleInternal(new MockRequestContext(request), handlerMethod);

//    verify(initializer).initBinder(any());
  }

  @Test
  void handleMethodWithCustomSessionManager() throws Throwable {
    SessionManager sessionManager = mock(SessionManager.class);
    WebSession session = mock(WebSession.class);
    when(sessionManager.getSession(any(), eq(false))).thenReturn(session);

    this.handlerAdapter.setSessionManager(sessionManager);
    this.handlerAdapter.setSynchronizeOnSession(true);
    handlerAdapter.afterPropertiesSet();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    HandlerMethod handlerMethod = new HandlerMethod(new TestController(), "handle");

    MockRequestContext ctx = new MockRequestContext(request);
    Object result = this.handlerAdapter.handleInternal(ctx, handlerMethod);

    verify(sessionManager).getSession(ctx, false);
    assertThat(result).isNull();
  }

  @Test
  void handleMethodWithoutCacheControlHeader() throws Throwable {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    HandlerMethod handlerMethod = new HandlerMethod(new TestController(), "handle");

    MockRequestContext requestContext = new MockRequestContext(request);
    handlerAdapter.afterPropertiesSet();
    this.handlerAdapter.handleInternal(requestContext, handlerMethod);

    assertThat(requestContext.responseHeaders().get(HttpHeaders.CACHE_CONTROL)).isNull();
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

  @ControllerAdvice({ "infra.web.handler.method", "java.lang" })
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

  private static class SseController {

    public ResponseEntity<?> handle(@RequestParam String q) throws IOException {
      if (q.equals("sse")) {
        SseEmitter emitter = ResponseBodyEmitter.forServerSentEvents();
        emitter.send("event 1");
        emitter.send("event 2");
        emitter.complete();
        return ResponseEntity.ok().body(emitter);
      }
      return ResponseEntity.ok("text");
    }

  }
}