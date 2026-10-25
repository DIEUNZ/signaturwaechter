package de.bjarne.sigguard.core.listener;

import de.bjarne.sigguard.core.ItemSignatureService;
import de.bjarne.sigguard.core.ItemSignatureService.Check;
import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.SigGuardConfiguration;
import de.bjarne.sigguard.core.detect.DetectionResult;
import de.bjarne.sigguard.core.detect.SignatureVerdict;
import de.bjarne.sigguard.core.reminder.AuctionTimeParser;
import de.bjarne.sigguard.core.reminder.ReminderManager;
import de.bjarne.sigguard.core.reminder.ReminderManager.Reminder;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.world.ItemStackTooltipEvent;

/**
 * Hängt die Warnung an den Item-Tooltip und merkt sich das gehoverte Item für den Timer.
 */
public class TooltipListener {

  private static final int MAX_WARNED = 500;

  private final SigGuardAddon addon;
  private final ItemSignatureService service;
  private final Set<String> warnedItems = Collections.synchronizedSet(new HashSet<>());

  public TooltipListener(SigGuardAddon addon, ItemSignatureService service) {
    this.addon = addon;
    this.service = service;
  }

  @Subscribe
  public void onTooltip(ItemStackTooltipEvent event) {
    if (!this.addon.configuration().enabled().get() || this.service.isComputingLines()) {
      return;
    }

    List<Component> lines = event.getTooltipLines();
    List<String> plain = ItemSignatureService.toPlain(lines);
    this.addon.hoverTracker().update(event.itemStack(), plain);

    Optional<Check> check = this.service.check(lines);
    if (check.isPresent() && check.get().result().verdict().isWarning()) {
      lines.add(Component.empty());
      lines.add(warningLine(check.get().result()));

      if (this.addon.configuration().chatWarning().get()) {
        this.warnOnceInChat(event.itemStack().getDisplayName(), check.get());
      }
    }

    this.addReminderHint(lines, plain);
  }

  // Schlägt bei Auktionen einen Timer vor bzw. zeigt den gesetzten an
  private void addReminderHint(List<Component> lines, List<String> plain) {
    SigGuardConfiguration config = this.addon.configuration();
    Key key = config.reminderKey().get();
    if (!config.reminderEnabled().get() || !config.reminderHint().get()
        || key == null || key == Key.NONE) {
      return;
    }

    Optional<Duration> remaining = AuctionTimeParser.parseRemaining(plain);
    if (remaining.isEmpty()) {
      return;
    }

    String keyName = "[" + key.getName() + "]";
    Optional<Reminder> existing =
        this.addon.reminders().find(AuctionTimeParser.stableKey(plain));
    Component hint;
    if (existing.isPresent()) {
      hint = Component.text("⏰ Erinnerung gesetzt für "
          + ReminderManager.format(existing.get().remindAt())
          + " – " + keyName + " zum Entfernen", NamedTextColor.GREEN);
    } else {
      long endsAt = System.currentTimeMillis() + remaining.get().toMillis();
      int lead = config.reminderLeadMinutes().get();
      String end = "endet ca. " + ReminderManager.format(endsAt);
      if (remaining.get().compareTo(Duration.ofMinutes(lead)) <= 0) {
        hint = Component.text("⏰ Endet gleich (" + end + ")", NamedTextColor.YELLOW);
      } else {
        String when = lead == 0 ? "bei Ablauf" : lead + " Min. vor Ende";
        hint = Component.text("⏰ " + keyName + " Erinnern " + when + " (" + end + ")",
            NamedTextColor.AQUA);
      }
    }

    lines.add(Component.empty());
    lines.add(hint);
  }

  public static Component warningLine(DetectionResult result) {
    String text = result.verdict() == SignatureVerdict.FAKE
        ? "⚠ FÄLSCHUNG – imitiert " + result.matchedName()
        : "⚠ Mögliche Fälschung – ähnlich zu " + result.matchedName();
    return Component.text(text, NamedTextColor.RED);
  }

  // Pro Item (Name + Signatur + Datum) nur einmal im Chat warnen
  private void warnOnceInChat(Component itemName, Check check) {
    String itemPlain = ItemSignatureService.toPlain(List.of(itemName)).get(0);
    String key = itemPlain + '|' + check.signature().name() + '|' + check.signature().date();
    if (this.warnedItems.size() > MAX_WARNED) {
      this.warnedItems.clear();
    }
    if (!this.warnedItems.add(key)) {
      return;
    }

    this.addon.displayMessage(Component.text("[SignaturWächter] ", NamedTextColor.DARK_RED)
        .append(Component.text(itemPlain + ": signiert von " + check.signature().name()
            + " ", NamedTextColor.GOLD))
        .append(warningLine(check.result())));
  }
}
