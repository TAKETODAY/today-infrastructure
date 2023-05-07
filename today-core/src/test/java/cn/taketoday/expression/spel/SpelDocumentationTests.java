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

package cn.taketoday.expression.spel;

import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.ExpressionParser;
import cn.taketoday.expression.ParserContext;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.expression.spel.testresources.Inventor;
import cn.taketoday.expression.spel.testresources.PlaceOfBirth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Test the examples specified in the documentation.
 *
 * <p>NOTE: any outgoing changes from this file upon synchronizing with the repo may indicate that
 * you need to update the documentation too !
 *
 * @author Andy Clement
 */
@SuppressWarnings("rawtypes")
class SpelDocumentationTests extends AbstractExpressionTests {

  static Inventor tesla;

  static Inventor pupin;

  static {
    GregorianCalendar c = new GregorianCalendar();
    c.set(1856, 7, 9);
    tesla = new Inventor("Nikola Tesla", c.getTime(), "Serbian");
    tesla.setPlaceOfBirth(new PlaceOfBirth("SmilJan"));
    tesla.setInventions(new String[] { "Telephone repeater", "Rotating magnetic field principle",
            "Polyphase alternating-current system", "Induction motor", "Alternating-current power transmission",
            "Tesla coil transformer", "Wireless communication", "Radio", "Fluorescent lights" });

    pupin = new Inventor("Pupin", c.getTime(), "Idvor");
    pupin.setPlaceOfBirth(new PlaceOfBirth("Idvor"));

  }

  @Test
  void methodInvocation() {
    evaluate("'Hello World'.concat('!')", "Hello World!", String.class);
  }

  @Test
  void beanPropertyAccess() {
    evaluate("new String('Hello World'.bytes)", "Hello World", String.class);
  }

  @Test
  void arrayLengthAccess() {
    evaluate("'Hello World'.bytes.length", 11, Integer.class);
  }

  @Test
  void rootObject() throws Exception {
    GregorianCalendar c = new GregorianCalendar();
    c.set(1856, 7, 9);

    //  The constructor arguments are name, birthday, and nationaltiy.
    Inventor tesla = new Inventor("Nikola Tesla", c.getTime(), "Serbian");

    ExpressionParser parser = new SpelExpressionParser();
    Expression exp = parser.parseExpression("name");

    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setRootObject(tesla);

    String name = (String) exp.getValue(context);
    assertThat(name).isEqualTo("Nikola Tesla");
  }

  @Test
  void equalityCheck() throws Exception {
    ExpressionParser parser = new SpelExpressionParser();

    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setRootObject(tesla);

    Expression exp = parser.parseExpression("name == 'Nikola Tesla'");
    boolean isEqual = exp.getValue(context, Boolean.class);  // evaluates to true
    assertThat(isEqual).isTrue();
  }

  // Section 7.4.1

  @Test
  void xmlBasedConfig() {
    evaluate("(T(java.lang.Math).random() * 100.0 )>0", true, Boolean.class);
  }

  // Section 7.5
  @Test
  void literals() throws Exception {
    ExpressionParser parser = new SpelExpressionParser();

    String helloWorld = (String) parser.parseExpression("'Hello World'").getValue(); // evals to "Hello World"
    assertThat(helloWorld).isEqualTo("Hello World");

    double avogadrosNumber = (Double) parser.parseExpression("6.0221415E+23").getValue();
    assertThat(avogadrosNumber).isCloseTo(6.0221415E+23, within((double) 0));

    int maxValue = (Integer) parser.parseExpression("0x7FFFFFFF").getValue();  // evals to 2147483647
    assertThat(maxValue).isEqualTo(Integer.MAX_VALUE);

    boolean trueValue = (Boolean) parser.parseExpression("true").getValue();
    assertThat(trueValue).isTrue();

    Object nullValue = parser.parseExpression("null").getValue();
    assertThat(nullValue).isNull();
  }

  @Test
  void propertyAccess() throws Exception {
    EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
    int year = (Integer) parser.parseExpression("Birthdate.Year + 1900").getValue(context); // 1856
    assertThat(year).isEqualTo(1856);

    String city = (String) parser.parseExpression("placeOfBirth.City").getValue(context);
    assertThat(city).isEqualTo("SmilJan");
  }

