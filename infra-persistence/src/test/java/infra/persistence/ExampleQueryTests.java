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

import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.Id;
import infra.persistence.annotation.Subquery;
import infra.persistence.annotation.Table;
import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 20:33
 */
class ExampleQueryTests {

  @Test
  void shouldCreateExampleQuery() {
    Object example = new Object();
    EntityMetadata exampleMetadata = mock(EntityMetadata.class);

    ExampleQuery query = new ExampleQuery(example, exampleMetadata, List.of());

    assertThat(query).isNotNull();
    assertThat(query.getDescription()).isEqualTo("Query entities with example");
  }

  @Test
  void shouldGetDebugLogMessage() {
    Object example = new Object();
    EntityMetadata exampleMetadata = mock(EntityMetadata.class);

    ExampleQuery query = new ExampleQuery(example, exampleMetadata, List.of());
    Object logMessage = query.getDebugLogMessage();

    assertThat(logMessage).isNotNull();
    assertThat(logMessage.toString()).contains("Query entity using example");
  }

  @Test
  void shouldGenerateAndBindSubquery() throws Exception {
    DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();
    DefaultEntityQueryFactory queryFactory = new DefaultEntityQueryFactory(metadataFactory);
    EntityMetadata labelMetadata = metadataFactory.getEntityMetadata(Label.class);
    QueryStatement query = queryFactory.createQuery(new TagQuery(42L));

    assertThat(query.render(labelMetadata).toStatementString(Platform.generic()))
            .endsWith("FROM label WHERE id IN (SELECT label_id FROM article_label WHERE article_id = ?)");

    PreparedStatement statement = mock(PreparedStatement.class);
    query.setParameter(labelMetadata, statement);

    verify(statement).setLong(1, 42L);
  }

  @EntityRef(Label.class)
  static class TagQuery {

    @Subquery(table = "article_label", select = "label_id")
    public final Long articleId;

    TagQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

  @Table("label")
  static class Label implements Serializable {

    @Id
    public Long id;

    public String name;

  }

  @Table("article_label")
  static class ArticleLabel {

    public Long labelId;

    public Long articleId;

    public ArticleLabel() {
    }

    public ArticleLabel(Long labelId, Long articleId) {
      this.labelId = labelId;
      this.articleId = articleId;
    }

  }

}
