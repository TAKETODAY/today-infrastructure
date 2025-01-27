/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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
    assertThat(strategy.resolve(number, 2)).isNull();
  }

  @Test
  void trimLike() {
    assertTrim(trimLike);

    var condition = strategy.resolve(trimLike, " \n f");
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
    assertThat(strategy.resolve(notTrimLike, " ")).isNull();
    assertThat(strategy.resolve(notTrimLike, "")).isNull();
    assertThat(strategy.resolve(notTrimLike, " fd ")).isNotNull();

    var condition = strategy.resolve(notTrimLike, " f ");
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

    var condition = strategy.resolve(trimPrefixLike, " \n f");
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
    assertThat(strategy.resolve(notTrimPrefixLike, " ")).isNull();
    assertThat(strategy.resolve(notTrimPrefixLike, "")).isNull();
    assertThat(strategy.resolve(notTrimPrefixLike, " fd ")).isNotNull();

    var condition = strategy.resolve(notTrimPrefixLike, " f ");
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

    var condition = strategy.resolve(trimSuffixLike, " \n f");
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
    assertThat(strategy.resolve(notTrimSuffixLike, " ")).isNull();
    assertThat(strategy.resolve(notTrimSuffixLike, "")).isNull();
    assertThat(strategy.resolve(notTrimSuffixLike, " fd ")).isNotNull();

    var condition = strategy.resolve(notTrimSuffixLike, " f ");
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
    assertThat(strategy.resolve(numberLike, 1)).isNull();
  }

  @Test
  void column() {
    var condition = strategy.resolve(column, " f");
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
    assertThat(strategy.resolve(trimLike, "    ")).isNull();
    assertThat(strategy.resolve(trimLike, " \n ")).isNull();
    assertThat(strategy.resolve(trimLike, " \t ")).isNull();
    assertThat(strategy.resolve(trimLike, " \r ")).isNull();
    assertThat(strategy.resolve(trimLike, "\r")).isNull();
    assertThat(strategy.resolve(trimLike, "\n")).isNull();
    assertThat(strategy.resolve(trimLike, "\t")).isNull();
    assertThat(strategy.resolve(trimLike, "\t \n \r")).isNull();
    assertThat(strategy.resolve(trimLike, "\t\n\r")).isNull();
    assertThat(strategy.resolve(trimLike, " \t\n\r")).isNull();

    assertThat(strategy.resolve(trimLike, "f ")).isNotNull();
    assertThat(strategy.resolve(trimLike, " f")).isNotNull();
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