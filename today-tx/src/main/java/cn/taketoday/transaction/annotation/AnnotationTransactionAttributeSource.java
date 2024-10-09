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

package cn.taketoday.transaction.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.interceptor.AbstractFallbackTransactionAttributeSource;
import cn.taketoday.transaction.interceptor.RollbackRuleAttribute;
import cn.taketoday.transaction.interceptor.RuleBasedTransactionAttribute;
import cn.taketoday.transaction.interceptor.TransactionAttribute;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

/**
 * Implementation of the
 * {@link cn.taketoday.transaction.interceptor.TransactionAttributeSource}
 * interface for working with transaction metadata in JDK 1.5+ annotation format.
 *
 * <p>This class reads Infra JDK 1.5+ {@link Transactional} annotation and
 * exposes corresponding transaction attributes to Framework's transaction infrastructure.
 * Also supports JTA 1.2's {@link jakarta.transaction.Transactional} and EJB3's
 * {@link jakarta.ejb.TransactionAttribute} annotation (if present).
 * This class may also serve as base class for a custom TransactionAttributeSource,
 * or get customized through {@link TransactionAnnotationParser} strategies.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Transactional
 * @see TransactionAnnotationParser
 * @see TransactionalAnnotationParser
 * @see Ejb3TransactionAnnotationParser
 * @see cn.taketoday.transaction.interceptor.TransactionInterceptor#setTransactionAttributeSource
 * @see cn.taketoday.transaction.interceptor.TransactionProxyFactoryBean#setTransactionAttributeSource
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AnnotationTransactionAttributeSource
        extends AbstractFallbackTransactionAttributeSource implements Serializable {

  private final boolean publicMethodsOnly;

  private final Set<TransactionAnnotationParser> annotationParsers;

  @Nullable
  private Set<RollbackRuleAttribute> defaultRollbackRules;

  /**
   * Create a default AnnotationTransactionAttributeSource, supporting
   * public methods that carry the {@code Transactional} annotation
   * or the EJB3 {@link jakarta.ejb.TransactionAttribute} annotation.
   */
  public AnnotationTransactionAttributeSource() {
    this(true);
  }

  /**
   * Create a custom AnnotationTransactionAttributeSource, supporting
   * public methods that carry the {@code Transactional} annotation
   * or the EJB3 {@link jakarta.ejb.TransactionAttribute} annotation.
   *
   * @param publicMethodsOnly whether to support public methods that carry
   * the {@code Transactional} annotation only (typically for use
   * with proxy-based AOP), or protected/private methods as well
   * (typically used with AspectJ class weaving)
   */
  public AnnotationTransactionAttributeSource(boolean publicMethodsOnly) {
    this.publicMethodsOnly = publicMethodsOnly;
    ClassLoader classLoader = getClass().getClassLoader();
    boolean jta12Present = ClassUtils.isPresent("jakarta.transaction.Transactional", classLoader);
    boolean ejb3Present = ClassUtils.isPresent("jakarta.ejb.TransactionAttribute", classLoader);

    if (jta12Present || ejb3Present) {
      this.annotationParsers = CollectionUtils.newLinkedHashSet(3);
      this.annotationParsers.add(new TransactionalAnnotationParser());
      if (jta12Present) {
        this.annotationParsers.add(new JtaTransactionAnnotationParser());
      }
      if (ejb3Present) {
        this.annotationParsers.add(new Ejb3TransactionAnnotationParser());
      }
    }
    else {
      this.annotationParsers = Collections.singleton(new TransactionalAnnotationParser());
    }
  }

  /**
   * Create a custom AnnotationTransactionAttributeSource.
   *
   * @param annotationParser the TransactionAnnotationParser to use
   */
  public AnnotationTransactionAttributeSource(TransactionAnnotationParser annotationParser) {
    this.publicMethodsOnly = true;
    Assert.notNull(annotationParser, "TransactionAnnotationParser is required");
    this.annotationParsers = Collections.singleton(annotationParser);
  }

  /**
   * Create a custom AnnotationTransactionAttributeSource.
   *
   * @param annotationParsers the TransactionAnnotationParsers to use
   */
  public AnnotationTransactionAttributeSource(TransactionAnnotationParser... annotationParsers) {
    this.publicMethodsOnly = true;
    Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
    this.annotationParsers = new LinkedHashSet<>(Arrays.asList(annotationParsers));
  }

  /**
   * Create a custom AnnotationTransactionAttributeSource.
   *
   * @param annotationParsers the TransactionAnnotationParsers to use
   */
  public AnnotationTransactionAttributeSource(Set<TransactionAnnotationParser> annotationParsers) {
    this.publicMethodsOnly = true;
    Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
    this.annotationParsers = annotationParsers;
  }

  /**
   * Add a default rollback rule, to be applied to all rule-based
   * transaction attributes returned by this source.
   * <p>By default, a rollback will be triggered on unchecked exceptions
   * but not on checked exceptions. A default rule may override this
   * while still respecting any custom rules in the transaction attribute.
   *
   * @param rollbackRule a rollback rule overriding the default behavior,
   * for example, {@link RollbackRuleAttribute#ROLLBACK_ON_ALL_EXCEPTIONS}
   * @see RuleBasedTransactionAttribute#getRollbackRules()
   * @see EnableTransactionManagement#rollbackOn()
   * @see Transactional#rollbackFor()
   * @see Transactional#noRollbackFor()
   * @since 5.0
   */
  public void addDefaultRollbackRule(RollbackRuleAttribute rollbackRule) {
    if (this.defaultRollbackRules == null) {
      this.defaultRollbackRules = new LinkedHashSet<>();
    }
    this.defaultRollbackRules.add(rollbackRule);
  }

  @Override
  public boolean isCandidateClass(Class<?> targetClass) {
    for (TransactionAnnotationParser parser : this.annotationParsers) {
      if (parser.isCandidateClass(targetClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Nullable
  protected TransactionAttribute findTransactionAttribute(Class<?> clazz) {
    return determineTransactionAttribute(clazz);
  }

  @Override
  @Nullable
  protected TransactionAttribute findTransactionAttribute(Method method) {
    return determineTransactionAttribute(method);
  }

  /**
   * Determine the transaction attribute for the given method or class.
   * <p>This implementation delegates to configured
   * {@link TransactionAnnotationParser TransactionAnnotationParsers}
   * for parsing known annotations into Framework's metadata attribute class.
   * Returns {@code null} if it's not transactional.
   * <p>Can be overridden to support custom annotations that carry transaction metadata.
   *
   * @param element the annotated method or class
   * @return the configured transaction attribute, or {@code null} if none was found
   */
  @Nullable
  protected TransactionAttribute determineTransactionAttribute(AnnotatedElement element) {
    for (TransactionAnnotationParser parser : this.annotationParsers) {
      TransactionAttribute attr = parser.parseTransactionAnnotation(element);
      if (attr != null) {
        if (this.defaultRollbackRules != null && attr instanceof RuleBasedTransactionAttribute ruleAttr) {
          ruleAttr.getRollbackRules().addAll(this.defaultRollbackRules);
        }
        return attr;
      }
    }
    return null;
  }

  /**
   * By default, only public methods can be made transactional.
   */
  @Override
  protected boolean allowPublicMethodsOnly() {
    return this.publicMethodsOnly;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AnnotationTransactionAttributeSource otherTas)) {
      return false;
    }
    return annotationParsers.equals(otherTas.annotationParsers)
            && publicMethodsOnly == otherTas.publicMethodsOnly;
  }

  @Override
  public int hashCode() {
    return this.annotationParsers.hashCode();
  }

}
