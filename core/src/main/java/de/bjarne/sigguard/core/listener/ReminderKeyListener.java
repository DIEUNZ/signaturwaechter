package de.bjarne.sigguard.core.listener;

import de.bjarne.sigguard.core.Messages;
import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.reminder.AuctionTooltipParser;
import de.bjarne.sigguard.core.reminder.HoverTracker.Hovered;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;
import net.labymod.api.event.client.input.KeyEvent.State;

/**
 * Timer-Taste: beim Hovern über eine Auktion eine Erinnerung setzen bzw. entfernen.
 */
public class ReminderKeyListener {

  private final SigGuardAddon addon;

  public ReminderKeyListener(SigGuardAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onKey(KeyEvent event) {
    Key key = this.addon.configuration().reminderKey().get();
    if (event.state() != State.PRESS
        || key == null || key == Key.NONE || event.key() != key
        || !this.addon.configuration().enabled().get()
        || !this.addon.configuration().reminderEnabled().get()) {
      return;
    }

    Hovered hovered = this.addon.hoverTracker().current();
    if (hovered == null || hovered.lines().isEmpty()) {
      return;
    }

    List<String> lines = hovered.lines();
    String itemName = lines.get(0);
    Optional<Duration> remaining = AuctionTooltipParser.parseRemaining(lines);
    if (remaining.isEmpty()) {
      this.addon.displayMessage(Messages.chat("reminder.noTime", NamedTextColor.RED, itemName));
      return;
    }

    String bid = AuctionTooltipParser.parseBid(lines).orElse(null);
    this.addon.reminders().toggle(
        AuctionTooltipParser.stableKey(lines), itemName, bid, remaining.get());
  }
}
