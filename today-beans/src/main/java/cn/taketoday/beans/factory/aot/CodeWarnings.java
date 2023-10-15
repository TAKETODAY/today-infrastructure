/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.factory.aot;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Stream;

import cn.taketoday.javapoet.AnnotationSpec;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.lang.Nullable;

/**
 * Helper class to register warnings that the compiler may trigger on
 * generated code.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SuppressWarnings
 * @since 4.0
 */
class CodeWarnings {

  private final LinkedHashSet<String> warnings = new LinkedHashSet<>();

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
      register(element.getAnnotation(Deprecated.class));
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
   * Include {@link SuppressWarnings} on the specified method if necessary.
   *
   * @param method the method to update
   */
  public void suppress(MethodSpec.Builder method) {
    if (this.warnings.isEmpty()) {
      return;
    }
    method.addAnnotation(buildAnnotationSpec());
  }

  /**
   * Return the currently registered warnings.
   *
   * @return the warnings
   */
  protected Set<String> getWarnings() {
    return Collections.unmodifiableSet(this.warnings);
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

  private AnnotationSpec buildAnnotationSpec() {
    return AnnotationSpec.builder(SuppressWarnings.class)
            .addMember("value", generateValueCode()).build();
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
    return new StringJoiner(", ", CodeWarnings.class.getSimpleName(), "")
            .add(this.warnings.toString())
            .toString();
  }

}
