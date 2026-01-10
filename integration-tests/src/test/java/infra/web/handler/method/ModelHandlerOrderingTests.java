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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import infra.core.MethodIntrospector;
import infra.core.annotation.AnnotationUtils;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.ui.Model;
import infra.util.ReflectionUtils;
import infra.web.BindingContext;
import infra.web.annotation.RequestMapping;
import infra.web.bind.annotation.ModelAttribute;
import infra.web.bind.resolver.ModelMethodProcessor;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/20 9:36
 */
class ModelHandlerOrderingTests {
  private static final Logger logger = LoggerFactory.getLogger(ModelHandlerOrderingTests.class);

  private final MockRequestContext webRequest = new MockRequestContext(
          null, new HttpMockRequestImpl(), new MockHttpResponseImpl());

  private final BindingContext mavContainer = new BindingContext();

  @BeforeEach
  void setup() {
    webRequest.setBinding(mavContainer);
    this.mavContainer.addAttribute("methods", new ArrayList<String>());
  }

  @Test
  void straightLineDependency() throws Throwable {
    runTest(new StraightLineDependencyController());
    assertInvokedBefore("getA", "getB1", "getB2", "getC1", "getC2", "getC3", "getC4");
    assertInvokedBefore("getB1", "getB2", "getC1", "getC2", "getC3", "getC4");
    assertInvokedBefore("getB2", "getC1", "getC2", "getC3", "getC4");
    assertInvokedBefore("getC1", "getC2", "getC3", "getC4");
    assertInvokedBefore("getC2", "getC3", "getC4");
    assertInvokedBefore("getC3", "getC4");
  }

  @Test
  void treeDependency() throws Throwable {
    runTest(new TreeDependencyController());
    assertInvokedBefore("getA", "getB1", "getB2", "getC1", "getC2", "getC3", "getC4");
    assertInvokedBefore("getB1", "getC1", "getC2");
    assertInvokedBefore("getB2", "getC3", "getC4");
  }

  @Test
  void InvertedTreeDependency() throws Throwable {
    runTest(new InvertedTreeDependencyController());
    assertInvokedBefore("getC1", "getA", "getB1");
    assertInvokedBefore("getC2", "getA", "getB1");
    assertInvokedBefore("getC3", "getA", "getB2");
    assertInvokedBefore("getC4", "getA", "getB2");
    assertInvokedBefore("getB1", "getA");
    assertInvokedBefore("getB2", "getA");
  }

  @Test
  void unresolvedDependency() throws Throwable {
    runTest(new UnresolvedDependencyController());
    assertInvokedBefore("getA", "getC1", "getC2", "getC3", "getC4");

    // No other order guarantees for methods with unresolvable dependencies (and methods that depend on them),
    // Required dependencies will be created via default constructor.
  }

  private void runTest(Object controller) throws Throwable {
    ParameterResolvingRegistry resolvers = new ParameterResolvingRegistry();
    resolvers.addCustomizedStrategies(new ModelAttributeMethodProcessor(false));
    resolvers.addCustomizedStrategies(new ModelMethodProcessor());
    var parameterFactory = new RegistryResolvableParameterFactory(resolvers);

    Class<?> type = controller.getClass();
    Set<Method> methods = MethodIntrospector.filterMethods(type, METHOD_FILTER);
    List<InvocableHandlerMethod> modelMethods = new ArrayList<>();
    for (Method method : methods) {
      InvocableHandlerMethod modelMethod = new InvocableHandlerMethod(controller, method, parameterFactory);
      modelMethods.add(modelMethod);
    }
    Collections.shuffle(modelMethods);

    ControllerMethodResolver methodResolver = new ControllerMethodResolver(null, parameterFactory);

    ModelHandler factory = new ModelHandler(methodResolver);
    factory.initModel(this.webRequest, this.mavContainer, new HandlerMethod(controller, "handle"));
    if (logger.isDebugEnabled()) {
      logger.debug(String.join(" >> ", getInvokedMethods()));
    }
  }

