package de.bjarne.sigguard.core.commands;

import de.bjarne.sigguard.core.offers.OfferRecorder;
import java.util.List;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;

/**
 * Clientseitiger Befehl /angebote export|anzahl|leeren – wird nicht an den Server gesendet.
 */
public class OffersCommand extends Command {

  private final OfferRecorder offers;

  public OffersCommand(OfferRecorder offers) {
    super("angebote");
    this.offers = offers;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    String action = arguments.length == 0 ? "anzahl" : arguments[0].toLowerCase();
    switch (action) {
      case "export" -> this.offers.export();
      case "anzahl" -> this.displayMessage(Component.text(
          this.offers.count() + " Angebote gespeichert in " + this.offers.directory(),
          NamedTextColor.GRAY));
      case "leeren" -> this.displayMessage(Component.text(
          this.offers.clear() + " Angebote gelöscht.", NamedTextColor.GRAY));
      default -> this.displayMessage(Component.text(
          "Benutzung: /angebote export | anzahl | leeren", NamedTextColor.RED));
    }
    return true;
  }

  @Override
  public List<String> complete(String[] arguments) {
    return arguments.length <= 1 ? List.of("export", "anzahl", "leeren") : List.of();
  }
}
