/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.util.Assert;

/**
 * A parsed bean property path, such as {@code person.addresses[1].city}.
 * <p>The grammar is {@code Segment ('.' Segment)*}, where a segment has a
 * name followed by zero or more {@code [key]} indexes. A name may be empty
 * only when followed by an index. Keys may be quoted with single or double
 * quotes, or unquoted with balanced nested brackets and no quote characters.
 * The {@linkplain #canonicalName() canonical name} is suitable for policy
 * matching, and the {@linkplain #segments() segments} for navigation.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public final class PropertyPath {

  private final String canonicalName;

  private final List<Segment> segments;

  private PropertyPath(String canonicalName, List<Segment> segments) {
    this.canonicalName = canonicalName;
    this.segments = segments;
  }

  /**
   * Parse a property path; an empty path has no segments.
   *
   * @param path the path to parse
   * @return the parsed path
   * @throws InvalidPropertyPathException if the path is malformed
   */
  public static PropertyPath parse(String path) {
    return parse(path, Options.UNLIMITED);
  }

  /**
   * Parse a property path subject to a maximum nesting depth.
   *
   * @param path the path to parse
   * @param options parsing options
   * @return the parsed path
   * @throws InvalidPropertyPathException if malformed or too deep
   */
  public static PropertyPath parse(String path, Options options) {
    Assert.notNull(path, "Property path is required");
    Assert.notNull(options, "Options is required");
    if (path.isEmpty()) {
      return new PropertyPath("", List.of());
    }
    PropertyPath parsed = new Parser(path).parse();
    if (parsed.segments.size() - 1 > options.maxNestedPathDepth) {
      throw new InvalidPropertyPathException(path,
              "nesting depth exceeds the maximum of " + options.maxNestedPathDepth);
    }
    return parsed;
  }

  /**
   * Return a canonical path or the original text if malformed (empty for null).
   * Intended for displaying or matching user-supplied names; navigation should
   * instead use {@link #parse(String)} and reject malformed paths.
   *
   * @param path a possibly null path
   * @return its canonical or original form
   * @since 5.0
   */
  public static String canonicalNameOrOriginal(@Nullable String path) {
    if (path == null) {
      return "";
    }
    try {
      return parse(path).canonicalName();
    }
    catch (InvalidPropertyPathException ex) {
      return path;
    }
  }

  /** Return the canonical path, stripping unnecessary quotes from keys. */
  public String canonicalName() {
    return this.canonicalName;
  }

  /** Return an unmodifiable list of segments in order. */
  public List<Segment> segments() {
    return this.segments;
  }

  /**
   * Return a path starting at the specified segment index.
   *
   * @param fromIndex the inclusive start index
   * @return the suffix, or this path for index zero
   * @throws IndexOutOfBoundsException if the index is negative
   * @throws IllegalArgumentException if it exceeds the number of segments
   */
  public PropertyPath subPath(int fromIndex) {
    if (fromIndex == 0) {
      return this;
    }
    Assert.isTrue(fromIndex <= this.segments.size(), "Index exceeds segment count");
    List<Segment> suffix = this.segments.subList(fromIndex, this.segments.size());
    StringBuilder canonical = new StringBuilder();
    for (Segment segment : suffix) {
      if (!canonical.isEmpty()) {
        canonical.append('.');
      }
      canonical.append(segment.toCanonicalName());
    }
    return new PropertyPath(canonical.toString(), suffix);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return this == other || other instanceof PropertyPath path && this.canonicalName.equals(path.canonicalName);
  }

  @Override
  public int hashCode() {
    return this.canonicalName.hashCode();
  }

  @Override
  public String toString() {
    return this.canonicalName;
  }

  /**
   * A dot-separated path segment with zero or more index keys.
   *
   * @param name the property name, empty only for a root-level index
   * @param keys the unquoted keys, possibly empty but never null
   */
  public record Segment(String name, List<String> keys) {

    public Segment {
      Assert.notNull(name, "Segment name is required");
      Assert.notNull(keys, "Segment keys is required");
      keys = List.copyOf(keys);
    }

    /** Return the canonical form of this segment. */
    public String toCanonicalName() {
      StringBuilder canonical = new StringBuilder(this.name);
      for (String key : this.keys) {
        char quote = canonicalQuoteChar(key);
        canonical.append('[');
        if (quote != 0) {
          canonical.append(quote);
        }
        canonical.append(key);
        if (quote != 0) {
          canonical.append(quote);
        }
        canonical.append(']');
      }
      return canonical.toString();
    }

    /** Return a segment with its last key removed, or this when it has none. */
    public Segment withoutLastKey() {
      return this.keys.isEmpty() ? this : new Segment(this.name, this.keys.subList(0, this.keys.size() - 1));
    }

    private static char canonicalQuoteChar(String key) {
      int depth = 0;
      boolean balanced = true;
      boolean single = false;
      boolean doubleQuote = false;
      for (int i = 0; i < key.length(); i++) {
        switch (key.charAt(i)) {
          case '[' -> depth++;
          case ']' -> {
            if (--depth < 0) {
              balanced = false;
            }
          }
          case '\'' -> single = true;
          case '"' -> doubleQuote = true;
          default -> { }
        }
      }
      if (balanced && depth == 0 && !single && !doubleQuote) {
        return 0;
      }
      return single && !doubleQuote ? '"' : '\'';
    }
  }

  /** Parsing options for property paths. */
  public static final class Options {

    /** No limit on nested segments. */
    public static final Options UNLIMITED = new Options(Integer.MAX_VALUE);

    private final int maxNestedPathDepth;

    private Options(int maxNestedPathDepth) {
      this.maxNestedPathDepth = maxNestedPathDepth;
    }

    /**
     * Limit the number of intermediate properties in a path.
     *
     * @param maxNestedPathDepth the non-negative maximum
     * @return parsing options enforcing the limit
     */
    public static Options withMaxNestedPathDepth(int maxNestedPathDepth) {
      Assert.isTrue(maxNestedPathDepth >= 0, "'maxNestedPathDepth' must not be negative");
      return new Options(maxNestedPathDepth);
    }
  }

  private static final class Parser {

    private final String path;

    private final List<Segment> segments = new ArrayList<>(2);

    private int pos;

    private int start;

    Parser(String path) {
      this.path = path;
    }

    PropertyPath parse() {
      while (this.pos < this.path.length()) {
        List<String> keys = new ArrayList<>(2);
        while (this.pos < this.path.length() && this.path.charAt(this.pos) != '.'
                && this.path.charAt(this.pos) != '[') {
          if (this.path.charAt(this.pos) == ']') {
            throw error("unexpected ']' without a matching '['");
          }
          this.pos++;
        }
        String name = this.path.substring(this.start, this.pos);
        while (this.pos < this.path.length() && this.path.charAt(this.pos) == '[') {
          this.pos++;
          keys.add(parseKey());
        }
        if (name.isEmpty() && keys.isEmpty()) {
          throw error("empty path segment");
        }
        this.segments.add(new Segment(name, keys));
        if (this.pos < this.path.length()) {
          if (this.path.charAt(this.pos) != '.') {
            throw error("unexpected '" + this.path.charAt(this.pos) + "' after an index; expected '.', '[', or the end of the path");
          }
          this.pos++;
          this.start = this.pos;
          if (this.pos == this.path.length()) {
            throw error("empty path segment");
          }
        }
      }
      StringBuilder canonical = new StringBuilder(this.path.length());
      for (Segment segment : this.segments) {
        if (!canonical.isEmpty()) {
          canonical.append('.');
        }
        canonical.append(segment.toCanonicalName());
      }
      return new PropertyPath(canonical.toString(), List.copyOf(this.segments));
    }

    private String parseKey() {
      if (this.pos == this.path.length()) {
        throw error("unclosed '['");
      }
      char first = this.path.charAt(this.pos);
      if (first == '\'' || first == '"') {
        int keyStart = ++this.pos;
        while (this.pos < this.path.length() && this.path.charAt(this.pos) != first) {
          this.pos++;
        }
        if (this.pos == this.path.length()) {
          throw error("unterminated quote '" + first + "'");
        }
        String key = this.path.substring(keyStart, this.pos++);
        if (this.pos == this.path.length()) {
          throw error("unclosed '['");
        }
        if (this.path.charAt(this.pos) != ']') {
          throw error("unexpected '" + this.path.charAt(this.pos) + "' after a closing quote; expected ']'");
        }
        this.pos++;
        return key;
      }
      int keyStart = this.pos;
      int depth = 0;
      while (this.pos < this.path.length()) {
        char ch = this.path.charAt(this.pos++);
        if (ch == '[') {
          depth++;
        }
        else if (ch == ']') {
          if (depth == 0) {
            return this.path.substring(keyStart, this.pos - 1);
          }
          depth--;
        }
        else if (ch == '\'' || ch == '"') {
          throw error("unexpected quote '" + ch + "' in an unquoted key; quote the whole key");
        }
      }
      throw error("unclosed '['");
    }

    private InvalidPropertyPathException error(String reason) {
      return new InvalidPropertyPathException(this.path, reason + " (at position " + this.pos + ")");
    }
  }
}
