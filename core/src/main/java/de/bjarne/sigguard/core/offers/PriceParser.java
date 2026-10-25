package de.bjarne.sigguard.core.offers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wandelt GG-Preise wie "2.500.000$" oder "1.234,50$" in Zahlen um.
 * Punkt = Tausendertrennzeichen, Komma = Dezimaltrennzeichen.
 */
public final class PriceParser {

  private static final Pattern NUMBER = Pattern.compile("\\d[\\d.]*(,\\d+)?");

  private PriceParser() {
  }

  public static Optional<BigDecimal> parse(String text) {
    if (text == null) {
      return Optional.empty();
    }
    Matcher matcher = NUMBER.matcher(text.replace(" ", ""));
    if (!matcher.find()) {
      return Optional.empty();
    }
    String normalized = matcher.group().replace(".", "").replace(',', '.');
    return Optional.of(new BigDecimal(normalized));
  }
}
