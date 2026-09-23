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

package infra.persistence.sql;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import infra.core.Pair;
import infra.persistence.Identifier;
import infra.persistence.Order;
import infra.persistence.platform.Platform;
import infra.util.StringUtils;

/**
 * An immutable ORDER BY specification: an ordered list of sort keys and/or raw SQL
 * fragments, rendered in declaration order so the first part takes precedence.
 *
 * <p>Most keys are structured ({@link Item}: a column with a direction, quoted via
 * {@link Platform}); {@link Fragment fragments} embed arbitrary SQL such as an
 * expression or a {@code CASE} clause and are rendered as-is. The two may be freely
 * mixed, for example {@code ORDER BY LENGTH(name) DESC, created_at DESC}.
 *
 * <p>Use {@link #of(Pair...)} / {@link #asc} / {@link #desc} for structured keys,
 * {@link #plain} for a single fragment, or {@link #builder()} to assemble a mix.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 12:39
 */
public final class OrderSpec {

  /** A shared, immutable spec that contributes no ordering. */
  private static final OrderSpec EMPTY = new OrderSpec(List.of());

  /** The ordered parts of the clause, already immutable. */
  private final List<Part> parts;

  /**
   * Internal constructor: the given list is kept as-is, so callers must pass an
   * immutable list (typically {@link List#of} or {@link List#copyOf}).
   */
  private OrderSpec(List<Part> parts) {
    this.parts = parts;
  }

  // ---------- structured factories ----------

  /**
   * Return the shared empty spec, which contributes no ordering.
   *
   * <p>Lets callers avoid {@code null}: an empty spec is always returned when no
   * ordering applies. {@link #isEmpty()} returns {@code true} for it.
   *
   * @return the shared empty spec
   */
  public static OrderSpec empty() {
    return EMPTY;
  }

  /**
   * Create an ORDER BY spec from an ordered sequence of structured sort keys.
   *
   * @param sortKeys the sort keys in declaration order
   * @return the immutable spec
   */
  public static OrderSpec of(Pair<String, Order>... sortKeys) {
    Builder builder = builder();
    for (Pair<String, Order> sortKey : sortKeys) {
      builder.orderBy(sortKey.first, sortKey.second);
    }
    return builder.build();
  }

  /**
   * Create a single ascending sort key spec.
   *
   * @param column the column name to order by
   * @return the immutable spec
   * @since 5.0
   */
  public static OrderSpec asc(String column) {
    return new OrderSpec(List.of(new Item(Identifier.parse(column), Order.ASC)));
  }

  /**
   * Create a single descending sort key spec.
   *
   * @param column the column name to order by
   * @return the immutable spec
   * @since 5.0
   */
  public static OrderSpec desc(String column) {
    return new OrderSpec(List.of(new Item(Identifier.parse(column), Order.DESC)));
  }

  /**
   * Create a spec from a single raw SQL fragment, for example
   * {@code "LENGTH(name) DESC"} or {@code "CASE WHEN status='active' THEN 1 ELSE 2 END"}.
   *
   * <p>The fragment is rendered as-is, without validation or dialect quoting. A blank
   * clause yields the shared {@link #empty() empty} spec.
   *
   * @param clause the raw SQL ORDER BY fragment
   * @return the immutable spec
   * @since 5.0
   */
  public static OrderSpec plain(CharSequence clause) {
    return StringUtils.hasText(clause)
            ? new OrderSpec(List.of(new Fragment(clause)))
            : EMPTY;
  }

  /**
   * Return a new builder for incrementally assembling an ORDER BY spec.
   *
   * @return a fresh builder
   * @since 5.0
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Return a builder seeded with this spec's parts, for mutating and re-building an
   * ORDER BY spec.
   *
   * @return a builder pre-populated from this spec
   * @since 5.0
   */
  public Builder mutate() {
    Builder builder = new Builder();
    builder.parts.addAll(parts);
    return builder;
  }

  // ---------- query ----------

  /**
   * Whether this spec contributes no ordering.
   *
   * @return {@code true} when there are no parts
   * @since 5.0
   */
  public boolean isEmpty() {
    return parts.isEmpty();
  }

  /**
   * Whether this spec contains at least one raw SQL {@link Fragment}.
   *
   * @return {@code true} if any part is a raw fragment
   * @since 5.0
   */
  public boolean containsRaw() {
    for (Part part : parts) {
      if (part instanceof Fragment) {
        return true;
      }
    }
    return false;
  }

  /**
   * Render this spec into a SQL ORDER BY fragment for the given platform, joining the
   * parts with {@code ", "}.
   *
   * @param platform the database platform whose quoting rules apply
   * @return the rendered clause; empty when {@link #isEmpty()}
   * @since 5.0
   */
  public String toClause(Platform platform) {
    StringBuilder builder = new StringBuilder(parts.size() * 12);
    appendTo(builder, platform);
    return builder.toString();
  }

