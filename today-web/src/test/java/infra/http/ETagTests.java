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

package infra.http;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 12:44
 */
class ETagTests {

  @Test
  void createETagWithTagAndWeakValidation() {
    ETag eTag = new ETag("12345", true);
    assertThat(eTag.tag()).isEqualTo("12345");
    assertThat(eTag.weak()).isTrue();
    assertThat(eTag.isWildcard()).isFalse();
  }

  @Test
  void createETagWithTagAndStrongValidation() {
    ETag eTag = new ETag("12345", false);
    assertThat(eTag.tag()).isEqualTo("12345");
    assertThat(eTag.weak()).isFalse();
    assertThat(eTag.isWildcard()).isFalse();
  }

  @Test
  void isWildcardReturnsFalseForNonWildcardETag() {
    ETag eTag = new ETag("12345", false);
    assertThat(eTag.isWildcard()).isFalse();
  }

  @Test
  void compareStrongETagsWithStrongComparison() {
    ETag eTag1 = new ETag("12345", false);
    ETag eTag2 = new ETag("12345", false);
    assertThat(eTag1.compare(eTag2, true)).isTrue();
  }

  @Test
  void compareWeakETagsWithStrongComparison() {
    ETag eTag1 = new ETag("12345", true);
    ETag eTag2 = new ETag("12345", true);
    assertThat(eTag1.compare(eTag2, true)).isFalse();
  }

  @Test
  void compareStrongAndWeakETagsWithStrongComparison() {
    ETag eTag1 = new ETag("12345", false);
    ETag eTag2 = new ETag("12345", true);
    assertThat(eTag1.compare(eTag2, true)).isFalse();
  }

  @Test
  void compareStrongETagsWithWeakComparison() {
    ETag eTag1 = new ETag("12345", false);
    ETag eTag2 = new ETag("12345", false);
    assertThat(eTag1.compare(eTag2, false)).isTrue();
  }

  @Test
  void compareWeakETagsWithWeakComparison() {
    ETag eTag1 = new ETag("12345", true);
    ETag eTag2 = new ETag("12345", true);
    assertThat(eTag1.compare(eTag2, false)).isTrue();
  }

  @Test
  void compareStrongAndWeakETagsWithWeakComparison() {
    ETag eTag1 = new ETag("12345", false);
    ETag eTag2 = new ETag("12345", true);
    assertThat(eTag1.compare(eTag2, false)).isTrue();
  }

  @Test
  void compareETagsWithDifferentTags() {
    ETag eTag1 = new ETag("12345", false);
    ETag eTag2 = new ETag("67890", false);
    assertThat(eTag1.compare(eTag2, true)).isFalse();
    assertThat(eTag1.compare(eTag2, false)).isFalse();
  }

  @Test
  void compareETagWithEmptyTag() {
    ETag eTag1 = new ETag("", false);
    ETag eTag2 = new ETag("12345", false);
    assertThat(eTag1.compare(eTag2, true)).isFalse();
    assertThat(eTag2.compare(eTag1, true)).isFalse();
  }

  @Test
  void equalsWithSameTagAndValidation() {
    ETag eTag1 = new ETag("12345", true);
    ETag eTag2 = new ETag("12345", true);
    assertThat(eTag1).isEqualTo(eTag2);
  }

  @Test
  void equalsWithDifferentTag() {
    ETag eTag1 = new ETag("12345", true);
    ETag eTag2 = new ETag("67890", true);
    assertThat(eTag1).isNotEqualTo(eTag2);
  }

  @Test
  void equalsWithDifferentValidation() {
    ETag eTag1 = new ETag("12345", true);
    ETag eTag2 = new ETag("12345", false);
    assertThat(eTag1).isNotEqualTo(eTag2);
  }

  @Test
  void equalsWithSameObject() {
    ETag eTag = new ETag("12345", true);
    assertThat(eTag).isEqualTo(eTag);
  }

  @Test
  void equalsWithNull() {
    ETag eTag = new ETag("12345", true);
    assertThat(eTag).isNotEqualTo(null);
  }

  @Test
  void hashCodeWithSameTagAndValidation() {
    ETag eTag1 = new ETag("12345", true);
    ETag eTag2 = new ETag("12345", true);
    assertThat(eTag1.hashCode()).isEqualTo(eTag2.hashCode());
  }

  @Test
  void hashCodeWithDifferentTag() {
    ETag eTag1 = new ETag("12345", true);
    ETag eTag2 = new ETag("67890", true);
    assertThat(eTag1.hashCode()).isNotEqualTo(eTag2.hashCode());
  }

  @Test
  void hashCodeWithDifferentValidation() {
    ETag eTag1 = new ETag("12345", true);
    ETag eTag2 = new ETag("12345", false);
    assertThat(eTag1.hashCode()).isNotEqualTo(eTag2.hashCode());
  }

  @Test
  void toStringReturnsFormattedTag() {
    ETag eTag = new ETag("12345", true);
    assertThat(eTag.toString()).isEqualTo("W/\"12345\"");
  }

  @Test
  void formattedTagForWeakETag() {
    ETag eTag = new ETag("12345", true);
    assertThat(eTag.formattedTag()).isEqualTo("W/\"12345\"");
  }

  @Test
  void formattedTagForStrongETag() {
    ETag eTag = new ETag("12345", false);
    assertThat(eTag.formattedTag()).isEqualTo("\"12345\"");
  }

  @Test
  void createFromFormattedWeakETag() {
    ETag eTag = ETag.create("W/\"12345\"");
    assertThat(eTag.tag()).isEqualTo("12345");
    assertThat(eTag.weak()).isTrue();
  }

