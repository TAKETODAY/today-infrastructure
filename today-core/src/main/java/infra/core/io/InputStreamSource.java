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

package infra.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import infra.lang.Constant;
import infra.util.function.ThrowingConsumer;

/**
 * Simple interface for objects that are sources for an {@link InputStream}.
 *
 * <p>This is the base interface for Infra more extensive {@link Resource} interface.
 *
 * <p>For single-use streams, {@link InputStreamResource} can be used for any
 * given {@code InputStream}. Infra {@link ByteArrayResource} or any
 * file-based {@code Resource} implementation can be used as a concrete
 * instance, allowing one to read the underlying content stream multiple times.
 * This makes this interface useful as an abstract content source for mail
 * attachments, for example.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.io.InputStream
 * @see Resource
 * @see InputStreamResource
 * @see ByteArrayResource
 * @since 2.1.6 2019-07-08 00:12
 */
@FunctionalInterface
public interface InputStreamSource extends ThrowingConsumer<OutputStream> {

  /**
   * Return an {@link InputStream} for the content of an underlying resource.
   * <p>It is usually expected that every such call creates a <i>fresh</i> stream.
   * <p>This requirement is particularly important when you consider an API such
   * as JavaMail, which needs to be able to read the stream multiple times when
   * creating mail attachments. For such a use case, it is <i>required</i>
   * that each {@code getInputStream()} call returns a fresh stream.
   *
   * @return the input stream for the underlying resource (must not be {@code null})
   * @throws java.io.FileNotFoundException if the underlying resource does not exist
   * @throws IOException if the content stream could not be opened
   * @see Resource#isReadable()
   * @see Resource#isOpen()
   */
  InputStream getInputStream() throws IOException;

  /**
   * Returns a {@link Reader} for the content of the underlying resource using the
   * default charset ({@link Constant#DEFAULT_CHARSET}). This method is a convenience
   * overload of {@link #getReader(Charset)} and is equivalent to calling
   * {@code getReader(Constant.DEFAULT_CHARSET)}.
   *
   * <p>This method is typically used when reading text-based resources where the
   * default charset is sufficient. It ensures that the content is interpreted
   * correctly according to the default encoding.
   *
   * <h3>Usage Example</h3>
   *
   * Reading the content of a resource as a string using the default charset:
   * <pre>{@code
   * try (Reader reader = inputStreamSource.getReader()) {
   *   StringBuilder content = new StringBuilder();
   *   char[] buffer = new char[1024];
   *   int charsRead;
   *   while ((charsRead = reader.read(buffer)) != -1) {
   *     content.append(buffer, 0, charsRead);
   *   }
   *   System.out.println(content.toString());
   * }
   * catch (IOException e) {
   *   e.printStackTrace();
   * }
   * }</pre>
   *
   * <p><b>Note:</b> The returned {@link Reader} should be closed after use to release
   * any underlying resources. Using a try-with-resources block is recommended.
   *
   * @return a {@link Reader} for the underlying resource's content using the
   * default charset
   * @throws IOException if an I/O error occurs while opening the reader or
   * accessing the underlying resource
   * @see #getReader(Charset)
   * @see Constant#DEFAULT_CHARSET
   */
  default Reader getReader() throws IOException {
    return getReader(Constant.DEFAULT_CHARSET);
  }

  /**
   * Returns a {@link Reader} for the content of the underlying resource using the specified charset.
   * This method wraps the {@link InputStream} returned by {@link #getInputStream()} with an
   * {@link InputStreamReader}, applying the provided charset for decoding the byte stream into characters.
   *
   * <p>This method is particularly useful when reading text-based resources where a specific character
   * encoding is required. It ensures that the content is interpreted correctly according to the given charset.
   *
   * <h3>Usage Example</h3>
   *
   * Reading the content of a resource as a string using a custom charset:
   * <pre>{@code
   * Charset utf16Charset = StandardCharsets.UTF_16;
   * try (Reader reader = inputStreamSource.getReader(utf16Charset)) {
   *   StringBuilder content = new StringBuilder();
   *   char[] buffer = new char[1024];
   *   int charsRead;
   *   while ((charsRead = reader.read(buffer)) != -1) {
   *     content.append(buffer, 0, charsRead);
   *   }
   *   System.out.println(content.toString());
   * }
   * catch (IOException e) {
   *   e.printStackTrace();
   * }
   * }</pre>
   *
   * <p><b>Note:</b> The returned {@link Reader} should be closed after use to release any underlying
   * resources. Using a try-with-resources block is recommended.
   *
   * @param encoding the charset to use for decoding the byte stream into characters (must not be {@code null})
   * @return a {@link Reader} for the underlying resource's content using the specified charset
   * @throws IOException if an I/O error occurs while opening the reader or accessing the underlying resource
   * @see #getInputStream()
   * @see InputStreamReader
   */
  default Reader getReader(Charset encoding) throws IOException {
    return new InputStreamReader(getInputStream(), encoding);
  }

