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

package cn.taketoday.buildpack.platform.docker.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import cn.taketoday.buildpack.platform.json.MappedObject;
import cn.taketoday.buildpack.platform.json.SharedObjectMapper;
import cn.taketoday.buildpack.platform.system.Environment;

/**
 * Docker configuration stored in metadata files managed by the Docker CLI.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DockerConfigurationMetadata {

  private static final String DOCKER_CONFIG = "DOCKER_CONFIG";

  private static final String DEFAULT_CONTEXT = "default";

  private static final String CONFIG_DIR = ".docker";

  private static final String CONTEXTS_DIR = "contexts";

  private static final String META_DIR = "meta";

  private static final String TLS_DIR = "tls";

  private static final String DOCKER_ENDPOINT = "docker";

  private static final String CONFIG_FILE_NAME = "config.json";

  private static final String CONTEXT_FILE_NAME = "meta.json";

  private final String configLocation;

  private final DockerConfig config;

  private final DockerContext context;

  private DockerConfigurationMetadata(String configLocation, DockerConfig config, DockerContext context) {
    this.configLocation = configLocation;
    this.config = config;
    this.context = context;
  }

  DockerConfig getConfiguration() {
    return this.config;
  }

  DockerContext getContext() {
    return this.context;
  }

  DockerContext forContext(String context) {
    return createDockerContext(this.configLocation, context);
  }

  static DockerConfigurationMetadata from(Environment environment) {
    String configLocation = (environment.get(DOCKER_CONFIG) != null) ? environment.get(DOCKER_CONFIG)
                                                                     : Path.of(System.getProperty("user.home"), CONFIG_DIR).toString();
    DockerConfig dockerConfig = createDockerConfig(configLocation);
    DockerContext dockerContext = createDockerContext(configLocation, dockerConfig.getCurrentContext());
    return new DockerConfigurationMetadata(configLocation, dockerConfig, dockerContext);
  }

  private static DockerConfig createDockerConfig(String configLocation) {
    Path path = Path.of(configLocation, CONFIG_FILE_NAME);
    if (!path.toFile().exists()) {
      return DockerConfig.empty();
    }
    try {
      return DockerConfig.fromJson(readPathContent(path));
    }
    catch (JsonProcessingException ex) {
      throw new IllegalStateException("Error parsing Docker configuration file '" + path + "'", ex);
    }
  }

  private static DockerContext createDockerContext(String configLocation, String currentContext) {
    if (currentContext == null || DEFAULT_CONTEXT.equals(currentContext)) {
      return DockerContext.empty();
    }
    Path metaPath = Path.of(configLocation, CONTEXTS_DIR, META_DIR, asHash(currentContext), CONTEXT_FILE_NAME);
    Path tlsPath = Path.of(configLocation, CONTEXTS_DIR, TLS_DIR, asHash(currentContext), DOCKER_ENDPOINT);
    if (!metaPath.toFile().exists()) {
      throw new IllegalArgumentException("Docker context '" + currentContext + "' does not exist");
    }
    try {
      DockerContext context = DockerContext.fromJson(readPathContent(metaPath));
      if (tlsPath.toFile().isDirectory()) {
        return context.withTlsPath(tlsPath.toString());
      }
      return context;
    }
    catch (JsonProcessingException ex) {
      throw new IllegalStateException("Error parsing Docker context metadata file '" + metaPath + "'", ex);
    }
  }

  private static String asHash(String currentContext) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(currentContext.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    }
    catch (NoSuchAlgorithmException ex) {
      return null;
    }
  }

  private static String readPathContent(Path path) {
    try {
      return Files.readString(path);
    }
    catch (IOException ex) {
      throw new IllegalStateException("Error reading Docker configuration file '" + path + "'", ex);
    }
  }

  static final class DockerConfig extends MappedObject {

    private final String currentContext;

    private DockerConfig(JsonNode node) {
      super(node, MethodHandles.lookup());
      this.currentContext = valueAt("/currentContext", String.class);
    }

    String getCurrentContext() {
      return this.currentContext;
    }

    static DockerConfig fromJson(String json) throws JsonProcessingException {
      return new DockerConfig(SharedObjectMapper.get().readTree(json));
    }

    static DockerConfig empty() {
      return new DockerConfig(NullNode.instance);
    }

  }

  static final class DockerContext extends MappedObject {

    private final String dockerHost;

    private final Boolean skipTlsVerify;

    private final String tlsPath;

    private DockerContext(JsonNode node, String tlsPath) {
      super(node, MethodHandles.lookup());
      this.dockerHost = valueAt("/Endpoints/" + DOCKER_ENDPOINT + "/Host", String.class);
      this.skipTlsVerify = valueAt("/Endpoints/" + DOCKER_ENDPOINT + "/SkipTLSVerify", Boolean.class);
      this.tlsPath = tlsPath;
    }

    String getDockerHost() {
      return this.dockerHost;
    }

    Boolean isTlsVerify() {
      return this.skipTlsVerify != null && !this.skipTlsVerify;
    }

    String getTlsPath() {
      return this.tlsPath;
    }

    DockerContext withTlsPath(String tlsPath) {
      return new DockerContext(this.getNode(), tlsPath);
    }

    static DockerContext fromJson(String json) throws JsonProcessingException {
      return new DockerContext(SharedObjectMapper.get().readTree(json), null);
    }

    static DockerContext empty() {
      return new DockerContext(NullNode.instance, null);
    }

  }

}
