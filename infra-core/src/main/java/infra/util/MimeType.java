/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.util;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import infra.lang.Assert;

/**
 * Represents a MIME Type, as originally defined in RFC 2046 and subsequently
 * used in other Internet protocols including HTTP.
 *
 * <p>
 * This class, however, does not contain support for the q-parameters used in
 * HTTP content negotiation. Those can be found in the subclass
 * {@code MediaType} in the {@code infra-web} module.
 *
 * <p>
 * Consists of a {@linkplain #getType() type} and a {@linkplain #getSubtype()
 * subtype}. Also has functionality to parse MIME Type values from a
 * {@code String} using {@link #valueOf(String)}. For more parsing options see
 * {@link MimeTypeUtils}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MimeTypeUtils
 * @since 2.1.7 2019-12-08 19:08
 */
public class MimeType implements Comparable<MimeType>, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final String WILDCARD_TYPE = "*";

  public static final String PARAM_CHARSET = "charset";

  /**
   * Public constant mime type that includes all media ranges (i.e.
   * "&#42;/&#42;").
   *
   * @since 5.0
   */
  public static final MimeType ALL = new MimeType("*", "*");

  /**
   * A String equivalent of {@link MimeType#ALL}.
   *
   * @since 5.0
   */
  public static final String ALL_VALUE = "*/*";

  /**
   * Public constant mime type for {@code application/json}.
   *
   * @since 5.0
   */
  public static final MimeType APPLICATION_JSON = new MimeType("application", "json");

  /**
   * A String equivalent of {@link MimeType#APPLICATION_JSON}.
   *
   * @since 5.0
   */
  public static final String APPLICATION_JSON_VALUE = "application/json";

  /**
   * Public constant mime type for {@code application/octet-stream}.
   *
   * @since 5.0
   */
  public static final MimeType APPLICATION_OCTET_STREAM = new MimeType("application", "octet-stream");

  /**
   * A String equivalent of {@link MimeType#APPLICATION_OCTET_STREAM}.
   *
   * @since 5.0
   */
  public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

  /**
   * Public constant mime type for {@code application/xml}.
   *
   * @since 5.0
   */
  public static final MimeType APPLICATION_XML = new MimeType("application", "xml");

  /**
   * A String equivalent of {@link MimeType#APPLICATION_XML}.
   *
   * @since 5.0
   */
  public static final String APPLICATION_XML_VALUE = "application/xml";

  /**
   * Public constant mime type for {@code image/gif}.
   *
   * @since 5.0
   */
  public static final MimeType IMAGE_GIF = new MimeType("image", "gif");

  /**
   * A String equivalent of {@link MimeType#IMAGE_GIF}.
   *
   * @since 5.0
   */
  public static final String IMAGE_GIF_VALUE = "image/gif";

  /**
   * Public constant mime type for {@code image/jpeg}.
   *
   * @since 5.0
   */
  public static final MimeType IMAGE_JPEG = new MimeType("image", "jpeg");

  /**
   * A String equivalent of {@link MimeType#IMAGE_JPEG}.
   *
   * @since 5.0
   */
  public static final String IMAGE_JPEG_VALUE = "image/jpeg";

  /**
   * Public constant mime type for {@code image/png}.
   *
   * @since 5.0
   */
  public static final MimeType IMAGE_PNG = new MimeType("image", "png");

  /**
   * A String equivalent of {@link MimeType#IMAGE_PNG}.
   *
   * @since 5.0
   */
  public static final String IMAGE_PNG_VALUE = "image/png";

  /**
   * Public constant mime type for {@code text/html}.
   *
   * @since 5.0
   */
  public static final MimeType TEXT_HTML = new MimeType("text", "html");

  /**
   * A String equivalent of {@link MimeType#TEXT_HTML}.
   *
   * @since 5.0
   */
  public static final String TEXT_HTML_VALUE = "text/html";

  /**
   * Public constant mime type for {@code text/plain}.
   *
   * @since 5.0
   */
  public static final MimeType TEXT_PLAIN = new MimeType("text", "plain");

  public static final MimeType TEXT_PLAIN_UTF8 = TEXT_PLAIN.withCharset(StandardCharsets.UTF_8);

  /**
   * A String equivalent of {@link MimeType#TEXT_PLAIN}.
   *
   * @since 5.0
   */
  public static final String TEXT_PLAIN_VALUE = "text/plain";

  /**
   * Public constant mime type for {@code text/xml}.
   *
   * @since 5.0
   */
  public static final MimeType TEXT_XML = new MimeType("text", "xml");

  /**
   * A String equivalent of {@link MimeType#TEXT_XML}.
   *
   * @since 5.0
   */
  public static final String TEXT_XML_VALUE = "text/xml";

  /** @since 5.0 */
  public static final String APPLICATION_YAML_VALUE = "application/yaml";

  /** @since 5.0 */
  public static final String APPLICATION_PDF_VALUE = "application/pdf";

  protected final String type;

  protected final String subtype;

  protected final Map<String, String> parameters;

  private volatile @Nullable String toStringValue;

  private transient @Nullable Charset resolvedCharset;

  /**
   * Create a new {@code MimeType} for the given primary type.
   * <p>
   * The {@linkplain #getSubtype() subtype} is set to <code>"&#42;"</code>, and
   * the parameters are empty.
   *
   * @param type the primary type
   * @throws IllegalArgumentException if any of the parameters contains illegal characters
   */
  public MimeType(String type) {
    this(type, WILDCARD_TYPE);
  }

  /**
   * Create a new {@code MimeType} for the given primary type and subtype.
   * <p>
   * The parameters are empty.
   *
   * @param type the primary type
   * @param subtype the subtype
   * @throws IllegalArgumentException if any of the parameters contains illegal characters
   */
  public MimeType(String type, String subtype) {
    this(type, subtype, (Map<String, String>) null);
  }

  /**
   * Create a new {@code MimeType} for the given type, subtype, and character set.
   *
   * @param type the primary type
   * @param subtype the subtype
   * @param charset the character set
   * @throws IllegalArgumentException if any of the parameters contains illegal characters
   */
  public MimeType(String type, String subtype, Charset charset) {
    this(type, subtype, Map.of(PARAM_CHARSET, charset.name()));
  }

  /**
   * Create a new {@code MimeType} for the given type, subtype, and parameters.
   *
   * @param type the primary type
   * @param subtype the subtype
   * @param parameters the parameters (maybe {@code null})
   * @throws IllegalArgumentException if any of the parameters contains illegal characters
   */
  public MimeType(String type, String subtype, @Nullable Map<String, String> parameters) {
    Assert.hasLength(type, "'type' must not be empty");
    Assert.hasLength(subtype, "'subtype' must not be empty");
    checkToken(type);
    checkToken(subtype);

    this.type = type.toLowerCase(Locale.ROOT);
    this.subtype = subtype.toLowerCase(Locale.ROOT);
    this.parameters = createParametersMap(parameters);
    if (this.parameters.containsKey(PARAM_CHARSET)) {
      this.resolvedCharset = Charset.forName(unquote(this.parameters.get(PARAM_CHARSET)));
    }
  }

  /**
   * Copy-constructor that copies the type, subtype, parameters of the given
   * {@code MimeType}, and allows to set the specified character set.
   *
   * @param other the other MimeType
   * @param charset the character set
   * @throws IllegalArgumentException if any of the parameters contains illegal characters
   * @see #withCharset(Charset)
   */
  public MimeType(MimeType other, Charset charset) {
    this.type = other.type;
    this.subtype = other.subtype;
    var map = new LinkedCaseInsensitiveMap<String>(other.parameters.size() + 1, Locale.ROOT);
    map.putAll(other.parameters);
    map.put(PARAM_CHARSET, charset.name());
    this.parameters = Collections.unmodifiableMap(map);
    this.resolvedCharset = charset;
  }

  /**
   * Copy-constructor that copies the type and subtype of the given {@code MimeType},
   * and allows for different parameter.
   *
   * @param other the other MimeType
   * @param parameters the parameters (may be {@code null})
   * @throws IllegalArgumentException if any of the parameters contains illegal characters
   */
  public MimeType(MimeType other, @Nullable Map<String, String> parameters) {
    this.type = other.type;
    this.subtype = other.subtype;
    this.parameters = createParametersMap(parameters);
    if (this.parameters.containsKey(PARAM_CHARSET)) {
      this.resolvedCharset = Charset.forName(unquote(this.parameters.get(PARAM_CHARSET)));
    }
  }

  /**
   * Copy-constructor that copies the type, subtype and parameters of the given {@code MimeType},
   * skipping checks performed in other constructors.
   *
   * @param other the other MimeType
   * @since 3.0
   */
  protected MimeType(MimeType other) {
    this.type = other.type;
    this.subtype = other.subtype;
    this.parameters = other.parameters;
    this.toStringValue = other.toStringValue;
    this.resolvedCharset = other.resolvedCharset;
  }

  /**
   * Checks the given token string for illegal characters, as defined in RFC 2616,
   * section 2.2.
   *
   * @throws IllegalArgumentException in case of illegal characters
   * @see <a href="https://tools.ietf.org/html/rfc2616#section-2.2">HTTP 1.1,
   * section 2.2</a>
   */
  private static void checkToken(String token) {
    int length = token.length();
    for (int i = 0; i < length; i++) {
      char ch = token.charAt(i);
      if (!MimeTypeUtils.TOKEN.get(ch)) {
        throw new IllegalArgumentException("Invalid token character '%s' in token \"%s\"".formatted(ch, token));
      }
    }
  }

  private Map<String, String> createParametersMap(@Nullable Map<String, String> parameters) {
    if (CollectionUtils.isNotEmpty(parameters)) {
      var map = new LinkedCaseInsensitiveMap<String>(parameters.size(), Locale.ROOT);
      for (Map.Entry<String, String> entry : parameters.entrySet()) {
        String parameter = entry.getKey();
        String value = entry.getValue();
        checkParameters(parameter, value);
        map.put(parameter, value);
      }
      return Collections.unmodifiableMap(map);
    }
    else {
      return Collections.emptyMap();
    }
  }

  protected void checkParameters(String parameter, String value) {
    Assert.hasLength(parameter, "'parameter' must not be empty");
    Assert.hasLength(value, "'value' must not be empty");
    checkToken(parameter);
    if (!isQuotedString(value)) {
      checkToken(value);
    }
  }

  private boolean isQuotedString(String s) {
    if (s.length() < 2) {
      return false;
    }
    return ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")));
  }

  /**
   * Unquote the given string if it is quoted.
   * <p>This method checks if the string starts and ends with the same quote character
   * (either single or double quotes) and removes them if so.
   *
   * @param s the string to unquote
   * @return the unquoted string, or the original string if it wasn't quoted
   */
  protected final String unquote(String s) {
    return isQuotedString(s) ? s.substring(1, s.length() - 1) : s;
  }

  /**
   * Indicates whether the {@linkplain #getType() type} is the wildcard character
   * <code>&#42;</code> or not.
   */
  public boolean isWildcardType() {
    return WILDCARD_TYPE.equals(getType());
  }

  /**
   * Indicates whether the {@linkplain #getSubtype() subtype} is the wildcard
   * character <code>&#42;</code> or the wildcard character followed by a suffix
   * (e.g. <code>&#42;+xml</code>).
   *
   * @return whether the subtype is a wildcard
   */
  public boolean isWildcardSubtype() {
    String subtype = getSubtype();
    return WILDCARD_TYPE.equals(subtype) || subtype.startsWith("*+");
  }

  /**
   * Indicates whether this MIME Type is concrete, i.e. whether neither the type
   * nor the subtype is a wildcard character <code>&#42;</code>.
   *
   * @return whether this MIME Type is concrete
   */
  public boolean isConcrete() {
    return !isWildcardType() && !isWildcardSubtype();
  }

  /**
   * Indicates whether the {@linkplain #getType() type} is the multipart or not.
   *
   * @since 5.0
   */
  public boolean isMultipartType() {
    return "multipart".equals(getType());
  }

  /**
   * Return the primary type.
   */
  public String getType() {
    return this.type;
  }

  /**
   * Return the subtype.
   */
  public String getSubtype() {
    return this.subtype;
  }

  /**
   * Return the subtype suffix as defined in RFC 6839.
   *
   * @since 4.0
   */
  public @Nullable String getSubtypeSuffix() {
    int suffixIndex = subtype.lastIndexOf('+');
    if (suffixIndex != -1) {
      return subtype.substring(suffixIndex + 1);
    }
    return null;
  }

  /**
   * Return the character set, as indicated by a {@code charset} parameter, if
   * any.
   *
   * @return the character set, or {@code null} if not available
   */
  public @Nullable Charset getCharset() {
    return resolvedCharset;
  }

  /**
   * Return a generic parameter value, given a parameter name.
   *
   * @param name the parameter name
   * @return the parameter value, or {@code null} if not present
   */
  public @Nullable String getParameter(String name) {
    return this.parameters.get(name);
  }

  /**
   * Return a generic parameter value, given a parameter name, unquoting
   * the value if necessary.
   *
   * @param name the parameter name
   * @return the parameter value, or {@code null} if not present
   * @see #unquote(String)
   * @since 5.0
   */
  public @Nullable String getUnquotedParameter(String name) {
    String val = parameters.get(name);
    return val != null ? unquote(val) : null;
  }

  /**
   * Return all generic parameter values.
   *
   * @return a read-only map (possibly empty, never {@code null})
   */
  public Map<String, String> getParameters() {
    return this.parameters;
  }

  /**
   * Copy that copies the type, subtype, parameters of the given
   * {@code MimeType}, and allows to set the specified character set.
   *
   * @param charset the character set
   * @throws IllegalArgumentException if any of the parameters contains illegal characters
   */
  public MimeType withCharset(Charset charset) {
    return new MimeType(this, charset);
  }

  /**
   * Indicate whether this MIME Type includes the given MIME Type.
   * <p>
   * For instance, {@code text/*} includes {@code text/plain} and
   * {@code text/html}, and {@code application/*+xml} includes
   * {@code application/soap+xml}, etc. This method is <b>not</b> symmetric.
   *
   * @param other the reference MIME Type with which to compare
   * @return {@code true} if this MIME Type includes the given MIME Type;
   * {@code false} otherwise
   */
  public boolean includes(@Nullable MimeType other) {
    if (other == null) {
      return false;
    }
    if (isWildcardType()) {
      // */* includes anything
      return true;
    }
    else if (getType().equals(other.getType())) {
      String subtype = getSubtype();
      String otherSubtype = other.getSubtype();
      if (subtype.equals(otherSubtype)) {
        return true;
      }
      if (isWildcardSubtype()) {
        // Wildcard with suffix, e.g. application/*+xml
        int thisPlusIdx = subtype.lastIndexOf('+');
        if (thisPlusIdx == -1) {
          return true;
        }
        else {
          // application/*+xml includes application/soap+xml
          int otherPlusIdx = otherSubtype.lastIndexOf('+');
          if (otherPlusIdx != -1) {
            String thisSubtypeSuffix = subtype.substring(thisPlusIdx + 1);
            String otherSubtypeSuffix = otherSubtype.substring(otherPlusIdx + 1);

            if (thisSubtypeSuffix.equals(otherSubtypeSuffix)) {
              String thisSubtypeNoSuffix = subtype.substring(0, thisPlusIdx);
              return WILDCARD_TYPE.equals(thisSubtypeNoSuffix);
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Indicate whether this MIME Type is compatible with the given MIME Type.
   * <p>
   * For instance, {@code text/*} is compatible with {@code text/plain},
   * {@code text/html}, and vice versa. In effect, this method is similar to
   * {@link #includes}, except that it <b>is</b> symmetric.
   *
   * @param other the reference MIME Type with which to compare
   * @return {@code true} if this MIME Type is compatible with the given MIME
   * Type; {@code false} otherwise
   */
  public boolean isCompatibleWith(@Nullable MimeType other) {
    if (other == null) {
      return false;
    }
    if (isWildcardType() || other.isWildcardType()) {
      return true;
    }
    else if (getType().equals(other.getType())) {
      String subtype = getSubtype();
      String otherSubtype = other.getSubtype();
      if (subtype.equals(otherSubtype)) {
        return true;
      }
      // Wildcard with suffix? e.g. application/*+xml
      if (isWildcardSubtype() || other.isWildcardSubtype()) {
        int thisPlusIdx = subtype.lastIndexOf('+');
        int otherPlusIdx = otherSubtype.lastIndexOf('+');
        if (thisPlusIdx == -1 && otherPlusIdx == -1) {
          return true;
        }
        else if (thisPlusIdx != -1 && otherPlusIdx != -1) {
          String thisSubtypeSuffix = subtype.substring(thisPlusIdx + 1);
          String otherSubtypeSuffix = otherSubtype.substring(otherPlusIdx + 1);
          if (thisSubtypeSuffix.equals(otherSubtypeSuffix)) {
            String thisSubtypeNoSuffix = subtype.substring(0, thisPlusIdx);
            if (WILDCARD_TYPE.equals(thisSubtypeNoSuffix)) {
              return true;
            }
            String otherSubtypeNoSuffix = otherSubtype.substring(0, otherPlusIdx);
            return WILDCARD_TYPE.equals(otherSubtypeNoSuffix);
          }
        }
      }
    }
    return false;
  }

  /**
   * Similar to {@link #equals(Object)} but based on the type and subtype only,
   * i.e. ignoring parameters.
   *
   * @param other the other mime type to compare to
   * @return whether the two mime types have the same type and subtype
   */
  public boolean equalsTypeAndSubtype(@Nullable MimeType other) {
    if (other == null) {
      return false;
    }
    return this.type.equals(other.type) && this.subtype.equals(other.subtype);
  }

  /**
   * Unlike {@link Collection#contains(Object)} which relies on
   * {@link MimeType#equals(Object)}, this method only checks the type and the
   * subtype, but otherwise ignores parameters.
   *
   * @param mimeTypes the list of mime types to perform the check against
   * @return whether the list contains the given mime type
   */
  public boolean isPresentIn(Collection<? extends MimeType> mimeTypes) {
    for (MimeType mimeType : mimeTypes) {
      if (mimeType.equalsTypeAndSubtype(this)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof MimeType otherType) {
      return this.type.equalsIgnoreCase(otherType.type)
              && this.subtype.equalsIgnoreCase(otherType.subtype)
              && parametersAreEqual(otherType);
    }
    return false;
  }

  /**
   * Determine if the parameters in this {@code MimeType} and the supplied
   * {@code MimeType} are equal, performing case-insensitive comparisons for
   * {@link Charset Charsets}.
   */
  private boolean parametersAreEqual(MimeType other) {
    Map<String, String> otherParameters = other.parameters;
    if (parameters.size() != otherParameters.size()) {
      return false;
    }

    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      String key = entry.getKey();
      if (!otherParameters.containsKey(key)) {
        return false;
      }
      if (PARAM_CHARSET.equals(key)) {
        if (!Objects.equals(getCharset(), other.getCharset())) {
          return false;
        }
      }
      else if (!Objects.equals(entry.getValue(), otherParameters.get(key))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = this.type.hashCode();
    result = 31 * result + this.subtype.hashCode();
    result = 31 * result + this.parameters.hashCode();
    return result;
  }

  @Override
  public String toString() {
    String value = this.toStringValue;
    if (value == null) {
      StringBuilder builder = new StringBuilder();
      appendTo(builder);
      value = builder.toString();
      this.toStringValue = value;
    }
    return value;
  }

  protected void appendTo(StringBuilder builder) {
    builder.append(this.type);
    builder.append('/');
    builder.append(this.subtype);
    appendTo(this.parameters, builder);
  }

  private void appendTo(Map<String, String> map, StringBuilder builder) {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      builder.append(';');
      builder.append(entry.getKey());
      builder.append('=');
      builder.append(entry.getValue());
    }
  }

  /**
   * Compares this MIME Type to another alphabetically.
   *
   * @param other the MIME Type to compare to
   */
  @Override
  public int compareTo(MimeType other) {
    int comp = getType().compareToIgnoreCase(other.getType());
    if (comp != 0) {
      return comp;
    }
    comp = getSubtype().compareToIgnoreCase(other.getSubtype());
    if (comp != 0) {
      return comp;
    }
    comp = getParameters().size() - other.getParameters().size();
    if (comp != 0) {
      return comp;
    }

    TreeSet<String> thisAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    thisAttributes.addAll(getParameters().keySet());
    TreeSet<String> otherAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    otherAttributes.addAll(other.getParameters().keySet());
    Iterator<String> thisAttributesIterator = thisAttributes.iterator();
    Iterator<String> otherAttributesIterator = otherAttributes.iterator();

    while (thisAttributesIterator.hasNext()) {
      String thisAttribute = thisAttributesIterator.next();
      String otherAttribute = otherAttributesIterator.next();
      comp = thisAttribute.compareToIgnoreCase(otherAttribute);
      if (comp != 0) {
        return comp;
      }
      if (PARAM_CHARSET.equals(thisAttribute)) {
        Charset thisCharset = getCharset();
        Charset otherCharset = other.getCharset();
        if (thisCharset != otherCharset) {
          if (thisCharset == null) {
            return -1;
          }
          if (otherCharset == null) {
            return 1;
          }
          comp = thisCharset.compareTo(otherCharset);
          if (comp != 0) {
            return comp;
          }
        }
      }
      else {
        String thisValue = getParameters().get(thisAttribute);
        String otherValue = other.getParameters().get(otherAttribute);
        if (thisValue == null) {
          throw new IllegalArgumentException("Parameter for %s must not be null".formatted(thisAttribute));
        }
        if (otherValue == null) {
          otherValue = "";
        }
        comp = thisValue.compareTo(otherValue);
        if (comp != 0) {
          return comp;
        }
      }
    }

    return 0;
  }

  /**
   * Indicates whether this {@code MimeType} is more specific than the given
   * type.
   * <ol>
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
  public boolean isMoreSpecific(MimeType other) {
    Assert.notNull(other, "Other is required");
    boolean thisWildcard = isWildcardType();
    boolean otherWildcard = other.isWildcardType();
    if (thisWildcard && !otherWildcard) {  // */* > audio/*
      return false;
    }
    else if (!thisWildcard && otherWildcard) {  // audio/* < */*
      return true;
    }
    else {
      boolean thisWildcardSubtype = isWildcardSubtype();
      boolean otherWildcardSubtype = other.isWildcardSubtype();
      if (thisWildcardSubtype && !otherWildcardSubtype) {  // audio/* > audio/basic
        return false;
      }
      else if (!thisWildcardSubtype && otherWildcardSubtype) {  // audio/basic < audio/*
        return true;
      }
      else if (getType().equals(other.getType()) && getSubtype().equals(other.getSubtype())) {
        int paramsSize1 = getParameters().size();
        int paramsSize2 = other.getParameters().size();
        return paramsSize1 > paramsSize2;
      }
      else {
        return false;
      }
    }
  }

  /**
   * Indicates whether this {@code MimeType} is more less than the given type.
   * <ol>
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
  public boolean isLessSpecific(MimeType other) {
    Assert.notNull(other, "Other is required");
    return other.isMoreSpecific(this);
  }

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization, just initialize state after deserialization.
    ois.defaultReadObject();

    // Initialize transient fields.
    String charsetName = getParameter(PARAM_CHARSET);
    if (charsetName != null) {
      this.resolvedCharset = Charset.forName(unquote(charsetName));
    }
  }

  // static

  /**
   * @see MimeTypeUtils#parseMimeType(String)
   */
  public static MimeType valueOf(String value) {
    return MimeTypeUtils.parseMimeType(value);
  }

  /**
   * @throws NullPointerException if charset is null
   */
  private static Map<String, String> addCharsetParameter(Charset charset, Map<String, String> parameters) {
    if (parameters.isEmpty()) {
      return Map.of(PARAM_CHARSET, charset.name());
    }
    LinkedHashMap<String, String> map = new LinkedHashMap<>(parameters);
    map.put(PARAM_CHARSET, charset.name());
    return map;
  }

  /**
   * Comparator to sort {@link MimeType MimeTypes} in order of specificity.
   *
   * @param <T> the type of mime types that may be compared by this comparator
   */
  public static class SpecificityComparator<T extends MimeType> implements Comparator<T> {

    @Override
    public int compare(T mimeType1, T mimeType2) {
      if (mimeType1.isWildcardType() && !mimeType2.isWildcardType()) { // */* < audio/*
        return 1;
      }
      else if (mimeType2.isWildcardType() && !mimeType1.isWildcardType()) { // audio/* > */*
        return -1;
      }
      else if (!mimeType1.getType().equals(mimeType2.getType())) { // audio/basic == text/html
        return 0;
      }
      else { // mediaType1.getType().equals(mediaType2.getType())
        if (mimeType1.isWildcardSubtype() && !mimeType2.isWildcardSubtype()) { // audio/* < audio/basic
          return 1;
        }
        else if (mimeType2.isWildcardSubtype() && !mimeType1.isWildcardSubtype()) { // audio/basic > audio/*
          return -1;
        }
        else if (!mimeType1.getSubtype().equals(mimeType2.getSubtype())) { // audio/basic == audio/wave
          return 0;
        }
        else { // mediaType2.getSubtype().equals(mediaType2.getSubtype())
          return compareParameters(mimeType1, mimeType2);
        }
      }
    }

    protected int compareParameters(T mimeType1, T mimeType2) {
      int paramsSize1 = mimeType1.getParameters().size();
      int paramsSize2 = mimeType2.getParameters().size();
      return Integer.compare(paramsSize2, paramsSize1); // audio/basic;level=1 < audio/basic
    }
  }

}
