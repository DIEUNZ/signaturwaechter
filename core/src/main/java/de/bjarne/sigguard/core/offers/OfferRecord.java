package de.bjarne.sigguard.core.offers;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ein gesehenes Auktionshaus-Angebot.
 *
 * @param key          Kennung des Angebots (stabil über Gebots- und Zeitänderungen)
 * @param timestamp    zuletzt gesehen (Epoch-Millis)
 * @param itemType     Item-ID, z. B. "minecraft:player_head"
 * @param displayName  Anzeigename ohne Formatierung
 * @param signature    Signierer oder leer
 * @param amount       Stückzahl
 * @param totalPrice   Gesamtpreis (aktuelles Gebot) oder {@code null}
 * @param seller       Verkäufer oder leer
 * @param fakeSuspect  Fälschungsverdacht laut Signaturprüfung
 */
public record OfferRecord(
    String key,
    long timestamp,
    String itemType,
    String displayName,
    String signature,
    int amount,
    BigDecimal totalPrice,
    String seller,
    boolean fakeSuspect
) {

  /** Preis pro Stück, auf 2 Nachkommastellen gerundet, oder {@code null}. */
  public BigDecimal pricePerUnit() {
    if (this.totalPrice == null || this.amount <= 0) {
      return null;
    }
    return this.totalPrice.divide(BigDecimal.valueOf(this.amount), 2, RoundingMode.HALF_UP)
        .stripTrailingZeros();
  }
}
