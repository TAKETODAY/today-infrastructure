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

package infra.jdbc.init;

import javax.sql.DataSource;

import infra.beans.factory.InitializingBean;
import infra.core.io.Resource;
import infra.jdbc.config.EmbeddedDatabaseConnection;
import infra.jdbc.datasource.init.ResourceDatabasePopulator;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.sql.init.AbstractScriptDatabaseInitializer;
import infra.sql.init.DatabaseInitializationSettings;

/**
 * {@link InitializingBean} that performs {@link DataSource} initialization using schema
 * (DDL) and data (DML) scripts.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class DataSourceScriptDatabaseInitializer extends AbstractScriptDatabaseInitializer {

  private static final Logger logger = LoggerFactory.getLogger(DataSourceScriptDatabaseInitializer.class);

  private final DataSource dataSource;

  /**
   * Creates a new {@link DataSourceScriptDatabaseInitializer} that will initialize the
   * given {@code DataSource} using the given settings.
   *
   * @param dataSource data source to initialize
   * @param settings the initialization settings
   */
  public DataSourceScriptDatabaseInitializer(DataSource dataSource, DatabaseInitializationSettings settings) {
    super(settings);
    this.dataSource = dataSource;
  }

  /**
   * Returns the {@code DataSource} that will be initialized.
   *
   * @return the initialization data source
   */
  protected final DataSource getDataSource() {
    return this.dataSource;
  }

  @Override
  protected boolean isEmbeddedDatabase() {
    try {
      return EmbeddedDatabaseConnection.isEmbedded(this.dataSource);
    }
    catch (Exception ex) {
      logger.debug("Could not determine if datasource is embedded", ex);
      return false;
    }
  }

  @Override
  protected void runScripts(Scripts scripts) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setContinueOnError(scripts.isContinueOnError());
    populator.setSeparator(scripts.getSeparator());
    if (scripts.getEncoding() != null) {
      populator.setSqlScriptEncoding(scripts.getEncoding().name());
    }
    for (Resource resource : scripts) {
      populator.addScript(resource);
    }
    customize(populator);
    populator.execute(dataSource);
  }

  /**
   * Customize the {@link ResourceDatabasePopulator}.
   *
   * @param populator the configured database populator
   * @since 2.6.2
   */
  protected void customize(ResourceDatabasePopulator populator) {

  }

}
