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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DockerEngineException}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class DockerEngineExceptionTests {

  private static final String HOST = "docker://localhost/";

  private static final URI URI;

  static {
    try {
      URI = new URI("example");
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static final Errors NO_ERRORS = new Errors(Collections.emptyList());

  private static final Errors ERRORS = new Errors(Collections.singletonList(new Errors.Error("code", "message")));

  private static final Message NO_MESSAGE = new Message(null);

  private static final Message MESSAGE = new Message("response message");

  @Test
  void createWhenHostIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DockerEngineException(null, null, 404, null, NO_ERRORS, NO_MESSAGE))
            .withMessage("Host must not be null");
  }

  @Test
  void createWhenUriIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DockerEngineException(HOST, null, 404, null, NO_ERRORS, NO_MESSAGE))
            .withMessage("URI must not be null");
  }

  @Test
  void create() {
    DockerEngineException exception = new DockerEngineException(HOST, URI, 404, "missing", ERRORS, MESSAGE);
    assertThat(exception.getMessage()).isEqualTo(
            "Docker API call to 'docker://localhost/example' failed with status code 404 \"missing\" and message \"response message\" [code: message]");
    assertThat(exception.getStatusCode()).isEqualTo(404);
    assertThat(exception.getReasonPhrase()).isEqualTo("missing");
    assertThat(exception.getErrors()).isSameAs(ERRORS);
    assertThat(exception.getResponseMessage()).isSameAs(MESSAGE);
  }

  @Test
  void createWhenReasonPhraseIsNull() {
    DockerEngineException exception = new DockerEngineException(HOST, URI, 404, null, ERRORS, MESSAGE);
    assertThat(exception.getMessage()).isEqualTo(
            "Docker API call to 'docker://localhost/example' failed with status code 404 and message \"response message\" [code: message]");
    assertThat(exception.getStatusCode()).isEqualTo(404);
    assertThat(exception.getReasonPhrase()).isNull();
    assertThat(exception.getErrors()).isSameAs(ERRORS);
    assertThat(exception.getResponseMessage()).isSameAs(MESSAGE);
  }

  @Test
  void createWhenErrorsIsNull() {
    DockerEngineException exception = new DockerEngineException(HOST, URI, 404, "missing", null, MESSAGE);
    assertThat(exception.getMessage()).isEqualTo(
            "Docker API call to 'docker://localhost/example' failed with status code 404 \"missing\" and message \"response message\"");
    assertThat(exception.getErrors()).isNull();
  }

  @Test
  void createWhenErrorsIsEmpty() {
    DockerEngineException exception = new DockerEngineException(HOST, URI, 404, "missing", NO_ERRORS, MESSAGE);
    assertThat(exception.getMessage()).isEqualTo(
            "Docker API call to 'docker://localhost/example' failed with status code 404 \"missing\" and message \"response message\"");
    assertThat(exception.getStatusCode()).isEqualTo(404);
    assertThat(exception.getReasonPhrase()).isEqualTo("missing");
    assertThat(exception.getErrors()).isSameAs(NO_ERRORS);
  }

  @Test
  void createWhenMessageIsNull() {
    DockerEngineException exception = new DockerEngineException(HOST, URI, 404, "missing", ERRORS, null);
    assertThat(exception.getMessage()).isEqualTo(
            "Docker API call to 'docker://localhost/example' failed with status code 404 \"missing\" [code: message]");
    assertThat(exception.getResponseMessage()).isNull();
  }

  @Test
  void createWhenMessageIsEmpty() {
    DockerEngineException exception = new DockerEngineException(HOST, URI, 404, "missing", ERRORS, NO_MESSAGE);
    assertThat(exception.getMessage()).isEqualTo(
            "Docker API call to 'docker://localhost/example' failed with status code 404 \"missing\" [code: message]");
    assertThat(exception.getResponseMessage()).isSameAs(NO_MESSAGE);
  }

}
