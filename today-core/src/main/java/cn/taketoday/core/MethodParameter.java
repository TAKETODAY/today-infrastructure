/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Helper class that encapsulates the specification of a method parameter, i.e. a {@link Method}
 * or {@link Constructor} plus a parameter index and a nested type index for a declared generic
 * type. Useful as a specification object to pass along.
 *
 * <p>there is a {@link cn.taketoday.core.annotation.SynthesizingMethodParameter}
 * subclass available which synthesizes annotations with attribute aliases. That subclass is used
 * for web and message endpoint processing, in particular.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Andy Clement
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.core.annotation.SynthesizingMethodParameter
 * @since 4.0
 */
public class MethodParameter {

  private final Executable executable;

  private final int parameterIndex;

  @Nullable
  private volatile Parameter parameter;

  private int nestingLevel;

  /** Map from Integer level to Integer type index. */
  @Nullable
  Map<Integer, Integer> typeIndexesPerLevel;

  /** The containing class. Could also be supplied by overriding {@link #getContainingClass()} */
  @Nullable
  private Class<?> containingClass;

  @Nullable
  private volatile Class<?> parameterType;

  @Nullable
  private volatile Type genericParameterType;

  @Nullable
  private volatile Annotation[] parameterAnnotations;

  @Nullable
  private volatile ParameterNameDiscoverer parameterNameDiscoverer;

  @Nullable
  volatile String parameterName;

  @Nullable
  private volatile MethodParameter nestedMethodParameter;

  /**
   * Create a new {@code MethodParameter} for the given method, with nesting level 1.
   *
   * @param method the Method to specify a parameter for
   * @param parameterIndex the index of the parameter: -1 for the method
   * return type; 0 for the first method parameter; 1 for the second method
   * parameter, etc.
   */
  public MethodParameter(Method method, int parameterIndex) {
    this(method, parameterIndex, 1);
  }

