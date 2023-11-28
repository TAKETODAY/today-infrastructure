/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.bind.validation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.testfixture.origin.MockOrigin;
import cn.taketoday.origin.Origin;
import cn.taketoday.validation.FieldError;
import cn.taketoday.validation.ObjectError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ValidationErrors}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ValidationErrorsTests {

  private static final ConfigurationPropertyName NAME = ConfigurationPropertyName.of("foo");

  @Test
  void createWhenNameIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ValidationErrors(null, Collections.emptySet(), Collections.emptyList()))
            .withMessageContaining("Name is required");
  }

  @Test
  void createWhenBoundPropertiesIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ValidationErrors(NAME, null, Collections.emptyList()))
            .withMessageContaining("BoundProperties is required");
  }

  @Test
  void createWhenErrorsIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ValidationErrors(NAME, Collections.emptySet(), null))
            .withMessageContaining("Errors is required");
  }

  @Test
  void getNameShouldReturnName() {
    ConfigurationPropertyName name = NAME;
    ValidationErrors errors = new ValidationErrors(name, Collections.emptySet(), Collections.emptyList());
    assertThat((Object) errors.getName()).isEqualTo(name);
  }

  @Test
  void getBoundPropertiesShouldReturnBoundProperties() {
    Set<ConfigurationProperty> boundProperties = new LinkedHashSet<>();
    boundProperties.add(new ConfigurationProperty(NAME, "foo", null));
    ValidationErrors errors = new ValidationErrors(NAME, boundProperties, Collections.emptyList());
    assertThat(errors.getBoundProperties()).isEqualTo(boundProperties);
  }

  @Test
  void getErrorsShouldReturnErrors() {
    List<ObjectError> allErrors = new ArrayList<>();
    allErrors.add(new ObjectError("foo", "bar"));
    ValidationErrors errors = new ValidationErrors(NAME, Collections.emptySet(), allErrors);
    assertThat(errors.getAllErrors()).isEqualTo(allErrors);
  }

  @Test
  void iteratorShouldIterateErrors() {
    List<ObjectError> allErrors = new ArrayList<>();
    allErrors.add(new ObjectError("foo", "bar"));
    ValidationErrors errors = new ValidationErrors(NAME, Collections.emptySet(), allErrors);
    assertThat(errors.iterator()).toIterable().containsExactlyElementsOf(allErrors);
  }

  @Test
  void getErrorsShouldAdaptFieldErrorsToBeOriginProviders() {
    Set<ConfigurationProperty> boundProperties = new LinkedHashSet<>();
    ConfigurationPropertyName name1 = ConfigurationPropertyName.of("foo.bar");
    Origin origin1 = MockOrigin.of("line1");
    boundProperties.add(new ConfigurationProperty(name1, "boot", origin1));
    ConfigurationPropertyName name2 = ConfigurationPropertyName.of("foo.baz.bar");
    Origin origin2 = MockOrigin.of("line2");
    boundProperties.add(new ConfigurationProperty(name2, "boot", origin2));
    List<ObjectError> allErrors = new ArrayList<>();
    allErrors.add(new FieldError("objectname", "bar", "message"));
    ValidationErrors errors = new ValidationErrors(ConfigurationPropertyName.of("foo.baz"), boundProperties,
            allErrors);
    assertThat(Origin.from(errors.getAllErrors().get(0))).isEqualTo(origin2);
  }

}
