/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.persistence.sql;

import org.junit.jupiter.api.Test;

import infra.persistence.Identifier;
import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class MutationStatementRenderingTests {

  private final Platform platform = Platform.generic();

  @Test
  void deleteRendersKeyNullnessAndVersionPredicatesInOrder() {
    var delete = new Delete("sessions").setComment("remove expired sessions")
            .addColumnRestriction("tenant_id", null, "session_id")
            .addColumnIsNullRestriction("renewed_at").addColumnIsNotNullRestriction("expires_at")
            .setVersionColumnName("version");

    assertThat(delete.toStatementString(platform)).isEqualTo("/* remove expired sessions */ DELETE FROM sessions"
            + " WHERE tenant_id = ? AND session_id = ? AND renewed_at is null AND expires_at is not null AND version = ?");
    assertThat(new Delete("staging").setVersionColumnName(null).toStatementString(platform)).isEqualTo("DELETE FROM staging");
  }

  @Test
  void updateRendersDialectQuotedIdentifiersAndKeepsAssignmentsInOrder() {
    var table = Identifier.parse("`audit`.`entry`");
    var update = new Update(table).setComment("archive matched entries")
            .addAssignments("`state`", "`reason`").addAssignment(Identifier.parse("`actor`"))
            .addAssignment("`version`", "version + 1")
            .addRestriction("`tenant_id`", null, "`id`").addRestriction("`version`", "?")
            .addColumnIsNullRestriction("`archived_at`").addColumnIsNotNullRestriction("`created_at`");

    assertThat(update.getTableName()).isEqualTo(table);
    assertThat(update.getRestrictions()).hasSize(5);
    assertThat(update.toStatementString(Platform.mysql())).isEqualTo("/* archive matched entries */ UPDATE `audit`.`entry`"
            + " set `state`=?, `reason`=?, `actor`=?, `version`=version + 1 WHERE `tenant_id` = ? AND `id` = ?"
            + " AND `version` = ? AND `archived_at` is null AND `created_at` is not null");
  }

  @Test
  void insertSelectRendersExplicitTargetColumnsAndSourceQuery() {
    var select = new Select().setSelectClause("id, tenant_id, amount").setFromClause("orders").setWhereClause("closed_at is not null");
    var insert = new InsertSelect().setTableName("order_archive").setComment("archive closed orders")
            .addColumn("id").addColumns(new String[] { "tenant_id", "amount" }).setSelect(select);

    assertThat(insert.toStatementString(platform)).isEqualTo("/* archive closed orders */ INSERT INTO order_archive (id, tenant_id, amount)"
            + " SELECT id, tenant_id, amount FROM orders WHERE closed_at is not null");
    assertThat(new InsertSelect().setTableName("order_archive").setSelect(select).toStatementString(platform))
            .isEqualTo("INSERT INTO order_archive SELECT id, tenant_id, amount FROM orders WHERE closed_at is not null");
  }

  @Test
  void insertSelectRequiresBothTargetAndSource() {
    assertThatIllegalStateException().isThrownBy(() -> new InsertSelect().setTableName("order_archive").toStatementString(platform))
            .withMessageContaining("no select defined");
    var select = new Select().setSelectClause("id").setFromClause("orders");
    assertThatIllegalStateException().isThrownBy(() -> new InsertSelect().setSelect(select).toStatementString(platform))
            .withMessageContaining("no table name defined");
  }

}
