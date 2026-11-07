package de.bjarne.sigguard.core;

import de.bjarne.sigguard.core.detect.AdminTextDetector;
import de.bjarne.sigguard.core.detect.DetectionResult;
import de.bjarne.sigguard.core.detect.ItemCatalog;
import de.bjarne.sigguard.core.detect.ItemFingerprint;
import de.bjarne.sigguard.core.detect.SignatureVerdict;
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
import net.labymod.api.nbt.tags.NBTTagCompound;

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
          && SpawnEggDetector.isMismatchForMob(eggMob(itemStack), plain.isEmpty() ? null : plain.get(0));
      Optional<Check> check = this.check(lines);
      boolean adminTextFake = this.config.adminTextCheck().get() && forgery(plain, check) != null;
      Analysis analysis = new Analysis(
          check,
          icedOutFake,
          spawnEggFake,
          adminTextFake,
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

  /**
   * Mob, das im Spawn-Ei steckt (z. B. "ghast"), oder {@code null}, wenn es kein Spawn-Ei ist.
   * Neue Versionen: aus der Item-ID. 1.8.9: aus dem Schadenswert. 1.9 bis 1.12: aus EntityTag.id.
   */
  public static String eggMob(ItemStack itemStack) {
    String path = itemStack.getIdentifier().getPath();
    if (path.endsWith("_spawn_egg")) {
      return path.substring(0, path.length() - "_spawn_egg".length());
    }
    if (!path.equals("spawn_egg")) {
      return null;
    }
    try {
      NBTTagCompound tag = itemStack.getNBTTag();
      if (tag != null && tag.contains("EntityTag")) {
        String fromTag = SpawnEggDetector.mobFromEntityTag(
            tag.getCompound("EntityTag").getString("id"));
        if (fromTag != null) {
          return fromTag;
        }
      }
    } catch (RuntimeException ignored) {
      // Versionen ohne lesbare NBT-Daten: dann bleibt nur der Schadenswert
    }
    return SpawnEggDetector.mobFromLegacyData(itemStack.getLegacyItemData());
  }

  /**
   * Prüft Text und Aufbau eines Items gegen echte Admin-Items. Reihenfolge: kopierte Kennung über
   * der Signatur, Name passt nicht zum Katalog, Admin-Text ohne echte Signatur.
   *
   * @return Meldung (Schlüssel und Argument) oder {@code null}, wenn nichts auffällt
   */
  public static Forgery forgery(List<String> plain, Optional<Check> check) {
    if (check.isPresent() && ItemFingerprint.codeAboveSignature(plain)) {
      return new Forgery("warning.codeAbove", "");
    }

    List<String> description = ItemFingerprint.of(plain, "", null, "").description();
    String expected = ItemCatalog.expectedName(ItemFingerprint.catalogKey(description));
    if (expected != null && !plain.isEmpty()
        && !ItemFingerprint.nameKey(expected)
        .equals(ItemFingerprint.nameKey(plain.get(0).trim()))) {
      return new Forgery("warning.titleMismatch", expected);
    }

    // GrieferGames signiert einige echte Items selbst (z. B. Galaxy Farbe)
    boolean signedByAdmin = check.isPresent()
        && (check.get().result().verdict() == SignatureVerdict.ECHT
        || "GrieferGames".equals(check.get().signature().name()));
    return AdminTextDetector.isFake(description, signedByAdmin)
        ? new Forgery("warning.adminText", "") : null;
  }

  /** Meldung zu einer erkannten Fälschung: i18n-Schlüssel und Argument. */
  public record Forgery(String key, String argument) {
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
   * @param spawnEggFake Spawn-Ei, dessen Name ein anderes Mob nennt
   * @param adminTextFake Admin-typischer Text ohne die echte Signatur
   * @param auctionKey Kennung der Auktion oder {@code null}, wenn es keine ist
   * @param ownBid     ob du auf die Auktion geboten hast (Höchstbietender oder Gebotslimit)
   * @param endsAtEstimate geschätzte Endzeit (Epoch-Millis) oder -1
   * @param computedAt Zeitpunkt der Analyse (Epoch-Millis)
   */
  public record Analysis(
      Optional<Check> check,
      boolean icedOutFake,
      boolean spawnEggFake,
      boolean adminTextFake,
      String auctionKey,
      boolean ownBid,
      long endsAtEstimate,
      long computedAt
  ) {

    /** Rote Warnung: gefälschte Signatur oder Iced Out ohne Verzauberung. */
    public boolean isFake() {
      return this.icedOutFake || this.adminTextFake
          || this.check.map(value -> value.result().verdict().isWarning()).orElse(false);
    }

    /** Irgendeine Warnung (rot oder orange). */
    public boolean isWarning() {
      return this.isFake() || this.spawnEggFake;
    }
  }
}
