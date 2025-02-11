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

package infra.aot.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import infra.javapoet.CodeBlock;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Code generator for a single value. Delegates code generation to a list of
 * configurable {@link Delegate} implementations.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class ValueCodeGenerator {

  private static final ValueCodeGenerator INSTANCE = new ValueCodeGenerator(ValueCodeGeneratorDelegates.INSTANCES, null);

  private static final CodeBlock NULL_VALUE_CODE_BLOCK = CodeBlock.of("null");

  private final List<Delegate> delegates;

  @Nullable
  private final GeneratedMethods generatedMethods;

  private ValueCodeGenerator(List<Delegate> delegates, @Nullable GeneratedMethods generatedMethods) {
    this.delegates = delegates;
    this.generatedMethods = generatedMethods;
  }

  /**
   * Return an instance that provides support for {@linkplain
   * ValueCodeGeneratorDelegates#INSTANCES common value types}.
   *
   * @return an instance with support for common value types
   */
  public static ValueCodeGenerator withDefaults() {
    return INSTANCE;
  }

  /**
   * Create an instance with the specified {@link Delegate} implementations.
   *
   * @param delegates the delegates to use
   * @return an instance with the specified delegates
   */
  public static ValueCodeGenerator with(Delegate... delegates) {
    return with(Arrays.asList(delegates));
  }

  /**
   * Create an instance with the specified {@link Delegate} implementations.
   *
   * @param delegates the delegates to use
   * @return an instance with the specified delegates
   */
  public static ValueCodeGenerator with(List<Delegate> delegates) {
    Assert.notEmpty(delegates, "Delegates must not be empty");
    return new ValueCodeGenerator(new ArrayList<>(delegates), null);
  }

  public ValueCodeGenerator add(List<Delegate> additionalDelegates) {
    Assert.notEmpty(additionalDelegates, "AdditionalDelegates must not be empty");
    List<Delegate> allDelegates = new ArrayList<>(this.delegates);
    allDelegates.addAll(additionalDelegates);
    return new ValueCodeGenerator(allDelegates, this.generatedMethods);
  }

  /**
   * Return a {@link ValueCodeGenerator} that is scoped for the specified
   * {@link GeneratedMethods}. This allows code generation to generate
   * additional methods if necessary, or perform some optimization in
   * case of visibility issues.
   *
   * @param generatedMethods the generated methods to use
   * @return an instance scoped to the specified generated methods
   */
  public ValueCodeGenerator scoped(GeneratedMethods generatedMethods) {
    return new ValueCodeGenerator(this.delegates, generatedMethods);
  }

  /**
   * Generate the code that represents the specified {@code value}.
   *
   * @param value the value to generate
   * @return the code that represents the specified value
   */
  public CodeBlock generateCode(@Nullable Object value) {
    if (value == null) {
      return NULL_VALUE_CODE_BLOCK;
    }
    try {
      for (Delegate delegate : this.delegates) {
        CodeBlock code = delegate.generateCode(this, value);
        if (code != null) {
          return code;
        }
      }
      throw new UnsupportedTypeValueCodeGenerationException(value);
    }
    catch (Exception ex) {
      throw new ValueCodeGenerationException(value, ex);
    }
  }

  /**
   * Return the {@link GeneratedMethods} that represents the scope
   * in which code generated by this instance will be added, or
   * {@code null} if no specific scope is set.
   *
   * @return the generated methods to use for code generation
   */
  @Nullable
  public GeneratedMethods getGeneratedMethods() {
    return this.generatedMethods;
  }

  /**
   * Strategy interface that can be used to implement code generation for a
   * particular value type.
   */
  public interface Delegate {

    /**
     * Generate the code for the specified non-null {@code value}. If this
     * instance does not support the value, it should return {@code null} to
     * indicate so.
     *
     * @param valueCodeGenerator the code generator to use for embedded values
     * @param value the value to generate
     * @return the code that represents the specified value or {@code null} if
     * the specified value is not supported.
     */
    @Nullable
    CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value);

  }

}
