package de.bjarne.sigguard.core.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemCatalogTest {

  private static String expectedFor(List<String> lines) {
    return ItemCatalog.expectedName(
        ItemFingerprint.catalogKey(ItemFingerprint.of(lines, "", null, "").description()));
  }

  @Test
  void renamedClanItemIsRecognized() {
    List<String> fake = List.of("Custom-Prefix (Prinzessin)", "Protection I", "",
        "Du erhältst +1 Clanmitglieder (Rechtsklick)", "Signiert von AbgegrieftHD am 13.09.2019",
        "MH0M3YIS70pzVsQ1", "Vergibt ein weiteres Clan-Mitglied! (Rechtsklick)", "░░░");
    assertEquals("+# Clanmitglied", expectedFor(fake));
  }

  @Test
  void renamedEnderchestItemIsRecognized() {
    List<String> fake = List.of("+1 Seite in /showcase Item", "",
        "Vergrößert deine Enderchest um eine weitere Zeile!", "░░░");
    assertEquals("Enderchest Upgrade", expectedFor(fake));
  }

  @Test
  void realNamesWithNumbersMatchTheCatalog() {
    assertEquals(ItemFingerprint.nameKey("+1 Clanmitglied"), "+# Clanmitglied");
    assertEquals(ItemFingerprint.nameKey("+5 Clanmitglied"), "+# Clanmitglied");
  }

  @Test
  void unknownDescriptionHasNoExpectedName() {
    assertTrue(expectedFor(List.of("Irgendein Item", "Ganz neuer Text", "░")) == null);
  }

  @Test
  void codeAboveSignatureIsDetected() {
    assertTrue(ItemFingerprint.codeAboveSignature(List.of("Item", "Text",
        "MH0M3YIS70pzVsQ1", "Signiert von AbgegrieftHD am 13.09.2019")));
    assertFalse(ItemFingerprint.codeAboveSignature(List.of("Item", "Text",
        "Signiert von AbgegrieftHD am 13.09.2019", "MH0M3YIS70pzVsQ1")));
    assertFalse(ItemFingerprint.codeAboveSignature(List.of("Item", "Haltbarkeitsstufe",
        "Signiert von AbgegrieftHD am 13.09.2019")));
  }
}
