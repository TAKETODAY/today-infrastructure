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

package infra.persistence.query;

import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;

import infra.lang.Constant;
import infra.persistence.Identifier;
import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class SubqueryConditionTests {

  private final Platform generic = Platform.generic();

  private final Platform mysql = Platform.mysql();

  private final Identifier targetId = Identifier.parse("id");

  private final Identifier targetCode = Identifier.parse("code");

  private final Identifier junctionTable = Identifier.parse("article_label");

  private final Identifier sourceColumn = Identifier.parse("article_id");

  private final String select = "label_id";

  private final Object value = 42L;

  @Test
  void shouldRenderInSubquery() {
    var condition = new SubqueryCondition(targetId, false, Constant.DEFAULT_NONE, select,
            junctionTable, sourceColumn, value, null);

    StringBuilder buf = new StringBuilder();
    condition.render(generic, buf);

    assertThat(buf.toString()).isEqualTo(
            "id IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void shouldRenderNotInSubquery() {
    var condition = new SubqueryCondition(targetId, true, Constant.DEFAULT_NONE, select,
            junctionTable, sourceColumn, value, null);

    StringBuilder buf = new StringBuilder();
    condition.render(generic, buf);

    assertThat(buf.toString()).isEqualTo(
            "id NOT IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void shouldRenderCustomOperatorSubquery() {
    var condition = new SubqueryCondition(targetId, false, ">", "MAX(score)",
            Identifier.parse("exam_result"), Identifier.parse("student_id"), value, null);

    StringBuilder buf = new StringBuilder();
    condition.render(generic, buf);

    assertThat(buf.toString()).isEqualTo(
            "id > (SELECT MAX(score) FROM exam_result WHERE student_id = ?)");
  }

  @Test
  void shouldRenderWithCustomTargetColumn() {
    var condition = new SubqueryCondition(targetCode, false, Constant.DEFAULT_NONE, select,
            junctionTable, sourceColumn, value, null);

    StringBuilder buf = new StringBuilder();
    condition.render(generic, buf);

    assertThat(buf.toString()).isEqualTo(
            "code IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void shouldQuoteIdentifiersWithMysql() {
    var quotedTarget = new infra.persistence.Identifier("id", true);
    var quotedTable = new infra.persistence.Identifier("article_label", true);
    var quotedSource = new infra.persistence.Identifier("article_id", true);
    var condition = new SubqueryCondition(quotedTarget, false, Constant.DEFAULT_NONE, select,
            quotedTable, quotedSource, value, null);

    StringBuilder buf = new StringBuilder();
    condition.render(mysql, buf);

    assertThat(buf.toString()).isEqualTo(
            "`id` IN (SELECT label_id FROM `article_label` WHERE `article_id` = ?)");
  }

  @Test
  void shouldRenderWithQuotedTargetColumn() {
    var quotedTarget = Identifier.parse("\"target_id\"");
    var condition = new SubqueryCondition(quotedTarget, false, Constant.DEFAULT_NONE, select,
            junctionTable, sourceColumn, value, null);

    StringBuilder buf = new StringBuilder();
    condition.render(generic, buf);

    assertThat(buf.toString()).isEqualTo(
            "\"target_id\" IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void shouldBindParameter() throws Exception {
    var entityProperty = mock(infra.persistence.EntityProperty.class);
    var condition = new SubqueryCondition(targetId, false, Constant.DEFAULT_NONE, select,
            junctionTable, sourceColumn, value, entityProperty);

    PreparedStatement ps = mock(PreparedStatement.class);
    int next = condition.setParameter(ps, 1);

    assertThat(next).isEqualTo(2);
    verify(entityProperty).setParameter(ps, 1, 42L);
  }

}
