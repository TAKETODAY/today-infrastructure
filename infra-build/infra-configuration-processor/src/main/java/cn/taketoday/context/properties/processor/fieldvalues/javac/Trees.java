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

package cn.taketoday.context.properties.processor.fieldvalues.javac;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * Reflection based access to {@code com.sun.source.util.Trees}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class Trees extends ReflectionWrapper {

  private Trees(Object instance) {
    super("com.sun.source.util.Trees", instance);
  }

  Tree getTree(Element element) throws Exception {
    Object tree = findMethod("getTree", Element.class).invoke(getInstance(), element);
    return (tree != null) ? new Tree(tree) : null;
  }

  static Trees instance(ProcessingEnvironment env) throws Exception {
    try {
      ClassLoader classLoader = env.getClass().getClassLoader();
      Class<?> type = findClass(classLoader, "com.sun.source.util.Trees");
      Method method = findMethod(type, "instance", ProcessingEnvironment.class);
      return new Trees(method.invoke(null, env));
    }
    catch (Exception ex) {
      return instance(unwrap(env));
    }
  }

  private static ProcessingEnvironment unwrap(ProcessingEnvironment wrapper) throws Exception {
    Field delegateField = wrapper.getClass().getDeclaredField("delegate");
    delegateField.setAccessible(true);
    return (ProcessingEnvironment) delegateField.get(wrapper);
  }

}
