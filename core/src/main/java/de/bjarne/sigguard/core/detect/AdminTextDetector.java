package de.bjarne.sigguard.core.detect;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Erkennt gefälschte Admin-Items an ihrem Text: Echte Items von GrieferGames (Farben, Schriften,
 * Perks, Kisten, Rechte usw.) nutzen feste Formulierungen und tragen die Signatur von
 * AbgegrieftHD. Steht so eine Formulierung auf einem Item ohne diese Signatur, ist es
 * vermutlich eine Fälschung. Die Muster stammen aus gesammelten echten Items.
 * Enthält keinen Minecraft-Code.
 */
public final class AdminTextDetector {

  private static final List<Pattern> ADMIN_LINES = List.of(
      Pattern.compile("^Du erhältst (?:die|den|das|jeweils) .+!(?: \\(.*\\))?$"),
      Pattern.compile("^Du erhältst \\+\\d+ .+"),
      Pattern.compile("^Aktiviert .+ per Rechtsklick!?$"),
      Pattern.compile("^Vergibt .+! \\(Rechtsklick\\)$"),
      Pattern.compile("^Schaltet den Skin .+ im Plot-NPC frei\\.$"));

  private AdminTextDetector() {
  }

  /**
   * @param description   feste Beschreibung des Items (siehe {@link ItemFingerprint})
   * @param signedByAdmin ob eine gültige Signatur aus der Whitelist gefunden wurde
   * @return {@code true}, wenn der Text nach Admin-Item klingt, die echte Signatur aber fehlt
   */
  public static boolean isFake(List<String> description, boolean signedByAdmin) {
    return !signedByAdmin && looksLikeAdminItem(description);
  }

  /** Mindestens eine Zeile folgt einer der festen Admin-Formulierungen. */
  public static boolean looksLikeAdminItem(List<String> description) {
    for (String line : description) {
      for (Pattern pattern : ADMIN_LINES) {
        if (pattern.matcher(line.trim()).find()) {
          return true;
        }
      }
    }
    return false;
  }
}
