/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Properties;

import static cn.taketoday.util.CollectionUtils.createSortedProperties;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author TODAY 2021/3/9 19:50
 */
public class SortedPropertiesTests {

  @Test
  public void keys() {
    assertKeys(createSortedProps());
  }

  @Test
  public void keysFromPrototype() {
    assertKeys(createSortedPropsFromPrototype());
  }

  @Test
  public void keySet() {
    assertKeySet(createSortedProps());
  }

  @Test
  public void keySetFromPrototype() {
    assertKeySet(createSortedPropsFromPrototype());
  }

  @Test
  public void entrySet() {
    assertEntrySet(createSortedProps());
  }

  @Test
  public void entrySetFromPrototype() {
    assertEntrySet(createSortedPropsFromPrototype());
  }

  @Test
  public void sortsPropertiesUsingOutputStream() throws IOException {
    SortedProperties sortedProperties = createSortedProps();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    sortedProperties.store(baos, "custom comment");

    String[] lines = lines(baos);
    assertThat(lines).hasSize(7);
    assertThat(lines[0]).isEqualTo("#custom comment");
    assertThat(lines[1]).as("timestamp").startsWith("#");

    assertPropsAreSorted(lines);
  }

  @Test
  public void sortsPropertiesUsingWriter() throws IOException {
    SortedProperties sortedProperties = createSortedProps();

    StringWriter writer = new StringWriter();
    sortedProperties.store(writer, "custom comment");

    String[] lines = lines(writer);
    assertThat(lines).hasSize(7);
    assertThat(lines[0]).isEqualTo("#custom comment");
    assertThat(lines[1]).as("timestamp").startsWith("#");

    assertPropsAreSorted(lines);
  }

  @Test
  public void sortsPropertiesAndOmitsCommentsUsingOutputStream() throws IOException {
    SortedProperties sortedProperties = createSortedProps(true);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    sortedProperties.store(baos, "custom comment");

    String[] lines = lines(baos);
    assertThat(lines).hasSize(5);

    assertPropsAreSorted(lines);
  }

  @Test
  public void sortsPropertiesAndOmitsCommentsUsingWriter() throws IOException {
    SortedProperties sortedProperties = createSortedProps(true);

    StringWriter writer = new StringWriter();
    sortedProperties.store(writer, "custom comment");

    String[] lines = lines(writer);
    assertThat(lines).hasSize(5);

    assertPropsAreSorted(lines);
  }

  @Test
  public void storingAsXmlSortsPropertiesAndOmitsComments() throws IOException {
    SortedProperties sortedProperties = createSortedProps(true);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    sortedProperties.storeToXML(baos, "custom comment");

    String[] lines = lines(baos);

    assertThat(lines).isNotEmpty();
    // Leniently match first line due to differences between JDK 8 and JDK 9+.
    String regex = "<\\?xml .*\\?>";
    assertThat(lines[0]).matches(regex);
    assertThat(lines).filteredOn(line -> !line.matches(regex))
            .containsExactly("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">",
                    "<properties>", //
                    "<entry key=\"color\">blue</entry>", //
                    "<entry key=\"fragrance\">sweet</entry>", //
                    "<entry key=\"fruit\">apple</entry>", //
                    "<entry key=\"size\">medium</entry>", //
                    "<entry key=\"vehicle\">car</entry>", //
                    "</properties>" //
            );
  }

  private SortedProperties createSortedProps() {
    return createSortedProps(false);
  }

  private SortedProperties createSortedProps(boolean omitComments) {
    SortedProperties sortedProperties = new SortedProperties(omitComments);
    populateProperties(sortedProperties);
    return sortedProperties;
  }

  private Properties createSortedPropsFromPrototype() {
    Properties properties = new Properties();
    populateProperties(properties);
    return createSortedProperties(properties, false);
  }

  private void populateProperties(Properties properties) {
    properties.setProperty("color", "blue");
    properties.setProperty("fragrance", "sweet");
    properties.setProperty("fruit", "apple");
    properties.setProperty("size", "medium");
    properties.setProperty("vehicle", "car");
  }

  private String[] lines(ByteArrayOutputStream baos) {
    return lines(baos.toString(StandardCharsets.ISO_8859_1));
  }

  private String[] lines(StringWriter writer) {
    return lines(writer.toString());
  }

  private String[] lines(String input) {
    return input.trim().split(SortedProperties.EOL);
  }

  private void assertKeys(Properties properties) {
    assertThat(Collections.list(properties.keys())) //
            .containsExactly("color", "fragrance", "fruit", "size", "vehicle");
  }

  private void assertKeySet(Properties properties) {
    assertThat(properties.keySet())
            .containsExactly("color", "fragrance", "fruit", "size", "vehicle");
  }

  private void assertEntrySet(Properties properties) {
    assertThat(properties.entrySet())
            .containsExactly(
                    entry("color", "blue"), //
                    entry("fragrance", "sweet"), //
                    entry("fruit", "apple"), //
                    entry("size", "medium"), //
                    entry("vehicle", "car") //
            );
  }

  private void assertPropsAreSorted(String[] lines) {
    assertThat(stream(lines).filter(s -> !s.startsWith("#")))
            .containsExactly(
                    "color=blue", //
                    "fragrance=sweet", //
                    "fruit=apple", //
                    "size=medium", //
                    "vehicle=car"//
            );
  }

}
