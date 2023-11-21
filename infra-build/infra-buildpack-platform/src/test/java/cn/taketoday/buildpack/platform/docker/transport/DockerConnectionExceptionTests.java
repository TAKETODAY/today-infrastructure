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

package cn.taketoday.buildpack.platform.docker.transport;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DockerEngineException}.
 *
 * @author Scott Frederick
 */
class DockerConnectionExceptionTests {

  private static final String HOST = "docker://localhost/";

  @Test
  void createWhenHostIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new DockerConnectionException(null, null))
            .withMessage("Host is required");
  }

  @Test
  void createWhenCauseIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new DockerConnectionException(HOST, null))
            .withMessage("Cause is required");
  }

  @Test
  void createWithIOException() {
    DockerConnectionException exception = new DockerConnectionException(HOST, new IOException("error"));
    assertThat(exception.getMessage())
            .contains("Connection to the Docker daemon at 'docker://localhost/' failed with error \"error\"");
  }

  @Test
  void createWithLastErrorException() {
    DockerConnectionException exception = new DockerConnectionException(HOST,
            new IOException(new com.sun.jna.LastErrorException("root cause")));
    assertThat(exception.getMessage())
            .contains("Connection to the Docker daemon at 'docker://localhost/' failed with error \"root cause\"");
  }

}
