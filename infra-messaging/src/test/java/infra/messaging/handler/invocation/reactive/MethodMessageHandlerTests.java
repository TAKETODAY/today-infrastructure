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

package infra.messaging.handler.invocation.reactive;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import infra.context.ApplicationContext;
import infra.context.support.StaticApplicationContext;
import infra.core.MethodParameter;
import infra.core.ReactiveAdapterRegistry;
import infra.util.Assert;
import infra.messaging.Message;
import infra.messaging.handler.DestinationPatternsMessageCondition;
import infra.messaging.handler.HandlerMethod;
import infra.messaging.handler.MessagingAdviceBean;
import infra.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import infra.messaging.handler.invocation.TestExceptionResolver;
import infra.messaging.support.GenericMessage;
import infra.util.AntPathMatcher;
import infra.util.ClassUtils;
import infra.util.PathMatcher;
import infra.util.RouteMatcher;
import infra.util.SimpleRouteMatcher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AbstractMethodMessageHandler}.
 *
 * @author Rossen Stoyanchev
 */
class MethodMessageHandlerTests {

  @Test
  void duplicateMapping() {
    assertThatIllegalStateException().isThrownBy(() ->
            initMethodMessageHandler(DuplicateMappingsController.class));
  }

  @Test
  void registeredMappings() {
    TestMethodMessageHandler messageHandler = initMethodMessageHandler(TestController.class);
    Map<String, HandlerMethod> mappings = messageHandler.getHandlerMethods();

    assertThat(mappings).containsOnlyKeys(
            "/handleMessage", "/handleMessageWithArgument", "/handleMessageWithError",
            "/handleMessageMatch1", "/handleMessageMatch2");
  }

  @Test
  @SuppressWarnings("unchecked")
  void bestMatch() throws NoSuchMethodException {
    TestMethodMessageHandler handler = new TestMethodMessageHandler();
    TestController controller = new TestController();
    handler.registerHandlerMethod(controller,
            TestController.class.getMethod("handleMessageMatch1"), "/bestmatch/{foo}/path");
    handler.registerHandlerMethod(controller,
            TestController.class.getMethod("handleMessageMatch2"), "/bestmatch/*/*");
    handler.afterPropertiesSet();

    Message<?> message = new GenericMessage<>("body", Collections.singletonMap(
            DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
            new SimpleRouteMatcher(new AntPathMatcher()).parseRoute("/bestmatch/bar/path")));

    handler.handleMessage(message).block(Duration.ofSeconds(5));

    StepVerifier.create((Publisher<Object>) handler.getLastReturnValue())
            .expectNext("handleMessageMatch1")
            .verifyComplete();
  }

  @Test
  @SuppressWarnings("unchecked")
  void argumentResolution() {

    ArgumentResolverConfigurer configurer = new ArgumentResolverConfigurer();
    configurer.addCustomResolver(new StubArgumentResolver(String.class, "foo"));

    TestMethodMessageHandler handler = initMethodMessageHandler(
            theHandler -> theHandler.setArgumentResolverConfigurer(configurer),
            TestController.class);

    Message<?> message = new GenericMessage<>("body", Collections.singletonMap(
            DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
            new SimpleRouteMatcher(new AntPathMatcher()).parseRoute("/handleMessageWithArgument")));

    handler.handleMessage(message).block(Duration.ofSeconds(5));

    StepVerifier.create((Publisher<Object>) handler.getLastReturnValue())
            .expectNext("handleMessageWithArgument,payload=foo")
            .verifyComplete();
  }

  @Test
  @SuppressWarnings("unchecked")
  void handleException() {

    TestMethodMessageHandler handler = initMethodMessageHandler(TestController.class);

    Message<?> message = new GenericMessage<>("body", Collections.singletonMap(
            DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
            new SimpleRouteMatcher(new AntPathMatcher()).parseRoute("/handleMessageWithError")));

    handler.handleMessage(message).block(Duration.ofSeconds(5));

    StepVerifier.create((Publisher<Object>) handler.getLastReturnValue())
            .expectNext("handleIllegalStateException,ex=rejected")
            .verifyComplete();
  }

