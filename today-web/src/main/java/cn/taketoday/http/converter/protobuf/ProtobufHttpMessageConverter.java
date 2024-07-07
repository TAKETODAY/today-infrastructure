/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.http.converter.protobuf;

import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.AbstractHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConversionException;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;

import static cn.taketoday.http.MediaType.APPLICATION_JSON;
import static cn.taketoday.http.MediaType.TEXT_PLAIN;

/**
 * An {@code HttpMessageConverter} that reads and writes
 * {@link com.google.protobuf.Message com.google.protobuf.Messages} using
 * <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>.
 *
 * <p>To generate {@code Message} Java classes, you need to install the {@code protoc} binary.
 *
 * <p>This converter supports by default {@code "application/x-protobuf"} and {@code "text/plain"}
 * with the official {@code "com.google.protobuf:protobuf-java"} library.
 * The {@code "application/json"} format is also supported with the {@code "com.google.protobuf:protobuf-java-util"}
 * dependency. See {@link ProtobufJsonFormatHttpMessageConverter} for a configurable variant.
 *
 * <p>This converter requires Protobuf 3 or higher as of 4.0
 *
 * @author Alex Antonov
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JsonFormat
 * @see ProtobufJsonFormatHttpMessageConverter
 * @since 4.0
 */
public class ProtobufHttpMessageConverter extends AbstractHttpMessageConverter<MessageOrBuilder> {

  /**
   * The media-type for protobuf {@code application/x-protobuf}.
   */
  public static final MediaType PROTOBUF = new MediaType("application", "x-protobuf", Constant.DEFAULT_CHARSET);

  /**
   * The HTTP header containing the protobuf schema.
   */
  public static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";

  /**
   * The HTTP header containing the protobuf message.
   */
  public static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";

  private static final ConcurrentReferenceHashMap<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

  final ExtensionRegistry extensionRegistry;

  @Nullable
  private final ProtobufFormatSupport protobufFormatSupport;

  /**
   * Flag populate protobuf headers to response
   */
  private boolean populateProtoHeader = true;

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

