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

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;
import com.googlecode.protobuf.format.FormatFactory;
import com.googlecode.protobuf.format.ProtobufFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.converter.AbstractHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConversionException;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MediaType;

import static cn.taketoday.util.MediaType.APPLICATION_JSON;
import static cn.taketoday.util.MediaType.APPLICATION_XML;
import static cn.taketoday.util.MediaType.TEXT_HTML;
import static cn.taketoday.util.MediaType.TEXT_PLAIN;

/**
 * An {@code HttpMessageConverter} that reads and writes
 * {@link com.google.protobuf.Message com.google.protobuf.Messages} using
 * <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>.
 *
 * <p>To generate {@code Message} Java classes, you need to install the {@code protoc} binary.
 *
 * <p>This converter supports by default {@code "application/x-protobuf"} and {@code "text/plain"}
 * with the official {@code "com.google.protobuf:protobuf-java"} library. Other formats can be
 * supported with one of the following additional libraries on the classpath:
 * <ul>
 * <li>{@code "application/json"}, {@code "application/xml"}, and {@code "text/html"} (write-only)
 * with the {@code "com.googlecode.protobuf-java-format:protobuf-java-format"} third-party library
 * <li>{@code "application/json"} with the official {@code "com.google.protobuf:protobuf-java-util"}
 * for Protobuf 3 (see {@link ProtobufJsonFormatHttpMessageConverter} for a configurable variant)
 * </ul>
 *
 * <p>Requires Protobuf 2.6 or higher (and Protobuf Java Format 1.4 or higher for formatting).
 * This converter will auto-adapt to Protobuf 3 and its default {@code protobuf-java-util} JSON
 * format if the Protobuf 2 based {@code protobuf-java-format} isn't present; however, for more
 * explicit JSON setup on Protobuf 3, consider {@link ProtobufJsonFormatHttpMessageConverter}.
 *
 * @author Alex Antonov
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @see FormatFactory
 * @see JsonFormat
 * @see ProtobufJsonFormatHttpMessageConverter
 * @since 4.0
 */
public class ProtobufHttpMessageConverter extends AbstractHttpMessageConverter<Message> {

  /**
   * The default charset used by the converter.
   */
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  /**
   * The media-type for protobuf {@code application/x-protobuf}.
   */
  public static final MediaType PROTOBUF = new MediaType("application", "x-protobuf", DEFAULT_CHARSET);

  /**
   * The HTTP header containing the protobuf schema.
   */
  public static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";

  /**
   * The HTTP header containing the protobuf message.
   */
  public static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";

  private static final Map<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

  final ExtensionRegistry extensionRegistry;

  @Nullable
  private final ProtobufFormatSupport protobufFormatSupport;

  /**
   * Construct a new {@code ProtobufHttpMessageConverter}.
   */
  public ProtobufHttpMessageConverter() {
    this(null, null);
  }

  /**
   * Construct a new {@code ProtobufHttpMessageConverter} with a registry that specifies
   * protocol message extensions.
   *
   * @param extensionRegistry the registry to populate
   */
  public ProtobufHttpMessageConverter(ExtensionRegistry extensionRegistry) {
    this(null, extensionRegistry);
  }

  ProtobufHttpMessageConverter(@Nullable ProtobufFormatSupport formatSupport,
                               @Nullable ExtensionRegistry extensionRegistry) {

    if (formatSupport != null) {
      this.protobufFormatSupport = formatSupport;
    }
    else if (ClassUtils.isPresent("com.googlecode.protobuf.format.FormatFactory", getClass().getClassLoader())) {
      this.protobufFormatSupport = new ProtobufJavaFormatSupport();
    }
    else if (ClassUtils.isPresent("com.google.protobuf.util.JsonFormat", getClass().getClassLoader())) {
      this.protobufFormatSupport = new ProtobufJavaUtilSupport(null, null);
    }
    else {
      this.protobufFormatSupport = null;
    }

    setSupportedMediaTypes(Arrays.asList(
            this.protobufFormatSupport != null ?
            this.protobufFormatSupport.supportedMediaTypes() : new MediaType[] { PROTOBUF, TEXT_PLAIN }));

    this.extensionRegistry = (extensionRegistry == null ? ExtensionRegistry.newInstance() : extensionRegistry);
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return Message.class.isAssignableFrom(clazz);
  }

