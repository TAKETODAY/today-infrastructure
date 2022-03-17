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

package cn.taketoday.transaction.interceptor;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 15:33
 */
public class TransactionAttributeSourceEditorTests {

  private final TransactionAttributeSourceEditor editor = new TransactionAttributeSourceEditor();

  @Test
  public void nullValue() throws Exception {
    editor.setAsText(null);
    TransactionAttributeSource tas = (TransactionAttributeSource) editor.getValue();

    Method m = Object.class.getMethod("hashCode");
    assertThat(tas.getTransactionAttribute(m, null)).isNull();
  }

  @Test
  public void invalidFormat() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            editor.setAsText("foo=bar"));
  }

  @Test
  public void matchesSpecific() throws Exception {
    editor.setAsText(
            "java.lang.Object.hashCode=PROPAGATION_REQUIRED\n" +
                    "java.lang.Object.equals=PROPAGATION_MANDATORY\n" +
                    "java.lang.Object.*it=PROPAGATION_SUPPORTS\n" +
                    "java.lang.Object.notify=PROPAGATION_SUPPORTS\n" +
                    "java.lang.Object.not*=PROPAGATION_REQUIRED");
    TransactionAttributeSource tas = (TransactionAttributeSource) editor.getValue();

    checkTransactionProperties(tas, Object.class.getMethod("hashCode"),
            TransactionDefinition.PROPAGATION_REQUIRED);
    checkTransactionProperties(tas, Object.class.getMethod("equals", Object.class),
            TransactionDefinition.PROPAGATION_MANDATORY);
    checkTransactionProperties(tas, Object.class.getMethod("wait"),
            TransactionDefinition.PROPAGATION_SUPPORTS);
    checkTransactionProperties(tas, Object.class.getMethod("wait", long.class),
            TransactionDefinition.PROPAGATION_SUPPORTS);
    checkTransactionProperties(tas, Object.class.getMethod("wait", long.class, int.class),
            TransactionDefinition.PROPAGATION_SUPPORTS);
    checkTransactionProperties(tas, Object.class.getMethod("notify"),
            TransactionDefinition.PROPAGATION_SUPPORTS);
    checkTransactionProperties(tas, Object.class.getMethod("notifyAll"),
            TransactionDefinition.PROPAGATION_REQUIRED);
    checkTransactionProperties(tas, Object.class.getMethod("toString"), -1);
  }

  @Test
  public void matchesAll() throws Exception {
    editor.setAsText("java.lang.Object.*=PROPAGATION_REQUIRED");
    TransactionAttributeSource tas = (TransactionAttributeSource) editor.getValue();

    checkTransactionProperties(tas, Object.class.getMethod("hashCode"),
            TransactionDefinition.PROPAGATION_REQUIRED);
    checkTransactionProperties(tas, Object.class.getMethod("equals", Object.class),
            TransactionDefinition.PROPAGATION_REQUIRED);
    checkTransactionProperties(tas, Object.class.getMethod("wait"),
            TransactionDefinition.PROPAGATION_REQUIRED);
    checkTransactionProperties(tas, Object.class.getMethod("wait", long.class),
            TransactionDefinition.PROPAGATION_REQUIRED);
    checkTransactionProperties(tas, Object.class.getMethod("wait", long.class, int.class),
            TransactionDefinition.PROPAGATION_REQUIRED);
    checkTransactionProperties(tas, Object.class.getMethod("notify"),
            TransactionDefinition.PROPAGATION_REQUIRED);
    checkTransactionProperties(tas, Object.class.getMethod("notifyAll"),
            TransactionDefinition.PROPAGATION_REQUIRED);
    checkTransactionProperties(tas, Object.class.getMethod("toString"),
            TransactionDefinition.PROPAGATION_REQUIRED);
  }

  private void checkTransactionProperties(TransactionAttributeSource tas, Method method, int propagationBehavior) {
    TransactionAttribute ta = tas.getTransactionAttribute(method, null);
    if (propagationBehavior >= 0) {
      assertThat(ta).isNotNull();
      assertThat(ta.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
      assertThat(ta.getPropagationBehavior()).isEqualTo(propagationBehavior);
    }
    else {
      assertThat(ta).isNull();
    }
  }

}
