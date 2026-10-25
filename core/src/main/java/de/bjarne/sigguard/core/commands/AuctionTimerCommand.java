package de.bjarne.sigguard.core.commands;

import de.bjarne.sigguard.core.reminder.ReminderManager;
import de.bjarne.sigguard.core.reminder.ReminderManager.Reminder;
import java.util.List;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;

/**
 * Clientseitiger Befehl /aht list|clear – wird nicht an den Server gesendet.
 */
public class AuctionTimerCommand extends Command {

  private final ReminderManager reminders;

  public AuctionTimerCommand(ReminderManager reminders) {
    super("aht");
    this.reminders = reminders;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    String action = arguments.length == 0 ? "list" : arguments[0].toLowerCase();
    switch (action) {
      case "clear" -> this.displayMessage(Component.text(
          this.reminders.clear() + " Erinnerung(en) gelöscht.", NamedTextColor.GRAY));
      case "list" -> this.list();
      default -> this.displayMessage(
          Component.text("Benutzung: /aht list | /aht clear", NamedTextColor.RED));
    }
    return true;
  }

  private void list() {
    List<Reminder> list = this.reminders.reminders();
    if (list.isEmpty()) {
      this.displayMessage(Component.text("Keine Erinnerungen aktiv.", NamedTextColor.GRAY));
      return;
    }
    for (Reminder reminder : list) {
      this.displayMessage(Component.text("• " + reminder.itemName()
          + " – endet " + ReminderManager.format(reminder.endsAt())
          + ", Erinnerung " + ReminderManager.format(reminder.remindAt()), NamedTextColor.YELLOW));
    }
  }

  @Override
  public List<String> complete(String[] arguments) {
    return arguments.length <= 1 ? List.of("list", "clear") : List.of();
  }
}
