package cn.taketoday.beans.factory.support;

public class Colour {

  public static final Colour RED = new Colour("RED");
  public static final Colour BLUE = new Colour("BLUE");
  public static final Colour GREEN = new Colour("GREEN");
  public static final Colour PURPLE = new Colour("PURPLE");

  private final String name;

  public Colour(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name;
  }

}