  /**
   * Append the ORDER BY clause contents to an existing buffer, without the
   * {@code ORDER BY} keyword. Parts are separated by {@code ", "}.
   *
   * @param builder the buffer to append to
   * @param platform the database platform whose quoting rules apply
   * @since 5.0
   */
  public void appendTo(StringBuilder builder, Platform platform) {
    Iterator<Part> iterator = parts.iterator();
    while (iterator.hasNext()) {
      iterator.next().appendTo(builder, platform);
      if (iterator.hasNext()) {
        builder.append(", ");
      }
    }
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return this == o || (o instanceof OrderSpec that && parts.equals(that.parts));
  }

  @Override
  public int hashCode() {
    return parts.hashCode();
  }

  // ---------- parts ----------

  /**
   * One element of an ORDER BY clause: either a structured {@link Item} or a raw
   * {@link Fragment}.
   */
  public sealed interface Part {

    /**
     * Append this part to an existing buffer.
     *
     * @param builder the buffer to append to
     * @param platform the database platform whose quoting rules apply
     */
    void appendTo(StringBuilder builder, Platform platform);
  }

  /** A structured sort key: a column and a direction. */
  public record Item(Identifier column, Order direction) implements Part {

    @Override
    public void appendTo(StringBuilder builder, Platform platform) {
      builder.append(column.render(platform)).append(' ').append(direction.name());
    }
  }

  /** A raw SQL ORDER BY fragment, rendered as-is without dialect quoting. */
  public record Fragment(CharSequence text) implements Part {

    @Override
    public void appendTo(StringBuilder builder, Platform platform) {
      builder.append(text);
    }
  }

  // ---------- builder ----------

  /**
   * Builder for incrementally assembling an immutable {@link OrderSpec}.
   */
  public static final class Builder {

    private final ArrayList<Part> parts = new ArrayList<>();

    /**
     * Whether this builder currently has no sort keys or raw fragments.
     *
     * @return {@code true} when there are no parts
     */
    public boolean isEmpty() {
      return parts.isEmpty();
    }

    /** Append an ascending sort key. */
    public Builder asc(String column) {
      return asc(Identifier.parse(column));
    }

    /** Append an ascending sort key. */
    public Builder asc(Identifier column) {
      return orderBy(column, Order.ASC);
    }

    /** Append a descending sort key. */
    public Builder desc(String column) {
      return desc(Identifier.parse(column));
    }

    /** Append a descending sort key. */
    public Builder desc(Identifier column) {
      return orderBy(column, Order.DESC);
    }

    /** Append a sort key with the given direction. */
    public Builder orderBy(String column, Order direction) {
      return orderBy(Identifier.parse(column), direction);
    }

    /** Append a sort key with the given direction. */
    public Builder orderBy(Identifier column, Order direction) {
      parts.add(new Item(column, direction));
      return this;
    }

    /**
     * Append a raw SQL fragment as a sort key, for example {@code LENGTH(name) DESC}.
     * It is rendered as-is and may be combined with structured keys. A blank clause is
     * ignored.
     *
     * @param clause the raw SQL ORDER BY fragment
     * @return this builder, to facilitate method chaining
     */
    public Builder raw(CharSequence clause) {
      if (StringUtils.hasText(clause)) {
        parts.add(new Fragment(clause));
      }
      return this;
    }

    /**
     * Remove every structured sort key on the given column, leaving raw fragments and
     * other keys untouched.
     *
     * @param column the column name whose keys to remove
     * @return this builder, to facilitate method chaining
     */
    public Builder remove(String column) {
      return remove(Identifier.parse(column));
    }

    /**
     * Remove every structured sort key on the given column, leaving raw fragments and
     * other keys untouched.
     *
     * @param column the column whose keys to remove
     * @return this builder, to facilitate method chaining
     */
    public Builder remove(Identifier column) {
      parts.removeIf(part -> part instanceof Item item && item.column().equals(column));
      return this;
    }

    /**
     * Remove every part matching the given filter.
     *
     * @param filter the filter selecting parts to remove
     * @return this builder, to facilitate method chaining
     */
    public Builder removeIf(Predicate<Part> filter) {
      parts.removeIf(filter);
      return this;
    }

    /**
     * Remove all parts.
     *
     * @return this builder, to facilitate method chaining
     */
    public Builder clear() {
      parts.clear();
      return this;
    }

    /**
     * Build the immutable spec holding the parts appended so far. A builder with no
     * parts yields the shared {@link #empty() empty} spec.
     */
    public OrderSpec build() {
      return parts.isEmpty() ? EMPTY : new OrderSpec(List.copyOf(parts));
    }
  }

}
