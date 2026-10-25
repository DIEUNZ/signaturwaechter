package de.bjarne.sigguard.core.reminder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuctionTooltipParserTest {

  private static final List<String> TOOLTIP = List.of(
      "+1 Clan-Home (Item)",
      "Signiert von AbgegrieftHD am 01.06.2026",
      "$ | Aktuelles Gebot:",
      "55.556$",
      "⌚ | Verbleibende Zeit:",
      "4 Minuten",
      "Klicke, um die Auktion zu öffnen.");

  @Test
  void zeitAusFolgezeile() {
    assertEquals(Duration.ofMinutes(4), AuctionTooltipParser.parseRemaining(TOOLTIP).orElseThrow());
  }

  @Test
  void gebotAusFolgezeile() {
    assertEquals("55.556$", AuctionTooltipParser.parseBid(TOOLTIP).orElseThrow());
  }

  @Test
  void zeitInDerselbenZeile() {
    assertEquals(Duration.ofMinutes(15),
        AuctionTooltipParser.parseRemaining(List.of("Verbleibende Zeit: §a15 Minuten")).orElseThrow());
  }

  @Test
  void verschiedeneEinheiten() {
    assertEquals(Duration.ofMinutes(1), AuctionTooltipParser.parseDuration("1 Minute").orElseThrow());
    assertEquals(Duration.ofHours(2).plusMinutes(5),
        AuctionTooltipParser.parseDuration("2 Stunden 5 Minuten").orElseThrow());
    assertEquals(Duration.ofSeconds(30), AuctionTooltipParser.parseDuration("30 Sekunden").orElseThrow());
    assertEquals(Duration.ofDays(1).plusHours(3),
        AuctionTooltipParser.parseDuration("1 Tag 3 Std").orElseThrow());
    assertEquals(Duration.ofHours(1).plusMinutes(2).plusSeconds(3),
        AuctionTooltipParser.parseDuration("1h 2m 3s").orElseThrow());
  }

  @Test
  void kennungBleibtBeiAblaufenderZeitGleich() {
    List<String> later = List.of(
        "+1 Clan-Home (Item)",
        "Signiert von AbgegrieftHD am 01.06.2026",
        "$ | Aktuelles Gebot:",
        "60.000$",
        "⌚ | Verbleibende Zeit:",
        "3 Minuten");
    assertEquals(AuctionTooltipParser.stableKey(TOOLTIP), AuctionTooltipParser.stableKey(later));
    assertEquals("+1 Clan-Home (Item)\nSigniert von AbgegrieftHD am 01.06.2026\n",
        AuctionTooltipParser.stableKey(TOOLTIP));
  }

  @Test
  void verkaeuferUndAuktionErkennen() {
    assertTrue(AuctionTooltipParser.isAuction(TOOLTIP));
    assertTrue(AuctionTooltipParser.parseSeller(TOOLTIP).isEmpty());
    assertEquals("Max_Muster", AuctionTooltipParser.parseSeller(
        List.of("Verkäufer: §aMax_Muster §7(online)")).orElseThrow());
    assertTrue(!AuctionTooltipParser.isAuction(List.of("Stein")));
  }

  @Test
  void keineZeit() {
    assertTrue(AuctionTooltipParser.parseRemaining(List.of("Frost-Schrift")).isEmpty());
    assertTrue(AuctionTooltipParser.parseDuration("bald").isEmpty());
  }
}
