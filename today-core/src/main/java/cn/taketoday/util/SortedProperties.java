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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serial;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Specialization of {@link Properties} that sorts properties alphanumerically
 * based on their keys.
 *
 * <p>This can be useful when storing the {@link Properties} instance in a
 * properties file, since it allows such files to be generated in a repeatable
 * manner with consistent ordering of properties.
 *
 * <p>Comments in generated properties files can also be optionally omitted.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.util.Properties
 * @since 3.0 2021/1/29 15:19
 */
class SortedProperties extends Properties {

  @Serial
  private static final long serialVersionUID = 1L;

  static final String EOL = System.lineSeparator();

  private static final Comparator<Object> keyComparator = Comparator.comparing(String::valueOf);

  private static final Comparator<Map.Entry<Object, Object>> entryComparator = Entry.comparingByKey(keyComparator);

  private final boolean omitComments;

  /**
   * Construct a new {@code SortedProperties} instance that honors the supplied
   * {@code omitComments} flag.
   *
   * @param omitComments {@code true} if comments should be omitted when
   * storing properties in a file
   */
  SortedProperties(boolean omitComments) {
    this.omitComments = omitComments;
  }

  /**
   * Construct a new {@code SortedProperties} instance with properties populated
   * from the supplied {@link Properties} object and honoring the supplied
   * {@code omitComments} flag.
   * <p>Default properties from the supplied {@code Properties} object will
   * not be copied.
   *
   * @param properties the {@code Properties} object from which to copy the
   * initial properties
   * @param omitComments {@code true} if comments should be omitted when
   * storing properties in a file
   */
  SortedProperties(Properties properties, boolean omitComments) {
    this(omitComments);
    putAll(properties);
  }

  @Override
  public void store(OutputStream out, String comments) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    super.store(baos, (this.omitComments ? null : comments));
    String contents = baos.toString(StandardCharsets.ISO_8859_1);
    for (String line : contents.split(EOL)) {
      if (!(this.omitComments && line.startsWith("#"))) {
        out.write((line + EOL).getBytes(StandardCharsets.ISO_8859_1));
      }
    }
  }

  @Override
  public void store(Writer writer, String comments) throws IOException {
    StringWriter stringWriter = new StringWriter();
    super.store(stringWriter, (this.omitComments ? null : comments));
    String contents = stringWriter.toString();
    for (String line : contents.split(EOL)) {
      if (!(this.omitComments && line.startsWith("#"))) {
        writer.write(line + EOL);
      }
    }
  }

  @Override
  public void storeToXML(OutputStream out, String comments) throws IOException {
    super.storeToXML(out, (this.omitComments ? null : comments));
  }

  @Override
  public void storeToXML(OutputStream out, String comments, String encoding) throws IOException {
    super.storeToXML(out, (this.omitComments ? null : comments), encoding);
  }

  /**
   * Return a sorted enumeration of the keys in this {@link Properties} object.
   *
   * @see #keySet()
   */
  @Override
  public synchronized Enumeration<Object> keys() {
    return Collections.enumeration(keySet());
  }

  /**
   * Return a sorted set of the keys in this {@link Properties} object.
   * <p>The keys will be converted to strings if necessary using
   * {@link String#valueOf(Object)} and sorted alphanumerically according to
   * the natural order of strings.
   */
  @Override
  public Set<Object> keySet() {
    Set<Object> sortedKeys = new TreeSet<>(keyComparator);
    sortedKeys.addAll(super.keySet());
    return Collections.synchronizedSet(sortedKeys);
  }

  /**
   * Return a sorted set of the entries in this {@link Properties} object.
   * <p>The entries will be sorted based on their keys, and the keys will be
   * converted to strings if necessary using {@link String#valueOf(Object)}
   * and compared alphanumerically according to the natural order of strings.
   */
  @Override
  public Set<Map.Entry<Object, Object>> entrySet() {
    Set<Map.Entry<Object, Object>> sortedEntries = new TreeSet<>(entryComparator);
    sortedEntries.addAll(super.entrySet());
    return Collections.synchronizedSet(sortedEntries);
  }

}
