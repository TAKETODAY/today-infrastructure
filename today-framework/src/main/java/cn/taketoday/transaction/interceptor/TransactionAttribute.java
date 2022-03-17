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

import java.util.Collection;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.util.StringUtils;

/**
 * This interface adds a {@code rollbackOn} specification to {@link TransactionDefinition}.
 * As custom {@code rollbackOn} is only possible with AOP, it resides in the AOP-related
 * transaction subpackage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @see DefaultTransactionAttribute
 * @see RuleBasedTransactionAttribute
 * @since 4.0
 */
public interface TransactionAttribute extends TransactionDefinition {

  /**
   * Return a qualifier value associated with this transaction attribute.
   * <p>This may be used for choosing a corresponding transaction manager
   * to process this specific transaction.
   */
  @Nullable
  String getQualifier();

  /**
   * Return labels associated with this transaction attribute.
   * <p>This may be used for applying specific transactional behavior
   * or follow a purely descriptive nature.
   */
  Collection<String> getLabels();

  /**
   * Should we roll back on the given exception?
   *
   * @param ex the exception to evaluate
   * @return whether to perform a rollback or not
   */
  boolean rollbackOn(Throwable ex);

  /**
   * for {@link TransactionAttribute} objects. Accepts a String of form
   * <p>{@code PROPAGATION_NAME, ISOLATION_NAME, readOnly, timeout_NNNN,+Exception1,-Exception2}
   * <p>where only propagation code is required. For example:
   * <p>{@code PROPAGATION_MANDATORY, ISOLATION_DEFAULT}
   *
   * <p>The tokens can be in <strong>any</strong> order. Propagation and isolation codes
   * must use the names of the constants in the TransactionDefinition class. Timeout values
   * are in seconds. If no timeout is specified, the transaction manager will apply a default
   * timeout specific to the particular transaction manager.
   *
   * <p>A "+" before an exception name substring indicates that transactions should commit
   * even if this exception is thrown; a "-" that they should roll back.
   * <p>
   * Format is PROPAGATION_NAME,ISOLATION_NAME,readOnly,timeout_NNNN,+Exception1,-Exception2.
   * Null or the empty string means that the method is non-transactional.
   *
   * @see RuleBasedTransactionAttribute
   */
  static TransactionAttribute parse(String text) throws IllegalArgumentException {
    // tokenize it with ","
    String[] tokens = StringUtils.commaDelimitedListToStringArray(text);
    RuleBasedTransactionAttribute attr = new RuleBasedTransactionAttribute();
    for (String token : tokens) {
      // Trim leading and trailing whitespace.
      String trimmedToken = token.strip();
      // Check whether token contains illegal whitespace within text.
      if (StringUtils.containsWhitespace(trimmedToken)) {
        throw new IllegalArgumentException(
                "Transaction attribute token contains illegal whitespace: [" + trimmedToken + "]");
      }
      // Check token type.
      if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_PROPAGATION)) {
        attr.setPropagationBehaviorName(trimmedToken);
      }
      else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_ISOLATION)) {
        attr.setIsolationLevelName(trimmedToken);
      }
      else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_TIMEOUT)) {
        String value = trimmedToken.substring(DefaultTransactionAttribute.PREFIX_TIMEOUT.length());
        attr.setTimeoutString(value);
      }
      else if (trimmedToken.equals(RuleBasedTransactionAttribute.READ_ONLY_MARKER)) {
        attr.setReadOnly(true);
      }
      else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_COMMIT_RULE)) {
        attr.getRollbackRules().add(new NoRollbackRuleAttribute(trimmedToken.substring(1)));
      }
      else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_ROLLBACK_RULE)) {
        attr.getRollbackRules().add(new RollbackRuleAttribute(trimmedToken.substring(1)));
      }
      else {
        throw new IllegalArgumentException("Invalid transaction attribute token: [" + trimmedToken + "]");
      }
    }
    attr.resolveAttributeStrings(null);  // placeholders expected to be pre-resolved
    return attr;
  }

}