  @Test
  void propertyNavigation() throws Exception {
    ExpressionParser parser = new SpelExpressionParser();

    // Inventions Array
    StandardEvaluationContext teslaContext = TestScenarioCreator.getTestEvaluationContext();
    // teslaContext.setRootObject(tesla);

    // evaluates to "Induction motor"
    String invention = parser.parseExpression("inventions[3]").getValue(teslaContext, String.class);
    assertThat(invention).isEqualTo("Induction motor");

    // Members List
    StandardEvaluationContext societyContext = new StandardEvaluationContext();
    IEEE ieee = new IEEE();
    ieee.Members[0] = tesla;
    societyContext.setRootObject(ieee);

    // evaluates to "Nikola Tesla"
    String name = parser.parseExpression("Members[0].Name").getValue(societyContext, String.class);
    assertThat(name).isEqualTo("Nikola Tesla");

    // List and Array navigation
    // evaluates to "Wireless communication"
    invention = parser.parseExpression("Members[0].Inventions[6]").getValue(societyContext, String.class);
    assertThat(invention).isEqualTo("Wireless communication");
  }

  @Test
  void dictionaryAccess() throws Exception {
    StandardEvaluationContext societyContext = new StandardEvaluationContext();
    societyContext.setRootObject(new IEEE());
    // Officer's Dictionary
    Inventor pupin = parser.parseExpression("officers['president']").getValue(societyContext, Inventor.class);
    assertThat(pupin).isNotNull();

    // evaluates to "Idvor"
    String city = parser.parseExpression("officers['president'].PlaceOfBirth.city").getValue(societyContext, String.class);
    assertThat(city).isNotNull();

    // setting values
    Inventor i = parser.parseExpression("officers['advisors'][0]").getValue(societyContext, Inventor.class);
    assertThat(i.getName()).isEqualTo("Nikola Tesla");

    parser.parseExpression("officers['advisors'][0].PlaceOfBirth.Country").setValue(societyContext, "Croatia");

    Inventor i2 = parser.parseExpression("reverse[0]['advisors'][0]").getValue(societyContext, Inventor.class);
    assertThat(i2.getName()).isEqualTo("Nikola Tesla");

  }

  // 7.5.3

  @Test
  void methodInvocation2() throws Exception {
    // string literal, evaluates to "bc"
    String c = parser.parseExpression("'abc'.substring(1, 3)").getValue(String.class);
    assertThat(c).isEqualTo("bc");

    StandardEvaluationContext societyContext = new StandardEvaluationContext();
    societyContext.setRootObject(new IEEE());
    // evaluates to true
    boolean isMember = parser.parseExpression("isMember('Mihajlo Pupin')").getValue(societyContext, Boolean.class);
    assertThat(isMember).isTrue();
  }

  // 7.5.4.1

  @Test
  void relationalOperators() throws Exception {
    boolean result = parser.parseExpression("2 == 2").getValue(Boolean.class);
    assertThat(result).isTrue();
    // evaluates to false
    result = parser.parseExpression("2 < -5.0").getValue(Boolean.class);
    assertThat(result).isFalse();

    // evaluates to true
    result = parser.parseExpression("'black' < 'block'").getValue(Boolean.class);
    assertThat(result).isTrue();
  }

  @Test
  void otherOperators() throws Exception {
    // evaluates to false
    boolean falseValue = parser.parseExpression("'xyz' instanceof T(int)").getValue(Boolean.class);
    assertThat(falseValue).isFalse();

    // evaluates to true
    boolean trueValue = parser.parseExpression("'5.00' matches '^-?\\d+(\\.\\d{2})?$'").getValue(Boolean.class);
    assertThat(trueValue).isTrue();

    //evaluates to false
    falseValue = parser.parseExpression("'5.0067' matches '^-?\\d+(\\.\\d{2})?$'").getValue(Boolean.class);
    assertThat(falseValue).isFalse();
  }

  // 7.5.4.2

  @Test
  void logicalOperators() throws Exception {

    StandardEvaluationContext societyContext = new StandardEvaluationContext();
    societyContext.setRootObject(new IEEE());

    // -- AND --

    // evaluates to false
    boolean falseValue = parser.parseExpression("true and false").getValue(Boolean.class);
    assertThat(falseValue).isFalse();
    // evaluates to true
    String expression = "isMember('Nikola Tesla') and isMember('Mihajlo Pupin')";
    boolean trueValue = parser.parseExpression(expression).getValue(societyContext, Boolean.class);

    // -- OR --

    // evaluates to true
    trueValue = parser.parseExpression("true or false").getValue(Boolean.class);
    assertThat(trueValue).isTrue();

    // evaluates to true
    expression = "isMember('Nikola Tesla') or isMember('Albert Einstien')";
    trueValue = parser.parseExpression(expression).getValue(societyContext, Boolean.class);
    assertThat(trueValue).isTrue();

    // -- NOT --

    // evaluates to false
    falseValue = parser.parseExpression("!true").getValue(Boolean.class);
    assertThat(falseValue).isFalse();

    // -- AND and NOT --
    expression = "isMember('Nikola Tesla') and !isMember('Mihajlo Pupin')";
    falseValue = parser.parseExpression(expression).getValue(societyContext, Boolean.class);
    assertThat(falseValue).isFalse();
  }