  @Test
  void createFromFormattedStrongETag() {
    ETag eTag = ETag.create("\"12345\"");
    assertThat(eTag.tag()).isEqualTo("12345");
    assertThat(eTag.weak()).isFalse();
  }

  @Test
  void createFromUnquotedETag() {
    ETag eTag = ETag.create("12345");
    assertThat(eTag.tag()).isEqualTo("12345");
    assertThat(eTag.weak()).isFalse();
  }

  @Test
  void parseIfMatchHeaderWithSingleETag() {
    List<ETag> eTags = ETag.parse("\"12345\"");
    assertThat(eTags).hasSize(1);
    assertThat(eTags.get(0).tag()).isEqualTo("12345");
    assertThat(eTags.get(0).weak()).isFalse();
  }

  @Test
  void parseIfMatchHeaderWithMultipleETags() {
    List<ETag> eTags = ETag.parse("\"12345\", \"67890\", W/\"abcde\"");
    assertThat(eTags).hasSize(3);
    assertThat(eTags.get(0).tag()).isEqualTo("12345");
    assertThat(eTags.get(0).weak()).isFalse();
    assertThat(eTags.get(1).tag()).isEqualTo("67890");
    assertThat(eTags.get(1).weak()).isFalse();
    assertThat(eTags.get(2).tag()).isEqualTo("abcde");
    assertThat(eTags.get(2).weak()).isTrue();
  }

  @Test
  void parseIfMatchHeaderWithWildcard() {
    List<ETag> eTags = ETag.parse("*");
    assertThat(eTags).hasSize(1);
    assertThat(eTags.get(0).isWildcard()).isTrue();
  }

  @Test
  void parseIfMatchHeaderWithWhitespace() {
    List<ETag> eTags = ETag.parse(" \"12345\" , W/\"67890\" ");
    assertThat(eTags).hasSize(2);
    assertThat(eTags.get(0).tag()).isEqualTo("12345");
    assertThat(eTags.get(1).tag()).isEqualTo("67890");
    assertThat(eTags.get(1).weak()).isTrue();
  }

  @Test
  void quoteETagIfNecessaryWithAlreadyQuotedETag() {
    String result = ETag.quoteETagIfNecessary("\"12345\"");
    assertThat(result).isEqualTo("\"12345\"");
  }

  @Test
  void quoteETagIfNecessaryWithWeakQuotedETag() {
    String result = ETag.quoteETagIfNecessary("W/\"12345\"");
    assertThat(result).isEqualTo("W/\"12345\"");
  }

  @Test
  void quoteETagIfNecessaryWithUnquotedETag() {
    String result = ETag.quoteETagIfNecessary("12345");
    assertThat(result).isEqualTo("\"12345\"");
  }

  @Test
  void compareETagWithEmptyOtherTag() {
    ETag eTag1 = new ETag("12345", false);
    ETag eTag2 = new ETag("", false);
    assertThat(eTag1.compare(eTag2, true)).isFalse();
    assertThat(eTag1.compare(eTag2, false)).isFalse();
  }

  @Test
  void compareETagWithBothEmptyTags() {
    ETag eTag1 = new ETag("", false);
    ETag eTag2 = new ETag("", false);
    assertThat(eTag1.compare(eTag2, true)).isFalse();
    assertThat(eTag1.compare(eTag2, false)).isFalse();
  }

  @Test
  void createFromPartiallyQuotedETag() {
    ETag eTag = ETag.create("\"12345");
    assertThat(eTag.tag()).isEqualTo("\"12345");
    assertThat(eTag.weak()).isFalse();
  }

  @Test
  void createFromPartiallyQuotedEndETag() {
    ETag eTag = ETag.create("12345\"");
    assertThat(eTag.tag()).isEqualTo("12345\"");
    assertThat(eTag.weak()).isFalse();
  }

  @Test
  void parseIfNoneMatchHeaderWithSingleETag() {
    List<ETag> eTags = ETag.parse("\"xyzzy\"");
    assertThat(eTags).hasSize(1);
    assertThat(eTags.get(0).tag()).isEqualTo("xyzzy");
    assertThat(eTags.get(0).weak()).isFalse();
  }

  @Test
  void parseIfMatchHeaderWithSingleWeakETag() {
    List<ETag> eTags = ETag.parse("W/\"xyzzy\"");
    assertThat(eTags).hasSize(1);
    assertThat(eTags.get(0).tag()).isEqualTo("xyzzy");
    assertThat(eTags.get(0).weak()).isTrue();
  }

  @Test
  void parseIfMatchHeaderWithMalformedETag() {
    List<ETag> eTags = ETag.parse("\"xyzzy");
    assertThat(eTags).isEmpty();
  }

  @Test
  void parseIfMatchHeaderWithNoETag() {
    List<ETag> eTags = ETag.parse("");
    assertThat(eTags).isEmpty();
  }

  @Test
  void parseIfMatchHeaderWithOnlyComma() {
    List<ETag> eTags = ETag.parse(",");
    assertThat(eTags).isEmpty();
  }

  @Test
  void parseIfMatchHeaderWithTrailingComma() {
    List<ETag> eTags = ETag.parse("\"12345\",");
    assertThat(eTags).hasSize(1);
    assertThat(eTags.get(0).tag()).isEqualTo("12345");
  }

  @Test
  void parseIfMatchHeaderWithLeadingComma() {
    List<ETag> eTags = ETag.parse(",\"12345\"");
    assertThat(eTags).hasSize(1);
    assertThat(eTags.get(0).tag()).isEqualTo("12345");
  }

  @Test
  void quoteETagIfNecessaryWithWeakPartiallyQuotedETag() {
    String result = ETag.quoteETagIfNecessary("W/\"12345");
    assertThat(result).isEqualTo("\"W/\"12345\"");
  }

}