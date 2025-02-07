/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.codec.multipart;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import infra.http.HttpHeaders;
import infra.http.HttpMessage;
import infra.http.MediaType;
import infra.lang.Nullable;

/**
 * Various static utility methods for dealing with multipart parsing.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class MultipartUtils {

  @Nullable
  public static byte[] boundary(HttpMessage message, Charset headersCharset) {
    MediaType contentType = message.getHeaders().getContentType();
    if (contentType != null) {
      String boundary = contentType.getParameter("boundary");
      if (boundary != null) {
        int len = boundary.length();
        if (len > 2 && boundary.charAt(0) == '"' && boundary.charAt(len - 1) == '"') {
          boundary = boundary.substring(1, len - 1);
        }
        return boundary.getBytes(headersCharset);
      }
    }
    return null;
  }

  /**
   * Return the character set of the given headers, as defined in the
   * {@link HttpHeaders#getContentType()} header.
   */
  public static Charset charset(HttpHeaders headers) {
    MediaType contentType = headers.getContentType();
    if (contentType != null) {
      Charset charset = contentType.getCharset();
      if (charset != null) {
        return charset;
      }
    }
    return StandardCharsets.UTF_8;
  }

  /**
   * Concatenates the given array of byte arrays.
   */
  public static byte[] concat(byte[]... byteArrays) {
    int len = 0;
    for (byte[] byteArray : byteArrays) {
      len += byteArray.length;
    }
    byte[] result = new byte[len];
    len = 0;
    for (byte[] byteArray : byteArrays) {
      System.arraycopy(byteArray, 0, result, len, byteArray.length);
      len += byteArray.length;
    }
    return result;
  }

  public static void closeChannel(Channel channel) {
    try {
      if (channel.isOpen()) {
        channel.close();
      }
    }
    catch (IOException ignore) {
    }
  }

  public static boolean isFormField(HttpHeaders headers) {
    MediaType contentType = headers.getContentType();
    return (contentType == null || MediaType.TEXT_PLAIN.equalsTypeAndSubtype(contentType))
            && headers.getContentDisposition().getFilename() == null;
  }

  public static void deleteFile(Path file) {
    try {
      Files.delete(file);
    }
    catch (IOException ignore) {
    }
  }

}
