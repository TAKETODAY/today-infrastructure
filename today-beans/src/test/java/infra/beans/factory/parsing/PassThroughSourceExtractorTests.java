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

package infra.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PassThroughSourceExtractor}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class PassThroughSourceExtractorTests {

  @Test
  public void testPassThroughContract() throws Exception {
    Object source = new Object();
    Object extractedSource = new PassThroughSourceExtractor().extractSource(source, null);
    assertThat(extractedSource).as("The contract of PassThroughSourceExtractor states that the supplied " +
            "source object *must* be returned as-is").isSameAs(source);
  }

  @Test
  public void testPassThroughContractEvenWithNull() throws Exception {
    Object extractedSource = new PassThroughSourceExtractor().extractSource(null, null);
    assertThat(extractedSource).as("The contract of PassThroughSourceExtractor states that the supplied " +
            "source object *must* be returned as-is (even if null)").isNull();
  }

}
