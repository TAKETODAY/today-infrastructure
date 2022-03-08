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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionReference;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.jdbc.core.SqlParameter;
import cn.taketoday.jdbc.core.namedparam.Customer;
import cn.taketoday.jdbc.datasource.TestDataSourceWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
@Execution(ExecutionMode.SAME_THREAD)
public class GenericSqlQueryTests {

  private static final String SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED =
          "select id, forename from custmr where id = ? and country = ?";

  private StandardBeanFactory beanFactory;

  private Connection connection;

  private PreparedStatement preparedStatement;

  private ResultSet resultSet;

  /*
	<bean id="dataSource" class="cn.taketoday.jdbc.datasource.TestDataSourceWrapper"/>
	<bean id="queryWithPlaceholders" class="cn.taketoday.jdbc.object.GenericSqlQuery">
		<property name="dataSource" ref="dataSource"/>
		<property name="sql" value="select id, forename from custmr where id = ? and country = ?"/>
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
						<util:constant static-field="java.sql.Types.VARCHAR"/>
					</constructor-arg>
				</bean>
			</list>
		</property>
    <property name="rowMapperClass" value="cn.taketoday.jdbc.object.CustomerMapper"/>
	</bean>
	<bean id="queryWithNamedParameters" class="cn.taketoday.jdbc.object.GenericSqlQuery">
		<property name="dataSource" ref="dataSource"/>
		<property name="sql" value="select id, forename from custmr where id = :id and country = :country"/>
		<property name="parameters">
			<list>
				<bean class="cn.taketoday.jdbc.core.SqlParameter">
					<constructor-arg index="0" value="id"/>
					<constructor-arg index="1">
						<util:constant static-field="java.sql.Types.INTEGER"/>
					</constructor-arg>
				</bean>
				<bean class="cn.taketoday.jdbc.core.SqlParameter">
					<constructor-arg index="0" value="country"/>
					<constructor-arg index="1">
						<util:constant static-field="java.sql.Types.VARCHAR"/>
					</constructor-arg>
				</bean>
			</list>
		</property>
        <property name="rowMapperClass" value="cn.taketoday.jdbc.object.CustomerMapper"/>
	</bean>

	<bean id="queryWithRowMapperBean" class="cn.taketoday.jdbc.object.GenericSqlQuery">
		<property name="dataSource" ref="dataSource"/>
		<property name="sql" value="select id, forename from custmr where id = :id and country = :country"/>
		<property name="parameters">
			<list>
				<bean class="cn.taketoday.jdbc.core.SqlParameter">
					<constructor-arg index="0" value="id"/>
					<constructor-arg index="1">
						<util:constant static-field="java.sql.Types.INTEGER"/>
					</constructor-arg>
				</bean>
				<bean class="cn.taketoday.jdbc.core.SqlParameter">
					<constructor-arg index="0" value="country"/>
					<constructor-arg index="1">
						<util:constant static-field="java.sql.Types.VARCHAR"/>
					</constructor-arg>
				</bean>
			</list>
		</property>
		<property name="rowMapper">
			<bean class="cn.taketoday.jdbc.object.CustomerMapper"/>
		</property>
	</bean>
   */

