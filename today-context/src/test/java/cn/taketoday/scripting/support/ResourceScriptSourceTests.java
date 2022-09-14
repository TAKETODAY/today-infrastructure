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
package cn.taketoday.scripting.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class ResourceScriptSourceTests {

  @Test
  public void doesNotPropagateFatalExceptionOnResourceThatCannotBeResolvedToAFile() throws Exception {
    Resource resource = mock(Resource.class);
    given(resource.lastModified()).willThrow(new IOException());

    ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
    long lastModified = scriptSource.retrieveLastModifiedTime();
    assertThat(lastModified).isEqualTo(0);
  }

  @Test
  public void beginsInModifiedState() throws Exception {
    Resource resource = mock(Resource.class);
    ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
    assertThat(scriptSource.isModified()).isTrue();
  }

  @Test
  public void lastModifiedWorksWithResourceThatDoesNotSupportFileBasedReading() throws Exception {
    Resource resource = mock(Resource.class);
    // underlying File is asked for so that the last modified time can be checked...
    // And then mock the file changing; i.e. the File says it has been modified
    given(resource.lastModified()).willReturn(100L, 100L, 200L);
    // does not support File-based reading; delegates to InputStream-style reading...
    //resource.getFile();
    //mock.setThrowable(new FileNotFoundException());
    given(resource.getInputStream()).willReturn(InputStream.nullInputStream());

    ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
    assertThat(scriptSource.isModified()).as("ResourceScriptSource must start off in the 'isModified' state (it obviously isn't).").isTrue();
    scriptSource.getScriptAsString();
    assertThat(scriptSource.isModified()).as("ResourceScriptSource must not report back as being modified if the underlying File resource is not reporting a changed lastModified time.").isFalse();
    // Must now report back as having been modified
    assertThat(scriptSource.isModified()).as("ResourceScriptSource must report back as being modified if the underlying File resource is reporting a changed lastModified time.").isTrue();
  }

  @Test
  public void lastModifiedWorksWithResourceThatDoesNotSupportFileBasedAccessAtAll() throws Exception {
    Resource resource = new ByteArrayResource(new byte[0]);
    ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
    assertThat(scriptSource.isModified()).as("ResourceScriptSource must start off in the 'isModified' state (it obviously isn't).").isTrue();
    scriptSource.getScriptAsString();
    assertThat(scriptSource.isModified()).as("ResourceScriptSource must not report back as being modified if the underlying File resource is not reporting a changed lastModified time.").isFalse();
    // Must now continue to report back as not having been modified 'cos the Resource does not support access as a File (and so the lastModified date cannot be determined).
    assertThat(scriptSource.isModified()).as("ResourceScriptSource must not report back as being modified if the underlying File resource is not reporting a changed lastModified time.").isFalse();
  }

}
