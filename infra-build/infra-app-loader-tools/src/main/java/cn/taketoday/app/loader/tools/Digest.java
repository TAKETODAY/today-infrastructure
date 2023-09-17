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

package cn.taketoday.app.loader.tools;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class used to calculate digests.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class Digest {

  /**
   * Return the SHA-1 digest from the supplied stream.
   *
   * @param supplier the stream supplier
   * @return the SHA-1 digest
   * @throws IOException on IO error
   */
  static String sha1(InputStreamSupplier supplier) throws IOException {
    try {
      try (DigestInputStream inputStream = new DigestInputStream(supplier.openStream(),
              MessageDigest.getInstance("SHA-1"))) {
        inputStream.readAllBytes();
        return HexFormat.of().formatHex(inputStream.getMessageDigest().digest());
      }
    }
    catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

}
