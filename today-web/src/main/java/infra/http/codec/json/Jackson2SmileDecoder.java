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

package infra.http.codec.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import infra.http.converter.json.Jackson2ObjectMapperBuilder;
import infra.lang.Assert;
import infra.util.MimeType;

/**
 * Decode a byte stream into Smile and convert to Object's with Jackson 2.9,
 * leveraging non-blocking parsing.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Jackson2JsonEncoder
 * @since 4.0
 */
public class Jackson2SmileDecoder extends AbstractJackson2Decoder {

  public Jackson2SmileDecoder() {
    this(Jackson2ObjectMapperBuilder.smile().build(), Jackson2SmileEncoder.DEFAULT_SMILE_MIME_TYPES);
  }

  public Jackson2SmileDecoder(ObjectMapper mapper, MimeType... mimeTypes) {
    super(mapper, mimeTypes);
    Assert.isAssignable(SmileFactory.class, mapper.getFactory().getClass());
  }

}
