package de.bjarne.sigguard.core.offers;

import de.bjarne.sigguard.core.ItemSignatureService;
import de.bjarne.sigguard.core.ItemSignatureService.Analysis;
import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.reminder.AuctionTooltipParser;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.labymod.api.Constants;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.TextColor;
import net.labymod.api.client.world.item.ItemStack;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.lifecycle.GameTickEvent;
import net.labymod.api.event.client.render.item.GuiItemLocation;
import net.labymod.api.event.client.render.item.ItemDecorationRenderEvent;

/**
 * Zeichnet sichtbare Auktionshaus-Angebote lokal auf und exportiert sie als CSV.
 */
public class OfferRecorder {

  private static final int SAVE_INTERVAL_TICKS = 20 * 30;

  private final SigGuardAddon addon;
  private final ItemSignatureService service;
  private final OfferBook book = new OfferBook();
  private final OfferStorage storage =
      new OfferStorage(Constants.Files.CONFIGS.resolve("sigguard"));

  // Bereits erfasste Item-Objekte, damit nicht jeden Frame neu gebaut wird
  private final Set<ItemStack> seen = Collections.synchronizedSet(
      Collections.newSetFromMap(new WeakHashMap<>()));
  // Ein Thread für alle Dateizugriffe, damit Speichern und Export nacheinander laufen
  private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "SignaturWaechter-IO");
    thread.setDaemon(true);
    return thread;
  });
  private int ticks;

  public OfferRecorder(SigGuardAddon addon, ItemSignatureService service) {
    this.addon = addon;
    this.service = service;
  }

  public void load() {
    try {
      this.book.addAll(this.storage.load());
    } catch (IOException exception) {
      this.addon.logger().warn("Angebote konnten nicht geladen werden", exception);
    }
  }

  @Subscribe
  public void onDecoration(ItemDecorationRenderEvent event) {
    ItemStack stack = event.itemStack();
    if (!this.addon.configuration().enabled().get()
        || !this.addon.configuration().recordOffers().get()
        || event.location() != GuiItemLocation.CONTAINER
        || stack.isAir()
        || !this.seen.add(stack)) {
      return;
    }

    Analysis analysis = this.service.analyze(stack);
    List<String> lines = analysis.lines();
    if (lines.isEmpty() || !AuctionTooltipParser.isAuction(lines)) {
      return;
    }

    this.book.upsert(new OfferRecord(
        AuctionTooltipParser.stableKey(lines),
        System.currentTimeMillis(),
        stack.getIdentifier().toString(),
        lines.get(0),
        analysis.check().map(check -> check.signature().name()).orElse(""),
        stack.getSize(),
        AuctionTooltipParser.parseBid(lines).flatMap(PriceParser::parse).orElse(null),
        AuctionTooltipParser.parseSeller(lines).orElse(""),
        analysis.isWarning()));
  }

  @Subscribe
  public void onTick(GameTickEvent event) {
    if (event.phase() == Phase.POST && ++this.ticks % SAVE_INTERVAL_TICKS == 0) {
      this.saveAsync();
    }
  }

  // Speichern im Hintergrund, damit das Spiel nicht ruckelt
  private void saveAsync() {
    if (!this.book.consumeDirty()) {
      return;
    }
    List<OfferRecord> snapshot = this.book.snapshot();
    this.io.execute(() -> {
      try {
        this.storage.save(snapshot);
      } catch (IOException exception) {
        this.addon.logger().warn("Angebote konnten nicht gespeichert werden", exception);
      }
    });
  }

  /** Exportiert alle Angebote als CSV und meldet das Ergebnis im Chat. */
  public void export() {
    List<OfferRecord> snapshot = this.book.snapshot();
    if (snapshot.isEmpty()) {
      this.message("Noch keine Angebote aufgezeichnet. Öffne das Auktionshaus und blättere durch.",
          NamedTextColor.RED);
      return;
    }

    this.saveAsync();
    this.io.execute(() -> {
      try {
        Path file = this.storage.export(snapshot, LocalDateTime.now(), ZoneId.systemDefault());
        this.message(snapshot.size() + " Angebote exportiert: "
            + file.toAbsolutePath().normalize(), NamedTextColor.GREEN);
      } catch (IOException exception) {
        this.addon.logger().warn("CSV-Export fehlgeschlagen", exception);
        this.message("Export fehlgeschlagen: " + exception.getMessage(), NamedTextColor.RED);
      }
    });
  }

  public int count() {
    return this.book.size();
  }

  public int clear() {
    int count = this.book.clear();
    this.seen.clear();
    this.saveAsync();
    return count;
  }

  public Path directory() {
    return this.storage.directory().toAbsolutePath().normalize();
  }

  private void message(String text, TextColor color) {
    this.addon.labyAPI().minecraft().executeOnRenderThread(() -> this.addon.displayMessage(
        Component.text("[SignaturWächter] ", NamedTextColor.DARK_AQUA)
            .append(Component.text(text, color))));
  }
}
