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

package cn.taketoday.beans;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 22:00
 */
class PropertyMatchesTests {

  @Test
  public void simpleBeanPropertyTypo() {
    PropertyMatches matches = PropertyMatches.forProperty("naem", SampleBeanProperties.class);
    assertThat(matches.getPossibleMatches()).contains("name");
  }

  @Test
  public void complexBeanPropertyTypo() {
    PropertyMatches matches = PropertyMatches.forProperty("desriptn", SampleBeanProperties.class);
    assertThat(matches.getPossibleMatches()).isEmpty();
  }

  @Test
  public void unknownBeanProperty() {
    PropertyMatches matches = PropertyMatches.forProperty("unknown", SampleBeanProperties.class);
    assertThat(matches.getPossibleMatches()).isEmpty();
  }

  @Test
  public void severalMatchesBeanProperty() {
    PropertyMatches matches = PropertyMatches.forProperty("counter", SampleBeanProperties.class);
    assertThat(matches.getPossibleMatches()).contains("counter1");
    assertThat(matches.getPossibleMatches()).contains("counter2");
    assertThat(matches.getPossibleMatches()).contains("counter3");
  }

  @Test
  public void simpleBeanPropertyErrorMessage() {
    PropertyMatches matches = PropertyMatches.forProperty("naem", SampleBeanProperties.class);
    String msg = matches.buildErrorMessage();
    assertThat(msg).contains("naem");
    assertThat(msg).contains("name");
    assertThat(msg).contains("setter");
    assertThat(msg).doesNotContain("field");
  }

  @Test
  public void complexBeanPropertyErrorMessage() {
    PropertyMatches matches = PropertyMatches.forProperty("counter", SampleBeanProperties.class);
    String msg = matches.buildErrorMessage();
    assertThat(msg).contains("counter");
    assertThat(msg).contains("counter1");
    assertThat(msg).contains("counter2");
    assertThat(msg).contains("counter3");
  }

  @Test
  public void simpleFieldPropertyTypo() {
    PropertyMatches matches = PropertyMatches.forField("naem", SampleFieldProperties.class);
    assertThat(matches.getPossibleMatches()).contains("name");
  }

  @Test
  public void complexFieldPropertyTypo() {
    PropertyMatches matches = PropertyMatches.forField("desriptn", SampleFieldProperties.class);
    assertThat(matches.getPossibleMatches()).isEmpty();
  }

  @Test
  public void unknownFieldProperty() {
    PropertyMatches matches = PropertyMatches.forField("unknown", SampleFieldProperties.class);
    assertThat(matches.getPossibleMatches()).isEmpty();
  }

  @Test
  public void severalMatchesFieldProperty() {
    PropertyMatches matches = PropertyMatches.forField("counter", SampleFieldProperties.class);
    assertThat(matches.getPossibleMatches()).contains("counter1");
    assertThat(matches.getPossibleMatches()).contains("counter2");
    assertThat(matches.getPossibleMatches()).contains("counter3");
  }

  @Test
  public void simpleFieldPropertyErrorMessage() {
    PropertyMatches matches = PropertyMatches.forField("naem", SampleFieldProperties.class);
    String msg = matches.buildErrorMessage();
    assertThat(msg).contains("naem");
    assertThat(msg).contains("name");
    assertThat(msg).contains("field");
    assertThat(msg).doesNotContain("setter");
  }

  @Test
  public void complexFieldPropertyErrorMessage() {
    PropertyMatches matches = PropertyMatches.forField("counter", SampleFieldProperties.class);
    String msg = matches.buildErrorMessage();
    assertThat(msg).contains("counter");
    assertThat(msg).contains("counter1");
    assertThat(msg).contains("counter2");
    assertThat(msg).contains("counter3");
  }

  @SuppressWarnings("unused")
  private static class SampleBeanProperties {

    private String name;

    private String description;

    private int counter1;

    private int counter2;

    private int counter3;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public int getCounter1() {
      return counter1;
    }

    public void setCounter1(int counter1) {
      this.counter1 = counter1;
    }

    public int getCounter2() {
      return counter2;
    }

    public void setCounter2(int counter2) {
      this.counter2 = counter2;
    }

    public int getCounter3() {
      return counter3;
    }

    public void setCounter3(int counter3) {
      this.counter3 = counter3;
    }
  }

  @SuppressWarnings("unused")
  private static class SampleFieldProperties {

    private String name;

    private String description;

    private int counter1;

    private int counter2;

    private int counter3;

  }

}
