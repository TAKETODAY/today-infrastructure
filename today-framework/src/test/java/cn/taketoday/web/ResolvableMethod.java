/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web;

import org.aopalliance.intercept.MethodInterceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.target.EmptyTargetSource;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.core.LocalVariableTableParameterNameDiscoverer;
import cn.taketoday.core.MethodIntrospector;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.core.bytecode.core.DefaultNamingPolicy;
import cn.taketoday.core.bytecode.proxy.Callback;
import cn.taketoday.core.bytecode.proxy.Enhancer;
import cn.taketoday.core.bytecode.proxy.Factory;
import cn.taketoday.core.bytecode.proxy.MethodProxy;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

import static java.util.stream.Collectors.joining;

/**
 * Convenience class to resolve to a Method and method parameters.
 *
 * <p>Note that a replica of this class also exists in spring-messaging.
 *
 * <h1>Background</h1>
 *
 * <p>When testing annotated methods we create test classes such as
 * "TestController" with a diverse range of method signatures representing
 * supported annotations and argument types. It becomes challenging to use
 * naming strategies to keep track of methods and arguments especially in
 * combination with variables for reflection metadata.
 *
 * <p>The idea with {@link ResolvableMethod} is NOT to rely on naming techniques
 * but to use hints to zero in on method parameters. Such hints can be strongly
 * typed and explicit about what is being tested.
 *
 * <h2>1. Declared Return Type</h2>
 *
 * When testing return types it's likely to have many methods with a unique
 * return type, possibly with or without an annotation.
 *
 * <pre>
 * import static cn.taketoday.web.method.ResolvableMethod.on;
 * import static cn.taketoday.web.method.MvcAnnotationPredicates.requestMapping;
 *
 * // Return type
 * on(TestController.class).resolveReturnType(Foo.class);
 * on(TestController.class).resolveReturnType(List.class, Foo.class);
 * on(TestController.class).resolveReturnType(Mono.class, responseEntity(Foo.class));
 *
 * // Annotation + return type
 * on(TestController.class).annotPresent(RequestMapping.class).resolveReturnType(Bar.class);
 *
 * // Annotation not present
 * on(TestController.class).annotNotPresent(RequestMapping.class).resolveReturnType();
 *
 * // Annotation with attributes
 * on(TestController.class).annot(requestMapping("/foo").params("p")).resolveReturnType();
 * </pre>
 *
 * <h2>2. Method Arguments</h2>
 *
 * When testing method arguments it's more likely to have one or a small number
 * of methods with a wide array of argument types and parameter annotations.
 *
 * <pre>
 * import static cn.taketoday.web.method.MvcAnnotationPredicates.requestParam;
 *
 * ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();
 *
 * testMethod.arg(Foo.class);
 * testMethod.annotPresent(RequestParam.class).arg(Integer.class);
 * testMethod.annotNotPresent(RequestParam.class)).arg(Integer.class);
 * testMethod.annot(requestParam().name("c").notRequired()).arg(Integer.class);
 * </pre>
 *
 * <h3>3. Mock Handler Method Invocation</h3>
 *
 * Locate a method by invoking it through a proxy of the target handler:
 *
 * <pre>
 * ResolvableMethod.on(TestController.class).mockCall(o -> o.handle(null)).method();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 16:01
 */
public class ResolvableMethod {

  private static final Logger logger = LoggerFactory.getLogger(ResolvableMethod.class);

  private static final ParameterNameDiscoverer nameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

  // Matches ValueConstants.DEFAULT_NONE
  private static final String DEFAULT_VALUE_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

  private final Method method;

  private ResolvableMethod(Method method) {
    Assert.notNull(method, "'method' is required");
    this.method = method;
  }

  /**
   * Return the resolved method.
   */
  public Method method() {
    return this.method;
  }

  /**
   * Return the declared return type of the resolved method.
   */
  public MethodParameter returnType() {
    return new SynthesizingMethodParameter(this.method, -1);
  }

  /**
   * Find a unique argument matching the given type.
   *
   * @param type the expected type
   * @param generics optional array of generic types
   */
  public ResolvableMethodParameter arg(Class<?> type, Class<?>... generics) {
    return new ArgResolver().arg(type, generics);
  }

