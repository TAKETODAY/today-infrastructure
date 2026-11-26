/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler.method;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.util.concurrent.atomic.AtomicInteger;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.core.Ordered;
import infra.core.ParameterNameDiscoverer;
import infra.stereotype.Controller;
import infra.web.BindingContext;
import infra.web.ServerHttpExchange;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.RequestMapping;
import infra.web.bind.WebDataBinder;
import infra.web.bind.annotation.InitBinder;
import infra.web.bind.annotation.ModelAttribute;
import infra.web.handler.CompositeHandlerExceptionHandler;
import infra.web.mock.MockServerHttpExchange;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.testfixture.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerMethodResolverTests {

  private final ControllerMethodResolver resolver = new ControllerMethodResolver(null, null);

  @Test
  void defaultMvcSharesResolverAndItsParameterConfiguration() {
    try (var context = new AnnotationConfigApplicationContext(RequestMappingHandlerAdapterTests.Config.class)) {
      var mapping = context.getBean(RequestMappingHandlerMapping.class);
      var adapter = context.getBean(RequestMappingHandlerAdapter.class);
      var exceptionHandlers = context.getBean(CompositeHandlerExceptionHandler.class).getExceptionHandlers();
      var exceptionHandler = exceptionHandlers.stream()
              .filter(ExceptionHandlerAnnotationExceptionHandler.class::isInstance)
              .findFirst().orElseThrow();

      var shared = ReflectionTestUtils.getField(mapping, "methodResolver");
      assertThat(shared).isInstanceOf(ControllerMethodResolver.class);
      assertThat(ReflectionTestUtils.getField(adapter, "methodResolver")).isSameAs(shared);
      assertThat(ReflectionTestUtils.getField(exceptionHandler, "methodResolver")).isSameAs(shared);
      assertThat(ReflectionTestUtils.getField(shared, "parameterRegistry"))
              .isSameAs(context.getBean(ParameterResolvingRegistry.class));
      assertThat(ReflectionTestUtils.getField(shared, "parameterFactory")).isInstanceOf(RegistryHandlerParameterFactory.class);
    }
  }

  @Test
  void defaultMvcDiscoversMethodAdviceOncePerContext() throws Exception {
    try (var first = new AnnotationConfigApplicationContext(); var second = new AnnotationConfigApplicationContext()) {
      var firstAdvice = new CountingAdvice("first");
      var secondAdvice = new CountingAdvice("second");
      for (var context : new AnnotationConfigApplicationContext[] { first, second }) {
        context.register(RequestMappingHandlerAdapterTests.Config.class);
        context.registerSingleton("countingAdvice", context == first ? firstAdvice : secondAdvice);
        context.registerSingleton("otherAdvice", new OtherAdvice());
        context.refresh();
      }

      var handler = handler(new TestController());
      var firstRequest = metadataRequest();
      first.getBean(RequestMappingHandlerAdapter.class).handle(firstRequest, handler);
      var secondRequest = metadataRequest();
      second.getBean(RequestMappingHandlerAdapter.class).handle(secondRequest, handler);
      assertThat(BindingContext.obtain(firstRequest).getModel()).containsEntry("contextAdvice", "first");
      assertThat(BindingContext.obtain(secondRequest).getModel()).containsEntry("contextAdvice", "second");
      assertThat(firstAdvice.orderLookups.get()).isEqualTo(1);
      assertThat(secondAdvice.orderLookups.get()).isEqualTo(1);
    }
  }

  @Test
  void mappingRegistrationDoesNotDiscoverMethodAdvice() throws Exception {
    try (var context = new AnnotationConfigApplicationContext()) {
      var advice = new CountingAdvice("mapping");
      context.registerSingleton("countingAdvice", advice);
      context.registerSingleton("otherAdvice", new OtherAdvice());
      context.register(MappedController.class);
      context.refresh();

      var resolver = new ControllerMethodResolver(context, null);
      var mapping = new RequestMappingHandlerMapping();
      mapping.setApplicationContext(context);
      mapping.setControllerMethodResolver(resolver);
      mapping.afterPropertiesSet();
      assertThat(mapping.getHandlerInternal(metadataRequest())).isInstanceOf(InvocableHandlerMethod.class);
      assertThat(advice.orderLookups.get()).isZero();

      var adapter = new RequestMappingHandlerAdapter();
      adapter.setApplicationContext(context);
      adapter.setControllerMethodResolver(resolver);
      adapter.afterPropertiesSet();
      assertThat(advice.orderLookups.get()).isEqualTo(1);
    }
  }

  @Test
  void adaptersSharingResolverKeepBindingParameterConfigurationSeparate() throws Exception {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.refresh();
      var resolver = new ControllerMethodResolver(context, null);
      var first = adapter(context, resolver, "first");
      var second = adapter(context, resolver, "second");
      var handler = handler(new ParameterizedBindingController());

      for (var adapter : new RequestMappingHandlerAdapter[] { first, second, first }) {
        String expected = adapter == first ? "first" : "second";
        var request = metadataRequest();
        adapter.handle(request, handler);
        var binding = BindingContext.obtain(request);
        assertThat(binding.getModel()).containsEntry("bindingValue", expected);
        assertThat(binding.createBinder(request, "test").getDisallowedFields()).containsExactly(expected);
      }
    }
  }

  private static RequestMappingHandlerAdapter adapter(AnnotationConfigApplicationContext context,
          ControllerMethodResolver resolver, String name) {
    var registry = new ParameterResolvingRegistry();
    registry.addCustomizedStrategies(new ParameterResolvingStrategy() {
      @Override
      public boolean supportsParameter(HandlerParameter parameter) {
        return name.equals(parameter.getName());
      }

      @Override
      public Object resolveArgument(ServerHttpExchange exchange, HandlerParameter parameter) {
        return name;
      }
    });
    var adapter = new RequestMappingHandlerAdapter();
    adapter.setApplicationContext(context);
    adapter.setControllerMethodResolver(resolver.withParameterConfiguration(registry, new ParameterNameDiscoverer() {
      @Override
      protected String[] doGet(Executable executable) {
        return executable.getParameterCount() == 2 ? new String[] { "binder", name } : new String[] { name };
      }
    }));
    adapter.afterPropertiesSet();
    return adapter;
  }

  @Test
  void metadataIsCachedOnFirstAccess() throws Exception {
    InvocableHandlerMethod handler = handler(new TestController());

    assertThat(handler.getBindingMethods(true)).isNull();
    assertThat(handler.getBindingMethods(false)).isNull();

    var binders = resolver.getBinderMethods(handler);
    var modelMethods = resolver.getModelAttributeMethods(handler);
    assertThat(binders).hasSize(1);
    assertThat(modelMethods).hasSize(1);
    assertThat(resolver.createHandlerMethod(handler)).isSameAs(handler);
    assertThat(resolver.getBinderMethods(handler)).isSameAs(binders);
    assertThat(resolver.getModelAttributeMethods(handler)).isSameAs(modelMethods);
  }

  @Test
  void dynamicallyRegisteredHandlerIsReusedAcrossRequests() throws Exception {
    try (var context = new AnnotationConfigApplicationContext(RequestMappingHandlerAdapterTests.Config.class)) {
      var mapping = context.getBean(RequestMappingHandlerMapping.class);
      var adapter = context.getBean(RequestMappingHandlerAdapter.class);
      var info = RequestMappingInfo.paths("/metadata").options(mapping.getBuilderConfiguration()).build();
      mapping.registerMapping(info, new TestController(), TestController.class.getMethod("handle"));

      var handler = (InvocableHandlerMethod) mapping.getHandlerMethods().get(info);
      assertThat(handler.getBindingMethods(false)).isNull();

      var firstRequest = metadataRequest();
      var secondRequest = metadataRequest();
      assertThat(mapping.getHandlerInternal(firstRequest)).isSameAs(handler);
      adapter.handle(firstRequest, handler);
      var modelMethods = handler.getBindingMethods(false);
      assertThat(modelMethods.methods()).hasSize(1);

      assertThat(mapping.getHandlerInternal(secondRequest)).isSameAs(handler);
      adapter.handle(secondRequest, handler);
      assertThat(handler.getBindingMethods(false)).isSameAs(modelMethods);
      assertThat(BindingContext.obtain(secondRequest)).isNotSameAs(BindingContext.obtain(firstRequest));
    }
  }

  @Test
  void customMappingKeepsItsParameterResolutionConfiguration() throws Exception {
    try (var context = new AnnotationConfigApplicationContext(RequestMappingHandlerAdapterTests.Config.class)) {
      var mapping = new RequestMappingHandlerMapping() {
        @Override
        protected void initHandlerMethods() {
          // Programmatic registration only, without an ApplicationContext.
        }
      };
      var registry = new ParameterResolvingRegistry();
      registry.addCustomizedStrategies(new ParameterResolvingStrategy() {
        @Override
        public boolean supportsParameter(HandlerParameter parameter) {
          return "customName".equals(parameter.getName());
        }

        @Override
        public Object resolveArgument(ServerHttpExchange exchange, HandlerParameter parameter) {
          return "custom value";
        }
      });
      mapping.setControllerMethodResolver(new ControllerMethodResolver(null, registry, new ParameterNameDiscoverer() {
        @Override
        protected String[] doGet(Executable executable) {
          return new String[] { "customName" };
        }
      }));
      mapping.afterPropertiesSet();
      var info = RequestMappingInfo.paths("/metadata").options(mapping.getBuilderConfiguration()).build();
      mapping.registerMapping(info, new TestController(), TestController.class.getMethod("echo", String.class));

      var request = metadataRequest();
      var handler = mapping.getHandlerInternal(request);
      assertThat(handler).isInstanceOf(InvocableHandlerMethod.class).isSameAs(mapping.getHandlerMethods().get(info));
      assertThat(context.getBean(RequestMappingHandlerAdapter.class).handle(request, handler)).isEqualTo("custom value");
    }
  }

  @Test
  void mappingsResolveInheritedParametersAgainstEachControllerType() throws Exception {
    try (var context = new AnnotationConfigApplicationContext(RequestMappingHandlerAdapterTests.Config.class)) {
      var adapter = context.getBean(RequestMappingHandlerAdapter.class);
      var registry = new ParameterResolvingRegistry();
      registry.addCustomizedStrategies(new ParameterResolvingStrategy() {
        @Override
        public boolean supportsParameter(HandlerParameter parameter) {
          return true;
        }

        @Override
        public Object resolveArgument(ServerHttpExchange exchange, HandlerParameter parameter) {
          return parameter.getParameterType() == String.class ? "text" : 42;
        }
      });

      var customMapping = new RequestMappingHandlerMapping();
      customMapping.setApplicationContext(context);
      customMapping.setControllerMethodResolver(new ControllerMethodResolver(context, registry));
      customMapping.afterPropertiesSet();
      var inherited = GenericController.class.getMethod("echo", Object.class);
      var text = RequestMappingInfo.paths("/text").options(customMapping.getBuilderConfiguration()).build();
      var number = RequestMappingInfo.paths("/number").options(customMapping.getBuilderConfiguration()).build();
      customMapping.registerMapping(text, new TextController(), inherited);
      customMapping.registerMapping(number, new NumberController(), inherited);

      var textRequest = new MockServerHttpExchange(null, new MockRequest("GET", "/text"), new MockResponse());
      var numberRequest = new MockServerHttpExchange(null, new MockRequest("GET", "/number"), new MockResponse());
      assertThat(adapter.handle(textRequest, customMapping.getHandlerInternal(textRequest))).isEqualTo("text");
      assertThat(adapter.handle(numberRequest, customMapping.getHandlerInternal(numberRequest))).isEqualTo(42);
    }
  }

  @Test
  void validationCopyRetainsMetadataButBeanCopyUsesNewController() throws Exception {
    InvocableHandlerMethod original = handler(new TestController());
    resolver.getBinderMethods(original);
    resolver.getModelAttributeMethods(original);

    InvocableHandlerMethod validated = (InvocableHandlerMethod) original.withValidateFlags();
    assertThat(validated.getBindingMethods(true)).isSameAs(original.getBindingMethods(true));
    assertThat(validated.getBindingMethods(false)).isSameAs(original.getBindingMethods(false));

    TestController replacement = new TestController();
    InvocableHandlerMethod rebound = (InvocableHandlerMethod) original.withBean(replacement);
    assertThat(rebound.getBindingMethods(true)).isNull();
    assertThat(rebound.getBindingMethods(false)).isNull();
    assertThat(resolver.getBinderMethods(rebound)).allSatisfy(method -> assertThat(method.getBean()).isSameAs(replacement));
    assertThat(resolver.getModelAttributeMethods(rebound)).allSatisfy(method -> assertThat(method.getBean()).isSameAs(replacement));
  }

  @Test
  void plainHandlerIsWrappedWithoutGlobalCache() throws Exception {
    HandlerMethod handler = new HandlerMethod(new TestController(), "handle");

    InvocableHandlerMethod first = resolver.createHandlerMethod(handler);
    InvocableHandlerMethod second = resolver.createHandlerMethod(handler);
    assertThat(first).isNotSameAs(second);
    assertThat(first.getBindingMethods(true)).isNull();
    assertThat(first.getBindingMethods(false)).isNull();
  }

  @Test
  void prototypeAdviceInstancesAreNotStoredInFields() throws Exception {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerPrototype("advice", PrototypeAdvice.class);
      context.refresh();
      var resolver = new ControllerMethodResolver(context, null);
      InvocableHandlerMethod handler = handler(new TestController());

      var firstBinders = resolver.getBinderMethods(handler);
      var firstModels = resolver.getModelAttributeMethods(handler);
      assertThat(resolver.getBinderMethods(handler).get(0).getBean()).isNotSameAs(firstBinders.get(0).getBean());
      assertThat(resolver.getModelAttributeMethods(handler).get(0).getBean()).isNotSameAs(firstModels.get(0).getBean());
      assertThat(handler.getBindingMethods(true)).isNull();
      assertThat(handler.getBindingMethods(false)).isNull();
    }
  }

  @Test
  void startupRegisteredHandlerResolvesMetadataOnDemand() throws Exception {
    try (var context = new AnnotationConfigApplicationContext(
            RequestMappingHandlerAdapterTests.Config.class, MappedController.class)) {
      var mapping = context.getBean(RequestMappingHandlerMapping.class);
      var adapter = context.getBean(RequestMappingHandlerAdapter.class);
      assertThat(mapping.getHandlerMethods()).hasSize(1);
      var handler = (InvocableHandlerMethod) mapping.getHandlerMethods().values().iterator().next();
      assertThat(handler.getBindingMethods(true)).isNull();
      assertThat(handler.getBindingMethods(false)).isNull();

      var request = metadataRequest();
      assertThat(mapping.getHandlerInternal(request)).isSameAs(handler);
      adapter.handle(request, handler);
      var binding = BindingContext.obtain(request);
      assertThat(handler.getBindingMethods(false).methods()).hasSize(1);
      assertThat(handler.getBindingMethods(true)).isNull();
      assertThat(binding.getModel()).containsEntry("string", "value");

      binding.createBinder(request, "test");
      assertThat(handler.getBindingMethods(true).methods()).hasSize(1);
    }
  }

  @Test
  void prototypeControllerKeepsTheInvocableTypeWhenResolvingItsBean() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(RequestMappingHandlerAdapterTests.Config.class);
      context.registerPrototype("prototypeController", MappedController.class);
      context.refresh();
      var mapping = context.getBean(RequestMappingHandlerMapping.class);
      var registered = (InvocableHandlerMethod) mapping.getHandlerMethods().values().iterator().next();
      assertThat(registered.getBean()).isEqualTo("prototypeController");
      assertThat(registered.getBindingMethods(true)).isNull();
      assertThat(registered.getBindingMethods(false)).isNull();

      var first = mapping.getHandlerInternal(metadataRequest());
      var second = mapping.getHandlerInternal(metadataRequest());
      assertThat(first).isInstanceOf(InvocableHandlerMethod.class).isNotSameAs(registered);
      assertThat(second).isInstanceOf(InvocableHandlerMethod.class).isNotSameAs(first);
      assertThat(first.getBean()).isInstanceOf(MappedController.class).isNotSameAs(second.getBean());
    }
  }

  private static MockServerHttpExchange metadataRequest() {
    return new MockServerHttpExchange(null, new MockRequest("GET", "/metadata"), new MockResponse());
  }

  private InvocableHandlerMethod handler(TestController controller) throws Exception {
    return resolver.createHandlerMethod(controller, TestController.class.getDeclaredMethod("handle"), null);
  }

  static class TestController {

    public void handle() {
    }

    public String echo(String value) {
      return value;
    }

    @InitBinder
    public void binder() {
    }

    @ModelAttribute
    public String attribute() {
      return "value";
    }
  }

  @Controller
  static class MappedController extends TestController {

    @Override
    @RequestMapping("/metadata")
    public void handle() {
    }
  }

  static class ParameterizedBindingController extends TestController {

    @ModelAttribute("bindingValue")
    public String bindingValue(String value) {
      return value;
    }

    @InitBinder
    public void bindingOptions(WebDataBinder binder, String value) {
      binder.setDisallowedFields(value);
    }
  }

  static class GenericController<T> {

    public T echo(T value) {
      return value;
    }
  }

  static class TextController extends GenericController<String> {
  }

  static class NumberController extends GenericController<Integer> {
  }

  @ControllerAdvice
  static class CountingAdvice implements Ordered {

    private final String value;

    private final AtomicInteger orderLookups = new AtomicInteger();

    CountingAdvice(String value) {
      this.value = value;
    }

    @Override
    public int getOrder() {
      this.orderLookups.incrementAndGet();
      return 0;
    }

    @ModelAttribute("contextAdvice")
    public String contextAdvice() {
      return this.value;
    }
  }

  @ControllerAdvice
  static class OtherAdvice {
  }

  @ControllerAdvice
  static class PrototypeAdvice {

    @InitBinder
    public void binder() {
    }

    @ModelAttribute
    public String attribute() {
      return "advice";
    }
  }
}
