package de.bjarne.sigguard.core;

import de.bjarne.sigguard.core.commands.AuctionTimerCommand;
import de.bjarne.sigguard.core.listener.ReminderKeyListener;
import de.bjarne.sigguard.core.listener.SlotFrameListener;
import de.bjarne.sigguard.core.listener.SlotHighlightListener;
import de.bjarne.sigguard.core.listener.TooltipListener;
import de.bjarne.sigguard.core.reminder.HoverTracker;
import de.bjarne.sigguard.core.reminder.ReminderManager;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;

/**
 * Einstiegspunkt: warnt vor gefälschten Signaturen und erinnert an Auktionen.
 * Liest nur lokale Item-Daten, sendet nichts an den Server.
 */
@AddonMain
public class SigGuardAddon extends LabyAddon<SigGuardConfiguration> {

  private final HoverTracker hoverTracker = new HoverTracker();
  private ReminderManager reminders;

  @Override
  protected void enable() {
    this.registerSettingCategory();

    ItemSignatureService service = new ItemSignatureService(this.configuration());
    this.reminders = new ReminderManager(this);

    this.registerListener(new TooltipListener(this, service));
    this.registerListener(new SlotFrameListener(this, service));
    this.registerListener(new SlotHighlightListener(this, service));
    this.registerListener(new ReminderKeyListener(this));
    this.registerListener(this.reminders);
    this.registerCommand(new AuctionTimerCommand(this));

    this.logger().info("AH-Fake-Warner aktiviert");
  }

  @Override
  protected Class<SigGuardConfiguration> configurationClass() {
    return SigGuardConfiguration.class;
  }

  public HoverTracker hoverTracker() {
    return this.hoverTracker;
  }

  public ReminderManager reminders() {
    return this.reminders;
  }
}
