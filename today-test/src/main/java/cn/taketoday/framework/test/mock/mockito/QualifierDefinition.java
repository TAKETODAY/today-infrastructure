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

package cn.taketoday.framework.test.mock.mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.annotation.MergedAnnotations;

/**
 * Definition of a Spring {@link Qualifier @Qualifier}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see Definition
 */
class QualifierDefinition {

  private final Field field;

  private final DependencyDescriptor descriptor;

  private final Set<Annotation> annotations;

  QualifierDefinition(Field field, Set<Annotation> annotations) {
    // We can't use the field or descriptor as part of the context key
    // but we can assume that if two fields have the same qualifiers then
    // it's safe for Spring to use either for qualifier logic
    this.field = field;
    this.descriptor = new DependencyDescriptor(field, true);
    this.annotations = annotations;
  }

  boolean matches(ConfigurableBeanFactory beanFactory, String beanName) {
    return beanFactory.isAutowireCandidate(beanName, this.descriptor);
  }

  void applyTo(RootBeanDefinition definition) {
    definition.setQualifiedElement(this.field);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
      return false;
    }
    QualifierDefinition other = (QualifierDefinition) obj;
    return this.annotations.equals(other.annotations);
  }

  @Override
  public int hashCode() {
    return this.annotations.hashCode();
  }

  static QualifierDefinition forElement(AnnotatedElement element) {
    if (element != null && element instanceof Field) {
      Field field = (Field) element;
      Set<Annotation> annotations = getQualifierAnnotations(field);
      if (!annotations.isEmpty()) {
        return new QualifierDefinition(field, annotations);
      }
    }
    return null;
  }

  private static Set<Annotation> getQualifierAnnotations(Field field) {
    // Assume that any annotations other than @MockBean/@SpyBean are qualifiers
    Annotation[] candidates = field.getDeclaredAnnotations();
    Set<Annotation> annotations = new HashSet<>(candidates.length);
    for (Annotation candidate : candidates) {
      if (!isMockOrSpyAnnotation(candidate.annotationType())) {
        annotations.add(candidate);
      }
    }
    return annotations;
  }

  private static boolean isMockOrSpyAnnotation(Class<? extends Annotation> type) {
    if (type.equals(MockBean.class) || type.equals(SpyBean.class)) {
      return true;
    }
    MergedAnnotations metaAnnotations = MergedAnnotations.from(type);
    return metaAnnotations.isPresent(MockBean.class) || metaAnnotations.isPresent(SpyBean.class);
  }

}
