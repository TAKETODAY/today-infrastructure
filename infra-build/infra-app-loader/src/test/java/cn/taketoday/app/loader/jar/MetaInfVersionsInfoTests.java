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

package cn.taketoday.app.loader.jar;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.app.loader.zip.ZipContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetaInfVersionsInfo}.
 *
 * @author Phillip Webb
 */
class MetaInfVersionsInfoTests {

  @Test
  void getParsesVersionsAndEntries() {
    List<ZipContent.Entry> entries = new ArrayList<>();
    entries.add(mockEntry("META-INF/"));
    entries.add(mockEntry("META-INF/MANIFEST.MF"));
    entries.add(mockEntry("META-INF/versions/"));
    entries.add(mockEntry("META-INF/versions/9/"));
    entries.add(mockEntry("META-INF/versions/9/Foo.class"));
    entries.add(mockEntry("META-INF/versions/11/"));
    entries.add(mockEntry("META-INF/versions/11/Foo.class"));
    entries.add(mockEntry("META-INF/versions/10/"));
    entries.add(mockEntry("META-INF/versions/10/Foo.class"));
    MetaInfVersionsInfo info = MetaInfVersionsInfo.get(entries.size(), entries::get);
    assertThat(info.versions()).containsExactly(9, 10, 11);
    assertThat(info.directories()).containsExactly("META-INF/versions/9/", "META-INF/versions/10/",
            "META-INF/versions/11/");
  }

  @Test
  void getWhenHasBadEntryParsesGoodVersionsAndEntries() {
    List<ZipContent.Entry> entries = new ArrayList<>();
    entries.add(mockEntry("META-INF/versions/9/Foo.class"));
    entries.add(mockEntry("META-INF/versions/0x11/Foo.class"));
    MetaInfVersionsInfo info = MetaInfVersionsInfo.get(entries.size(), entries::get);
    assertThat(info.versions()).containsExactly(9);
    assertThat(info.directories()).containsExactly("META-INF/versions/9/");
  }

  @Test
  void getWhenHasNoEntriesReturnsNone() {
    List<ZipContent.Entry> entries = new ArrayList<>();
    MetaInfVersionsInfo info = MetaInfVersionsInfo.get(entries.size(), entries::get);
    assertThat(info.versions()).isEmpty();
    assertThat(info.directories()).isEmpty();
    assertThat(info).isSameAs(MetaInfVersionsInfo.NONE);
  }

  private ZipContent.Entry mockEntry(String name) {
    ZipContent.Entry entry = mock(ZipContent.Entry.class);
    given(entry.getName()).willReturn(name);
    given(entry.hasNameStartingWith(any()))
            .willAnswer((invocation) -> name.startsWith(invocation.getArgument(0, CharSequence.class).toString()));
    given(entry.isDirectory()).willAnswer((invocation) -> name.endsWith("/"));
    return entry;
  }

}
