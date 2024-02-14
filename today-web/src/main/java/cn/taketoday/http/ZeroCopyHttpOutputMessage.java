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

package cn.taketoday.http;

import java.io.File;
import java.nio.file.Path;

import reactor.core.publisher.Mono;

/**
 * Sub-interface of {@code ReactiveOutputMessage} that has support for "zero-copy"
 * file transfers.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see <a href="https://en.wikipedia.org/wiki/Zero-copy">Zero-copy</a>
 * @since 4.0
 */
public interface ZeroCopyHttpOutputMessage extends ReactiveHttpOutputMessage {

  /**
   * Use the given {@link File} to write the body of the message to the underlying
   * HTTP layer.
   *
   * @param file the file to transfer
   * @return a publisher that indicates completion or error.
   */
  default Mono<Void> writeWith(File file) {
    return writeWith(file, 0, file.length());
  }

  /**
   * Use the given {@link File} to write the body of the message to the underlying
   * HTTP layer.
   *
   * @param file the file to transfer
   * @param position the position within the file from which the transfer is to begin
   * @param count the number of bytes to be transferred
   * @return a publisher that indicates completion or error.
   */
  default Mono<Void> writeWith(File file, long position, long count) {
    return writeWith(file.toPath(), position, count);
  }

  /**
   * Use the given {@link Path} to write the body of the message to the underlying
   * HTTP layer.
   *
   * @param file the file to transfer
   * @param position the position within the file from which the transfer is to begin
   * @param count the number of bytes to be transferred
   * @return a publisher that indicates completion or error.
   */
  Mono<Void> writeWith(Path file, long position, long count);

}
