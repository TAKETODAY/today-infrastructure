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

package infra.test.junit;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.util.Preconditions;

import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * An {@link ArgumentsProvider} that provides {@code true} and {@code false} values.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class BooleanArgumentsProvider implements ArgumentsProvider {

  @Override
  public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations, ExtensionContext context) {
    Method testMethod = context.getRequiredTestMethod();
    Preconditions.condition(testMethod.getParameterCount() > 0, () -> String.format(
            "@BooleanValueSource cannot provide arguments to method [%s]: the method does not declare any formal parameters.",
            testMethod.toGenericString()));

    return Stream.of(Arguments.arguments(false), Arguments.arguments(true));
  }

}
