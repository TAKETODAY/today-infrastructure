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

package cn.taketoday.framework.logging;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Utility class that can be used to format a correlation identifier for logging based on
 * <a href=
 * "https://www.w3.org/TR/trace-context/#examples-of-http-traceparent-headers">W3C</a>
 * recommendations.
 * <p>
 * The formatter can be configured with a comma-separated list of names and the expected
 * length of their resolved value. Each item should be specified in the form
 * {@code "<name>(length)"}. For example, {@code "traceId(32),spanId(16)"} specifies the
 * names {@code "traceId"} and {@code "spanId"} with expected lengths of {@code 32} and
 * {@code 16} respectively.
 * <p>
 * Correlation IDs are formatted as dash separated strings surrounded in square brackets.
 * Formatted output is always of a fixed width and with trailing space. Dashes are omitted
 * if none of the named items can be resolved.
 * <p>
 * The following example would return a formatted result of
 * {@code "[01234567890123456789012345678901-0123456789012345] "}: <pre class="code">
 * CorrelationIdFormatter formatter = CorrelationIdFormatter.of("traceId(32),spanId(16)");
 * Map&lt;String, String&gt; mdc = Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345");
 * return formatter.format(mdc::get);
 * </pre>
 * <p>
 * If {@link #of(String)} is called with an empty spec the {@link #DEFAULT} formatter will
 * be used.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #of(String)
 * @see #of(Collection)
 * @since 4.0
 */
public final class CorrelationIdFormatter {

  /**
   * Default {@link CorrelationIdFormatter}.
   */
  public static final CorrelationIdFormatter DEFAULT = CorrelationIdFormatter.of(
          "traceId(32)", "spanId(16)");

  private final List<Part> parts;

  private final String blank;

  private CorrelationIdFormatter(List<Part> parts) {
    this.parts = parts;
    this.blank = String.format("[%s] ", parts.stream().map(Part::blank).collect(Collectors.joining(" ")));
  }

  /**
   * Format a correlation from the values in the given resolver.
   *
   * @param resolver the resolver used to resolve named values
   * @return a formatted correlation id
   */
  public String format(Function<String, String> resolver) {
    StringBuilder result = new StringBuilder();
    formatTo(resolver, result);
    return result.toString();
  }

  /**
   * Format a correlation from the values in the given resolver and append it to the
   * given {@link Appendable}.
   *
   * @param resolver the resolver used to resolve named values
   * @param appendable the appendable for the formatted correlation id
   */
  public void formatTo(Function<String, String> resolver, Appendable appendable) {
    Predicate<Part> canResolve = part -> StringUtils.isNotEmpty(resolver.apply(part.name()));
    try {
      if (parts.stream().anyMatch(canResolve)) {
        appendable.append('[');
        for (Iterator<Part> iterator = this.parts.iterator(); iterator.hasNext(); ) {
          appendable.append(iterator.next().resolve(resolver));
          if (iterator.hasNext()) {
            appendable.append('-');
          }
        }
        appendable.append("] ");
      }
      else {
        appendable.append(blank);
      }
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public String toString() {
    return this.parts.stream()
            .map(Part::toString)
            .collect(Collectors.joining(","));
  }

  /**
   * Create a new {@link CorrelationIdFormatter} instance from the given specification.
   *
   * @param spec a comma-separated specification
   * @return a new {@link CorrelationIdFormatter} instance
   */
  public static CorrelationIdFormatter of(String spec) {
    try {
      return StringUtils.isBlank(spec) ? DEFAULT : of(List.of(spec.split(",")));
    }
    catch (Exception ex) {
      throw new IllegalStateException("Unable to parse correlation formatter spec '%s'".formatted(spec), ex);
    }
  }

  /**
   * Create a new {@link CorrelationIdFormatter} instance from the given specification.
   *
   * @param spec a pre-separated specification
   * @return a new {@link CorrelationIdFormatter} instance
   */
  public static CorrelationIdFormatter of(@Nullable String... spec) {
    return of((spec != null) ? Arrays.asList(spec) : Collections.emptyList());
  }

  /**
   * Create a new {@link CorrelationIdFormatter} instance from the given specification.
   *
   * @param spec a pre-separated specification
   * @return a new {@link CorrelationIdFormatter} instance
   */
  public static CorrelationIdFormatter of(Collection<String> spec) {
    if (CollectionUtils.isEmpty(spec)) {
      return DEFAULT;
    }
    List<Part> parts = spec.stream().map(Part::of).toList();
    return new CorrelationIdFormatter(parts);
  }

  /**
   * A part of the correlation id.
   *
   * @param name the name of the correlation part
   * @param length the expected length of the correlation part
   */
  record Part(String name, int length) {

    private static final Pattern pattern = Pattern.compile("^(.+?)\\((\\d+)\\)$");

    String resolve(Function<String, String> resolver) {
      String resolved = resolver.apply(name());
      if (resolved == null) {
        return blank();
      }
      int padding = length() - resolved.length();
      return (padding <= 0) ? resolved : resolved + " ".repeat(padding);
    }

    String blank() {
      return " ".repeat(this.length);
    }

    @Override
    public String toString() {
      return "%s(%s)".formatted(name(), length());
    }

    static Part of(String part) {
      Matcher matcher = pattern.matcher(part.trim());
      Assert.state(matcher.matches(), () -> "Invalid specification part '%s'".formatted(part));
      String name = matcher.group(1);
      int length = Integer.parseInt(matcher.group(2));
      return new Part(name, length);
    }

  }

}
