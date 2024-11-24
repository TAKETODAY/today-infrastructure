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

package infra.scheduling.support;

import java.time.temporal.Temporal;

import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Extension of {@link CronField} that wraps an array of cron fields.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
final class CompositeCronField extends CronField {

  private final String value;
  private final CronField[] fields;

  private CompositeCronField(Type type, CronField[] fields, String value) {
    super(type);
    this.value = value;
    this.fields = fields;
  }

  /**
   * Composes the given fields into a {@link CronField}.
   */
  public static CronField compose(CronField[] fields, Type type, String value) {
    Assert.notEmpty(fields, "Fields must not be empty");
    Assert.hasLength(value, "Value must not be empty");

    if (fields.length == 1) {
      return fields[0];
    }
    else {
      return new CompositeCronField(type, fields, value);
    }
  }

  @Nullable
  @Override
  public <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal) {
    T result = null;
    for (CronField field : this.fields) {
      T candidate = field.nextOrSame(temporal);
      if (result == null
              || candidate != null && candidate.compareTo(result) < 0) {
        result = candidate;
      }
    }
    return result;
  }

  @Override
  public int hashCode() {
    return this.value.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CompositeCronField other)) {
      return false;
    }
    return type() == other.type() &&
            this.value.equals(other.value);
  }

  @Override
  public String toString() {
    return type() + " '" + this.value + "'";

  }
}
