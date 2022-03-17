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
import java.util.Properties;

import cn.taketoday.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the various {@link TransactionAttributeSource} implementations.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 * @see cn.taketoday.transaction.interceptor.TransactionProxyFactoryBean
 * @since 4.0
 */
public class TransactionAttributeSourceTests {

  @Test
  public void matchAlwaysTransactionAttributeSource() throws Exception {
    MatchAlwaysTransactionAttributeSource tas = new MatchAlwaysTransactionAttributeSource();
    TransactionAttribute ta = tas.getTransactionAttribute(Object.class.getMethod("hashCode"), null);
    assertThat(ta).isNotNull();
    assertThat(TransactionDefinition.PROPAGATION_REQUIRED == ta.getPropagationBehavior()).isTrue();

    tas.setTransactionAttribute(new DefaultTransactionAttribute(TransactionDefinition.PROPAGATION_SUPPORTS));
    ta = tas.getTransactionAttribute(IOException.class.getMethod("getMessage"), IOException.class);
    assertThat(ta).isNotNull();
    assertThat(TransactionDefinition.PROPAGATION_SUPPORTS == ta.getPropagationBehavior()).isTrue();
  }

  @Test
  public void nameMatchTransactionAttributeSourceWithStarAtStartOfMethodName() throws Exception {
    NameMatchTransactionAttributeSource tas = new NameMatchTransactionAttributeSource();
    Properties attributes = new Properties();
    attributes.put("*ashCode", "PROPAGATION_REQUIRED");
    tas.setProperties(attributes);
    TransactionAttribute ta = tas.getTransactionAttribute(Object.class.getMethod("hashCode"), null);
    assertThat(ta).isNotNull();
    assertThat(ta.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
  }

  @Test
  public void nameMatchTransactionAttributeSourceWithStarAtEndOfMethodName() throws Exception {
    NameMatchTransactionAttributeSource tas = new NameMatchTransactionAttributeSource();
    Properties attributes = new Properties();
    attributes.put("hashCod*", "PROPAGATION_REQUIRED");
    tas.setProperties(attributes);
    TransactionAttribute ta = tas.getTransactionAttribute(Object.class.getMethod("hashCode"), null);
    assertThat(ta).isNotNull();
    assertThat(ta.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
  }

  @Test
  public void nameMatchTransactionAttributeSourceMostSpecificMethodNameIsDefinitelyMatched() throws Exception {
    NameMatchTransactionAttributeSource tas = new NameMatchTransactionAttributeSource();
    Properties attributes = new Properties();
    attributes.put("*", "PROPAGATION_REQUIRED");
    attributes.put("hashCode", "PROPAGATION_MANDATORY");
    tas.setProperties(attributes);
    TransactionAttribute ta = tas.getTransactionAttribute(Object.class.getMethod("hashCode"), null);
    assertThat(ta).isNotNull();
    assertThat(ta.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_MANDATORY);
  }

  @Test
  public void nameMatchTransactionAttributeSourceWithEmptyMethodName() throws Exception {
    NameMatchTransactionAttributeSource tas = new NameMatchTransactionAttributeSource();
    Properties attributes = new Properties();
    attributes.put("", "PROPAGATION_MANDATORY");
    tas.setProperties(attributes);
    TransactionAttribute ta = tas.getTransactionAttribute(Object.class.getMethod("hashCode"), null);
    assertThat(ta).isNull();
  }

}
