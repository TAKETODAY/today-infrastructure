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

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.List;

import infra.persistence.annotation.Between;
import infra.persistence.annotation.Column;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.Id;
import infra.persistence.annotation.In;
import infra.persistence.annotation.Like;
import infra.persistence.annotation.NotBetween;
import infra.persistence.annotation.NotIn;
import infra.persistence.annotation.OR;
import infra.persistence.annotation.Subquery;
import infra.persistence.annotation.Table;
import infra.persistence.annotation.Where;
import infra.persistence.annotation.WhereIsNull;
import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Renders the full SQL produced by every {@code @Subquery} configuration and its
 * combinations with other condition annotations, through the real
 * {@link DefaultEntityQueryFactory} pipeline.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class SubqueryQueryRenderingTests {

  final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();

  final DefaultEntityQueryFactory queryFactory = new DefaultEntityQueryFactory(metadataFactory);

  final EntityMetadata labelMetadata = metadataFactory.getEntityMetadata(Label.class);

  final Platform generic = Platform.generic();

  @Test
  void fromTableUsesIdTarget() {
    assertThat(render(new FromTableQuery(42L)))
            .isEqualTo("WHERE id IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void fromEntityResolvesTableName() {
    assertThat(render(new FromEntityQuery(42L)))
            .isEqualTo("WHERE id IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void negativeRendersNotIn() {
    assertThat(render(new NegativeQuery(42L)))
            .isEqualTo("WHERE id NOT IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void targetColumnRenders() {
    assertThat(render(new TargetQuery(42L)))
            .isEqualTo("WHERE code IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void targetResolvesMappedColumn() {
    assertThat(render(new RenamedTargetQuery(42L)))
            .isEqualTo("WHERE display_code IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void unknownTargetFallsBackToRawColumn() {
    assertThat(render(new UnknownTargetQuery(42L)))
            .isEqualTo("WHERE raw_col IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void whereOverrideDrivesSourceColumn() {
    assertThat(render(new WhereOverrideQuery(42L)))
            .isEqualTo("WHERE id IN (SELECT label_id FROM article_label WHERE post_id = ?)");
  }

  @Test
  void customSelectAndTableRender() {
    assertThat(render(new CustomSelectQuery(42L)))
            .isEqualTo("WHERE id IN (SELECT custom_id FROM custom_junction WHERE article_id = ?)");
  }

  @Test
  void negativeWithTargetAndWhere() {
    assertThat(render(new CombinedQuery(42L)))
            .isEqualTo("WHERE code NOT IN (SELECT label_id FROM article_label WHERE post_id = ?)");
  }

  @Test
  void whereAnnotationWinsOverSubquery() {
    assertThat(render(new WhereWinsQuery(42L)))
            .isEqualTo("WHERE status = ?");
  }

  @Test
  void subqueryWinsOverLike() {
    assertThat(render(new SubqueryOverLikeQuery("abc")))
            .isEqualTo("WHERE id IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void subqueryWinsOverIn() {
    assertThat(render(new SubqueryOverInQuery(42L)))
            .isEqualTo("WHERE id IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void twoSubqueriesJoinWithAnd() {
    assertThat(render(new TwoSubqueriesQuery(1L, 2L)))
            .isEqualTo("WHERE id IN (SELECT label_id FROM article_label WHERE article_id = ?)"
                    + " AND id IN (SELECT label_id FROM other_label WHERE article_id = ?)");
  }

  @Test
  void selfQueryWithoutEntityRefUsesOwnId() {
    EntityMetadata articleMetadata = metadataFactory.getEntityMetadata(ArticleQuery.class);
    QueryStatement query = queryFactory.createQuery(new ArticleQuery(42L));

    assertThat(query.render(articleMetadata).toStatementString(generic))
            .contains("WHERE id IN (SELECT label_id FROM article_label WHERE article_id = ?)");
  }

  @Test
  void mysqlQuotesRecursivelyQuotedIdentifiers() {
    QueryStatement query = queryFactory.createQuery(new QuotedQuery(42L));
    EntityMetadata metadata = metadataFactory.getEntityMetadata(QuotedQuery.class);

    assertThat(query.render(metadata).toStatementString(Platform.mysql()))
            .contains("WHERE `id` IN (SELECT label_id FROM `article_label` WHERE `article_id` = ?)");
  }

  @Test
  void bindsArticleIdParameter() throws Exception {
    QueryStatement query = queryFactory.createQuery(new FromTableQuery(42L));

    PreparedStatement statement = mock(PreparedStatement.class);
    query.setParameter(labelMetadata, statement);

    verify(statement).setLong(1, 42L);
  }

  @Test
  void threeSubqueriesJoinWithAnd() {
    assertThat(render(new ThreeSubqueriesQuery(1L, 2L, 3L)))
            .isEqualTo("WHERE id IN (SELECT label_id FROM article_label WHERE article_id = ?)"
                    + " AND id IN (SELECT label_id FROM other_label WHERE other_id = ?)"
                    + " AND code NOT IN (SELECT label_id FROM third_label WHERE third_id = ?)");
  }

  @Test
  void orConnectorBetweenSubqueries() {
    assertThat(render(new OrSubqueriesQuery(1L, 2L)))
            .isEqualTo("WHERE (id IN (SELECT label_id FROM article_label WHERE article_id = ?)"
                    + " OR id IN (SELECT label_id FROM other_label WHERE other_id = ?))");
  }

  @Test
  void mixedAnnotationsAcrossFieldsJoinWithAnd() {
    assertThat(render(new MixedAnnotationsQuery(7L, "active", "ab", 30)))
            .isEqualTo("WHERE id IN (SELECT label_id FROM article_label WHERE article_id = ?)"
                    + " AND status = ? AND name like ? AND age = ?");
  }

  @Test
  void subqueryMixedWithInBetweenAndNullness() {
    assertThat(render(new MultiValueMixQuery(7L, List.of(1, 2, 3), List.of(18, 30))))
            .isEqualTo("WHERE id IN (SELECT label_id FROM article_label WHERE article_id = ?)"
                    + " AND statuses IN (?, ?, ?)"
                    + " AND age BETWEEN ? AND ?"
                    + " AND gender is null");
  }

  @Test
  void subqueryMixedWithNotVariants() {
    assertThat(render(new NotVariantsQuery(7L, List.of(1, 2), List.of(18, 30))))
            .isEqualTo("WHERE id IN (SELECT label_id FROM article_label WHERE article_id = ?)"
                    + " AND excluded NOT IN (?, ?)"
                    + " AND age NOT BETWEEN ? AND ?");
  }

  @Test
  void orConnectorBetweenSubqueryAndLike() {
    assertThat(render(new OrSubqueryWithLikeQuery(7L, "ab")))
            .isEqualTo("WHERE (id IN (SELECT label_id FROM article_label WHERE article_id = ?)"
                    + " OR name like ?)");
  }

  @Test
  void multipleSubqueriesBindInFieldOrder() throws Exception {
    QueryStatement query = queryFactory.createQuery(new ThreeSubqueriesQuery(1L, 2L, 3L));

    PreparedStatement statement = mock(PreparedStatement.class);
    query.setParameter(labelMetadata, statement);

    verify(statement).setLong(1, 1L);
    verify(statement).setLong(2, 2L);
    verify(statement).setLong(3, 3L);
  }

  private String render(Object example) {
    QueryStatement query = queryFactory.createQuery(example);
    String statement = query.render(labelMetadata).toStatementString(generic);
    int where = statement.indexOf("WHERE ");
    assertThat(where).as("no WHERE clause in: %s", statement).isGreaterThanOrEqualTo(0);
    return statement.substring(where);
  }

  // ---------------- query classes ----------------

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
  static class RenamedTargetQuery {

    @Subquery(select = "label_id", fromTable = "article_label", target = "displayCode")
    public final Long articleId;

    RenamedTargetQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class UnknownTargetQuery {

    @Subquery(select = "label_id", fromTable = "article_label", target = "raw_col")
    public final Long articleId;

    UnknownTargetQuery(Long articleId) {
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
  static class CustomSelectQuery {

    @Subquery(select = "custom_id", fromTable = "custom_junction")
    public final Long articleId;

    CustomSelectQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class CombinedQuery {

    @Subquery(select = "label_id", fromTable = "article_label",
            target = "code", where = "post_id", negative = true)
    public final Long articleId;

    CombinedQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class WhereWinsQuery {

    @Where("status = ?")
    @Subquery(select = "label_id", fromTable = "article_label")
    public final Long articleId;

    WhereWinsQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class SubqueryOverLikeQuery {

    @Like
    @Subquery(select = "label_id", fromTable = "article_label")
    public final String articleId;

    SubqueryOverLikeQuery(String articleId) {
      this.articleId = articleId;
    }
  }

  @EntityRef(Label.class)
  static class SubqueryOverInQuery {

    @In
    @Subquery(select = "label_id", fromTable = "article_label")
    public final List<Long> articleId;

    SubqueryOverInQuery(Long articleId) {
      this.articleId = List.of(articleId);
    }
  }

  @EntityRef(Label.class)
  static class TwoSubqueriesQuery {

    @Subquery(select = "label_id", fromTable = "article_label")
    public final Long articleId;

    @Subquery(select = "label_id", fromTable = "other_label", where = "article_id")
    public final Long otherId;

    TwoSubqueriesQuery(Long articleId, Long otherId) {
      this.articleId = articleId;
      this.otherId = otherId;
    }
  }

  @EntityRef(Label.class)
  static class ThreeSubqueriesQuery {

    @Subquery(select = "label_id", fromTable = "article_label")
    public final Long articleId;

    @Subquery(select = "label_id", fromTable = "other_label")
    public final Long otherId;

    @Subquery(select = "label_id", fromTable = "third_label", target = "code", negative = true)
    public final Long thirdId;

    ThreeSubqueriesQuery(Long articleId, Long otherId, Long thirdId) {
      this.articleId = articleId;
      this.otherId = otherId;
      this.thirdId = thirdId;
    }
  }

  @EntityRef(Label.class)
  static class OrSubqueriesQuery {

    @Subquery(select = "label_id", fromTable = "article_label")
    public final Long articleId;

    @OR
    @Subquery(select = "label_id", fromTable = "other_label")
    public final Long otherId;

    OrSubqueriesQuery(Long articleId, Long otherId) {
      this.articleId = articleId;
      this.otherId = otherId;
    }
  }

  @EntityRef(Label.class)
  static class MixedAnnotationsQuery {

    @Subquery(select = "label_id", fromTable = "article_label")
    public final Long articleId;

    @Where("status = ?")
    public final String status;

    @Like
    public final String name;

    public final Integer age;

    MixedAnnotationsQuery(Long articleId, String status, String name, Integer age) {
      this.articleId = articleId;
      this.status = status;
      this.name = name;
      this.age = age;
    }
  }

  @EntityRef(Label.class)
  static class MultiValueMixQuery {

    @Subquery(select = "label_id", fromTable = "article_label")
    public final Long articleId;

    @In
    public final List<Integer> statuses;

    @Between
    public final List<Integer> age;

    @WhereIsNull
    public Integer gender;

    MultiValueMixQuery(Long articleId, List<Integer> statuses, List<Integer> age) {
      this.articleId = articleId;
      this.statuses = statuses;
      this.age = age;
    }
  }

  @EntityRef(Label.class)
  static class NotVariantsQuery {

    @Subquery(select = "label_id", fromTable = "article_label")
    public final Long articleId;

    @NotIn
    public final List<Integer> excluded;

    @NotBetween
    public final List<Integer> age;

    NotVariantsQuery(Long articleId, List<Integer> excluded, List<Integer> age) {
      this.articleId = articleId;
      this.excluded = excluded;
      this.age = age;
    }
  }

  @EntityRef(Label.class)
  static class OrSubqueryWithLikeQuery {

    @Subquery(select = "label_id", fromTable = "article_label")
    public final Long articleId;

    @OR
    @Like
    public final String name;

    OrSubqueryWithLikeQuery(Long articleId, String name) {
      this.articleId = articleId;
      this.name = name;
    }
  }

  @Table("article")
  static class ArticleQuery {

    @Id
    public Long id;

    @Subquery(select = "label_id", fromTable = "article_label")
    public final Long articleId;

    ArticleQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @Table("quoted_article")
  static class QuotedQuery {

    @Id
    @Column("`id`")
    public Long id;

    @Subquery(select = "label_id", fromTable = "`article_label`", where = "`article_id`")
    public final Long articleId;

    QuotedQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  // ---------------- referenced / junction entities ----------------

  @Table("label")
  static class Label implements Serializable {

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