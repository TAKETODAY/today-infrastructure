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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

import infra.beans.factory.BeanFactory;
import infra.context.MessageSource;
import infra.core.BridgeMethodResolver;
import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.SynthesizingMethodParameter;
import infra.core.i18n.LocaleContextHolder;
import infra.http.HttpStatusCode;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.MapCache;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;
import infra.web.annotation.ResponseBody;
import infra.web.annotation.ResponseStatus;
import infra.web.cors.CorsConfiguration;
import infra.web.handler.AsyncHandler;
import infra.web.handler.HandlerWrapper;
import infra.web.handler.result.CollectedValuesList;

/**
 * Encapsulates information about a handler method consisting of a
 * {@linkplain #getMethod() method} and a {@linkplain #getBean() bean}.
 * Provides convenient access to method parameters, the method return value,
 * method annotations, etc.
 *
 * <p>The class may be created with a bean instance or with a bean name
 * (e.g. lazy-init bean, prototype bean). Use {@link #withBean(Object)}}
 * to obtain a {@code HandlerMethod} instance with a bean instance resolved
 * through the associated {@link BeanFactory}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-06-25 20:03:11
 */
public class HandlerMethod implements AsyncHandler {

  /** Logger that is available to subclasses. */
  protected static final Logger log = LoggerFactory.getLogger(HandlerMethod.class);

  static MapCache<AnnotationKey, Boolean, @Nullable HandlerMethod> methodAnnotationCache = new MapCache<>(128) {
    @Override
    protected Boolean createValue(AnnotationKey key, @Nullable HandlerMethod handlerMethod) {
      return AnnotatedElementUtils.hasAnnotation(key.method, key.annotationType);
    }
  };

  private final Object bean;

  private final Class<?> beanType;

  /** action **/
  private final Method method;

  /**
   * If the bean method is a bridge method, this method is the bridged
   * (user-defined) method. Otherwise, it returns the same method as {@link #getMethod()}.
   */
  protected final Method bridgedMethod;

  /** parameter list **/
  private final MethodParameter[] parameters;

  /** @since 2.3.7 */
  private final Class<?> returnType;

  /**
   * @since 4.0
   */
  @Nullable
  private MethodParameter returnTypeParameter;

  @Nullable
  private final MessageSource messageSource;

  /** @since 4.0 */
  private final boolean responseBody;

  @Nullable
  private HttpStatusCode responseStatus;

  @Nullable
  private String responseStatusReason;

  /** @since 4.0 */
  @Nullable
  private volatile ArrayList<Annotation[][]> inheritedParameterAnnotations;

  /**
   * cors config cache
   *
   * @since 4.0
   */
  @Nullable
  CorsConfiguration corsConfig;

  /**
   * Create an instance from a bean instance and a method.
   */
  public HandlerMethod(Object bean, Method method) {
    this(bean, method, null);
  }

