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

package infra.expression.spel.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.core.MethodParameter;
import infra.core.TypeDescriptor;
import infra.expression.MethodExecutor;

class Spr7538Tests {

  @Test
  void repro() throws Exception {
    AlwaysTrueReleaseStrategy target = new AlwaysTrueReleaseStrategy();
    BeanFactoryTypeConverter converter = new BeanFactoryTypeConverter();

    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setTypeConverter(converter);

    List<Foo> arguments = Collections.emptyList();

    List<TypeDescriptor> paramDescriptors = new ArrayList<>();
    Method method = AlwaysTrueReleaseStrategy.class.getMethod("checkCompleteness", List.class);
    paramDescriptors.add(new TypeDescriptor(new MethodParameter(method, 0)));

    List<TypeDescriptor> argumentTypes = new ArrayList<>();
    argumentTypes.add(TypeDescriptor.forObject(arguments));
    ReflectiveMethodResolver resolver = new ReflectiveMethodResolver();
    MethodExecutor executor = resolver.resolve(context, target, "checkCompleteness", argumentTypes);

    Object result = executor.execute(context, target, arguments);
    System.out.println("Result: " + result);
  }

  static class AlwaysTrueReleaseStrategy {
    public boolean checkCompleteness(List<Foo> messages) {
      return true;
    }
  }

  static class Foo { }
}
