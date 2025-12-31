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

package infra.web.multipart.parsing;

import org.jspecify.annotations.Nullable;

/**
 * Internal class, which is used to invoke the {@link ProgressListener}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/28 22:22
 */
final class ProgressNotifier {

  /**
   * The listener to invoke.
   */
  private final ProgressListener listener;

  /**
   * Number of expected bytes, if known, or -1.
   */
  private final long contentLength;

  /**
   * Number of bytes, which have been read so far.
   */
  private long bytesRead;

  /**
   * Number of items, which have been read so far.
   */
  private int items;

  /**
   * Creates a new instance with the given listener and content length.
   *
   * @param listener The listener to invoke.
   * @param contentLength The expected content length.
   */
  public ProgressNotifier(final @Nullable ProgressListener listener, final long contentLength) {
    this.listener = listener != null ? listener : ProgressListener.NOP;
    this.contentLength = contentLength;
  }

  /**
   * Called to indicate that bytes have been read.
   *
   * @param byteCount Number of bytes, which have been read.
   */
  public void onBytesRead(final int byteCount) {
    // Indicates, that the given number of bytes have been read from the input stream.
    bytesRead += byteCount;
    notifyListener();
  }

  /**
   * Called to indicate, that a new file item has been detected.
   */
  public void onItem() {
    ++items;
    notifyListener();
  }

  /**
   * Called for notifying the listener.
   */
  private void notifyListener() {
    listener.update(bytesRead, contentLength, items);
  }

}
