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

import java.util.function.Consumer;

import infra.javapoet.ClassName;
import infra.javapoet.MethodSpec;
import infra.lang.Assert;

/**
 * A generated method.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see GeneratedMethods
 * @since 4.0
 */
public final class GeneratedMethod {

  private final ClassName className;

  private final String name;

  private final MethodSpec methodSpec;

  /**
   * Create a new {@link GeneratedMethod} instance with the given name. This
   * constructor is package-private since names should only be generated via
   * {@link GeneratedMethods}.
   *
   * @param className the declaring class of the method
   * @param name the generated method name
   * @param method consumer to generate the method
   */
  GeneratedMethod(ClassName className, String name, Consumer<MethodSpec.Builder> method) {
    this.className = className;
    this.name = name;
    MethodSpec.Builder builder = MethodSpec.methodBuilder(this.name);
    method.accept(builder);
    this.methodSpec = builder.build();
    Assert.state(this.name.equals(this.methodSpec.name),
            "'method' consumer must not change the generated method name");
  }

  /**
   * Return the generated name of the method.
   *
   * @return the name of the generated method
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return a {@link MethodReference} to this generated method.
   *
   * @return a method reference
   */
  public MethodReference toMethodReference() {
    return new DefaultMethodReference(this.methodSpec, this.className);
  }

  /**
   * Return the {@link MethodSpec} for this generated method.
   *
   * @return the method spec
   * @throws IllegalStateException if one of the {@code generateBy(...)}
   * methods has not been called
   */
  MethodSpec getMethodSpec() {
    return this.methodSpec;
  }

  @Override
  public String toString() {
    return this.name;
  }

}
