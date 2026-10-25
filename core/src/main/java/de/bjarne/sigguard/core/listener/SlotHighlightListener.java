package de.bjarne.sigguard.core.listener;

import de.bjarne.sigguard.core.ItemSignatureService;
import de.bjarne.sigguard.core.ItemSignatureService.Analysis;
import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.SigGuardConfiguration;
import net.labymod.api.client.gui.screen.state.ScreenCanvas;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.render.item.GuiItemLocation;
import net.labymod.api.event.client.render.item.ItemSlotBackgroundRenderEvent;
import net.labymod.api.util.Color;

/**
 * Färbt Auktions-Slots hinter dem Item ein: Timer gesetzt bzw. eigenes Gebot.
 * Farben sind in den Settings einstellbar.
 */
public class SlotHighlightListener {

  private static final float SLOT_SIZE = 16F;

  private final SigGuardAddon addon;
  private final ItemSignatureService service;

  public SlotHighlightListener(SigGuardAddon addon, ItemSignatureService service) {
    this.addon = addon;
    this.service = service;
  }

  @Subscribe
  public void onSlotBackground(ItemSlotBackgroundRenderEvent event) {
    SigGuardConfiguration config = this.addon.configuration();
    if (!config.enabled().get()
        || (!config.highlightReminder().get() && !config.highlightBid().get())
        || event.location() != GuiItemLocation.CONTAINER
        || event.itemStack().isAir()) {
      return;
    }

    Analysis analysis = this.service.analyze(event.itemStack());
    if (analysis.auctionKey() == null) {
      return;
    }

    boolean bid = config.highlightBid().get() && analysis.ownBid();
    boolean reminder = config.highlightReminder().get() && config.reminderEnabled().get()
        && this.addon.reminders().find(analysis.auctionKey()).isPresent();
    if (!bid && !reminder) {
      return;
    }

    ScreenCanvas canvas = event.screenContext().canvas();
    if (bid) {
      canvas.submitRelativeRect(event.x(), event.y(), SLOT_SIZE, SLOT_SIZE,
          argb(config.bidColor().get()));
    }
    if (reminder) {
      int color = argb(config.reminderColor().get());
      // Beides aktiv: Gebot als Fläche, Timer als Rahmen
      if (bid) {
        canvas.submitRelativeOutlineRect(event.x(), event.y(), SLOT_SIZE, SLOT_SIZE, 1F,
            color, color);
      } else {
        canvas.submitRelativeRect(event.x(), event.y(), SLOT_SIZE, SLOT_SIZE, color);
      }
    }
  }

  // Farbe inkl. der im Farbwähler eingestellten Transparenz
  private static int argb(Color color) {
    return (color.getAlpha() << 24) | (color.get() & 0x00_FF_FF_FF);
  }
}
