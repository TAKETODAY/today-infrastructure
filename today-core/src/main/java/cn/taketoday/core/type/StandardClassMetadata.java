/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.core.type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link ClassMetadata} implementation that uses standard reflection
 * to introspect a given {@code Class}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public class StandardClassMetadata implements ClassMetadata {

  private final Class<?> introspectedClass;

  /**
   * Create a new StandardClassMetadata wrapper for the given Class.
   *
   * @param introspectedClass the Class to introspect
   */
  public StandardClassMetadata(Class<?> introspectedClass) {
    Assert.notNull(introspectedClass, "Class is required");
    this.introspectedClass = introspectedClass;
  }

  /**
   * Return the underlying Class.
   */
  public final Class<?> getIntrospectedClass() {
    return this.introspectedClass;
  }

  @Override
  public String getClassName() {
    return this.introspectedClass.getName();
  }

  @Override
  public boolean isInterface() {
    return this.introspectedClass.isInterface();
  }

  @Override
  public boolean isAnnotation() {
    return this.introspectedClass.isAnnotation();
  }

  @Override
  public boolean isAbstract() {
    return Modifier.isAbstract(this.introspectedClass.getModifiers());
  }

  @Override
  public boolean isFinal() {
    return Modifier.isFinal(this.introspectedClass.getModifiers());
  }

  @Override
  public boolean isIndependent() {
    return !hasEnclosingClass()
            || (this.introspectedClass.getDeclaringClass() != null
            && Modifier.isStatic(this.introspectedClass.getModifiers()));
  }

  @Override
  public int getModifiers() {
    return introspectedClass.getModifiers();
  }

  @Override
  @Nullable
  public String getEnclosingClassName() {
    Class<?> enclosingClass = this.introspectedClass.getEnclosingClass();
    return (enclosingClass != null ? enclosingClass.getName() : null);
  }

  @Override
  @Nullable
  public String getSuperClassName() {
    Class<?> superClass = this.introspectedClass.getSuperclass();
    return (superClass != null ? superClass.getName() : null);
  }

  @Override
  public String[] getInterfaceNames() {
    Class<?>[] ifcs = this.introspectedClass.getInterfaces();
    String[] ifcNames = new String[ifcs.length];
    for (int i = 0; i < ifcs.length; i++) {
      ifcNames[i] = ifcs[i].getName();
    }
    return ifcNames;
  }

  @Override
  public String[] getMemberClassNames() {
    LinkedHashSet<String> memberClassNames = new LinkedHashSet<>(4);
    for (Class<?> nestedClass : this.introspectedClass.getDeclaredClasses()) {
      memberClassNames.add(nestedClass.getName());
    }
    return StringUtils.toStringArray(memberClassNames);
  }

  @Override
  public Set<MethodMetadata> getDeclaredMethods() {
    Method[] methods = ReflectionUtils.getDeclaredMethods(getIntrospectedClass());
    return Stream.of(methods)
            .map(this::mapMethod)
            .collect(Collectors.toSet());
  }

  protected MethodMetadata mapMethod(Method method) {
    return new StandardMethodMetadata(method);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return ((this == obj) || ((obj instanceof StandardClassMetadata) &&
            getIntrospectedClass().equals(((StandardClassMetadata) obj).getIntrospectedClass())));
  }

  @Override
  public int hashCode() {
    return getIntrospectedClass().hashCode();
  }

  @Override
  public String toString() {
    return getClassName();
  }

}
