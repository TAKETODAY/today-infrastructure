/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.converter.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.net.URI;
import java.util.Map;

import cn.taketoday.lang.Nullable;

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
@JacksonXmlRootElement(localName = "problem", namespace = ProblemDetailJacksonXmlMixin.RFC_7807_NAMESPACE)
public interface ProblemDetailJacksonXmlMixin {

  /** RFC 7807 namespace. */
  String RFC_7807_NAMESPACE = "urn:ietf:rfc:7807";

  @JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
  URI getType();

  @JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
  String getTitle();

  @JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
  int getStatus();

  @JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
  String getDetail();

  @JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
  URI getInstance();

  @JsonAnySetter
  void setProperty(String name, @Nullable Object value);

  @JsonAnyGetter
  @JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
  Map<String, Object> getProperties();

}