  /**
   * Find a unique argument matching the given type.
   *
   * @param type the expected type
   * @param generic at least one generic type
   * @param generics optional array of generic types
   */
  public ResolvableMethodParameter arg(Class<?> type, ResolvableType generic, ResolvableType... generics) {
    return new ArgResolver().arg(type, generic, generics);
  }

  /**
   * Find a unique argument matching the given type.
   *
   * @param type the expected type
   */
  public ResolvableMethodParameter arg(ResolvableType type) {
    return new ArgResolver().arg(type);
  }

  /**
   * Filter on method arguments with annotation.
   */
  @SafeVarargs
  public final ArgResolver annot(Predicate<MethodParameter>... filter) {
    return new ArgResolver(filter);
  }

  @SafeVarargs
  public final ArgResolver annotPresent(Class<? extends Annotation>... annotationTypes) {
    return new ArgResolver().annotPresent(annotationTypes);
  }

  /**
   * Filter on method arguments that don't have the given annotation type(s).
   *
   * @param annotationTypes the annotation types
   */
  @SafeVarargs
  public final ArgResolver annotNotPresent(Class<? extends Annotation>... annotationTypes) {
    return new ArgResolver().annotNotPresent(annotationTypes);
  }

  @Override
  public String toString() {
    return "ResolvableMethod=" + formatMethod();
  }

  private String formatMethod() {
    return (method().getName() +
            Arrays.stream(this.method.getParameters())
                    .map(this::formatParameter)
                    .collect(joining(",\n\t", "(\n\t", "\n)")));
  }

  private String formatParameter(Parameter param) {
    Annotation[] anns = param.getAnnotations();
    return (anns.length > 0 ?
            Arrays.stream(anns).map(this::formatAnnotation).collect(joining(",", "[", "]")) + " " + param :
            param.toString());
  }

  private String formatAnnotation(Annotation annotation) {
    Map<String, Object> map = AnnotationUtils.getAnnotationAttributes(annotation);
    map.forEach((key, value) -> {
      if (value.equals(DEFAULT_VALUE_NONE)) {
        map.put(key, "NONE");
      }
    });
    return annotation.annotationType().getName() + map;
  }

  private static ResolvableType toResolvableType(Class<?> type, Class<?>... generics) {
    return (ObjectUtils.isEmpty(generics) ? ResolvableType.fromClass(type) :
            ResolvableType.fromClassWithGenerics(type, generics));
  }

  private static ResolvableType toResolvableType(Class<?> type, ResolvableType generic, ResolvableType... generics) {
    ResolvableType[] genericTypes = new ResolvableType[generics.length + 1];
    genericTypes[0] = generic;
    System.arraycopy(generics, 0, genericTypes, 1, generics.length);
    return ResolvableType.fromClassWithGenerics(type, genericTypes);
  }

  /**
   * Create a {@code ResolvableMethod} builder for the given handler class.
   */
  public static <T> Builder<T> on(Class<T> objectClass) {
    return new Builder<>(objectClass);
  }

  /**
   * Builder for {@code ResolvableMethod}.
   */
  public static class Builder<T> {

    private final Class<?> objectClass;

    private final List<Predicate<Method>> filters = new ArrayList<>(4);

    private Builder(Class<?> objectClass) {
      Assert.notNull(objectClass, "Class must not be null");
      this.objectClass = objectClass;
    }

    private void addFilter(String message, Predicate<Method> filter) {
      this.filters.add(new LabeledPredicate<>(message, filter));
    }

    /**
     * Filter on methods with the given name.
     */
    public Builder<T> named(String methodName) {
      addFilter("methodName=" + methodName, method -> method.getName().equals(methodName));
      return this;
    }

    /**
     * Filter on methods with the given parameter types.
     */
    public Builder<T> argTypes(Class<?>... argTypes) {
      addFilter("argTypes=" + Arrays.toString(argTypes), method ->
              ObjectUtils.isEmpty(argTypes) ? method.getParameterCount() == 0 :
              Arrays.equals(method.getParameterTypes(), argTypes));
      return this;
    }

    /**
     * Filter on annotated methods.
     */
    @SafeVarargs
    public final Builder<T> annot(Predicate<Method>... filters) {
      this.filters.addAll(Arrays.asList(filters));
      return this;
    }

