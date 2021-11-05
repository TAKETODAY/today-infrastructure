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

package cn.taketoday.web.http;

import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;

/**
 * Sub-interface of {@code ReactiveOutputMessage} that has support for "zero-copy"
 * file transfers.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 4.0
 * @see <a href="https://en.wikipedia.org/wiki/Zero-copy">Zero-copy</a>
 */
public interface ZeroCopyHttpOutputMessage extends ReactiveHttpOutputMessage {

	/**
	 * Use the given {@link File} to write the body of the message to the underlying
	 * HTTP layer.
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
	 * @param file the file to transfer
	 * @param position the position within the file from which the transfer is to begin
	 * @param count the number of bytes to be transferred
	 * @return a publisher that indicates completion or error.
	 * @since 4.0
	 */
	Mono<Void> writeWith(Path file, long position, long count);

}
