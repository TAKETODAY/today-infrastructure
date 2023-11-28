/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StreamUtils;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Represent the Content-Disposition type and parameters as defined in RFC 6266.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author TODAY <br>
 * 2020-01-31 12:37
 * @see <a href="https://tools.ietf.org/html/rfc6266">RFC 6266</a>
 */
public final class ContentDisposition {

  private final static Pattern BASE64_ENCODED_PATTERN =
          Pattern.compile("=\\?([0-9a-zA-Z-_]+)\\?B\\?([+/0-9a-zA-Z]+=*)\\?=");

  private final static Pattern QUOTED_PRINTABLE_ENCODED_PATTERN =
          Pattern.compile("=\\?([0-9a-zA-Z-_]+)\\?Q\\?([!->@-~]+)\\?="); // Printable ASCII other than "?" or SPACE

  private static final String INVALID_HEADER_FIELD_PARAMETER_FORMAT =
          "Invalid header field parameter format (as defined in RFC 5987)";

  @Nullable
  private final String type;

  @Nullable
  private final String name;

  @Nullable
  private final String filename;

  @Nullable
  private final Charset charset;

  @Nullable
  private final Long size;

  @Nullable
  private final ZonedDateTime creationDate;

  @Nullable
  private final ZonedDateTime modificationDate;

  @Nullable
  private final ZonedDateTime readDate;

  /**
   * Private constructor. See static factory methods in this class.
   */
  private ContentDisposition(@Nullable String type, @Nullable String name, @Nullable String filename,
          @Nullable Charset charset, @Nullable Long size, @Nullable ZonedDateTime creationDate,
          @Nullable ZonedDateTime modificationDate, @Nullable ZonedDateTime readDate) {

    this.type = type;
    this.name = name;
    this.filename = filename;
    this.charset = charset;
    this.size = size;
    this.creationDate = creationDate;
    this.modificationDate = modificationDate;
    this.readDate = readDate;
  }

  /**
   * Return whether the {@link #getType() type} is {@literal "attachment"}.
   */
  public boolean isAttachment() {
    return type != null && type.equalsIgnoreCase("attachment");
  }

  /**
   * Return whether the {@link #getType() type} is {@literal "form-data"}.
   */
  public boolean isFormData() {
    return type != null && type.equalsIgnoreCase("form-data");
  }

  /**
   * Return whether the {@link #getType() type} is {@literal "inline"}.
   */
  public boolean isInline() {
    return type != null && type.equalsIgnoreCase("inline");
  }

  /**
   * Return the disposition type.
   *
   * @see #isAttachment()
   * @see #isFormData()
   * @see #isInline()
   */
  @Nullable
  public String getType() {
    return this.type;
  }

  /**
   * Return the value of the {@literal name} parameter, or {@code null} if not defined.
   */
  @Nullable
  public String getName() {
    return this.name;
  }

  /**
   * Return the value of the {@literal filename} parameter, possibly decoded
   * from BASE64 encoding based on RFC 2047, or of the {@literal filename*}
   * parameter, possibly decoded as defined in the RFC 5987.
   */
  @Nullable
  public String getFilename() {
    return this.filename;
  }

  /**
   * Return the charset defined in {@literal filename*} parameter, or {@code null} if not defined.
   */
  @Nullable
  public Charset getCharset() {
    return this.charset;
  }

  /**
   * Return the value of the {@literal size} parameter, or {@code null} if not defined.
   */
  @Nullable
  public Long getSize() {
    return this.size;
  }

  /**
   * Return the value of the {@literal creation-date} parameter, or {@code null} if not defined.
   */
  @Nullable
  public ZonedDateTime getCreationDate() {
    return this.creationDate;
  }

  /**
   * Return the value of the {@literal modification-date} parameter, or {@code null} if not defined.
   */
  @Nullable
  public ZonedDateTime getModificationDate() {
    return this.modificationDate;
  }