  /**
   * Variant of {@link #HandlerMethod(Object, Method)} that
   * also accepts a {@link MessageSource} for use from subclasses.
   *
   * @since 5.0
   */
  protected HandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource) {
    Assert.notNull(method, "Method is required");
    this.bean = bean;
    this.method = method;
    this.messageSource = messageSource;
    this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
    this.beanType = ClassUtils.getUserClass(bean);
    this.returnType = bridgedMethod.getReturnType();
    this.parameters = initMethodParameters();
    this.responseBody = computeResponseBody();
    evaluateResponseStatus();
    ReflectionUtils.makeAccessible(bridgedMethod);
  }

  /**
   * Create an instance from a bean instance, method name, and parameter types.
   *
   * @throws NoSuchMethodException when the method cannot be found
   */
  public HandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
    Assert.notNull(bean, "Bean is required");
    Assert.notNull(methodName, "Method name is required");
    this.bean = bean;
    this.messageSource = null;
    this.beanType = ClassUtils.getUserClass(bean);
    this.method = bean.getClass().getMethod(methodName, parameterTypes);
    this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
    this.returnType = bridgedMethod.getReturnType();
    this.parameters = initMethodParameters();
    this.responseBody = computeResponseBody();
    evaluateResponseStatus();
    ReflectionUtils.makeAccessible(bridgedMethod);
  }

  /**
   * Create an instance from a bean name, a method, and a {@code BeanFactory}.
   */
  @SuppressWarnings("NullAway")
  public HandlerMethod(String beanName, BeanFactory beanFactory, @Nullable MessageSource messageSource, Method method) {
    Assert.notNull(method, "Method is required");
    Assert.hasText(beanName, "Bean name is required");
    Assert.notNull(beanFactory, "BeanFactory is required");

    this.bean = beanFactory.isSingleton(beanName) ? beanFactory.getBean(beanName) : beanName;
    this.method = method;
    this.messageSource = messageSource;
    Class<?> beanType = beanFactory.getType(beanName);
    if (beanType == null) {
      throw new IllegalStateException("Cannot resolve bean type for bean with name '%s'".formatted(beanName));
    }
    this.beanType = ClassUtils.getUserClass(beanType);
    this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
    this.returnType = bridgedMethod.getReturnType();
    ReflectionUtils.makeAccessible(bridgedMethod);
    this.parameters = initMethodParameters();
    this.responseBody = computeResponseBody();
    evaluateResponseStatus();
  }

  /**
   * Copy constructor for use in subclasses.
   */
  protected HandlerMethod(HandlerMethod other) {
    Assert.notNull(other, "HandlerMethod is required");
    this.bean = other.bean;
    this.messageSource = other.messageSource;
    this.method = other.method;
    this.beanType = other.beanType;
    this.returnType = other.returnType;
    this.bridgedMethod = other.bridgedMethod;
    this.parameters = other.parameters;
    this.responseStatus = other.responseStatus;
    this.responseStatusReason = other.responseStatusReason;
    this.responseBody = other.responseBody;
    this.corsConfig = other.corsConfig;
    this.returnTypeParameter = other.returnTypeParameter;
    this.inheritedParameterAnnotations = other.inheritedParameterAnnotations;
  }

  /**
   * Re-create HandlerMethod with the resolved handler.
   */
  protected HandlerMethod(HandlerMethod other, Object handler) {
    this.bean = handler;
    this.messageSource = other.messageSource;
    this.beanType = other.beanType;
    this.method = other.method;
    this.returnType = other.returnType;
    this.bridgedMethod = other.bridgedMethod;
    this.parameters = other.parameters;
    this.responseStatus = other.responseStatus;
    this.responseStatusReason = other.responseStatusReason;
    this.responseBody = other.responseBody;
    this.corsConfig = other.corsConfig;
    this.returnTypeParameter = other.returnTypeParameter;
    this.inheritedParameterAnnotations = other.inheritedParameterAnnotations;
  }

  // ---- useful methods

  /**
   * Return the bean for this handler method.
   */
  public Object getBean() {
    return this.bean;
  }

  /**
   * Return the method for this handler method.
   */
  public Method getMethod() {
    return this.method;
  }

  /**
   * This method returns the type of the handler for this handler method.
   * <p>Note that if the bean type is a CGLIB-generated class, the original
   * user-defined class is returned.
   */
  public Class<?> getBeanType() {
    return this.beanType;
  }

  /**
   * Return the method parameters for this handler method.
   */
  public MethodParameter[] getMethodParameters() {
    return this.parameters;
  }

  /**
   * Returns the number of formal parameters (whether explicitly
   * declared or implicitly declared or neither) for the executable
   * represented by this object.
   *
   * @return The number of formal parameters for the executable this
   * object represents
   * @since 4.0
   */
  public int getParameterCount() {
    return parameters.length;
  }

  /**
   * Return the specified response status, if any.
   *
   * @see ResponseStatus#code()
   */
  @Nullable
  protected HttpStatusCode getResponseStatus() {
    return this.responseStatus;
  }

  /**
   * Return the associated response status reason, if any.
   *
   * @see ResponseStatus#reason()
   */
  @Nullable
  protected String getResponseStatusReason() {
    return this.responseStatusReason;
  }

  /**
   * Return the HandlerMethod return type.
   */
  public MethodParameter getReturnType() {
    MethodParameter returnType = returnTypeParameter;
    if (returnType == null) {
      returnType = new HandlerMethodParameter(-1);
      this.returnTypeParameter = returnType;
    }
    return returnType;
  }

  /**
   * Return the actual return value type.
   */
  public MethodParameter getReturnValueType(@Nullable Object returnValue) {
    return new ReturnValueMethodParameter(returnValue);
  }

  /**
   * Return the actual return type.
   */
  public Class<?> getRawReturnType() {
    return returnType;
  }

  /**
   * Determine if the return type of this handler method is assignable to the given superclass.
   *
   * @param superClass the superclass to check against
   * @return {@code true} if the return type is assignable to the given superclass, {@code false} otherwise
   */
  public boolean isReturnTypeAssignableTo(Class<?> superClass) {
    return superClass.isAssignableFrom(returnType);
  }

  /**
   * Check if the return type of this handler method matches the given type exactly.
   *
   * @param returnType the type to compare with the handler method's return type
   * @return {@code true} if the return type matches exactly, {@code false} otherwise
   */
  public boolean isReturn(Class<?> returnType) {
    return returnType == this.returnType;
  }

  /**
   * Indicates whether the {@link ResponseBody} annotation is present on the method or its declaring class.
   *
   * @return {@code true} if the {@link ResponseBody} annotation is present, {@code false} otherwise
   */
  public boolean isResponseBody() {
    return this.responseBody;
  }

  protected boolean computeResponseBody() {
    MergedAnnotation<ResponseBody> annotation = MergedAnnotations.from(method).get(ResponseBody.class);
    if (annotation.isPresent()) {
      return annotation.getBoolean(MergedAnnotation.VALUE);
    }
    annotation = MergedAnnotations.from(method.getDeclaringClass()).get(ResponseBody.class);
    if (annotation.isPresent()) {
      return annotation.getBoolean(MergedAnnotation.VALUE);
    }
    return false;
  }

  /**
   * Return {@code true} if the method return type is void, {@code false} otherwise.
   */
  public boolean isVoid() {
    return returnType == void.class;
  }

  /**
   * Return a single annotation on the underlying method traversing its super methods
   * if no annotation can be found on the given method itself.
   * <p>Also supports <em>merged</em> composed annotations with attribute
   * overrides
   *
   * @param annotationType the type of annotation to introspect the method for
   * @return the annotation, or {@code null} if none found
   * @see AnnotatedElementUtils#findMergedAnnotation
   */
  @Nullable
  public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
    return AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
  }

  /**
   * Return whether the parameter is declared with the given annotation type.
   *
   * @param annotationType the annotation type to look for
   * @see AnnotatedElementUtils#hasAnnotation
   * @since 4.0
   */
  public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
    return methodAnnotationCache.get(new AnnotationKey(method, annotationType), this);
  }

  @Override
  public ConcurrentResultHandlerMethod wrapConcurrentResult(@Nullable Object result) {
    return new ConcurrentResultHandlerMethod(new ConcurrentResultMethodParameter(result), this);
  }

  /**
   * create with a new bean
   *
   * @since 4.0
   */
  public HandlerMethod withBean(Object handler) {
    return new HandlerMethod(this, handler);
  }

  /**
   * Return a short representation of this handler method for log message purposes.
   *
   * @since 4.0
   */
  public String getShortLogMessage() {
    return "%s#%s[%d args]".formatted(getBeanType().getName(), this.method.getName(), this.method.getParameterCount());
  }

  // Object

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof HandlerMethod otherMethod)) {
      return false;
    }
    return (this.bean.equals(otherMethod.bean) && this.method.equals(otherMethod.method));
  }

  @Override
  public int hashCode() {
    return (this.bean.hashCode() * 31 + this.method.hashCode());
  }

  @Override
  public String toString() {
    return initDescription(beanType, method);
  }

  private ArrayList<Annotation[][]> getInheritedParameterAnnotations() {
    var parameterAnnotations = this.inheritedParameterAnnotations;
    if (parameterAnnotations == null) {
      parameterAnnotations = new ArrayList<>();
      Class<?> clazz = this.method.getDeclaringClass();
      while (clazz != null) {
        for (Class<?> ifc : clazz.getInterfaces()) {
          for (Method candidate : ifc.getMethods()) {
            if (isOverrideFor(candidate)) {
              parameterAnnotations.add(candidate.getParameterAnnotations());
            }
          }
        }
        clazz = clazz.getSuperclass();
        if (clazz == Object.class) {
          clazz = null;
        }
        if (clazz != null) {
          for (Method candidate : clazz.getDeclaredMethods()) {
            if (isOverrideFor(candidate)) {
              parameterAnnotations.add(candidate.getParameterAnnotations());
            }
          }
        }
      }
      this.inheritedParameterAnnotations = parameterAnnotations;
    }
    return parameterAnnotations;
  }

  private boolean isOverrideFor(Method candidate) {
    if (Modifier.isPrivate(candidate.getModifiers())
            || !candidate.getName().equals(this.method.getName())
            || (candidate.getParameterCount() != this.method.getParameterCount())) {
      return false;
    }
    Class<?>[] paramTypes = this.method.getParameterTypes();
    if (Arrays.equals(candidate.getParameterTypes(), paramTypes)) {
      return true;
    }
    for (int i = 0; i < paramTypes.length; i++) {
      if (paramTypes[i] !=
              ResolvableType.forMethodParameter(candidate, i, this.method.getDeclaringClass()).toClass()) {
        return false;
      }
    }
    return true;
  }

  private MethodParameter[] initMethodParameters() {
    int count = bridgedMethod.getParameterCount();
    MethodParameter[] result = new MethodParameter[count];
    for (int i = 0; i < count; i++) {
      result[i] = new HandlerMethodParameter(i);
    }
    return result;
  }

  private void evaluateResponseStatus() {
    ResponseStatus annotation = getMethodAnnotation(ResponseStatus.class);
    if (annotation == null) {
      annotation = AnnotatedElementUtils.findMergedAnnotation(getBeanType(), ResponseStatus.class);
    }
    if (annotation != null) {
      String reason = annotation.reason();
      String resolvedReason = StringUtils.hasText(reason) && messageSource != null
              ? messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale())
              : reason;

      this.responseStatus = annotation.code();
      this.responseStatusReason = resolvedReason;
      if (StringUtils.hasText(resolvedReason) && getMethod().getReturnType() != void.class) {
        log.warn("Return value of [{}] will be ignored since @ResponseStatus 'reason' attribute is set.", getMethod());
      }
    }
  }

  private static String initDescription(Class<?> beanType, Method method) {
    StringJoiner joiner = new StringJoiner(", ", "(", ")");
    for (Class<?> paramType : method.getParameterTypes()) {
      joiner.add(paramType.getSimpleName());
    }
    return beanType.getName() + "#" + method.getName() + joiner;
  }

  // HandlerMethod

  /**
   * Determine whether the given object is a handler method.
   *
   * @param handler the object to check
   * @return {@code true} if the object is a handler method, {@code false} otherwise
   * @since 5.0
   */
  public static boolean isHandler(@Nullable Object handler) {
    return unwrap(handler) != null;
  }

  /**
   * Unwrap the given handler to extract the underlying {@link HandlerMethod}, if any.
   *
   * @param handler the handler to unwrap
   * @return the extracted {@link HandlerMethod}, or {@code null} if not found
   */
  @Nullable
  public static HandlerMethod unwrap(@Nullable Object handler) {
    if (handler instanceof HandlerMethod) {
      return (HandlerMethod) handler;
    }
    else if (handler instanceof HandlerWrapper wrapper
            && wrapper.getRawHandler() instanceof HandlerMethod target) {
      return target;
    }
    return null;
  }

  protected static class ConcurrentResultHandlerMethod extends HandlerMethod {
    private final HandlerMethod target;

    private final MethodParameter returnType;

    public ConcurrentResultHandlerMethod(ConcurrentResultMethodParameter returnType, HandlerMethod target) {
      super(target);
      this.target = target;
      this.returnType = returnType;
    }

    /**
     * Bridge to actual controller type-level annotations.
     */
    @Override
    public Class<?> getBeanType() {
      return target.getBeanType();
    }

    /**
     * Bridge to actual return value or generic type within the declared
     * async return type, e.g. Foo instead of {@code DeferredResult<Foo>}.
     */
    @Override
    public MethodParameter getReturnValueType(@Nullable Object returnValue) {
      return this.returnType;
    }

    @Override
    public MethodParameter getReturnType() {
      return returnType;
    }

    /**
     * Bridge to controller method-level annotations.
     */
    @Nullable
    @Override
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
      return target.getMethodAnnotation(annotationType);
    }

    /**
     * Bridge to controller method-level annotations.
     */
    @Override
    public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
      return target.hasMethodAnnotation(annotationType);
    }

    @Override
    public boolean isReturn(Class<?> returnType) {
      return this.returnType.getParameterType() == returnType;
    }

    @Override
    public boolean isReturnTypeAssignableTo(Class<?> superClass) {
      return superClass.isAssignableFrom(returnType.getParameterType());
    }

  }

  /**
   * MethodParameter subclass based on the actual return value type or if
   * that's null falling back on the generic type within the declared async
   * return type, e.g. Foo instead of {@code DeferredResult<Foo>}.
   */
  private class ConcurrentResultMethodParameter extends HandlerMethodParameter {

    @Nullable
    private final Object returnValue;

    private final ResolvableType returnType;

    public ConcurrentResultMethodParameter(@Nullable Object returnValue) {
      super(-1);
      this.returnValue = returnValue;
      this.returnType = returnValue instanceof CollectedValuesList list
              ? list.getReturnType()
              : ResolvableType.forType(super.getGenericParameterType()).getGeneric();
    }

    public ConcurrentResultMethodParameter(ConcurrentResultMethodParameter original) {
      super(original);
      this.returnValue = original.returnValue;
      this.returnType = original.returnType;
    }

    @Override
    public Class<?> getParameterType() {
      if (this.returnValue != null) {
        return this.returnValue.getClass();
      }
      if (!ResolvableType.NONE.equals(this.returnType)) {
        return this.returnType.toClass();
      }
      return super.getParameterType();
    }

    @Override
    public Type getGenericParameterType() {
      return this.returnType.getType();
    }

    @Override
    public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
      // Ensure @ResponseBody-style handling for values collected from a reactive type
      // even if actual return type is ResponseEntity<Flux<T>>
      return super.hasMethodAnnotation(annotationType)
              || (annotationType == ResponseBody.class && this.returnValue instanceof CollectedValuesList);
    }

    @Override
    public ConcurrentResultMethodParameter clone() {
      return new ConcurrentResultMethodParameter(this);
    }
  }

  /**
   * A MethodParameter with HandlerMethod-specific behavior.
   */
  protected class HandlerMethodParameter extends SynthesizingMethodParameter {

    private volatile Annotation @Nullable [] combinedAnnotations;

    public HandlerMethodParameter(int index) {
      super(method, index);
    }

    protected HandlerMethodParameter(HandlerMethodParameter original) {
      super(original);
      this.combinedAnnotations = original.combinedAnnotations;
    }

    @Override
    public Method getMethod() {
      return HandlerMethod.this.bridgedMethod;
    }

    @Override
    public Class<?> getContainingClass() {
      return HandlerMethod.this.getBeanType();
    }

    @Nullable
    @Override
    public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
      return HandlerMethod.this.getMethodAnnotation(annotationType);
    }

    @Override
    public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
      return HandlerMethod.this.hasMethodAnnotation(annotationType);
    }

    @Override
    public Annotation[] getParameterAnnotations() {
      Annotation[] anns = this.combinedAnnotations;
      if (anns == null) {
        anns = super.getParameterAnnotations();
        int index = getParameterIndex();
        if (index >= 0) {
          for (Annotation[][] ifcAnns : getInheritedParameterAnnotations()) {
            if (index < ifcAnns.length) {
              Annotation[] paramAnns = ifcAnns[index];
              if (paramAnns.length > 0) {
                ArrayList<Annotation> merged = new ArrayList<>(anns.length + paramAnns.length);
                CollectionUtils.addAll(merged, anns);
                for (Annotation paramAnn : paramAnns) {
                  boolean existingType = false;
                  for (Annotation ann : anns) {
                    if (ann.annotationType() == paramAnn.annotationType()) {
                      existingType = true;
                      break;
                    }
                  }
                  if (!existingType) {
                    merged.add(adaptAnnotation(paramAnn));
                  }
                }
                anns = merged.toArray(Constant.EMPTY_ANNOTATIONS);
              }
            }
          }
        }
        this.combinedAnnotations = anns;
      }
      return anns;
    }

    @Override
    public HandlerMethodParameter clone() {
      return new HandlerMethodParameter(this);
    }

    @Override
    public String toString() {
      return getParameterType().getSimpleName() + " " + getParameterName();
    }

  }

  /**
   * A MethodParameter for a HandlerMethod return type based on an actual return value.
   */
  private class ReturnValueMethodParameter extends HandlerMethodParameter {

    @Nullable
    private final Class<?> returnValueType;

    public ReturnValueMethodParameter(@Nullable Object returnValue) {
      super(-1);
      this.returnValueType = returnValue != null ? returnValue.getClass() : null;
    }

    protected ReturnValueMethodParameter(ReturnValueMethodParameter original) {
      super(original);
      this.returnValueType = original.returnValueType;
    }

    @Override
    public Class<?> getParameterType() {
      return returnValueType != null ? returnValueType : super.getParameterType();
    }

    @Override
    public ReturnValueMethodParameter clone() {
      return new ReturnValueMethodParameter(this);
    }

  }

  static final class AnnotationKey {

    private final int hash;

    public final Method method;

    public final Class<? extends Annotation> annotationType;

    AnnotationKey(Method method, Class<? extends Annotation> annotationType) {
      this.method = method;
      this.annotationType = annotationType;
      this.hash = Objects.hash(method, annotationType);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof AnnotationKey annotationKey))
        return false;
      return hash == annotationKey.hash
              && Objects.equals(method, annotationKey.method)
              && Objects.equals(annotationType, annotationKey.annotationType);
    }

    @Override
    public int hashCode() {
      return this.hash;
    }

  }
}
