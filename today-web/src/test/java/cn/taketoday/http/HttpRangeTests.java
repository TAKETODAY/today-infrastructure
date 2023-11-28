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

package cn.taketoday.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.InputStreamResource;
import cn.taketoday.core.io.ResourceRegion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author TODAY 2021/11/6 23:49
 */
class HttpRangeTests {

  @Test
  public void invalidFirstPosition() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpRange.createByteRange(-1));
  }

  @Test
  public void invalidLastLessThanFirst() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpRange.createByteRange(10, 9));
  }

  @Test
  public void invalidSuffixLength() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpRange.createSuffixRange(-1));
  }

  @Test
  public void byteRange() {
    HttpRange range = HttpRange.createByteRange(0, 499);
    assertThat(range.getRangeStart(1000)).isEqualTo(0);
    assertThat(range.getRangeEnd(1000)).isEqualTo(499);
  }

  @Test
  public void byteRangeWithoutLastPosition() {
    HttpRange range = HttpRange.createByteRange(9500);
    assertThat(range.getRangeStart(10000)).isEqualTo(9500);
    assertThat(range.getRangeEnd(10000)).isEqualTo(9999);
  }

  @Test
  public void byteRangeOfZeroLength() {
    HttpRange range = HttpRange.createByteRange(9500, 9500);
    assertThat(range.getRangeStart(10000)).isEqualTo(9500);
    assertThat(range.getRangeEnd(10000)).isEqualTo(9500);
  }

  @Test
  public void suffixRange() {
    HttpRange range = HttpRange.createSuffixRange(500);
    assertThat(range.getRangeStart(1000)).isEqualTo(500);
    assertThat(range.getRangeEnd(1000)).isEqualTo(999);
  }

  @Test
  public void suffixRangeShorterThanRepresentation() {
    HttpRange range = HttpRange.createSuffixRange(500);
    assertThat(range.getRangeStart(350)).isEqualTo(0);
    assertThat(range.getRangeEnd(350)).isEqualTo(349);
  }

  @Test
  public void parseRanges() {
    List<HttpRange> ranges = HttpRange.parseRanges("bytes=0-0,500-,-1");
    assertThat(ranges.size()).isEqualTo(3);
    assertThat(ranges.get(0).getRangeStart(1000)).isEqualTo(0);
    assertThat(ranges.get(0).getRangeEnd(1000)).isEqualTo(0);
    assertThat(ranges.get(1).getRangeStart(1000)).isEqualTo(500);
    assertThat(ranges.get(1).getRangeEnd(1000)).isEqualTo(999);
    assertThat(ranges.get(2).getRangeStart(1000)).isEqualTo(999);
    assertThat(ranges.get(2).getRangeEnd(1000)).isEqualTo(999);
  }

  @Test
  public void parseRangesValidations() {

    // 1. At limit..
    StringBuilder atLimit = new StringBuilder("bytes=0-0");
    for (int i = 0; i < 99; i++) {
      atLimit.append(',').append(i).append('-').append(i + 1);
    }
    List<HttpRange> ranges = HttpRange.parseRanges(atLimit.toString());
    assertThat(ranges.size()).isEqualTo(100);

    // 2. Above limit..
    StringBuilder aboveLimit = new StringBuilder("bytes=0-0");
    for (int i = 0; i < 100; i++) {
      aboveLimit.append(',').append(i).append('-').append(i + 1);
    }
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpRange.parseRanges(aboveLimit.toString()));
  }

  @Test
  public void rangeToString() {
    List<HttpRange> ranges = new ArrayList<>();
    ranges.add(HttpRange.createByteRange(0, 499));
    ranges.add(HttpRange.createByteRange(9500));
    ranges.add(HttpRange.createSuffixRange(500));
    assertThat(HttpRange.toString(ranges)).as("Invalid Range header").isEqualTo("bytes=0-499, 9500-, -500");
  }

  @Test
  public void toResourceRegion() {
    byte[] bytes = "Spring Framework".getBytes(StandardCharsets.UTF_8);
    ByteArrayResource resource = new ByteArrayResource(bytes);
    HttpRange range = HttpRange.createByteRange(0, 5);
    ResourceRegion region = range.toResourceRegion(resource);
    assertThat(region.getResource()).isEqualTo(resource);
    assertThat(region.getPosition()).isEqualTo(0L);
    assertThat(region.getCount()).isEqualTo(6L);
  }

  @Test
  public void toResourceRegionInputStreamResource() {
    InputStreamResource resource = mock(InputStreamResource.class);
    HttpRange range = HttpRange.createByteRange(0, 9);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> range.toResourceRegion(resource));
  }

  @Test
  public void toResourceRegionIllegalLength() {
    ByteArrayResource resource = mock(ByteArrayResource.class);
    given(resource.contentLength()).willReturn(-1L);
    HttpRange range = HttpRange.createByteRange(0, 9);
    assertThatIllegalArgumentException().isThrownBy(() -> range.toResourceRegion(resource));
  }

  @Test
  public void toResourceRegionExceptionLength() throws IOException {
    InputStreamResource resource = mock(InputStreamResource.class);
    given(resource.contentLength()).willThrow(IOException.class);
    HttpRange range = HttpRange.createByteRange(0, 9);
    assertThatIllegalArgumentException().isThrownBy(() -> range.toResourceRegion(resource));
  }

  @Test // gh-23576
  public void toResourceRegionStartingAtResourceByteCount() {
    byte[] bytes = "Spring Framework".getBytes(StandardCharsets.UTF_8);
    ByteArrayResource resource = new ByteArrayResource(bytes);
    HttpRange range = HttpRange.createByteRange(resource.contentLength());
    assertThatIllegalArgumentException().isThrownBy(() -> range.toResourceRegion(resource));
  }

  @Test
  public void toResourceRegionsValidations() {
    byte[] bytes = "12345".getBytes(StandardCharsets.UTF_8);
    ByteArrayResource resource = new ByteArrayResource(bytes);

    // 1. Below length
    List<HttpRange> belowLengthRanges = HttpRange.parseRanges("bytes=0-1,2-3");
    List<ResourceRegion> regions = HttpRange.toResourceRegions(belowLengthRanges, resource);
    assertThat(regions.size()).isEqualTo(2);

    // 2. At length
    List<HttpRange> atLengthRanges = HttpRange.parseRanges("bytes=0-1,2-4");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpRange.toResourceRegions(atLengthRanges, resource));
  }

}
