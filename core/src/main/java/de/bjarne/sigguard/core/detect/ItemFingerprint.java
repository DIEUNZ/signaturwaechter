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
  private static final Pattern CODE_LINE = Pattern.compile("^(?=.*\\d)[A-Za-z0-9_]{12,20}$");
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

  /**
   * Schlüssel der festen Beschreibung für den Katalog echter Items: Zahlen als "#", ohne
   * technische Zeilen (Verzauberungen, Übersetzungsschlüssel).
   */
  public static String catalogKey(List<String> description) {
    List<String> kept = new ArrayList<>();
    for (String line : description) {
      if (!line.startsWith("enchantment.") && !line.startsWith("potion.")
          && !line.startsWith("item.") && !line.startsWith("attribute.")
          && !line.startsWith("block.") && !line.startsWith("filled_map.")
          && !IcedOutDetector.isEnchantmentLine(line)) {
        kept.add(line.replaceAll("\\d+", "#"));
      }
    }
    return String.join(" / ", kept);
  }

  /**
   * Itemname zum Vergleichen: Zahlen als "#", ohne Mehrzahl-n ("2 Sommer-Kisten" und
   * "1 Sommer-Kiste" ergeben denselben Schlüssel).
   */
  public static String nameKey(String name) {
    return name.replaceAll("\\d+", "#").replaceAll("en$", "e");
  }

  /** Eine echte Signatur steht in einer eigenen Zeile, nie im Itemnamen (Zeile 0). */
  public static boolean signatureInName(List<String> lines) {
    return !lines.isEmpty() && lines.get(0).toLowerCase(Locale.ROOT).contains("signiert von");
  }

  /**
   * Bei Fälschungen steht die zufällige Kennung über der Signatur-Zeile, bei Originalen
   * darunter.
   */
  public static boolean codeAboveSignature(List<String> lines) {
    for (int i = 1; i < lines.size(); i++) {
      if (!SIGNATURE_LINE.matcher(lines.get(i).trim()).matches()) {
        continue;
      }
      for (int j = i - 1; j >= 1; j--) {
        String previous = lines.get(j).trim();
        if (!previous.isEmpty()) {
          return CODE_LINE.matcher(previous).matches();
        }
      }
      return false;
    }
    return false;
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
