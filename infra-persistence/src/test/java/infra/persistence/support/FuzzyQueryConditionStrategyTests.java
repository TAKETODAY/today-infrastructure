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
import infra.persistence.ValueNormalizer;
import infra.persistence.annotation.Like;
import infra.persistence.annotation.PrefixLike;
import infra.persistence.annotation.SuffixLike;
import infra.persistence.annotation.Trim;
import infra.persistence.platform.Platform;

import infra.persistence.support.FuzzyQueryConditionStrategy.LikeRestriction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@link FuzzyQueryConditionStrategy} turns {@code @Like} family
 * values into {@code LIKE} conditions, trimming the value when the property is
 * annotated with {@code @Trim}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/10/12 19:34
 */
class FuzzyQueryConditionStrategyTests {

  FuzzyQueryConditionStrategy strategy = new FuzzyQueryConditionStrategy();

  EntityMetadata entityMetadata = new DefaultEntityMetadataFactory().getEntityMetadata(Model.class);

  EntityProperty number = entityMetadata.findProperty("number");
  EntityProperty like = entityMetadata.findProperty("like");
  EntityProperty trimLike = entityMetadata.findProperty("trimLike");
  EntityProperty prefixLike = entityMetadata.findProperty("prefixLike");
  EntityProperty trimPrefixLike = entityMetadata.findProperty("trimPrefixLike");
  EntityProperty suffixLike = entityMetadata.findProperty("suffixLike");
  EntityProperty trimSuffixLike = entityMetadata.findProperty("trimSuffixLike");
  EntityProperty numberLike = entityMetadata.findProperty("numberLike");
  EntityProperty column = entityMetadata.findProperty("column");

  @Test
  void noLikeAnnotation() {
    assertThat(strategy.resolve(number, 2, ValueNormalizer.DEFAULT)).isNull();
  }

  @Test
  void numberLike() {
    assertThat(strategy.resolve(numberLike, 1, ValueNormalizer.DEFAULT)).isNull();
  }

  @Test
  void like() {
    PropertyCondition condition = (PropertyCondition) strategy.resolve(like, " \n f", ValueNormalizer.DEFAULT);
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(like);
    assertThat(condition.value).isEqualTo("% \n f%");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName.getText()).isEqualTo("like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("like like ?");
  }

  @Test
  void trimLike() {
    var normalizer = ValueNormalizer.DEFAULT;
    assertTrim(trimLike, normalizer);

    PropertyCondition condition = (PropertyCondition) strategy.resolve(trimLike, " f ", normalizer);
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(trimLike);
    assertThat(condition.value).isEqualTo("%f%");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName.getText()).isEqualTo("trim_like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("trim_like like ?");
  }

  @Test
  void prefixLike() {
    PropertyCondition condition = (PropertyCondition) strategy.resolve(prefixLike, " f ", ValueNormalizer.DEFAULT);
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(prefixLike);
    assertThat(condition.value).isEqualTo(" f %");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName.getText()).isEqualTo("prefix_like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("prefix_like like ?");
  }

  @Test
  void trimPrefixLike() {
    var normalizer = ValueNormalizer.DEFAULT;
    assertTrim(trimPrefixLike, normalizer);

    PropertyCondition condition = (PropertyCondition) strategy.resolve(trimPrefixLike, " f ", normalizer);
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(trimPrefixLike);
    assertThat(condition.value).isEqualTo("f%");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName.getText()).isEqualTo("trim_prefix_like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("trim_prefix_like like ?");
  }

  @Test
  void suffixLike() {
    PropertyCondition condition = (PropertyCondition) strategy.resolve(suffixLike, " f ", ValueNormalizer.DEFAULT);
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(suffixLike);
    assertThat(condition.value).isEqualTo("% f ");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName.getText()).isEqualTo("suffix_like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("suffix_like like ?");
  }

  @Test
  void trimSuffixLike() {
    var normalizer = ValueNormalizer.DEFAULT;
    assertTrim(trimSuffixLike, normalizer);

    PropertyCondition condition = (PropertyCondition) strategy.resolve(trimSuffixLike, " f ", normalizer);
    assertThat(condition).isNotNull();
    assertThat(condition.entityProperty).isSameAs(trimSuffixLike);
    assertThat(condition.value).isEqualTo("%f");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName.getText()).isEqualTo("trim_suffix_like");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("trim_suffix_like like ?");
  }

  @Test
  void column() {
    PropertyCondition condition = (PropertyCondition) strategy.resolve(column, " f", ValueNormalizer.DEFAULT);
    assertThat(condition).isNotNull();

    assertThat(condition.entityProperty).isSameAs(column);
    assertThat(condition.value).isEqualTo("% f%");
    assertThat(condition.restriction).isInstanceOf(LikeRestriction.class);

    var likeRestriction = (LikeRestriction) condition.restriction;
    assertThat(likeRestriction.columnName.getText()).isEqualTo("col");

    String string = render(likeRestriction);
    assertThat(string).isEqualTo("col like ?");
  }

  private static String render(LikeRestriction likeRestriction) {
    StringBuilder sqlBuffer = new StringBuilder();
    likeRestriction.render(Platform.mysql(), sqlBuffer);

    return sqlBuffer.toString();
  }

  private void assertTrim(EntityProperty trimLike, ValueNormalizer normalizer) {
    assertThat(strategy.resolve(trimLike, "    ", normalizer)).isNull();
    assertThat(strategy.resolve(trimLike, " \n ", normalizer)).isNull();
    assertThat(strategy.resolve(trimLike, " \t ", normalizer)).isNull();
    assertThat(strategy.resolve(trimLike, " \r ", normalizer)).isNull();
    assertThat(strategy.resolve(trimLike, " f ", normalizer)).isNotNull();
  }

  static class Model {

    public int number;

    @Like
    public int numberLike;

    @Like(column = "col")
    public String column;

    @Like
    public String like;

    @Like
    @Trim
    public String trimLike;

    @PrefixLike
    public String prefixLike;

    @PrefixLike
    @Trim
    public String trimPrefixLike;

    @SuffixLike
    public String suffixLike;

    @SuffixLike
    @Trim
    public String trimSuffixLike;

  }

}
