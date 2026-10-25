package de.bjarne.sigguard.core.detect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class IcedOutDetectorTest {

  private static final List<String> FAKE = List.of(
      "*Iced Out* Schaf", "",
      "Du erhältst ein zufälliges Spawnei oder doch nicht?",
      "Wird per Rechtsklick direkt aktiviert!",
      "minecraft:sheep_spawn_egg", "17 component(s)");

  private static final List<String> REAL = List.of(
      "*Iced Out* Lohenei", "Luck of the Sea I",
      "minecraft:blaze_spawn_egg", "17 component(s)");

  @Test
  void itemWithoutEnchantmentIsFake() {
    assertTrue(IcedOutDetector.isFake(FAKE, false));
  }

  @Test
  void enchantmentDataMakesItReal() {
    assertFalse(IcedOutDetector.isFake(FAKE, true));
  }

  @Test
  void enchantmentLineMakesItReal() {
    assertFalse(IcedOutDetector.isFake(REAL, false));
  }

  @Test
  void germanEnchantmentLineMakesItReal() {
    assertFalse(IcedOutDetector.isFake(List.of("*Iced Out* Schaf", "Glück des Meeres III"), false));
  }

  @Test
  void otherItemsAreIgnored() {
    assertFalse(IcedOutDetector.isFake(List.of("Normales Spawnei", "Hallo"), false));
    assertFalse(IcedOutDetector.isFake(List.of(), false));
  }

  @Test
  void titleVariantsAreRecognized() {
    assertTrue(IcedOutDetector.isIcedOut(List.of("*ICED OUT* Schaf")));
    assertTrue(IcedOutDetector.isIcedOut(List.of("Iced-Out Kuh")));
  }
}
