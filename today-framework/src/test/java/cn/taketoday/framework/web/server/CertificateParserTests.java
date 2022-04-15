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

package cn.taketoday.framework.web.server;

import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link CertificateParser}.
 *
 * @author Scott Frederick
 */
class CertificateParserTests {

  @Test
  void parseCertificate() {
    X509Certificate[] certificates = CertificateParser.parse("classpath:test-cert.pem");
    assertThat(certificates).isNotNull();
    assertThat(certificates.length).isEqualTo(1);
    assertThat(certificates[0].getType()).isEqualTo("X.509");
  }

  @Test
  void parseCertificateChain() {
    X509Certificate[] certificates = CertificateParser.parse("classpath:test-cert-chain.pem");
    assertThat(certificates).isNotNull();
    assertThat(certificates.length).isEqualTo(2);
    assertThat(certificates[0].getType()).isEqualTo("X.509");
    assertThat(certificates[1].getType()).isEqualTo("X.509");
  }

  @Test
  void parseWithInvalidPathWillThrowException() {
    String path = "file:///bad/path/cert.pem";
    assertThatIllegalStateException().isThrownBy(() -> CertificateParser.parse("file:///bad/path/cert.pem"))
            .withMessageContaining(path);
  }

}
