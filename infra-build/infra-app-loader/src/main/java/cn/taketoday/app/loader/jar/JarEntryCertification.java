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

package cn.taketoday.app.loader.jar;

import java.security.CodeSigner;
import java.security.cert.Certificate;

/**
 * {@link Certificate} and {@link CodeSigner} details for a {@link JarEntry} from a signed
 * {@link JarFile}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JarEntryCertification {

  static final JarEntryCertification NONE = new JarEntryCertification(null, null);

  private final Certificate[] certificates;

  private final CodeSigner[] codeSigners;

  JarEntryCertification(Certificate[] certificates, CodeSigner[] codeSigners) {
    this.certificates = certificates;
    this.codeSigners = codeSigners;
  }

  Certificate[] getCertificates() {
    return (this.certificates != null) ? this.certificates.clone() : null;
  }

  CodeSigner[] getCodeSigners() {
    return (this.codeSigners != null) ? this.codeSigners.clone() : null;
  }

  static JarEntryCertification from(java.util.jar.JarEntry certifiedEntry) {
    Certificate[] certificates = (certifiedEntry != null) ? certifiedEntry.getCertificates() : null;
    CodeSigner[] codeSigners = (certifiedEntry != null) ? certifiedEntry.getCodeSigners() : null;
    if (certificates == null && codeSigners == null) {
      return NONE;
    }
    return new JarEntryCertification(certificates, codeSigners);
  }

}
