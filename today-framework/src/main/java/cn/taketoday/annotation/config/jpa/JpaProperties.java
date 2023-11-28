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

package cn.taketoday.annotation.config.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.orm.jpa.vendor.Database;

/**
 * External configuration properties for a JPA EntityManagerFactory created by Infra.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "jpa")
public class JpaProperties {

  /**
   * Additional native properties to set on the JPA provider.
   */
  private Map<String, String> properties = new HashMap<>();

  /**
   * Mapping resources (equivalent to "mapping-file" entries in persistence.xml).
   */
  private final List<String> mappingResources = new ArrayList<>();

  /**
   * Name of the target database to operate on, auto-detected by default. Can be
   * alternatively set using the "Database" enum.
   */
  private String databasePlatform;

  /**
   * Target database to operate on, auto-detected by default. Can be alternatively set
   * using the "databasePlatform" property.
   */
  private Database database;

  /**
   * Whether to initialize the schema on startup.
   */
  private boolean generateDdl = false;

  /**
   * Whether to enable logging of SQL statements.
   */
  private boolean showSql = false;

  /**
   * Register OpenEntityManagerInViewInterceptor. Binds a JPA EntityManager to the
   * thread for the entire processing of the request.
   */
  private Boolean openInView;

  public Map<String, String> getProperties() {
    return this.properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public List<String> getMappingResources() {
    return this.mappingResources;
  }

  public String getDatabasePlatform() {
    return this.databasePlatform;
  }

  public void setDatabasePlatform(String databasePlatform) {
    this.databasePlatform = databasePlatform;
  }

  public Database getDatabase() {
    return this.database;
  }

  public void setDatabase(Database database) {
    this.database = database;
  }

  public boolean isGenerateDdl() {
    return this.generateDdl;
  }

  public void setGenerateDdl(boolean generateDdl) {
    this.generateDdl = generateDdl;
  }

  public boolean isShowSql() {
    return this.showSql;
  }

  public void setShowSql(boolean showSql) {
    this.showSql = showSql;
  }

  public Boolean getOpenInView() {
    return this.openInView;
  }

  public void setOpenInView(Boolean openInView) {
    this.openInView = openInView;
  }

}
