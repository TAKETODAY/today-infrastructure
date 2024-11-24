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

package infra.core.ssl.pem;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.List;

import infra.core.io.ClassPathResource;
import infra.core.ssl.pem.PemCertificateParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PemCertificateParser}.
 *
 * @author Scott Frederick
 */
class PemCertificateParserTests {

  @Test
  void parseCertificate() throws Exception {
    List<X509Certificate> certificates = PemCertificateParser.parse(read("ssl/test-cert.pem"));
    assertThat(certificates).isNotNull();
    assertThat(certificates).hasSize(1);
    assertThat(certificates.get(0).getType()).isEqualTo("X.509");
  }

  @Test
  void parseCertificateChain() throws Exception {
    List<X509Certificate> certificates = PemCertificateParser.parse(read("ssl/test-cert-chain.pem"));
    assertThat(certificates).isNotNull();
    assertThat(certificates).hasSize(2);
    assertThat(certificates.get(0).getType()).isEqualTo("X.509");
    assertThat(certificates.get(1).getType()).isEqualTo("X.509");
  }

  private String read(String path) throws IOException {
    return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
  }

}
