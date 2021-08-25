/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.beans.support.BeanConstructor;
import cn.taketoday.core.Assert;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.util.AnnotationUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.OrderUtils;

/**
 * Standard implementation of {@link BeanDefinition}
 *
 * @author TODAY <br>
 * 2019-02-01 12:29
 */
public class StandardBeanDefinition extends DefaultBeanDefinition implements BeanDefinition {

  /** Declaring name @since 2.1.2 */
  private String declaringName;

  private Method factoryMethod;

  public StandardBeanDefinition(String beanName, Class<?> beanClass) {
    super(beanName, beanClass);
  }

  public StandardBeanDefinition(String beanName, BeanDefinition childDef) {
    super(beanName, childDef);
  }

  public String getDeclaringName() {
    return declaringName;
  }

  public StandardBeanDefinition setDeclaringName(String declaringName) {
    this.declaringName = declaringName;
    return this;
  }

  /**
   * {@link BeanDefinition}'s Order
   */
  @Override
  public int getOrder() {
    final int order = super.getOrder();
    if (LOWEST_PRECEDENCE == order) {
      return OrderUtils.getOrder(getFactoryMethod());
    }
    return order + OrderUtils.getOrder(getFactoryMethod());
  }

  public Method getFactoryMethod() {
    return factoryMethod;
  }

  public StandardBeanDefinition setFactoryMethod(Method factoryMethod) {
    this.factoryMethod = factoryMethod;
    return this;
  }

  @Override
  public Executable getExecutable() {
    return obtainFactoryMethod();
  }

  @Override
  protected BeanConstructor createConstructor(BeanFactory factory) {
    final Method factoryMethod = obtainFactoryMethod();

    final MethodInvoker methodInvoker = MethodInvoker.fromMethod(factoryMethod);
    if (Modifier.isStatic(factoryMethod.getModifiers())) {
      return BeanConstructor.fromStaticMethod(methodInvoker);
    }
    final Object bean = factory.getBean(getDeclaringName());
    return BeanConstructor.fromMethod(methodInvoker, bean);
  }

  private Method obtainFactoryMethod() {
    final Method factoryMethod = getFactoryMethod();
    Assert.notNull(factoryMethod, "StandardBeanDefinition is not ready");
    return factoryMethod;
  }

  // Object

  @Override
  public String toString() {
    return new StringBuilder()//
            .append("{\n\t\"name\":\"").append(getName())//
            .append("\",\n\t\"declaringName\":\"").append(getDeclaringName())//
            .append("\",\n\t\"beanClass\":\"").append(getBeanClass())//
            .append("\",\n\t\"scope\":\"").append(getScope())//
            .append("\",\n\t\"factoryMethod\":\"").append(getFactoryMethod())//
            .append("\",\n\t\"initMethods\":\"").append(Arrays.toString(getInitMethods()))//
            .append("\",\n\t\"destroyMethods\":\"").append(Arrays.toString(getDestroyMethods()))//
            .append("\",\n\t\"propertyValues\":\"").append(Arrays.toString(getPropertySetters()))//
            .append("\",\n\t\"initialized\":\"").append(isInitialized())//
            .append("\",\n\t\"factoryBean\":\"").append(isFactoryBean())//
            .append("\",\n\t\"abstract\":\"").append(isAbstract())//
            .append("\"\n}")//
            .toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj instanceof StandardBeanDefinition) {
      final boolean equals = super.equals(obj);
      if (!equals) {
        return false;
      }
      final StandardBeanDefinition other = (StandardBeanDefinition) obj;
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
    final T ret = AnnotationUtils.getAnnotation(annotationClass, getFactoryMethod());
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

  protected Annotation[] mergeAnnotations(final Annotation[] methodAnns, final Annotation[] classAnns) {

    if (ObjectUtils.isEmpty(methodAnns)) {
      return classAnns;
    }

    if (ObjectUtils.isNotEmpty(classAnns)) {
      final Set<Annotation> rets = new HashSet<>();
      final Set<Class<?>> clazz = Stream.of(methodAnns)
              .map(Annotation::annotationType)
              .collect(Collectors.toSet());

      Collections.addAll(rets, methodAnns);

      for (final Annotation annotation : classAnns) {
        if (!clazz.contains(annotation.annotationType())) {
          rets.add(annotation);
        }
      }
      return rets.toArray(new Annotation[rets.size()]);
    }
    return methodAnns;
  }

}
