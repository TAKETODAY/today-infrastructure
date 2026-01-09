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

package infra.scheduling.support;

import org.jspecify.annotations.Nullable;

import java.time.temporal.Temporal;

import infra.lang.Assert;

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
