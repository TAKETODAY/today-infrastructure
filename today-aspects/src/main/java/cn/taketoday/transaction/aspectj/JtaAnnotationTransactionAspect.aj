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

package cn.taketoday.transaction.aspectj;

import org.aspectj.lang.annotation.RequiredTypes;

import cn.taketoday.transaction.annotation.AnnotationTransactionAttributeSource;
import jakarta.transaction.Transactional;

/**
 * Concrete AspectJ transaction aspect using the JTA 1.2
 * {@link Transactional} annotation.
 *
 * <p>When using this aspect, you <i>must</i> annotate the implementation class
 * (and/or methods within that class), <i>not</i> the interface (if any) that
 * the class implements. AspectJ follows Java's rule that annotations on
 * interfaces are <i>not</i> inherited.
 *
 * <p>An @Transactional annotation on a class specifies the default transaction
 * semantics for the execution of any <b>public</b> operation in the class.
 *
 * <p>An @Transactional annotation on a method within the class overrides the
 * default transaction semantics given by the class annotation (if present).
 * Any method may be annotated (regardless of visibility). Annotating
 * non-public methods directly is the only way to get transaction demarcation
 * for the execution of such operations.
 *
 * @author Stephane Nicoll
 * @since 4.2
 * @see Transactional
 * @see AnnotationTransactionAspect
 */
@RequiredTypes("jakarta.transaction.Transactional")
public aspect JtaAnnotationTransactionAspect extends AbstractTransactionAspect {

  public JtaAnnotationTransactionAspect() {
    super(new AnnotationTransactionAttributeSource(false));
  }

  /**
   * Matches the execution of any public method in a type with the Transactional
   * annotation, or any subtype of a type with the Transactional annotation.
   */
  private pointcut executionOfAnyPublicMethodInAtTransactionalType():
          execution(public * ((@Transactional *)+).*(..)) && within(@Transactional *);

  /**
   * Matches the execution of any method with the Transactional annotation.
   */
  private pointcut executionOfTransactionalMethod():
          execution(@Transactional * *(..));

  /**
   * Definition of pointcut from super aspect - matched join points
   * will have Spring transaction management applied.
   */
  protected pointcut transactionalMethodExecution(Object txObject):
          (executionOfAnyPublicMethodInAtTransactionalType() || executionOfTransactionalMethod() ) && this(txObject);

}
