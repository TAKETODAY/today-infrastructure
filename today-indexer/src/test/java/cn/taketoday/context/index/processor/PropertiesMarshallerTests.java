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

package cn.taketoday.context.index.processor;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesMarshaller}.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
public class PropertiesMarshallerTests {

  @Test
  public void readWrite() throws IOException {
    CandidateComponentsMetadata metadata = new CandidateComponentsMetadata();
    metadata.add(createItem("com.foo", "first", "second"));
    metadata.add(createItem("com.bar", "first"));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PropertiesMarshaller.write(metadata, outputStream);
    CandidateComponentsMetadata readMetadata = PropertiesMarshaller.read(
            new ByteArrayInputStream(outputStream.toByteArray()));
    assertThat(readMetadata).has(Metadata.of("com.foo", "first", "second"));
    assertThat(readMetadata).has(Metadata.of("com.bar", "first"));
    assertThat(readMetadata.getItems()).hasSize(2);
  }

  @Test
  public void metadataIsWrittenDeterministically() throws IOException {
    CandidateComponentsMetadata metadata = new CandidateComponentsMetadata();
    metadata.add(createItem("com.b", "type"));
    metadata.add(createItem("com.c", "type"));
    metadata.add(createItem("com.a", "type"));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PropertiesMarshaller.write(metadata, outputStream);
    String contents = new String(outputStream.toByteArray(), StandardCharsets.ISO_8859_1);
    assertThat(contents.split(System.lineSeparator())).containsExactly("com.a=type", "com.b=type", "com.c=type");
  }

  private static ItemMetadata createItem(String type, String... stereotypes) {
    return new ItemMetadata(type, new HashSet<>(Arrays.asList(stereotypes)));
  }

}
