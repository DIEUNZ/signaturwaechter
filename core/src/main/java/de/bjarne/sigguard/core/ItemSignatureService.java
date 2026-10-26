package de.bjarne.sigguard.core;

import de.bjarne.sigguard.core.detect.DetectionResult;
import de.bjarne.sigguard.core.detect.IcedOutDetector;
import de.bjarne.sigguard.core.detect.SpawnEggDetector;
import de.bjarne.sigguard.core.detect.SignatureDetector;
import de.bjarne.sigguard.core.extract.SignatureExtractor;
import de.bjarne.sigguard.core.extract.SignatureExtractor.Signature;
import de.bjarne.sigguard.core.reminder.AuctionTooltipParser;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.serializer.plain.PlainTextComponentSerializer;
import net.labymod.api.client.world.item.ItemStack;

/**
 * Verbindet Minecraft-Items mit der Erkennungslogik und cached Ergebnisse.
 */
public class ItemSignatureService {

  private final SigGuardConfiguration config;

  private static final long CACHE_MILLIS = 5_000;

  // Pro Item-Objekt gecachtes Ergebnis, damit die Slot-Markierungen nicht jeden Frame rechnen
  private final Map<ItemStack, Analysis> slotCache =
      Collections.synchronizedMap(new WeakHashMap<>());

  private SignatureDetector detector;
  private SignatureExtractor extractor;
  private String settingsKey = "";

  // Verhindert, dass unsere eigene Tooltip-Abfrage den Tooltip-Listener erneut auslöst
  private boolean computingLines;

  public ItemSignatureService(SigGuardConfiguration config) {
    this.config = config;
  }

  /** Prüft fertige Tooltip-Zeilen (aus dem Tooltip-Event). */
  public Optional<Check> check(List<Component> lines) {
    this.refreshIfSettingsChanged();
    List<String> plain = toPlain(lines);
    return this.extractor.extract(plain)
        .map(signature -> new Check(signature, this.detector.detect(signature.name()), plain));
  }

  /**
   * Analysiert ein Item ohne offenen Tooltip (Slot-Markierungen), mit Cache. Der Cache läuft
   * nach kurzer Zeit ab, damit z. B. ein überbotenes Gebot nicht ewig markiert bleibt.
   */
  public Analysis analyze(ItemStack itemStack) {
    this.refreshIfSettingsChanged();
    long now = System.currentTimeMillis();
    Analysis cached = this.slotCache.get(itemStack);
    if (cached != null && now - cached.computedAt() < CACHE_MILLIS) {
      return cached;
    }

    this.computingLines = true;
    try {
      List<Component> lines = itemStack.getTooltipLines(false);
      List<String> plain = toPlain(lines);
      boolean auction = AuctionTooltipParser.isAuction(plain);
      Optional<Duration> remaining = AuctionTooltipParser.parseRemaining(plain);
      boolean icedOutFake = this.config.icedOutCheck().get()
          && IcedOutDetector.isFake(plain, !itemStack.getEnchantmentEntries().isEmpty());
      boolean spawnEggFake = this.config.spawnEggCheck().get()
          && SpawnEggDetector.isMismatch(itemId(itemStack), plain.isEmpty() ? null : plain.get(0));
      Analysis analysis = new Analysis(
          this.check(lines),
          icedOutFake || spawnEggFake,
          auction ? AuctionTooltipParser.stableKey(plain) : null,
          auction && AuctionTooltipParser.hasOwnBid(plain, Laby.labyAPI().getName()),
          remaining.isPresent() ? now + remaining.get().toMillis() : -1,
          now);
      this.slotCache.put(itemStack, analysis);
      return analysis;
    } finally {
      this.computingLines = false;
    }
  }

  /** Item-ID wie {@code minecraft:ghast_spawn_egg}. */
  public static String itemId(ItemStack itemStack) {
    return itemStack.getIdentifier().getNamespace() + ":" + itemStack.getIdentifier().getPath();
  }

  public boolean isComputingLines() {
    return this.computingLines;
  }

  // Detector und Extractor neu bauen, wenn sich relevante Settings geändert haben
  private void refreshIfSettingsChanged() {
    String key = this.config.whitelist().get() + '\u0000'
        + this.config.threshold().get() + '\u0000'
        + this.config.signaturePattern().get();
    if (this.detector != null && key.equals(this.settingsKey)) {
      return;
    }

    this.settingsKey = key;
    this.detector = new SignatureDetector(this.config.whitelistNames(), this.config.threshold().get());
    this.extractor = new SignatureExtractor(this.config.signaturePattern().get());
    this.slotCache.clear();
  }

  public static List<String> toPlain(List<Component> lines) {
    PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
    List<String> plain = new ArrayList<>(lines.size());
    for (Component line : lines) {
      plain.add(SignatureExtractor.stripFormatting(serializer.serialize(line)));
    }
    return plain;
  }

  /**
   * @param signature gefundene Signatur
   * @param result    Prüfergebnis
   * @param lines     Tooltip als reiner Text
   */
  public record Check(Signature signature, DetectionResult result, List<String> lines) {
  }

  /**
   * @param check      Signaturprüfung, falls eine Signatur gefunden wurde
   * @param icedOutFake "Iced Out"-Item ohne Verzauberung
   * @param auctionKey Kennung der Auktion oder {@code null}, wenn es keine ist
   * @param ownBid     ob du auf die Auktion geboten hast (Höchstbietender oder Gebotslimit)
   * @param endsAtEstimate geschätzte Endzeit (Epoch-Millis) oder -1
   * @param computedAt Zeitpunkt der Analyse (Epoch-Millis)
   */
  public record Analysis(
      Optional<Check> check,
      boolean icedOutFake,
      String auctionKey,
      boolean ownBid,
      long endsAtEstimate,
      long computedAt
  ) {

    public boolean isWarning() {
      return this.icedOutFake
          || this.check.map(value -> value.result().verdict().isWarning()).orElse(false);
    }
  }
}
