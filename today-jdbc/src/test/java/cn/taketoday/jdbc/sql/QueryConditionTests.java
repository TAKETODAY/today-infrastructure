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

package cn.taketoday.jdbc.sql;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import cn.taketoday.jdbc.sql.model.Gender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/11 00:01
 */
class QueryConditionTests {

  @Test
  void and() {
    DefaultQueryCondition condition = DefaultQueryCondition.equalsTo("name", "T");

    StringBuilder sql = new StringBuilder();
    condition.render(sql);
    assertThat(sql.toString()).isEqualTo(" `name` = ?");

    condition.and(DefaultQueryCondition.isNull("age"));

    sql = new StringBuilder();
    condition.render(sql);
    assertThat(sql.toString()).isEqualTo(" `name` = ? AND `age` is null");

    condition.and(
            DefaultQueryCondition.between("age", new Date(), new Date())
    );

    sql = new StringBuilder();
    condition.render(sql);

    System.out.println(sql);

    assertThat(sql.toString()).isEqualTo(" `name` = ? AND `age` BETWEEN ? AND ?");
  }

  @Test
  void composite() {
    NestedQueryCondition condition = QueryCondition.nested(
            QueryCondition.equalsTo("name", "TODAY")
                    .or(QueryCondition.equalsTo("age", 10))
    );

    StringBuilder sql = new StringBuilder();
    assertThat(condition.render(sql)).isTrue();
    assertThat(sql.toString()).isEqualTo(" ( `name` = ? OR `age` = ? )");
    //

    condition.and(
            QueryCondition.nested(
                    QueryCondition.equalsTo("gender", Gender.MALE)
                            .and(QueryCondition.of("email", Operator.PREFIX_LIKE, "taketoday"))
            )
    );

    sql = new StringBuilder();
    assertThat(condition.render(sql)).isTrue();
    assertThat(sql.toString()).isEqualTo(" ( `name` = ? OR `age` = ? ) AND ( `gender` = ? AND `email` like concat(?, '%') )");

    //

    condition.and(
            QueryCondition.nested(
                    QueryCondition.equalsTo("gender", Gender.MALE)
                            .and(QueryCondition.of("email", Operator.PREFIX_LIKE, "taketoday")
                                    .and(QueryCondition.nested(
                                                    QueryCondition.equalsTo("name", "TODAY")
                                                            .or(QueryCondition.equalsTo("age", 10))
                                            )
                                    )
                            )

            )

    );

    sql = new StringBuilder();
    assertThat(condition.render(sql)).isTrue();
    assertThat(sql.toString()).isEqualTo(" ( `name` = ? OR `age` = ? ) AND ( `gender` = ? AND `email` like concat(?, '%') AND ( `name` = ? OR `age` = ? ) )");

  }

  @Test
  void andExprShouldAssignOnce() {
    var condition = QueryCondition.equalsTo("gender", Gender.MALE)
            .and(QueryCondition.of("email", Operator.PREFIX_LIKE, "taketoday"))
            .and(QueryCondition.nested(
                            QueryCondition.equalsTo("name", "TODAY")
                                    .or(QueryCondition.equalsTo("age", 10))
                    )
            );

    StringBuilder sql = new StringBuilder();
    assertThat(condition.render(sql)).isTrue();
    assertThat(sql.toString()).isEqualTo(" `gender` = ? AND ( `name` = ? OR `age` = ? )");
  }

  @Test
  void setParameter() throws SQLException {
    PreparedStatement statement = Mockito.mock(PreparedStatement.class);

    QueryCondition condition = QueryCondition.nested(
            QueryCondition.equalsTo("name", "TODAY").or(QueryCondition.equalsTo("age", 10))
    ).and(QueryCondition.nested(
                    QueryCondition.equalsTo("gender", Gender.MALE)
                            .and(
                                    QueryCondition.of("email", Operator.PREFIX_LIKE, "taketoday")
                                            .and(
                                                    QueryCondition.equalsTo("name", "TODAY")
                                                            .or(QueryCondition.equalsTo("name", "TODAY"))
                                            )
                            )
            ).and(
                    QueryCondition.equalsTo("name", "TODAY7")
                            .or(QueryCondition.equalsTo("name", "TODAY8"))
            )
    );

    StringBuilder sql = new StringBuilder();
    condition.render(sql);
    System.out.println(sql);

    condition.setParameter(statement);

    InOrder inOrder = Mockito.inOrder(statement);
    inOrder.verify(statement).setObject(1, "TODAY");
    inOrder.verify(statement).setObject(2, 10);
    inOrder.verify(statement).setObject(3, Gender.MALE);
    inOrder.verify(statement).setObject(4, "taketoday");
    inOrder.verify(statement).setObject(5, "TODAY");
    inOrder.verify(statement).setObject(6, "TODAY");

    inOrder.verify(statement).setObject(7, "TODAY7");
    inOrder.verify(statement).setObject(8, "TODAY8");

  }

}