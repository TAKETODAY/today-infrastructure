/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.bind.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertyName.Form;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginProvider;
import cn.taketoday.validation.FieldError;
import cn.taketoday.validation.ObjectError;

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

  ValidationErrors(
          ConfigurationPropertyName name,
          Set<ConfigurationProperty> boundProperties,
          List<ObjectError> errors) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(boundProperties, "BoundProperties must not be null");
    Assert.notNull(errors, "Errors must not be null");
    this.name = name;
    this.boundProperties = Collections.unmodifiableSet(boundProperties);
    this.errors = convertErrors(name, boundProperties, errors);
  }

  private List<ObjectError> convertErrors(
          ConfigurationPropertyName name,
          Set<ConfigurationProperty> boundProperties,
          List<ObjectError> errors) {
    ArrayList<ObjectError> converted = new ArrayList<>(errors.size());
    for (ObjectError error : errors) {
      converted.add(convertError(name, boundProperties, error));
    }
    return Collections.unmodifiableList(converted);
  }

  private ObjectError convertError(
          ConfigurationPropertyName name, Set<ConfigurationProperty> boundProperties, ObjectError error) {
    if (error instanceof FieldError) {
      return convertFieldError(name, boundProperties, (FieldError) error);
    }
    return error;
  }

  private FieldError convertFieldError(
          ConfigurationPropertyName name,
          Set<ConfigurationProperty> boundProperties, FieldError error) {
    if (error instanceof OriginProvider) {
      return error;
    }
    return OriginTrackedFieldError.of(error, findFieldErrorOrigin(name, boundProperties, error));
  }

  @Nullable
  private Origin findFieldErrorOrigin(
          ConfigurationPropertyName name, Set<ConfigurationProperty> boundProperties, FieldError error) {
    for (ConfigurationProperty boundProperty : boundProperties) {
      if (isForError(name, boundProperty.getName(), error)) {
        return Origin.from(boundProperty);
      }
    }
    return null;
  }

  private boolean isForError(
          ConfigurationPropertyName name, ConfigurationPropertyName boundPropertyName, FieldError error) {
    return name.isParentOf(boundPropertyName)
            && boundPropertyName.getLastElement(Form.UNIFORM).equalsIgnoreCase(error.getField());
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
