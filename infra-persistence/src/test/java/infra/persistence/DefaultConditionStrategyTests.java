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

package infra.persistence;

import org.junit.jupiter.api.Test;

import infra.persistence.sql.Restrictions;
import infra.persistence.support.PropertyCondition;

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

    assertThat(strategy.resolve(entityMetadata, name, "  ", ValueNormalizer.DEFAULT)).isNull();
    assertThat(strategy.resolve(entityMetadata, name, "\n", ValueNormalizer.DEFAULT)).isNull();
    assertThat(strategy.resolve(entityMetadata, name, "\t\n\r", ValueNormalizer.DEFAULT)).isNull();
    assertThat(strategy.resolve(entityMetadata, name, "\t\n\r ", ValueNormalizer.DEFAULT)).isNull();
    assertThat(strategy.resolve(entityMetadata, name, " ", ValueNormalizer.DEFAULT)).isNull();

    PropertyCondition condition =
            (PropertyCondition) strategy.resolve(entityMetadata, name, "name", ValueNormalizer.DEFAULT);
    assertThat(condition).isNotNull();

    assertThat(condition.entityProperty).isSameAs(name);
    assertThat(condition.value).isEqualTo("name");
    assertThat(condition.restriction).isEqualTo(Restrictions.equal("name"));

  }

  @Test
  void normal() {
    EntityMetadataFactory factory = new DefaultEntityMetadataFactory();
    EntityMetadata entityMetadata = factory.getEntityMetadata(Model.class);

    EntityProperty number = entityMetadata.findProperty("number");
    assertThat(number).isNotNull();

    PropertyCondition condition =
            (PropertyCondition) strategy.resolve(entityMetadata, number, 2, ValueNormalizer.DEFAULT);
    assertThat(condition).isNotNull();

    assertThat(condition.entityProperty).isSameAs(number);
    assertThat(condition.value).isEqualTo(2);
    assertThat(condition.restriction).isEqualTo(Restrictions.equal("number"));
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