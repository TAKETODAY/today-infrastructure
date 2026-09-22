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

package infra.persistence.support;

import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;

import infra.persistence.Condition;
import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.IllegalEntityException;
import infra.persistence.ValueNormalizer;
import infra.persistence.annotation.Column;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.Id;
import infra.persistence.annotation.Subquery;
import infra.persistence.annotation.Table;
import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies {@link SubqueryConditionStrategy} resolves the junction table (explicit
 * {@code fromTable} or via {@code from} entity), the target column (ID by default
 * or {@code target}), the {@code WHERE} column, the {@code IN}/{@code NOT IN}
 * operator, and error/decline cases.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class SubqueryConditionStrategyTests {

  final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();

  final SubqueryConditionStrategy strategy = new SubqueryConditionStrategy(metadataFactory);

  final Platform generic = Platform.generic();

  @Test
  void shouldRenderInFromTable() {
    Condition condition = resolve(FromTableQuery.class, "articleId", 42L);

    assertThat(render(condition)).isEqualTo(
            "id IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void shouldRenderInFromEntity() {
    Condition condition = resolve(FromEntityQuery.class, "articleId", 42L);

    assertThat(render(condition)).isEqualTo(
            "id IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void shouldRenderNotIn() {
    Condition condition = resolve(NegativeQuery.class, "articleId", 42L);

    assertThat(render(condition)).isEqualTo(
            "id NOT IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void shouldRenderCustomTargetColumn() {
    Condition condition = resolve(TargetQuery.class, "articleId", 42L);

    assertThat(render(condition)).isEqualTo(
            "code IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void shouldResolveTargetViaMappedColumn() {
    Condition condition = resolve(RenamedTargetQuery.class, "articleId", 42L);

    assertThat(render(condition)).isEqualTo(
            "display_code IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void shouldFallBackToRawTargetWhenUnknown() {
    Condition condition = resolve(UnknownTargetQuery.class, "articleId", 42L);

    assertThat(render(condition)).isEqualTo(
            "unknown_col IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void shouldRenderCustomSelectAndTable() {
    Condition condition = resolve(SelectQuery.class, "articleId", 42L);

    assertThat(render(condition)).isEqualTo(
            "id IN (SELECT label_id FROM custom_junction WHERE article_id = ?)");
  }

  @Test
  void whereOverrideShouldDriveSourceColumn() {
    Condition condition = resolve(WhereOverrideQuery.class, "articleId", 42L);

    assertThat(render(condition)).isEqualTo(
            "id IN (SELECT label_id FROM article_label WHERE post_id = ?)");
  }

  @Test
  void shouldQuoteTargetColumnPerPlatform() {
    Condition condition = resolve(QuotedTargetQuery.class, "articleId", 42L);

    StringBuilder mysql = new StringBuilder();
    condition.render(Platform.mysql(), mysql);
    assertThat(mysql.toString()).isEqualTo(
            "`code` IN (SELECT label_id FROM article_label WHERE article_id = ?)");

    StringBuilder ansi = new StringBuilder();
    condition.render(generic, ansi);
    assertThat(ansi.toString()).isEqualTo(
            "\"code\" IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void shouldBindParameter() throws Exception {
    Condition condition = resolve(FromTableQuery.class, "articleId", 42L);

    PreparedStatement ps = mock(PreparedStatement.class);
    int next = condition.setParameter(ps, 3);

    assertThat(next).isEqualTo(4);
    verify(ps).setLong(3, 42L);
  }

  @Test
  void noSubqueryAnnotationDeclines() {
    assertThat(resolveCondition(PlainQuery.class, "articleId", 42L)).isNull();
  }

  @Test
  void noEntityRefDeclines() {
    assertThat(resolveCondition(NoEntityRefQuery.class, "articleId", 42L)).isNull();
  }

  @Test
  void missingTableAndFromThrows() {
    assertThatExceptionOfType(IllegalEntityException.class)
            .isThrownBy(() -> resolveCondition(NoTableQuery.class, "articleId", 42L))
            .withMessageContaining("fromTable");
  }

  private Condition resolve(Class<?> queryClass, String propertyName, Object value) {
    Condition condition = resolveCondition(queryClass, propertyName, value);
    assertThat(condition).isNotNull();
    return condition;
  }

  private Condition resolveCondition(Class<?> queryClass, String propertyName, Object value) {
    EntityMetadata metadata = metadataFactory.getEntityMetadata(queryClass);
    EntityProperty property = metadata.findProperty(propertyName);
    assertThat(property).isNotNull();
    return strategy.resolve(metadata, property, value, ValueNormalizer.DEFAULT);
  }

  private String render(Condition condition) {
    StringBuilder buf = new StringBuilder();
    condition.render(generic, buf);
    return buf.toString();
  }

  @EntityRef(Label.class)
  static class FromTableQuery {

    @Subquery(select = "label_id", fromTable = "article_label")
    public final Long articleId;

    FromTableQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class FromEntityQuery {

    @Subquery(select = "label_id", from = ArticleLabel.class)
    public final Long articleId;

    FromEntityQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class NegativeQuery {

    @Subquery(select = "label_id", fromTable = "article_label", negative = true)
    public final Long articleId;

    NegativeQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class TargetQuery {

    @Subquery(select = "label_id", fromTable = "article_label", target = "code")
    public final Long articleId;

    TargetQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class QuotedTargetQuery {

    @Subquery(select = "label_id", fromTable = "article_label", target = "`code`")
    public final Long articleId;

    QuotedTargetQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class RenamedTargetQuery {

    @Subquery(select = "label_id", fromTable = "article_label", target = "displayCode")
    public final Long articleId;

    RenamedTargetQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class UnknownTargetQuery {

    @Subquery(select = "label_id", fromTable = "article_label", target = "unknown_col")
    public final Long articleId;

    UnknownTargetQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class SelectQuery {

    @Subquery(select = "label_id", fromTable = "custom_junction")
    public final Long articleId;

    SelectQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class WhereOverrideQuery {

    @Subquery(select = "label_id", fromTable = "article_label", where = "post_id")
    public final Long articleId;

    WhereOverrideQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class PlainQuery {

    public final Long articleId;

    PlainQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  static class NoEntityRefQuery {

    @Subquery(select = "label_id", fromTable = "article_label")
    public final Long articleId;

    NoEntityRefQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class NoTableQuery {

    @Subquery(select = "label_id")
    public final Long articleId;

    NoTableQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @Table("label")
  static class Label {

    @Id
    public Long id;

    public String name;

    public String code;

    @Column("display_code")
    public String displayCode;

  }

  @Table("article_label")
  static class ArticleLabel {

    public Long labelId;

    public Long articleId;

  }

}