    /**
     * Filter on methods annotated with the given annotation type.
     *
     * @see #annot(Predicate[])
     */
    @SafeVarargs
    public final Builder<T> annotPresent(Class<? extends Annotation>... annotationTypes) {
      String message = "annotationPresent=" + Arrays.toString(annotationTypes);
      addFilter(message, method ->
              Arrays.stream(annotationTypes).allMatch(annotType ->
                      AnnotatedElementUtils.findMergedAnnotation(method, annotType) != null));
      return this;
    }

    /**
     * Filter on methods not annotated with the given annotation type.
     */
    @SafeVarargs
    public final Builder<T> annotNotPresent(Class<? extends Annotation>... annotationTypes) {
      String message = "annotationNotPresent=" + Arrays.toString(annotationTypes);
      addFilter(message, method -> {
        if (annotationTypes.length != 0) {
          return Arrays.stream(annotationTypes).noneMatch(annotType ->
                  AnnotatedElementUtils.findMergedAnnotation(method, annotType) != null);
        }
        else {
          return method.getAnnotations().length == 0;
        }
      });
      return this;
    }

    /**
     * Filter on methods returning the given type.
     *
     * @param returnType the return type
     * @param generics optional array of generic types
     */
    public Builder<T> returning(Class<?> returnType, Class<?>... generics) {
      return returning(toResolvableType(returnType, generics));
    }

    /**
     * Filter on methods returning the given type with generics.
     *
     * @param returnType the return type
     * @param generic at least one generic type
     * @param generics optional extra generic types
     */
    public Builder<T> returning(Class<?> returnType, ResolvableType generic, ResolvableType... generics) {
      return returning(toResolvableType(returnType, generic, generics));
    }

    /**
     * Filter on methods returning the given type.
     *
     * @param returnType the return type
     */
    public Builder<T> returning(ResolvableType returnType) {
      String expected = returnType.toString();
      String message = "returnType=" + expected;
      addFilter(message, m -> expected.equals(ResolvableType.forReturnType(m).toString()));
      return this;
    }

    /**
     * Build a {@code ResolvableMethod} from the provided filters which must
     * resolve to a unique, single method.
     * <p>See additional resolveXxx shortcut methods going directly to
     * {@link Method} or return type parameter.
     *
     * @throws IllegalStateException for no match or multiple matches
     */
    public ResolvableMethod build() {
      Set<Method> methods = MethodIntrospector.filterMethods(this.objectClass, this::isMatch);
      Assert.state(!methods.isEmpty(), () -> "No matching method: " + this);
      Assert.state(methods.size() == 1, () -> "Multiple matching methods: " + this + formatMethods(methods));
      return new ResolvableMethod(methods.iterator().next());
    }

    private boolean isMatch(Method method) {
      return this.filters.stream().allMatch(p -> p.test(method));
    }

    private String formatMethods(Set<Method> methods) {
      return "\nMatched:\n" + methods.stream()
              .map(Method::toGenericString).collect(joining(",\n\t", "[\n\t", "\n]"));
    }

    public ResolvableMethod mockCall(Consumer<T> invoker) {
      MethodInvocationInterceptor interceptor = new MethodInvocationInterceptor();
      T proxy = initProxy(this.objectClass, interceptor);
      invoker.accept(proxy);
      Method method = interceptor.getInvokedMethod();
      return new ResolvableMethod(method);
    }

    // Build & resolve shortcuts...

    /**
     * Resolve and return the {@code Method} equivalent to:
     * <p>{@code build().method()}
     */
    public final Method resolveMethod() {
      return build().method();
    }

    /**
     * Resolve and return the {@code Method} equivalent to:
     * <p>{@code named(methodName).build().method()}
     */
    public Method resolveMethod(String methodName) {
      return named(methodName).build().method();
    }

    /**
     * Resolve and return the declared return type equivalent to:
     * <p>{@code build().returnType()}
     */
    public final MethodParameter resolveReturnType() {
      return build().returnType();
    }

