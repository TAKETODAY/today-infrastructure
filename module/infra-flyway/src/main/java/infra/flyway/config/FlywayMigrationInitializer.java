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

package infra.flyway.config;

import org.flywaydb.core.Flyway;
import org.jspecify.annotations.Nullable;

import infra.beans.factory.InitializingBean;
import infra.core.Ordered;
import infra.core.OrderedSupport;
import infra.lang.Assert;

/**
 * {@link InitializingBean} used to trigger {@link Flyway} migration through the
 * {@link FlywayMigrationStrategy}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class FlywayMigrationInitializer extends OrderedSupport implements InitializingBean, Ordered {

  private final Flyway flyway;

  private final @Nullable FlywayMigrationStrategy migrationStrategy;

  /**
   * Create a new {@link FlywayMigrationInitializer} instance.
   *
   * @param flyway the flyway instance
   */
  public FlywayMigrationInitializer(Flyway flyway) {
    this(flyway, null);
  }

  /**
   * Create a new {@link FlywayMigrationInitializer} instance.
   *
   * @param flyway the flyway instance
   * @param migrationStrategy the migration strategy or {@code null}
   */
  public FlywayMigrationInitializer(Flyway flyway, @Nullable FlywayMigrationStrategy migrationStrategy) {
    Assert.notNull(flyway, "'flyway' is required");
    this.flyway = flyway;
    this.migrationStrategy = migrationStrategy;
  }

  @Override
  public void afterPropertiesSet() {
    if (migrationStrategy != null) {
      migrationStrategy.migrate(flyway);
    }
    else {
      flyway.migrate();
    }
  }

}
