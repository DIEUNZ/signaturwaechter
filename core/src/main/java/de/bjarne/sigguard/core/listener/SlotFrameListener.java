package de.bjarne.sigguard.core.listener;

import de.bjarne.sigguard.core.ItemSignatureService;
import de.bjarne.sigguard.core.SigGuardAddon;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.render.item.GuiItemLocation;
import net.labymod.api.event.client.render.item.ItemDecorationRenderEvent;

/**
 * Zeichnet einen roten Rahmen um Slots mit verdächtiger Signatur.
 */
public class SlotFrameListener {

  private static final int RED = 0xFF_FF_20_20;
  private static final float SLOT_SIZE = 16F;

  private final SigGuardAddon addon;
  private final ItemSignatureService service;

  public SlotFrameListener(SigGuardAddon addon, ItemSignatureService service) {
    this.addon = addon;
    this.service = service;
  }

  @Subscribe
  public void onDecoration(ItemDecorationRenderEvent event) {
    if (!this.addon.configuration().enabled().get()
        || !this.addon.configuration().slotFrame().get()
        || event.location() != GuiItemLocation.CONTAINER
        || event.itemStack().isAir()) {
      return;
    }

    if (this.service.analyze(event.itemStack()).isWarning()) {
      event.screenContext().canvas().submitRelativeOutlineRect(
          event.x(), event.y(), SLOT_SIZE, SLOT_SIZE, 1F, RED, RED);
    }
  }
}
