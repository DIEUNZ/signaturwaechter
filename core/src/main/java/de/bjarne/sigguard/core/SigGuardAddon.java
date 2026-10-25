package de.bjarne.sigguard.core;

import de.bjarne.sigguard.core.commands.AuctionTimerCommand;
import de.bjarne.sigguard.core.commands.OffersCommand;
import de.bjarne.sigguard.core.listener.ReminderKeyListener;
import de.bjarne.sigguard.core.listener.SlotFrameListener;
import de.bjarne.sigguard.core.listener.SlotHighlightListener;
import de.bjarne.sigguard.core.listener.TooltipListener;
import de.bjarne.sigguard.core.offers.OfferRecorder;
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

  private static SigGuardAddon instance;

  private final HoverTracker hoverTracker = new HoverTracker();
  private ReminderManager reminders;
  private OfferRecorder offers;

  /** Für den Export-Button in den Settings. */
  public static SigGuardAddon get() {
    return instance;
  }

  @Override
  protected void enable() {
    instance = this;
    this.registerSettingCategory();

    ItemSignatureService service = new ItemSignatureService(this.configuration());
    this.reminders = new ReminderManager(this);
    this.offers = new OfferRecorder(this, service);
    this.offers.load();

    this.registerListener(new TooltipListener(this, service));
    this.registerListener(new SlotFrameListener(this, service));
    this.registerListener(new SlotHighlightListener(this, service));
    this.registerListener(new ReminderKeyListener(this));
    this.registerListener(this.reminders);
    this.registerListener(this.offers);
    this.registerCommand(new AuctionTimerCommand(this));
    this.registerCommand(new OffersCommand(this));

    this.logger().info("SignaturWächter aktiviert");
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

  public OfferRecorder offers() {
    return this.offers;
  }
}
