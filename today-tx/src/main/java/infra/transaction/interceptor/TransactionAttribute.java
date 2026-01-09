/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.transaction.interceptor;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

import infra.transaction.TransactionDefinition;
import infra.util.StringUtils;

/**
 * This interface adds a {@code rollbackOn} specification to {@link TransactionDefinition}.
 * As custom {@code rollbackOn} is only possible with AOP, it resides in the AOP-related
 * transaction subpackage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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
