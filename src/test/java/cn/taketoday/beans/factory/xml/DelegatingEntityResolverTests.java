/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link DelegatingEntityResolver} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class DelegatingEntityResolverTests {

  @Test
  public void testCtorWhereDtdEntityResolverIsNull() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new DelegatingEntityResolver(null, new NoOpEntityResolver()));
  }

  @Test
  public void testCtorWhereSchemaEntityResolverIsNull() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new DelegatingEntityResolver(new NoOpEntityResolver(), null));
  }

  @Test
  public void testCtorWhereEntityResolversAreBothNull() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new DelegatingEntityResolver(null, null));
  }

  private static final class NoOpEntityResolver implements EntityResolver {
    @Override
    public InputSource resolveEntity(String publicId, String systemId) {
      return null;
    }
  }

}
