package de.bjarne.sigguard.core.detect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Katalog echter Admin-Items: feste Beschreibung -> Name, den das echte Item trägt. Stammt aus
 * gesammelten Items (assets/sigguard/catalog.tsv, je Zeile "Beschreibung TAB Name").
 * Enthält keinen Minecraft-Code.
 */
public final class ItemCatalog {

  private static final Map<String, String> EXPECTED = load();

  private ItemCatalog() {
  }

  /**
   * @param catalogKey Beschreibung als {@link ItemFingerprint#catalogKey}
   * @return der echte Name (Zahlen als "#") oder {@code null}, wenn die Beschreibung unbekannt ist
   */
  public static String expectedName(String catalogKey) {
    return EXPECTED.get(catalogKey);
  }

  private static Map<String, String> load() {
    Map<String, String> map = new HashMap<>();
    try (InputStream stream = ItemCatalog.class.getResourceAsStream(
        "/assets/sigguard/catalog.tsv")) {
      if (stream == null) {
        return map;
      }
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(stream, StandardCharsets.UTF_8));
      String line;
      while ((line = reader.readLine()) != null) {
        int tab = line.indexOf('\t');
        if (tab > 0) {
          map.put(line.substring(0, tab), line.substring(tab + 1));
        }
      }
    } catch (IOException ignored) {
      // Ohne Katalog entfällt nur der Namensvergleich, der Rest des Addons läuft weiter
    }
    return map;
  }
}
