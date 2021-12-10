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
import cn.taketoday.dao.IncorrectResultSizeDataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.Customer;
import cn.taketoday.jdbc.core.SqlParameter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Trevor Cook
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class SqlQueryTests  {

	//FIXME inline?
	private static final String SELECT_ID =
			"select id from custmr";
	private static final String SELECT_ID_WHERE =
			"select id from custmr where forename = ? and id = ?";
	private static final String SELECT_FORENAME =
			"select forename from custmr";
	private static final String SELECT_FORENAME_EMPTY =
			"select forename from custmr WHERE 1 = 2";
	private static final String SELECT_ID_FORENAME_WHERE =
			"select id, forename from prefix:custmr where forename = ?";
	private static final String SELECT_ID_FORENAME_NAMED_PARAMETERS =
			"select id, forename from custmr where id = :id and country = :country";
	private static final String SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED =
			"select id, forename from custmr where id = ? and country = ?";
	private static final String SELECT_ID_FORENAME_WHERE_ID_IN_LIST_1 =
			"select id, forename from custmr where id in (?, ?)";
	private static final String SELECT_ID_FORENAME_WHERE_ID_IN_LIST_2 =
			"select id, forename from custmr where id in (:ids)";
	private static final String SELECT_ID_FORENAME_WHERE_ID_REUSED_1 =
			"select id, forename from custmr where id = ? or id = ?)";
	private static final String SELECT_ID_FORENAME_WHERE_ID_REUSED_2 =
			"select id, forename from custmr where id = :id1 or id = :id1)";
	private static final String SELECT_ID_FORENAME_WHERE_ID =
			"select id, forename from custmr where id <= ?";

	private static final String[] COLUMN_NAMES = new String[] {"id", "forename"};
	private static final int[] COLUMN_TYPES = new int[] {Types.INTEGER, Types.VARCHAR};

	private Connection connection;
	private DataSource dataSource;
	private PreparedStatement preparedStatement;
	private ResultSet resultSet;


	@BeforeEach
	public void setUp() throws Exception {
		this.connection = mock(Connection.class);
		this.dataSource = mock(DataSource.class);
		this.preparedStatement = mock(PreparedStatement.class);
		this.resultSet = mock(ResultSet.class);
		given(this.dataSource.getConnection()).willReturn(this.connection);
		given(this.connection.prepareStatement(anyString())).willReturn(this.preparedStatement);
		given(preparedStatement.executeQuery()).willReturn(resultSet);
	}

	@Test
	public void testQueryWithoutParams() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(1);

		SqlQuery<Integer> query = new MappingSqlQueryWithParameters<Integer>() {
			@Override
			protected Integer mapRow(ResultSet rs, int rownum, @Nullable Object[] params, @Nullable Map<? ,?> context)
					throws SQLException {
				assertThat(params == null).as("params were null").isTrue();
				assertThat(context == null).as("context was null").isTrue();
				return rs.getInt(1);
			}
		};
		query.setDataSource(dataSource);
		query.setSql(SELECT_ID);
		query.compile();
		List<Integer> list = query.execute();

		assertThat(list).isEqualTo(Arrays.asList(1));
		verify(connection).prepareStatement(SELECT_ID);
		verify(resultSet).close();
		verify(preparedStatement).close();
	}

	@Test
	public void testQueryWithoutEnoughParams() {
		MappingSqlQuery<Integer> query = new MappingSqlQuery<Integer>() {
			@Override
			protected Integer mapRow(ResultSet rs, int rownum) throws SQLException {
				return rs.getInt(1);
			}
		};
		query.setDataSource(dataSource);
		query.setSql(SELECT_ID_WHERE);
		query.declareParameter(new SqlParameter(COLUMN_NAMES[0], COLUMN_TYPES[0]));
		query.declareParameter(new SqlParameter(COLUMN_NAMES[1], COLUMN_TYPES[1]));
		query.compile();

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(
				query::execute);
	}

	@Test
	public void testQueryWithMissingMapParams() {
		MappingSqlQuery<Integer> query = new MappingSqlQuery<Integer>() {
			@Override
			protected Integer mapRow(ResultSet rs, int rownum) throws SQLException {
				return rs.getInt(1);
			}
		};
		query.setDataSource(dataSource);
		query.setSql(SELECT_ID_WHERE);
		query.declareParameter(new SqlParameter(COLUMN_NAMES[0], COLUMN_TYPES[0]));
		query.declareParameter(new SqlParameter(COLUMN_NAMES[1], COLUMN_TYPES[1]));
		query.compile();

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				query.executeByNamedParam(Collections.singletonMap(COLUMN_NAMES[0], "value")));
	}

	@Test
	public void testStringQueryWithResults() throws Exception {
		String[] dbResults = new String[] { "alpha", "beta", "charlie" };
		given(resultSet.next()).willReturn(true, true, true, false);
		given(resultSet.getString(1)).willReturn(dbResults[0], dbResults[1], dbResults[2]);
		StringQuery query = new StringQuery(dataSource, SELECT_FORENAME);
		query.setRowsExpected(3);
		String[] results = query.run();
		assertThat(results).isEqualTo(dbResults);
		verify(connection).prepareStatement(SELECT_FORENAME);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	public void testStringQueryWithoutResults() throws SQLException {
		given(resultSet.next()).willReturn(false);
		StringQuery query = new StringQuery(dataSource, SELECT_FORENAME_EMPTY);
		String[] results = query.run();
		assertThat(results).isEqualTo(new String[0]);
		verify(connection).prepareStatement(SELECT_FORENAME_EMPTY);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	public void testFindCustomerIntInt() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_WHERE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id, int otherNum) {
				return findObject(id, otherNum);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		Customer cust = query.findCustomer(1, 1);

		assertThat(cust.getId() == 1).as("Customer id was assigned correctly").isTrue();
		assertThat(cust.getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 1, Types.NUMERIC);
		verify(connection).prepareStatement(SELECT_ID_WHERE);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	public void testFindCustomerString() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				declareParameter(new SqlParameter(Types.VARCHAR));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(String id) {
				return findObject(id);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		Customer cust = query.findCustomer("rod");

		assertThat(cust.getId() == 1).as("Customer id was assigned correctly").isTrue();
		assertThat(cust.getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
		verify(preparedStatement).setString(1, "rod");
		verify(connection).prepareStatement(SELECT_ID_FORENAME_WHERE);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	public void testFindCustomerMixed() throws SQLException {
		reset(connection);
		PreparedStatement preparedStatement2 = mock(PreparedStatement.class);
		ResultSet resultSet2 = mock(ResultSet.class);
		given(preparedStatement2.executeQuery()).willReturn(resultSet2);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");
		given(resultSet2.next()).willReturn(false);
		given(connection.prepareStatement(SELECT_ID_WHERE)).willReturn(preparedStatement, preparedStatement2);

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_WHERE);
				declareParameter(new SqlParameter(COLUMN_NAMES[0], COLUMN_TYPES[0]));
				declareParameter(new SqlParameter(COLUMN_NAMES[1], COLUMN_TYPES[1]));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id, String name) {
				return findObject(new Object[] { id, name });
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);

		Customer cust1 = query.findCustomer(1, "rod");
		assertThat(cust1 != null).as("Found customer").isTrue();
		assertThat(cust1.getId() == 1).as("Customer id was assigned correctly").isTrue();

		Customer cust2 = query.findCustomer(1, "Roger");
		assertThat(cust2 == null).as("No customer found").isTrue();

		verify(preparedStatement).setObject(1, 1, Types.INTEGER);
		verify(preparedStatement).setString(2, "rod");
		verify(preparedStatement2).setObject(1, 1, Types.INTEGER);
		verify(preparedStatement2).setString(2, "Roger");
		verify(resultSet).close();
		verify(resultSet2).close();
		verify(preparedStatement).close();
		verify(preparedStatement2).close();
		verify(connection, times(2)).close();
	}

	@Test
	public void testFindTooManyCustomers() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(resultSet.getString("forename")).willReturn("rod", "rod");

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				declareParameter(new SqlParameter(Types.VARCHAR));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(String id) {
				return findObject(id);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				query.findCustomer("rod"));
		verify(preparedStatement).setString(1, "rod");
		verify(connection).prepareStatement(SELECT_ID_FORENAME_WHERE);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	public void testListCustomersIntInt() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(resultSet.getString("forename")).willReturn("rod", "dave");

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_WHERE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		List<Customer> list = query.execute(1, 1);
		assertThat(list.size() == 2).as("2 results in list").isTrue();
		assertThat(list.get(0).getForename()).isEqualTo("rod");
		assertThat(list.get(1).getForename()).isEqualTo("dave");
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 1, Types.NUMERIC);
		verify(connection).prepareStatement(SELECT_ID_WHERE);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	public void testListCustomersString() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(resultSet.getString("forename")).willReturn("rod", "dave");

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				declareParameter(new SqlParameter(Types.VARCHAR));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		List<Customer> list = query.execute("one");
		assertThat(list.size() == 2).as("2 results in list").isTrue();
		assertThat(list.get(0).getForename()).isEqualTo("rod");
		assertThat(list.get(1).getForename()).isEqualTo("dave");
		verify(preparedStatement).setString(1, "one");
		verify(connection).prepareStatement(SELECT_ID_FORENAME_WHERE);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	public void testFancyCustomerQuery() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		given(connection.prepareStatement(SELECT_ID_FORENAME_WHERE,
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)
			).willReturn(preparedStatement);

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id) {
				return findObject(id);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		Customer cust = query.findCustomer(1);
		assertThat(cust.getId() == 1).as("Customer id was assigned correctly").isTrue();
		assertThat(cust.getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	public void testUnnamedParameterDeclarationWithNamedParameterQuery()
			throws SQLException {
		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id) {
				Map<String, Integer> params = new HashMap<>();
				params.put("id", id);
				return executeByNamedParam(params).get(0);
			}
		}

		// Query should not succeed since parameter declaration did not specify parameter name
		CustomerQuery query = new CustomerQuery(dataSource);
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				query.findCustomer(1));
	}

	@Test
	public void testNamedParameterCustomerQueryWithUnnamedDeclarations()
			throws SQLException {
		doTestNamedParameterCustomerQuery(false);
	}

	@Test
	public void testNamedParameterCustomerQueryWithNamedDeclarations()
			throws SQLException {
		doTestNamedParameterCustomerQuery(true);
	}

	private void doTestNamedParameterCustomerQuery(final boolean namedDeclarations)
			throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");
		given(connection.prepareStatement(SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED,
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)
			).willReturn(preparedStatement);

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_NAMED_PARAMETERS);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				if (namedDeclarations) {
					declareParameter(new SqlParameter("country", Types.VARCHAR));
					declareParameter(new SqlParameter("id", Types.NUMERIC));
				}
				else {
					declareParameter(new SqlParameter(Types.NUMERIC));
					declareParameter(new SqlParameter(Types.VARCHAR));
				}
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id, String country) {
				Map<String, Object> params = new HashMap<>();
				params.put("id", id);
				params.put("country", country);
				return executeByNamedParam(params).get(0);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		Customer cust = query.findCustomer(1, "UK");
		assertThat(cust.getId() == 1).as("Customer id was assigned correctly").isTrue();
		assertThat(cust.getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setString(2, "UK");
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	public void testNamedParameterInListQuery() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(resultSet.getString("forename")).willReturn("rod", "juergen");

		given(connection.prepareStatement(SELECT_ID_FORENAME_WHERE_ID_IN_LIST_1,
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)
			).willReturn(preparedStatement);

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID_IN_LIST_2);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter("ids", Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public List<Customer> findCustomers(List<Integer> ids) {
				Map<String, Object> params = new HashMap<>();
				params.put("ids", ids);
				return executeByNamedParam(params);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		List<Integer> ids = new ArrayList<>();
		ids.add(1);
		ids.add(2);
		List<Customer> cust = query.findCustomers(ids);

		assertThat(cust.size()).as("We got two customers back").isEqualTo(2);
		assertThat(1).as("First customer id was assigned correctly").isEqualTo(cust.get(0).getId());
		assertThat("rod").as("First customer forename was assigned correctly").isEqualTo(cust.get(0).getForename());
		assertThat(2).as("Second customer id was assigned correctly").isEqualTo(cust.get(1).getId());
		assertThat("juergen").as("Second customer forename was assigned correctly").isEqualTo(cust.get(1).getForename());
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 2, Types.NUMERIC);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	public void testNamedParameterQueryReusingParameter() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(resultSet.getString("forename")).willReturn("rod", "juergen");

		given(connection.prepareStatement(SELECT_ID_FORENAME_WHERE_ID_REUSED_1,
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)).willReturn(preparedStatement)
;

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID_REUSED_2);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter("id1", Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public List<Customer> findCustomers(Integer id) {
				Map<String, Object> params = new HashMap<>();
				params.put("id1", id);
				return executeByNamedParam(params);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		List<Customer> cust = query.findCustomers(1);

		assertThat(cust.size()).as("We got two customers back").isEqualTo(2);
		assertThat(1).as("First customer id was assigned correctly").isEqualTo(cust.get(0).getId());
		assertThat("rod").as("First customer forename was assigned correctly").isEqualTo(cust.get(0).getForename());
		assertThat(2).as("Second customer id was assigned correctly").isEqualTo(cust.get(1).getId());
		assertThat("juergen").as("Second customer forename was assigned correctly").isEqualTo(cust.get(1).getForename());

		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 1, Types.NUMERIC);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	public void testNamedParameterUsingInvalidQuestionMarkPlaceHolders()
			throws SQLException {
		given(
		connection.prepareStatement(SELECT_ID_FORENAME_WHERE_ID_REUSED_1,
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)).willReturn(preparedStatement);

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID_REUSED_1);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter("id1", Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public List<Customer> findCustomers(Integer id1) {
				Map<String, Integer> params = new HashMap<>();
				params.put("id1", id1);
				return executeByNamedParam(params);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				query.findCustomers(1));
	}

	@Test
	public void testUpdateCustomers() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(connection.prepareStatement(SELECT_ID_FORENAME_WHERE_ID,
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
			).willReturn(preparedStatement);

		class CustomerUpdateQuery extends UpdatableSqlQuery<Customer> {

			public CustomerUpdateQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID);
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer updateRow(ResultSet rs, int rownum, @Nullable Map<? ,?> context)
					throws SQLException {
				rs.updateString(2, "" + context.get(rs.getInt(COLUMN_NAMES[0])));
				return null;
			}
		}

		CustomerUpdateQuery query = new CustomerUpdateQuery(dataSource);
		Map<Integer, String> values = new HashMap<>(2);
		values.put(1, "Rod");
		values.put(2, "Thomas");
		query.execute(2, values);
		verify(resultSet).updateString(2, "Rod");
		verify(resultSet).updateString(2, "Thomas");
		verify(resultSet, times(2)).updateRow();
		verify(preparedStatement).setObject(1, 2, Types.NUMERIC);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	private static class StringQuery extends MappingSqlQuery<String> {

		public StringQuery(DataSource ds, String sql) {
			super(ds, sql);
			compile();
		}

		@Override
		protected String mapRow(ResultSet rs, int rownum) throws SQLException {
			return rs.getString(1);
		}

		public String[] run() {
			return StringUtils.toStringArray(execute());
		}
	}

}
