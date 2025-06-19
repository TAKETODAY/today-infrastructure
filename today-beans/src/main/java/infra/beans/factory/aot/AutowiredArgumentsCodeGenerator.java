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

package infra.beans.factory.aot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;

import infra.javapoet.CodeBlock;
import infra.lang.Assert;
import infra.util.ReflectionUtils;

/**
 * Code generator to apply {@link AutowiredArguments}.
 *
 * <p>Generates code in the form: {@code args.get(0), args.get(1)} or
 * {@code args.get(0, String.class), args.get(1, Integer.class)}
 *
 * <p>The simpler form is only used if the target method or constructor is
 * unambiguous.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class AutowiredArgumentsCodeGenerator {

  private final Class<?> target;

  private final Executable executable;

  public AutowiredArgumentsCodeGenerator(Class<?> target, Executable executable) {
    this.target = target;
    this.executable = executable;
  }

  public CodeBlock generateCode(Class<?>[] parameterTypes) {
    return generateCode(parameterTypes, 0, "args");
  }

  public CodeBlock generateCode(Class<?>[] parameterTypes, int startIndex) {
    return generateCode(parameterTypes, startIndex, "args");
  }

  public CodeBlock generateCode(Class<?>[] parameterTypes, int startIndex, String variableName) {
    Assert.notNull(parameterTypes, "'parameterTypes' is required");
    Assert.notNull(variableName, "'variableName' is required");
    boolean ambiguous = isAmbiguous();
    CodeBlock.Builder code = CodeBlock.builder();
    for (int i = startIndex; i < parameterTypes.length; i++) {
      code.add(i > startIndex ? ", " : "");
      if (!ambiguous) {
        code.add("$L.get($L)", variableName, i);
      }
      else {
        code.add("$L.get($L, $T.class)", variableName, i, parameterTypes[i]);
      }
    }
    return code.build();
  }

  private boolean isAmbiguous() {
    if (this.executable instanceof Constructor<?> constructor) {
      return Arrays.stream(this.target.getDeclaredConstructors())
              .filter(Predicate.not(constructor::equals))
              .anyMatch(this::hasSameParameterCount);
    }
    if (this.executable instanceof Method method) {
      return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(this.target))
              .filter(Predicate.not(method::equals))
              .filter(candidate -> candidate.getName().equals(method.getName()))
              .anyMatch(this::hasSameParameterCount);
    }
    return true;
  }

  private boolean hasSameParameterCount(Executable executable) {
    return this.executable.getParameterCount() == executable.getParameterCount();
  }

}
