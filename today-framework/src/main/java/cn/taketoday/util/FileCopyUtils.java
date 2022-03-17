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

package cn.taketoday.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * Simple utility methods for file and stream copying. All copy methods use a block size
 * of 4096 bytes, and close all affected streams when done. A variation of the copy
 * methods from this class that leave streams open can be found in {@link StreamUtils}.
 *
 * <p>Mainly for use within the framework, but also useful for application code.
 *
 * @author Juergen Hoeller
 * @author Hyunjin Choi
 * @author TODAY 2021/8/21 00:04
 * @see StreamUtils
 * @see FileSystemUtils
 * @since 4.0
 */
public abstract class FileCopyUtils {

  /**
   * The default buffer size used when copying bytes.
   */
  public static final int BUFFER_SIZE = StreamUtils.BUFFER_SIZE;

  //---------------------------------------------------------------------
  // Copy methods for java.io.File
  //---------------------------------------------------------------------

  /**
   * Copy the contents of the given input File to the given output File.
   *
   * @param in the file to copy from
   * @param out the file to copy to
   * @return the number of bytes copied
   * @throws IOException in case of I/O errors
   */
  public static int copy(File in, File out) throws IOException {
    Assert.notNull(in, "No input File specified");
    Assert.notNull(out, "No output File specified");
    return copy(Files.newInputStream(in.toPath()), Files.newOutputStream(out.toPath()));
  }

  /**
   * Copy the contents of the given byte array to the given output File.
   *
   * @param in the byte array to copy from
   * @param out the file to copy to
   * @throws IOException in case of I/O errors
   */
  public static void copy(byte[] in, File out) throws IOException {
    Assert.notNull(in, "No input byte array specified");
    Assert.notNull(out, "No output File specified");
    copy(new ByteArrayInputStream(in), Files.newOutputStream(out.toPath()));
  }

  /**
   * Copy the contents of the given input File into a new byte array.
   *
   * @param in the file to copy from
   * @return the new byte array that has been copied to
   * @throws IOException in case of I/O errors
   */
  public static byte[] copyToByteArray(File in) throws IOException {
    Assert.notNull(in, "No input File specified");
    return copyToByteArray(Files.newInputStream(in.toPath()));
  }

  //---------------------------------------------------------------------
  // Copy methods for java.io.InputStream / java.io.OutputStream
  //---------------------------------------------------------------------

  /**
   * Copy the contents of the given InputStream to the given OutputStream.
   * Closes both streams when done.
   *
   * @param in the stream to copy from
   * @param out the stream to copy to
   * @return the number of bytes copied
   * @throws IOException in case of I/O errors
   */
  public static int copy(InputStream in, OutputStream out) throws IOException {
    try {
      return StreamUtils.copy(in, out);
    }
    finally {
      close(in);
      close(out);
    }
  }

  /**
   * Copy the contents of the given byte array to the given OutputStream.
   * Closes the stream when done.
   *
   * @param in the byte array to copy from
   * @param out the OutputStream to copy to
   * @throws IOException in case of I/O errors
   */
  public static void copy(byte[] in, OutputStream out) throws IOException {
    Assert.notNull(in, "No input byte array specified");
    Assert.notNull(out, "No OutputStream specified");

    try {
      out.write(in);
    }
    finally {
      close(out);
    }
  }

  /**
   * Copy the contents of the given InputStream into a new byte array.
   * Closes the stream when done.
   *
   * @param in the stream to copy from (may be {@code null} or empty)
   * @return the new byte array that has been copied to (possibly empty)
   * @throws IOException in case of I/O errors
   */
  public static byte[] copyToByteArray(@Nullable InputStream in) throws IOException {
    return copyToByteArray(in, BUFFER_SIZE);
  }

  /**
   * Copy the contents of the given InputStream into a new byte array.
   * Closes the stream when done.
   *
   * @param in the stream to copy from (may be {@code null} or empty)
   * @param bufferSize user specified buffer size
   * @return the new byte array that has been copied to (possibly empty)
   * @throws IOException in case of I/O errors
   */
  public static byte[] copyToByteArray(@Nullable InputStream in, int bufferSize) throws IOException {
    if (in == null) {
      return new byte[0];
    }

    ByteArrayOutputStream out = new ByteArrayOutputStream(bufferSize);
    copy(in, out);
    return out.toByteArray();
  }

  //---------------------------------------------------------------------
  // Copy methods for java.io.Reader / java.io.Writer
  //---------------------------------------------------------------------

  /**
   * Copy the contents of the given Reader to the given Writer.
   * Closes both when done.
   *
   * @param in the Reader to copy from
   * @param out the Writer to copy to
   * @return the number of characters copied
   * @throws IOException in case of I/O errors
   */
  public static int copy(Reader in, Writer out) throws IOException {
    return copy(in, out, BUFFER_SIZE);
  }

  /**
   * Copy the contents of the given Reader to the given Writer.
   * Closes both when done.
   *
   * @param in the Reader to copy from
   * @param out the Writer to copy to
   * @param bufferSize user specified buffer size
   * @return the number of characters copied
   * @throws IOException in case of I/O errors
   */
  public static int copy(Reader in, Writer out, int bufferSize) throws IOException {
    Assert.notNull(in, "No Reader specified");
    Assert.notNull(out, "No Writer specified");

    try {
      int charCount = 0;
      char[] buffer = new char[bufferSize];
      int charsRead;
      while ((charsRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, charsRead);
        charCount += charsRead;
      }
      out.flush();
      return charCount;
    }
    finally {
      close(in);
      close(out);
    }
  }

  /**
   * Copy the contents of the given String to the given Writer.
   * Closes the writer when done.
   *
   * @param in the String to copy from
   * @param out the Writer to copy to
   * @throws IOException in case of I/O errors
   */
  public static void copy(String in, Writer out) throws IOException {
    Assert.notNull(in, "No input String specified");
    Assert.notNull(out, "No Writer specified");

    try {
      out.write(in);
    }
    finally {
      close(out);
    }
  }

  /**
   * Copy the contents of the given Reader into a String.
   * Closes the reader when done.
   *
   * @param in the reader to copy from (may be {@code null} or empty)
   * @return the String that has been copied to (possibly empty)
   * @throws IOException in case of I/O errors
   */
  public static String copyToString(@Nullable Reader in) throws IOException {
    return copyToString(in, BUFFER_SIZE);
  }

  /**
   * Copy the contents of the given Reader into a String.
   * Closes the reader when done.
   *
   * @param in the reader to copy from (may be {@code null} or empty)
   * @param bufferSize user specified buffer size
   * @return the String that has been copied to (possibly empty)
   * @throws IOException in case of I/O errors
   */
  public static String copyToString(@Nullable Reader in, int bufferSize) throws IOException {
    if (in == null) {
      return Constant.BLANK;
    }
    StringWriter out = new StringWriter(bufferSize);
    copy(in, out);
    return out.toString();
  }

  /**
   * Attempt to close the supplied {@link Closeable}, silently swallowing any
   * exceptions.
   *
   * @param closeable the {@code Closeable} to close
   */
  private static void close(@Nullable Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      }
      catch (IOException ex) {
        // ignore
      }
    }
  }

}