  @BeforeEach
  public void setUp() throws Exception {
    this.beanFactory = new StandardBeanFactory();

    // <bean id="dataSource" class="cn.taketoday.jdbc.datasource.TestDataSourceWrapper"/>

    beanFactory.registerBeanDefinition(new BeanDefinition("dataSource", TestDataSourceWrapper.class));
    beanFactory.registerBeanDefinition(new BeanDefinition("queryWithPlaceholders", GenericSqlQuery.class)
            .addPropertyValue("dataSource", RuntimeBeanReference.from("dataSource"))
            .addPropertyValue("rowMapperClass", CustomerMapper.class)
            .addPropertyValue("sql", "select id, forename from custmr where id = ? and country = ?")
            .addPropertyValue("parameters", List.of(
                    /* <bean class="cn.taketoday.jdbc.core.SqlParameter">
                        <constructor-arg index="0" value="amount"/>
                        <constructor-arg index="1">
                          <util:constant static-field="java.sql.Types.INTEGER"/>
                        </constructor-arg>
                      </bean>
                      <bean class="cn.taketoday.jdbc.core.SqlParameter">
                        <constructor-arg index="0" value="custid"/>
                        <constructor-arg index="1">
                          <util:constant static-field="java.sql.Types.VARCHAR"/>
                        </constructor-arg>
                      </bean>*/

                    new SqlParameter("amount", Types.INTEGER),
                    new SqlParameter("custid", Types.VARCHAR)
            ))

    );

    beanFactory.registerBeanDefinition(new BeanDefinition("queryWithNamedParameters", GenericSqlQuery.class)
            .addPropertyValue("dataSource", RuntimeBeanReference.from("dataSource"))
            .addPropertyValue("rowMapperClass", CustomerMapper.class)
            .addPropertyValue("sql", "select id, forename from custmr where id = :id and country = :country")
            .addPropertyValue("parameters", List.of(
                    new SqlParameter("id", Types.INTEGER),
                    new SqlParameter("country", Types.VARCHAR)
            ))

    );
    beanFactory.registerBeanDefinition(new BeanDefinition("queryWithRowMapperBean", GenericSqlQuery.class)
            .addPropertyValue("dataSource", RuntimeBeanReference.from("dataSource"))
            .addPropertyValue("rowMapperClass", CustomerMapper.class)
            .addPropertyValue("rowMapper", BeanDefinitionReference.from(CustomerMapper.class))
            .addPropertyValue("sql", "select id, forename from custmr where id = :id and country = :country")
            .addPropertyValue("parameters", List.of(
                    /*<list>
                        <bean class="cn.taketoday.jdbc.core.SqlParameter">
                          <constructor-arg index="0" value="id"/>
                          <constructor-arg index="1">
                            <util:constant static-field="java.sql.Types.INTEGER"/>
                          </constructor-arg>
                        </bean>
                        <bean class="cn.taketoday.jdbc.core.SqlParameter">
                          <constructor-arg index="0" value="country"/>
                          <constructor-arg index="1">
                            <util:constant static-field="java.sql.Types.VARCHAR"/>
                          </constructor-arg>
                        </bean>
                      </list>*/
                    new SqlParameter("id", Types.INTEGER),
                    new SqlParameter("country", Types.VARCHAR)
            ))

    );

    DataSource dataSource = mock(DataSource.class);
    this.connection = mock(Connection.class);
    this.preparedStatement = mock(PreparedStatement.class);
    this.resultSet = mock(ResultSet.class);
    given(dataSource.getConnection()).willReturn(connection);
    TestDataSourceWrapper testDataSource = (TestDataSourceWrapper) beanFactory.getBean("dataSource");
    testDataSource.setTarget(dataSource);
  }

  @Test
  public void testCustomerQueryWithPlaceholders() throws SQLException {
    SqlQuery<?> query = (SqlQuery<?>) beanFactory.getBean("queryWithPlaceholders");
    doTestCustomerQuery(query, false);
  }

  @Test
  public void testCustomerQueryWithNamedParameters() throws SQLException {
    SqlQuery<?> query = (SqlQuery<?>) beanFactory.getBean("queryWithNamedParameters");
    doTestCustomerQuery(query, true);
  }

  @Test
  public void testCustomerQueryWithRowMapperInstance() throws SQLException {
    SqlQuery<?> query = (SqlQuery<?>) beanFactory.getBean("queryWithRowMapperBean");
    doTestCustomerQuery(query, true);
  }

  private void doTestCustomerQuery(SqlQuery<?> query, boolean namedParameters) throws SQLException {
    given(resultSet.next()).willReturn(true);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");
    given(resultSet.next()).willReturn(true, false);
    given(preparedStatement.executeQuery()).willReturn(resultSet);
    given(connection.prepareStatement(SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED)).willReturn(preparedStatement);

    List<?> queryResults;
    if (namedParameters) {
      Map<String, Object> params = new HashMap<>(2);
      params.put("id", 1);
      params.put("country", "UK");
      queryResults = query.executeByNamedParam(params);
    }
    else {
      Object[] params = new Object[] { 1, "UK" };
      queryResults = query.execute(params);
    }
    assertThat(queryResults.size() == 1).as("Customer was returned correctly").isTrue();
    Customer cust = (Customer) queryResults.get(0);
    assertThat(cust.getId() == 1).as("Customer id was assigned correctly").isTrue();
    assertThat(cust.getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();

    verify(resultSet).close();
    verify(preparedStatement).setObject(1, 1, Types.INTEGER);
    verify(preparedStatement).setString(2, "UK");
    verify(preparedStatement).close();
  }

}
