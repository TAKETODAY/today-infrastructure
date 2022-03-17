/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Extension of the standard JavaBeans {@link PropertyDescriptor} class,
 * overriding {@code getPropertyType()} such that a generically declared
 * type variable will be resolved against the containing bean class.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/23 11:34
 */
final class GenericTypeAwarePropertyDescriptor extends PropertyDescriptor {

  private final Class<?> beanClass;

  @Nullable
  private final Method readMethod;

  @Nullable
  private final Method writeMethod;

  @Nullable
  private volatile Set<Method> ambiguousWriteMethods;

  @Nullable
  private MethodParameter writeMethodParameter;

  @Nullable
  private Class<?> propertyType;

  @Nullable
  private final Class<?> propertyEditorClass;

  public GenericTypeAwarePropertyDescriptor(
          Class<?> beanClass, String propertyName,
          @Nullable Method readMethod, @Nullable Method writeMethod,
          @Nullable Class<?> propertyEditorClass) throws IntrospectionException {

    super(propertyName, null, null);
    this.beanClass = beanClass;

    readMethod = readMethod != null ? BridgeMethodResolver.findBridgedMethod(readMethod) : null;
    writeMethod = writeMethod != null ? BridgeMethodResolver.findBridgedMethod(writeMethod) : null;
    if (writeMethod == null && readMethod != null) {
      // Fallback: Original JavaBeans introspection might not have found matching setter
      // method due to lack of bridge method resolution, in case of the getter using a
      // covariant return type whereas the setter is defined for the concrete property type.
      Method candidate = ReflectionUtils.getWriteMethod(beanClass, null, propertyName);
      if (candidate != null && candidate.getParameterCount() == 1) {
        writeMethod = candidate;
      }
    }
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;

    if (writeMethod != null) {
      if (readMethod == null) {
        // Write method not matched against read method: potentially ambiguous through
        // several overloaded variants, in which case an arbitrary winner has been chosen
        // by the JDK's JavaBeans Introspector...
        HashSet<Method> ambiguousCandidates = new HashSet<>();
        for (Method method : beanClass.getMethods()) {
          if (method.getName().equals(writeMethod.getName())
                  && !method.equals(writeMethod) && !method.isBridge()
                  && method.getParameterCount() == writeMethod.getParameterCount()) {
            ambiguousCandidates.add(method);
          }
        }
        if (!ambiguousCandidates.isEmpty()) {
          this.ambiguousWriteMethods = ambiguousCandidates;
        }
      }
      this.writeMethodParameter = new MethodParameter(this.writeMethod, 0).withContainingClass(this.beanClass);
    }

    if (readMethod != null) {
      this.propertyType = GenericTypeResolver.resolveReturnType(readMethod, this.beanClass);
    }
    else if (writeMethodParameter != null) {
      this.propertyType = this.writeMethodParameter.getParameterType();
    }

    this.propertyEditorClass = propertyEditorClass;
  }

  public Class<?> getBeanClass() {
    return this.beanClass;
  }

  @Override
  @Nullable
  public Method getReadMethod() {
    return this.readMethod;
  }

  @Override
  @Nullable
  public Method getWriteMethod() {
    return this.writeMethod;
  }

  public Method getWriteMethodForActualAccess() {
    Assert.state(this.writeMethod != null, "No write method available");
    Set<Method> ambiguousCandidates = this.ambiguousWriteMethods;
    if (ambiguousCandidates != null) {
      this.ambiguousWriteMethods = null;
      LoggerFactory.getLogger(GenericTypeAwarePropertyDescriptor.class)
              .debug("Non-unique JavaBean property '{}' being accessed! Ambiguous write methods found next to actually used [{}]: {}",
                      getName(), writeMethod, ambiguousCandidates);
    }
    return this.writeMethod;
  }

  @Nullable
  public MethodParameter getWriteMethodParameter() {
    return this.writeMethodParameter;
  }

  @Override
  @Nullable
  public Class<?> getPropertyType() {
    return this.propertyType;
  }

  @Override
  @Nullable
  public Class<?> getPropertyEditorClass() {
    return this.propertyEditorClass;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof GenericTypeAwarePropertyDescriptor otherPd)) {
      return false;
    }
    return (getBeanClass().equals(otherPd.getBeanClass()) && PropertyDescriptorUtils.equals(this, otherPd));
  }

  @Override
  public int hashCode() {
    int hashCode = getBeanClass().hashCode();
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getReadMethod());
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getWriteMethod());
    return hashCode;
  }

}
