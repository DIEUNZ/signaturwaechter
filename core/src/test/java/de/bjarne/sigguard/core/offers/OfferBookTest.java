package de.bjarne.sigguard.core.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OfferBookTest {

  private static OfferRecord offer(String key, long time, String price) {
    return new OfferRecord(key, time, "minecraft:stone", "Stein", "", 64,
        new BigDecimal(price), "", false);
  }

  @Test
  void gleichesAngebotWirdAktualisiertNichtVerdoppelt() {
    OfferBook book = new OfferBook();
    assertTrue(book.upsert(offer("a", 1, "100")));
    assertFalse(book.upsert(offer("a", 2, "100")), "nur Zeitstempel neu");
    assertTrue(book.upsert(offer("a", 3, "150")), "neues Gebot");
    assertEquals(1, book.size());
    assertEquals(new BigDecimal("150"), book.snapshot().get(0).totalPrice());
  }

  @Test
  void dirtyFlag() {
    OfferBook book = new OfferBook();
    assertFalse(book.consumeDirty());
    book.upsert(offer("a", 1, "1"));
    assertTrue(book.consumeDirty());
    assertFalse(book.consumeDirty());
  }

  @Test
  void preisProStueck() {
    assertEquals(new BigDecimal("1.56"), offer("a", 1, "100").pricePerUnit());
    assertEquals(new BigDecimal("2"), offer("a", 1, "128").pricePerUnit());
  }

  @Test
  void preiseParsen() {
    assertEquals(Optional.of(new BigDecimal("2500000")), PriceParser.parse("2.500.000$"));
    assertEquals(Optional.of(new BigDecimal("55556")), PriceParser.parse("55.556$"));
    assertEquals(Optional.of(new BigDecimal("1234.50")), PriceParser.parse("1.234,50 $"));
    assertEquals(Optional.of(new BigDecimal("7")), PriceParser.parse("7$"));
    assertEquals(Optional.empty(), PriceParser.parse("kein Gebot"));
  }
}
