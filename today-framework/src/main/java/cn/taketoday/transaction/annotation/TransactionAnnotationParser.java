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

import java.lang.reflect.AnnotatedElement;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.interceptor.TransactionAttribute;

/**
 * Strategy interface for parsing known transaction annotation types.
 * {@link AnnotationTransactionAttributeSource} delegates to such
 * parsers for supporting specific annotation types such as Framework's own
 * {@link Transactional}, JTA 1.2's {@link jakarta.transaction.Transactional}
 * or EJB3's {@link jakarta.ejb.TransactionAttribute}.
 *
 * @author Juergen Hoeller
 * @see AnnotationTransactionAttributeSource
 * @see TransactionalAnnotationParser
 * @see Ejb3TransactionAnnotationParser
 * @see JtaTransactionAnnotationParser
 * @since 4.0
 */
public interface TransactionAnnotationParser {

  /**
   * Determine whether the given class is a candidate for transaction attributes
   * in the annotation format of this {@code TransactionAnnotationParser}.
   * <p>If this method returns {@code false}, the methods on the given class
   * will not get traversed for {@code #parseTransactionAnnotation} introspection.
   * Returning {@code false} is therefore an optimization for non-affected
   * classes, whereas {@code true} simply means that the class needs to get
   * fully introspected for each method on the given class individually.
   *
   * @param targetClass the class to introspect
   * @return {@code false} if the class is known to have no transaction
   * annotations at class or method level; {@code true} otherwise. The default
   * implementation returns {@code true}, leading to regular introspection.
   */
  default boolean isCandidateClass(Class<?> targetClass) {
    return true;
  }

  /**
   * Parse the transaction attribute for the given method or class,
   * based on an annotation type understood by this parser.
   * <p>This essentially parses a known transaction annotation into Framework's metadata
   * attribute class. Returns {@code null} if the method/class is not transactional.
   *
   * @param element the annotated method or class
   * @return the configured transaction attribute, or {@code null} if none found
   * @see AnnotationTransactionAttributeSource#determineTransactionAttribute
   */
  @Nullable
  TransactionAttribute parseTransactionAnnotation(AnnotatedElement element);

}
