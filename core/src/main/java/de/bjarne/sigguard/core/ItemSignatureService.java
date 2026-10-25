package de.bjarne.sigguard.core;

import de.bjarne.sigguard.core.detect.DetectionResult;
import de.bjarne.sigguard.core.detect.SignatureDetector;
import de.bjarne.sigguard.core.extract.SignatureExtractor;
import de.bjarne.sigguard.core.extract.SignatureExtractor.Signature;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.serializer.plain.PlainTextComponentSerializer;
import net.labymod.api.client.world.item.ItemStack;

/**
 * Verbindet Minecraft-Items mit der Erkennungslogik und cached Ergebnisse.
 */
public class ItemSignatureService {

  private final SigGuardConfiguration config;

  // Pro Item-Objekt gecachtes Ergebnis, damit der Slot-Rahmen nicht jeden Frame rechnet
  private final Map<ItemStack, Optional<Check>> slotCache =
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

  /** Prüft ein Item ohne offenen Tooltip (für den Slot-Rahmen), mit Cache. */
  public Optional<Check> checkCached(ItemStack itemStack) {
    this.refreshIfSettingsChanged();
    return this.slotCache.computeIfAbsent(itemStack, stack -> {
      this.computingLines = true;
      try {
        return this.check(stack.getTooltipLines(false));
      } finally {
        this.computingLines = false;
      }
    });
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
}
