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

package cn.taketoday.buildpack.platform.docker.ssl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link CertificateParser}.
 *
 * @author Scott Frederick
 */
class CertificateParserTests {

  private PemFileWriter fileWriter;

  @BeforeEach
  void setUp() throws IOException {
    this.fileWriter = new PemFileWriter();
  }

  @AfterEach
  void tearDown() throws IOException {
    this.fileWriter.cleanup();
  }

  @Test
  void parseCertificates() throws IOException {
    Path caPath = this.fileWriter.writeFile("ca.pem", PemFileWriter.CA_CERTIFICATE);
    Path certPath = this.fileWriter.writeFile("cert.pem", PemFileWriter.CERTIFICATE);
    X509Certificate[] certificates = CertificateParser.parse(caPath, certPath);
    assertThat(certificates).isNotNull();
    assertThat(certificates).hasSize(2);
    assertThat(certificates[0].getType()).isEqualTo("X.509");
    assertThat(certificates[1].getType()).isEqualTo("X.509");
  }

  @Test
  void parseCertificateChain() throws IOException {
    Path path = this.fileWriter.writeFile("ca.pem", PemFileWriter.CA_CERTIFICATE, PemFileWriter.CERTIFICATE);
    X509Certificate[] certificates = CertificateParser.parse(path);
    assertThat(certificates).isNotNull();
    assertThat(certificates).hasSize(2);
    assertThat(certificates[0].getType()).isEqualTo("X.509");
    assertThat(certificates[1].getType()).isEqualTo("X.509");
  }

  @Test
  void parseWithInvalidPathWillThrowException() throws URISyntaxException {
    Path path = Paths.get(new URI("file:///bad/path/cert.pem"));
    assertThatIllegalStateException().isThrownBy(() -> CertificateParser.parse(path))
            .withMessageContaining(path.toString());
  }

}