  /**
   * Create a new {@code MethodParameter} for the given method.
   *
   * @param method the Method to specify a parameter for
   * @param parameterIndex the index of the parameter: -1 for the method
   * return type; 0 for the first method parameter; 1 for the second method
   * parameter, etc.
   * @param nestingLevel the nesting level of the target type
   * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
   * nested List, whereas 2 would indicate the element of the nested List)
   */
  public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
    Assert.notNull(method, "Method must not be null");
    this.executable = method;
    this.parameterIndex = validateIndex(method, parameterIndex);
    this.nestingLevel = nestingLevel;
  }

  /**
   * Create a new MethodParameter for the given constructor, with nesting level 1.
   *
   * @param constructor the Constructor to specify a parameter for
   * @param parameterIndex the index of the parameter
   */
  public MethodParameter(Constructor<?> constructor, int parameterIndex) {
    this(constructor, parameterIndex, 1);
  }

  /**
   * Create a new MethodParameter for the given constructor.
   *
   * @param constructor the Constructor to specify a parameter for
   * @param parameterIndex the index of the parameter
   * @param nestingLevel the nesting level of the target type
   * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
   * nested List, whereas 2 would indicate the element of the nested List)
   */
  public MethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
    Assert.notNull(constructor, "Constructor must not be null");
    this.executable = constructor;
    this.parameterIndex = validateIndex(constructor, parameterIndex);
    this.nestingLevel = nestingLevel;
  }

  /**
   * Internal constructor used to create a {@link MethodParameter} with a
   * containing class already set.
   *
   * @param executable the Executable to specify a parameter for
   * @param parameterIndex the index of the parameter
   * @param containingClass the containing class
   */
  MethodParameter(Executable executable, int parameterIndex, @Nullable Class<?> containingClass) {
    Assert.notNull(executable, "Executable must not be null");
    this.nestingLevel = 1;
    this.executable = executable;
    this.containingClass = containingClass;
    this.parameterIndex = validateIndex(executable, parameterIndex);
  }

  /**
   * Copy constructor, resulting in an independent MethodParameter object
   * based on the same metadata and cache state that the original object was in.
   *
   * @param original the original MethodParameter object to copy from
   */
  public MethodParameter(MethodParameter original) {
    Assert.notNull(original, "Original must not be null");
    this.executable = original.executable;
    this.parameterIndex = original.parameterIndex;
    this.parameter = original.parameter;
    this.nestingLevel = original.nestingLevel;
    this.typeIndexesPerLevel = original.typeIndexesPerLevel;
    this.containingClass = original.containingClass;
    this.parameterType = original.parameterType;
    this.genericParameterType = original.genericParameterType;
    this.parameterAnnotations = original.parameterAnnotations;
    this.parameterNameDiscoverer = original.parameterNameDiscoverer;
    this.parameterName = original.parameterName;
  }

  /**
   * Return the wrapped Method, if any.
   * <p>Note: Either Method or Constructor is available.
   *
   * @return the Method, or {@code null} if none
   */
  @Nullable
  public Method getMethod() {
    return this.executable instanceof Method ? (Method) this.executable : null;
  }

  /**
   * Return the wrapped Constructor, if any.
   * <p>Note: Either Method or Constructor is available.
   *
   * @return the Constructor, or {@code null} if none
   */
  @Nullable
  public Constructor<?> getConstructor() {
    return this.executable instanceof Constructor ? (Constructor<?>) this.executable : null;
  }

  /**
   * Return the class that declares the underlying Method or Constructor.
   */
  public Class<?> getDeclaringClass() {
    return this.executable.getDeclaringClass();
  }

  /**
   * Return the wrapped member.
   *
   * @return the Method or Constructor as Member
   */
  public Member getMember() {
    return this.executable;
  }

  /**
   * Return the wrapped annotated element.
   * <p>Note: This method exposes the annotations declared on the method/constructor
   * itself (i.e. at the method/constructor level, not at the parameter level).
   *
   * @return the Method or Constructor as AnnotatedElement
   */
  public AnnotatedElement getAnnotatedElement() {
    return this.executable;
  }

  /**
   * Return the wrapped executable.
   *
   * @return the Method or Constructor as Executable
   */
  public Executable getExecutable() {
    return this.executable;
  }

  /**
   * Return the {@link Parameter} descriptor for method/constructor parameter.
   */
  public Parameter getParameter() {
    if (this.parameterIndex < 0) {
      throw new IllegalStateException("Cannot retrieve Parameter descriptor for method return type");
    }
    Parameter parameter = this.parameter;
    if (parameter == null) {
      parameter = getExecutable().getParameters()[this.parameterIndex];
      this.parameter = parameter;
    }
    return parameter;
  }

  /**
   * Return the index of the method/constructor parameter.
   *
   * @return the parameter index (-1 in case of the return type)
   */
  public int getParameterIndex() {
    return this.parameterIndex;
  }

  /**
   * Return the nesting level of the target type
   * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
   * nested List, whereas 2 would indicate the element of the nested List).
   */
  public int getNestingLevel() {
    return this.nestingLevel;
  }

  /**
   * Return a variant of this {@code MethodParameter} with the type
   * for the current level set to the specified value.
   *
   * @param typeIndex the new type index
   */
  public MethodParameter withTypeIndex(int typeIndex) {
    return nested(this.nestingLevel, typeIndex);
  }

  /**
   * Return the type index for the current nesting level.
   *
   * @return the corresponding type index, or {@code null}
   * if none specified (indicating the default type index)
   * @see #getNestingLevel()
   */
  @Nullable
  public Integer getTypeIndexForCurrentLevel() {
    return getTypeIndexForLevel(this.nestingLevel);
  }

  /**
   * Return the type index for the specified nesting level.
   *
   * @param nestingLevel the nesting level to check
   * @return the corresponding type index, or {@code null}
   * if none specified (indicating the default type index)
   */
  @Nullable
  public Integer getTypeIndexForLevel(int nestingLevel) {
    return getTypeIndexesPerLevel().get(nestingLevel);
  }

  /**
   * Obtain the (lazily constructed) type-indexes-per-level Map.
   */
  private Map<Integer, Integer> getTypeIndexesPerLevel() {
    if (this.typeIndexesPerLevel == null) {
      this.typeIndexesPerLevel = new HashMap<>(4);
    }
    return this.typeIndexesPerLevel;
  }

  /**
   * Return a variant of this {@code MethodParameter} which points to the
   * same parameter but one nesting level deeper.
   */
  public MethodParameter nested() {
    return nested(null);
  }

  /**
   * Return a variant of this {@code MethodParameter} which points to the
   * same parameter but one nesting level deeper.
   *
   * @param typeIndex the type index for the new nesting level
   */
  public MethodParameter nested(@Nullable Integer typeIndex) {
    MethodParameter nestedParam = this.nestedMethodParameter;
    if (nestedParam != null && typeIndex == null) {
      return nestedParam;
    }
    nestedParam = nested(this.nestingLevel + 1, typeIndex);
    if (typeIndex == null) {
      this.nestedMethodParameter = nestedParam;
    }
    return nestedParam;
  }

  private MethodParameter nested(int nestingLevel, @Nullable Integer typeIndex) {
    MethodParameter copy = clone();
    copy.nestingLevel = nestingLevel;
    if (this.typeIndexesPerLevel != null) {
      copy.typeIndexesPerLevel = new HashMap<>(this.typeIndexesPerLevel);
    }
    if (typeIndex != null) {
      copy.getTypeIndexesPerLevel().put(copy.nestingLevel, typeIndex);
    }
    copy.parameterType = null;
    copy.genericParameterType = null;
    return copy;
  }

  /**
   * Return whether this method indicates a parameter which is not required:
   * either in the form of Java 8's {@link java.util.Optional}, any variant
   * of a parameter-level {@code Nullable} annotation (such as from JSR-305
   * or the FindBugs set of annotations), or a language-level nullable type
   * declaration or {@code Continuation} parameter in Kotlin.
   */
  public boolean isOptional() {
    return getParameterType() == Optional.class || isNullable();
  }

  /**
   * Return whether this method indicates a parameter which can be {@code null}:
   * either in the form of any variant of a parameter-level {@code Nullable}
   * annotation (such as from JSR-305 or the FindBugs set of annotations),
   * or a language-level nullable type declaration
   */
  public boolean isNullable() {
    for (Annotation ann : getParameterAnnotations()) {
      if ("Nullable".equals(ann.annotationType().getSimpleName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return a variant of this {@code MethodParameter} which points to
   * the same parameter but one nesting level deeper in case of a
   * {@link java.util.Optional} declaration.
   *
   * @see #isOptional()
   * @see #nested()
   */
  public MethodParameter nestedIfOptional() {
    return getParameterType() == Optional.class ? nested() : this;
  }

  /**
   * Return a variant of this {@code MethodParameter} which refers to the
   * given containing class.
   *
   * @param containingClass a specific containing class (potentially a
   * subclass of the declaring class, e.g. substituting a type variable)
   * @see #getParameterType()
   */
  public MethodParameter withContainingClass(@Nullable Class<?> containingClass) {
    MethodParameter result = clone();
    result.parameterType = null;
    result.containingClass = containingClass;
    return result;
  }

  /**
   * Return the containing class for this method parameter.
   *
   * @return a specific containing class (potentially a subclass of the
   * declaring class), or otherwise simply the declaring class itself
   * @see #getDeclaringClass()
   */
  public Class<?> getContainingClass() {
    Class<?> containingClass = this.containingClass;
    return containingClass != null ? containingClass : getDeclaringClass();
  }

  /**
   * Return the type of the method/constructor parameter.
   *
   * @return the parameter type (never {@code null})
   */
  public Class<?> getParameterType() {
    Class<?> paramType = this.parameterType;
    if (paramType != null) {
      return paramType;
    }
    if (getContainingClass() != getDeclaringClass()) {
      paramType = ResolvableType.forMethodParameter(this, null, 1).resolve();
    }
    if (paramType == null) {
      paramType = computeParameterType();
    }
    this.parameterType = paramType;
    return paramType;
  }

  /**
   * Return the generic type of the method/constructor parameter.
   *
   * @return the parameter type (never {@code null})
   */
  public Type getGenericParameterType() {
    Type paramType = this.genericParameterType;
    if (paramType == null) {
      if (this.parameterIndex < 0) {
        Method method = getMethod();
        paramType = method != null ? method.getGenericReturnType() : void.class;
      }
      else {
        Type[] genericParameterTypes = this.executable.getGenericParameterTypes();
        int index = this.parameterIndex;
        if (this.executable instanceof Constructor &&
                ClassUtils.isInnerClass(this.executable.getDeclaringClass()) &&
                genericParameterTypes.length == this.executable.getParameterCount() - 1) {
          // Bug in javac: type array excludes enclosing instance parameter
          // for inner classes with at least one generic constructor parameter,
          // so access it with the actual parameter index lowered by 1
          index = this.parameterIndex - 1;
        }
        paramType = (index >= 0 && index < genericParameterTypes.length ?
                     genericParameterTypes[index] : computeParameterType());
      }
      this.genericParameterType = paramType;
    }
    return paramType;
  }

  private Class<?> computeParameterType() {
    if (this.parameterIndex < 0) {
      Method method = getMethod();
      if (method == null) {
        return void.class;
      }
      return method.getReturnType();
    }
    return this.executable.getParameterTypes()[this.parameterIndex];
  }

  /**
   * Return the nested type of the method/constructor parameter.
   *
   * @return the parameter type (never {@code null})
   * @see #getNestingLevel()
   */
  public Class<?> getNestedParameterType() {
    if (this.nestingLevel > 1) {
      Type type = getGenericParameterType();
      for (int i = 2; i <= this.nestingLevel; i++) {
        if (type instanceof ParameterizedType) {
          Type[] args = ((ParameterizedType) type).getActualTypeArguments();
          Integer index = getTypeIndexForLevel(i);
          type = args[index != null ? index : args.length - 1];
        }
        // TODO: Object.class if unresolvable
      }
      if (type instanceof Class) {
        return (Class<?>) type;
      }
      else if (type instanceof ParameterizedType) {
        Type arg = ((ParameterizedType) type).getRawType();
        if (arg instanceof Class) {
          return (Class<?>) arg;
        }
      }
      return Object.class;
    }
    else {
      return getParameterType();
    }
  }

  /**
   * Return the nested generic type of the method/constructor parameter.
   *
   * @return the parameter type (never {@code null})
   * @see #getNestingLevel()
   */
  public Type getNestedGenericParameterType() {
    if (this.nestingLevel > 1) {
      Type type = getGenericParameterType();
      for (int i = 2; i <= this.nestingLevel; i++) {
        if (type instanceof ParameterizedType) {
          Type[] args = ((ParameterizedType) type).getActualTypeArguments();
          Integer index = getTypeIndexForLevel(i);
          type = args[index != null ? index : args.length - 1];
        }
      }
      return type;
    }
    else {
      return getGenericParameterType();
    }
  }

  /**
   * Return the annotations associated with the target method/constructor itself.
   */
  public Annotation[] getMethodAnnotations() {
    return adaptAnnotationArray(getAnnotatedElement().getAnnotations());
  }

  /**
   * Return the method/constructor annotation of the given type, if available.
   *
   * @param annotationType the annotation type to look for
   * @return the annotation object, or {@code null} if not found
   */
  @Nullable
  public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
    A annotation = getAnnotatedElement().getAnnotation(annotationType);
    return annotation != null ? adaptAnnotation(annotation) : null;
  }

  /**
   * Return whether the method/constructor is annotated with the given type.
   *
   * @param annotationType the annotation type to look for
   * @see #getMethodAnnotation(Class)
   */
  public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
    return getAnnotatedElement().isAnnotationPresent(annotationType);
  }

  /**
   * Return the annotations associated with the specific method/constructor parameter.
   */
  public Annotation[] getParameterAnnotations() {
    Annotation[] paramAnns = this.parameterAnnotations;
    if (paramAnns == null) {
      Annotation[][] annotationArray = this.executable.getParameterAnnotations();
      int index = this.parameterIndex;
      if (this.executable instanceof Constructor
              && ClassUtils.isInnerClass(this.executable.getDeclaringClass())
              && annotationArray.length == this.executable.getParameterCount() - 1) {
        // Bug in javac in JDK <9: annotation array excludes enclosing instance parameter
        // for inner classes, so access it with the actual parameter index lowered by 1
        index = this.parameterIndex - 1;
      }
      paramAnns = index >= 0 && index < annotationArray.length
                  ? adaptAnnotationArray(annotationArray[index])
                  : Constant.EMPTY_ANNOTATIONS;
      this.parameterAnnotations = paramAnns;
    }
    return paramAnns;
  }

  /**
   * Return {@code true} if the parameter has at least one annotation,
   * {@code false} if it has none.
   *
   * @see #getParameterAnnotations()
   */
  public boolean hasParameterAnnotations() {
    return getParameterAnnotations().length != 0;
  }

  /**
   * Return the parameter annotation of the given type, if available.
   *
   * @param annotationType the annotation type to look for
   * @return the annotation object, or {@code null} if not found
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <A extends Annotation> A getParameterAnnotation(Class<A> annotationType) {
    Annotation[] anns = getParameterAnnotations();
    for (Annotation ann : anns) {
      if (annotationType.isInstance(ann)) {
        return (A) ann;
      }
    }
    return null;
  }

  /**
   * Return whether the parameter is declared with the given annotation type.
   *
   * @param annotationType the annotation type to look for
   * @see #getParameterAnnotation(Class)
   */
  public <A extends Annotation> boolean hasParameterAnnotation(Class<A> annotationType) {
    return getParameterAnnotation(annotationType) != null;
  }

  /**
   * Initialize parameter name discovery for this method parameter.
   * <p>This method does not actually try to retrieve the parameter name at
   * this point; it just allows discovery to happen when the application calls
   * {@link #getParameterName()} (if ever).
   */
  public void initParameterNameDiscovery(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  /**
   * Return the name of the method/constructor parameter.
   *
   * @return the parameter name (may be {@code null} if no
   * parameter name metadata is contained in the class file or no
   * {@link #initParameterNameDiscovery ParameterNameDiscoverer}
   * has been set to begin with)
   */
  @Nullable
  public String getParameterName() {
    if (this.parameterIndex < 0) {
      return null;
    }
    String parameterName = this.parameterName;
    if (parameterName == null) {
      ParameterNameDiscoverer discoverer = this.parameterNameDiscoverer;
      if (discoverer != null) {
        String[] parameterNames = discoverer.getParameterNames(this.executable);
        if (parameterNames != null) {
          parameterName = parameterNames[this.parameterIndex];
          this.parameterName = parameterName;
        }
        this.parameterNameDiscoverer = null;
      }
    }
    return parameterName;
  }

  /**
   * A template method to post-process a given annotation instance before
   * returning it to the caller.
   * <p>The default implementation simply returns the given annotation as-is.
   *
   * @param annotation the annotation about to be returned
   * @return the post-processed annotation (or simply the original one)
   */
  protected <A extends Annotation> A adaptAnnotation(A annotation) {
    return annotation;
  }

  /**
   * A template method to post-process a given annotation array before
   * returning it to the caller.
   * <p>The default implementation simply returns the given annotation array as-is.
   *
   * @param annotations the annotation array about to be returned
   * @return the post-processed annotation array (or simply the original one)
   */
  protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
    return annotations;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MethodParameter otherParam)) {
      return false;
    }
    return getContainingClass() == otherParam.getContainingClass()
            && ObjectUtils.nullSafeEquals(this.typeIndexesPerLevel, otherParam.typeIndexesPerLevel)
            && this.nestingLevel == otherParam.nestingLevel
            && this.parameterIndex == otherParam.parameterIndex
            && this.executable.equals(otherParam.executable);
  }

  @Override
  public int hashCode() {
    return (31 * this.executable.hashCode() + this.parameterIndex);
  }

  @Override
  public String toString() {
    Method method = getMethod();
    return (method != null ? "method '" + method.getName() + "'" : "constructor") +
            " parameter " + this.parameterIndex;
  }

  @Override
  public MethodParameter clone() {
    return new MethodParameter(this);
  }

  /**
   * Create a new MethodParameter for the given method or constructor.
   * <p>This is a convenience factory method for scenarios where a
   * Method or Constructor reference is treated in a generic fashion.
   *
   * @param executable the Method or Constructor to specify a parameter for
   * @param parameterIndex the index of the parameter
   * @return the corresponding MethodParameter instance
   */
  public static MethodParameter forExecutable(Executable executable, int parameterIndex) {
    if (executable instanceof Method) {
      return new MethodParameter((Method) executable, parameterIndex);
    }
    else if (executable instanceof Constructor) {
      return new MethodParameter((Constructor<?>) executable, parameterIndex);
    }
    else {
      throw new IllegalArgumentException("Not a Method/Constructor: " + executable);
    }
  }

  /**
   * Create a new MethodParameter for the given parameter descriptor.
   * <p>This is a convenience factory method for scenarios where a
   * Java 8 {@link Parameter} descriptor is already available.
   *
   * @param parameter the parameter descriptor
   * @return the corresponding MethodParameter instance
   */
  public static MethodParameter forParameter(Parameter parameter) {
    return forExecutable(parameter.getDeclaringExecutable(), ReflectionUtils.getParameterIndex(parameter));
  }

  private static int validateIndex(Executable executable, int parameterIndex) {
    int count = executable.getParameterCount();
    Assert.isTrue(parameterIndex >= -1 && parameterIndex < count,
            () -> "Parameter index needs to be between -1 and " + (count - 1));
    return parameterIndex;
  }

  /**
   * Create a new MethodParameter for the given field-aware constructor,
   * e.g. on a data class or record type.
   * <p>A field-aware method parameter will detect field annotations as well,
   * as long as the field name matches the parameter name.
   *
   * @param ctor the Constructor to specify a parameter for
   * @param parameterIndex the index of the parameter
   * @param fieldName the name of the underlying field,
   * matching the constructor's parameter name
   * @return the corresponding MethodParameter instance
   * @since 4.0
   */
  public static MethodParameter forFieldAwareConstructor(Constructor<?> ctor, int parameterIndex, String fieldName) {
    return new FieldAwareConstructorParameter(ctor, parameterIndex, fieldName);
  }

  /**
   * {@link MethodParameter} subclass which detects field annotations as well.
   */
  private static class FieldAwareConstructorParameter extends MethodParameter {

    @Nullable
    private volatile Annotation[] combinedAnnotations;

    public FieldAwareConstructorParameter(Constructor<?> constructor, int parameterIndex, String fieldName) {
      super(constructor, parameterIndex);
      this.parameterName = fieldName;
    }

    @Override
    public Annotation[] getParameterAnnotations() {
      String parameterName = this.parameterName;
      Assert.state(parameterName != null, "Parameter name not initialized");

      Annotation[] anns = this.combinedAnnotations;
      if (anns == null) {
        anns = super.getParameterAnnotations();
        try {
          Field field = getDeclaringClass().getDeclaredField(parameterName);
          Annotation[] fieldAnns = field.getAnnotations();
          if (fieldAnns.length > 0) {
            List<Annotation> merged = new ArrayList<>(anns.length + fieldAnns.length);
            merged.addAll(Arrays.asList(anns));
            for (Annotation fieldAnn : fieldAnns) {
              boolean existingType = false;
              for (Annotation ann : anns) {
                if (ann.annotationType() == fieldAnn.annotationType()) {
                  existingType = true;
                  break;
                }
              }
              if (!existingType) {
                merged.add(fieldAnn);
              }
            }
            anns = merged.toArray(new Annotation[0]);
          }
        }
        catch (NoSuchFieldException | SecurityException ex) {
          // ignore
        }
        this.combinedAnnotations = anns;
      }
      return anns;
    }
  }

}
