package de.bjarne.sigguard.core.detect;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Erkennt gefälschte Rang-Items ganz ohne Signatur: Echte Ränge (z. B. "SUPREME-Rang (Item)")
 * tragen die Zeile "Signiert von ...". Fehlt sie, ist es vermutlich eine Fälschung.
 * Enthält keinen Minecraft-Code.
 */
public final class UnsignedRankDetector {

  // "SUPREME-Rang (Item)", "Rang Premium" usw.: das Wort "Rang" als eigenes Wort im Namen
  private static final Pattern RANK_NAME =
      Pattern.compile("(?i)(?:^|[\\s-])rang(?:$|[\\s(:-])");

  private UnsignedRankDetector() {
  }

  /**
   * @param lines        Tooltip als reiner Text, Zeile 0 ist der Itemname
   * @param hasSignature ob im Tooltip eine Signatur gefunden wurde
   * @return {@code true}, wenn der Name ein Rang ist, aber keine Signatur vorhanden ist
   */
  public static boolean isFake(List<String> lines, boolean hasSignature) {
    return !hasSignature && isRank(lines);
  }

  /** Der Itemname enthält "Rang" als eigenes Wort. */
  public static boolean isRank(List<String> lines) {
    return !lines.isEmpty() && RANK_NAME.matcher(lines.get(0).trim()).find();
  }
}
