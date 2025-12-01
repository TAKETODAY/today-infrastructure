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

package infra.util;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import infra.lang.Assert;
import infra.lang.Constant;

/**
 * Simple utility methods for dealing with streams. The copy methods of this class are
 * similar to those defined in {@link FileCopyUtils} except that all affected streams are
 * left open when done. All copy methods use a block size of 8192 bytes.
 *
 * <p>Mainly for use within the framework, but also useful for application code.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FileCopyUtils
 * @since 4.0 2021/8/21 00:03
 */
public abstract class StreamUtils {

  /**
   * The default buffer size used when copying bytes.
   */
  public static final int BUFFER_SIZE = 8192;

  /**
   * Copy the contents of the given InputStream into a new byte array.
   * <p>Leaves the stream open when done.
   *
   * @param in the stream to copy from (may be {@code null} or empty)
   * @return the new byte array that has been copied to (possibly empty)
   * @throws IOException in case of I/O errors
   * @see FileCopyUtils#copyToByteArray(InputStream)
   */
  public static byte[] copyToByteArray(@Nullable InputStream in) throws IOException {
    if (in == null) {
      return Constant.EMPTY_BYTES;
    }
    return in.readAllBytes();
  }

  /**
   * Copy the contents of the given InputStream into a String.
   * <p>Leaves the stream open when done.
   *
   * @param in the InputStream to copy from (may be {@code null} or empty)
   * @return the String that has been copied to (possibly empty)
   * @throws IOException in case of I/O errors
   */
  public static String copyToString(@Nullable InputStream in) throws IOException {
    return copyToString(in, StandardCharsets.UTF_8);
  }

  /**
   * Copy the contents of the given InputStream into a String.
   * <p>Leaves the stream open when done.
   *
   * @param in the InputStream to copy from (may be {@code null} or empty)
   * @param charset the {@link Charset} to use to decode the bytes
   * @return the String that has been copied to (possibly empty)
   * @throws IOException in case of I/O errors
   */
  public static String copyToString(@Nullable InputStream in, Charset charset) throws IOException {
    return copyToString(in, charset, BUFFER_SIZE);
  }

  /**
   * Copy the contents of the given InputStream into a String.
   * <p>Leaves the stream open when done.
   *
   * @param in the InputStream to copy from (may be {@code null} or empty)
   * @param charset the {@link Charset} to use to decode the bytes
   * @param bufferSize user specified buffer size
   * @return the String that has been copied to (possibly empty)
   * @throws IOException in case of I/O errors
   */
  public static String copyToString(@Nullable InputStream in, Charset charset, int bufferSize) throws IOException {
    if (in == null) {
      return Constant.BLANK;
    }

    StringBuilder out = new StringBuilder(bufferSize);
    InputStreamReader reader = new InputStreamReader(in, charset);
    char[] buffer = new char[bufferSize];
    int charsRead;
    while ((charsRead = reader.read(buffer)) != -1) {
      out.append(buffer, 0, charsRead);
    }
    return out.toString();
  }

  /**
   * Copy the contents of the given {@link ByteArrayOutputStream} into a {@link String}.
   * <p>This is a more effective equivalent of {@code new String(baos.toByteArray(), charset)}.
   *
   * @param baos the {@code ByteArrayOutputStream} to be copied into a String
   * @param charset the {@link Charset} to use to decode the bytes
   * @return the String that has been copied to (possibly empty)
   */
  public static String copyToString(ByteArrayOutputStream baos, Charset charset) {
    Assert.notNull(baos, "No ByteArrayOutputStream specified");
    Assert.notNull(charset, "No Charset specified");
    return baos.toString(charset);
  }

  /**
   * Copy the contents of the given byte array to the given OutputStream.
   * <p>Leaves the stream open when done.
   *
   * @param in the byte array to copy from
   * @param out the OutputStream to copy to
   * @throws IOException in case of I/O errors
   */
  public static void copy(byte[] in, OutputStream out) throws IOException {
    Assert.notNull(in, "No input byte array specified");
    Assert.notNull(out, "No OutputStream specified");

    out.write(in);
    out.flush();
  }

