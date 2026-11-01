package de.bjarne.sigguard.core.detect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UnsignedRankDetectorTest {

  private static final List<String> RANK = List.of("SUPREME-Rang (Item)", "",
      "Aktuelles Gebot:", "minecraft:pink_dye");

  @Test
  void rankWithoutSignatureIsFake() {
    assertTrue(UnsignedRankDetector.isFake(RANK, false));
  }

  @Test
  void signedRankIsFine() {
    assertFalse(UnsignedRankDetector.isFake(RANK, true));
  }

  @Test
  void otherItemsAreIgnored() {
    assertFalse(UnsignedRankDetector.isFake(List.of("+1 Clan-Home (Item)", "Hallo"), false));
    assertFalse(UnsignedRankDetector.isFake(List.of("Rangliste", "Hallo"), false));
    assertFalse(UnsignedRankDetector.isFake(List.of("Strange Item"), false));
    assertFalse(UnsignedRankDetector.isFake(List.of(), false));
  }
}