  // 7.5.4.3

  @Test
  void numericalOperators() throws Exception {
    // Addition
    int two = parser.parseExpression("1 + 1").getValue(Integer.class); // 2
    assertThat(two).isEqualTo(2);

    String testString = parser.parseExpression("'test' + ' ' + 'string'").getValue(String.class); // 'test string'
    assertThat(testString).isEqualTo("test string");

    // Subtraction
    int four = parser.parseExpression("1 - -3").getValue(Integer.class); // 4
    assertThat(four).isEqualTo(4);

    double d = parser.parseExpression("1000.00 - 1e4").getValue(Double.class); // -9000
    assertThat(d).isCloseTo(-9000.0d, within((double) 0));

    // Multiplication
    int six = parser.parseExpression("-2 * -3").getValue(Integer.class); // 6
    assertThat(six).isEqualTo(6);

    double twentyFour = parser.parseExpression("2.0 * 3e0 * 4").getValue(Double.class); // 24.0
    assertThat(twentyFour).isCloseTo(24.0d, within((double) 0));

    // Division
    int minusTwo = parser.parseExpression("6 / -3").getValue(Integer.class); // -2
    assertThat(minusTwo).isEqualTo(-2);

    double one = parser.parseExpression("8.0 / 4e0 / 2").getValue(Double.class); // 1.0
    assertThat(one).isCloseTo(1.0d, within((double) 0));

    // Modulus
    int three = parser.parseExpression("7 % 4").getValue(Integer.class); // 3
    assertThat(three).isEqualTo(3);

    int oneInt = parser.parseExpression("8 / 5 % 2").getValue(Integer.class); // 1
    assertThat(oneInt).isEqualTo(1);

    // Operator precedence
    int minusTwentyOne = parser.parseExpression("1+2-3*8").getValue(Integer.class); // -21
    assertThat(minusTwentyOne).isEqualTo(-21);
  }

  // 7.5.5

  @Test
  void assignment() throws Exception {
    Inventor inventor = new Inventor();
    StandardEvaluationContext inventorContext = new StandardEvaluationContext();
    inventorContext.setRootObject(inventor);

    parser.parseExpression("foo").setValue(inventorContext, "Alexander Seovic2");

    assertThat(parser.parseExpression("foo").getValue(inventorContext, String.class)).isEqualTo("Alexander Seovic2");
    // alternatively

    String aleks = parser.parseExpression("foo = 'Alexandar Seovic'").getValue(inventorContext, String.class);
    assertThat(parser.parseExpression("foo").getValue(inventorContext, String.class)).isEqualTo("Alexandar Seovic");
    assertThat(aleks).isEqualTo("Alexandar Seovic");
  }

  // 7.5.6

  @Test
  void types() throws Exception {
    Class<?> dateClass = parser.parseExpression("T(java.util.Date)").getValue(Class.class);
    assertThat(dateClass).isEqualTo(Date.class);
    boolean trueValue = parser.parseExpression("T(java.math.RoundingMode).CEILING < T(java.math.RoundingMode).FLOOR").getValue(Boolean.class);
    assertThat(trueValue).isTrue();
  }

  // 7.5.7

  @Test
  void constructors() throws Exception {
    StandardEvaluationContext societyContext = new StandardEvaluationContext();
    societyContext.setRootObject(new IEEE());
    Inventor einstein =
            parser.parseExpression("new cn.taketoday.expression.spel.testresources.Inventor('Albert Einstein',new java.util.Date(), 'German')").getValue(Inventor.class);
    assertThat(einstein.getName()).isEqualTo("Albert Einstein");
    //create new inventor instance within add method of List
    parser.parseExpression("Members2.add(new cn.taketoday.expression.spel.testresources.Inventor('Albert Einstein', 'German'))").getValue(societyContext);
  }

  // 7.5.8

  @Test
  void variables() throws Exception {
    Inventor tesla = new Inventor("Nikola Tesla", "Serbian");
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("newName", "Mike Tesla");

    context.setRootObject(tesla);

    parser.parseExpression("foo = #newName").getValue(context);

    assertThat(tesla.getFoo()).isEqualTo("Mike Tesla");
  }

