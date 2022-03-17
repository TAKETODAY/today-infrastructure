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

package cn.taketoday.http.codec.multipart;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.HttpHeaders;
import reactor.core.publisher.Flux;

/**
 * Representation for a part in a "multipart/form-data" request.
 *
 * <p>The origin of a multipart request may be a browser form in which case each
 * part is either a {@link FormFieldPart} or a {@link FilePart}.
 *
 * <p>Multipart requests may also be used outside of a browser for data of any
 * content type (e.g. JSON, PDF, etc).
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578 (multipart/form-data)</a>
 * @see <a href="https://tools.ietf.org/html/rfc2183">RFC 2183 (Content-Disposition)</a>
 * @see <a href="https://www.w3.org/TR/html5/forms.html#multipart-form-data">HTML5 (multipart forms)</a>
 * @since 4.0
 */
public interface Part {

  /**
   * Return the name of the part in the multipart form.
   *
   * @return the name of the part, never {@code null} or empty
   */
  String name();

  /**
   * Return the headers associated with the part.
   */
  HttpHeaders headers();

  /**
   * Return the content for this part.
   * <p>Note that for a {@link FormFieldPart} the content may be accessed
   * more easily via {@link FormFieldPart#value()}.
   */
  Flux<DataBuffer> content();

}
