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

import infra.beans.factory.parsing.NullSourceExtractor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public class NullSourceExtractorTests {

  @Test
  public void testPassThroughContract() throws Exception {
    Object source = new Object();
    Object extractedSource = new NullSourceExtractor().extractSource(source, null);
    assertThat(extractedSource).as("The contract of NullSourceExtractor states that the extraction *always* return null").isNull();
  }

  @Test
  public void testPassThroughContractEvenWithNull() throws Exception {
    Object extractedSource = new NullSourceExtractor().extractSource(null, null);
    assertThat(extractedSource).as("The contract of NullSourceExtractor states that the extraction *always* return null").isNull();
  }

}
