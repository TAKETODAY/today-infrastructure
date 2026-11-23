/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import infra.jdbc.model.UserModel;
import infra.persistence.support.DefaultEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/20 22:36
 */
class QueryTests extends AbstractRepositoryManagerTests {

  @Test
  void bindsPositionalValuesAndReplacesParametersBeforeExecution() throws SQLException {
    Connection connection = mock();
    PreparedStatement statement = mock();
    String sql = "INSERT INTO entries VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    given(connection.prepareStatement(sql)).willReturn(statement);
    given(statement.executeUpdate()).willReturn(1);
    var date = LocalDate.of(2026, 5, 8);
    var time = LocalTime.of(9, 30);
    var dateTime = date.atTime(time);
    var amount = new BigDecimal("7.25");
    var payload = new ByteArrayInputStream(new byte[] { 1, 2 });

    try (var jdbc = borrowedConnection(connection); var query = jdbc.createQuery(sql)) {
      query.addParameter(0).addParameter(0L).addParameter("initial").addParameter(false)
              .addParameter(new ByteArrayInputStream(new byte[0])).addParameter(date.minusDays(1))
              .addParameter(time.minusHours(1)).addParameter(dateTime.minusDays(1)).addParameter((Object) BigDecimal.ZERO);
      assertThat(query.getQueryParameters()).hasSize(9);
      query.setParameter(0, 2).setParameter(1, 3L).setParameter(2, "final").setParameter(3, true)
              .setParameter(4, payload).setParameter(5, date).setParameter(6, time)
              .setParameter(7, dateTime).setParameter(8, (Object) amount);

      assertThat(query.executeUpdate().getAffectedRows()).isEqualTo(1);
      query.clearParameters();
      assertThat(query.getQueryParameters()).isEmpty();
    }

    var order = inOrder(statement);
    order.verify(statement).setInt(1, 2);
    order.verify(statement).setLong(2, 3L);
    order.verify(statement).setString(3, "final");
    order.verify(statement).setBoolean(4, true);
    order.verify(statement).setBinaryStream(5, payload);
    order.verify(statement).setObject(6, date);
    order.verify(statement).setObject(7, time);
    order.verify(statement).setObject(8, dateTime);
    order.verify(statement).setBigDecimal(9, amount);
    order.verify(statement).executeUpdate();
    order.verify(statement).close();
    verify(connection, never()).close();
  }

  @Test
  void bindingFailureIdentifiesJdbcParameterIndex() throws SQLException {
    Connection connection = mock();
    PreparedStatement statement = mock();
    String sql = "UPDATE entries SET amount = ? WHERE id = ?";
    given(connection.prepareStatement(sql)).willReturn(statement);
    var failure = new SQLException("value rejected");
    willThrow(failure).given(statement).setLong(2, 7L);

    try (var jdbc = borrowedConnection(connection); var query = jdbc.createQuery(sql)) {
      query.addParameter(10).addParameter(7L);
      assertThatThrownBy(query::executeUpdate).isInstanceOf(ParameterBindFailedException.class)
              .hasMessageContaining("index: '2'").hasCause(failure);
    }

    verify(statement, never()).executeUpdate();
    verify(statement).close();
  }

  @Test
  void requestsNamedKeyColumnsAndRunsCallbackBeforeUpdate() throws SQLException {
    Connection connection = mock();
    PreparedStatement statement = mock();
    ResultSet keys = mock();
    String sql = "INSERT INTO entries (amount) VALUES (?)";
    given(connection.prepareStatement(sql, new String[] { "id" })).willReturn(statement);
    given(statement.executeUpdate()).willReturn(1);
    given(statement.getGeneratedKeys()).willReturn(keys);
    given(keys.next()).willReturn(true, false);
    given(keys.getObject(1)).willReturn(22L);

    try (var jdbc = borrowedConnection(connection); var query = jdbc.createQuery(sql, "id")) {
      query.addParameter(7).processStatement(ps -> ps.setQueryTimeout(3));
      var result = query.executeUpdate(true);

      assertThat(result.getAffectedRows()).isEqualTo(1);
      assertThat(result.getFirstKey()).isEqualTo(22L);
    }

    var order = inOrder(statement, keys);
    order.verify(statement).setInt(1, 7);
    order.verify(statement).setQueryTimeout(3);
    order.verify(statement).executeUpdate();
    order.verify(statement).getGeneratedKeys();
    order.verify(keys).close();
  }

