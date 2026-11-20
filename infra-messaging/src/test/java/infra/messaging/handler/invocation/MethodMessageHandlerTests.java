/*
 * Copyright 2002-present the original author or authors.
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

package infra.messaging.handler.invocation;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import infra.context.ApplicationContext;
import infra.context.support.StaticApplicationContext;
import infra.core.MethodParameter;
import infra.util.Assert;
import infra.messaging.Message;
import infra.messaging.converter.SimpleMessageConverter;
import infra.messaging.handler.DestinationPatternsMessageCondition;
import infra.messaging.handler.HandlerMethod;
import infra.messaging.handler.MessagingAdviceBean;
import infra.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import infra.messaging.support.MessageBuilder;
import infra.util.AntPathMatcher;
import infra.util.PathMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for
 * {@link AbstractMethodMessageHandler}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
class MethodMessageHandlerTests {

  private static final String DESTINATION_HEADER = "destination";

  private TestMethodMessageHandler messageHandler;

  private TestController testController;

  @BeforeEach
  void setup() {

    List<String> destinationPrefixes = List.of("/test");

    this.messageHandler = new TestMethodMessageHandler();
    this.messageHandler.setApplicationContext(new StaticApplicationContext());
    this.messageHandler.setDestinationPrefixes(destinationPrefixes);
    this.messageHandler.afterPropertiesSet();

    this.testController = new TestController();
    this.messageHandler.registerHandler(this.testController);
  }

  @Test
  void duplicateMapping() {
    assertThatIllegalStateException().isThrownBy(() ->
            this.messageHandler.registerHandler(new DuplicateMappingsController()));
  }

  @Test
  void registeredMappings() {

    Map<String, HandlerMethod> handlerMethods = this.messageHandler.getHandlerMethods();

    assertThat(handlerMethods).isNotNull();
    assertThat(handlerMethods).hasSize(3);
  }

  @Test
  void patternMatch() throws Exception {

    Method method = this.testController.getClass().getMethod("handlerPathMatchWildcard");
    this.messageHandler.registerHandlerMethod(this.testController, method, "/handlerPathMatch*");

    this.messageHandler.handleMessage(toDestination("/test/handlerPathMatchFoo"));

    assertThat(this.testController.method).isEqualTo("pathMatchWildcard");
  }

  @Test
  void bestMatch() throws Exception {

    Method method = this.testController.getClass().getMethod("bestMatch");
    this.messageHandler.registerHandlerMethod(this.testController, method, "/bestmatch/{foo}/path");

    method = this.testController.getClass().getMethod("secondBestMatch");
    this.messageHandler.registerHandlerMethod(this.testController, method, "/bestmatch/*/*");

    this.messageHandler.handleMessage(toDestination("/test/bestmatch/bar/path"));

    assertThat(this.testController.method).isEqualTo("bestMatch");
  }

  @Test
  void argumentResolution() {

    this.messageHandler.handleMessage(toDestination("/test/handlerArgumentResolver"));

    assertThat(this.testController.method).isEqualTo("handlerArgumentResolver");
    assertThat(this.testController.arguments.get("message")).isNotNull();
  }

  @Test
  void handleException() {

    this.messageHandler.handleMessage(toDestination("/test/handlerThrowsExc"));

    assertThat(this.testController.method).isEqualTo("illegalStateException");
    assertThat(this.testController.arguments.get("exception")).isNotNull();
  }

  @Test
  void ignoresMessagesWithoutMatchingDestination() {
    this.messageHandler.handleMessage(MessageBuilder.withPayload(new byte[0]).build());
    this.messageHandler.handleMessage(toDestination("/other/handlerArgumentResolver"));
    this.messageHandler.handleMessage(toDestination("/test/noSuchHandler"));

    assertThat(this.testController.method).isNull();
  }

  @Test
  void resolverConfigurationAndLookupDestination() {
    HandlerMethodArgumentResolver argumentResolver = mock();
    HandlerMethodReturnValueHandler returnValueHandler = mock();

    this.messageHandler.setCustomArgumentResolvers(List.of(argumentResolver));
    assertThat(this.messageHandler.getCustomArgumentResolvers()).containsExactly(argumentResolver);
    this.messageHandler.setCustomArgumentResolvers(null);
    assertThat(this.messageHandler.getCustomArgumentResolvers()).isEmpty();

    this.messageHandler.setCustomReturnValueHandlers(List.of(returnValueHandler));
    assertThat(this.messageHandler.getCustomReturnValueHandlers()).containsExactly(returnValueHandler);
    this.messageHandler.setCustomReturnValueHandlers(null);
    assertThat(this.messageHandler.getCustomReturnValueHandlers()).isEmpty();

    this.messageHandler.setDestinationPrefixes(null);
    assertThat(this.messageHandler.lookupDestination("/anywhere")).isEqualTo("/anywhere");
    this.messageHandler.setDestinationPrefixes(List.of("/api", "/test"));
    assertThat(this.messageHandler.lookupDestination("/test/foo")).isEqualTo("/foo");
    assertThat(this.messageHandler.lookupDestination("/other/foo")).isNull();
    assertThat(this.messageHandler.lookupDestination(null)).isNull();
  }

  @Test
  void rejectsNullMapping() throws Exception {
    Method method = this.testController.getClass().getMethod("handlerArgumentResolver", Message.class);
    assertThatIllegalArgumentException().isThrownBy(() ->
            this.messageHandler.registerHandlerMethod(this.testController, method, null));
  }

  @Test
  void rejectsAmbiguousPatternsAtDispatchTime() throws Exception {
    this.messageHandler.registerHandlerMethod(this.testController,
            TestController.class.getMethod("bestMatch"), "/orders/{id}");
    this.messageHandler.registerHandlerMethod(this.testController,
            TestController.class.getMethod("secondBestMatch"), "/orders/{name}");

    assertThatIllegalStateException().isThrownBy(() -> this.messageHandler.handleMessage(toDestination("/test/orders/42")))
            .withMessageContaining("/orders/42", "bestMatch", "secondBestMatch");
    assertThat(this.testController.method).isNull();
  }

  @Test
  void skipsUnresolvableBeansAndScopedTargetsDuringDiscovery() {
    ApplicationContext context = mock();
    given(context.getBeanNamesForType(Object.class)).willReturn(new String[] { "lazy", "scopedTarget.controller", "plain" });
    given(context.getType("lazy")).willThrow(new IllegalStateException("unresolvable bean"));
    doReturn(String.class).when(context).getType("plain");
    TestMethodMessageHandler handler = new TestMethodMessageHandler();
    handler.setApplicationContext(context);

    handler.afterPropertiesSet();

    assertThat(handler.getHandlerMethods()).isEmpty();
    verify(context, never()).getType("scopedTarget.controller");
    verify(context).getType("plain");
  }

  @Test
  void discoversAndInvokesHandlerByBeanName() {
    try (StaticApplicationContext context = new StaticApplicationContext()) {
      TestController controller = new TestController();
      context.getBeanFactory().registerSingleton("controller", controller);
      TestMethodMessageHandler handler = new TestMethodMessageHandler();
      handler.setApplicationContext(context);
      handler.afterPropertiesSet();

      assertThat(handler.getHandlerMethods().get("/handlerArgumentResolver").getBean()).isEqualTo("controller");
      handler.handleMessage(toDestination("/handlerArgumentResolver"));

      assertThat(controller.method).isEqualTo("handlerArgumentResolver");
    }
  }

  @Test
  void explicitResolversOverrideDefaultsWithoutAnApplicationContext() {
    TestMethodMessageHandler handler = new TestMethodMessageHandler();
    HandlerMethodArgumentResolver argumentResolver = mock();
    HandlerMethodReturnValueHandler returnValueHandler = mock();
    handler.setArgumentResolvers(List.of(argumentResolver));
    handler.setReturnValueHandlers(List.of(returnValueHandler));

    handler.afterPropertiesSet();

    assertThat(handler.getArgumentResolvers()).containsExactly(argumentResolver);
    assertThat(handler.getReturnValueHandlers()).containsExactly(returnValueHandler);
    handler.setArgumentResolvers(null);
    handler.setReturnValueHandlers(null);
    assertThat(handler.getArgumentResolvers()).isEmpty();
    assertThat(handler.getReturnValueHandlers()).isEmpty();
  }

  @Test
  void sendsNestedExceptionToApplicableAdviceAndHandlesItsReturnValue() throws Exception {
    ExceptionAdvice advice = new ExceptionAdvice();
    MessagingAdviceBean inapplicable = mock();
    MessagingAdviceBean applicable = mock();
    given(applicable.isApplicableToBeanType(TestController.class)).willReturn(true);
    given(applicable.resolveBean()).willReturn(advice);
    TestExceptionResolver resolver = new TestExceptionResolver(ExceptionAdvice.class);
    this.messageHandler.registerExceptionHandlerAdvice(inapplicable, resolver);
    this.messageHandler.registerExceptionHandlerAdvice(applicable, resolver);
    HandlerMethodReturnValueHandler responseHandler = mock();
    given(responseHandler.supportsReturnType(any(MethodParameter.class))).willReturn(true);
    this.messageHandler.setReturnValueHandlers(List.of(responseHandler));
    this.messageHandler.registerHandlerMethod(this.testController, TestController.class.getMethod("nestedFailure"), "/nested");

    this.messageHandler.handleMessage(toDestination("/test/nested"));

    assertThat(advice.failure).isSameAs(this.testController.nestedCause);
    verify(responseHandler).handleReturnValue(eq("recovered"), any(), any());
    verify(inapplicable, never()).resolveBean();
  }

  @Test
  void routesAsyncReturnValueHandlingFailureToController() throws Exception {
    HandlerMethodReturnValueHandler responseHandler = mock();
    given(responseHandler.supportsReturnType(any(MethodParameter.class))).willReturn(true);
    IllegalStateException failure = new IllegalStateException("reply not sent");
    doThrow(failure).when(responseHandler).handleReturnValue(eq("reply"), any(), any());
    this.messageHandler.setReturnValueHandlers(List.of(new CompletableFutureReturnValueHandler(), responseHandler));
    this.messageHandler.registerHandlerMethod(this.testController, TestController.class.getMethod("asyncResult"), "/async");

    this.messageHandler.handleMessage(toDestination("/test/async"));
    assertThat(this.testController.method).isNull();
    this.testController.reply.complete("reply");

    assertThat(this.testController.method).isEqualTo("illegalStateException");
    assertThat(this.testController.arguments).containsEntry("exception", failure);
  }

  @Test
  void routesExceptionallyCompletedFutureToController() throws Exception {
    this.messageHandler.setReturnValueHandlers(List.of(new CompletableFutureReturnValueHandler()));
    this.messageHandler.registerHandlerMethod(this.testController, TestController.class.getMethod("asyncResult"), "/async");
    IllegalStateException failure = new IllegalStateException("reply failed");

    this.messageHandler.handleMessage(toDestination("/test/async"));
    this.testController.reply.completeExceptionally(failure);

    assertThat(this.testController.method).isEqualTo("illegalStateException");
    assertThat(this.testController.arguments).containsEntry("exception", failure);
  }

  @Test
  void unhandledHandlerExceptionDoesNotEscapeDispatch() throws Exception {
    Method method = this.testController.getClass().getMethod("handleMessageWithoutAdvice");
    this.messageHandler.registerHandlerMethod(this.testController, method, "/without-advice");

    this.messageHandler.handleMessage(toDestination("/test/without-advice"));

    assertThat(this.testController.method).isNull();
  }

  private Message<?> toDestination(String destination) {
    return MessageBuilder.withPayload(new byte[0]).setHeader(DESTINATION_HEADER, destination).build();
  }

  @SuppressWarnings("unused")
  private static class TestController {

    public String method;

    private Map<String, Object> arguments = new LinkedHashMap<>();

    private final IllegalArgumentException nestedCause = new IllegalArgumentException("invalid value");

    private final CompletableFuture<String> reply = new CompletableFuture<>();

    public void nestedFailure() {
      throw new UnsupportedOperationException(this.nestedCause);
    }

    public CompletableFuture<String> asyncResult() {
      return this.reply;
    }

    public void handlerPathMatchWildcard() {
      this.method = "pathMatchWildcard";
    }

    @SuppressWarnings("rawtypes")
    public void handlerArgumentResolver(Message message) {
      this.method = "handlerArgumentResolver";
      this.arguments.put("message", message);
    }

    public void handlerThrowsExc() {
      throw new IllegalStateException();
    }

    public void bestMatch() {
      this.method = "bestMatch";
    }

    public void secondBestMatch() {
      this.method = "secondBestMatch";
    }

    public void handleIllegalStateException(IllegalStateException exception) {
      this.method = "illegalStateException";
      this.arguments.put("exception", exception);
    }

    public void handleMessageWithoutAdvice() {
      throw new UnsupportedOperationException("no advice");
    }

  }

  private static class ExceptionAdvice {

    private IllegalArgumentException failure;

    public String handleIllegalArgumentException(IllegalArgumentException failure) {
      this.failure = failure;
      return "recovered";
    }
  }

  @SuppressWarnings("unused")
  private static class DuplicateMappingsController {

    public void handlerFoo() { }

    public void handlerFoo(String arg) { }
  }

  private static class TestMethodMessageHandler extends AbstractMethodMessageHandler<String> {

    private PathMatcher pathMatcher = new AntPathMatcher();

    public void registerHandler(Object handler) {
      super.detectHandlerMethods(handler);
    }

    @Override
    public void registerHandlerMethod(Object handler, Method method, String mapping) {
      super.registerHandlerMethod(handler, method, mapping);
    }

    public @Nullable String lookupDestination(@Nullable String destination) {
      return getLookupDestination(destination);
    }

    @Override
    protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
      List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
      resolvers.add(new MessageMethodArgumentResolver(new SimpleMessageConverter()));
      resolvers.addAll(getCustomArgumentResolvers());
      return resolvers;
    }

    @Override
    protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
      return new ArrayList<>(getCustomReturnValueHandlers());
    }

    @Override
    protected boolean isHandler(Class<?> beanType) {
      return beanType.getName().contains("Controller");
    }

    @Override
    protected String getMappingForMethod(Method method, Class<?> handlerType) {
      String methodName = method.getName();
      if (methodName.startsWith("handler")) {
        return "/" + methodName;
      }
      return null;
    }

    @Override
    protected Set<String> getDirectLookupDestinations(String mapping) {
      Set<String> result = new LinkedHashSet<>();
      if (!this.pathMatcher.isPattern(mapping)) {
        result.add(mapping);
      }
      return result;
    }

    @Override
    protected String getDestination(Message<?> message) {
      return (String) message.getHeaders().get(DESTINATION_HEADER);
    }

    @Override
    protected @Nullable String getMatchingMapping(String mapping, Message<?> message) {
      String destination = getLookupDestination(getDestination(message));
      Assert.notNull(destination, "No destination");
      return mapping.equals(destination) || this.pathMatcher.match(mapping, destination) ? mapping : null;
    }

    @Override
    protected Comparator<String> getMappingComparator(final Message<?> message) {
      return (info1, info2) -> {
        DestinationPatternsMessageCondition cond1 = new DestinationPatternsMessageCondition(info1);
        DestinationPatternsMessageCondition cond2 = new DestinationPatternsMessageCondition(info2);
        return cond1.compareTo(cond2, message);
      };
    }

    @Override
    protected AbstractExceptionHandlerMethodResolver createExceptionHandlerMethodResolverFor(Class<?> beanType) {
      return new TestExceptionResolver(beanType);
    }
  }

}