  ProtobufHttpMessageConverter(@Nullable ProtobufFormatSupport formatSupport, @Nullable ExtensionRegistry extensionRegistry) {
    if (formatSupport != null) {
      this.protobufFormatSupport = formatSupport;
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

  /**
   * Flag populate protobuf headers to response
   *
   * @param populateProtoHeader populate protobuf headers to response
   * @see #X_PROTOBUF_SCHEMA_HEADER
   * @see #X_PROTOBUF_MESSAGE_HEADER
   * @see #setProtoHeader(HttpOutputMessage, MessageOrBuilder)
   */
  public void setPopulateProtoHeader(boolean populateProtoHeader) {
    this.populateProtoHeader = populateProtoHeader;
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return MessageOrBuilder.class.isAssignableFrom(clazz);
  }

  @Override
  public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
    return Message.class.isAssignableFrom(clazz) && canRead(mediaType);
  }

  @Override
  protected MediaType getDefaultContentType(MessageOrBuilder message) {
    return PROTOBUF;
  }

  @Override
  protected Message readInternal(Class<? extends MessageOrBuilder> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    MediaType contentType = inputMessage.getHeaders().getContentType();
    if (contentType == null) {
      contentType = PROTOBUF;
    }

    Message.Builder builder = getMessageBuilder(clazz);
    if (PROTOBUF.isCompatibleWith(contentType)) {
      builder.mergeFrom(inputMessage.getBody(), this.extensionRegistry);
    }
    else if (TEXT_PLAIN.isCompatibleWith(contentType)) {
      Charset charset = getCharset(contentType);
      InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), charset);
      TextFormat.merge(reader, this.extensionRegistry, builder);
    }
    else if (protobufFormatSupport != null) {
      Charset charset = getCharset(contentType);
      protobufFormatSupport.merge(inputMessage, charset, contentType, this.extensionRegistry, builder);
    }
    return builder.build();
  }

  /**
   * Create a new {@code Message.Builder} instance for the given class.
   * <p>This method uses a ConcurrentReferenceHashMap for caching method lookups.
   */
  private Message.Builder getMessageBuilder(Class<?> clazz) {
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
    return super.canWrite(mediaType)
            || (protobufFormatSupport != null && protobufFormatSupport.supportsWriteOnly(mediaType));
  }

  @Override
  protected void writeInternal(MessageOrBuilder message, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException //
  {

    MediaType contentType = outputMessage.getHeaders().getContentType();
    if (contentType == null) {
      contentType = getDefaultContentType(message);
      Assert.state(contentType != null, "No content type");
    }

    if (PROTOBUF.isCompatibleWith(contentType)) {
      if (populateProtoHeader) {
        setProtoHeader(outputMessage, message);
      }
      if (message instanceof Message.Builder builder) {
        builder.build().writeTo(outputMessage.getBody());
      }
      else {
        ((Message) message).writeTo(outputMessage.getBody());
      }
    }
    else if (TEXT_PLAIN.isCompatibleWith(contentType)) {
      OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputMessage.getBody(), getCharset(contentType));
      TextFormat.printer().print(message, outputStreamWriter);
      outputStreamWriter.flush();
    }
    else if (protobufFormatSupport != null) {
      protobufFormatSupport.print(message, outputMessage, contentType);
    }
  }

  private static Charset getCharset(MediaType contentType) {
    Charset charset = contentType.getCharset();
    if (charset == null) {
      charset = Constant.DEFAULT_CHARSET;
    }
    return charset;
  }

  /**
   * Set the "X-Protobuf-*" HTTP headers when responding with a message of
   * content type "application/x-protobuf"
   * <p><b>Note:</b> <code>outputMessage.getBody()</code> should not have been called
   * before because it writes HTTP headers (making them read only).</p>
   */
  protected void setProtoHeader(HttpOutputMessage response, MessageOrBuilder message) {
    HttpHeaders headers = response.getHeaders();
    Descriptors.Descriptor descriptorForType = message.getDescriptorForType();
    headers.setOrRemove(X_PROTOBUF_SCHEMA_HEADER, descriptorForType.getFile().getName());
    headers.setOrRemove(X_PROTOBUF_MESSAGE_HEADER, descriptorForType.getFullName());
  }

  @Override
  protected boolean supportsRepeatableWrites(MessageOrBuilder message) {
    return true;
  }

  /**
   * Protobuf format support.
   */
  interface ProtobufFormatSupport {

    MediaType[] supportedMediaTypes();

    boolean supportsWriteOnly(@Nullable MediaType mediaType);

    void merge(HttpInputMessage input, Charset charset, MediaType contentType,
            ExtensionRegistry extensionRegistry, Message.Builder builder)
            throws IOException, HttpMessageConversionException;

    void print(MessageOrBuilder message, HttpOutputMessage output, MediaType contentType)
            throws IOException, HttpMessageConversionException;
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
    public void merge(HttpInputMessage input, Charset charset, MediaType contentType,
            ExtensionRegistry extensionRegistry, Message.Builder builder)
            throws IOException, HttpMessageConversionException //
    {

      if (contentType.isCompatibleWith(APPLICATION_JSON)) {
        InputStreamReader reader = new InputStreamReader(input.getBody(), charset);
        this.parser.merge(reader, builder);
      }
      else {
        throw new HttpMessageConversionException(
                "protobuf-java-util does not support parsing " + contentType);
      }
    }

    @Override
    public void print(MessageOrBuilder message, HttpOutputMessage output, MediaType contentType)
            throws IOException, HttpMessageConversionException //
    {

      if (contentType.isCompatibleWith(APPLICATION_JSON)) {
        OutputStreamWriter writer = new OutputStreamWriter(output.getBody(), getCharset(contentType));
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
