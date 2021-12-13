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

package cn.taketoday.transaction.interceptor;

import java.lang.reflect.Method;

import cn.taketoday.lang.Nullable;

/**
 * Strategy interface used by {@link TransactionInterceptor} for metadata retrieval.
 *
 * <p>Implementations know how to source transaction attributes, whether from configuration,
 * metadata attributes at source level (such as Java 5 annotations), or anywhere else.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see TransactionInterceptor#setTransactionAttributeSource
 * @see TransactionProxyFactoryBean#setTransactionAttributeSource
 * @see cn.taketoday.transaction.annotation.AnnotationTransactionAttributeSource
 * @since 4.0
 */
public interface TransactionAttributeSource {

  /**
   * Determine whether the given class is a candidate for transaction attributes
   * in the metadata format of this {@code TransactionAttributeSource}.
   * <p>If this method returns {@code false}, the methods on the given class
   * will not get traversed for {@link #getTransactionAttribute} introspection.
   * Returning {@code false} is therefore an optimization for non-affected
   * classes, whereas {@code true} simply means that the class needs to get
   * fully introspected for each method on the given class individually.
   *
   * @param targetClass the class to introspect
   * @return {@code false} if the class is known to have no transaction
   * attributes at class or method level; {@code true} otherwise. The default
   * implementation returns {@code true}, leading to regular introspection.
   */
  default boolean isCandidateClass(Class<?> targetClass) {
    return true;
  }

  /**
   * Return the transaction attribute for the given method,
   * or {@code null} if the method is non-transactional.
   *
   * @param method the method to introspect
   * @param targetClass the target class (may be {@code null},
   * in which case the declaring class of the method must be used)
   * @return the matching transaction attribute, or {@code null} if none found
   */
  @Nullable
  TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass);

}
