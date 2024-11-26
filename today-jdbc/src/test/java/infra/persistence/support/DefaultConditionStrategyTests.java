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

package infra.persistence.support;

import org.junit.jupiter.api.Test;

import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityMetadataFactory;
import infra.persistence.EntityProperty;
import infra.persistence.sql.Restriction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/10/12 19:24
 */
class DefaultConditionStrategyTests {

  DefaultConditionStrategy strategy = new DefaultConditionStrategy();

  @Test
  void blank() {
    EntityMetadataFactory factory = new DefaultEntityMetadataFactory();
    EntityMetadata entityMetadata = factory.getEntityMetadata(Model.class);

    EntityProperty name = entityMetadata.findProperty("name");
    assertThat(name).isNotNull();

    assertThat(strategy.resolve(name, "  ")).isNull();
    assertThat(strategy.resolve(name, "\n")).isNull();
    assertThat(strategy.resolve(name, "\t\n\r")).isNull();
    assertThat(strategy.resolve(name, "\t\n\r ")).isNull();
    assertThat(strategy.resolve(name, " ")).isNull();

    var condition = strategy.resolve(name, "name");
    assertThat(condition).isNotNull();

    assertThat(condition.entityProperty).isSameAs(name);
    assertThat(condition.propertyValue).isEqualTo("name");
    assertThat(condition.restriction).isEqualTo(Restriction.equal("name"));

  }

  @Test
  void normal() {
    EntityMetadataFactory factory = new DefaultEntityMetadataFactory();
    EntityMetadata entityMetadata = factory.getEntityMetadata(Model.class);

    EntityProperty number = entityMetadata.findProperty("number");
    assertThat(number).isNotNull();


    var condition = strategy.resolve(number, 2);
    assertThat(condition).isNotNull();

    assertThat(condition.entityProperty).isSameAs(number);
    assertThat(condition.propertyValue).isEqualTo(2);
    assertThat(condition.restriction).isEqualTo(Restriction.equal("number"));
  }



  static class Model {

    public final int number;

    public final String name;

    Model(int number, String name) {
      this.number = number;
      this.name = name;
    }
  }

}