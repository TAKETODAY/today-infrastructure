/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.util.JsonFormat;

import cn.taketoday.lang.Nullable;

/**
 * Subclass of {@link ProtobufHttpMessageConverter} which enforces the use of Protobuf 3 and
 * its official library {@code "com.google.protobuf:protobuf-java-util"} for JSON processing.
 *
 * <p>Most importantly, this class allows for custom JSON parser and printer configurations
 * through the {@link JsonFormat} utility. If no special parser or printer configuration is
 * given, default variants will be used instead.
 *
 * <p>Requires Protobuf 3.x and {@code "com.google.protobuf:protobuf-java-util"} 3.x,
 * with 3.3 or higher recommended.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @see JsonFormat#parser()
 * @see JsonFormat#printer()
 * @see #ProtobufJsonFormatHttpMessageConverter(com.google.protobuf.util.JsonFormat.Parser, com.google.protobuf.util.JsonFormat.Printer)
 * @since 4.0
 */
public class ProtobufJsonFormatHttpMessageConverter extends ProtobufHttpMessageConverter {

  /**
   * Constructor with default instances of
   * {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser},
   * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer},
   * and {@link ExtensionRegistry}.
   */
  public ProtobufJsonFormatHttpMessageConverter() {
    this(null, null);
  }

  /**
   * Constructor with given instances of
   * {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser},
   * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer},
   * and a default instance of {@link ExtensionRegistry}.
   */
  public ProtobufJsonFormatHttpMessageConverter(
          @Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {

    this(parser, printer, null);
  }

  /**
   * Constructor with given instances of
   * {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser},
   * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer},
   * and {@link ExtensionRegistry}.
   */
  public ProtobufJsonFormatHttpMessageConverter(
          @Nullable JsonFormat.Parser parser,
          @Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistry extensionRegistry) {

    super(new ProtobufJavaUtilSupport(parser, printer), extensionRegistry);
  }

}
