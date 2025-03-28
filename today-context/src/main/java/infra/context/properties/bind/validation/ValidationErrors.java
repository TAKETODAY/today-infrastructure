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

package infra.context.properties.bind.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.origin.Origin;
import infra.origin.OriginProvider;
import infra.validation.FieldError;
import infra.validation.ObjectError;

/**
 * A collection of {@link ObjectError ObjectErrors} caused by bind validation failures.
 * Where possible, included {@link FieldError FieldErrors} will be OriginProvider.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ValidationErrors implements Iterable<ObjectError> {

  private final ConfigurationPropertyName name;

  private final Set<ConfigurationProperty> boundProperties;

  private final List<ObjectError> errors;

  ValidationErrors(ConfigurationPropertyName name, Set<ConfigurationProperty> boundProperties, List<ObjectError> errors) {
    Assert.notNull(name, "Name is required");
    Assert.notNull(errors, "Errors is required");
    Assert.notNull(boundProperties, "BoundProperties is required");
    this.name = name;
    this.errors = convertErrors(name, boundProperties, errors);
    this.boundProperties = Collections.unmodifiableSet(boundProperties);
  }

  private List<ObjectError> convertErrors(ConfigurationPropertyName name,
          Set<ConfigurationProperty> boundProperties, List<ObjectError> errors) {
    ArrayList<ObjectError> converted = new ArrayList<>(errors.size());
    for (ObjectError error : errors) {
      converted.add(convertError(name, boundProperties, error));
    }
    return Collections.unmodifiableList(converted);
  }

  private ObjectError convertError(ConfigurationPropertyName name,
          Set<ConfigurationProperty> boundProperties, ObjectError error) {
    if (error instanceof FieldError) {
      return convertFieldError(name, boundProperties, (FieldError) error);
    }
    return error;
  }

  private FieldError convertFieldError(ConfigurationPropertyName name,
          Set<ConfigurationProperty> boundProperties, FieldError error) {
    if (error instanceof OriginProvider) {
      return error;
    }
    return OriginTrackedFieldError.of(error, findFieldErrorOrigin(name, boundProperties, error));
  }

  @Nullable
  private Origin findFieldErrorOrigin(ConfigurationPropertyName name,
          Set<ConfigurationProperty> boundProperties, FieldError error) {
    for (ConfigurationProperty boundProperty : boundProperties) {
      if (isForError(name, boundProperty.getName(), error)) {
        return Origin.from(boundProperty);
      }
    }
    return null;
  }

  private boolean isForError(ConfigurationPropertyName name,
          ConfigurationPropertyName boundPropertyName, FieldError error) {
    return name.isParentOf(boundPropertyName)
            && boundPropertyName.getLastElement(ConfigurationPropertyName.Form.UNIFORM).equalsIgnoreCase(error.getField());
  }

  /**
   * Return the name of the item that was being validated.
   *
   * @return the name of the item
   */
  public ConfigurationPropertyName getName() {
    return this.name;
  }

  /**
   * Return the properties that were bound before validation failed.
   *
   * @return the boundProperties
   */
  public Set<ConfigurationProperty> getBoundProperties() {
    return this.boundProperties;
  }

  public boolean hasErrors() {
    return !this.errors.isEmpty();
  }

  /**
   * Return the list of all validation errors.
   *
   * @return the errors
   */
  public List<ObjectError> getAllErrors() {
    return this.errors;
  }

  @Override
  public Iterator<ObjectError> iterator() {
    return this.errors.iterator();
  }

}