  @Test
  @SuppressWarnings("unchecked")
  void specialVariables() throws Exception {
    // create an array of integers
    List<Integer> primes = Arrays.asList(2, 3, 5, 7, 11, 13, 17);

    // create parser and set variable 'primes' as the array of integers
    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("primes", primes);

    // all prime numbers > 10 from the list (using selection ?{...})
    List<Integer> primesGreaterThanTen = (List<Integer>) parser.parseExpression("#primes.?[#this>10]").getValue(context);
    assertThat(primesGreaterThanTen.toString()).isEqualTo("[11, 13, 17]");
  }

  // 7.5.9

  @Test
  void functions() throws Exception {
    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.registerFunction("reverseString", StringUtils.class.getDeclaredMethod("reverseString", String.class));

    String helloWorldReversed = parser.parseExpression("#reverseString('hello world')").getValue(context, String.class);
    assertThat(helloWorldReversed).isEqualTo("dlrow olleh");
  }

  @Test
  void methodHandlesNotBound() throws Throwable {
    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext context = new StandardEvaluationContext();

    MethodHandle mh = MethodHandles.lookup().findVirtual(String.class, "formatted",
            MethodType.methodType(String.class, Object[].class));
    context.setVariable("message", mh);

    String message = parser.parseExpression("#message('Simple message: <%s>', 'Hello World', 'ignored')")
            .getValue(context, String.class);
    assertThat(message).isEqualTo("Simple message: <Hello World>");
  }

  @Test
  void methodHandlesFullyBound() throws Throwable {
    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext context = new StandardEvaluationContext();

    String template = "This is a %s message with %s words: <%s>";
    Object varargs = new Object[] { "prerecorded", 3, "Oh Hello World!", "ignored" };
    MethodHandle mh = MethodHandles.lookup().findVirtual(String.class, "formatted",
                    MethodType.methodType(String.class, Object[].class))
            .bindTo(template)
            .bindTo(varargs); //here we have to provide arguments in a single array binding
    context.setVariable("message", mh);

    String message = parser.parseExpression("#message()")
            .getValue(context, String.class);
    assertThat(message).isEqualTo("This is a prerecorded message with 3 words: <Oh Hello World!>");
  }

  // 7.5.10

  @Test
  void ternary() throws Exception {
    String falseString = parser.parseExpression("false ? 'trueExp' : 'falseExp'").getValue(String.class);
    assertThat(falseString).isEqualTo("falseExp");

    StandardEvaluationContext societyContext = new StandardEvaluationContext();
    societyContext.setRootObject(new IEEE());

    parser.parseExpression("Name").setValue(societyContext, "IEEE");
    societyContext.setVariable("queryName", "Nikola Tesla");

    String expression = "isMember(#queryName)? #queryName + ' is a member of the ' "
            + "+ Name + ' Society' : #queryName + ' is not a member of the ' + Name + ' Society'";

    String queryResultString = parser.parseExpression(expression).getValue(societyContext, String.class);
    assertThat(queryResultString).isEqualTo("Nikola Tesla is a member of the IEEE Society");
    // queryResultString = "Nikola Tesla is a member of the IEEE Society"
  }

  // 7.5.11

  @Test
  @SuppressWarnings("unchecked")
  void selection() throws Exception {
    StandardEvaluationContext societyContext = new StandardEvaluationContext();
    societyContext.setRootObject(new IEEE());
    List<Inventor> list = (List<Inventor>) parser.parseExpression("Members2.?[nationality == 'Serbian']").getValue(societyContext);
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getName()).isEqualTo("Nikola Tesla");
  }

  // 7.5.12

  @Test
  void templating() throws Exception {
    String randomPhrase =
            parser.parseExpression("random number is ${T(java.lang.Math).random()}", new TemplatedParserContext()).getValue(String.class);
    assertThat(randomPhrase.startsWith("random number")).isTrue();
  }

  static class TemplatedParserContext implements ParserContext {

    @Override
    public String getExpressionPrefix() {
      return "${";
    }

    @Override
    public String getExpressionSuffix() {
      return "}";
    }

    @Override
    public boolean isTemplate() {
      return true;
    }
  }

  static class IEEE {
    private String name;

    public Inventor[] Members = new Inventor[1];
    public List Members2 = new ArrayList();
    public Map<String, Object> officers = new HashMap<>();

    public List<Map<String, Object>> reverse = new ArrayList<>();

    @SuppressWarnings("unchecked")
    IEEE() {
      officers.put("president", pupin);
      List linv = new ArrayList();
      linv.add(tesla);
      officers.put("advisors", linv);
      Members2.add(tesla);
      Members2.add(pupin);

      reverse.add(officers);
    }

    public boolean isMember(String name) {
      return true;
    }

    public String getName() { return name; }

    public void setName(String n) { this.name = n; }
  }

  static class StringUtils {

    public static String reverseString(String input) {
      return new StringBuilder(input).reverse().toString();
    }
  }

}
