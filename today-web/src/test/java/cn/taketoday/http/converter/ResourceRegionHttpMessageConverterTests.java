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

package cn.taketoday.http.converter;

import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.TypeReference;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceRegion;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRange;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.MockHttpOutputMessage;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test cases for {@link ResourceRegionHttpMessageConverter} class.
 *
 * @author Brian Clozel
 */
public class ResourceRegionHttpMessageConverterTests {

  private final ResourceRegionHttpMessageConverter converter = new ResourceRegionHttpMessageConverter();

  @Test
  public void canReadResource() {
    assertThat(converter.canRead(Resource.class, MediaType.APPLICATION_OCTET_STREAM)).isFalse();
    assertThat(converter.canRead(Resource.class, MediaType.ALL)).isFalse();
    assertThat(converter.canRead(List.class, MediaType.APPLICATION_OCTET_STREAM)).isFalse();
    assertThat(converter.canRead(List.class, MediaType.ALL)).isFalse();
  }

  @Test
  public void canWriteResource() {
    assertThat(converter.canWrite(ResourceRegion.class, null, MediaType.APPLICATION_OCTET_STREAM)).isTrue();
    assertThat(converter.canWrite(ResourceRegion.class, null, MediaType.ALL)).isTrue();
    assertThat(converter.canWrite(Object.class, null, MediaType.ALL)).isFalse();
  }

  @Test
  public void canWriteResourceCollection() {
    Type resourceRegionList = new TypeReference<List<ResourceRegion>>() { }.getType();
    assertThat(converter.canWrite(resourceRegionList, null, MediaType.APPLICATION_OCTET_STREAM)).isTrue();
    assertThat(converter.canWrite(resourceRegionList, null, MediaType.ALL)).isTrue();

    assertThat(converter.canWrite(List.class, MediaType.APPLICATION_OCTET_STREAM)).isFalse();
    assertThat(converter.canWrite(List.class, MediaType.ALL)).isFalse();
    Type resourceObjectList = new TypeReference<List<Object>>() { }.getType();
    assertThat(converter.canWrite(resourceObjectList, null, MediaType.ALL)).isFalse();
  }

  @Test
  public void shouldWritePartialContentByteRange() throws Exception {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    Resource body = new ClassPathResource("byterangeresource.txt", getClass());
    ResourceRegion region = HttpRange.createByteRange(0, 5).toResourceRegion(body);
    converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

    HttpHeaders headers = outputMessage.getHeaders();
    assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(headers.getContentLength()).isEqualTo(6L);
    assertThat(headers.get(HttpHeaders.CONTENT_RANGE)).hasSize(1);
    assertThat(headers.get(HttpHeaders.CONTENT_RANGE).get(0)).isEqualTo("bytes 0-5/39");
    assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo("Spring");
  }

  @Test
  public void shouldWritePartialContentByteRangeNoEnd() throws Exception {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    Resource body = new ClassPathResource("byterangeresource.txt", getClass());
    ResourceRegion region = HttpRange.createByteRange(7).toResourceRegion(body);
    converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

    HttpHeaders headers = outputMessage.getHeaders();
    assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(headers.getContentLength()).isEqualTo(32L);
    assertThat(headers.get(HttpHeaders.CONTENT_RANGE)).hasSize(1);
    assertThat(headers.get(HttpHeaders.CONTENT_RANGE).get(0)).isEqualTo("bytes 7-38/39");
    assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo("Framework test resource content.");
  }

  @Test
  public void partialContentMultipleByteRanges() throws Exception {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    Resource body = new ClassPathResource("byterangeresource.txt", getClass());
    List<HttpRange> rangeList = HttpRange.parseRanges("bytes=0-5,7-15,17-20,22-38");
    List<ResourceRegion> regions = new ArrayList<>();
    for (HttpRange range : rangeList) {
      regions.add(range.toResourceRegion(body));
    }

    converter.write(regions, MediaType.TEXT_PLAIN, outputMessage);

    HttpHeaders headers = outputMessage.getHeaders();
    assertThat(headers.getContentType().toString()).startsWith("multipart/byteranges;boundary=");
    String boundary = "--" + headers.getContentType().toString().substring(30);
    String content = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
    String[] ranges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);

