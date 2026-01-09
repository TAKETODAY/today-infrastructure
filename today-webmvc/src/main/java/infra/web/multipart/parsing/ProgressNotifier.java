/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
