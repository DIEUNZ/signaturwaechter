package de.bjarne.sigguard.core.extract;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Findet die Signaturzeile in reinem Text (ohne Minecraft-Abhängigkeiten).
 * Beispiel: "Signiert von AbgegrieftHD am 01.06.2026".
 */
public final class SignatureExtractor {

  public static final String DEFAULT_PATTERN =
      "(?i)signiert von\\s+(\\S+)\\s+am\\s+(\\d{1,2}\\.\\d{1,2}\\.\\d{4})";

  // Legacy-Farbcodes wie §a oder &a
  private static final Pattern COLOR_CODES = Pattern.compile("(?i)[§&][0-9a-fk-orx]");

  private final Pattern pattern;

  public SignatureExtractor(String regex) {
    this.pattern = compileOrDefault(regex);
  }

  public Optional<Signature> extract(List<String> lines) {
    for (String line : lines) {
      Matcher matcher = this.pattern.matcher(stripFormatting(line));
      if (matcher.find() && matcher.groupCount() >= 1) {
        String date = matcher.groupCount() >= 2 ? matcher.group(2) : null;
        return Optional.of(new Signature(matcher.group(1), date));
      }
    }
    return Optional.empty();
  }

  public static String stripFormatting(String text) {
    return COLOR_CODES.matcher(text).replaceAll("").strip();
  }

  // Ungültige Regex aus den Settings fällt auf das Standardmuster zurück
  private static Pattern compileOrDefault(String regex) {
    if (regex != null && !regex.isBlank()) {
      try {
        return Pattern.compile(regex);
      } catch (PatternSyntaxException ignored) {
      }
    }
    return Pattern.compile(DEFAULT_PATTERN);
  }

  /**
   * @param name Name des Signierers
   * @param date Datum laut Signatur oder {@code null}
   */
  public record Signature(String name, String date) {
  }
}
