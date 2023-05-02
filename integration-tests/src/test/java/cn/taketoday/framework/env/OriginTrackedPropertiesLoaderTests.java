/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.env;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.framework.env.OriginTrackedPropertiesLoader.Document;
import cn.taketoday.origin.OriginTrackedValue;
import cn.taketoday.origin.TextResourceOrigin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link OriginTrackedPropertiesLoader}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class OriginTrackedPropertiesLoaderTests {

  private ClassPathResource resource;

  private List<Document> documents;

  @BeforeEach
  void setUp() throws Exception {
    String path = "test-properties.properties";
    this.resource = new ClassPathResource(path, getClass());
    this.documents = new OriginTrackedPropertiesLoader(this.resource).load();
  }

  @Test
  void compareToJavaProperties() throws Exception {
    Properties java = PropertiesUtils.loadProperties(this.resource);
    Properties ours = new Properties();
    new OriginTrackedPropertiesLoader(this.resource).load(false)
            .get(0)
            .asMap()
            .forEach((k, v) -> ours.put(k, v.getValue()));
    assertThat(ours).isEqualTo(java);
  }

  @Test
  void getSimpleProperty() {
    OriginTrackedValue value = getFromFirst("test");
    assertThat(getValue(value)).isEqualTo("properties");
    assertThat(getLocation(value)).isEqualTo("11:6");
  }

  @Test
  void getSimplePropertyWithColonSeparator() {
    OriginTrackedValue value = getFromFirst("test-colon-separator");
    assertThat(getValue(value)).isEqualTo("my-property");
    assertThat(getLocation(value)).isEqualTo("15:23");
  }

  @Test
  void getPropertyWithSeparatorSurroundedBySpaces() {
    OriginTrackedValue value = getFromFirst("blah");
    assertThat(getValue(value)).isEqualTo("hello world");
    assertThat(getLocation(value)).isEqualTo("2:12");
  }

  @Test
  void getUnicodeProperty() {
    OriginTrackedValue value = getFromFirst("test-unicode");
    assertThat(getValue(value)).isEqualTo("properties&test");
    assertThat(getLocation(value)).isEqualTo("12:14");
  }

  @Test
  void getMalformedUnicodeProperty() {
    // gh-12716
    ClassPathResource resource = new ClassPathResource("test-properties-malformed-unicode.properties", getClass());
    assertThatIllegalStateException().isThrownBy(() -> new OriginTrackedPropertiesLoader(resource).load())
            .withMessageContaining("Malformed \\uxxxx encoding");
  }

  @Test
  void getEscapedProperty() {
    OriginTrackedValue value = getFromFirst("test=property");
    assertThat(getValue(value)).isEqualTo("helloworld");
    assertThat(getLocation(value)).isEqualTo("14:15");
  }

  @Test
  void getPropertyWithTab() {
    OriginTrackedValue value = getFromFirst("test-tab-property");
    assertThat(getValue(value)).isEqualTo("foo\tbar");
    assertThat(getLocation(value)).isEqualTo("16:19");
  }

  @Test
  void getPropertyWithBang() {
    OriginTrackedValue value = getFromFirst("test-bang-property");
    assertThat(getValue(value)).isEqualTo("foo!");
    assertThat(getLocation(value)).isEqualTo("34:20");
  }

  @Test
  void getPropertyWithValueComment() {
    OriginTrackedValue value = getFromFirst("test-property-value-comment");
    assertThat(getValue(value)).isEqualTo("foo !bar #foo");
    assertThat(getLocation(value)).isEqualTo("36:29");
  }

  @Test
  void getPropertyWithMultilineImmediateBang() {
    OriginTrackedValue value = getFromFirst("test-multiline-immediate-bang");
    assertThat(getValue(value)).isEqualTo("!foo");
    assertThat(getLocation(value)).isEqualTo("39:1");
  }

  @Test
  void getPropertyWithCarriageReturn() {
    OriginTrackedValue value = getFromFirst("test-return-property");
    assertThat(getValue(value)).isEqualTo("foo\rbar");
    assertThat(getLocation(value)).isEqualTo("17:22");
  }

  @Test
  void getPropertyWithNewLine() {
    OriginTrackedValue value = getFromFirst("test-newline-property");
    assertThat(getValue(value)).isEqualTo("foo\nbar");
    assertThat(getLocation(value)).isEqualTo("18:23");
  }

  @Test
  void getPropertyWithFormFeed() {
    OriginTrackedValue value = getFromFirst("test-form-feed-property");
    assertThat(getValue(value)).isEqualTo("foo\fbar");
    assertThat(getLocation(value)).isEqualTo("19:25");
  }

  @Test
  void getPropertyWithWhiteSpace() {
    OriginTrackedValue value = getFromFirst("test-whitespace-property");
    assertThat(getValue(value)).isEqualTo("foo   bar");
    assertThat(getLocation(value)).isEqualTo("20:32");
  }

  @Test
  void getCommentedOutPropertyShouldBeNull() {
    assertThat(getFromFirst("commented-property")).isNull();
    assertThat(getFromFirst("#commented-property")).isNull();
    assertThat(getFromFirst("commented-two")).isNull();
    assertThat(getFromFirst("!commented-two")).isNull();
  }

  @Test
  void getMultiline() {
    OriginTrackedValue value = getFromFirst("test-multiline");
    assertThat(getValue(value)).isEqualTo("ab\\c");
    assertThat(getLocation(value)).isEqualTo("21:17");
  }

  @Test
  void getImmediateMultiline() {
    OriginTrackedValue value = getFromFirst("test-multiline-immediate");
    assertThat(getValue(value)).isEqualTo("foo");
    assertThat(getLocation(value)).isEqualTo("32:1");
  }

  @Test
  void loadWhenMultiDocumentWithPoundPrefixAndWithoutWhitespaceLoadsMultiDoc() throws IOException {
    String content = "a=a\n#---\nb=b";
    List<Document> loaded = new OriginTrackedPropertiesLoader(new ByteArrayResource(content.getBytes())).load();
    assertThat(loaded).hasSize(2);
  }

  @Test
  void loadWhenMultiDocumentWithExclamationPrefixAndWithoutWhitespaceLoadsMultiDoc() throws IOException {
    String content = "a=a\n!---\nb=b";
    List<Document> loaded = new OriginTrackedPropertiesLoader(new ByteArrayResource(content.getBytes())).load();
    assertThat(loaded).hasSize(2);
  }

  @Test
  void loadWhenMultiDocumentWithPoundPrefixAndLeadingWhitespaceLoadsSingleDoc() throws IOException {
    String content = "a=a\n \t#---\nb=b";
    List<Document> loaded = new OriginTrackedPropertiesLoader(new ByteArrayResource(content.getBytes())).load();
    assertThat(loaded).hasSize(1);
  }

  @Test
  void loadWhenMultiDocumentWithExclamationPrefixAndLeadingWhitespaceLoadsSingleDoc() throws IOException {
    String content = "a=a\n \t!---\nb=b";
    List<Document> loaded = new OriginTrackedPropertiesLoader(new ByteArrayResource(content.getBytes())).load();
    assertThat(loaded).hasSize(1);
  }

  @Test
  void loadWhenMultiDocumentWithPoundPrefixAndTrailingWhitespaceLoadsMultiDoc() throws IOException {
    String content = "a=a\n#--- \t \nb=b";
    List<Document> loaded = new OriginTrackedPropertiesLoader(new ByteArrayResource(content.getBytes())).load();
    assertThat(loaded).hasSize(2);
  }

  @Test
  void loadWhenMultiDocumentWithExclamationPrefixAndTrailingWhitespaceLoadsMultiDoc() throws IOException {
    String content = "a=a\n!--- \t \nb=b";
    List<Document> loaded = new OriginTrackedPropertiesLoader(new ByteArrayResource(content.getBytes())).load();
    assertThat(loaded).hasSize(2);
  }

  @Test
  void loadWhenMultiDocumentWithPoundPrefixAndTrailingCharsLoadsSingleDoc() throws IOException {
    String content = "a=a\n#--- \tcomment\nb=b";
    List<Document> loaded = new OriginTrackedPropertiesLoader(new ByteArrayResource(content.getBytes())).load();
    assertThat(loaded).hasSize(1);
  }

  @Test
  void loadWhenMultiDocumentWithExclamationPrefixAndTrailingCharsLoadsSingleDoc() throws IOException {
    String content = "a=a\n!--- \tcomment\nb=b";
    List<Document> loaded = new OriginTrackedPropertiesLoader(new ByteArrayResource(content.getBytes())).load();
    assertThat(loaded).hasSize(1);
  }

  @Test
  void loadWhenMultiDocumentSeparatorPrefixDifferentFromCommentPrefixLoadsMultiDoc() throws IOException {
    String[] contents = new String[] { "a=a\n# comment\n!---\nb=b", "a=a\n! comment\n#---\nb=b" };
    for (String content : contents) {
      List<Document> loaded = new OriginTrackedPropertiesLoader(new ByteArrayResource(content.getBytes())).load();
      assertThat(loaded).hasSize(2);
    }
  }

  @Test
  void getPropertyWithWhitespaceAfterKey() {
    OriginTrackedValue value = getFromFirst("bar");
    assertThat(getValue(value)).isEqualTo("foo=baz");
    assertThat(getLocation(value)).isEqualTo("3:7");
  }

  @Test
  void getPropertyWithSpaceSeparator() {
    OriginTrackedValue value = getFromFirst("hello");
    assertThat(getValue(value)).isEqualTo("world");
    assertThat(getLocation(value)).isEqualTo("4:9");
  }

  @Test
  void getPropertyWithBackslashEscaped() {
    OriginTrackedValue value = getFromFirst("proper\\ty");
    assertThat(getValue(value)).isEqualTo("test");
    assertThat(getLocation(value)).isEqualTo("5:11");
  }

  @Test
  void getPropertyWithEmptyValue() {
    OriginTrackedValue value = getFromFirst("foo");
    assertThat(getValue(value)).isEqualTo("");
    assertThat(getLocation(value)).isEqualTo("7:0");
  }

  @Test
  void getPropertyWithBackslashEscapedInValue() {
    OriginTrackedValue value = getFromFirst("bat");
    assertThat(getValue(value)).isEqualTo("a\\");
    assertThat(getLocation(value)).isEqualTo("7:7");
  }

  @Test
  void getPropertyWithSeparatorInValue() {
    OriginTrackedValue value = getFromFirst("bling");
    assertThat(getValue(value)).isEqualTo("a=b");
    assertThat(getLocation(value)).isEqualTo("8:9");
  }

  @Test
  void getListProperty() {
    OriginTrackedValue apple = getFromFirst("foods[0]");
    assertThat(getValue(apple)).isEqualTo("Apple");
    assertThat(getLocation(apple)).isEqualTo("24:9");
    OriginTrackedValue orange = getFromFirst("foods[1]");
    assertThat(getValue(orange)).isEqualTo("Orange");
    assertThat(getLocation(orange)).isEqualTo("25:1");
    OriginTrackedValue strawberry = getFromFirst("foods[2]");
    assertThat(getValue(strawberry)).isEqualTo("Strawberry");
    assertThat(getLocation(strawberry)).isEqualTo("26:1");
    OriginTrackedValue mango = getFromFirst("foods[3]");
    assertThat(getValue(mango)).isEqualTo("Mango");
    assertThat(getLocation(mango)).isEqualTo("27:1");
  }

  @Test
  void getPropertyWithISO88591Character() {
    OriginTrackedValue value = getFromFirst("test-iso8859-1-chars");
    assertThat(getValue(value)).isEqualTo("æ×ÈÅÞßáñÀÿ");
  }

  @Test
  void getPropertyWithTrailingSpace() {
    OriginTrackedValue value = getFromFirst("test-with-trailing-space");
    assertThat(getValue(value)).isEqualTo("trailing ");
  }

  @Test
  void getPropertyWithEscapedTrailingSpace() {
    OriginTrackedValue value = getFromFirst("test-with-escaped-trailing-space");
    assertThat(getValue(value)).isEqualTo("trailing ");
  }

  @Test
  void existingCommentsAreNotTreatedAsMultiDoc() throws Exception {
    this.resource = new ClassPathResource("existing-non-multi-document.properties", getClass());
    this.documents = new OriginTrackedPropertiesLoader(this.resource).load();
    assertThat(this.documents).hasSize(1);
  }

  @Test
  void getPropertyAfterPoundCharacter() {
    OriginTrackedValue value = getFromFirst("test-line-after-empty-pound");
    assertThat(getValue(value)).isEqualTo("abc");
  }

  private OriginTrackedValue getFromFirst(String key) {
    return this.documents.get(0).asMap().get(key);
  }

  private Object getValue(OriginTrackedValue value) {
    return (value != null) ? value.getValue() : null;
  }

  private String getLocation(OriginTrackedValue value) {
    if (value == null) {
      return null;
    }
    return ((TextResourceOrigin) value.getOrigin()).getLocation().toString();
  }

}