  private void assertInvokedBefore(String beforeMethod, String... afterMethods) {
    List<String> actual = getInvokedMethods();
    for (String afterMethod : afterMethods) {
      assertThat(actual.indexOf(beforeMethod) < actual.indexOf(afterMethod))
              .as(beforeMethod + " should be before " + afterMethod + ". Actual order: " + actual.toString())
              .isTrue();
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> getInvokedMethods() {
    return (List<String>) this.mavContainer.getModel().get("methods");
  }

  private static class AbstractController {

    @RequestMapping
    public void handle() {
    }

    @SuppressWarnings("unchecked")
    <T> T updateAndReturn(Model model, String methodName, T returnValue) throws IOException {
      ((List<String>) model.asMap().get("methods")).add(methodName);
      return returnValue;
    }
  }

  private static class StraightLineDependencyController extends AbstractController {

    @ModelAttribute
    public A getA(Model model) throws IOException {
      return updateAndReturn(model, "getA", new A());
    }

    @ModelAttribute
    public B1 getB1(@ModelAttribute A a, Model model) throws IOException {
      return updateAndReturn(model, "getB1", new B1());
    }

    @ModelAttribute
    public B2 getB2(@ModelAttribute B1 b1, Model model) throws IOException {
      return updateAndReturn(model, "getB2", new B2());
    }

    @ModelAttribute
    public C1 getC1(@ModelAttribute B2 b2, Model model) throws IOException {
      return updateAndReturn(model, "getC1", new C1());
    }

    @ModelAttribute
    public C2 getC2(@ModelAttribute C1 c1, Model model) throws IOException {
      return updateAndReturn(model, "getC2", new C2());
    }

    @ModelAttribute
    public C3 getC3(@ModelAttribute C2 c2, Model model) throws IOException {
      return updateAndReturn(model, "getC3", new C3());
    }

    @ModelAttribute
    public C4 getC4(@ModelAttribute C3 c3, Model model) throws IOException {
      return updateAndReturn(model, "getC4", new C4());
    }
  }

  private static class TreeDependencyController extends AbstractController {

    @ModelAttribute
    public A getA(Model model) throws IOException {
      return updateAndReturn(model, "getA", new A());
    }

    @ModelAttribute
    public B1 getB1(@ModelAttribute A a, Model model) throws IOException {
      return updateAndReturn(model, "getB1", new B1());
    }

    @ModelAttribute
    public B2 getB2(@ModelAttribute A a, Model model) throws IOException {
      return updateAndReturn(model, "getB2", new B2());
    }

    @ModelAttribute
    public C1 getC1(@ModelAttribute B1 b1, Model model) throws IOException {
      return updateAndReturn(model, "getC1", new C1());
    }

    @ModelAttribute
    public C2 getC2(@ModelAttribute B1 b1, Model model) throws IOException {
      return updateAndReturn(model, "getC2", new C2());
    }

    @ModelAttribute
    public C3 getC3(@ModelAttribute B2 b2, Model model) throws IOException {
      return updateAndReturn(model, "getC3", new C3());
    }

    @ModelAttribute
    public C4 getC4(@ModelAttribute B2 b2, Model model) throws IOException {
      return updateAndReturn(model, "getC4", new C4());
    }
  }

  private static class InvertedTreeDependencyController extends AbstractController {

    @ModelAttribute
    public C1 getC1(Model model) throws IOException {
      return updateAndReturn(model, "getC1", new C1());
    }

    @ModelAttribute
    public C2 getC2(Model model) throws IOException {
      return updateAndReturn(model, "getC2", new C2());
    }

    @ModelAttribute
    public C3 getC3(Model model) throws IOException {
      return updateAndReturn(model, "getC3", new C3());
    }

    @ModelAttribute
    public C4 getC4(Model model) throws IOException {
      return updateAndReturn(model, "getC4", new C4());
    }

    @ModelAttribute
    public B1 getB1(@ModelAttribute C1 c1, @ModelAttribute C2 c2, Model model) throws IOException {
      return updateAndReturn(model, "getB1", new B1());
    }

    @ModelAttribute
    public B2 getB2(@ModelAttribute C3 c3, @ModelAttribute C4 c4, Model model) throws IOException {
      return updateAndReturn(model, "getB2", new B2());
    }

    @ModelAttribute
    public A getA(@ModelAttribute B1 b1, @ModelAttribute B2 b2, Model model) throws IOException {
      return updateAndReturn(model, "getA", new A());
    }

  }

  private static class UnresolvedDependencyController extends AbstractController {

    @ModelAttribute
    public A getA(Model model) throws IOException {
      return updateAndReturn(model, "getA", new A());
    }

    @ModelAttribute
    public C1 getC1(@ModelAttribute B1 b1, Model model) throws IOException {
      return updateAndReturn(model, "getC1", new C1());
    }

    @ModelAttribute
    public C2 getC2(@ModelAttribute B1 b1, Model model) throws IOException {
      return updateAndReturn(model, "getC2", new C2());
    }

    @ModelAttribute
    public C3 getC3(@ModelAttribute B2 b2, Model model) throws IOException {
      return updateAndReturn(model, "getC3", new C3());
    }

    @ModelAttribute
    public C4 getC4(@ModelAttribute B2 b2, Model model) throws IOException {
      return updateAndReturn(model, "getC4", new C4());
    }
  }

  private static class A { }

  private static class B1 { }

  private static class B2 { }

  private static class C1 { }

  private static class C2 { }

  private static class C3 { }

  private static class C4 { }

  private static final ReflectionUtils.MethodFilter METHOD_FILTER = method ->
          ((AnnotationUtils.findAnnotation(method, RequestMapping.class) == null) &&
                  (AnnotationUtils.findAnnotation(method, ModelAttribute.class) != null));

}
