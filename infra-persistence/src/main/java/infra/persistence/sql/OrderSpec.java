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
import java.util.List;
import java.util.Objects;

import infra.core.Pair;
import infra.persistence.Identifier;
import infra.persistence.Order;
import infra.persistence.platform.Platform;
import infra.util.StringUtils;

/**
 * An immutable ORDER BY specification: an ordered list of sort keys, or a raw SQL
 * fragment for cases that cannot be expressed as plain column ordering.
 *
 * <p>Sort keys are rendered in declaration order, so the first key takes precedence.
 * Each key's column is an {@link Identifier}, allowing platform-aware quoting.
 *
 * <p>Use {@link #of(Pair...)} / {@link #asc} / {@link #desc} for structured keys, or
 * {@link #plain} to embed an arbitrary SQL fragment such as an expression or a
 * {@code CASE} clause. Build incrementally with {@link #builder()}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 12:39
 */
public final class OrderSpec {

  /** A shared, immutable spec that contributes no ordering. */
  private static final OrderSpec EMPTY = new OrderSpec(List.of(), null);

  /** Ordered sort keys; empty when this is a raw clause. */
  private final List<Item> items;

  /** Raw SQL fragment, non-null only when built via {@link #plain}. */
  private final @Nullable CharSequence rawClause;

  private OrderSpec(List<Item> items, @Nullable CharSequence rawClause) {
    this.items = List.copyOf(items);
    this.rawClause = rawClause;
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
   * Create an ORDER BY spec from an ordered sequence of sort keys.
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
   */
  public static OrderSpec asc(String column) {
    return builder().asc(column).build();
  }

  /**
   * Create a single descending sort key spec.
   *
   * @param column the column name to order by
   * @return the immutable spec
   */
  public static OrderSpec desc(String column) {
    return builder().desc(column).build();
  }

  /**
   * Use an arbitrary SQL fragment as the ORDER BY clause, for example
   * {@code "LENGTH(name) DESC"} or {@code "CASE WHEN status='active' THEN 1 ELSE 2 END"}.
   *
   * <p>The fragment is rendered as-is, without validation or dialect quoting.
   *
   * @param clause the raw SQL ORDER BY fragment
   * @return the immutable spec
   */
  public static OrderSpec plain(CharSequence clause) {
    return new OrderSpec(List.of(), clause);
  }

  /**
   * Return a new builder for incrementally assembling an ORDER BY spec.
   *
   * @return a fresh builder
   */
  public static Builder builder() {
    return new Builder();
  }

  // ---------- query ----------

  /**
   * Whether this spec contributes no ordering.
   *
   * @return {@code true} when there are no sort keys and no non-blank raw clause
   */
  public boolean isEmpty() {
    return rawClause != null ? StringUtils.isBlank(rawClause) : items.isEmpty();
  }

  /**
   * Render this spec into a SQL ORDER BY fragment for the given platform.
   *
   * @param platform the database platform whose quoting rules apply
   * @return the rendered clause; empty when {@link #isEmpty()}
   */
  public CharSequence toClause(Platform platform) {
    if (rawClause != null) {
      return rawClause;
    }
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(items.get(i).toClause(platform));
    }
    return builder;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof OrderSpec that
            && items.equals(that.items)
            && Objects.equals(rawClauseText(), that.rawClauseText());
  }

  @Override
  public int hashCode() {
    return 31 * items.hashCode() + Objects.hashCode(rawClauseText());
  }

  private @Nullable String rawClauseText() {
    return rawClause != null ? rawClause.toString() : null;
  }

  // ---------- sort key ----------

  /** A single ordered sort key: a column and a direction. */
  public record Item(Identifier column, Order direction) {

    CharSequence toClause(Platform platform) {
      return new StringBuilder()
              .append(column.render(platform))
              .append(' ').append(direction.name());
    }
  }

  // ---------- builder ----------

  /**
   * Builder for incrementally assembling an immutable {@link OrderSpec}.
   */
  public static final class Builder {

    private final ArrayList<Item> items = new ArrayList<>();

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
      items.add(new Item(column, direction));
      return this;
    }

    /** Build the immutable spec holding the keys appended so far. */
    public OrderSpec build() {
      return items.isEmpty() ? EMPTY : new OrderSpec(items, null);
    }
  }

}
