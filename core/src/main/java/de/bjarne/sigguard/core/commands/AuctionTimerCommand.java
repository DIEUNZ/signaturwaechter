package de.bjarne.sigguard.core.commands;

import de.bjarne.sigguard.core.Messages;
import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.reminder.ReminderManager;
import de.bjarne.sigguard.core.reminder.ReminderManager.Reminder;
import java.util.List;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.format.NamedTextColor;

/**
 * Clientseitiger Befehl /aht list|clear – wird nicht an den Server gesendet.
 */
public class AuctionTimerCommand extends Command {

  private final SigGuardAddon addon;

  public AuctionTimerCommand(SigGuardAddon addon) {
    super("aht");
    this.addon = addon;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    if (!this.addon.configuration().enabled().get()) {
      this.displayMessage(Messages.chat("disabled", NamedTextColor.RED));
      return true;
    }

    ReminderManager reminders = this.addon.reminders();
    String action = arguments.length == 0 ? "list" : arguments[0].toLowerCase();
    switch (action) {
      case "clear" -> this.displayMessage(
          Messages.chat("reminder.cleared", NamedTextColor.GRAY, reminders.clear()));
      case "list" -> this.list(reminders);
      default -> this.displayMessage(Messages.chat("reminder.usage", NamedTextColor.RED));
    }
    return true;
  }

  private void list(ReminderManager reminders) {
    List<Reminder> list = reminders.reminders();
    if (list.isEmpty()) {
      this.displayMessage(Messages.chat("reminder.none", NamedTextColor.GRAY));
      return;
    }
    for (Reminder reminder : list) {
      this.displayMessage(Messages.tr("reminder.entry", NamedTextColor.YELLOW,
          reminder.itemName(), ReminderManager.format(reminder.endsAt()),
          ReminderManager.format(reminder.remindAt())));
    }
  }

  @Override
  public List<String> complete(String[] arguments) {
    return arguments.length <= 1 ? List.of("list", "clear") : List.of();
  }
}
