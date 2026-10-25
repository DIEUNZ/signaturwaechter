package de.bjarne.sigguard.core.detect;

import java.util.Locale;
import java.util.Map;

/**
 * Bringt Namen in eine Vergleichsform, in der verwechselbare Zeichen gleich sind.
 */
public final class NameNormalizer {

  // Einzelne Zeichen, die optisch verwechselt werden (nach Kleinschreibung)
  private static final Map<Character, Character> CHAR_MAP = Map.ofEntries(
      Map.entry('i', 'l'),
      Map.entry('1', 'l'),
      Map.entry('|', 'l'),
      Map.entry('!', 'l'),
      Map.entry('0', 'o'),
      Map.entry('5', 's'),
      Map.entry('3', 'e'),
      // Kyrillische Doppelgänger
      Map.entry('а', 'a'),
      Map.entry('в', 'b'),
      Map.entry('е', 'e'),
      Map.entry('һ', 'h'),
      Map.entry('і', 'l'),
      Map.entry('ӏ', 'l'),
      Map.entry('о', 'o'),
      Map.entry('р', 'p'),
      Map.entry('с', 'c'),
      Map.entry('т', 't'),
      Map.entry('х', 'x'),
      Map.entry('у', 'y'),
      Map.entry('ԁ', 'd')
  );

  // Zeichenfolgen, die zusammen wie ein anderer Buchstabe aussehen
  private static final String[][] SEQUENCES = {
      {"rn", "m"},
      {"vv", "w"},
      {"cl", "d"}
  };

  private NameNormalizer() {
  }

  public static String normalize(String name) {
    String lower = name.trim().toLowerCase(Locale.ROOT);
    StringBuilder builder = new StringBuilder(lower.length());
    for (int i = 0; i < lower.length(); i++) {
      char c = lower.charAt(i);
      builder.append(CHAR_MAP.getOrDefault(c, c));
    }

    String result = builder.toString();
    for (String[] sequence : SEQUENCES) {
      result = result.replace(sequence[0], sequence[1]);
    }
    return result;
  }
}
