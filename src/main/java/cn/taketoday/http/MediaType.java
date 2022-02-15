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
package cn.taketoday.http;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.InvalidMediaTypeException;
import cn.taketoday.util.InvalidMimeTypeException;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.StringUtils;

import static java.util.Collections.singletonMap;

/**
 * A subclass of {@link MimeType} that adds support for quality parameters as
 * defined in the HTTP specification.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Kazuki Shimizu
 * @author Sam Brannen
 * @author TODAY <br>
 * 2019-12-08 20:02
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.1"> HTTP 1.1:
 * Semantics and Content, section 3.1.1.1</a>
 * @since 2.1.7
 */
public class MediaType extends MimeType implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Public constant media type that includes all media ranges (i.e.
   * "&#42;/&#42;").
   */
  public static final MediaType ALL = new MediaType("*", "*");

  /** A String equivalent of {@link MediaType#ALL}. */
  public static final String ALL_VALUE = "*/*";

  /** Public constant media type for {@code application/atom+xml}. */
  public static final MediaType APPLICATION_ATOM_XML = new MediaType("application", "atom+xml");

  /** A String equivalent of {@link MediaType#APPLICATION_ATOM_XML}. */
  public static final String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

  /** Public constant media type for {@code application/cbor}. */
  public static final MediaType APPLICATION_CBOR = new MediaType("application", "cbor");

  /** A String equivalent of {@link MediaType#APPLICATION_CBOR}. */
  public static final String APPLICATION_CBOR_VALUE = "application/cbor";

  /** Public constant media type for {@code application/x-www-form-urlencoded}. */
  public static final MediaType APPLICATION_FORM_URLENCODED = new MediaType("application", "x-www-form-urlencoded");

  /** A String equivalent of {@link MediaType#APPLICATION_FORM_URLENCODED}. */
  public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

  /** Public constant media type for {@code application/json}. */
  public static final MediaType APPLICATION_JSON = new MediaType("application", "json");

  /**
   * A String equivalent of {@link MediaType#APPLICATION_JSON}.
   *
   * @see #APPLICATION_JSON_UTF8_VALUE
   */
  public static final String APPLICATION_JSON_VALUE = "application/json";
  public static final String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";

  /** Public constant media type for {@code application/octet-stream}. */
  public static final MediaType APPLICATION_OCTET_STREAM = new MediaType("application", "octet-stream");

  /** A String equivalent of {@link MediaType#APPLICATION_OCTET_STREAM}. */
  public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

  /** Public constant media type for {@code application/pdf}. */
  public static final MediaType APPLICATION_PDF = new MediaType("application", "pdf");

  /** A String equivalent of {@link MediaType#APPLICATION_PDF}. */
  public static final String APPLICATION_PDF_VALUE = "application/pdf";

  /**
   * Public constant media type for {@code application/problem+json}.*
   *
   * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.1"> Problem
   * Details for HTTP APIs, 6.1. application/problem+json</a>
   */
  public static final MediaType APPLICATION_PROBLEM_JSON = new MediaType("application", "problem+json");

  /** A String equivalent of {@link MediaType#APPLICATION_PROBLEM_JSON}. */
  public static final String APPLICATION_PROBLEM_JSON_VALUE = "application/problem+json";

  /**
   * Public constant media type for {@code application/problem+xml}.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.2"> Problem
   * Details for HTTP APIs, 6.2. application/problem+xml</a>
   */
  public static final MediaType APPLICATION_PROBLEM_XML = new MediaType("application", "problem+xml");

  /** A String equivalent of {@link MediaType#APPLICATION_PROBLEM_XML}. */
  public static final String APPLICATION_PROBLEM_XML_VALUE = "application/problem+xml";

  /** Public constant media type for {@code application/rss+xml}. */
  public static final MediaType APPLICATION_RSS_XML = new MediaType("application", "rss+xml");

  /** A String equivalent of {@link MediaType#APPLICATION_RSS_XML}. */
  public static final String APPLICATION_RSS_XML_VALUE = "application/rss+xml";

  /**
   * Public constant media type for {@code application/x-ndjson}.
   */
  public static final MediaType APPLICATION_NDJSON = new MediaType("application", "x-ndjson");

  /**
   * A String equivalent of {@link MediaType#APPLICATION_NDJSON}.
   */
  public static final String APPLICATION_NDJSON_VALUE = "application/x-ndjson";

  /** Public constant media type for {@code application/stream+json}. */
  public static final MediaType APPLICATION_STREAM_JSON = new MediaType("application", "stream+json");

  /** A String equivalent of {@link MediaType#APPLICATION_STREAM_JSON}. */
  public static final String APPLICATION_STREAM_JSON_VALUE = "application/stream+json";

  /** Public constant media type for {@code application/xhtml+xml}. */
  public static final MediaType APPLICATION_XHTML_XML = new MediaType("application", "xhtml+xml");

  /** A String equivalent of {@link MediaType#APPLICATION_XHTML_XML}. */
  public static final String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";

  /** Public constant media type for {@code application/xml}. */
  public static final MediaType APPLICATION_XML = new MediaType("application", "xml");

  /** A String equivalent of {@link MediaType#APPLICATION_XML}. */
  public static final String APPLICATION_XML_VALUE = "application/xml";

  /** Public constant media type for {@code image/gif}. */
  public static final MediaType IMAGE_GIF = new MediaType("image", "gif");

  /** A String equivalent of {@link MediaType#IMAGE_GIF}. */
  public static final String IMAGE_GIF_VALUE = "image/gif";

  /** Public constant media type for {@code image/jpeg}. */
  public static final MediaType IMAGE_JPEG = new MediaType("image", "jpeg");

  /** A String equivalent of {@link MediaType#IMAGE_JPEG}. */
  public static final String IMAGE_JPEG_VALUE = "image/jpeg";

  /** Public constant media type for {@code image/png}. */
  public static final MediaType IMAGE_PNG = new MediaType("image", "png");

  /** A String equivalent of {@link MediaType#IMAGE_PNG}. */
  public static final String IMAGE_PNG_VALUE = "image/png";

  /** Public constant media type for {@code multipart/form-data}. */
  public static final MediaType MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");

  /** A String equivalent of {@link MediaType#MULTIPART_FORM_DATA}. */
  public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

  /** Public constant media type for {@code multipart/mixed}. */
  public static final MediaType MULTIPART_MIXED = new MediaType("multipart", "mixed");

  /** A String equivalent of {@link MediaType#MULTIPART_MIXED}. */
  public static final String MULTIPART_MIXED_VALUE = "multipart/mixed";

  /**
   * Public constant media type for {@code multipart/related}.
   *
   * @since 4.0
   */
  public static final MediaType MULTIPART_RELATED = new MediaType("multipart", "related");

  /**
   * A String equivalent of {@link MediaType#MULTIPART_RELATED}.
   *
   * @since 4.0
   */
  public static final String MULTIPART_RELATED_VALUE = "multipart/related";

  /**
   * Public constant media type for {@code text/event-stream}.
   *
   * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C
   * recommendation</a>
   */
  public static final MediaType TEXT_EVENT_STREAM = new MediaType("text", "event-stream");

  /** A String equivalent of {@link MediaType#TEXT_EVENT_STREAM}. */
  public static final String TEXT_EVENT_STREAM_VALUE = "text/event-stream";

  /** Public constant media type for {@code text/html}. */
  public static final MediaType TEXT_HTML = new MediaType("text", "html");

  /** A String equivalent of {@link MediaType#TEXT_HTML}. */
  public static final String TEXT_HTML_VALUE = "text/html";

  /** Public constant media type for {@code text/markdown}. */
  public static final MediaType TEXT_MARKDOWN = new MediaType("text", "markdown");

  /** A String equivalent of {@link MediaType#TEXT_MARKDOWN}. */
  public static final String TEXT_MARKDOWN_VALUE = "text/markdown";

  /** Public constant media type for {@code text/plain}. */
  public static final MediaType TEXT_PLAIN = new MediaType("text", "plain");

  /** A String equivalent of {@link MediaType#TEXT_PLAIN}. */
  public static final String TEXT_PLAIN_VALUE = "text/plain";

  /** Public constant media type for {@code text/xml}. */
  public static final MediaType TEXT_XML = new MediaType("text", "xml");

  /** A String equivalent of {@link MediaType#TEXT_XML}. */
  public static final String TEXT_XML_VALUE = "text/xml";

  private static final String PARAM_QUALITY_FACTOR = "q";

  /**
   * Create a new {@code MediaType} for the given primary type.
   * <p>
   * The {@linkplain #getSubtype() subtype} is set to "&#42;", parameters empty.
   *
   * @param type the primary type
   * @throws IllegalArgumentException if any of the parameters contain illegal characters
   */
  public MediaType(String type) {
    super(type);
  }

  /**
   * Create a new {@code MediaType} for the given primary type and subtype.
   * <p>
   * The parameters are empty.
   *
   * @param type the primary type
   * @param subtype the subtype
   * @throws IllegalArgumentException if any of the parameters contain illegal characters
   */
  public MediaType(String type, String subtype) {
    super(type, subtype, Collections.emptyMap());
  }

  /**
   * Create a new {@code MediaType} for the given type, subtype, and character
   * set.
   *
   * @param type the primary type
   * @param subtype the subtype
   * @param charset the character set
   * @throws IllegalArgumentException if any of the parameters contain illegal characters
   */
  public MediaType(String type, String subtype, Charset charset) {
    super(type, subtype, charset);
  }

  /**
   * Create a new {@code MediaType} for the given type, subtype, and quality
   * value.
   *
   * @param type the primary type
   * @param subtype the subtype
   * @param qualityValue the quality value
   * @throws IllegalArgumentException if any of the parameters contain illegal characters
   */
  public MediaType(String type, String subtype, double qualityValue) {
    this(type, subtype, singletonMap(PARAM_QUALITY_FACTOR, Double.toString(qualityValue)));
  }

  /**
   * Copy-constructor that copies the type, subtype and parameters of the given
   * {@code MediaType}, and allows to set the specified character set.
   *
   * @param other the other media type
   * @param charset the character set
   * @throws IllegalArgumentException if any of the parameters contain illegal characters
   */
  public MediaType(MediaType other, Charset charset) {
    super(other, charset);
  }

  /**
   * Copy-constructor that copies the type and subtype of the given
   * {@code MediaType}, and allows for different parameters.
   *
   * @param other the other media type
   * @param parameters the parameters, may be {@code null}
   * @throws IllegalArgumentException if any of the parameters contain illegal characters
   */
  public MediaType(MediaType other, @Nullable Map<String, String> parameters) {
    super(other.getType(), other.getSubtype(), parameters);
  }

  /**
   * Create a new {@code MediaType} for the given type, subtype, and parameters.
   *
   * @param type the primary type
   * @param subtype the subtype
   * @param parameters the parameters, may be {@code null}
   * @throws IllegalArgumentException if any of the parameters contain illegal characters
   */
  public MediaType(String type, String subtype, @Nullable Map<String, String> parameters) {
    super(type, subtype, parameters);
  }

  @Override
  protected void checkParameters(String attribute, String value) {
    super.checkParameters(attribute, value);
    if (PARAM_QUALITY_FACTOR.equals(attribute)) {
      value = unquote(value);
      double d = Double.parseDouble(value);
      if (!(d >= 0D && d <= 1D)) {
        throw new IllegalArgumentException("Invalid quality value \"" + value + "\": should be between 0.0 and 1.0");
      }
    }
  }

  /**
   * Return the quality factor, as indicated by a {@code q} parameter, if any.
   * Defaults to {@code 1.0}.
   *
   * @return the quality factor as double value
   */
  public double getQualityValue() {
    String qualityFactor = getParameter(PARAM_QUALITY_FACTOR);
    return (qualityFactor != null ? Double.parseDouble(unquote(qualityFactor)) : 1D);
  }

  /**
   * Indicate whether this {@code MediaType} includes the given media type.
   * <p>
   * For instance, {@code text/*} includes {@code text/plain} and
   * {@code text/html}, and {@code application/*+xml} includes
   * {@code application/soap+xml}, etc. This method is <b>not</b> symmetric.
   * <p>
   * Simply calls {@link MimeType#includes(MimeType)} but declared with a
   * {@code MediaType} parameter for binary backwards compatibility.
   *
   * @param other the reference media type with which to compare
   * @return {@code true} if this media type includes the given media type;
   * {@code false} otherwise
   */
  public boolean includes(MediaType other) {
    return super.includes(other);
  }

  /**
   * Indicate whether this {@code MediaType} is compatible with the given media
   * type.
   * <p>
   * For instance, {@code text/*} is compatible with {@code text/plain},
   * {@code text/html}, and vice versa. In effect, this method is similar to
   * {@link #includes}, except that it <b>is</b> symmetric.
   * <p>
   * Simply calls {@link MimeType#isCompatibleWith(MimeType)} but declared with a
   * {@code MediaType} parameter for binary backwards compatibility.
   *
   * @param other the reference media type with which to compare
   * @return {@code true} if this media type is compatible with the given media
   * type; {@code false} otherwise
   */
  public boolean isCompatibleWith(MediaType other) {
    return super.isCompatibleWith(other);
  }

  /**
   * Return a replica of this instance with the quality value of the given
   * {@code MediaType}.
   *
   * @return the same instance if the given MediaType doesn't have a quality
   * value, or a new one otherwise
   */
  public MediaType copyQualityValue(MediaType mediaType) {
    if (!mediaType.getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
      return this;
    }
    LinkedHashMap<String, String> params = new LinkedHashMap<>(getParameters());
    params.put(PARAM_QUALITY_FACTOR, mediaType.getParameters().get(PARAM_QUALITY_FACTOR));
    return new MediaType(this, params);
  }

  /**
   * Return a replica of this instance with its quality value removed.
   *
   * @return the same instance if the media type doesn't contain a quality value,
   * or a new one otherwise
   */
  public MediaType removeQualityValue() {
    if (!getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
      return this;
    }
    LinkedHashMap<String, String> params = new LinkedHashMap<>(getParameters());
    params.remove(PARAM_QUALITY_FACTOR);
    return new MediaType(this, params);
  }

  /**
   * Parse the given String value into a {@code MediaType} object
   *
   * @param value the string to parse
   * @throws InvalidMediaTypeException if the media type value cannot be parsed
   * @see #parseMediaType(String)
   */
  public static MediaType valueOf(String value) {
    return parseMediaType(value);
  }

  /**
   * Parse the given String into a single {@code MediaType}.
   *
   * @param mediaType the string to parse
   * @return the media type
   * @throws InvalidMediaTypeException if the media type value cannot be parsed
   */
  public static MediaType parseMediaType(String mediaType) {
    MimeType type;
    try {
      type = MimeTypeUtils.parseMimeType(mediaType);
    }
    catch (InvalidMimeTypeException ex) {
      throw new InvalidMediaTypeException(ex);
    }
    try {
      return new MediaType(type.getType(), type.getSubtype(), type.getParameters());
    }
    catch (IllegalArgumentException ex) {
      throw new InvalidMediaTypeException(mediaType, ex.getMessage());
    }
  }

  /**
   * Parse the comma-separated string into a list of {@code MediaType} objects.
   * <p>
   * This method can be used to parse an Accept or Content-Type header.
   *
   * @param mediaTypes the string to parse
   * @return the list of media types
   * @throws InvalidMediaTypeException if the media type value cannot be parsed
   */
  public static List<MediaType> parseMediaTypes(String mediaTypes) {
    if (StringUtils.isEmpty(mediaTypes)) {
      return Collections.emptyList();
    }
    // Avoid using java.util.stream.Stream in hot paths
    List<String> tokenizedTypes = MimeTypeUtils.tokenize(mediaTypes);
    ArrayList<MediaType> result = new ArrayList<>(tokenizedTypes.size());
    for (String type : tokenizedTypes) {
      if (StringUtils.isNotEmpty(type)) {
        result.add(parseMediaType(type));
      }
    }
    return result;
  }

  /**
   * Parse the given list of (potentially) comma-separated strings into a list of
   * {@code MediaType} objects.
   * <p>
   * This method can be used to parse an Accept or Content-Type header.
   *
   * @param mediaTypes the string to parse
   * @return the list of media types
   * @throws InvalidMediaTypeException if the media type value cannot be parsed
   */
  public static List<MediaType> parseMediaTypes(List<String> mediaTypes) {
    if (CollectionUtils.isEmpty(mediaTypes)) {
      return Collections.emptyList();
    }
    else if (mediaTypes.size() == 1) {
      return parseMediaTypes(mediaTypes.get(0));
    }
    else {
      ArrayList<MediaType> result = new ArrayList<>(8);
      for (String mediaType : mediaTypes) {
        result.addAll(parseMediaTypes(mediaType));
      }
      return result;
    }
  }

  /**
   * Re-create the given mime types as media types.
   */
  public static List<MediaType> asMediaTypes(List<MimeType> mimeTypes) {
    ArrayList<MediaType> mediaTypes = new ArrayList<>(mimeTypes.size());
    for (MimeType mimeType : mimeTypes) {
      mediaTypes.add(MediaType.asMediaType(mimeType));
    }
    return mediaTypes;
  }

  /**
   * Re-create the given mime type as a media type.
   */
  public static MediaType asMediaType(MimeType mimeType) {
    return mimeType instanceof MediaType
           ? (MediaType) mimeType
           : new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
  }

  /**
   * Return a string representation of the given list of {@code MediaType}
   * objects.
   * <p>
   * This method can be used to for an {@code Accept} or {@code Content-Type}
   * header.
   *
   * @param mediaTypes the media types to create a string representation for
   * @return the string representation
   */
  public static String toString(Collection<MediaType> mediaTypes) {
    return MimeTypeUtils.toString(mediaTypes);
  }

  /**
   * Indicates whether this {@code MediaType} more specific than the given type.
   * <ol>
   * <li>if this media type has a {@linkplain #getQualityValue() quality factor} higher than the other,
   * then this method returns {@code true}.</li>
   * <li>if this media type has a {@linkplain #getQualityValue() quality factor} lower than the other,
   * then this method returns {@code false}.</li>
   * <li>if this mime type has a {@linkplain #isWildcardType() wildcard type},
   * and the other does not, then this method returns {@code false}.</li>
   * <li>if this mime type does not have a {@linkplain #isWildcardType() wildcard type},
   * and the other does, then this method returns {@code true}.</li>
   * <li>if this mime type has a {@linkplain #isWildcardType() wildcard type},
   * and the other does not, then this method returns {@code false}.</li>
   * <li>if this mime type does not have a {@linkplain #isWildcardType() wildcard type},
   * and the other does, then this method returns {@code true}.</li>
   * <li>if the two mime types have identical {@linkplain #getType() type} and
   * {@linkplain #getSubtype() subtype}, then the mime type with the most
   * parameters is more specific than the other.</li>
   * <li>Otherwise, this method returns {@code false}.</li>
   * </ol>
   *
   * @param other the {@code MimeType} to be compared
   * @return the result of the comparison
   * @see #isLessSpecific(MimeType)
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics
   * and Content, section 5.3.2</a>
   * @since 4.0
   */
  @Override
  public boolean isMoreSpecific(MimeType other) {
    Assert.notNull(other, "Other must not be null");
    if (other instanceof MediaType otherMediaType) {
      double quality1 = getQualityValue();
      double quality2 = otherMediaType.getQualityValue();
      if (quality1 > quality2) {
        return true;
      }
      else if (quality1 < quality2) {
        return false;
      }
    }
    return super.isMoreSpecific(other);
  }

  /**
   * Indicates whether this {@code MediaType} more specific than the given type.
   * <ol>
   * <li>if this media type has a {@linkplain #getQualityValue() quality factor} higher than the other,
   * then this method returns {@code false}.</li>
   * <li>if this media type has a {@linkplain #getQualityValue() quality factor} lower than the other,
   * then this method returns {@code true}.</li>
   * <li>if this mime type has a {@linkplain #isWildcardType() wildcard type},
   * and the other does not, then this method returns {@code true}.</li>
   * <li>if this mime type does not have a {@linkplain #isWildcardType() wildcard type},
   * and the other does, then this method returns {@code false}.</li>
   * <li>if this mime type has a {@linkplain #isWildcardType() wildcard type},
   * and the other does not, then this method returns {@code true}.</li>
   * <li>if this mime type does not have a {@linkplain #isWildcardType() wildcard type},
   * and the other does, then this method returns {@code false}.</li>
   * <li>if the two mime types have identical {@linkplain #getType() type} and
   * {@linkplain #getSubtype() subtype}, then the mime type with the least
   * parameters is less specific than the other.</li>
   * <li>Otherwise, this method returns {@code false}.</li>
   * </ol>
   *
   * @param other the {@code MimeType} to be compared
   * @return the result of the comparison
   * @see #isMoreSpecific(MimeType)
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics
   * and Content, section 5.3.2</a>
   * @since 4.0
   */
  @Override
  public boolean isLessSpecific(MimeType other) {
    Assert.notNull(other, "Other must not be null");
    return other.isMoreSpecific(this);
  }

  /**
   * Sorts the given list of {@code MediaType} objects by specificity.
   * <p>
   * Given two media types:
   * <ol>
   * <li>if either media type has a {@linkplain #isWildcardType() wildcard type},
   * then the media type without the wildcard is ordered before the other.</li>
   * <li>if the two media types have different {@linkplain #getType() types}, then
   * they are considered equal and remain their current order.</li>
   * <li>if either media type has a {@linkplain #isWildcardSubtype() wildcard
   * subtype}, then the media type without the wildcard is sorted before the
   * other.</li>
   * <li>if the two media types have different {@linkplain #getSubtype()
   * subtypes}, then they are considered equal and remain their current
   * order.</li>
   * <li>if the two media types have different {@linkplain #getQualityValue()
   * quality value}, then the media type with the highest quality value is ordered
   * before the other.</li>
   * <li>if the two media types have a different amount of
   * {@linkplain #getParameter(String) parameters}, then the media type with the
   * most parameters is ordered before the other.</li>
   * </ol>
   * <p>
   * For example: <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
   * <blockquote>audio/* &lt; audio/*;q=0.7; audio/*;q=0.3</blockquote>
   * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
   * <blockquote>audio/basic == text/html</blockquote> <blockquote>audio/basic ==
   * audio/wave</blockquote>
   *
   * @param mediaTypes the list of media types to be sorted
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1:
   * Semantics and Content, section 5.3.2</a>
   */
  public static void sortBySpecificity(List<MediaType> mediaTypes) {
    Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
    if (mediaTypes.size() > 1) {
      mediaTypes.sort(SPECIFICITY_COMPARATOR);
    }
  }

  /**
   * Sorts the given list of {@code MediaType} objects by quality value.
   * <p>
   * Given two media types:
   * <ol>
   * <li>if the two media types have different {@linkplain #getQualityValue()
   * quality value}, then the media type with the highest quality value is ordered
   * before the other.</li>
   * <li>if either media type has a {@linkplain #isWildcardType() wildcard type},
   * then the media type without the wildcard is ordered before the other.</li>
   * <li>if the two media types have different {@linkplain #getType() types}, then
   * they are considered equal and remain their current order.</li>
   * <li>if either media type has a {@linkplain #isWildcardSubtype() wildcard
   * subtype}, then the media type without the wildcard is sorted before the
   * other.</li>
   * <li>if the two media types have different {@linkplain #getSubtype()
   * subtypes}, then they are considered equal and remain their current
   * order.</li>
   * <li>if the two media types have a different amount of
   * {@linkplain #getParameter(String) parameters}, then the media type with the
   * most parameters is ordered before the other.</li>
   * </ol>
   *
   * @param mediaTypes the list of media types to be sorted
   * @see #getQualityValue()
   */
  public static void sortByQualityValue(List<MediaType> mediaTypes) {
    Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
    if (mediaTypes.size() > 1) {
      mediaTypes.sort(QUALITY_VALUE_COMPARATOR);
    }
  }

  /**
   * Sorts the given list of {@code MediaType} objects by specificity as the
   * primary criteria and quality value the secondary.
   *
   * @see MediaType#sortBySpecificity(List)
   * @see MediaType#sortByQualityValue(List)
   */
  public static void sortBySpecificityAndQuality(List<MediaType> mediaTypes) {
    Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
    if (mediaTypes.size() > 1) {
      mediaTypes.sort(MediaType.SPECIFICITY_COMPARATOR.thenComparing(MediaType.QUALITY_VALUE_COMPARATOR));
    }
  }

  /**
   * Comparator used by {@link #sortByQualityValue(List)}.
   */
  public static final Comparator<MediaType> QUALITY_VALUE_COMPARATOR = (mediaType1, mediaType2) -> {
    double quality1 = mediaType1.getQualityValue();
    double quality2 = mediaType2.getQualityValue();
    int qualityComparison = Double.compare(quality2, quality1);
    if (qualityComparison != 0) {
      return qualityComparison; // audio/*;q=0.7 < audio/*;q=0.3
    }
    else if (mediaType1.isWildcardType() && !mediaType2.isWildcardType()) { // */* < audio/*
      return 1;
    }
    else if (mediaType2.isWildcardType() && !mediaType1.isWildcardType()) { // audio/* > */*
      return -1;
    }
    else if (!mediaType1.getType().equals(mediaType2.getType())) { // audio/basic == text/html
      return 0;
    }
    else { // mediaType1.getType().equals(mediaType2.getType())
      if (mediaType1.isWildcardSubtype() && !mediaType2.isWildcardSubtype()) { // audio/* < audio/basic
        return 1;
      }
      else if (mediaType2.isWildcardSubtype() && !mediaType1.isWildcardSubtype()) { // audio/basic > audio/*
        return -1;
      }
      else if (!mediaType1.getSubtype().equals(mediaType2.getSubtype())) { // audio/basic == audio/wave
        return 0;
      }
      else {
        int paramsSize1 = mediaType1.getParameters().size();
        int paramsSize2 = mediaType2.getParameters().size();
        return Integer.compare(paramsSize2, paramsSize1); // audio/basic;level=1 < audio/basic
      }
    }
  };

  /**
   * Comparator used by {@link #sortBySpecificity(List)}.
   */
  public static final Comparator<MediaType> SPECIFICITY_COMPARATOR = new SpecificityComparator<>() {

    @Override
    protected int compareParameters(MediaType mediaType1, MediaType mediaType2) {
      double quality1 = mediaType1.getQualityValue();
      double quality2 = mediaType2.getQualityValue();
      int qualityComparison = Double.compare(quality2, quality1);
      if (qualityComparison != 0) {
        return qualityComparison; // audio/*;q=0.7 < audio/*;q=0.3
      }
      return super.compareParameters(mediaType1, mediaType2);
    }
  };

  // --------------------------------------------

  /**
   * Determine a media type for the given resource, if possible.
   *
   * @param resource the resource to introspect
   * @return the corresponding media type, or {@code null} if none found
   */
  @Nullable
  public static MediaType fromResource(Resource resource) {
    return resource == null ? null : fromFileName(resource.getName());
  }

  /**
   * Determine a media type for the given file name, if possible.
   *
   * @param filename the file name plus extension
   * @return the corresponding media type, or {@code null} if none found
   */
  @Nullable
  public static MediaType fromFileName(String filename) {
    Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(filename);
    return mediaType.orElse(null);
  }

}
