/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.origin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.context.testfixture.origin.MockOrigin;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.WritableResource;
import cn.taketoday.origin.OriginTrackedResource.OriginTrackedWritableResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OriginTrackedResource}.
 *
 * @author Phillip Webb
 */
class OriginTrackedResourceTests {

  private Origin origin;

  private WritableResource resource;

  private OriginTrackedWritableResource tracked;

  @BeforeEach
  void setup() {
    this.origin = MockOrigin.of("test");
    this.resource = mock(WritableResource.class);
    this.tracked = OriginTrackedResource.from(this.resource, this.origin);
  }

  @Test
  void getInputStreamDelegatesToResource() throws IOException {
    this.tracked.getInputStream();
    then(this.resource).should().getInputStream();
  }

  @Test
  void existsDelegatesToResource() {
    this.tracked.exists();
    then(this.resource).should().exists();
  }

  @Test
  void isReadableDelegatesToResource() {
    this.tracked.isReadable();
    then(this.resource).should().isReadable();
  }

  @Test
  void isOpenDelegatesToResource() {
    this.tracked.isOpen();
    then(this.resource).should().isOpen();
  }

  @Test
  void isFileDelegatesToResource() {
    this.tracked.isFile();
    then(this.resource).should().isFile();
  }

  @Test
  void getURLDelegatesToResource() throws IOException {
    this.tracked.getURL();
    then(this.resource).should().getURL();
  }

  @Test
  void getURIDelegatesToResource() throws IOException {
    this.tracked.getURI();
    then(this.resource).should().getURI();
  }

  @Test
  void getFileDelegatesToResource() throws IOException {
    this.tracked.getFile();
    then(this.resource).should().getFile();
  }

  @Test
  void readableChannelDelegatesToResource() throws IOException {
    this.tracked.readableChannel();
    then(this.resource).should().readableChannel();
  }

  @Test
  void contentLengthDelegatesToResource() throws IOException {
    this.tracked.contentLength();
    then(this.resource).should().contentLength();
  }

  @Test
  void lastModifiedDelegatesToResource() throws IOException {
    this.tracked.lastModified();
    then(this.resource).should().lastModified();
  }

  @Test
  void createRelativeDelegatesToResource() throws IOException {
    this.tracked.createRelative("path");
    then(this.resource).should().createRelative("path");
  }

  @Test
  void getFilenameDelegatesToResource() {
    this.tracked.getName();
    then(this.resource).should().getName();
  }

  @Test
  void getOutputStreamDelegatesToResource() throws IOException {
    this.tracked.getOutputStream();
    then(this.resource).should().getOutputStream();
  }

  @Test
  void toStringDelegatesToResource() {
    Resource resource = new ClassPathResource("test");
    Resource tracked = OriginTrackedResource.from(resource, this.origin);
    assertThat(tracked).hasToString(resource.toString());
  }

  @Test
  void getOriginReturnsOrigin() {
    assertThat(this.tracked.getOrigin()).isEqualTo(this.origin);
  }

  @Test
  void getResourceReturnsResource() {
    assertThat(this.tracked.getDelegate()).isEqualTo(this.resource);
  }

  @Test
  void equalsAndHashCode() {
    Origin o1 = MockOrigin.of("o1");
    Origin o2 = MockOrigin.of("o2");
    Resource r1 = mock(Resource.class);
    Resource r2 = mock(Resource.class);
    OriginTrackedResource r1o1a = OriginTrackedResource.from(r1, o1);
    OriginTrackedResource r1o1b = OriginTrackedResource.from(r1, o1);
    OriginTrackedResource r1o2 = OriginTrackedResource.from(r1, o2);
    OriginTrackedResource r2o1 = OriginTrackedResource.from(r2, o1);
    OriginTrackedResource r2o2 = OriginTrackedResource.from(r2, o2);
    assertThat(r1o1a).isEqualTo(r1o1a).isEqualTo(r1o1a).isNotEqualTo(r1o2).isNotEqualTo(r2o1).isNotEqualTo(r2o2);
    assertThat(r1o1a.hashCode()).isEqualTo(r1o1b.hashCode());
  }

  @Test
  void ofReturnsOriginTrackedResource() {
    Resource resource = mock(Resource.class);
    Resource tracked = OriginTrackedResource.from(resource, this.origin);
    assertThat(tracked).isExactlyInstanceOf(OriginTrackedResource.class);
  }

  @Test
  void ofWhenWritableReturnsOriginTrackedWritableResource() {
    Resource resource = mock(WritableResource.class);
    Resource tracked = OriginTrackedResource.from(resource, this.origin);
    assertThat(tracked).isInstanceOf(WritableResource.class)
            .isExactlyInstanceOf(OriginTrackedWritableResource.class);
  }

}