  /**
   * Return the value of the {@literal read-date} parameter, or {@code null} if not defined.
   */
  @Nullable
  public ZonedDateTime getReadDate() {
    return this.readDate;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof ContentDisposition otherCd) {
      return Objects.equals(this.type, otherCd.type)
              && Objects.equals(this.name, otherCd.name)
              && Objects.equals(this.filename, otherCd.filename)
              && Objects.equals(this.charset, otherCd.charset)
              && Objects.equals(this.size, otherCd.size)
              && Objects.equals(this.creationDate, otherCd.creationDate)
              && Objects.equals(this.modificationDate, otherCd.modificationDate)
              && Objects.equals(this.readDate, otherCd.readDate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = ObjectUtils.nullSafeHashCode(this.type);
    result = 31 * result + ObjectUtils.nullSafeHashCode(this.name);
    result = 31 * result + ObjectUtils.nullSafeHashCode(this.filename);
    result = 31 * result + ObjectUtils.nullSafeHashCode(this.charset);
    result = 31 * result + ObjectUtils.nullSafeHashCode(this.size);
    result = 31 * result + (this.creationDate != null ? this.creationDate.hashCode() : 0);
    result = 31 * result + (this.modificationDate != null ? this.modificationDate.hashCode() : 0);
    result = 31 * result + (this.readDate != null ? this.readDate.hashCode() : 0);
    return result;
  }

  /**
   * Return the header value for this content disposition as defined in RFC 6266.
   *
   * @see #parse(String)
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (this.type != null) {
      sb.append(this.type);
    }
    if (this.name != null) {
      sb.append("; name=\"");
      sb.append(this.name).append('\"');
    }
    if (this.filename != null) {
      if (this.charset == null || StandardCharsets.US_ASCII.equals(this.charset)) {
        sb.append("; filename=\"");
        sb.append(encodeQuotedPairs(this.filename)).append('\"');
      }
      else {
        sb.append("; filename=\"");
        sb.append(encodeQuotedPrintableFilename(this.filename, this.charset)).append('\"');
        sb.append("; filename*=");
        sb.append(encodeRfc5987Filename(this.filename, this.charset));
      }
    }
    if (this.size != null) {
      sb.append("; size=");
      sb.append(this.size);
    }
    if (this.creationDate != null) {
      sb.append("; creation-date=\"");
      sb.append(RFC_1123_DATE_TIME.format(this.creationDate));
      sb.append('\"');
    }
    if (this.modificationDate != null) {
      sb.append("; modification-date=\"");
      sb.append(RFC_1123_DATE_TIME.format(this.modificationDate));
      sb.append('\"');
    }
    if (this.readDate != null) {
      sb.append("; read-date=\"");
      sb.append(RFC_1123_DATE_TIME.format(this.readDate));
      sb.append('\"');
    }
    return sb.toString();
  }

  /**
   * Return a builder for a {@code ContentDisposition} of type {@literal "attachment"}.
   */
  public static Builder attachment() {
    return builder("attachment");
  }

  /**
   * Return a builder for a {@code ContentDisposition} of type {@literal "form-data"}.
   */
  public static Builder formData() {
    return builder("form-data");
  }

  /**
   * Return a builder for a {@code ContentDisposition} of type {@literal "inline"}.
   */
  public static Builder inline() {
    return builder("inline");
  }

  /**
   * Return a builder for a {@code ContentDisposition}.
   *
   * @param type the disposition type like for example {@literal inline},
   * {@literal attachment}, or {@literal form-data}
   * @return the builder
   */
  public static Builder builder(String type) {
    return new Builder(type);
  }

  /**
   * Return an empty content disposition.
   */
  public static ContentDisposition empty() {
    return new ContentDisposition("", null, null, null, null, null, null, null);
  }

  /**
   * Parse a {@literal Content-Disposition} header value as defined in RFC 2183.
   *
   * @param contentDisposition the {@literal Content-Disposition} header value
   * @return the parsed content disposition
   * @see #toString()
   */
  public static ContentDisposition parse(String contentDisposition) {
    List<String> parts = tokenize(contentDisposition);
    String type = parts.get(0);
    String name = null;
    String filename = null;
    Charset charset = null;
    Long size = null;
    ZonedDateTime creationDate = null;
    ZonedDateTime modificationDate = null;
    ZonedDateTime readDate = null;
    for (int i = 1; i < parts.size(); i++) {
      String part = parts.get(i);
      int eqIndex = part.indexOf('=');
      if (eqIndex != -1) {
        String attribute = part.substring(0, eqIndex);
        String value = part.startsWith("\"", eqIndex + 1) && part.endsWith("\"")
                       ? part.substring(eqIndex + 2, part.length() - 1)
                       : part.substring(eqIndex + 1);
        if ("name".equals(attribute)) {
          name = value;
        }
        else if ("filename*".equals(attribute)) {
          int idx1 = value.indexOf('\'');
          int idx2 = value.indexOf('\'', idx1 + 1);
          if (idx1 != -1 && idx2 != -1) {
            charset = Charset.forName(value.substring(0, idx1).trim());
            Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
                    "Charset should be UTF-8 or ISO-8859-1");
            filename = decodeRfc5987Filename(value.substring(idx2 + 1), charset);
          }
          else {
            // US ASCII
            filename = decodeRfc5987Filename(value, StandardCharsets.US_ASCII);
          }
        }
        else if ("filename".equals(attribute) && (filename == null)) {
          if (value.startsWith("=?")) {
            Matcher matcher = BASE64_ENCODED_PATTERN.matcher(value);
            if (matcher.find()) {
              Base64.Decoder decoder = Base64.getDecoder();
              StringBuilder builder = new StringBuilder();
              do {
                charset = Charset.forName(matcher.group(1));
                byte[] decoded = decoder.decode(matcher.group(2));
                builder.append(new String(decoded, charset));
              }
              while (matcher.find());

              filename = builder.toString();
            }
            else {
              matcher = QUOTED_PRINTABLE_ENCODED_PATTERN.matcher(value);
              if (matcher.find()) {
                StringBuilder builder = new StringBuilder();
                do {
                  charset = Charset.forName(matcher.group(1));
                  String decoded = decodeQuotedPrintableFilename(matcher.group(2), charset);
                  builder.append(decoded);
                }
                while (matcher.find());

                filename = builder.toString();
              }
              else {
                filename = value;
              }
            }
          }
          else if (value.indexOf('\\') != -1) {
            filename = decodeQuotedPairs(value);
          }
          else {
            filename = value;
          }
        }
        else if ("size".equals(attribute)) {
          size = Long.parseLong(value);
        }
        else if ("creation-date".equals(attribute)) {
          creationDate = parseZonedDateTime(value);
        }
        else if ("modification-date".equals(attribute)) {
          modificationDate = parseZonedDateTime(value);
        }
        else if ("read-date".equals(attribute)) {
          readDate = parseZonedDateTime(value);
        }
      }
      else {
        throw new IllegalArgumentException("Invalid content disposition format");
      }
    }
    return new ContentDisposition(type, name, filename, charset, size, creationDate, modificationDate, readDate);
  }