    /**
     * Shortcut to the unique return type equivalent to:
     * <p>{@code returning(returnType).build().returnType()}
     *
     * @param returnType the return type
     * @param generics optional array of generic types
     */
    public MethodParameter resolveReturnType(Class<?> returnType, Class<?>... generics) {
      return returning(returnType, generics).build().returnType();
    }

    public HandlerMethod resolveHandlerMethod(Class<?> returnType, Class<?>... generics) {
      Method method = returning(returnType, generics).build().method();
      Object instance = BeanUtils.newInstance(objectClass);
      return new HandlerMethod(instance, method);
    }

    public HandlerMethod resolveHandlerMethod(Object bean, Class<?> returnType, Class<?>... generics) {
      Method method = returning(returnType, generics).build().method();
      return new HandlerMethod(bean, method);
    }

    public HandlerMethod resolveHandlerMethod(Class<?> returnType, ResolvableType generic,
            ResolvableType... generics) {
      Method method = returning(returnType, generic, generics).build().method();
      Object instance = BeanUtils.newInstance(objectClass);
      return new HandlerMethod(instance, method);
    }

    public HandlerMethod resolveHandlerMethod(
            Object bean, Class<?> returnType, ResolvableType generic, ResolvableType... generics) {
      Method method = returning(returnType, generic, generics).build().method();
      return new HandlerMethod(bean, method);
    }

    public HandlerMethod resolveHandlerMethod(ResolvableType returnType) {
      Method method = returning(returnType).build().method();
      Object instance = BeanUtils.newInstance(objectClass);
      return new HandlerMethod(instance, method);
    }

    /**
     * Shortcut to the unique return type equivalent to:
     * <p>{@code returning(returnType).build().returnType()}
     *
     * @param returnType the return type
     * @param generic at least one generic type
     * @param generics optional extra generic types
     */
    public MethodParameter resolveReturnType(Class<?> returnType, ResolvableType generic,
            ResolvableType... generics) {

      return returning(returnType, generic, generics).build().returnType();
    }

    public MethodParameter resolveReturnType(ResolvableType returnType) {
      return returning(returnType).build().returnType();
    }

    @Override
    public String toString() {
      return "ResolvableMethod.Builder[\n" +
              "\tobjectClass = " + this.objectClass.getName() + ",\n" +
              "\tfilters = " + formatFilters() + "\n]";
    }

    private String formatFilters() {
      return this.filters.stream().map(Object::toString)
              .collect(joining(",\n\t\t", "[\n\t\t", "\n\t]"));
    }
  }

  /**
   * Predicate with a descriptive label.
   */
  private record LabeledPredicate<T>(String label, Predicate<T> delegate) implements Predicate<T> {

    @Override
    public boolean test(T method) {
      return this.delegate.test(method);
    }

    @Override
    public Predicate<T> and(Predicate<? super T> other) {
      return this.delegate.and(other);
    }

    @Override
    public Predicate<T> negate() {
      return this.delegate.negate();
    }

