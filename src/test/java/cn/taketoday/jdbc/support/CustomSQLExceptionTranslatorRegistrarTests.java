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

package cn.taketoday.jdbc.support;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.TransientDataAccessResourceException;
import cn.taketoday.jdbc.BadSqlGrammarException;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for custom {@link SQLExceptionTranslator}.
 *
 * @author Thomas Risberg
 */
public class CustomSQLExceptionTranslatorRegistrarTests {

  /*
  	<jdbc:embedded-database id="dataSource" type="H2"/>

	<bean class="cn.taketoday.jdbc.support.CustomSQLExceptionTranslatorRegistrar">
		<property name="translators">
			<map>
				<entry key="H2">
					<bean class="cn.taketoday.jdbc.support.CustomSqlExceptionTranslator"/>
				</entry>
			</map>
		</property>
	</bean>

*/
  @Test
  @SuppressWarnings("resource")
  public void customErrorCodeTranslation() {
    GenericApplicationContext context = new GenericApplicationContext();

    context.registerBeanDefinition(new BeanDefinition("dataSource", EmbeddedDatabaseFactoryBean.class)
            .addPropertyValue("databaseType", "H2"));

    context.registerBeanDefinition(new BeanDefinition("customSQLExceptionTranslatorRegistrar", CustomSQLExceptionTranslatorRegistrar.class)
            .addPropertyValue("translators[H2]", new CustomSqlExceptionTranslator()));

    context.refresh();

    SQLErrorCodes codes = SQLErrorCodesFactory.getInstance().getErrorCodes("H2");
    SQLErrorCodeSQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator();
    sext.setSqlErrorCodes(codes);

    DataAccessException exFor4200 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 42000));
    assertThat(exFor4200).as("Should have been translated").isNotNull();
    assertThat(BadSqlGrammarException.class.isAssignableFrom(exFor4200.getClass())).as("Should have been instance of BadSqlGrammarException").isTrue();

    DataAccessException exFor2 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 2));
    assertThat(exFor2).as("Should have been translated").isNotNull();
    assertThat(TransientDataAccessResourceException.class.isAssignableFrom(exFor2.getClass())).as("Should have been instance of TransientDataAccessResourceException").isTrue();

    DataAccessException exFor3 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 3));
    assertThat(exFor3).as("Should not have been translated").isNull();
  }

}
