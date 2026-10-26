package de.bjarne.sigguard.core.listener;

import de.bjarne.sigguard.core.ItemSignatureService;
import de.bjarne.sigguard.core.ItemSignatureService.Check;
import de.bjarne.sigguard.core.Messages;
import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.SigGuardConfiguration;
import de.bjarne.sigguard.core.VersionCompat;
import de.bjarne.sigguard.core.detect.DetectionResult;
import de.bjarne.sigguard.core.detect.IcedOutDetector;
import de.bjarne.sigguard.core.detect.SpawnEggDetector;
import de.bjarne.sigguard.core.detect.SignatureVerdict;
import de.bjarne.sigguard.core.reminder.AuctionTooltipParser;
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

    if (this.addon.configuration().icedOutCheck().get()
        && IcedOutDetector.isFake(plain, !event.itemStack().getEnchantmentEntries().isEmpty())) {
      lines.add(Component.empty());
      lines.add(Messages.withSymbol(VersionCompat.WARNING, "warning.icedOut", NamedTextColor.RED));

      if (this.addon.configuration().chatWarning().get()
          && this.warnedItems.add("icedout|" + plain.get(0))) {
        Messages.send("warning.chatIcedOut", plain.get(0));
      }
    }

    String eggMob = ItemSignatureService.eggMob(event.itemStack());
    if (this.addon.configuration().spawnEggCheck().get()
        && SpawnEggDetector.isMismatchForMob(eggMob, plain.isEmpty() ? null : plain.get(0))) {
      lines.add(Component.empty());
      lines.add(Messages.withSymbol(VersionCompat.WARNING, "warning.spawnEgg",
          NamedTextColor.GOLD, eggMob));

      if (this.addon.configuration().chatWarning().get()
          && this.warnedItems.add("spawnegg|" + plain.get(0))) {
        Messages.send("warning.chatSpawnEgg", plain.get(0), eggMob);
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

    Optional<Duration> remaining = AuctionTooltipParser.parseRemaining(plain);
    if (remaining.isEmpty()) {
      return;
    }

    String keyName = key.getName();
    long endsAtMillis = System.currentTimeMillis() + remaining.get().toMillis();
    Optional<Reminder> existing =
        this.addon.reminders().find(AuctionTooltipParser.stableKey(plain), endsAtMillis);
    Component hint;
    if (existing.isPresent()) {
      hint = Messages.withSymbol(VersionCompat.CLOCK, "hint.set", NamedTextColor.GREEN,
          ReminderManager.format(existing.get().remindAt()), keyName);
    } else {
      String endsAt = ReminderManager.format(endsAtMillis);
      int lead = config.reminderLeadMinutes().get();
      if (remaining.get().compareTo(Duration.ofMinutes(lead)) <= 0) {
        hint = Messages.withSymbol(VersionCompat.CLOCK, "hint.endingSoon",
            NamedTextColor.YELLOW, endsAt);
      } else if (lead == 0) {
        hint = Messages.withSymbol(VersionCompat.CLOCK, "hint.offerAtEnd",
            NamedTextColor.AQUA, keyName, endsAt);
      } else {
        hint = Messages.withSymbol(VersionCompat.CLOCK, "hint.offer",
            NamedTextColor.AQUA, keyName, lead, endsAt);
      }
    }

    lines.add(Component.empty());
    lines.add(hint);
  }

  private static String warningKey(DetectionResult result) {
    return result.verdict() == SignatureVerdict.FAKE ? "warning.fake" : "warning.suspect";
  }

  public static Component warningLine(DetectionResult result) {
    return Messages.withSymbol(VersionCompat.WARNING, warningKey(result), NamedTextColor.RED,
        result.matchedName());
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

    Messages.send("warning.chat", itemPlain, check.signature().name(),
        Messages.text(warningKey(check.result()), check.result().matchedName()));
  }
}
