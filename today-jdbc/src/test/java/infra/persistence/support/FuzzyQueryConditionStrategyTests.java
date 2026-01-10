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

import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.Like;
import infra.persistence.PrefixLike;
import infra.persistence.SuffixLike;
import infra.persistence.support.FuzzyQueryConditionStrategy.LikeRestriction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/10/12 19:34
 */
class FuzzyQueryConditionStrategyTests {

  boolean logicalAnd = true;
  
  FuzzyQueryConditionStrategy strategy = new FuzzyQueryConditionStrategy();

  EntityMetadata entityMetadata = new DefaultEntityMetadataFactory().getEntityMetadata(Model.class);

  EntityProperty number = entityMetadata.findProperty("number");
  EntityProperty trimLike = entityMetadata.findProperty("trimLike");
  EntityProperty notTrimLike = entityMetadata.findProperty("notTrimLike");
  EntityProperty trimPrefixLike = entityMetadata.findProperty("trimPrefixLike");
  EntityProperty notTrimPrefixLike = entityMetadata.findProperty("notTrimPrefixLike");
  EntityProperty trimSuffixLike = entityMetadata.findProperty("trimSuffixLike");
  EntityProperty notTrimSuffixLike = entityMetadata.findProperty("notTrimSuffixLike");
  EntityProperty numberLike = entityMetadata.findProperty("numberLike");
  EntityProperty column = entityMetadata.findProperty("column");

  @Test
  void noLikeAnnotation() {
    assertThat(strategy.resolve(logicalAnd, number, 2)).isNull();
  }

  @Test
  void trimLike() {
    assertTrim(trimLike);

    var condition = strategy.resolve(logicalAnd, trimLike, " \n f");
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(trimLike);
    assertThat(condition.value).isEqualTo("%f%");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName).isEqualTo("trim_like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("`trim_like` like ?");
  }

  @Test
  void notTrimLike() {
    assertThat(strategy.resolve(logicalAnd, notTrimLike, " ")).isNull();
    assertThat(strategy.resolve(logicalAnd, notTrimLike, "")).isNull();
    assertThat(strategy.resolve(logicalAnd, notTrimLike, " fd ")).isNotNull();

    var condition = strategy.resolve(logicalAnd, notTrimLike, " f ");
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(notTrimLike);
    assertThat(condition.value).isEqualTo("% f %");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName).isEqualTo("not_trim_like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("`not_trim_like` like ?");
  }

  @Test
  void trimPrefixLike() {
    assertTrim(trimPrefixLike);

    var condition = strategy.resolve(logicalAnd, trimPrefixLike, " \n f");
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(trimPrefixLike);
    assertThat(condition.value).isEqualTo("f%");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName).isEqualTo("trim_prefix_like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("`trim_prefix_like` like ?");
  }

  @Test
  void notTrimPrefixLike() {
    assertThat(strategy.resolve(logicalAnd, notTrimPrefixLike, " ")).isNull();
    assertThat(strategy.resolve(logicalAnd, notTrimPrefixLike, "")).isNull();
    assertThat(strategy.resolve(logicalAnd, notTrimPrefixLike, " fd ")).isNotNull();

    var condition = strategy.resolve(logicalAnd, notTrimPrefixLike, " f ");
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(notTrimPrefixLike);
    assertThat(condition.value).isEqualTo(" f %");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName).isEqualTo("not_trim_prefix_like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("`not_trim_prefix_like` like ?");
  }

  @Test
  void trimSuffixLike() {
    assertTrim(trimSuffixLike);

    var condition = strategy.resolve(logicalAnd, trimSuffixLike, " \n f");
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(trimSuffixLike);
    assertThat(condition.value).isEqualTo("%f");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName).isEqualTo("trim_suffix_like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("`trim_suffix_like` like ?");
  }

  @Test
  void notTrimSuffixLike() {
    assertThat(strategy.resolve(logicalAnd, notTrimSuffixLike, " ")).isNull();
    assertThat(strategy.resolve(logicalAnd, notTrimSuffixLike, "")).isNull();
    assertThat(strategy.resolve(logicalAnd, notTrimSuffixLike, " fd ")).isNotNull();

    var condition = strategy.resolve(logicalAnd, notTrimSuffixLike, " f ");
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(notTrimSuffixLike);
    assertThat(condition.value).isEqualTo("% f ");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName).isEqualTo("not_trim_suffix_like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("`not_trim_suffix_like` like ?");
  }

  @Test
  void numberLike() {
    assertThat(strategy.resolve(logicalAnd, numberLike, 1)).isNull();
  }

  @Test
  void column() {
    var condition = strategy.resolve(logicalAnd, column, " f");
    assertThat(condition).isNotNull();

    assertThat(condition.entityProperty).isSameAs(column);
    assertThat(condition.value).isEqualTo("%f%");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName).isEqualTo("col");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("`col` like ?");
  }

  private static String render(LikeRestriction likeRestriction) {
    StringBuilder sqlBuffer = new StringBuilder();
    likeRestriction.render(sqlBuffer);

    return sqlBuffer.toString();
  }

  private void assertTrim(EntityProperty trimLike) {
    assertThat(strategy.resolve(logicalAnd, trimLike, "    ")).isNull();
    assertThat(strategy.resolve(logicalAnd, trimLike, " \n ")).isNull();
    assertThat(strategy.resolve(logicalAnd, trimLike, " \t ")).isNull();
    assertThat(strategy.resolve(logicalAnd, trimLike, " \r ")).isNull();
    assertThat(strategy.resolve(logicalAnd, trimLike, "\r")).isNull();
    assertThat(strategy.resolve(logicalAnd, trimLike, "\n")).isNull();
    assertThat(strategy.resolve(logicalAnd, trimLike, "\t")).isNull();
    assertThat(strategy.resolve(logicalAnd, trimLike, "\t \n \r")).isNull();
    assertThat(strategy.resolve(logicalAnd, trimLike, "\t\n\r")).isNull();
    assertThat(strategy.resolve(logicalAnd, trimLike, " \t\n\r")).isNull();

    assertThat(strategy.resolve(logicalAnd, trimLike, "f ")).isNotNull();
    assertThat(strategy.resolve(logicalAnd, trimLike, " f")).isNotNull();
  }

  static class Model {

    public int number;

    @Like
    public int numberLike;

    @Like(column = "col")
    public String column;

    @Like
    public String trimLike;

    @Like(trim = false)
    public String notTrimLike;

    @PrefixLike
    public String trimPrefixLike;

    @PrefixLike(trim = false)
    public String notTrimPrefixLike;

    @SuffixLike
    public String trimSuffixLike;

    @SuffixLike(trim = false)
    public String notTrimSuffixLike;

  }

}