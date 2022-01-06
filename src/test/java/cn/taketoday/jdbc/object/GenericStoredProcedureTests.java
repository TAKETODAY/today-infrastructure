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

package cn.taketoday.jdbc.object;

import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.BeanReference;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.jdbc.core.SqlParameter;
import cn.taketoday.jdbc.datasource.TestDataSourceWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Thomas Risberg
 */
public class GenericStoredProcedureTests {

  /*
	<bean id="dataSource" class="cn.taketoday.jdbc.datasource.TestDataSourceWrapper"/>
	<bean id="genericProcedure" class="cn.taketoday.jdbc.object.GenericStoredProcedure">
		<property name="dataSource" ref="dataSource"/>
		<property name="sql" value="add_invoice"/>
		<property name="parameters">
			<list>
				<bean class="cn.taketoday.jdbc.core.SqlParameter">
					<constructor-arg index="0" value="amount"/>
					<constructor-arg index="1">
						<util:constant static-field="java.sql.Types.INTEGER"/>
					</constructor-arg>
				</bean>
				<bean class="cn.taketoday.jdbc.core.SqlParameter">
					<constructor-arg index="0" value="custid"/>
					<constructor-arg index="1">
						<util:constant static-field="java.sql.Types.INTEGER"/>
					</constructor-arg>
				</bean>
				<bean class="cn.taketoday.jdbc.core.SqlOutParameter">
					<constructor-arg index="0" value="newid"/>
					<constructor-arg index="1">
						<util:constant static-field="java.sql.Types.INTEGER"/>
					</constructor-arg>
				</bean>
			</list>
		</property>
	</bean>
   */

  @Test
  public void testAddInvoices() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();

    bf.registerBeanDefinition(new BeanDefinition("dataSource", TestDataSourceWrapper.class));

    bf.registerBeanDefinition(new BeanDefinition("genericProcedure", GenericStoredProcedure.class)
            .addPropertyValue("dataSource", BeanReference.from("dataSource"))
            .addPropertyValue("sql", "add_invoice")
            .addPropertyValue("parameters", List.of(
                    new SqlParameter("amount", java.sql.Types.INTEGER),
                    new SqlParameter("custid", java.sql.Types.INTEGER),
                    new SqlParameter("newid", java.sql.Types.INTEGER)
            )));

    Connection connection = mock(Connection.class);
    DataSource dataSource = mock(DataSource.class);
    given(dataSource.getConnection()).willReturn(connection);
    CallableStatement callableStatement = mock(CallableStatement.class);
    TestDataSourceWrapper testDataSource = (TestDataSourceWrapper) bf.getBean("dataSource");
    testDataSource.setTarget(dataSource);

    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(3)).willReturn(4);

    given(connection.prepareCall("{call " + "add_invoice" + "(?, ?, ?)}")).willReturn(callableStatement);

    StoredProcedure adder = (StoredProcedure) bf.getBean("genericProcedure");
    Map<String, Object> in = new HashMap<>(2);
    in.put("amount", 1106);
    in.put("custid", 3);
    Map<String, Object> out = adder.execute(in);
    Integer id = (Integer) out.get("newid");
    assertThat(id.intValue()).isEqualTo(4);

    verify(callableStatement).setObject(1, 1106, Types.INTEGER);
    verify(callableStatement).setObject(2, 3, Types.INTEGER);
    verify(callableStatement).registerOutParameter(3, Types.INTEGER);
    verify(callableStatement).close();
  }

}
