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
import java.lang.reflect.Type;
import java.util.StringJoiner;

import infra.beans.factory.BeanFactory;
import infra.context.MessageSource;
import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.annotation.AnnotatedMethod;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.i18n.LocaleContextHolder;
import infra.http.HttpStatusCode;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
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
public class HandlerMethod extends AnnotatedMethod implements AsyncHandler {

  /** Logger that is available to subclasses. */
  protected static final Logger log = LoggerFactory.getLogger(HandlerMethod.class);

  private final Object bean;

  private final Class<?> beanType;

  /** @since 4.0 */
  private final boolean responseBody;

  /**
   * @since 4.0
   */
  private @Nullable MethodParameter returnTypeParameter;

  private final @Nullable MessageSource messageSource;

  private @Nullable HttpStatusCode responseStatus;

  private @Nullable String responseStatusReason;

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
    super(method);
    this.bean = bean;
    this.messageSource = messageSource;
    this.beanType = ClassUtils.getUserClass(bean);
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
    super(bean.getClass().getMethod(methodName, parameterTypes));
    this.bean = bean;
    this.messageSource = null;
    this.beanType = ClassUtils.getUserClass(bean);
    this.responseBody = computeResponseBody();
    evaluateResponseStatus();
    ReflectionUtils.makeAccessible(bridgedMethod);
  }

  /**
   * Create an instance from a bean name, a method, and a {@code BeanFactory}.
   */
  public HandlerMethod(String beanName, BeanFactory beanFactory, @Nullable MessageSource messageSource, Method method) {
    super(method);
    Assert.hasText(beanName, "Bean name is required");
    Assert.notNull(beanFactory, "BeanFactory is required");

    this.bean = beanFactory.isSingleton(beanName) ? beanFactory.getBean(beanName) : beanName;
    this.messageSource = messageSource;
    Class<?> beanType = beanFactory.getType(beanName);
    if (beanType == null) {
      throw new IllegalStateException("Cannot resolve bean type for bean with name '%s'".formatted(beanName));
    }
    this.beanType = ClassUtils.getUserClass(beanType);
    ReflectionUtils.makeAccessible(bridgedMethod);
    this.responseBody = computeResponseBody();
    evaluateResponseStatus();
  }

  /**
   * Copy constructor for use in subclasses.
   */
  protected HandlerMethod(HandlerMethod other) {
    super(other);
    this.bean = other.bean;
    this.messageSource = other.messageSource;
    this.beanType = other.beanType;
    this.responseStatus = other.responseStatus;
    this.responseStatusReason = other.responseStatusReason;
    this.responseBody = other.responseBody;
    this.corsConfig = other.corsConfig;
    this.returnTypeParameter = other.returnTypeParameter;
  }

  /**
   * Re-create HandlerMethod with the resolved handler.
   */
  protected HandlerMethod(HandlerMethod other, Object handler) {
    super(other);
    this.bean = handler;
    this.messageSource = other.messageSource;
    this.beanType = other.beanType;
    this.responseStatus = other.responseStatus;
    this.responseStatusReason = other.responseStatusReason;
    this.responseBody = other.responseBody;
    this.corsConfig = other.corsConfig;
    this.returnTypeParameter = other.returnTypeParameter;
  }

  /**
   * Return the bean for this handler method.
   */
  public Object getBean() {
    return this.bean;
  }

  /**
   * This method returns the type of the handler for this handler method.
   * <p>Note that if the bean type is a CGLIB-generated class, the original
   * user-defined class is returned.
   */
  public Class<?> getBeanType() {
    return this.beanType;
  }

  @Override
  protected Class<?> getContainingClass() {
    return this.beanType;
  }

  /**
   * Return the specified response status, if any.
   *
   * @see ResponseStatus#code()
   */
  protected @Nullable HttpStatusCode getResponseStatus() {
    return this.responseStatus;
  }

  /**
   * Return the associated response status reason, if any.
   *
   * @see ResponseStatus#reason()
   */
  protected @Nullable String getResponseStatusReason() {
    return this.responseStatusReason;
  }

  /**
   * Return the HandlerMethod return type.
   */
  @Override
  public MethodParameter getReturnType() {
    MethodParameter returnType = returnTypeParameter;
    if (returnType == null) {
      returnType = super.getReturnType();
      this.returnTypeParameter = returnType;
    }
    return returnType;
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
    StringJoiner joiner = new StringJoiner(", ", "(", ")");
    for (Class<?> paramType : method.getParameterTypes()) {
      joiner.add(paramType.getSimpleName());
    }
    return beanType.getName() + "#" + method.getName() + joiner;
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
  private class ConcurrentResultMethodParameter extends AnnotatedMethodParameter {

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

}
