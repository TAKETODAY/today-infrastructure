/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aot.hint.predicate;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import infra.aot.hint.ExecutableHint;
import infra.aot.hint.ExecutableMode;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeHint;
import infra.aot.hint.TypeReference;
import infra.core.MethodIntrospector;
import infra.lang.Assert;
import infra.util.ReflectionUtils;

/**
 * Generator of {@link ReflectionHints} predicates, testing whether the given hints
 * match the expected behavior for reflection.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class ReflectionHintsPredicates {

  ReflectionHintsPredicates() {
  }

  /**
   * Return a predicate that checks whether a reflection hint is registered for the given type.
   * <p>The returned type exposes additional methods that refine the predicate behavior.
   *
   * @param typeReference the type
   * @return the {@link RuntimeHints} predicate
   */
  public TypeHintPredicate onType(TypeReference typeReference) {
    Assert.notNull(typeReference, "'typeReference' is required");
    return new TypeHintPredicate(typeReference);
  }

  /**
   * Return a predicate that checks whether a reflection hint is registered for the given type.
   * <p>The returned type exposes additional methods that refine the predicate behavior.
   *
   * @param type the type
   * @return the {@link RuntimeHints} predicate
   */
  public TypeHintPredicate onType(Class<?> type) {
    Assert.notNull(type, "'type' is required");
    return new TypeHintPredicate(TypeReference.of(type));
  }

  /**
   * Return a predicate that checks whether a reflection hint is registered for the given constructor.
   * By default, both introspection and invocation hints match.
   * <p>The returned type exposes additional methods that refine the predicate behavior.
   *
   * @param constructor the constructor
   * @return the {@link RuntimeHints} predicate
   * @deprecated since 5.0 in favor of {@link #onConstructorInvocation(Constructor)}
   * or {@link #onType(Class)}.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  public ConstructorHintPredicate onConstructor(Constructor<?> constructor) {
    Assert.notNull(constructor, "'constructor' is required");
    return new ConstructorHintPredicate(constructor);
  }

  /**
   * Return a predicate that checks whether an invocation hint is registered for the given constructor.
   *
   * @param constructor the constructor
   * @return the {@link RuntimeHints} predicate
   * @since 5.0
   */
  public Predicate<RuntimeHints> onConstructorInvocation(Constructor<?> constructor) {
    Assert.notNull(constructor, "'constructor' is required");
    return new ConstructorHintPredicate(constructor).invoke();
  }

  /**
   * Return a predicate that checks whether a reflection hint is registered for the given method.
   * By default, both introspection and invocation hints match.
   * <p>The returned type exposes additional methods that refine the predicate behavior.
   *
   * @param method the method
   * @return the {@link RuntimeHints} predicate
   * @deprecated since 5.0 in favor of {@link #onMethodInvocation(Method)}
   * or {@link #onType(Class)}.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  public MethodHintPredicate onMethod(Method method) {
    Assert.notNull(method, "'method' is required");
    return new MethodHintPredicate(method);
  }

  /**
   * Return a predicate that checks whether an invocation hint is registered for the given method.
   *
   * @param method the method
   * @return the {@link RuntimeHints} predicate
   * @since 5.0
   */
  public Predicate<RuntimeHints> onMethodInvocation(Method method) {
    Assert.notNull(method, "'method' is required");
    return new MethodHintPredicate(method).invoke();
  }

  /**
   * Return a predicate that checks whether a reflection hint is registered for the method that matches the given selector.
   * This looks up a method on the given type with the expected name, if unique.
   * By default, both introspection and invocation hints match.
   * <p>The returned type exposes additional methods that refine the predicate behavior.
   *
   * @param type the type holding the method
   * @param methodName the method name
   * @return the {@link RuntimeHints} predicate
   * @throws IllegalArgumentException if the method cannot be found or if multiple methods are found with the same name.
   * @deprecated since 5.0 in favor of {@link #onMethodInvocation(Class, String)}
   * or {@link #onType(Class)}.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  public MethodHintPredicate onMethod(Class<?> type, String methodName) {
    Assert.notNull(type, "'type' is required");
    Assert.hasText(methodName, "'methodName' must not be empty");
    return new MethodHintPredicate(getMethod(type, methodName));
  }

  /**
   * Return a predicate that checks whether an invocation hint is registered for the method that matches the given selector.
   * This looks up a method on the given type with the expected name, if unique.
   *
   * @param type the type holding the method
   * @param methodName the method name
   * @return the {@link RuntimeHints} predicate
   * @throws IllegalArgumentException if the method cannot be found or if multiple methods are found with the same name.
   * @since 5.0
   */
  public Predicate<RuntimeHints> onMethodInvocation(Class<?> type, String methodName) {
    Assert.notNull(type, "'type' is required");
    Assert.hasText(methodName, "'methodName' must not be empty");
    return new MethodHintPredicate(getMethod(type, methodName)).invoke();
  }

  /**
   * Return a predicate that checks whether a reflection hint is registered for the method that matches the given selector.
   * This looks up a method on the given type with the expected name, if unique.
   * By default, both introspection and invocation hints match.
   * <p>The returned type exposes additional methods that refine the predicate behavior.
   *
   * @param className the name of the class holding the method
   * @param methodName the method name
   * @return the {@link RuntimeHints} predicate
   * @throws ClassNotFoundException if the class cannot be resolved.
   * @throws IllegalArgumentException if the method cannot be found or if multiple methods are found with the same name.
   * @deprecated since 5.0 in favor of {@link #onMethodInvocation(String, String)}
   * or {@link #onType(Class)}.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  public MethodHintPredicate onMethod(String className, String methodName) throws ClassNotFoundException {
    Assert.hasText(className, "'className' must not be empty");
    Assert.hasText(methodName, "'methodName' must not be empty");
    return onMethod(Class.forName(className), methodName);
  }

  /**
   * Return a predicate that checks whether an invocation hint is registered for the method that matches the given selector.
   * This looks up a method on the given type with the expected name, if unique.
   *
   * @param className the name of the class holding the method
   * @param methodName the method name
   * @return the {@link RuntimeHints} predicate
   * @throws ClassNotFoundException if the class cannot be resolved.
   * @throws IllegalArgumentException if the method cannot be found or if multiple methods are found with the same name.
   * @since 5.0
   */
  public Predicate<RuntimeHints> onMethodInvocation(String className, String methodName) throws ClassNotFoundException {
    Assert.hasText(className, "'className' must not be empty");
    Assert.hasText(methodName, "'methodName' must not be empty");
    return onMethod(Class.forName(className), methodName).invoke();
  }

  private Method getMethod(Class<?> type, String methodName) {
    ReflectionUtils.MethodFilter selector = method -> methodName.equals(method.getName());
    Set<Method> methods = MethodIntrospector.filterMethods(type, selector);
    if (methods.size() == 1) {
      return methods.iterator().next();
    }
    else if (methods.size() > 1) {
      throw new IllegalArgumentException("Found multiple methods named '%s' on class %s".formatted(methodName, type.getName()));
    }
    else {
      throw new IllegalArgumentException("No method named '%s' on class %s".formatted(methodName, type.getName()));
    }
  }

  /**
   * Return a predicate that checks whether a reflective field access hint is registered for the field.
   * This looks up a field on the given type with the expected name, if present.
   *
   * @param type the type holding the field
   * @param fieldName the field name
   * @return the {@link RuntimeHints} predicate
   * @throws IllegalArgumentException if a field cannot be found with the given name.
   * @deprecated since 5.0 in favor of {@link #onFieldAccess(Class, String)} with similar semantics.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  public Predicate<RuntimeHints> onField(Class<?> type, String fieldName) {
    return onFieldAccess(type, fieldName);
  }

  /**
   * Return a predicate that checks whether a reflective field access hint is registered for the field.
   * This looks up a field on the given type with the expected name, if present.
   *
   * @param type the type holding the field
   * @param fieldName the field name
   * @return the {@link RuntimeHints} predicate
   * @throws IllegalArgumentException if a field cannot be found with the given name.
   * @since 5.0
   */
  public Predicate<RuntimeHints> onFieldAccess(Class<?> type, String fieldName) {
    Assert.notNull(type, "'type' is required");
    Assert.hasText(fieldName, "'fieldName' must not be empty");
    Field field = ReflectionUtils.findField(type, fieldName);
    if (field == null) {
      throw new IllegalArgumentException("No field named '%s' on class %s".formatted(fieldName, type.getName()));
    }
    return new FieldHintPredicate(field);
  }

  /**
   * Return a predicate that checks whether a reflective field access hint is registered for the field.
   * This looks up a field on the given type with the expected name, if present.
   *
   * @param className the name of the class holding the field
   * @param fieldName the field name
   * @return the {@link RuntimeHints} predicate
   * @throws ClassNotFoundException if the class cannot be resolved.
   * @throws IllegalArgumentException if a field cannot be found with the given name.
   * @deprecated since 5.0 in favor of {@link #onFieldAccess(String, String)} with similar semantics.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  public Predicate<RuntimeHints> onField(String className, String fieldName) throws ClassNotFoundException {
    return onFieldAccess(className, fieldName);
  }

  /**
   * Return a predicate that checks whether an invocation hint is registered for the field.
   * This looks up a field on the given type with the expected name, if present.
   *
   * @param className the name of the class holding the field
   * @param fieldName the field name
   * @return the {@link RuntimeHints} predicate
   * @throws ClassNotFoundException if the class cannot be resolved.
   * @throws IllegalArgumentException if a field cannot be found with the given name.
   * @since 5.0
   */
  public Predicate<RuntimeHints> onFieldAccess(String className, String fieldName) throws ClassNotFoundException {
    Assert.hasText(className, "'className' must not be empty");
    Assert.hasText(fieldName, "'fieldName' must not be empty");
    return onFieldAccess(Class.forName(className), fieldName);
  }

  /**
   * Return a predicate that checks whether a reflective field access hint is registered for the given field.
   *
   * @param field the field
   * @return the {@link RuntimeHints} predicate
   * @deprecated since 5.0 in favor of {@link #onFieldAccess(Field)} with similar semantics.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  public Predicate<RuntimeHints> onField(Field field) {
    return onFieldAccess(field);
  }

  /**
   * Return a predicate that checks whether an invocation hint is registered for the given field.
   *
   * @param field the field
   * @return the {@link RuntimeHints} predicate
   * @since 5.0
   */
  public Predicate<RuntimeHints> onFieldAccess(Field field) {
    Assert.notNull(field, "'field' is required");
    return new FieldHintPredicate(field);
  }

  public static class TypeHintPredicate implements Predicate<RuntimeHints> {

    private final TypeReference type;

    TypeHintPredicate(TypeReference type) {
      this.type = type;
    }

    @Nullable
    private TypeHint getTypeHint(RuntimeHints hints) {
      return hints.reflection().getTypeHint(this.type);
    }

    @Override
    public boolean test(RuntimeHints hints) {
      return getTypeHint(hints) != null;
    }

    /**
     * Refine the current predicate to only match if the given {@link MemberCategory} is present.
     *
     * @param memberCategory the member category
     * @return the refined {@link RuntimeHints} predicate
     */
    public Predicate<RuntimeHints> withMemberCategory(MemberCategory memberCategory) {
      Assert.notNull(memberCategory, "'memberCategory' is required");
      return and(hints -> {
        TypeHint hint = getTypeHint(hints);
        return (hint != null && hint.getMemberCategories().contains(memberCategory));
      });
    }

    /**
     * Refine the current predicate to only match if the given {@link MemberCategory categories} are present.
     *
     * @param memberCategories the member categories
     * @return the refined {@link RuntimeHints} predicate
     */
    public Predicate<RuntimeHints> withMemberCategories(MemberCategory... memberCategories) {
      Assert.notEmpty(memberCategories, "'memberCategories' must not be empty");
      return and(hints -> {
        TypeHint hint = getTypeHint(hints);
        return (hint != null && hint.getMemberCategories().containsAll(Arrays.asList(memberCategories)));
      });
    }

    /**
     * Refine the current predicate to match if any of the given {@link MemberCategory categories} is present.
     *
     * @param memberCategories the member categories
     * @return the refined {@link RuntimeHints} predicate
     */
    public Predicate<RuntimeHints> withAnyMemberCategory(MemberCategory... memberCategories) {
      Assert.notEmpty(memberCategories, "'memberCategories' must not be empty");
      return and(hints -> {
        TypeHint hint = getTypeHint(hints);
        return (hint != null && Arrays.stream(memberCategories)
                .anyMatch(memberCategory -> hint.getMemberCategories().contains(memberCategory)));
      });
    }
  }

  @Deprecated(since = "5.0", forRemoval = true)
  @SuppressWarnings("removal")
  public abstract static class ExecutableHintPredicate<T extends Executable> implements Predicate<RuntimeHints> {

    protected final T executable;

    protected ExecutableMode executableMode = ExecutableMode.INTROSPECT;

    ExecutableHintPredicate(T executable) {
      this.executable = executable;
    }

    /**
     * Refine the current predicate to match for reflection introspection on the current type.
     *
     * @return the refined {@link RuntimeHints} predicate
     */
    public ExecutableHintPredicate<T> introspect() {
      this.executableMode = ExecutableMode.INTROSPECT;
      return this;
    }

    /**
     * Refine the current predicate to match for reflection invocation on the current type.
     *
     * @return the refined {@link RuntimeHints} predicate
     */
    public ExecutableHintPredicate<T> invoke() {
      this.executableMode = ExecutableMode.INVOKE;
      return this;
    }

    abstract Predicate<RuntimeHints> exactMatch();

    /**
     * Indicate whether the specified {@code ExecutableHint} covers the
     * reflection needs of the specified executable definition.
     *
     * @return {@code true} if the member matches (same type, name, and parameters),
     * and the configured {@code ExecutableMode} is compatible
     */
    static boolean includes(ExecutableHint hint, String name,
            List<TypeReference> parameterTypes, ExecutableMode executableModes) {
      return hint.getName().equals(name) && hint.getParameterTypes().equals(parameterTypes) &&
              (hint.getMode().equals(ExecutableMode.INVOKE) || !executableModes.equals(ExecutableMode.INVOKE));
    }
  }

  @Deprecated(since = "5.0", forRemoval = true)
  @SuppressWarnings("removal")
  public static class ConstructorHintPredicate extends ExecutableHintPredicate<Constructor<?>> {

    ConstructorHintPredicate(Constructor<?> constructor) {
      super(constructor);
    }

    @Override
    public boolean test(RuntimeHints runtimeHints) {
      return (new TypeHintPredicate(TypeReference.of(this.executable.getDeclaringClass()))
              .and(hints -> this.executableMode == ExecutableMode.INTROSPECT))
              .or(new TypeHintPredicate(TypeReference.of(this.executable.getDeclaringClass()))
                      .withMemberCategory(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
                      .and(hints -> Modifier.isPublic(this.executable.getModifiers()))
                      .and(hints -> this.executableMode == ExecutableMode.INVOKE))
              .or(new TypeHintPredicate(TypeReference.of(this.executable.getDeclaringClass()))
                      .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
                      .and(hints -> this.executableMode == ExecutableMode.INVOKE))
              .or(exactMatch()).test(runtimeHints);
    }

    @Override
    Predicate<RuntimeHints> exactMatch() {
      return hints -> {
        TypeHint hint = hints.reflection().getTypeHint(this.executable.getDeclaringClass());
        return (hint != null && hint.constructors().anyMatch(executableHint -> {
          List<TypeReference> parameters = TypeReference.listOf(this.executable.getParameterTypes());
          return includes(executableHint, "<init>", parameters, this.executableMode);
        }));
      };
    }
  }

  @Deprecated(since = "5.0", forRemoval = true)
  @SuppressWarnings("removal")
  public static class MethodHintPredicate extends ExecutableHintPredicate<Method> {

    MethodHintPredicate(Method method) {
      super(method);
    }

    @Override
    public boolean test(RuntimeHints runtimeHints) {
      return (new TypeHintPredicate(TypeReference.of(this.executable.getDeclaringClass()))
              .and(hints -> this.executableMode == ExecutableMode.INTROSPECT))
              .or((new TypeHintPredicate(TypeReference.of(this.executable.getDeclaringClass()))
                      .withMemberCategory(MemberCategory.INVOKE_PUBLIC_METHODS)
                      .and(hints -> Modifier.isPublic(this.executable.getModifiers()))
                      .and(hints -> this.executableMode == ExecutableMode.INVOKE)))
              .or((new TypeHintPredicate(TypeReference.of(this.executable.getDeclaringClass()))
                      .withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)
                      .and(hints -> !Modifier.isPublic(this.executable.getModifiers()))
                      .and(hints -> this.executableMode == ExecutableMode.INVOKE)))
              .or(exactMatch()).test(runtimeHints);
    }

    @Override
    Predicate<RuntimeHints> exactMatch() {
      return hints -> {
        TypeHint hint = hints.reflection().getTypeHint(this.executable.getDeclaringClass());
        return (hint != null && hint.methods().anyMatch(executableHint -> {
          List<TypeReference> parameters = TypeReference.listOf(this.executable.getParameterTypes());
          return includes(executableHint, this.executable.getName(), parameters, this.executableMode);
        }));
      };
    }
  }

  @Deprecated(since = "5.0", forRemoval = true)
  public static class FieldHintPredicate implements Predicate<RuntimeHints> {

    private final Field field;

    FieldHintPredicate(Field field) {
      this.field = field;
    }

    @Override
    public boolean test(RuntimeHints runtimeHints) {
      TypeHint typeHint = runtimeHints.reflection().getTypeHint(this.field.getDeclaringClass());
      if (typeHint == null) {
        return false;
      }
      return memberCategoryMatch(typeHint) || exactMatch(typeHint);
    }

    @SuppressWarnings("removal")
    private boolean memberCategoryMatch(TypeHint typeHint) {
      if (Modifier.isPublic(this.field.getModifiers())) {
        return typeHint.getMemberCategories().contains(MemberCategory.ACCESS_PUBLIC_FIELDS)
                || typeHint.getMemberCategories().contains(MemberCategory.PUBLIC_FIELDS);
      }
      else {
        return typeHint.getMemberCategories().contains(MemberCategory.ACCESS_DECLARED_FIELDS)
                || typeHint.getMemberCategories().contains(MemberCategory.DECLARED_FIELDS);
      }
    }

    private boolean exactMatch(TypeHint typeHint) {
      return typeHint.fields().anyMatch(fieldHint ->
              this.field.getName().equals(fieldHint.getName()));
    }
  }

}
