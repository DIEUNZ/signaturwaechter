package de.bjarne.sigguard.core.detect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdminTextDetectorTest {

  @Test
  void fakeColorWithOtherSignerIsFake() {
    assertTrue(AdminTextDetector.isFake(
        List.of("Du erhältst die Galaxy Farbe! (Klicken)"), false));
  }

  @Test
  void fakeChestsWithoutAmountAreFake() {
    assertTrue(AdminTextDetector.isFake(
        List.of("Aktiviert die Kisten per Rechtsklick!"), false));
  }

  @Test
  void realItemWithAdminSignatureIsFine() {
    assertFalse(AdminTextDetector.isFake(
        List.of("Du erhältst die Frost-Schrift! (Klicken)"), true));
  }

  @Test
  void clanItemsAreRecognized() {
    assertTrue(AdminTextDetector.looksLikeAdminItem(
        List.of("Du erhältst +1 Clanmitglieder (Rechtsklick)")));
    assertTrue(AdminTextDetector.looksLikeAdminItem(
        List.of("Aktiviert +1 weiteres Clan-Home per Rechtsklick!")));
    assertTrue(AdminTextDetector.looksLikeAdminItem(
        List.of("Vergibt ein Farbrecht! (Rechtsklick)")));
  }

  @Test
  void normalUnsignedItemsAreIgnored() {
    assertFalse(AdminTextDetector.isFake(
        List.of("Dauer: 12 Monate", "Nur für kurze Zeit im Shop!"), false));
    assertFalse(AdminTextDetector.isFake(
        List.of("Du erhältst 100 Kristalle oder doch nicht?"), false));
    assertFalse(AdminTextDetector.isFake(
        List.of("Du erhältst einen zufälligen GrieferGames Team Kopf."), false));
  }
}
