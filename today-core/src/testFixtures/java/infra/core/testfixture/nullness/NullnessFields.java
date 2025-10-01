/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core.testfixture.nullness;

public class NullnessFields {

  public String unannotatedField = "";

  public @org.jspecify.annotations.Nullable String jspecifyNullableField;

  public @org.jspecify.annotations.NonNull String jspecifyNonNullField = "";

  @infra.core.testfixture.nullness.custom.Nullable
  public String customNullableField;

  public int primitiveField = 0;
}
