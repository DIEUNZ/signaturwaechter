package de.bjarne.sigguard.core.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemFingerprintTest {

  private static final List<String> HOME = List.of(
      "+1 Clan-Home (Item)",
      "",
      "Aktiviert +1 weiteres Clan-Home per Rechtsklick!",
      "Signiert von AbgegrieftHD am 01.06.2026",
      "iUD5RUvCldkIR_jCo",
      "",
      "░░░░░░░░░░░░░░░░",
      "",
      "$ | Aktuelles Gebot:",
      "1$",
      "Klicke, um die Auktion zu öffnen.",
      "minecraft:spruce_door");

  @Test
  void descriptionKeepsOnlyFixedLines() {
    ItemFingerprint fingerprint = ItemFingerprint.of(HOME, "minecraft:spruce_door",
        "AbgegrieftHD", "ECHT");
    assertEquals("+1 Clan-Home (Item)", fingerprint.name());
    assertEquals(List.of("Aktiviert +1 weiteres Clan-Home per Rechtsklick!"),
        fingerprint.description());
  }

  @Test
  void sameItemWithOtherDateAndIdHasSameKey() {
    List<String> other = List.of(
        "+1 Clan-Home (Item)", "", "Aktiviert +1 weiteres Clan-Home per Rechtsklick!",
        "Signiert von AbgegrieftHD am 13.09.2019", "xYz12345_abcDEFg", "░░░", "$ | Aktuelles Gebot:");
    assertEquals(
        ItemFingerprint.of(HOME, "minecraft:spruce_door", "AbgegrieftHD", "ECHT").key(),
        ItemFingerprint.of(other, "minecraft:spruce_door", "AbgegrieftHD", "ECHT").key());
  }
}
