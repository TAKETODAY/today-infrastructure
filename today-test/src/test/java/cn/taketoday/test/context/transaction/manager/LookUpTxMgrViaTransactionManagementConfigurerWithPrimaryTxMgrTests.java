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

package cn.taketoday.test.context.transaction.manager;

import org.junit.jupiter.api.Test;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.test.context.junit.jupiter.ApplicationJUnitConfig;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionManager;
import cn.taketoday.transaction.annotation.TransactionManagementConfigurer;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.transaction.testfixture.CallCountingTransactionManager;

/**
 * Integration test that verifies the behavior for transaction manager lookups
 * when one transaction manager is {@link Primary @Primary} and an additional
 * transaction manager is configured via the
 * {@link TransactionManagementConfigurer} API.
 *
 * @author Sam Brannen
 * @since 5.2.6
 */
@SpringJUnitConfig
@Transactional
class LookUpTxMgrViaTransactionManagementConfigurerWithPrimaryTxMgrTests {

	@Autowired
	CallCountingTransactionManager primary;

	@Autowired
	@Qualifier("annotationDrivenTransactionManager")
	CallCountingTransactionManager annotationDriven;


	@Test
	void transactionalTest() {
		assertThat(primary.begun).isEqualTo(0);
		assertThat(primary.inflight).isEqualTo(0);
		assertThat(primary.commits).isEqualTo(0);
		assertThat(primary.rollbacks).isEqualTo(0);

		assertThat(annotationDriven.begun).isEqualTo(1);
		assertThat(annotationDriven.inflight).isEqualTo(1);
		assertThat(annotationDriven.commits).isEqualTo(0);
		assertThat(annotationDriven.rollbacks).isEqualTo(0);
	}

	@AfterTransaction
	void afterTransaction() {
		assertThat(primary.begun).isEqualTo(0);
		assertThat(primary.inflight).isEqualTo(0);
		assertThat(primary.commits).isEqualTo(0);
		assertThat(primary.rollbacks).isEqualTo(0);

		assertThat(annotationDriven.begun).isEqualTo(1);
		assertThat(annotationDriven.inflight).isEqualTo(0);
		assertThat(annotationDriven.commits).isEqualTo(0);
		assertThat(annotationDriven.rollbacks).isEqualTo(1);
	}


	@Configuration
	static class Config implements TransactionManagementConfigurer {

		@Bean
		@Primary
		PlatformTransactionManager primary() {
			return new CallCountingTransactionManager();
		}

		@Bean
		@Override
		public TransactionManager annotationDrivenTransactionManager() {
			return new CallCountingTransactionManager();
		}

	}

}
