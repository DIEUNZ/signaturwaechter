package de.bjarne.sigguard.core.offers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sammlung aller gesehenen Angebote, pro Angebot nur der neueste Stand.
 */
public class OfferBook {

  public static final int MAX_ENTRIES = 20_000;

  private final Map<String, OfferRecord> offers = new LinkedHashMap<>();
  private boolean dirty;

  /** Fügt ein Angebot hinzu oder aktualisiert es. Gibt zurück, ob sich etwas geändert hat. */
  public synchronized boolean upsert(OfferRecord offer) {
    OfferRecord previous = this.offers.get(offer.key());
    if (previous != null && sameContent(previous, offer)) {
      return false;
    }

    this.offers.remove(offer.key());
    this.offers.put(offer.key(), offer);
    // Älteste Einträge verwerfen, damit die Datei nicht endlos wächst
    while (this.offers.size() > MAX_ENTRIES) {
      this.offers.remove(this.offers.keySet().iterator().next());
    }
    this.dirty = true;
    return true;
  }

  public synchronized void addAll(List<OfferRecord> loaded) {
    loaded.forEach(offer -> this.offers.put(offer.key(), offer));
  }

  /** Alle Angebote, ältester zuerst. */
  public synchronized List<OfferRecord> snapshot() {
    List<OfferRecord> list = new ArrayList<>(this.offers.values());
    list.sort(Comparator.comparingLong(OfferRecord::timestamp));
    return list;
  }

  public synchronized int size() {
    return this.offers.size();
  }

  public synchronized int clear() {
    int count = this.offers.size();
    this.offers.clear();
    this.dirty = true;
    return count;
  }

  /** Liefert, ob seit dem letzten Aufruf etwas geändert wurde, und setzt das Flag zurück. */
  public synchronized boolean consumeDirty() {
    boolean wasDirty = this.dirty;
    this.dirty = false;
    return wasDirty;
  }

  // Gleich bis auf den Zeitstempel → kein neuer Eintrag nötig
  private static boolean sameContent(OfferRecord a, OfferRecord b) {
    return a.amount() == b.amount()
        && a.fakeSuspect() == b.fakeSuspect()
        && a.itemType().equals(b.itemType())
        && a.displayName().equals(b.displayName())
        && a.signature().equals(b.signature())
        && a.seller().equals(b.seller())
        && (a.totalPrice() == null
        ? b.totalPrice() == null
        : b.totalPrice() != null && a.totalPrice().compareTo(b.totalPrice()) == 0);
  }
}
