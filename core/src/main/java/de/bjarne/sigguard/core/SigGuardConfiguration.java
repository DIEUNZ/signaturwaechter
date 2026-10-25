package de.bjarne.sigguard.core;

import de.bjarne.sigguard.core.extract.SignatureExtractor;
import java.util.Arrays;
import java.util.List;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.KeybindWidget.KeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.annotation.SettingSection;

@ConfigName("settings")
public class SigGuardConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  // --- Signaturprüfung ---
  @SettingSection("signature")
  @TextFieldSetting
  private final ConfigProperty<String> whitelist = new ConfigProperty<>("AbgegrieftHD");

  @SliderSetting(min = 0, max = 4)
  private final ConfigProperty<Integer> threshold = new ConfigProperty<>(2);

  @SwitchSetting
  private final ConfigProperty<Boolean> slotFrame = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> chatWarning = new ConfigProperty<>(false);

  @TextFieldSetting
  private final ConfigProperty<String> signaturePattern =
      new ConfigProperty<>(SignatureExtractor.DEFAULT_PATTERN);

  // --- Auktions-Erinnerung ---
  @SettingSection("reminder")
  @SwitchSetting
  private final ConfigProperty<Boolean> reminderEnabled = new ConfigProperty<>(true);

  @KeyBindSetting
  private final ConfigProperty<Key> reminderKey = new ConfigProperty<>(Key.R);

  @SliderSetting(min = 0, max = 30)
  private final ConfigProperty<Integer> reminderLeadMinutes = new ConfigProperty<>(2);

  @SwitchSetting
  private final ConfigProperty<Boolean> reminderHint = new ConfigProperty<>(true);

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<String> whitelist() {
    return this.whitelist;
  }

  /** Whitelist als Liste (Komma, Semikolon oder Leerzeichen getrennt). */
  public List<String> whitelistNames() {
    return Arrays.stream(this.whitelist.get().split("[,;\\s]+"))
        .filter(name -> !name.isBlank())
        .toList();
  }

  public ConfigProperty<Integer> threshold() {
    return this.threshold;
  }

  public ConfigProperty<Boolean> slotFrame() {
    return this.slotFrame;
  }

  public ConfigProperty<Boolean> chatWarning() {
    return this.chatWarning;
  }

  public ConfigProperty<String> signaturePattern() {
    return this.signaturePattern;
  }

  public ConfigProperty<Boolean> reminderEnabled() {
    return this.reminderEnabled;
  }

  public ConfigProperty<Key> reminderKey() {
    return this.reminderKey;
  }

  public ConfigProperty<Integer> reminderLeadMinutes() {
    return this.reminderLeadMinutes;
  }

  public ConfigProperty<Boolean> reminderHint() {
    return this.reminderHint;
  }
}