  @Test
  void returnsEmptyForMessageWithoutMatchingDestination() {
    TestMethodMessageHandler handler = initMethodMessageHandler(TestController.class);

    Message<?> message = new GenericMessage<>("body", Collections.singletonMap(
            DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
                new SimpleRouteMatcher(new AntPathMatcher()).parseRoute("/not-mapped")));
    StepVerifier.create(handler.handleMessage(message))
            .verifyComplete();
  }

  @Test
  void handlesMessagesWithoutDestinationAndReportsInvocationFailures() throws Exception {
    TestMethodMessageHandler handler = new TestMethodMessageHandler();
    handler.setHandlers(Collections.emptyList());
    handler.afterPropertiesSet();

    StepVerifier.create(handler.handleMessage(new GenericMessage<>("body")))
            .verifyComplete();

    handler = new TestMethodMessageHandler();
    handler.setHandlers(Collections.singletonList(new FailingController()));
    handler.afterPropertiesSet();
    handler.registerHandlerMethod(new FailingController(), FailingController.class.getMethod("fail"), "/fail");
    Message<?> message = new GenericMessage<>("body", Collections.singletonMap(
            DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
            new SimpleRouteMatcher(new AntPathMatcher()).parseRoute("/fail")));
    StepVerifier.create(handler.handleMessage(message))
            .expectError(IllegalStateException.class)
            .verify(Duration.ofSeconds(5));
  }

  @Test
  void supportsExplicitHandlersWithoutApplicationContext() {
    TestMethodMessageHandler handler = new TestMethodMessageHandler();
    handler.setHandlerPredicate(type -> true);
    handler.setHandlers(Collections.singletonList(new TestController()));
    assertThat(handler.getHandlerPredicate()).isNull();
    handler.afterPropertiesSet();
    assertThat(handler.getHandlerMethods()).containsKey("/handleMessage");
    assertThat(handler.getDestinationLookup().get("/handleMessage")).contains("/handleMessage");
  }

  @Test
  void rejectsBeanNameDetectionWithoutApplicationContext() throws Exception {
    TestMethodMessageHandler handler = new TestMethodMessageHandler();
    assertThatIllegalStateException().isThrownBy(() ->
            handler.detectHandlerMethodsForTest("controller"));
  }

  @Test
  void ignoresUnresolvableBeanTypesAndScopedTargetsDuringDiscovery() {
    ApplicationContext context = mock();
    given(context.getBeanNamesForType(Object.class)).willReturn(new String[] { "lazy", "scopedTarget.controller" });
    given(context.getType("lazy")).willThrow(new IllegalStateException("type not available"));
    TestMethodMessageHandler handler = new TestMethodMessageHandler();
    handler.setApplicationContext(context);

    handler.afterPropertiesSet();

    assertThat(handler.getHandlerMethods()).isEmpty();
    verify(context, never()).getType("scopedTarget.controller");
  }

  @Test
  void fallsBackToApplicableAdviceWhenReturnValueHandlingFails() throws Exception {
    TestController controller = new TestController();
    TestMethodMessageHandler handler = new TestMethodMessageHandler();
    IllegalArgumentException failure = new IllegalArgumentException("reply not sent");
    TestAdvice advice = new TestAdvice();
    MessagingAdviceBean ignored = mock();
    MessagingAdviceBean applicable = mock();
    given(applicable.isApplicableToBeanType(TestController.class)).willReturn(true);
    given(applicable.resolveBean()).willReturn(advice);
    TestExceptionResolver resolver = new TestExceptionResolver(TestAdvice.class);
    handler.registerExceptionHandlerAdvice(ignored, resolver);
    handler.registerExceptionHandlerAdvice(applicable, resolver);
    handler.registerHandlerMethod(controller, TestController.class.getMethod("handleMessage"), "/reply");
    handler.returnValueHandler = new TestReturnValueHandler() {
      @Override
      public Mono<Void> handleReturnValue(@Nullable Object value, MethodParameter returnType, Message<?> message) {
        return (returnType.getExecutable().getName().equals("handleMessage") ? Mono.error(failure) :
                super.handleReturnValue(value, returnType, message));
      }
    };
    handler.afterPropertiesSet();
    Message<?> message = new GenericMessage<>("body", Map.of(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
            new SimpleRouteMatcher(new AntPathMatcher()).parseRoute("/reply")));

    StepVerifier.create(handler.handleMessage(message)).expectComplete().verify(Duration.ofSeconds(5));

    assertThat(advice.failure).isSameAs(failure);
    assertThat(handler.getLastReturnValue()).isEqualTo("recovered");
    verify(ignored, never()).resolveBean();
  }

