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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 * @since 4.0
 */
public class RuleBasedTransactionAttributeTests {

  @Test
  public void testDefaultRule() {
    RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute();
    assertThat(rta.rollbackOn(new RuntimeException())).isTrue();
    assertThat(rta.rollbackOn(new MyRuntimeException(""))).isTrue();
    assertThat(rta.rollbackOn(new Exception())).isFalse();
    assertThat(rta.rollbackOn(new IOException())).isFalse();
  }

  /**
   * Test one checked exception that should roll back.
   */
  @Test
  public void testRuleForRollbackOnChecked() {
    List<RollbackRuleAttribute> list = new ArrayList<>();
    list.add(new RollbackRuleAttribute(IOException.class.getName()));
    RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

    assertThat(rta.rollbackOn(new RuntimeException())).isTrue();
    assertThat(rta.rollbackOn(new MyRuntimeException(""))).isTrue();
    assertThat(rta.rollbackOn(new Exception())).isFalse();
    // Check that default behaviour is overridden
    assertThat(rta.rollbackOn(new IOException())).isTrue();
  }

  @Test
  public void testRuleForCommitOnUnchecked() {
    List<RollbackRuleAttribute> list = new ArrayList<>();
    list.add(new NoRollbackRuleAttribute(MyRuntimeException.class.getName()));
    list.add(new RollbackRuleAttribute(IOException.class.getName()));
    RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

    assertThat(rta.rollbackOn(new RuntimeException())).isTrue();
    // Check default behaviour is overridden
    assertThat(rta.rollbackOn(new MyRuntimeException(""))).isFalse();
    assertThat(rta.rollbackOn(new Exception())).isFalse();
    // Check that default behaviour is overridden
    assertThat(rta.rollbackOn(new IOException())).isTrue();
  }

  @Test
  public void testRuleForSelectiveRollbackOnCheckedWithString() {
    List<RollbackRuleAttribute> l = new ArrayList<>();
    l.add(new RollbackRuleAttribute(RemoteException.class.getName()));
    RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, l);
    doTestRuleForSelectiveRollbackOnChecked(rta);
  }

  @Test
  public void testRuleForSelectiveRollbackOnCheckedWithClass() {
    List<RollbackRuleAttribute> l = Collections.singletonList(new RollbackRuleAttribute(RemoteException.class));
    RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, l);
    doTestRuleForSelectiveRollbackOnChecked(rta);
  }

  private void doTestRuleForSelectiveRollbackOnChecked(RuleBasedTransactionAttribute rta) {
    assertThat(rta.rollbackOn(new RuntimeException())).isTrue();
    // Check default behaviour is overridden
    assertThat(rta.rollbackOn(new Exception())).isFalse();
    // Check that default behaviour is overridden
    assertThat(rta.rollbackOn(new RemoteException())).isTrue();
  }

  /**
   * Check that a rule can cause commit on a IOException
   * when Exception prompts a rollback.
   */
  @Test
  public void testRuleForCommitOnSubclassOfChecked() {
    List<RollbackRuleAttribute> list = new ArrayList<>();
    // Note that it's important to ensure that we have this as
    // a FQN: otherwise it will match everything!
    list.add(new RollbackRuleAttribute("java.lang.Exception"));
    list.add(new NoRollbackRuleAttribute("IOException"));
    RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

    assertThat(rta.rollbackOn(new RuntimeException())).isTrue();
    assertThat(rta.rollbackOn(new Exception())).isTrue();
    // Check that default behaviour is overridden
    assertThat(rta.rollbackOn(new IOException())).isFalse();
  }

  @Test
  public void testRollbackNever() {
    List<RollbackRuleAttribute> list = new ArrayList<>();
    list.add(new NoRollbackRuleAttribute("Throwable"));
    RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

    assertThat(rta.rollbackOn(new Throwable())).isFalse();
    assertThat(rta.rollbackOn(new RuntimeException())).isFalse();
    assertThat(rta.rollbackOn(new MyRuntimeException(""))).isFalse();
    assertThat(rta.rollbackOn(new Exception())).isFalse();
    assertThat(rta.rollbackOn(new IOException())).isFalse();
  }

  @Test
  public void testToStringMatchesEditor() {
    List<RollbackRuleAttribute> list = new ArrayList<>();
    list.add(new NoRollbackRuleAttribute("Throwable"));
    RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

    TransactionAttributeEditor tae = new TransactionAttributeEditor();
    tae.setAsText(rta.toString());
    rta = (RuleBasedTransactionAttribute) tae.getValue();

    assertThat(rta.rollbackOn(new Throwable())).isFalse();
    assertThat(rta.rollbackOn(new RuntimeException())).isFalse();
    assertThat(rta.rollbackOn(new MyRuntimeException(""))).isFalse();
    assertThat(rta.rollbackOn(new Exception())).isFalse();
    assertThat(rta.rollbackOn(new IOException())).isFalse();
  }

  /**
   * See <a href="https://forum.springframework.org/showthread.php?t=41350">this forum post</a>.
   */
  @Test
  public void testConflictingRulesToDetermineExactContract() {
    List<RollbackRuleAttribute> list = new ArrayList<>();
    list.add(new NoRollbackRuleAttribute(MyBusinessWarningException.class));
    list.add(new RollbackRuleAttribute(MyBusinessException.class));
    RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

    assertThat(rta.rollbackOn(new MyBusinessException())).isTrue();
    assertThat(rta.rollbackOn(new MyBusinessWarningException())).isFalse();
  }

  @SuppressWarnings("serial")
  private static class MyBusinessException extends Exception { }

  @SuppressWarnings("serial")
  private static final class MyBusinessWarningException extends MyBusinessException { }

}
