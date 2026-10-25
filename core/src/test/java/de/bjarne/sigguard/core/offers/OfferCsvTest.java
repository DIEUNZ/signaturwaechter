package de.bjarne.sigguard.core.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class OfferCsvTest {

  private static final ZoneId ZONE = ZoneOffset.UTC;
  private static final long TIME =
      LocalDateTime.of(2026, 10, 25, 16, 5, 12).toInstant(ZoneOffset.UTC).toEpochMilli();

  private static final OfferRecord FAKE = new OfferRecord(
      "Frost-Schrift\nSigniert von AbgegriefHD am 24.10.2026\n", TIME, "minecraft:player_head",
      "Frost-Schrift", "AbgegriefHD", 1, new BigDecimal("2500000"), "", true);
  private static final OfferRecord REAL = new OfferRecord(
      "clan-home", TIME, "minecraft:spruce_door", "+1 Clan-Home (Item)", "AbgegrieftHD", 3,
      new BigDecimal("55556"), "Händler;01", false);

  @Test
  void exportFuerDeutschesExcel() throws IOException {
    StringWriter out = new StringWriter();
    OfferCsv.writeExport(List.of(FAKE, REAL), out, ZONE);
    String[] lines = out.toString().split("\r\n");

    assertTrue(lines[0].startsWith("﻿"), "BOM für Excel");
    assertEquals("﻿Zeitstempel;Item-Typ;Anzeigename;Signatur;Menge;Gesamtpreis;"
        + "Preis pro Stück;Verkäufer;Fake-Verdacht", lines[0]);
    assertEquals("25.10.2026 16:05:12;minecraft:player_head;Frost-Schrift;AbgegriefHD;1;"
        + "2500000;2500000;;ja", lines[1]);
    // "+1 ..." wird gegen Formel-Auswertung geschützt, ";" im Feld wird gequotet,
    // Preis pro Stück mit Dezimalkomma
    assertEquals("25.10.2026 16:05:12;minecraft:spruce_door; +1 Clan-Home (Item);AbgegrieftHD;3;"
        + "55556;18518,67;\"Händler;01\";nein", lines[2]);
  }

  @Test
  void datenRoundtripBleibtUnveraendert() throws IOException {
    StringWriter out = new StringWriter();
    OfferCsv.writeData(List.of(FAKE, REAL), out);
    List<OfferRecord> loaded = OfferCsv.readData(new StringReader(out.toString()));
    assertEquals(List.of(FAKE, REAL), loaded);
  }

  @Test
  void kaputteZeilenWerdenUebersprungen() throws IOException {
    String data = "key;timestamp;itemType;displayName;signature;amount;totalPrice;seller;fakeSuspect\r\n"
        + "a;keineZahl;x;y;z;1;;;false\r\n"
        + "b;1;x;y;z;2;;;true\r\n"
        + "zu;kurz\r\n";
    List<OfferRecord> loaded = OfferCsv.readData(new StringReader(data));
    assertEquals(1, loaded.size());
    assertNull(loaded.get(0).totalPrice());
    assertTrue(loaded.get(0).fakeSuspect());
  }

  @Test
  void escapeNurWennNoetig() {
    assertEquals("normal", OfferCsv.escape("normal", true));
    assertEquals("\"a \"\"b\"\"\"", OfferCsv.escape("a \"b\"", true));
    assertEquals(" =1+1", OfferCsv.escape("=1+1", true));
    assertEquals("=1+1", OfferCsv.escape("=1+1", false));
    assertFalse(OfferCsv.escape(null, true).contains("null"));
  }
}
