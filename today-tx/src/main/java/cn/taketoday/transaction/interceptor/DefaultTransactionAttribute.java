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

package cn.taketoday.transaction.interceptor;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.core.StringValueResolver;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.support.DefaultTransactionDefinition;
import cn.taketoday.util.StringUtils;

/**
 * Framework's common transaction attribute implementation.
 * Rolls back on runtime, but not checked, exceptions by default.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultTransactionAttribute extends DefaultTransactionDefinition implements TransactionAttribute {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private String descriptor;

  @Nullable
  private String timeoutString;

  @Nullable
  private String qualifier;

  private Collection<String> labels = Collections.emptyList();

  /**
   * Create a new DefaultTransactionAttribute, with default settings.
   * Can be modified through bean property setters.
   *
   * @see #setPropagationBehavior
   * @see #setIsolationLevel
   * @see #setTimeout
   * @see #setReadOnly
   * @see #setName
   */
  public DefaultTransactionAttribute() {
    super();
  }

  /**
   * Copy constructor. Definition can be modified through bean property setters.
   *
   * @see #setPropagationBehavior
   * @see #setIsolationLevel
   * @see #setTimeout
   * @see #setReadOnly
   * @see #setName
   */
  public DefaultTransactionAttribute(TransactionAttribute other) {
    super(other);
  }

  /**
   * Create a new DefaultTransactionAttribute with the given
   * propagation behavior. Can be modified through bean property setters.
   *
   * @param propagationBehavior one of the propagation constants in the
   * TransactionDefinition interface
   * @see #setIsolationLevel
   * @see #setTimeout
   * @see #setReadOnly
   */
  public DefaultTransactionAttribute(int propagationBehavior) {
    super(propagationBehavior);
  }

  /**
   * Set a descriptor for this transaction attribute,
   * e.g. indicating where the attribute is applying.
   */
  public void setDescriptor(@Nullable String descriptor) {
    this.descriptor = descriptor;
  }

  /**
   * Return a descriptor for this transaction attribute,
   * or {@code null} if none.
   */
  @Nullable
  public String getDescriptor() {
    return this.descriptor;
  }

  /**
   * Set the timeout to apply, if any,
   * as a String value that resolves to a number of seconds.
   *
   * @see #setTimeout
   * @see #resolveAttributeStrings
   */
  public void setTimeoutString(@Nullable String timeoutString) {
    this.timeoutString = timeoutString;
  }

  /**
   * Return the timeout to apply, if any,
   * as a String value that resolves to a number of seconds.
   *
   * @see #getTimeout
   * @see #resolveAttributeStrings
   */
  @Nullable
  public String getTimeoutString() {
    return this.timeoutString;
  }

  /**
   * Associate a qualifier value with this transaction attribute.
   * <p>This may be used for choosing a corresponding transaction manager
   * to process this specific transaction.
   *
   * @see #resolveAttributeStrings
   */
  public void setQualifier(@Nullable String qualifier) {
    this.qualifier = qualifier;
  }

  /**
   * Return a qualifier value associated with this transaction attribute.
   */
  @Override
  @Nullable
  public String getQualifier() {
    return this.qualifier;
  }

  /**
   * Associate one or more labels with this transaction attribute.
   * <p>This may be used for applying specific transactional behavior
   * or follow a purely descriptive nature.
   *
   * @see #resolveAttributeStrings
   */
  public void setLabels(Collection<String> labels) {
    this.labels = labels;
  }

  @Override
  public Collection<String> getLabels() {
    return this.labels;
  }

  /**
   * The default behavior is as with EJB: rollback on unchecked exception
   * ({@link RuntimeException}), assuming an unexpected outcome outside of any
   * business rules. Additionally, we also attempt to rollback on {@link Error} which
   * is clearly an unexpected outcome as well. By contrast, a checked exception is
   * considered a business exception and therefore a regular expected outcome of the
   * transactional business method, i.e. a kind of alternative return value which
   * still allows for regular completion of resource operations.
   * <p>This is largely consistent with TransactionTemplate's default behavior,
   * except that TransactionTemplate also rolls back on undeclared checked exceptions
   * (a corner case). For declarative transactions, we expect checked exceptions to be
   * intentionally declared as business exceptions, leading to a commit by default.
   *
   * @see cn.taketoday.transaction.support.TransactionTemplate#executeWithoutResult
   */
  @Override
  public boolean rollbackOn(Throwable ex) {
    return ex instanceof RuntimeException || ex instanceof Error;
  }

  /**
   * Resolve attribute values that are defined as resolvable Strings:
   * {@link #setTimeoutString}, {@link #setQualifier}, {@link #setLabels}.
   * This is typically used for resolving "${...}" placeholders.
   *
   * @param resolver the embedded value resolver to apply, if any
   */
  public void resolveAttributeStrings(@Nullable StringValueResolver resolver) {
    String timeoutString = this.timeoutString;
    if (StringUtils.hasText(timeoutString)) {
      if (resolver != null) {
        timeoutString = resolver.resolveStringValue(timeoutString);
      }
      if (StringUtils.isNotEmpty(timeoutString)) {
        try {
          setTimeout(Integer.parseInt(timeoutString));
        }
        catch (RuntimeException ex) {
          throw new IllegalArgumentException(
                  "Invalid timeoutString value \"%s\" - cannot parse into int".formatted(timeoutString), ex);
        }
      }
    }

    if (resolver != null) {
      if (this.qualifier != null) {
        this.qualifier = resolver.resolveStringValue(this.qualifier);
      }
      Set<String> resolvedLabels = new LinkedHashSet<>(this.labels.size());
      for (String label : this.labels) {
        resolvedLabels.add(resolver.resolveStringValue(label));
      }
      this.labels = resolvedLabels;
    }
  }

  /**
   * Return an identifying description for this transaction attribute.
   * <p>Available to subclasses, for inclusion in their {@code toString()} result.
   */
  protected final StringBuilder getAttributeDescription() {
    StringBuilder result = getDefinitionDescription();
    if (StringUtils.hasText(this.qualifier)) {
      result.append("; '").append(this.qualifier).append('\'');
    }
    if (!this.labels.isEmpty()) {
      result.append("; ").append(this.labels);
    }
    return result;
  }

}
