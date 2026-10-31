package de.bjarne.sigguard.core.commands;

import de.bjarne.sigguard.core.Messages;
import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.SigGuardConfiguration;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.chat.command.Command;

/**
 * Clientseitiger Befehl /status: zeigt, was das Addon gerade macht, und den Link zum Forum-Thread.
 * Wird nicht an den Server gesendet.
 */
public class StatusCommand extends Command {

  private static final String FORUM_URL = "https://shorturl.at/MOHa6";

  private final SigGuardAddon addon;

  public StatusCommand(SigGuardAddon addon) {
    super("status", "ahstatus");
    this.addon = addon;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    SigGuardConfiguration config = this.addon.configuration();
    if (!config.enabled().get()) {
      Messages.send("disabled");
      return true;
    }

    Messages.send("status.line",
        onOff(config.icedOutCheck().get()),
        onOff(config.spawnEggCheck().get()),
        config.whitelist().get(),
        this.addon.reminders().reminders().size());

    Component link = Component.text(FORUM_URL, NamedTextColor.AQUA)
        .clickEvent(ClickEvent.openUrl(FORUM_URL));
    Laby.labyAPI().minecraft().chatExecutor().displayClientMessage(
        Component.text(Messages.text("status.forum") + " ", NamedTextColor.GRAY).append(link));
    return true;
  }

  private static String onOff(boolean value) {
    return Messages.text(value ? "status.on" : "status.off");
  }
}
