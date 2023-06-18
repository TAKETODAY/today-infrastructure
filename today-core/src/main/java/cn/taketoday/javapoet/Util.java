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
package cn.taketoday.javapoet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

import static java.lang.Character.isISOControl;

/**
 * Like Guava, but worse and standalone. This makes it easier to mix JavaPoet with libraries that
 * bring their own version of Guava.
 */
final class Util {
  private Util() {
  }

  static <K, V> Map<K, List<V>> immutableMultimap(Map<K, List<V>> multimap) {
    LinkedHashMap<K, List<V>> result = new LinkedHashMap<>();
    for (Map.Entry<K, List<V>> entry : multimap.entrySet()) {
      if (entry.getValue().isEmpty())
        continue;
      result.put(entry.getKey(), immutableList(entry.getValue()));
    }
    return Collections.unmodifiableMap(result);
  }

  static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(map));
  }

  static void checkArgument(boolean condition, String format, Object... args) {
    if (!condition)
      throw new IllegalArgumentException(String.format(format, args));
  }

  static <T> T checkNotNull(T reference, String format, Object... args) {
    if (reference == null)
      throw new NullPointerException(String.format(format, args));
    return reference;
  }

  static void checkState(boolean condition, String format, Object... args) {
    if (!condition)
      throw new IllegalStateException(String.format(format, args));
  }

  static <T> List<T> immutableList(Collection<T> collection) {
    return Collections.unmodifiableList(new ArrayList<>(collection));
  }

  static <T> Set<T> immutableSet(Collection<T> set) {
    return Collections.unmodifiableSet(new LinkedHashSet<>(set));
  }

  static <T> Set<T> union(Set<T> a, Set<T> b) {
    Set<T> result = new LinkedHashSet<>();
    result.addAll(a);
    result.addAll(b);
    return result;
  }

  static void requireExactlyOneOf(Set<Modifier> modifiers, Modifier... mutuallyExclusive) {
    int count = 0;
    for (Modifier modifier : mutuallyExclusive) {
      if (modifiers.contains(modifier))
        count++;
    }
    checkArgument(count == 1, "modifiers %s must contain one of %s",
            modifiers, Arrays.toString(mutuallyExclusive));
  }

  static String characterLiteralWithoutSingleQuotes(char c) {
    // see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
    switch (c) {
      case '\b':
        return "\\b"; /* \u0008: backspace (BS) */
      case '\t':
        return "\\t"; /* \u0009: horizontal tab (HT) */
      case '\n':
        return "\\n"; /* \u000a: linefeed (LF) */
      case '\f':
        return "\\f"; /* \u000c: form feed (FF) */
      case '\r':
        return "\\r"; /* \u000d: carriage return (CR) */
      case '\"':
        return "\"";  /* \u0022: double quote (") */
      case '\'':
        return "\\'"; /* \u0027: single quote (') */
      case '\\':
        return "\\\\";  /* \u005c: backslash (\) */
      default:
        return isISOControl(c) ? String.format("\\u%04x", (int) c) : Character.toString(c);
    }
  }

  /** Returns the string literal representing {@code value}, including wrapping double quotes. */
  static String stringLiteralWithDoubleQuotes(String value, String indent) {
    StringBuilder result = new StringBuilder(value.length() + 2);
    result.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      // trivial case: single quote must not be escaped
      if (c == '\'') {
        result.append("'");
        continue;
      }
      // trivial case: double quotes must be escaped
      if (c == '\"') {
        result.append("\\\"");
        continue;
      }
      // default case: just let character literal do its work
      result.append(characterLiteralWithoutSingleQuotes(c));
      // need to append indent after linefeed?
      if (c == '\n' && i + 1 < value.length()) {
        result.append("\"\n").append(indent).append(indent).append("+ \"");
      }
    }
    result.append('"');
    return result.toString();
  }
}