  /**
   * Copy the contents of the given String to the given OutputStream.
   * <p>Leaves the stream open when done.
   *
   * @param in the String to copy from
   * @param charset the Charset
   * @param out the OutputStream to copy to
   * @throws IOException in case of I/O errors
   */
  public static void copy(String in, Charset charset, OutputStream out) throws IOException {
    Assert.notNull(in, "No input String specified");
    Assert.notNull(charset, "No Charset specified");
    Assert.notNull(out, "No OutputStream specified");

    out.write(in.getBytes(charset));
    out.flush();
  }

  /**
   * Copy the contents of the given InputStream to the given OutputStream.
   * <p>Leaves both streams open when done.
   *
   * <p>
   * The buffer size is given by {@link #BUFFER_SIZE}.
   * </p>
   *
   * @param in the InputStream to copy from
   * @param out the OutputStream to copy to
   * @return the number of bytes copied
   * @throws IOException in case of I/O errors
   */
  public static long copy(InputStream in, OutputStream out) throws IOException {
    return copy(in, out, BUFFER_SIZE);
  }

  /**
   * Copy the contents of the given InputStream to the given OutputStream.
   * <p>Leaves both streams open when done.
   *
   * @param in the InputStream to copy from
   * @param out the OutputStream to copy to
   * @param bufferSize the copy buffer size
   * @return the number of bytes copied
   * @throws IOException in case of I/O errors
   */
  public static long copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
    return copy(in, out, new byte[bufferSize]);
  }

  /**
   * Copy the contents of the given InputStream to the given OutputStream.
   * <p>Leaves both streams open when done.
   *
   * @param in the InputStream to copy from
   * @param out the OutputStream to copy to
   * @param buffer the buffer to use for the copy
   * @return the number of bytes copied
   * @throws IOException in case of I/O errors
   * @throws NullPointerException if the buffer is {@code null}.
   * @since 5.0
   */
  public static long copy(InputStream in, OutputStream out, byte[] buffer) throws IOException {
    Assert.notNull(in, "No InputStream specified");
    Assert.notNull(out, "No OutputStream specified");

    long count = 0;
    int n;
    while ((n = in.read(buffer)) != -1) {
      out.write(buffer, 0, n);
      count += n;
    }
    out.flush();
    return count;
  }

  /**
   * Copy a range of content of the given InputStream to the given OutputStream.
   * <p>If the specified range exceeds the length of the InputStream, this copies
   * up to the end of the stream and returns the actual number of copied bytes.
   * <p>Leaves both streams open when done.
   *
   * @param in the InputStream to copy from
   * @param out the OutputStream to copy to
   * @param start the position to start copying from
   * @param end the position to end copying
   * @return the number of bytes copied
   * @throws IOException in case of I/O errors
   */
  public static long copyRange(InputStream in, OutputStream out, long start, long end) throws IOException {
    Assert.notNull(in, "No InputStream specified");
    Assert.notNull(out, "No OutputStream specified");

    long skipped = in.skip(start);
    if (skipped < start) {
      throw new IOException("Skipped only %d bytes out of %d required".formatted(skipped, start));
    }

    long bytesToCopy = end - start + 1;
    byte[] buffer = new byte[(int) Math.min(StreamUtils.BUFFER_SIZE, bytesToCopy)];
    while (bytesToCopy > 0) {
      int bytesRead = bytesToCopy < buffer.length ? in.read(buffer, 0, (int) bytesToCopy) : in.read(buffer);
      if (bytesRead == -1) {
        break;
      }
      out.write(buffer, 0, bytesRead);
      bytesToCopy -= bytesRead;
    }
    return (end - start + 1 - bytesToCopy);
  }

  /**
   * Drain the remaining content of the given InputStream.
   * <p>Leaves the InputStream open when done.
   *
   * @param in the InputStream to drain
   * @return the number of bytes read
   * @throws IOException in case of I/O errors
   */
  public static int drain(@Nullable InputStream in) throws IOException {
    return drain(in, BUFFER_SIZE);
  }

  /**
   * Drain the remaining content of the given InputStream.
   * <p>Leaves the InputStream open when done.
   *
   * @param in the InputStream to drain
   * @param bufferSize buffer size
   * @return the number of bytes read
   * @throws IOException in case of I/O errors
   */
  public static int drain(@Nullable InputStream in, int bufferSize) throws IOException {
    if (in == null) {
      return 0;
    }

    byte[] buffer = new byte[bufferSize];
    int bytesRead;
    int byteCount = 0;
    while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1) {
      byteCount += bytesRead;
    }
    return byteCount;
  }

  /**
   * Return a variant of the given {@link InputStream} where calling
   * {@link InputStream#close() close()} has no effect.
   *
   * @param in the InputStream to decorate
   * @return a version of the InputStream that ignores calls to close
   */
  public static InputStream nonClosing(InputStream in) {
    Assert.notNull(in, "No InputStream specified");
    return new NonClosingInputStream(in);
  }

  /**
   * Return a variant of the given {@link OutputStream} where calling
   * {@link OutputStream#close() close()} has no effect.
   *
   * @param out the OutputStream to decorate
   * @return a version of the OutputStream that ignores calls to close
   */
  public static OutputStream nonClosing(OutputStream out) {
    Assert.notNull(out, "No OutputStream specified");
    return new NonClosingOutputStream(out);
  }

  /**
   * Closes an {@link AutoCloseable} unconditionally.
   * <p>
   * Equivalent to {@link AutoCloseable#close()}, except any exceptions will be ignored.
   * This is typically used in finally blocks.
   * </p>
   * <p>
   * Example code:
   * </p>
   * <pre>
   *   byte[] data = new byte[1024];
   *   InputStream in = null;
   *   try {
   *       in = new FileInputStream("foo.txt");
   *       in.read(data);
   *       in.close(); //close errors are handled
   *   } catch (Exception e) {
   *       // error handling
   *   } finally {
   *       StreamUtils.closeQuietly(in);
   *   }
   * </pre>
   * <p>
   * Also consider using a try-with-resources statement where appropriate.
   * </p>
   *
   * @param input the InputStream to close, may be null or already closed.
   * @see Throwable#addSuppressed(Throwable)
   * @since 5.0
   */
  public static void closeQuietly(final AutoCloseable input) {
    closeQuietly(input, null);
  }

  /**
   * Closes the given {@link AutoCloseable} as a null-safe operation while consuming IOException by the given {@code consumer}.
   *
   * @param closeable The resource to close, may be null.
   * @param consumer Consumes the Exception thrown by {@link AutoCloseable#close()}.
   * @since 5.0
   */
  public static void closeQuietly(final @Nullable AutoCloseable closeable, final @Nullable Consumer<Exception> consumer) {
    if (closeable != null) {
      try {
        closeable.close();
      }
      catch (final Exception e) {
        if (consumer != null) {
          consumer.accept(e);
        }
      }
    }
  }

  private static class NonClosingInputStream extends FilterInputStream {

    public NonClosingInputStream(InputStream in) {
      super(in);
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public byte[] readAllBytes() throws IOException {
      return in.readAllBytes();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
      return in.readNBytes(len);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
      return in.readNBytes(b, off, len);
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
      return in.transferTo(out);
    }
  }

  private static class NonClosingOutputStream extends FilterOutputStream {

    public NonClosingOutputStream(OutputStream out) {
      super(out);
    }

    @Override
    public void write(byte[] b, int off, int let) throws IOException {
      // It is critical that we override this method for performance
      this.out.write(b, off, let);
    }

    @Override
    public void close() throws IOException {
    }
  }

}