  @Override
  protected MediaType getDefaultContentType(Message message) {
    return PROTOBUF;
  }

  @Override
  protected Message readInternal(Class<? extends Message> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    MediaType contentType = inputMessage.getHeaders().getContentType();
    if (contentType == null) {
      contentType = PROTOBUF;
    }
    Charset charset = contentType.getCharset();
    if (charset == null) {
      charset = DEFAULT_CHARSET;
    }

    Message.Builder builder = getMessageBuilder(clazz);
    if (PROTOBUF.isCompatibleWith(contentType)) {
      builder.mergeFrom(inputMessage.getBody(), this.extensionRegistry);
    }
    else if (TEXT_PLAIN.isCompatibleWith(contentType)) {
      InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), charset);
      TextFormat.merge(reader, this.extensionRegistry, builder);
    }
    else if (this.protobufFormatSupport != null) {
      this.protobufFormatSupport.merge(
              inputMessage.getBody(), charset, contentType, this.extensionRegistry, builder);
    }
    return builder.build();
  }

  /**
   * Create a new {@code Message.Builder} instance for the given class.
   * <p>This method uses a ConcurrentReferenceHashMap for caching method lookups.
   */
  private Message.Builder getMessageBuilder(Class<? extends Message> clazz) {
    try {
      Method method = methodCache.get(clazz);
      if (method == null) {
        method = clazz.getMethod("newBuilder");
        methodCache.put(clazz, method);
      }
      return (Message.Builder) method.invoke(clazz);
    }
    catch (Exception ex) {
      throw new HttpMessageConversionException(
              "Invalid Protobuf Message type: no invocable newBuilder() method on " + clazz, ex);
    }
  }

  @Override
  protected boolean canWrite(@Nullable MediaType mediaType) {
    return (super.canWrite(mediaType) ||
            (this.protobufFormatSupport != null && this.protobufFormatSupport.supportsWriteOnly(mediaType)));
  }

  @SuppressWarnings("deprecation")
  @Override
  protected void writeInternal(Message message, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {

    MediaType contentType = outputMessage.getHeaders().getContentType();
    if (contentType == null) {
      contentType = getDefaultContentType(message);
      Assert.state(contentType != null, "No content type");
    }
    Charset charset = contentType.getCharset();
    if (charset == null) {
      charset = DEFAULT_CHARSET;
    }

    if (PROTOBUF.isCompatibleWith(contentType)) {
      setProtoHeader(outputMessage, message);
      CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputMessage.getBody());
      message.writeTo(codedOutputStream);
      codedOutputStream.flush();
    }
    else if (TEXT_PLAIN.isCompatibleWith(contentType)) {
      OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputMessage.getBody(), charset);
      TextFormat.print(message, outputStreamWriter);  // deprecated on Protobuf 3.9
      outputStreamWriter.flush();
      outputMessage.getBody().flush();
    }
    else if (this.protobufFormatSupport != null) {
      this.protobufFormatSupport.print(message, outputMessage.getBody(), contentType, charset);
      outputMessage.getBody().flush();
    }
  }

  /**
   * Set the "X-Protobuf-*" HTTP headers when responding with a message of
   * content type "application/x-protobuf"
   * <p><b>Note:</b> <code>outputMessage.getBody()</code> should not have been called
   * before because it writes HTTP headers (making them read only).</p>
   */
  private void setProtoHeader(HttpOutputMessage response, Message message) {
    HttpHeaders headers = response.getHeaders();
    Descriptors.Descriptor descriptorForType = message.getDescriptorForType();
    headers.set(X_PROTOBUF_SCHEMA_HEADER, descriptorForType.getFile().getName());
    headers.set(X_PROTOBUF_MESSAGE_HEADER, descriptorForType.getFullName());
  }

  /**
   * Protobuf format support.
   */
  interface ProtobufFormatSupport {

    MediaType[] supportedMediaTypes();

    boolean supportsWriteOnly(@Nullable MediaType mediaType);

    void merge(InputStream input, Charset charset, MediaType contentType,
               ExtensionRegistry extensionRegistry, Message.Builder builder)
            throws IOException, HttpMessageConversionException;

    void print(Message message, OutputStream output, MediaType contentType, Charset charset)
            throws IOException, HttpMessageConversionException;
  }

  /**
   * {@link ProtobufFormatSupport} implementation used when
   * {@code com.googlecode.protobuf.format.FormatFactory} is available.
   */
  static class ProtobufJavaFormatSupport implements ProtobufFormatSupport {

    private final ProtobufFormatter jsonFormatter;

    private final ProtobufFormatter xmlFormatter;

    private final ProtobufFormatter htmlFormatter;

    public ProtobufJavaFormatSupport() {
      FormatFactory formatFactory = new FormatFactory();
      this.jsonFormatter = formatFactory.createFormatter(FormatFactory.Formatter.JSON);
      this.xmlFormatter = formatFactory.createFormatter(FormatFactory.Formatter.XML);
      this.htmlFormatter = formatFactory.createFormatter(FormatFactory.Formatter.HTML);
    }

    @Override
    public MediaType[] supportedMediaTypes() {
      return new MediaType[] { PROTOBUF, TEXT_PLAIN, APPLICATION_XML, APPLICATION_JSON };
    }

    @Override
    public boolean supportsWriteOnly(@Nullable MediaType mediaType) {
      return TEXT_HTML.isCompatibleWith(mediaType);
    }

    @Override
    public void merge(InputStream input, Charset charset, MediaType contentType,
                      ExtensionRegistry extensionRegistry, Message.Builder builder)
            throws IOException, HttpMessageConversionException //
    {
      if (contentType.isCompatibleWith(APPLICATION_JSON)) {
        this.jsonFormatter.merge(input, charset, extensionRegistry, builder);
      }
      else if (contentType.isCompatibleWith(APPLICATION_XML)) {
        this.xmlFormatter.merge(input, charset, extensionRegistry, builder);
      }
      else {
        throw new HttpMessageConversionException(
                "protobuf-java-format does not support parsing " + contentType);
      }
    }

    @Override
    public void print(Message message, OutputStream output, MediaType contentType, Charset charset)
            throws IOException, HttpMessageConversionException //
    {

      if (contentType.isCompatibleWith(APPLICATION_JSON)) {
        this.jsonFormatter.print(message, output, charset);
      }
      else if (contentType.isCompatibleWith(APPLICATION_XML)) {
        this.xmlFormatter.print(message, output, charset);
      }
      else if (contentType.isCompatibleWith(TEXT_HTML)) {
        this.htmlFormatter.print(message, output, charset);
      }
      else {
        throw new HttpMessageConversionException(
                "protobuf-java-format does not support printing " + contentType);
      }
    }
  }

  /**
   * {@link ProtobufFormatSupport} implementation used when
   * {@code com.google.protobuf.util.JsonFormat} is available.
   */
  static class ProtobufJavaUtilSupport implements ProtobufFormatSupport {

    private final JsonFormat.Parser parser;

    private final JsonFormat.Printer printer;

    public ProtobufJavaUtilSupport(@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {
      this.parser = (parser != null ? parser : JsonFormat.parser());
      this.printer = (printer != null ? printer : JsonFormat.printer());
    }

    @Override
    public MediaType[] supportedMediaTypes() {
      return new MediaType[] { PROTOBUF, TEXT_PLAIN, APPLICATION_JSON };
    }

    @Override
    public boolean supportsWriteOnly(@Nullable MediaType mediaType) {
      return false;
    }

    @Override
    public void merge(InputStream input, Charset charset, MediaType contentType,
                      ExtensionRegistry extensionRegistry, Message.Builder builder)
            throws IOException, HttpMessageConversionException //
    {
      if (contentType.isCompatibleWith(APPLICATION_JSON)) {
        InputStreamReader reader = new InputStreamReader(input, charset);
        this.parser.merge(reader, builder);
      }
      else {
        throw new HttpMessageConversionException(
                "protobuf-java-util does not support parsing " + contentType);
      }
    }

    @Override
    public void print(Message message, OutputStream output, MediaType contentType, Charset charset)
            throws IOException, HttpMessageConversionException //
    {

      if (contentType.isCompatibleWith(APPLICATION_JSON)) {
        OutputStreamWriter writer = new OutputStreamWriter(output, charset);
        this.printer.appendTo(message, writer);
        writer.flush();
      }
      else {
        throw new HttpMessageConversionException(
                "protobuf-java-util does not support printing " + contentType);
      }
    }
  }

}