  @Test
  void rejectsEquallySpecificDestinationPatterns() throws Exception {
    TestMethodMessageHandler handler = new TestMethodMessageHandler();
    TestController controller = new TestController();
    handler.registerHandlerMethod(controller, TestController.class.getMethod("handleMessageMatch1"), "/orders/{id}");
    handler.registerHandlerMethod(controller, TestController.class.getMethod("handleMessageMatch2"), "/orders/{name}");
    handler.afterPropertiesSet();
    Message<?> message = new GenericMessage<>("body", Map.of(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
            new SimpleRouteMatcher(new AntPathMatcher()).parseRoute("/orders/42")));

    StepVerifier.create(handler.handleMessage(message))
            .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(IllegalStateException.class)
                    .hasMessageContainingAll("/orders/42", "handleMessageMatch1", "handleMessageMatch2"))
            .verify(Duration.ofSeconds(5));
    assertThat(handler.getLastReturnValue()).isNull();
  }

  @Test
  void configurationPropertiesAndExplicitHandlers() {
    TestMethodMessageHandler handler = new TestMethodMessageHandler();
    assertThat(handler.getHandlerPredicate()).isNotNull();

    handler.setHandlerPredicate(null);
    assertThat(handler.getHandlerPredicate()).isNull();
    handler.setHandlerPredicate(type -> true);
    assertThat(handler.getHandlerPredicate()).isNotNull();

    List<Object> handlers = Collections.singletonList(new TestController());
    handler.setHandlers(handlers);
    assertThat(handler.getHandlerPredicate()).isNull();

    ArgumentResolverConfigurer argumentConfigurer = new ArgumentResolverConfigurer();
    ReturnValueHandlerConfigurer returnValueConfigurer = new ReturnValueHandlerConfigurer();
    ReactiveAdapterRegistry registry = new ReactiveAdapterRegistry();
    handler.setArgumentResolverConfigurer(argumentConfigurer);
    handler.setReturnValueHandlerConfigurer(returnValueConfigurer);
    handler.setReactiveAdapterRegistry(registry);
    handler.setApplicationContext(new StaticApplicationContext());
    handler.setBeanName("testHandler");

    assertThat(handler.getArgumentResolverConfigurer()).isSameAs(argumentConfigurer);
    assertThat(handler.getReturnValueHandlerConfigurer()).isSameAs(returnValueConfigurer);
    assertThat(handler.getReactiveAdapterRegistry()).isSameAs(registry);
    assertThat(handler.getApplicationContext()).isInstanceOf(StaticApplicationContext.class);
    assertThat(handler.getBeanName()).isEqualTo("testHandler");

    handler.afterPropertiesSet();
    assertThat(handler.getHandlerMethods()).isNotEmpty();
  }

  private TestMethodMessageHandler initMethodMessageHandler(Class<?>... handlerTypes) {
    return initMethodMessageHandler(handler -> { }, handlerTypes);
  }

  private TestMethodMessageHandler initMethodMessageHandler(
          Consumer<TestMethodMessageHandler> customizer, Class<?>... handlerTypes) {

    StaticApplicationContext context = new StaticApplicationContext();
    for (Class<?> handlerType : handlerTypes) {
      String beanName = ClassUtils.getShortNameAsProperty(handlerType);
      context.registerPrototype(beanName, handlerType);
    }
    TestMethodMessageHandler messageHandler = new TestMethodMessageHandler();
    messageHandler.setApplicationContext(context);
    customizer.accept(messageHandler);
    messageHandler.afterPropertiesSet();
    return messageHandler;
  }

  @SuppressWarnings("unused")
  private static class TestController {

    public Mono<String> handleMessage() {
      return delay("handleMessage");
    }

    public Mono<String> handleMessageWithArgument(String payload) {
      return delay("handleMessageWithArgument,payload=" + payload);
    }

    public Mono<String> handleMessageWithError() {
      return Mono.delay(Duration.ofMillis(10))
              .flatMap(aLong -> Mono.error(new IllegalStateException("rejected")));
    }

    public Mono<String> handleMessageMatch1() {
      return delay("handleMessageMatch1");
    }

    public Mono<String> handleMessageMatch2() {
      return delay("handleMessageMatch2");
    }

    public Mono<String> handleIllegalStateException(IllegalStateException ex) {
      return delay("handleIllegalStateException,ex=" + ex.getMessage());
    }

    private Mono<String> delay(String value) {
      return Mono.delay(Duration.ofMillis(10)).map(aLong -> value);
    }
  }

  @SuppressWarnings("unused")
  private static class DuplicateMappingsController {

    void handleMessageFoo() { }

    void handleMessageFoo(String foo) { }
  }

  private static class FailingController {

    public Mono<String> fail() {
      return Mono.error(new IllegalStateException("failure"));
    }
  }

  private static class TestAdvice {

    private IllegalArgumentException failure;

    public String handleIllegalArgumentException(IllegalArgumentException failure) {
      this.failure = failure;
      return "recovered";
    }
  }

  private static class TestMethodMessageHandler extends AbstractMethodMessageHandler<String> {

    private TestReturnValueHandler returnValueHandler = new TestReturnValueHandler();

    private PathMatcher pathMatcher = new AntPathMatcher();

    public TestMethodMessageHandler() {
      setHandlerPredicate(handlerType -> handlerType.getName().endsWith("Controller"));
    }

    @Override
    protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
      return Collections.emptyList();
    }

    @Override
    protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
      return Collections.singletonList(this.returnValueHandler);
    }

    public @Nullable Object getLastReturnValue() {
      return this.returnValueHandler.getLastReturnValue();
    }

    public void detectHandlerMethodsForTest(Object handler) {
      detectHandlerMethods(handler);
    }

    @Override
    protected String getMappingForMethod(Method method, Class<?> handlerType) {
      String methodName = method.getName();
      if (methodName.startsWith("handleMessage")) {
        return "/" + methodName;
      }
      return null;
    }

    @Override
    protected Set<String> getDirectLookupMappings(String mapping) {
      return Collections.singleton(mapping);
    }

    @Override
    protected RouteMatcher.@Nullable Route getDestination(Message<?> message) {
      return (RouteMatcher.Route) message.getHeaders().get(
              DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER);
    }

    @Override
    protected String getMatchingMapping(String mapping, Message<?> message) {
      RouteMatcher.Route destination = getDestination(message);
      Assert.notNull(destination, "No destination");
      return mapping.equals(destination.value()) ||
              this.pathMatcher.match(mapping, destination.value()) ? mapping : null;
    }

    @Override
    protected Comparator<String> getMappingComparator(Message<?> message) {
      return (info1, info2) -> {
        SimpleRouteMatcher routeMatcher = new SimpleRouteMatcher(new AntPathMatcher());
        DestinationPatternsMessageCondition cond1 =
                new DestinationPatternsMessageCondition(new String[] { info1 }, routeMatcher);
        DestinationPatternsMessageCondition cond2 =
                new DestinationPatternsMessageCondition(new String[] { info2 }, routeMatcher);
        return cond1.compareTo(cond2, message);
      };
    }

    @Override
    protected AbstractExceptionHandlerMethodResolver createExceptionMethodResolverFor(Class<?> beanType) {
      return new TestExceptionResolver(beanType);
    }
  }

}
