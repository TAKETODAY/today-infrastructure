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

package infra.http.converter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.StreamingHttpOutputMessage;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.StreamUtils;
import infra.util.StringUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can read and write
 * {@link BufferedImage BufferedImages}.
 *
 * <p>By default, this converter can read all media types that are supported
 * by the {@linkplain ImageIO#getReaderMIMETypes() registered image readers},
 * and writes using the media type of the first available
 * {@linkplain ImageIO#getWriterMIMETypes() registered image writer}.
 * The latter can be overridden by setting the
 * {@link #setDefaultContentType defaultContentType} property.
 *
 * <p>If the {@link #setCacheDir cacheDir} property is set, this converter
 * will cache image data.
 *
 * <p>The {@link #process(ImageReadParam)} and {@link #process(ImageWriteParam)}
 * template methods allow subclasses to override Image I/O parameters.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BufferedImageHttpMessageConverter implements HttpMessageConverter<BufferedImage> {

  private final ArrayList<MediaType> readableMediaTypes = new ArrayList<>();

  @Nullable
  private File cacheDir;

  @Nullable
  private MediaType defaultContentType;

  public BufferedImageHttpMessageConverter() {
    String[] readerMediaTypes = ImageIO.getReaderMIMETypes();
    for (String mediaType : readerMediaTypes) {
      if (StringUtils.hasText(mediaType)) {
        this.readableMediaTypes.add(MediaType.parseMediaType(mediaType));
      }
    }

    String[] writerMediaTypes = ImageIO.getWriterMIMETypes();
    for (String mediaType : writerMediaTypes) {
      if (StringUtils.hasText(mediaType)) {
        this.defaultContentType = MediaType.parseMediaType(mediaType);
        break;
      }
    }
  }

  /**
   * Sets the default {@code Content-Type} to be used for writing.
   *
   * @throws IllegalArgumentException if the given content type is not supported by the Java Image I/O API
   */
  public void setDefaultContentType(@Nullable MediaType defaultContentType) {
    if (defaultContentType != null) {
      Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(defaultContentType.toString());
      if (!imageWriters.hasNext()) {
        throw new IllegalArgumentException(
                "Content-Type [" + defaultContentType + "] is not supported by the Java Image I/O API");
      }
    }

    this.defaultContentType = defaultContentType;
  }

  /**
   * Returns the default {@code Content-Type} to be used for writing.
   * Called when {@link #write} is invoked without a specified content type parameter.
   */
  @Nullable
  public MediaType getDefaultContentType() {
    return this.defaultContentType;
  }

  /**
   * Sets the cache directory. If this property is set to an existing directory,
   * this converter will cache image data.
   */
  public void setCacheDir(File cacheDir) {
    Assert.notNull(cacheDir, "'cacheDir' is required");
    if (!cacheDir.isDirectory()) {
      throw new IllegalArgumentException("'cacheDir' is not a directory: " + cacheDir);
    }
    this.cacheDir = cacheDir;
  }

  @Override
  public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
    return BufferedImage.class == clazz && isReadable(mediaType);
  }

  private boolean isReadable(@Nullable MediaType mediaType) {
    if (mediaType == null) {
      return true;
    }
    Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(mediaType.toString());
    return imageReaders.hasNext();
  }

  @Override
  public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
    return BufferedImage.class == clazz && isWritable(mediaType);
  }

  private boolean isWritable(@Nullable MediaType mediaType) {
    if (mediaType == null || MediaType.ALL.equalsTypeAndSubtype(mediaType)) {
      return true;
    }
    Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(mediaType.toString());
    return imageWriters.hasNext();
  }

  @Override
  public List<MediaType> getSupportedMediaTypes() {
    return this.readableMediaTypes;
  }

  @Override
  public BufferedImage read(@Nullable Class<? extends BufferedImage> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException //
  {
    ImageReader imageReader = null;
    try (ImageInputStream imageInputStream = createImageInputStream(inputMessage.getBody())) {
      MediaType contentType = inputMessage.getHeaders().getContentType();
      if (contentType == null) {
        throw new HttpMessageNotReadableException("No Content-Type header", inputMessage);
      }
      Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(contentType.toString());
      if (imageReaders.hasNext()) {
        imageReader = imageReaders.next();
        ImageReadParam irp = imageReader.getDefaultReadParam();
        process(irp);
        imageReader.setInput(imageInputStream, true);
        return imageReader.read(0, irp);
      }
      else {
        throw new HttpMessageNotReadableException(
                "Could not find javax.imageio.ImageReader for Content-Type [" + contentType + "]",
                inputMessage);
      }
    }
    finally {
      if (imageReader != null) {
        imageReader.dispose();
      }
      // ignore
    }
  }

  private ImageInputStream createImageInputStream(InputStream is) throws IOException {
    is = StreamUtils.nonClosing(is);
    if (this.cacheDir != null) {
      return new FileCacheImageInputStream(is, this.cacheDir);
    }
    else {
      return new MemoryCacheImageInputStream(is);
    }
  }

  @Override
  public void write(BufferedImage image, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException //
  {
    MediaType selectedContentType = getContentType(contentType);
    outputMessage.getHeaders().setContentType(selectedContentType);

    if (outputMessage instanceof StreamingHttpOutputMessage streaming) {
      streaming.setBody(new StreamingHttpOutputMessage.Body() {
        @Override
        public void writeTo(OutputStream outputStream) throws IOException {
          BufferedImageHttpMessageConverter.this.writeInternal(image, selectedContentType, outputStream);
        }

        @Override
        public boolean repeatable() {
          return true;
        }
      });
    }
    else {
      writeInternal(image, selectedContentType, outputMessage.getBody());
    }
  }

  private MediaType getContentType(@Nullable MediaType contentType) {
    if (contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype()) {
      contentType = getDefaultContentType();
    }
    Assert.notNull(contentType, "Could not select Content-Type. Please specify one through the 'defaultContentType' property.");
    return contentType;
  }

  private void writeInternal(BufferedImage image, MediaType contentType, OutputStream body)
          throws IOException, HttpMessageNotWritableException //
  {
    ImageOutputStream imageOutputStream = null;
    ImageWriter imageWriter = null;
    try {
      Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(contentType.toString());
      if (imageWriters.hasNext()) {
        imageWriter = imageWriters.next();
        ImageWriteParam iwp = imageWriter.getDefaultWriteParam();
        process(iwp);
        imageOutputStream = createImageOutputStream(body);
        imageWriter.setOutput(imageOutputStream);
        imageWriter.write(null, new IIOImage(image, null, null), iwp);
      }
      else {
        throw new HttpMessageNotWritableException(
                "Could not find javax.imageio.ImageWriter for Content-Type [" + contentType + "]");
      }
    }
    finally {
      if (imageWriter != null) {
        imageWriter.dispose();
      }
      if (imageOutputStream != null) {
        try {
          imageOutputStream.close();
        }
        catch (IOException ex) {
          // ignore
        }
      }
    }
  }

  private ImageOutputStream createImageOutputStream(OutputStream os) throws IOException {
    if (this.cacheDir != null) {
      return new FileCacheImageOutputStream(os, this.cacheDir);
    }
    else {
      return new MemoryCacheImageOutputStream(os);
    }
  }

  /**
   * Template method that allows for manipulating the {@link ImageReadParam}
   * before it is used to read an image.
   * <p>The default implementation is empty.
   */
  protected void process(ImageReadParam irp) { }

  /**
   * Template method that allows for manipulating the {@link ImageWriteParam}
   * before it is used to write an image.
   * <p>The default implementation is empty.
   */
  protected void process(ImageWriteParam iwp) { }

}