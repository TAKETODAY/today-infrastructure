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

package infra.transaction.support;

import org.junit.jupiter.api.Test;

import infra.context.testfixture.jndi.SimpleNamingContextBuilder;
import infra.core.testfixture.io.SerializationTestUtils;
import infra.transaction.jta.JtaTransactionManager;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Rod Johnson
 */
public class JtaTransactionManagerSerializationTests {

  @Test
  public void serializable() throws Exception {
    UserTransaction ut1 = mock(UserTransaction.class);
    UserTransaction ut2 = mock(UserTransaction.class);
    TransactionManager tm = mock(TransactionManager.class);

    JtaTransactionManager jtam = new JtaTransactionManager();
    jtam.setUserTransaction(ut1);
    jtam.setTransactionManager(tm);
    jtam.setRollbackOnCommitFailure(true);
    jtam.afterPropertiesSet();

    SimpleNamingContextBuilder jndiEnv = SimpleNamingContextBuilder
            .emptyActivatedContextBuilder();
    jndiEnv.bind(JtaTransactionManager.DEFAULT_USER_TRANSACTION_NAME, ut2);
    JtaTransactionManager serializedJtatm = SerializationTestUtils.serializeAndDeserialize(jtam);

    // should do client-side lookup
    assertThat(serializedJtatm.logger).as("Logger must survive serialization").isNotNull();
    assertThat(serializedJtatm
            .getUserTransaction() == ut2).as("UserTransaction looked up on client").isTrue();
    assertThat(serializedJtatm
            .getTransactionManager()).as("TransactionManager didn't survive").isNull();
    assertThat(serializedJtatm.isRollbackOnCommitFailure()).isEqualTo(true);
  }

}
