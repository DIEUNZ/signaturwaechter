package de.bjarne.sigguard.core.commands;

import de.bjarne.sigguard.core.Messages;
import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.offers.OfferRecorder;
import java.util.List;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.format.NamedTextColor;

/**
 * Clientseitiger Befehl /angebote export|anzahl|leeren – wird nicht an den Server gesendet.
 */
public class OffersCommand extends Command {

  private final SigGuardAddon addon;

  public OffersCommand(SigGuardAddon addon) {
    super("angebote");
    this.addon = addon;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    if (!this.addon.configuration().enabled().get()) {
      this.displayMessage(Messages.chat("disabled", NamedTextColor.RED));
      return true;
    }

    OfferRecorder offers = this.addon.offers();
    String action = arguments.length == 0 ? "anzahl" : arguments[0].toLowerCase();
    switch (action) {
      case "export" -> offers.export();
      case "anzahl" -> this.displayMessage(Messages.chat("offers.count", NamedTextColor.GRAY,
          offers.count(), offers.directory()));
      case "leeren" -> this.displayMessage(
          Messages.chat("offers.cleared", NamedTextColor.GRAY, offers.clear()));
      default -> this.displayMessage(Messages.chat("offers.usage", NamedTextColor.RED));
    }
    return true;
  }

  @Override
  public List<String> complete(String[] arguments) {
    return arguments.length <= 1 ? List.of("export", "anzahl", "leeren") : List.of();
  }
}
