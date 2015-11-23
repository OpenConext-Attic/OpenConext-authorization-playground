package authzplay;

import org.junit.Test;

public class DelMeTest {

  @Test
  public void testProperties() {
    System.out.println(convert("do_some_stuff"));
  }

  private String convert(String input) {
    return String.format(input.replaceAll("_(.)", "%S"), input.replaceAll("[^_]*_(.)[^_]*", "$1_").split("_"));
  }
}
