/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.properties.bind.validation;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.lang.Assert;
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
@SuppressWarnings("NullAway")
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
