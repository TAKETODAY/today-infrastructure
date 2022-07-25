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

package cn.taketoday.transaction;

/**
 * Exception that represents a transaction failure caused by a heuristic
 * decision on the side of the transaction coordinator.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/10 21:35
 */
@SuppressWarnings("serial")
public class HeuristicCompletionException extends TransactionException {

  /**
   * Unknown outcome state.
   */
  public static final int STATE_UNKNOWN = 0;

  /**
   * Committed outcome state.
   */
  public static final int STATE_COMMITTED = 1;

  /**
   * Rolledback outcome state.
   */
  public static final int STATE_ROLLED_BACK = 2;

  /**
   * Mixed outcome state.
   */
  public static final int STATE_MIXED = 3;

  public static String getStateString(int state) {
    return switch (state) {
      case STATE_COMMITTED -> "committed";
      case STATE_ROLLED_BACK -> "rolled back";
      case STATE_MIXED -> "mixed";
      default -> "unknown";
    };
  }

  /**
   * The outcome state of the transaction: have some or all resources been committed?
   */
  private final int outcomeState;

  /**
   * Constructor for HeuristicCompletionException.
   *
   * @param outcomeState the outcome state of the transaction
   * @param cause the root cause from the transaction API in use
   */
  public HeuristicCompletionException(int outcomeState, Throwable cause) {
    super("Heuristic completion: outcome state is " + getStateString(outcomeState), cause);
    this.outcomeState = outcomeState;
  }

  /**
   * Return the outcome state of the transaction state,
   * as one of the constants in this class.
   *
   * @see #STATE_UNKNOWN
   * @see #STATE_COMMITTED
   * @see #STATE_ROLLED_BACK
   * @see #STATE_MIXED
   */
  public int getOutcomeState() {
    return this.outcomeState;
  }

}
