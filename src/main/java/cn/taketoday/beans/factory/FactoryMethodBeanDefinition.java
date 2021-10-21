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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.OrderUtils;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Standard implementation of {@link BeanDefinition}
 *
 * @author TODAY <br>
 * 2019-02-01 12:29
 */
public class FactoryMethodBeanDefinition extends DefaultBeanDefinition implements BeanDefinition {

  /** Declaring name @since 2.1.2 */
  private String declaringName;

  private final Method factoryMethod;

  public FactoryMethodBeanDefinition(Method factoryMethod) {
    super(factoryMethod.getReturnType());
    this.factoryMethod = factoryMethod;
  }

  public String getDeclaringName() {
    return declaringName;
  }

  public FactoryMethodBeanDefinition setDeclaringName(String declaringName) {
    this.declaringName = declaringName;
    return this;
  }

  /**
   * {@link BeanDefinition}'s Order
   */
  @Override
  public int getOrder() {
    int order = super.getOrder();
    if (LOWEST_PRECEDENCE == order) {
      return OrderUtils.getOrderOrLowest(getFactoryMethod());
    }
    return order - OrderUtils.getOrderOrLowest(getFactoryMethod());
  }

  public Method getFactoryMethod() {
    return factoryMethod;
  }

  @Override
  public Executable getExecutable() {
    return obtainFactoryMethod();
  }

  @Override
  protected BeanInstantiator createConstructor(BeanFactory factory) {
    Method factoryMethod = obtainFactoryMethod();

    MethodInvoker methodInvoker = MethodInvoker.fromMethod(factoryMethod);
    if (Modifier.isStatic(factoryMethod.getModifiers())) {
      return BeanInstantiator.fromStaticMethod(methodInvoker);
    }
    Object bean = factory.getBean(getDeclaringName());
    return BeanInstantiator.fromMethod(methodInvoker, bean);
  }

  private Method obtainFactoryMethod() {
    Method factoryMethod = getFactoryMethod();
    Assert.state(factoryMethod != null, "StandardBeanDefinition is not ready");
    return factoryMethod;
  }

  @Override
  public boolean isAssignableTo(ResolvableType typeToMatch) {
    BeanDefinition child = getChild();
    if (child != null) {
      Class<?> implementationClass = child.getBeanClass();
      return ResolvableType.forReturnType(factoryMethod, implementationClass)
              .isAssignableFrom(typeToMatch);
    }
    return ResolvableType.forReturnType(factoryMethod)
            .isAssignableFrom(typeToMatch);
  }

  @Override
  public void validate() throws BeanDefinitionValidationException {
    if (StringUtils.isEmpty(getDeclaringName())) {
      throw new BeanDefinitionValidationException("Declaring name can't be null in: " + this);
    }
    if (getFactoryMethod() == null) {
      throw new BeanDefinitionValidationException("Factory Method can't be null " + this);
    }
    super.validate();
  }

  // Object

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("class [");
    sb.append(getBeanClass().getName()).append(']');
    sb.append("; scope=").append(getScope());
    sb.append("; abstract=").append(isAbstract());
    sb.append("; lazyInit=").append(isLazyInit());
    sb.append("; primary=").append(isPrimary());
    sb.append("; initialized=").append(isInitialized());
    sb.append("; factoryBean=").append(isFactoryBean());
    sb.append("; factoryMethod=").append(this.factoryMethod);
    sb.append("; declaringName=").append(this.declaringName);
    sb.append("; initMethods=").append(Arrays.toString(getInitMethods()));
    sb.append("; destroyMethods=").append(Arrays.toString(getDestroyMethods()));
    sb.append("; child=").append(getChild());
    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj instanceof FactoryMethodBeanDefinition) {
      boolean equals = super.equals(obj);
      if (!equals) {
        return false;
      }
      FactoryMethodBeanDefinition other = (FactoryMethodBeanDefinition) obj;
      return Objects.equals(declaringName, other.declaringName)
              && Objects.equals(factoryMethod, other.factoryMethod);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), declaringName, factoryMethod);
  }

  // AnnotatedElement
  // -----------------------------

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
    return AnnotationUtils.isPresent(getFactoryMethod(), annotation) || super.isAnnotationPresent(annotation);
  }

  @Override
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    T ret = AnnotationUtils.getAnnotation(getFactoryMethod(), annotationClass);
    if (ret == null) {
      return super.getAnnotation(annotationClass);
    }
    return ret;
  }

  @Override
  public Annotation[] getAnnotations() {
    return mergeAnnotations(getFactoryMethod().getAnnotations(), super.getAnnotations());
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return mergeAnnotations(getFactoryMethod().getDeclaredAnnotations(), super.getDeclaredAnnotations());
  }

  protected Annotation[] mergeAnnotations(Annotation[] methodAnns, Annotation[] classAnns) {

    if (ObjectUtils.isEmpty(methodAnns)) {
      return classAnns;
    }

    if (ObjectUtils.isNotEmpty(classAnns)) {
      Set<Annotation> rets = new HashSet<>();
      Set<Class<?>> clazz = Stream.of(methodAnns)
              .map(Annotation::annotationType)
              .collect(Collectors.toSet());

      Collections.addAll(rets, methodAnns);

      for (Annotation annotation : classAnns) {
        if (!clazz.contains(annotation.annotationType())) {
          rets.add(annotation);
        }
      }
      return rets.toArray(new Annotation[rets.size()]);
    }
    return methodAnns;
  }

}
