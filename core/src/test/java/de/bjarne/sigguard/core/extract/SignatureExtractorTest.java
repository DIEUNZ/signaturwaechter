package de.bjarne.sigguard.core.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bjarne.sigguard.core.extract.SignatureExtractor.Signature;
import java.util.List;
import org.junit.jupiter.api.Test;

class SignatureExtractorTest {

  private final SignatureExtractor extractor =
      new SignatureExtractor(SignatureExtractor.DEFAULT_PATTERN);

  @Test
  void fakeAusAuktionshaus() {
    List<String> lore = List.of(
        "Du erhältst die Frost-Schrift! (Klicken)",
        "a4Wwn2ZYFcQVn6Yw",
        "Signiert von AbgegriefHD am 24.10.2026",
        "$ | Aktuelles Gebot:");
    Signature signature = this.extractor.extract(lore).orElseThrow();
    assertEquals("AbgegriefHD", signature.name());
    assertEquals("24.10.2026", signature.date());
  }

  @Test
  void echteMitFarbcodes() {
    List<String> lore = List.of(
        "§7Aktiviert §a+1 weiteres Clan-Home §7per Rechtsklick!",
        "§7Signiert von §aAbgegrieftHD §7am §e01.06.2026",
        "§d70h5lIJ0209aAFKa");
    assertEquals("AbgegrieftHD", this.extractor.extract(lore).orElseThrow().name());
  }

  @Test
  void ohneSignatur() {
    assertTrue(this.extractor.extract(List.of("Ein normales Item")).isEmpty());
  }

  @Test
  void ungueltigeRegexFaelltZurueck() {
    SignatureExtractor broken = new SignatureExtractor("([");
    assertEquals("AbgegrieftHD",
        broken.extract(List.of("Signiert von AbgegrieftHD am 1.6.2026")).orElseThrow().name());
  }
}
