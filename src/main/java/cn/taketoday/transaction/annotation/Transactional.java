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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;

/**
 * Describes a transaction attribute on an individual method or on a class.
 *
 * <p>When this annotation is declared at the class level, it applies as a default
 * to all methods of the declaring class and its subclasses. Note that it does not
 * apply to ancestor classes up the class hierarchy; inherited methods need to be
 * locally redeclared in order to participate in a subclass-level annotation. For
 * details on method visibility constraints, consult the
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction">Transaction Management</a>
 * section of the reference manual.
 *
 * <p>This annotation type is generally directly comparable to Framework's
 * {@link cn.taketoday.transaction.interceptor.RuleBasedTransactionAttribute}
 * class, and in fact {@link AnnotationTransactionAttributeSource} will directly
 * convert the data to the latter class, so that Framework's transaction support code
 * does not have to know about annotations. If no custom rollback rules apply,
 * the transaction will roll back on {@link RuntimeException} and {@link Error}
 * but not on checked exceptions.
 *
 * <p>For specific information about the semantics of this annotation's attributes,
 * consult the {@link cn.taketoday.transaction.TransactionDefinition} and
 * {@link cn.taketoday.transaction.interceptor.TransactionAttribute} javadocs.
 *
 * <p>This annotation commonly works with thread-bound transactions managed by a
 * {@link PlatformTransactionManager}, exposing a
 * transaction to all data access operations within the current execution thread.
 * <b>Note: This does NOT propagate to newly started threads within the method.</b>
 *
 * <p>Alternatively, this annotation may demarcate a reactive transaction managed
 * by a {@link cn.taketoday.transaction.ReactiveTransactionManager} which
 * uses the Reactor context instead of thread-local variables. As a consequence,
 * all participating data access operations need to execute within the same
 * Reactor context in the same reactive pipeline.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Paluch
 * @see cn.taketoday.transaction.interceptor.TransactionAttribute
 * @see cn.taketoday.transaction.interceptor.DefaultTransactionAttribute
 * @see cn.taketoday.transaction.interceptor.RuleBasedTransactionAttribute
 * @since 4.0
 */