  /**
   * Return a {@link ReadableByteChannel}.
   * <p>
   * It is expected that each call creates a <i>fresh</i> channel.
   * <p>
   * The default implementation returns {@link Channels#newChannel(InputStream)}
   * with the result of {@link #getInputStream()}.
   *
   * @return the byte channel for the underlying resource (must not be
   * {@code null})
   * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
   * @throws IOException if the content channel could not be opened
   * @see #getInputStream()
   */
  default ReadableByteChannel readableChannel() throws IOException {
    return Channels.newChannel(getInputStream());
  }

  /**
   * Reads all bytes from this {@link #getInputStream()} and writes the bytes to the
   * given output stream in the order that they are read. On return, this
   * input stream will be at end of stream. This method does not close either
   * stream.
   * <p>
   * This method may block indefinitely reading from the input stream, or
   * writing to the output stream. The behavior for the case where the input
   * and/or output stream is <i>asynchronously closed</i>, or the thread
   * interrupted during the transfer, is highly input and output stream
   * specific, and therefore not specified.
   * <p>
   * If an I/O error occurs reading from the input stream or writing to the
   * output stream, then it may do so after some bytes have been read or
   * written. Consequently the input stream may not be at end of stream and
   * one, or both, streams may be in an inconsistent state. It is strongly
   * recommended that both streams be promptly closed if an I/O error occurs.
   *
   * @param out the output stream, non-null
   * @return the number of bytes transferred
   * @throws IOException if an I/O error occurs when reading or writing
   * @throws NullPointerException if {@code out} is {@code null}
   * @since 4.0
   */
  default long transferTo(OutputStream out) throws IOException {
    try (InputStream in = getInputStream()) {
      return in.transferTo(out);
    }
  }

  /**
   * Accepts an {@link OutputStream} and transfers all bytes from the underlying
   * input stream to the provided output stream. This method delegates the transfer
   * logic to the {@link #transferTo(OutputStream)} method, which reads all bytes
   * from the input stream and writes them to the output stream in the order they
   * are read.
   *
   * <p>This method does not close either the input or output stream. If an I/O
   * error occurs during the transfer, it is strongly recommended to promptly close
   * both streams to avoid resource leaks or inconsistent states.
   *
   * <p><b>Note:</b> This method may block indefinitely while reading from the input
   * stream or writing to the output stream. The behavior in cases where the streams
   * are asynchronously closed or the thread is interrupted is implementation-specific
   * and therefore not guaranteed.
   *
   * <h3>Usage Example</h3>
   *
   * Transferring the content of an {@code InputStreamSource} to a file:
   * <pre>{@code
   * InputStreamSource source = ...; // Obtain the InputStreamSource
   * Path outputPath = Paths.get("output.txt");
   * try (OutputStream out = Files.newOutputStream(outputPath)) {
   *   source.acceptWithException(out);
   *   System.out.println("Data successfully written to " + outputPath);
   * }
   * catch (Exception e) {
   *   e.printStackTrace();
   * }
   * }</pre>
   *
   * @param out the output stream to which the data will be transferred (must not be null)
   * @throws Exception if an I/O error occurs during the transfer or if the provided
   * output stream is null
   * @see #transferTo(OutputStream)
   */
  @Override
  default void acceptWithException(OutputStream out) throws Exception {
    transferTo(out);
  }

}
