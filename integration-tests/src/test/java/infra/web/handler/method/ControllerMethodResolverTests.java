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

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.stereotype.Controller;
import infra.web.BindingContext;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.RequestMapping;
import infra.web.bind.annotation.InitBinder;
import infra.web.bind.annotation.ModelAttribute;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerMethodResolverTests {

  private final HandlerParameterFactory parameterFactory = new HandlerParameterFactory();

  private final ControllerMethodResolver resolver = new ControllerMethodResolver(null, parameterFactory);

  @Test
  void metadataIsCachedOnFirstAccess() throws Exception {
    InvocableHandlerMethod handler = handler(new TestController());

    assertThat(handler.getCachedBinderMethods()).isNull();
    assertThat(handler.getCachedModelAttributeMethods()).isNull();

    var binders = resolver.getBinderMethods(handler);
    var modelMethods = resolver.getModelAttributeMethods(handler);
    assertThat(binders).hasSize(1).isSameAs(handler.getCachedBinderMethods());
    assertThat(modelMethods).hasSize(1).isSameAs(handler.getCachedModelAttributeMethods());
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
      assertThat(handler.getCachedModelAttributeMethods()).isNull();

      var firstRequest = metadataRequest();
      var secondRequest = metadataRequest();
      assertThat(mapping.getHandlerInternal(firstRequest)).isSameAs(handler);
      adapter.handle(firstRequest, handler);
      var modelMethods = handler.getCachedModelAttributeMethods();
      assertThat(modelMethods).hasSize(1);

      assertThat(mapping.getHandlerInternal(secondRequest)).isSameAs(handler);
      adapter.handle(secondRequest, handler);
      assertThat(handler.getCachedModelAttributeMethods()).isSameAs(modelMethods);
      assertThat(BindingContext.obtain(secondRequest)).isNotSameAs(BindingContext.obtain(firstRequest));
    }
  }

  @Test
  void validationCopyRetainsMetadataButBeanCopyUsesNewController() throws Exception {
    InvocableHandlerMethod original = handler(new TestController());
    resolver.getBinderMethods(original);
    resolver.getModelAttributeMethods(original);

    InvocableHandlerMethod validated = (InvocableHandlerMethod) original.withValidateFlags();
    assertThat(validated.getCachedBinderMethods()).isSameAs(original.getCachedBinderMethods());
    assertThat(validated.getCachedModelAttributeMethods()).isSameAs(original.getCachedModelAttributeMethods());

    TestController replacement = new TestController();
    InvocableHandlerMethod rebound = (InvocableHandlerMethod) original.withBean(replacement);
    assertThat(rebound.getCachedBinderMethods()).isNull();
    assertThat(rebound.getCachedModelAttributeMethods()).isNull();
    assertThat(resolver.getBinderMethods(rebound)).allSatisfy(method -> assertThat(method.getBean()).isSameAs(replacement));
    assertThat(resolver.getModelAttributeMethods(rebound)).allSatisfy(method -> assertThat(method.getBean()).isSameAs(replacement));
  }

  @Test
  void plainHandlerIsWrappedWithoutGlobalCache() throws Exception {
    HandlerMethod handler = new HandlerMethod(new TestController(), "handle");

    InvocableHandlerMethod first = resolver.createHandlerMethod(handler);
    InvocableHandlerMethod second = resolver.createHandlerMethod(handler);
    assertThat(first).isNotSameAs(second);
    assertThat(first.getCachedBinderMethods()).isNull();
    assertThat(first.getCachedModelAttributeMethods()).isNull();
  }

  @Test
  void prototypeAdviceInstancesAreNotStoredInFields() throws Exception {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerPrototype("advice", PrototypeAdvice.class);
      context.refresh();
      var resolver = new ControllerMethodResolver(context, parameterFactory);
      InvocableHandlerMethod handler = handler(new TestController());

      assertThat(handler.getCachedBinderMethods()).isNull();
      assertThat(handler.getCachedModelAttributeMethods()).isNull();
      var firstBinders = resolver.getBinderMethods(handler);
      var firstModels = resolver.getModelAttributeMethods(handler);
      assertThat(resolver.getBinderMethods(handler).get(0).getBean()).isNotSameAs(firstBinders.get(0).getBean());
      assertThat(resolver.getModelAttributeMethods(handler).get(0).getBean()).isNotSameAs(firstModels.get(0).getBean());
      assertThat(handler.getCachedBinderMethods()).isNull();
      assertThat(handler.getCachedModelAttributeMethods()).isNull();
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
      assertThat(handler.getCachedBinderMethods()).isNull();
      assertThat(handler.getCachedModelAttributeMethods()).isNull();

      var request = metadataRequest();
      assertThat(mapping.getHandlerInternal(request)).isSameAs(handler);
      adapter.handle(request, handler);
      var binding = BindingContext.obtain(request);
      assertThat(handler.getCachedModelAttributeMethods()).hasSize(1);
      assertThat(handler.getCachedBinderMethods()).isNull();
      assertThat(binding.getModel()).containsEntry("string", "value");

      binding.createBinder(request, "test");
      assertThat(handler.getCachedBinderMethods()).hasSize(1);
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
      assertThat(registered.getCachedBinderMethods()).isNull();
      assertThat(registered.getCachedModelAttributeMethods()).isNull();

      var first = mapping.getHandlerInternal(metadataRequest());
      var second = mapping.getHandlerInternal(metadataRequest());
      assertThat(first).isInstanceOf(InvocableHandlerMethod.class).isNotSameAs(registered);
      assertThat(second).isInstanceOf(InvocableHandlerMethod.class).isNotSameAs(first);
      assertThat(first.getBean()).isInstanceOf(MappedController.class).isNotSameAs(second.getBean());
    }
  }

  private static MockHttpContext metadataRequest() {
    return new MockHttpContext(null, new MockRequest("GET", "/metadata"), new MockResponse());
  }

  private InvocableHandlerMethod handler(TestController controller) throws Exception {
    return new InvocableHandlerMethod(controller, TestController.class.getDeclaredMethod("handle"), parameterFactory);
  }

  static class TestController {

    public void handle() {
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
