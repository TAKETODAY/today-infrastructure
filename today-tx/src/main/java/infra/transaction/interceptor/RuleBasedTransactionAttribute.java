/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.transaction.interceptor;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionAttribute implementation that works out whether a given exception
 * should cause transaction rollback by applying a number of rollback rules,
 * both positive and negative. If no custom rollback rules apply, this attribute
 * behaves like DefaultTransactionAttribute (rolling back on runtime exceptions).
 *
 * <p>{@link TransactionAttributeEditor} creates objects of this class.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionAttributeEditor
 * @since 4.0
 */
public class RuleBasedTransactionAttribute extends DefaultTransactionAttribute implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /** Prefix for rollback-on-exception rules in description strings. */
  public static final String PREFIX_ROLLBACK_RULE = "-";

  /** Prefix for commit-on-exception rules in description strings. */
  public static final String PREFIX_COMMIT_RULE = "+";

  @Nullable
  private List<RollbackRuleAttribute> rollbackRules;

  /**
   * Create a new RuleBasedTransactionAttribute, with default settings.
   * Can be modified through bean property setters.
   *
   * @see #setPropagationBehavior
   * @see #setIsolationLevel
   * @see #setTimeout
   * @see #setReadOnly
   * @see #setName
   * @see #setRollbackRules
   */
  public RuleBasedTransactionAttribute() {
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
   * @see #setRollbackRules
   */
  public RuleBasedTransactionAttribute(RuleBasedTransactionAttribute other) {
    super(other);
    this.rollbackRules = (other.rollbackRules != null ? new ArrayList<>(other.rollbackRules) : null);
  }

  /**
   * Create a new DefaultTransactionAttribute with the given
   * propagation behavior. Can be modified through bean property setters.
   *
   * @param propagationBehavior one of the propagation constants in the
   * TransactionDefinition interface
   * @param rollbackRules the list of RollbackRuleAttributes to apply
   * @see #setIsolationLevel
   * @see #setTimeout
   * @see #setReadOnly
   */
  public RuleBasedTransactionAttribute(int propagationBehavior, List<RollbackRuleAttribute> rollbackRules) {
    super(propagationBehavior);
    this.rollbackRules = rollbackRules;
  }

  /**
   * Set the list of {@code RollbackRuleAttribute} objects
   * (and/or {@code NoRollbackRuleAttribute} objects) to apply.
   *
   * @see RollbackRuleAttribute
   * @see NoRollbackRuleAttribute
   */
  public void setRollbackRules(List<RollbackRuleAttribute> rollbackRules) {
    this.rollbackRules = rollbackRules;
  }

  /**
   * Return the list of {@code RollbackRuleAttribute} objects
   * (never {@code null}).
   */
  public List<RollbackRuleAttribute> getRollbackRules() {
    if (this.rollbackRules == null) {
      this.rollbackRules = new ArrayList<>();
    }
    return this.rollbackRules;
  }

  /**
   * Winning rule is the shallowest rule (that is, the closest in the
   * inheritance hierarchy to the exception). If no rule applies (-1),
   * return false.
   *
   * @see TransactionAttribute#rollbackOn(Throwable)
   */
  @Override
  public boolean rollbackOn(Throwable ex) {
    RollbackRuleAttribute winner = null;
    int deepest = Integer.MAX_VALUE;

    if (this.rollbackRules != null) {
      for (RollbackRuleAttribute rule : this.rollbackRules) {
        int depth = rule.getDepth(ex);
        if (depth >= 0 && depth < deepest) {
          deepest = depth;
          winner = rule;
        }
      }
    }

    // User superclass behavior (rollback on unchecked) if no rule matches.
    if (winner == null) {
      return super.rollbackOn(ex);
    }

    return !(winner instanceof NoRollbackRuleAttribute);
  }

  @Override
  public String toString() {
    StringBuilder result = getAttributeDescription();
    if (this.rollbackRules != null) {
      for (RollbackRuleAttribute rule : this.rollbackRules) {
        String sign = (rule instanceof NoRollbackRuleAttribute ? PREFIX_COMMIT_RULE : PREFIX_ROLLBACK_RULE);
        result.append(',').append(sign).append(rule.getExceptionName());
      }
    }
    return result.toString();
  }

}
