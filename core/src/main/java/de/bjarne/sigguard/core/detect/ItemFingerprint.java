package de.bjarne.sigguard.core.detect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Fingerabdruck eines Items: Name, Material und die feste Beschreibung (ohne Signatur-Zeile,
 * zufällige Kennung und Auktionsangaben). Damit lassen sich Admin-Items miteinander vergleichen.
 * Enthält keinen Minecraft-Code.
 */
public record ItemFingerprint(
    String name,
    String material,
    List<String> description,
    String signer,
    String verdict
) {

  private static final Pattern SIGNATURE_LINE = Pattern.compile("(?i)^signiert von\\b.*");
  // Zufällige Kennung wie "1iD_j0cFiO_jxGswCf": ein einzelnes Wort ohne Leerzeichen
  private static final Pattern ID_LINE = Pattern.compile("^[A-Za-z0-9_]{10,}$");

  /**
   * @param lines    Tooltip als reiner Text, Zeile 0 ist der Itemname
   * @param material Item-ID, z. B. {@code minecraft:spruce_door}
   * @param signer   gefundener Signierer oder {@code null}
   * @param verdict  Ergebnis der Signaturprüfung oder "KEINE_SIGNATUR"
   */
  public static ItemFingerprint of(List<String> lines, String material, String signer,
      String verdict) {
    String name = lines.isEmpty() ? "" : lines.get(0).trim();
    List<String> description = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      if (isEndOfDescription(line)) {
        break;
      }
      if (line.isEmpty() || SIGNATURE_LINE.matcher(line).matches()
          || ID_LINE.matcher(line).matches()) {
        continue;
      }
      description.add(line);
    }
    return new ItemFingerprint(name, material, description, signer, verdict);
  }

  /** Gleiche Items ergeben denselben Schlüssel (zum Aussortieren von Doppelten). */
  public String key() {
    return this.name + '|' + this.material + '|' + String.join("/", this.description) + '|'
        + this.signer + '|' + this.verdict;
  }

  // Der Trennstrich oder die Auktionsangaben beenden die feste Beschreibung
  private static boolean isEndOfDescription(String line) {
    String lower = line.toLowerCase(Locale.ROOT);
    return line.contains("░") || lower.contains("aktuelles gebot")
        || lower.contains("sofortkauf") || lower.contains("verbleibende zeit")
        || lower.startsWith("klicke, um");
  }
}
