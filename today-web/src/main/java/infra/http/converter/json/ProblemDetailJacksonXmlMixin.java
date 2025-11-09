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

package infra.http.converter.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * Provides the same declarations as {@link ProblemDetailJacksonMixin} and some
 * additional ones to support XML serialization when {@code jackson-dataformat-xml}
 * is on the classpath. Customizes the XML root element name and adds namespace
 * information.
 *
 * <p>Note that we cannot use {@code @JsonRootName} to specify the namespace since that
 * is not inherited by fields of the class. This is why we need a dedicated "mix-in"
 * when {@code jackson-dataformat-xml} is on the classpath. For more details, see
 * <a href="https://github.com/FasterXML/jackson-dataformat-xml/issues/355">FasterXML/jackson-dataformat-xml#355</a>.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@JsonInclude(NON_EMPTY)
@JsonRootName(value = "problem", namespace = ProblemDetailJacksonXmlMixin.NAMESPACE)
@JacksonXmlRootElement(localName = "problem", namespace = ProblemDetailJacksonXmlMixin.NAMESPACE)
public interface ProblemDetailJacksonXmlMixin {

  /** RFC 7807 (obsoleted by RFC 9457) namespace. */
  String NAMESPACE = "urn:ietf:rfc:7807";

  @JacksonXmlProperty(namespace = NAMESPACE)
  URI getType();

  @JacksonXmlProperty(namespace = NAMESPACE)
  String getTitle();

  @JacksonXmlProperty(namespace = NAMESPACE)
  int getStatus();

  @JacksonXmlProperty(namespace = NAMESPACE)
  String getDetail();

  @JacksonXmlProperty(namespace = NAMESPACE)
  URI getInstance();

  @JsonAnySetter
  void setProperty(String name, @Nullable Object value);

  @JsonAnyGetter
  @JacksonXmlProperty(namespace = NAMESPACE)
  Map<String, Object> getProperties();

}