  @Test
  void flushesAtBatchLimitAndTracksRemainingRecords() throws SQLException {
    Connection connection = mock();
    PreparedStatement statement = mock();
    String sql = "INSERT INTO entries (amount) VALUES (?)";
    given(connection.prepareStatement(sql)).willReturn(statement);
    given(statement.executeBatch()).willReturn(new int[] { 1, 1 }, new int[] { 1 });

    try (var jdbc = borrowedConnection(connection); var query = jdbc.createQuery(sql)) {
      query.setMaxBatchRecords(2);
      query.addParameter(7).addToBatch();
      assertThat(query.getCurrentBatchRecords()).isEqualTo(1);
      assertThat(query.isExplicitExecuteBatchRequired()).isTrue();
      query.setParameter(0, 8).addToBatch();
      assertThat(query.getCurrentBatchRecords()).isZero();
      assertThat(query.isExplicitExecuteBatchRequired()).isFalse();
      verify(statement).executeBatch();
      query.setParameter(0, 9).addToBatch();

      var result = query.executeBatch();
      assertThat(result.getLastBatchResult()).containsExactly(1);
      assertThat(result.getAffectedRows()).isEqualTo(3);
    }

    verify(statement, times(3)).addBatch();
    verify(statement, times(2)).executeBatch();
  }

  private static JdbcConnection borrowedConnection(Connection connection) {
    return new JdbcConnection(new RepositoryManager(mock(DataSource.class)), connection);
  }

  @Override
  protected void prepareTestsData(DbType dbType, RepositoryManager repositoryManager) {
    repositoryManager.createQuery("drop table if exists t_user").executeUpdate();
    try (NamedQuery query = repositoryManager.createNamedQuery("""
            create table t_user
            (
                id               int auto_increment primary key,
                age              int           default 0    ,
                name             varchar(255)  default null ,
                avatar           varchar(255)  default null ,
                password         varchar(255)  default null ,
                introduce        varchar(1000) default null ,
                email            varchar(255)  default null ,
                gender           int           default -1   ,
                mobile_phone     varchar(36)   default null
            );
            """)) {

      query.executeUpdate();
    }

  }

  @ParameterizedRepositoryManagerTest
  void create(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    try (JdbcConnection connection = repositoryManager.open()) {

      Query query = connection.createQuery("select * from t_user where id=?")
              .addParameter(1);

      query.setAutoDerivingColumns(true);

      assertThat(query.fetchFirst(UserModel.class))
              .isNotNull()
              .extracting("id").isEqualTo(1);
    }
  }

  @ParameterizedRepositoryManagerTest
  void addParameter(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    try (JdbcConnection connection = repositoryManager.open()) {
      Query query = connection.createQuery("select * from t_user where id=? and name=?")
              .addParameter(1)
              .addParameter("TODAY");

      query.setAutoDerivingColumns(true);

      assertThat(query.fetchFirst(UserModel.class))
              .isNotNull()
              .extracting("id", "name")
              .containsExactly(1, "TODAY");

    }
  }

  public static void createData(DefaultEntityManager entityManager) {
    UserModel userModel = UserModel.male("TODAY", 9);

    List<Object> entities = new ArrayList<>();
    entities.add(userModel);

    for (int i = 0; i < 10; i++) {
      entities.add(UserModel.male("TODAY", 10 + i));
    }

    entityManager.persist(entities);
  }
}
