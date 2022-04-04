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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.testcontainers;

import org.testcontainers.utility.DockerImageName;

/**
 * Create {@link DockerImageName} instances for services used in integration tests.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public final class DockerImageNames {

  private static final String CASSANDRA_VERSION = "3.11.10";

  private static final String COUCHBASE_VERSION = "6.5.1";

  private static final String MONGO_VERSION = "4.0.23";

  private static final String NEO4J_VERSION = "4.0";

  private static final String POSTGRESQL_VERSION = "14.0";

  private static final String REDIS_VERSION = "4.0.14";

  private static final String REGISTRY_VERSION = "2.7.1";

  private DockerImageNames() {

  }

  /**
   * Return a {@link DockerImageName} suitable for running Cassandra.
   *
   * @return a docker image name for running cassandra
   */
  public static DockerImageName cassandra() {
    return DockerImageName.parse("cassandra").withTag(CASSANDRA_VERSION);
  }

  /**
   * Return a {@link DockerImageName} suitable for running Couchbase.
   *
   * @return a docker image name for running couchbase
   */
  public static DockerImageName couchbase() {
    return DockerImageName.parse("couchbase/server").withTag(COUCHBASE_VERSION);
  }

  /**
   * Return a {@link DockerImageName} suitable for running Elasticsearch according to
   * the version available on the classpath.
   *
   * @return a docker image name for running elasticsearch
   */
  public static DockerImageName elasticsearch() {
    String version = org.elasticsearch.Version.CURRENT.toString();
    return DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch").withTag(version);
  }

  /**
   * Return a {@link DockerImageName} suitable for running Mongo.
   *
   * @return a docker image name for running mongo
   */
  public static DockerImageName mongo() {
    return DockerImageName.parse("mongo").withTag(MONGO_VERSION);
  }

  /**
   * Return a {@link DockerImageName} suitable for running Neo4j.
   *
   * @return a docker image name for running neo4j
   */
  public static DockerImageName neo4j() {
    return DockerImageName.parse("neo4j").withTag(NEO4J_VERSION);
  }

  /**
   * Return a {@link DockerImageName} suitable for running PostgreSQL.
   *
   * @return a docker image name for running postgresql
   */
  public static DockerImageName postgresql() {
    return DockerImageName.parse("postgres").withTag(POSTGRESQL_VERSION);
  }

  /**
   * Return a {@link DockerImageName} suitable for running Redis.
   *
   * @return a docker image name for running redis
   */
  public static DockerImageName redis() {
    return DockerImageName.parse("redis").withTag(REDIS_VERSION);
  }

  /**
   * Return a {@link DockerImageName} suitable for running a Docker registry.
   *
   * @return a docker image name for running a registry
   * @since 4.0
   */
  public static DockerImageName registry() {
    return DockerImageName.parse("registry").withTag(REGISTRY_VERSION);
  }

}