@Inherited
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {

  /**
   * Alias for {@link #transactionManager}.
   *
   * @see #transactionManager
   */
  @AliasFor("transactionManager")
  String value() default "";

  /**
   * A <em>qualifier</em> value for the specified transaction.
   * <p>May be used to determine the target transaction manager, matching the
   * qualifier value (or the bean name) of a specific
   * {@link PlatformTransactionManager TransactionManager}
   * bean definition.
   *
   * @see #value
   * @see PlatformTransactionManager
   * @see cn.taketoday.transaction.ReactiveTransactionManager
   */
  @AliasFor("value")
  String transactionManager() default "";

  /**
   * Defines zero (0) or more transaction labels.
   * <p>Labels may be used to describe a transaction, and they can be evaluated
   * by individual transaction managers. Labels may serve a solely descriptive
   * purpose or map to pre-defined transaction manager-specific options.
   * <p>See the documentation of the actual transaction manager implementation
   * for details on how it evaluates transaction labels.
   *
   * @see cn.taketoday.transaction.interceptor.DefaultTransactionAttribute#getLabels()
   */
  String[] label() default {};

  /**
   * The transaction propagation type.
   * <p>Defaults to {@link Propagation#REQUIRED}.
   *
   * @see cn.taketoday.transaction.interceptor.TransactionAttribute#getPropagationBehavior()
   */
  Propagation propagation() default Propagation.REQUIRED;

  /**
   * The transaction isolation level.
   * <p>Defaults to {@link Isolation#DEFAULT}.
   * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
   * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
   * transactions. Consider switching the "validateExistingTransactions" flag to
   * "true" on your transaction manager if you'd like isolation level declarations
   * to get rejected when participating in an existing transaction with a different
   * isolation level.
   *
   * @see cn.taketoday.transaction.interceptor.TransactionAttribute#getIsolationLevel()
   * @see cn.taketoday.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
   */
  Isolation isolation() default Isolation.DEFAULT;

  /**
   * The timeout for this transaction (in seconds).
   * <p>Defaults to the default timeout of the underlying transaction system.
   * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
   * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
   * transactions.
   *
   * @return the timeout in seconds
   * @see cn.taketoday.transaction.interceptor.TransactionAttribute#getTimeout()
   */
  int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;

  /**
   * The timeout for this transaction (in seconds).
   * <p>Defaults to the default timeout of the underlying transaction system.
   * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
   * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
   * transactions.
   *
   * @return the timeout in seconds as a String value, e.g. a placeholder
   * @see cn.taketoday.transaction.interceptor.TransactionAttribute#getTimeout()
   */
  String timeoutString() default "";

  /**
   * A boolean flag that can be set to {@code true} if the transaction is
   * effectively read-only, allowing for corresponding optimizations at runtime.
   * <p>Defaults to {@code false}.
   * <p>This just serves as a hint for the actual transaction subsystem;
   * it will <i>not necessarily</i> cause failure of write access attempts.
   * A transaction manager which cannot interpret the read-only hint will
   * <i>not</i> throw an exception when asked for a read-only transaction
   * but rather silently ignore the hint.
   *
   * @see cn.taketoday.transaction.interceptor.TransactionAttribute#isReadOnly()
   * @see cn.taketoday.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
   */
  boolean readOnly() default false;

  /**
   * Defines zero (0) or more exception {@link Class classes}, which must be
   * subclasses of {@link Throwable}, indicating which exception types must cause
   * a transaction rollback.
   * <p>By default, a transaction will be rolling back on {@link RuntimeException}
   * and {@link Error} but not on checked exceptions (business exceptions). See
   * {@link cn.taketoday.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)}
   * for a detailed explanation.
   * <p>This is the preferred way to construct a rollback rule (in contrast to
   * {@link #rollbackForClassName}), matching the exception class and its subclasses.
   * <p>Similar to {@link cn.taketoday.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(Class clazz)}.
   *
   * @see #rollbackForClassName
   * @see cn.taketoday.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
   */
  Class<? extends Throwable>[] rollbackFor() default {};

  /**
   * Defines zero (0) or more exception names (for exceptions which must be a
   * subclass of {@link Throwable}), indicating which exception types must cause
   * a transaction rollback.
   * <p>This can be a substring of a fully qualified class name, with no wildcard
   * support at present. For example, a value of {@code "ServletException"} would
   * match {@code jakarta.servlet.ServletException} and its subclasses.
   * <p><b>NB:</b> Consider carefully how specific the pattern is and whether
   * to include package information (which isn't mandatory). For example,
   * {@code "Exception"} will match nearly anything and will probably hide other
   * rules. {@code "java.lang.Exception"} would be correct if {@code "Exception"}
   * were meant to define a rule for all checked exceptions. With more unusual
   * {@link Exception} names such as {@code "BaseBusinessException"} there is no
   * need to use a FQN.
   * <p>Similar to {@link cn.taketoday.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(String exceptionName)}.
   *
   * @see #rollbackFor
   * @see cn.taketoday.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
   */
  String[] rollbackForClassName() default {};

  /**
   * Defines zero (0) or more exception {@link Class Classes}, which must be
   * subclasses of {@link Throwable}, indicating which exception types must
   * <b>not</b> cause a transaction rollback.
   * <p>This is the preferred way to construct a rollback rule (in contrast
   * to {@link #noRollbackForClassName}), matching the exception class and
   * its subclasses.
   * <p>Similar to {@link cn.taketoday.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(Class clazz)}.
   *
   * @see #noRollbackForClassName
   * @see cn.taketoday.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
   */
  Class<? extends Throwable>[] noRollbackFor() default {};

  /**
   * Defines zero (0) or more exception names (for exceptions which must be a
   * subclass of {@link Throwable}) indicating which exception types must <b>not</b>
   * cause a transaction rollback.
   * <p>See the description of {@link #rollbackForClassName} for further
   * information on how the specified names are treated.
   * <p>Similar to {@link cn.taketoday.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(String exceptionName)}.
   *
   * @see #noRollbackFor
   * @see cn.taketoday.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
   */
  String[] noRollbackForClassName() default {};

}
