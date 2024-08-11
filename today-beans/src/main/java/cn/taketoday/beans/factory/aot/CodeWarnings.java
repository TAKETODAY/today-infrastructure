/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.beans.factory.aot;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.javapoet.AnnotationSpec;
import cn.taketoday.javapoet.AnnotationSpec.Builder;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.FieldSpec;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.TypeSpec;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Helper class to register warnings that the compiler may trigger on
 * generated code.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SuppressWarnings
 * @since 4.0
 */
public class CodeWarnings {

  private final Set<String> warnings = new LinkedHashSet<>();

  /**
   * Register a warning to be included for this block. Does nothing if
   * the warning is already registered.
   *
   * @param warning the warning to register, if it hasn't been already
   */
  public void register(String warning) {
    this.warnings.add(warning);
  }

  /**
   * Detect the presence of {@link Deprecated} on the specified elements.
   *
   * @param elements the elements to check
   * @return {@code this} instance
   */
  public CodeWarnings detectDeprecation(AnnotatedElement... elements) {
    for (AnnotatedElement element : elements) {
      registerDeprecationIfNecessary(element);
    }
    return this;
  }

  /**
   * Detect the presence of {@link Deprecated} on the specified elements.
   *
   * @param elements the elements to check
   * @return {@code this} instance
   */
  public CodeWarnings detectDeprecation(Stream<AnnotatedElement> elements) {
    elements.forEach(element -> register(element.getAnnotation(Deprecated.class)));
    return this;
  }

  /**
   * Detect the presence of {@link Deprecated} on the signature of the
   * specified {@link ResolvableType}.
   *
   * @param resolvableType a type signature
   * @return {@code this} instance
   */
  public CodeWarnings detectDeprecation(ResolvableType resolvableType) {
    if (ResolvableType.NONE.equals(resolvableType)) {
      return this;
    }
    Class<?> type = ClassUtils.getUserClass(resolvableType.toClass());
    detectDeprecation(type);
    if (resolvableType.hasGenerics() && !resolvableType.hasUnresolvableGenerics()) {
      for (ResolvableType generic : resolvableType.getGenerics()) {
        detectDeprecation(generic);
      }
    }
    return this;
  }

  /**
   * Include {@link SuppressWarnings} on the specified method if necessary.
   *
   * @param method the method to update
   */
  public void suppress(MethodSpec.Builder method) {
    suppress(annotationBuilder -> method.addAnnotation(annotationBuilder.build()));
  }

  /**
   * Include {@link SuppressWarnings} on the specified type if necessary.
   *
   * @param type the type to update
   */
  public void suppress(TypeSpec.Builder type) {
    suppress(annotationBuilder -> type.addAnnotation(annotationBuilder.build()));
  }

  /**
   * Consume the builder for {@link SuppressWarnings} if necessary. If this
   * instance has no warnings registered, the consumer is not invoked.
   *
   * @param annotationSpec a consumer of the {@link Builder}
   * @see MethodSpec.Builder#addAnnotation(AnnotationSpec)
   * @see TypeSpec.Builder#addAnnotation(AnnotationSpec)
   * @see FieldSpec.Builder#addAnnotation(AnnotationSpec)
   */
  protected void suppress(Consumer<Builder> annotationSpec) {
    if (!this.warnings.isEmpty()) {
      Builder annotation = AnnotationSpec.builder(SuppressWarnings.class)
              .addMember("value", generateValueCode());
      annotationSpec.accept(annotation);
    }
  }

  /**
   * Return the currently registered warnings.
   *
   * @return the warnings
   */
  protected Set<String> getWarnings() {
    return Collections.unmodifiableSet(this.warnings);
  }

  private void registerDeprecationIfNecessary(@Nullable AnnotatedElement element) {
    if (element == null) {
      return;
    }
    register(element.getAnnotation(Deprecated.class));
    if (element instanceof Class<?> type) {
      registerDeprecationIfNecessary(type.getEnclosingClass());
    }
  }

  private void register(@Nullable Deprecated annotation) {
    if (annotation != null) {
      if (annotation.forRemoval()) {
        register("removal");
      }
      else {
        register("deprecation");
      }
    }
  }

  private CodeBlock generateValueCode() {
    if (this.warnings.size() == 1) {
      return CodeBlock.of("$S", this.warnings.iterator().next());
    }
    CodeBlock values = CodeBlock.join(this.warnings.stream()
            .map(warning -> CodeBlock.of("$S", warning)).toList(), ", ");
    return CodeBlock.of("{ $L }", values);
  }

  @Override
  public String toString() {
    return CodeWarnings.class.getSimpleName() + this.warnings;
  }

}
