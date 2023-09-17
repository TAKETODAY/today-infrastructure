/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import cn.taketoday.context.testfixture.origin.MockOrigin;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.origin.TextResourceOrigin.Location;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TextResourceOrigin}.
 *
 * @author Phillip Webb
 */
class TextResourceOriginTests {

  @Test
  void createWithNullResourceSetsNullResource() {
    TextResourceOrigin origin = new TextResourceOrigin(null, null);
    assertThat(origin.getResource()).isNull();
  }

  @Test
  void createWithNullLocationSetsNullLocation() {
    TextResourceOrigin origin = new TextResourceOrigin(null, null);
    assertThat(origin.getLocation()).isNull();
  }

  @Test
  void getResourceReturnResource() {
    ClassPathResource resource = new ClassPathResource("foo.txt");
    TextResourceOrigin origin = new TextResourceOrigin(resource, null);
    assertThat(origin.getResource()).isEqualTo(resource);
  }

  @Test
  void getLocationReturnsLocation() {
    Location location = new Location(1, 2);
    TextResourceOrigin origin = new TextResourceOrigin(null, location);
    assertThat(origin.getLocation()).isEqualTo(location);
  }

  @Test
  void getParentWhenResourceIsNotOriginTrackedReturnsNull() {
    ClassPathResource resource = new ClassPathResource("foo.txt");
    TextResourceOrigin origin = new TextResourceOrigin(resource, null);
    assertThat(origin.getParent()).isNull();
  }

  @Test
  void getParentWhenResourceIsOriginTrackedReturnsResourceOrigin() {
    Origin resourceOrigin = MockOrigin.of("test");
    Resource resource = OriginTrackedResource.from(new ClassPathResource("foo.txt"), resourceOrigin);
    TextResourceOrigin origin = new TextResourceOrigin(resource, null);
    assertThat(origin.getParent()).isSameAs(resourceOrigin);
  }

  @Test
  void getLocationLineReturnsLine() {
    Location location = new Location(1, 2);
    assertThat(location.getLine()).isEqualTo(1);
  }

  @Test
  void getLocationColumnReturnsColumn() {
    Location location = new Location(1, 2);
    assertThat(location.getColumn()).isEqualTo(2);
  }

  @Test
  void locationToStringReturnsNiceString() {
    Location location = new Location(1, 2);
    assertThat(location.toString()).isEqualTo("2:3");
  }

  @Test
  void toStringReturnsNiceString() {
    ClassPathResource resource = new ClassPathResource("foo.txt");
    Location location = new Location(1, 2);
    TextResourceOrigin origin = new TextResourceOrigin(resource, location);
    assertThat(origin.toString()).isEqualTo("class path resource [foo.txt] - 2:3");
  }

  @Test
  void toStringWhenResourceIsNullReturnsNiceString() {
    Location location = new Location(1, 2);
    TextResourceOrigin origin = new TextResourceOrigin(null, location);
    assertThat(origin.toString()).isEqualTo("unknown resource [?] - 2:3");
  }

  @Test
  void toStringWhenLocationIsNullReturnsNiceString() {
    ClassPathResource resource = new ClassPathResource("foo.txt");
    TextResourceOrigin origin = new TextResourceOrigin(resource, null);
    assertThat(origin.toString()).isEqualTo("class path resource [foo.txt]");
  }

  @Test
  void toStringWhenResourceIsClasspathResourceReturnsToStringWithJar() {
    ClassPathResource resource = new ClassPathResource("foo.txt") {

      @Override
      public URI getURI() throws IOException {
        try {
          return new URI("jar:file:/home/user/project/target/project-0.0.1-SNAPSHOT.jar"
                  + "!/APP-INF/classes!/foo.txt");
        }
        catch (URISyntaxException ex) {
          throw new IllegalStateException(ex);
        }
      }

    };
    Location location = new Location(1, 2);
    TextResourceOrigin origin = new TextResourceOrigin(resource, location);
    assertThat(origin.toString()).isEqualTo("class path resource [foo.txt] from project-0.0.1-SNAPSHOT.jar - 2:3");
  }

  @Test
  void locationEqualsAndHashCodeUsesLineAndColumn() {
    Location location1 = new Location(1, 2);
    Location location2 = new Location(1, 2);
    Location location3 = new Location(2, 2);
    assertThat(location1.hashCode()).isEqualTo(location1.hashCode());
    assertThat(location1.hashCode()).isEqualTo(location2.hashCode());
    assertThat(location1.hashCode()).isNotEqualTo(location3.hashCode());
    assertThat(location1).isEqualTo(location1);
    assertThat(location1).isEqualTo(location2);
    assertThat(location1).isNotEqualTo(location3);
  }

  @Test
  void equalsAndHashCodeUsesResourceAndLocation() {
    TextResourceOrigin origin1 = new TextResourceOrigin(new ClassPathResource("foo.txt"), new Location(1, 2));
    TextResourceOrigin origin2 = new TextResourceOrigin(new ClassPathResource("foo.txt"), new Location(1, 2));
    TextResourceOrigin origin3 = new TextResourceOrigin(new ClassPathResource("foo.txt"), new Location(2, 2));
    TextResourceOrigin origin4 = new TextResourceOrigin(new ClassPathResource("foo2.txt"), new Location(1, 2));
    assertThat(origin1.hashCode()).isEqualTo(origin1.hashCode());
    assertThat(origin1.hashCode()).isEqualTo(origin2.hashCode());
    assertThat(origin1.hashCode()).isNotEqualTo(origin3.hashCode());
    assertThat(origin1.hashCode()).isNotEqualTo(origin4.hashCode());
    assertThat(origin1).isEqualTo(origin1);
    assertThat(origin1).isEqualTo(origin2);
    assertThat(origin1).isNotEqualTo(origin3);
    assertThat(origin1).isNotEqualTo(origin4);
  }

}
