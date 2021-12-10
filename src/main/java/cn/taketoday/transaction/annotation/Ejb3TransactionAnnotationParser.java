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

package cn.taketoday.transaction.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;

import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.interceptor.DefaultTransactionAttribute;
import cn.taketoday.transaction.interceptor.TransactionAttribute;
import jakarta.ejb.ApplicationException;
import jakarta.ejb.TransactionAttributeType;

/**
 * Strategy implementation for parsing EJB3's {@link jakarta.ejb.TransactionAttribute}
 * annotation.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class Ejb3TransactionAnnotationParser implements TransactionAnnotationParser, Serializable {

  @Override
  public boolean isCandidateClass(Class<?> targetClass) {
    return AnnotationUtils.isCandidateClass(targetClass, jakarta.ejb.TransactionAttribute.class);
  }

  @Override
  @Nullable
  public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
    jakarta.ejb.TransactionAttribute ann = element.getAnnotation(jakarta.ejb.TransactionAttribute.class);
    if (ann != null) {
      return parseTransactionAnnotation(ann);
    }
    else {
      return null;
    }
  }

  public TransactionAttribute parseTransactionAnnotation(jakarta.ejb.TransactionAttribute ann) {
    return new Ejb3TransactionAttribute(ann.value());
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof Ejb3TransactionAnnotationParser);
  }

  @Override
  public int hashCode() {
    return Ejb3TransactionAnnotationParser.class.hashCode();
  }

  /**
   * EJB3-specific TransactionAttribute, implementing EJB3's rollback rules
   * which are based on annotated exceptions.
   */
  private static class Ejb3TransactionAttribute extends DefaultTransactionAttribute {

    public Ejb3TransactionAttribute(TransactionAttributeType type) {
      setPropagationBehaviorName(PREFIX_PROPAGATION + type.name());
    }

    @Override
    public boolean rollbackOn(Throwable ex) {
      ApplicationException ann = ex.getClass().getAnnotation(ApplicationException.class);
      return (ann != null ? ann.rollback() : super.rollbackOn(ex));
    }
  }

}
