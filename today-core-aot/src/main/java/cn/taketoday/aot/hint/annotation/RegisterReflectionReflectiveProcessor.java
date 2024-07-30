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

package cn.taketoday.aot.hint.annotation;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

/**
 * A {@link ReflectiveProcessor} implementation that pairs with
 * {@link RegisterReflection @RegisterReflection}. Can be used as a base
 * implementation for composed annotations that are meta-annotated with
 * {@link RegisterReflection}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class RegisterReflectionReflectiveProcessor implements ReflectiveProcessor {

  private static final Logger logger = LoggerFactory.getLogger(RegisterReflectionReflectiveProcessor.class);

  @Override
  public final void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
    RegisterReflection annotation = AnnotatedElementUtils.getMergedAnnotation(element, RegisterReflection.class);
    Assert.notNull(annotation, "Element must be annotated with @" + RegisterReflection.class.getSimpleName() + ": " + element);
    ReflectionRegistration registration = parse(element, annotation);
    registerReflectionHints(hints, registration);
  }

  protected ReflectionRegistration parse(AnnotatedElement element, RegisterReflection annotation) {
    List<Class<?>> allClassNames = new ArrayList<>();
    allClassNames.addAll(Arrays.asList(annotation.classes()));
    allClassNames.addAll(Arrays.stream(annotation.classNames())
            .map(this::loadClass).filter(Objects::nonNull).toList());
    if (allClassNames.isEmpty()) {
      if (element instanceof Class<?> clazz) {
        allClassNames.add(clazz);
      }
      else {
        throw new IllegalStateException("At least one class must be specified, could not detect target from '%s'".formatted(element));
      }
    }
    return new ReflectionRegistration(allClassNames.toArray(new Class<?>[0]),
            annotation.memberCategories());
  }

  protected void registerReflectionHints(ReflectionHints hints, ReflectionRegistration registration) {
    for (Class<?> target : registration.classes) {
      registerReflectionHints(hints, target, registration.memberCategories);
    }
  }

  protected void registerReflectionHints(ReflectionHints hints, Class<?> target, MemberCategory[] memberCategories) {
    hints.registerType(target, type -> type.withMembers(memberCategories));
  }

  @Nullable
  private Class<?> loadClass(String className) {
    try {
      return ClassUtils.forName(className, getClass().getClassLoader());
    }
    catch (Exception ex) {
      logger.warn("Ignoring '" + className + "': " + ex.getMessage());
      return null;
    }
  }

  protected record ReflectionRegistration(Class<?>[] classes, MemberCategory[] memberCategories) { }

}