  private static ZonedDateTime parseZonedDateTime(String value) {
    try {
      return ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
    }
    catch (DateTimeParseException ex) {
      // ignore
    }
    return null;
  }

  private static List<String> tokenize(String headerValue) {
    int index = headerValue.indexOf(';');
    String type = (index >= 0 ? headerValue.substring(0, index) : headerValue).trim();
    if (type.isEmpty()) {
      throw new IllegalArgumentException("Content-Disposition header must not be empty");
    }
    ArrayList<String> parts = new ArrayList<>();
    parts.add(type);
    if (index >= 0) {
      int headerLength = headerValue.length();
      do {
        int nextIndex = index + 1;
        boolean quoted = false;
        boolean escaped = false;
        while (nextIndex < headerLength) {
          char ch = headerValue.charAt(nextIndex);
          if (ch == ';') {
            if (!quoted) {
              break;
            }
          }
          else if (!escaped && ch == '"') {
            quoted = !quoted;
          }
          escaped = (!escaped && ch == '\\');
          nextIndex++;
        }
        String part = headerValue.substring(index + 1, nextIndex).trim();
        if (!part.isEmpty()) {
          parts.add(part);
        }
        index = nextIndex;
      }
      while (index < headerLength);
    }
    return parts;
  }

  /**
   * Decode the given header field param as described in RFC 5987.
   * <p>Only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported.
   *
   * @param filename the filename
   * @param charset the charset for the filename
   * @return the encoded header field param
   * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
   */
  private static String decodeRfc5987Filename(String filename, Charset charset) {
    byte[] value = filename.getBytes(charset);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int index = 0;
    while (index < value.length) {
      byte b = value[index];
      if (isRFC5987AttrChar(b)) {
        baos.write((char) b);
        index++;
      }
      else if (b == '%' && index < value.length - 2) {
        char[] array = new char[] { (char) value[index + 1], (char) value[index + 2] };
        try {
          baos.write(Integer.parseInt(String.valueOf(array), 16));
        }
        catch (NumberFormatException ex) {
          throw new IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT, ex);
        }
        index += 3;
      }
      else {
        throw new IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT);
      }
    }
    return StreamUtils.copyToString(baos, charset);
  }

  private static boolean isRFC5987AttrChar(byte c) {
    return (c >= '0' && c <= '9')
            || (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || c == '!'
            || c == '#'
            || c == '$'
            || c == '&'
            || c == '+'
            || c == '-'
            || c == '.'
            || c == '^'
            || c == '_'
            || c == '`'
            || c == '|'
            || c == '~';
  }

  /**
   * Decode the given header field param as described in RFC 2047.
   *
   * @param filename the filename
   * @param charset the charset for the filename
   * @return the decoded header field param
   * @see <a href="https://tools.ietf.org/html/rfc2047">RFC 2047</a>
   */
  private static String decodeQuotedPrintableFilename(String filename, Charset charset) {
    byte[] value = filename.getBytes(US_ASCII);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int index = 0;
    while (index < value.length) {
      byte b = value[index];
      if (b == '_') {
        baos.write(' ');
        index++;
      }
      else if (b == '=' && index < value.length - 2) {
        int i1 = Character.digit((char) value[index + 1], 16);
        int i2 = Character.digit((char) value[index + 2], 16);
        if (i1 == -1 || i2 == -1) {
          throw new IllegalArgumentException("Not a valid hex sequence: " + filename.substring(index));
        }
        baos.write((i1 << 4) | i2);
        index += 3;
      }
      else {
        baos.write(b);
        index++;
      }
    }
    return StreamUtils.copyToString(baos, charset);
  }

  /**
   * Encode the given header field param as described in RFC 2047.
   *
   * @param filename the filename
   * @param charset the charset for the filename
   * @return the encoded header field param
   * @see <a href="https://tools.ietf.org/html/rfc2047">RFC 2047</a>
   */
  private static String encodeQuotedPrintableFilename(String filename, Charset charset) {
    Assert.notNull(filename, "'filename' is required");
    Assert.notNull(charset, "'charset' is required");

    byte[] source = filename.getBytes(charset);
    StringBuilder sb = new StringBuilder(source.length << 1);
    sb.append("=?");
    sb.append(charset.name());
    sb.append("?Q?");
    for (byte b : source) {
      if (isPrintable(b)) {
        sb.append((char) b);
      }
      else {
        sb.append('=');
        char ch1 = hexDigit(b >> 4);
        char ch2 = hexDigit(b);
        sb.append(ch1);
        sb.append(ch2);
      }
    }
    sb.append("?=");
    return sb.toString();
  }

  private static boolean isPrintable(byte c) {
    return (c >= '!' && c <= '<') || (c >= '>' && c <= '~');
  }

  private static String encodeQuotedPairs(String filename) {
    if (filename.indexOf('"') == -1 && filename.indexOf('\\') == -1) {
      return filename;
    }
    StringBuilder sb = new StringBuilder();
    int length = filename.length();
    for (int i = 0; i < length; i++) {
      char c = filename.charAt(i);
      if (c == '"' || c == '\\') {
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }

  private static String decodeQuotedPairs(String filename) {
    StringBuilder sb = new StringBuilder();
    int length = filename.length();
    for (int i = 0; i < length; i++) {
      char c = filename.charAt(i);
      if (c == '\\' && i + 1 < length) {
        i++;
        char next = filename.charAt(i);
        if (next != '"' && next != '\\') {
          sb.append(c);
        }
        sb.append(next);
      }
      else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * Encode the given header field param as describe in RFC 5987.
   *
   * @param input the header field param
   * @param charset the charset of the header field param string,
   * only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported
   * @return the encoded header field param
   * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
   */
  private static String encodeRfc5987Filename(String input, Charset charset) {
    Assert.isTrue(!StandardCharsets.US_ASCII.equals(charset), "ASCII does not require encoding");
    Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset), "Only UTF-8 and ISO-8859-1 are supported");

    byte[] source = input.getBytes(charset);
    StringBuilder sb = new StringBuilder(source.length << 1);
    sb.append(charset.name());
    sb.append("''");
    for (byte b : source) {
      if (isRFC5987AttrChar(b)) {
        sb.append((char) b);
      }
      else {
        sb.append('%');
        char hex1 = hexDigit(b >> 4);
        char hex2 = hexDigit(b);
        sb.append(hex1);
        sb.append(hex2);
      }
    }
    return sb.toString();
  }

  private static char hexDigit(int b) {
    return Character.toUpperCase(Character.forDigit(b & 0xF, 16));
  }

  /**
   * A mutable builder for {@code ContentDisposition}.
   */
  public static class Builder {

    private final String type;

    @Nullable
    private String name;

    @Nullable
    private String filename;

    @Nullable
    private Charset charset;

    @Nullable
    private Long size;

    @Nullable
    private ZonedDateTime creationDate;

    @Nullable
    private ZonedDateTime modificationDate;

    @Nullable
    private ZonedDateTime readDate;

    public Builder(String type) {
      Assert.hasText(type, "'type' must not be not empty");
      this.type = type;
    }

    /**
     * Set the value of the {@literal name} parameter.
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Set the value of the {@literal filename} parameter. The given
     * filename will be formatted as quoted-string, as defined in RFC 2616,
     * section 2.2, and any quote characters within the filename value will
     * be escaped with a backslash, e.g. {@code "foo\"bar.txt"} becomes
     * {@code "foo\\\"bar.txt"}.
     */
    public Builder filename(String filename) {
      Assert.hasText(filename, "No filename");
      this.filename = filename;
      return this;
    }

    /**
     * Set the value of the {@code filename} that will be encoded as
     * defined in RFC 5987. Only the US-ASCII, UTF-8, and ISO-8859-1
     * charsets are supported.
     * <p><strong>Note:</strong> Do not use this for a
     * {@code "multipart/form-data"} request since
     * <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a>
     * and also RFC 5987 mention it does not apply to multipart requests.
     */
    public Builder filename(String filename, Charset charset) {
      Assert.hasText(filename, "No filename");
      this.filename = filename;
      this.charset = charset;
      return this;
    }

    /**
     * Set the value of the {@literal size} parameter.
     */
    public Builder size(Long size) {
      this.size = size;
      return this;
    }

    /**
     * Set the value of the {@literal creation-date} parameter.
     */
    public Builder creationDate(ZonedDateTime creationDate) {
      this.creationDate = creationDate;
      return this;
    }

    /**
     * Set the value of the {@literal modification-date} parameter.
     */
    public Builder modificationDate(ZonedDateTime modificationDate) {
      this.modificationDate = modificationDate;
      return this;
    }

    /**
     * Set the value of the {@literal read-date} parameter.
     */
    public Builder readDate(ZonedDateTime readDate) {
      this.readDate = readDate;
      return this;
    }

    /**
     * Build the content disposition.
     */
    public ContentDisposition build() {
      return new ContentDisposition(this.type, this.name, this.filename, this.charset,
              this.size, this.creationDate, this.modificationDate, this.readDate);
    }

  }

}
