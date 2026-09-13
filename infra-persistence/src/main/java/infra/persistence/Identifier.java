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

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

import infra.persistence.platform.Platform;
import infra.util.StringUtils;

/**
 * A SQL identifier, pairing a bare name with whether it has to be quoted.
 *
 * <p>An identifier stays unresolved until it is rendered. Calling
 * {@link #render(Platform)} yields the dialect-specific text, so the same
 * identifier can serve every supported database; {@link #render()} instead
 * produces an internal backtick-marked form that is only meaningful as a neutral
 * placeholder before a platform is known.
 *
 * <p>Instances are immutable and safe to share.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class Identifier implements Comparable<Identifier> {

  /**
   * A plain SQL name: a letter or underscore followed by letters, digits,
   * underscores or dollar signs.
   */
  private static final Pattern PLAIN_NAME = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_$]*");

  /** The bare name, without any quote marker. */
  private final String name;

  /** Whether {@link #render(Platform)} should wrap the name in quotes. */
  private final boolean quoted;

  /**
   * Construct an identifier from its bare name.
   *
   * @param text the bare name; must be non-empty and free of quote markers
   * @param quoted whether the name should be rendered quoted
   * @throws IllegalArgumentException if the name is empty or already carries
   * quote markers
   */
  public Identifier(String text, boolean quoted) {
    if (StringUtils.isEmpty(text)) {
      throw new IllegalArgumentException("Identifier text cannot be empty");
    }
    if (isQuoted(text)) {
      throw new IllegalArgumentException("Identifier text must not be wrapped in quote markers");
    }
    this.name = text;
    this.quoted = quoted;
  }

  /**
   * Construct an unquoted identifier from its bare name. Intended for subclasses
   * that already normalized the name.
   *
   * @param text the bare name
   */
  protected Identifier(String text) {
    this.name = text;
    this.quoted = false;
  }

  /**
   * Return the bare name, without quote markers.
   *
   * @return the name
   */
  public String getText() {
    return name;
  }

  /**
   * Return whether this identifier should be rendered quoted.
   *
   * @return {@code true} if quoting is requested
   */
  public boolean isQuoted() {
    return quoted;
  }

  /**
   * Return a quoted counterpart, leaving an already quoted identifier unchanged.
   *
   * @return this identifier when quoted, otherwise a quoted equivalent
   */
  public Identifier quoted() {
    return quoted ? this : new Identifier(name, true);
  }

  /**
   * Render this identifier for the given platform: the name wrapped in the
   * platform's quote characters when quoting is requested, otherwise the bare
   * name.
   *
   * @param platform the platform whose quoting rules apply
   * @return the platform-specific text
   */
  public String render(Platform platform) {
    return quoted ? platform.toQuotedIdentifier(name) : name;
  }

  /**
   * Render this identifier in an internal neutral form, using backticks as the
   * quote markers. Prefer {@link #render(Platform)} for actual SQL text.
   *
   * @return the backtick-marked form when quoted, otherwise the bare name
   */
  public String render() {
    return quoted ? '`' + name + '`' : name;
  }

  /**
   * Return the name used for case-insensitive comparison: quoted names keep their
   * case, unquoted names are lowered.
   *
   * @return the comparison key
   */
  public String getCanonicalName() {
    return quoted ? name : name.toLowerCase(Locale.ENGLISH);
  }

  @Override
  public String toString() {
    return render();
  }

  @Override
  public boolean equals(@Nullable Object object) {
    return object == this || (object instanceof Identifier that
            && getCanonicalName().equals(that.getCanonicalName()));
  }

  /**
   * Test whether the given name matches this identifier, honoring case rules:
   * quoted identifiers match exactly, unquoted ones ignore case.
   *
   * @param other the name to compare with
   * @return {@code true} if the name matches
   */
  public boolean matches(String other) {
    return quoted ? name.equals(other) : name.equalsIgnoreCase(other);
  }

  @Override
  public int hashCode() {
    return getCanonicalName().hashCode();
  }

  @Override
  public int compareTo(Identifier other) {
    return getCanonicalName().compareTo(other.getCanonicalName());
  }

  /**
   * Build an identifier from its text form, without forcing quoting.
   *
   * <p>Returns {@code null} for a blank input. Text wrapped in a matched pair of
   * quote markers (backtick, double quote, or brackets) is treated as quoted and
   * kept without the markers. Otherwise the name is quoted automatically when it
   * is not a {@linkplain #PLAIN_NAME plain name}.
   *
   * @param text the text form, possibly {@code null}
   * @return the identifier, or {@code null} if the text is blank
   */
  public static @Nullable Identifier toIdentifier(@Nullable String text) {
    return toIdentifier(text, false, true);
  }

  /**
   * Build an identifier from its text form.
   *
   * @param text the text form, possibly {@code null}
   * @param quote whether an unquoted name should be rendered quoted
   * @return the identifier, or {@code null} if the text is blank
   * @see #toIdentifier(String)
   */
  public static @Nullable Identifier toIdentifier(@Nullable String text, boolean quote) {
    return toIdentifier(text, quote, true);
  }

  /**
   * Build an identifier from its text form.
   *
   * @param text the text form, possibly {@code null}
   * @param quote whether an unquoted name should be rendered quoted
   * @param autoquote whether a non-{@linkplain #PLAIN_NAME plain} name should be
   * quoted automatically
   * @return the identifier, or {@code null} if the text is blank
   * @see #toIdentifier(String)
   */
  public static @Nullable Identifier toIdentifier(@Nullable String text, boolean quote, boolean autoquote) {
    if (StringUtils.isBlank(text)) {
      return null;
    }
    String name = text.trim();
    if (isQuoted(name)) {
      return new Identifier(unQuote(name), true);
    }
    boolean mustQuote = quote || (autoquote && !PLAIN_NAME.matcher(name).matches());
    return new Identifier(name, mustQuote);
  }

  /**
   * Does the given text start and end with a matched pair of quote markers, with
   * at least one character in between?
   *
   * @param name the text to inspect
   * @return {@code true} if the text is a quoted form
   */
  public static boolean isQuoted(String name) {
    return isQuoted(name, 0, name.length());
  }

  /**
   * Does the given range start and end with a matched pair of quote markers, with
   * at least one character in between?
   *
   * @param name the text to inspect
   * @param start the start index, inclusive
   * @param end the end index, exclusive
   * @return {@code true} if the range is a quoted form
   */
  public static boolean isQuoted(String name, int start, int end) {
    if (end - start < 3) {
      return false;
    }
    char open = name.charAt(start);
    char close = name.charAt(end - 1);
    return (open == '`' && close == '`')
            || (open == '"' && close == '"')
            || (open == '[' && close == ']');
  }

  /**
   * Strip the surrounding quote markers from a quoted form.
   *
   * @param name a quoted form
   * @return the bare name
   * @throws IllegalArgumentException if the text is not a quoted form
   */
  public static String unQuote(String name) {
    if (!isQuoted(name)) {
      throw new IllegalArgumentException("Not a quoted identifier: " + name);
    }
    return name.substring(1, name.length() - 1);
  }

}
