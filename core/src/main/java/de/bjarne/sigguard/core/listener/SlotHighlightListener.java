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
  private static final int STRIPE_WIDTH = 4;

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
        && analysis.endsAtEstimate() >= 0
        && this.addon.reminders().find(analysis.auctionKey(), analysis.endsAtEstimate())
        .isPresent();
    if (!bid && !reminder) {
      return;
    }

    ScreenCanvas canvas = event.screenContext().canvas();
    int bidColor = argb(config.bidColor().get());
    int reminderColor = argb(config.reminderColor().get());
    if (bid && reminder) {
      this.drawStripes(canvas, event.x(), event.y(), bidColor, reminderColor);
    } else {
      int color = bid ? bidColor : reminderColor;
      canvas.submitRelativeRect(event.x(), event.y(), SLOT_SIZE, SLOT_SIZE, color);
    }
  }

  // Diagonale Streifen im Wechsel (Gebot / Timer), zeilenweise in Abschnitten gezeichnet
  private void drawStripes(ScreenCanvas canvas, float x, float y, int first, int second) {
    int size = (int) SLOT_SIZE;
    for (int row = 0; row < size; row++) {
      int column = 0;
      while (column < size) {
        int band = (column + row) / STRIPE_WIDTH;
        int end = Math.min(size, (band + 1) * STRIPE_WIDTH - row);
        canvas.submitRelativeRect(x + column, y + row, end - column, 1F,
            band % 2 == 0 ? first : second);
        column = end;
      }
    }
  }

  // Farbe inkl. der im Farbwähler eingestellten Transparenz
  private static int argb(Color color) {
    return (color.getAlpha() << 24) | (color.get() & 0x00_FF_FF_FF);
  }
}
