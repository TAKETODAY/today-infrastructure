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

// Modifications Copyright 2026 the TODAY authors.

package infra.core.annotation;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.core.BridgeMethodResolver;
import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.lang.NullValue;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

/**
 * A convenient wrapper for a {@link Method} handle, providing deep annotation
 * introspection on methods and method parameters, including the exposure of
 * interface-declared parameter annotations from the concrete target method.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see #getMethodAnnotation(Class)
 * @see #getMethodParameters()
 * @see AnnotatedElementUtils
 * @see SynthesizingMethodParameter
 * @since 5.0
 */
public class AnnotatedMethod {

  protected final Method method;

  protected final Method bridgedMethod;

  protected final MethodParameter[] parameters;

  protected final Class<?> returnType;

  private final Map<Class<? extends Annotation>, Object> annotations;

  private volatile @Nullable List<Annotation[][]> inheritedParameterAnnotations;

  /**
   * Create an instance that wraps the given {@link Method}.
   *
   * @param method the {@code Method} handle to wrap
   */
  public AnnotatedMethod(Method method) {
    Assert.notNull(method, "Method is required");
    this.method = method;
    this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
    ReflectionUtils.makeAccessible(this.bridgedMethod);
    this.returnType = bridgedMethod.getReturnType();
    this.parameters = initMethodParameters();
    this.annotations = new ConcurrentHashMap<>(4);
  }

  /**
   * Copy constructor for use in subclasses.
   */
  protected AnnotatedMethod(AnnotatedMethod other) {
    Assert.notNull(other, "AnnotatedMethod is required");
    this.method = other.method;
    this.bridgedMethod = other.bridgedMethod;
    this.parameters = other.parameters;
    this.annotations = other.annotations;
    this.returnType = other.returnType;
    this.inheritedParameterAnnotations = other.inheritedParameterAnnotations;
  }

  /**
   * Return the annotated method.
   */
  public final Method getMethod() {
    return this.method;
  }

  /**
   * If the annotated method is a bridge method, this method returns the bridged
   * (user-defined) method. Otherwise, it returns the same method as {@link #getMethod()}.
   */
  protected final Method getBridgedMethod() {
    return this.bridgedMethod;
  }

  /**
   * Expose the containing class for method parameters.
   *
   * @see MethodParameter#getContainingClass()
   */
  protected Class<?> getContainingClass() {
    return this.method.getDeclaringClass();
  }

  /**
   * Return the method parameters for this {@code AnnotatedMethod}.
   */
  public final MethodParameter[] getMethodParameters() {
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
  public final int getParameterCount() {
    return parameters.length;
  }

  private MethodParameter[] initMethodParameters() {
    int count = this.bridgedMethod.getParameterCount();
    MethodParameter[] result = new MethodParameter[count];
    for (int i = 0; i < count; i++) {
      result[i] = new AnnotatedMethodParameter(i);
    }
    return result;
  }

  /**
   * Return a {@link MethodParameter} for the declared return type.
   */
  public MethodParameter getReturnType() {
    return new AnnotatedMethodParameter(-1);
  }

  /**
   * Return the actual return type.
   */
  public final Class<?> getRawReturnType() {
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
   * Return a {@link MethodParameter} for the actual return value type.
   */
  public MethodParameter getReturnValueType(@Nullable Object returnValue) {
    return new ReturnValueMethodParameter(returnValue);
  }

  /**
   * Return {@code true} if the method's return type is void, {@code false} otherwise.
   */
  public boolean isVoid() {
    return returnType == void.class;
  }

  /**
   * Return a single annotation on the underlying method, traversing its super methods
   * if no annotation can be found on the given method itself.
   * <p>Supports <em>merged</em> composed annotations with attribute overrides.
   *
   * @param annotationType the annotation type to look for
   * @return the annotation, or {@code null} if none found
   * @see AnnotatedElementUtils#findMergedAnnotation
   */
  @SuppressWarnings("unchecked")
  public <A extends Annotation> @Nullable A getMethodAnnotation(Class<A> annotationType) {
    Object result = this.annotations.computeIfAbsent(annotationType, key -> {
      Object value = AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
      return value == null ? NullValue.INSTANCE : value;
    });
    return result == NullValue.INSTANCE ? null : (A) result;
  }

  /**
   * Determine if an annotation of the given type is <em>present</em> or
   * <em>meta-present</em> on the method.
   *
   * @param annotationType the annotation type to look for
   * @see AnnotatedElementUtils#hasAnnotation
   */
  public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
    return (getMethodAnnotation(annotationType) != null);
  }

  private List<Annotation[][]> getInheritedParameterAnnotations() {
    List<Annotation[][]> parameterAnnotations = this.inheritedParameterAnnotations;
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
            || (candidate.getParameterCount() != this.method.getParameterCount())
            || !candidate.getName().equals(this.method.getName())) {
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

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof AnnotatedMethod o && this.method.equals(o.method)));
  }

  @Override
  public int hashCode() {
    return this.method.hashCode();
  }

  @Override
  public String toString() {
    return this.method.toGenericString();
  }

  // Support methods for use in subclass variants

  protected static @Nullable Object findProvidedArgument(MethodParameter parameter, @Nullable Object[] providedArgs) {
    if (ObjectUtils.isNotEmpty(providedArgs)) {
      Class<?> parameterType = ClassUtils.resolvePrimitiveIfNecessary(parameter.getParameterType());
      for (Object providedArg : providedArgs) {
        if (parameterType.isInstance(providedArg)) {
          return providedArg;
        }
      }
    }
    return null;
  }

  protected static String formatArgumentError(MethodParameter param, String message) {
    return "Could not resolve parameter [%d] in %s%s".formatted(
            param.getParameterIndex(), param.getExecutable().toGenericString(), StringUtils.hasText(message) ? ": " + message : "");
  }

  /**
   * A MethodParameter with AnnotatedMethod-specific behavior.
   */
  protected class AnnotatedMethodParameter extends SynthesizingMethodParameter {

    private volatile Annotation @Nullable [] combinedAnnotations;

    public AnnotatedMethodParameter(int index) {
      super(AnnotatedMethod.this.getBridgedMethod(), index);
    }

    protected AnnotatedMethodParameter(AnnotatedMethodParameter original) {
      super(original);
      this.combinedAnnotations = original.combinedAnnotations;
    }

    @Override
    public Method getMethod() {
      return AnnotatedMethod.this.getBridgedMethod();
    }

    @Override
    public Class<?> getContainingClass() {
      return AnnotatedMethod.this.getContainingClass();
    }

    @Override
    public <T extends Annotation> @Nullable T getMethodAnnotation(Class<T> annotationType) {
      return AnnotatedMethod.this.getMethodAnnotation(annotationType);
    }

    @Override
    public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
      return AnnotatedMethod.this.hasMethodAnnotation(annotationType);
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
    public AnnotatedMethodParameter clone() {
      return new AnnotatedMethodParameter(this);
    }
  }

  /**
   * A MethodParameter for an AnnotatedMethod return type based on an actual return value.
   */
  private class ReturnValueMethodParameter extends AnnotatedMethodParameter {

    private final @Nullable Class<?> returnValueType;

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

}
