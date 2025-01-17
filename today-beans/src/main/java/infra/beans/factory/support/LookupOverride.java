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

package infra.beans.factory.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import infra.beans.factory.BeanFactory;
import infra.core.ResolvableType;
import infra.lang.Nullable;

/**
 * Represents an override of a method that looks up an object in the same IoC context,
 * either by bean name or by bean type (based on the declared method return type).
 *
 * <p>Methods eligible for lookup override may declare arguments in which case the
 * given arguments are passed to the bean retrieval operation.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanFactory#getBean(String)
 * @see BeanFactory#getBean(Class)
 * @see BeanFactory#getBean(String, Object...)
 * @see BeanFactory#getBean(Class, Object...)
 * @see BeanFactory#getBeanProvider(ResolvableType)
 * @since 4.0 2022/3/7 12:44
 */
public class LookupOverride extends MethodOverride {

  @Nullable
  private final String beanName;

  @Nullable
  private Method method;

  /**
   * Construct a new LookupOverride.
   *
   * @param methodName the name of the method to override
   * @param beanName the name of the bean in the current {@code BeanFactory} that the
   * overridden method should return (may be {@code null} for type-based bean retrieval)
   */
  public LookupOverride(String methodName, @Nullable String beanName) {
    super(methodName);
    this.beanName = beanName;
  }

  /**
   * Construct a new LookupOverride.
   *
   * @param method the method declaration to override
   * @param beanName the name of the bean in the current {@code BeanFactory} that the
   * overridden method should return (may be {@code null} for type-based bean retrieval)
   */
  public LookupOverride(Method method, @Nullable String beanName) {
    super(method.getName());
    this.method = method;
    this.beanName = beanName;
  }

  /**
   * Return the name of the bean that should be returned by this method.
   */
  @Nullable
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Match the specified method by {@link Method} reference or method name.
   * <p>For backwards compatibility reasons, in a scenario with overloaded
   * non-abstract methods of the given name, only the no-arg variant of a
   * method will be turned into a container-driven lookup method.
   * <p>In case of a provided {@link Method}, only straight matches will
   * be considered, usually demarcated by the {@code @Lookup} annotation.
   */
  @Override
  public boolean matches(Method method) {
    if (this.method != null) {
      return method.equals(this.method);
    }
    else {
      return method.getName().equals(getMethodName())
              && (
              !isOverloaded()
                      || Modifier.isAbstract(method.getModifiers())
                      || method.getParameterCount() == 0
      );
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof LookupOverride that) || !super.equals(other)) {
      return false;
    }
    return Objects.equals(method, that.method)
            && Objects.equals(beanName, that.beanName);
  }

  @Override
  public int hashCode() {
    return (29 * super.hashCode() + Objects.hashCode(beanName));
  }

  @Override
  public String toString() {
    return "LookupOverride for method '" + getMethodName() + "'";
  }

}