    assertThat(ranges[0]).isEqualTo(boundary);
    assertThat(ranges[1]).isEqualTo("Content-Type: text/plain");
    assertThat(ranges[2]).isEqualTo("Content-Range: bytes 0-5/39");
    assertThat(ranges[3]).isEqualTo("Spring");

    assertThat(ranges[4]).isEqualTo(boundary);
    assertThat(ranges[5]).isEqualTo("Content-Type: text/plain");
    assertThat(ranges[6]).isEqualTo("Content-Range: bytes 7-15/39");
    assertThat(ranges[7]).isEqualTo("Framework");

    assertThat(ranges[8]).isEqualTo(boundary);
    assertThat(ranges[9]).isEqualTo("Content-Type: text/plain");
    assertThat(ranges[10]).isEqualTo("Content-Range: bytes 17-20/39");
    assertThat(ranges[11]).isEqualTo("test");

    assertThat(ranges[12]).isEqualTo(boundary);
    assertThat(ranges[13]).isEqualTo("Content-Type: text/plain");
    assertThat(ranges[14]).isEqualTo("Content-Range: bytes 22-38/39");
    assertThat(ranges[15]).isEqualTo("resource content.");
  }

  @Test
  public void partialContentMultipleByteRangesInRandomOrderAndOverlapping() throws Exception {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    Resource body = new ClassPathResource("byterangeresource.txt", getClass());
    List<HttpRange> rangeList = HttpRange.parseRanges("bytes=7-15,0-5,17-20,20-29");
    List<ResourceRegion> regions = new ArrayList<>();
    for (HttpRange range : rangeList) {
      regions.add(range.toResourceRegion(body));
    }

    converter.write(regions, MediaType.TEXT_PLAIN, outputMessage);

    HttpHeaders headers = outputMessage.getHeaders();
    assertThat(headers.getContentType().toString()).startsWith("multipart/byteranges;boundary=");
    String boundary = "--" + headers.getContentType().toString().substring(30);
    String content = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
    String[] ranges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);

    assertThat(ranges[0]).isEqualTo(boundary);
    assertThat(ranges[1]).isEqualTo("Content-Type: text/plain");
    assertThat(ranges[2]).isEqualTo("Content-Range: bytes 7-15/39");
    assertThat(ranges[3]).isEqualTo("Framework");

    assertThat(ranges[4]).isEqualTo(boundary);
    assertThat(ranges[5]).isEqualTo("Content-Type: text/plain");
    assertThat(ranges[6]).isEqualTo("Content-Range: bytes 0-5/39");
    assertThat(ranges[7]).isEqualTo("Spring");

    assertThat(ranges[8]).isEqualTo(boundary);
    assertThat(ranges[9]).isEqualTo("Content-Type: text/plain");
    assertThat(ranges[10]).isEqualTo("Content-Range: bytes 17-20/39");
    assertThat(ranges[11]).isEqualTo("test");

    assertThat(ranges[12]).isEqualTo(boundary);
    assertThat(ranges[13]).isEqualTo("Content-Type: text/plain");
    assertThat(ranges[14]).isEqualTo("Content-Range: bytes 20-29/39");
    assertThat(ranges[15]).isEqualTo("t resource");
  }

  @Test // SPR-15041
  public void applicationOctetStreamDefaultContentType() throws Exception {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    ClassPathResource body = mock(ClassPathResource.class);
    given(body.getName()).willReturn("spring.dat");
    given(body.contentLength()).willReturn(12L);
    given(body.getInputStream()).willReturn(new ByteArrayInputStream("Spring Framework".getBytes()));
    HttpRange range = HttpRange.createByteRange(0, 5);
    ResourceRegion resourceRegion = range.toResourceRegion(body);

    converter.write(Collections.singletonList(resourceRegion), null, outputMessage);

    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    assertThat(outputMessage.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 0-5/12");
    assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo("Spring");
  }

}
