package de.bjarne.sigguard.core.reminder;

import de.bjarne.sigguard.core.Messages;
import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.VersionCompat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.lifecycle.GameTickEvent;
import net.labymod.api.notification.Notification;

/**
 * Hält die Auktions-Erinnerungen und löst sie zum richtigen Zeitpunkt aus.
 * Nur im Speicher; nichts wird an den Server gesendet.
 */
public class ReminderManager {

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

  private final SigGuardAddon addon;
  private final List<Reminder> reminders = new ArrayList<>();

  public ReminderManager(SigGuardAddon addon) {
    this.addon = addon;
  }

  /** Legt eine Erinnerung an oder entfernt sie, falls es für das Item schon eine gibt. */
  public synchronized void toggle(String key, String itemName, String bid, Duration remaining) {
    long now = System.currentTimeMillis();
    long endsAt = now + remaining.toMillis();
    if (this.reminders.removeIf(reminder -> AuctionIdentity.sameAuction(
        reminder.key(), reminder.endsAt(), key, endsAt))) {
      Messages.send("reminder.removed", itemName);
      return;
    }

    long lead = Duration.ofMinutes(this.addon.configuration().reminderLeadMinutes().get()).toMillis();
    long remindAt = Math.max(now, endsAt - lead);
    this.reminders.add(new Reminder(key, itemName, bid, endsAt, remindAt));

    Messages.send("reminder.set", itemName, format(endsAt), format(remindAt));
  }

  /** Sucht die Erinnerung zu dieser Auktion (gleicher Name und etwa gleiche Endzeit). */
  public synchronized Optional<Reminder> find(String key, long endsAt) {
    for (Reminder reminder : this.reminders) {
      if (AuctionIdentity.sameAuction(reminder.key(), reminder.endsAt(), key, endsAt)) {
        return Optional.of(reminder);
      }
    }
    return Optional.empty();
  }

  public synchronized List<Reminder> reminders() {
    return List.copyOf(this.reminders);
  }

  public synchronized int clear() {
    int count = this.reminders.size();
    this.reminders.clear();
    return count;
  }

  @Subscribe
  public void onTick(GameTickEvent event) {
    if (event.phase() != Phase.POST) {
      return;
    }

    List<Reminder> due = new ArrayList<>();
    boolean enabled = this.addon.configuration().enabled().get()
        && this.addon.configuration().reminderEnabled().get();
    synchronized (this) {
      long now = System.currentTimeMillis();
      Iterator<Reminder> iterator = this.reminders.iterator();
      while (iterator.hasNext()) {
        Reminder reminder = iterator.next();
        if (reminder.remindAt() <= now) {
          due.add(reminder);
          iterator.remove();
        }
      }
    }
    // Bei deaktiviertem Addon fällige Erinnerungen still verwerfen statt nachträglich auszulösen
    if (enabled) {
      for (Reminder reminder : due) {
        this.fire(reminder);
      }
    }
  }

  private void fire(Reminder reminder) {
    long minutesLeft = Math.max(0, Math.round(
        (reminder.endsAt() - System.currentTimeMillis()) / 60_000.0));
    String plain = reminder.bid() == null
        ? Messages.text("reminder.due", reminder.itemName(), minutesLeft)
        : Messages.text("reminder.dueWithBid", reminder.itemName(), minutesLeft, reminder.bid());
    Component text = Component.text(plain, NamedTextColor.YELLOW);

    Messages.sendWithSymbol(VersionCompat.CLOCK, plain);
    Laby.labyAPI().notificationController().push(Notification.builder()
        .title(Messages.tr("reminder.title", NamedTextColor.WHITE))
        .text(text)
        .build());
    Laby.labyAPI().minecraft().sounds().playSound(VersionCompat.REMINDER_SOUND, 1F, 1F);
  }

  public static String format(long epochMillis) {
    return LocalTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).format(TIME);
  }

  /**
   * @param key      Erkennung desselben Items (Name + Signatur + Kennung)
   * @param itemName Anzeigename
   * @param bid      aktuelles Gebot oder {@code null}
   * @param endsAt   Ablaufzeitpunkt (Epoch-Millis)
   * @param remindAt Erinnerungszeitpunkt (Epoch-Millis)
   */
  public record Reminder(String key, String itemName, String bid, long endsAt, long remindAt) {
  }
}