    @Override
    public Predicate<T> or(Predicate<? super T> other) {
      return this.delegate.or(other);
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  /**
   * Resolver for method arguments.
   */
  public class ArgResolver {

    private final List<Predicate<MethodParameter>> filters = new ArrayList<>(4);

    @SafeVarargs
    private ArgResolver(Predicate<MethodParameter>... filter) {
      this.filters.addAll(Arrays.asList(filter));
    }

    /**
     * Filter on method arguments with annotations.
     */
    @SafeVarargs
    public final ArgResolver annot(Predicate<MethodParameter>... filters) {
      this.filters.addAll(Arrays.asList(filters));
      return this;
    }

    /**
     * Filter on method arguments that have the given annotations.
     *
     * @param annotationTypes the annotation types
     * @see #annot(Predicate[])
     */
    @SafeVarargs
    public final ArgResolver annotPresent(Class<? extends Annotation>... annotationTypes) {
      this.filters.add(param -> Arrays.stream(annotationTypes).allMatch(param::hasParameterAnnotation));
      return this;
    }

    /**
     * Filter on method arguments that don't have the given annotations.
     *
     * @param annotationTypes the annotation types
     */
    @SafeVarargs
    public final ArgResolver annotNotPresent(Class<? extends Annotation>... annotationTypes) {
      this.filters.add(param ->
              (annotationTypes.length > 0 ?
               Arrays.stream(annotationTypes).noneMatch(param::hasParameterAnnotation) :
               param.getParameterAnnotations().length == 0));
      return this;
    }

    /**
     * Resolve the argument also matching to the given type.
     *
     * @param type the expected type
     */
    public ResolvableMethodParameter arg(Class<?> type, Class<?>... generics) {
      return arg(toResolvableType(type, generics));
    }

    /**
     * Resolve the argument also matching to the given type.
     *
     * @param type the expected type
     */
    public ResolvableMethodParameter arg(Class<?> type, ResolvableType generic, ResolvableType... generics) {
      return arg(toResolvableType(type, generic, generics));
    }

    /**
     * Resolve the argument also matching to the given type.
     *
     * @param type the expected type
     */
    public ResolvableMethodParameter arg(ResolvableType type) {
      this.filters.add(p -> type.toString().equals(ResolvableType.forMethodParameter(p).toString()));
      return arg();
    }

    /**
     * Resolve the argument.
     */
    public final ResolvableMethodParameter arg() {
      List<ResolvableMethodParameter> matches = applyFilters();
      Assert.state(!matches.isEmpty(), () ->
              "No matching arg in method\n" + formatMethod());
      Assert.state(matches.size() == 1, () ->
              "Multiple matching args in method\n" + formatMethod() + "\nMatches:\n\t" + matches);
      return matches.get(0);
    }

    private List<ResolvableMethodParameter> applyFilters() {
      List<ResolvableMethodParameter> matches = new ArrayList<>();
      for (int i = 0; i < method.getParameterCount(); i++) {
        MethodParameter param = new SynthesizingMethodParameter(method, i);
        param.initParameterNameDiscovery(nameDiscoverer);
        if (this.filters.stream().allMatch(p -> p.test(param))) {
          matches.add(new ResolvableMethodParameter(param));
        }
      }
      return matches;
    }
  }

  private static class MethodInvocationInterceptor
          implements cn.taketoday.core.bytecode.proxy.MethodInterceptor, MethodInterceptor {

    private Method invokedMethod;

    Method getInvokedMethod() {
      return this.invokedMethod;
    }

    @Override
    @Nullable
    public Object intercept(Object object, Method method, Object[] args, MethodProxy proxy) {
      if (ReflectionUtils.isObjectMethod(method)) {
        return ReflectionUtils.invokeMethod(method, object, args);
      }
      else {
        this.invokedMethod = method;
        return null;
      }
    }

    @Override
    @Nullable
    public Object invoke(org.aopalliance.intercept.MethodInvocation inv) throws Throwable {
      return intercept(inv.getThis(), inv.getMethod(), inv.getArguments(), null);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T initProxy(Class<?> type, MethodInvocationInterceptor interceptor) {
    Assert.notNull(type, "'type' must not be null");
    if (type.isInterface()) {
      ProxyFactory factory = new ProxyFactory(EmptyTargetSource.INSTANCE);
      factory.addInterface(type);
      factory.addInterface(Supplier.class);
      factory.addAdvice(interceptor);
      return (T) factory.getProxy();
    }

    else {
      Enhancer enhancer = new Enhancer();
      enhancer.setSuperclass(type);
      enhancer.setInterfaces(Supplier.class);
      enhancer.setNamingPolicy(DefaultNamingPolicy.INSTANCE);
      enhancer.setCallbackType(cn.taketoday.core.bytecode.proxy.MethodInterceptor.class);

      Class<?> proxyClass = enhancer.createClass();
      Object proxy = null;

      try {
        proxy = BeanInstantiator.forSerialization(proxyClass);
      }
      catch (Exception ex) {
        logger.debug("Objenesis failed, falling back to default constructor", ex);
      }

      if (proxy == null) {
        try {
          proxy = ReflectionUtils.accessibleConstructor(proxyClass).newInstance();
        }
        catch (Throwable ex) {
          throw new IllegalStateException("Unable to instantiate proxy " +
                  "via both Objenesis and default constructor fails as well", ex);
        }
      }

      ((Factory) proxy).setCallbacks(new Callback[] { interceptor });
      return (T) proxy;
    }
  }

}
