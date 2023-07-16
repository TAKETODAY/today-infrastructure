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

import javax.net.ssl.SSLContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslContextFactory}.
 *
 * @author Scott Frederick
 */
class SslContextFactoryTests {

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
  void createKeyStoreWithCertChain() throws IOException {
    this.fileWriter.writeFile("cert.pem", PemFileWriter.CERTIFICATE);
    this.fileWriter.writeFile("key.pem", PemFileWriter.PRIVATE_RSA_KEY);
    this.fileWriter.writeFile("ca.pem", PemFileWriter.CA_CERTIFICATE);
    SSLContext sslContext = new SslContextFactory().forDirectory(this.fileWriter.getTempDir().toString());
    assertThat(sslContext).isNotNull();
  }

}
