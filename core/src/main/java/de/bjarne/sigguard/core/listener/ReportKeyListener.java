package de.bjarne.sigguard.core.listener;

import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.reminder.HoverTracker.Hovered;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;
import net.labymod.api.event.client.input.KeyEvent.State;

/**
 * Melde-Taste: beim Hovern über ein Item die Auswahl "Fehler melden" öffnen.
 */
public class ReportKeyListener {

  private final SigGuardAddon addon;

  public ReportKeyListener(SigGuardAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onKey(KeyEvent event) {
    Key key = this.addon.configuration().reportKey().get();
    if (event.state() != State.PRESS
        || key == null || key == Key.NONE || event.key() != key
        || !this.addon.configuration().enabled().get()
        || !this.addon.configuration().reportEnabled().get()) {
      return;
    }

    Hovered hovered = this.addon.hoverTracker().current();
    if (hovered == null || hovered.lines().isEmpty()) {
      return;
    }
    this.addon.reports().start(hovered);
  }
}
