package de.bjarne.sigguard.core;

import de.bjarne.sigguard.core.extract.SignatureExtractor;
import java.util.ArrayList;
import java.util.List;
import net.labymod.api.Laby;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.KeybindWidget.KeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.dropdown.DropdownWidget.DropdownSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.color.ColorPickerWidget.ColorPickerSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.annotation.SettingSection;
import net.labymod.api.util.Color;
import net.labymod.api.util.MethodOrder;

@ConfigName("settings")
public class SigGuardConfiguration extends AddonConfig {

  private static final String DISCORD_URL = "https://discord.gg/sCex8JNTGS";

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  /** Sprache der Chat-Meldungen und Hinweise des Addons, unabhängig von der Spielsprache. */
  @DropdownSetting
  private final ConfigProperty<Language> language = new ConfigProperty<>(Language.GERMAN);

  // --- Signaturprüfung ---
  @SettingSection("signature")
  // AbgegrieftHD signiert die offiziellen Admin-Items (Perks, Prefixe, Suffixe usw.)
  @TextFieldSetting
  private final ConfigProperty<String> whitelist = new ConfigProperty<>("AbgegrieftHD");

  @SliderSetting(min = 0, max = 4)
  private final ConfigProperty<Integer> threshold = new ConfigProperty<>(2);

  @SwitchSetting
  private final ConfigProperty<Boolean> icedOutCheck = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> spawnEggCheck = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> adminTextCheck = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> verifiedNote = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> playerDetection = new ConfigProperty<>(true);

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

  @DropdownSetting
  private final ConfigProperty<ReminderSound> reminderSound =
      new ConfigProperty<>(ReminderSound.PLING);

  @SliderSetting(min = 0, max = 100)
  private final ConfigProperty<Integer> reminderVolume = new ConfigProperty<>(100);

  @SwitchSetting
  private final ConfigProperty<Boolean> reminderHint = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> reportEnabled = new ConfigProperty<>(true);

  @KeyBindSetting
  private final ConfigProperty<Key> reportKey = new ConfigProperty<>(Key.K);

  // --- Info-Panel neben dem Auktionshaus ---
  @SettingSection("panel")
  @SwitchSetting
  private final ConfigProperty<Boolean> panelEnabled = new ConfigProperty<>(true);

  @DropdownSetting
  private final ConfigProperty<PanelSide> panelSide = new ConfigProperty<>(PanelSide.RIGHT);

  @SwitchSetting
  private final ConfigProperty<Boolean> panelKeys = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> panelLegend = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> panelCommands = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> panelStatus = new ConfigProperty<>(true);

  // --- Slot-Farben im Auktionshaus ---
  @SettingSection("highlight")
  @SwitchSetting
  private final ConfigProperty<Boolean> highlightReminder = new ConfigProperty<>(true);

  @ColorPickerSetting(alpha = true)
  private final ConfigProperty<Color> reminderColor =
      new ConfigProperty<>(Color.ofRGB(30, 120, 255, 180));

  @SwitchSetting
  private final ConfigProperty<Boolean> highlightBid = new ConfigProperty<>(true);

  @ColorPickerSetting(alpha = true)
  private final ConfigProperty<Color> bidColor =
      new ConfigProperty<>(Color.ofRGB(40, 200, 60, 150));

  // --- Support ---
  /** Öffnet den Support-Discord (mit Rückfrage von LabyMod, bevor der Link aufgeht). */
  @SettingSection("support")
  @MethodOrder(after = "bidColor")
  @ButtonSetting
  public void openDiscord() {
    Laby.labyAPI().minecraft().chatExecutor().openUrl(DISCORD_URL, true);
  }

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<Language> language() {
    return this.language;
  }

  public ConfigProperty<String> whitelist() {
    return this.whitelist;
  }

  /** Whitelist als Liste (Komma, Semikolon oder Leerzeichen getrennt). */
  public List<String> whitelistNames() {
    List<String> names = new ArrayList<>();
    for (String name : this.whitelist.get().split("[,;\\s]+")) {
      if (!name.isBlank()) {
        names.add(name);
      }
    }
    return names;
  }

  public ConfigProperty<Integer> threshold() {
    return this.threshold;
  }

  public ConfigProperty<Boolean> icedOutCheck() {
    return this.icedOutCheck;
  }

  public ConfigProperty<Boolean> spawnEggCheck() {
    return this.spawnEggCheck;
  }

  public ConfigProperty<Boolean> adminTextCheck() {
    return this.adminTextCheck;
  }

  public ConfigProperty<Boolean> verifiedNote() {
    return this.verifiedNote;
  }

  public ConfigProperty<Boolean> playerDetection() {
    return this.playerDetection;
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

  public ConfigProperty<Boolean> reportEnabled() {
    return this.reportEnabled;
  }

  public ConfigProperty<Key> reportKey() {
    return this.reportKey;
  }

  public ConfigProperty<Boolean> panelEnabled() {
    return this.panelEnabled;
  }

  public ConfigProperty<PanelSide> panelSide() {
    return this.panelSide;
  }

  public ConfigProperty<Boolean> panelKeys() {
    return this.panelKeys;
  }

  public ConfigProperty<Boolean> panelLegend() {
    return this.panelLegend;
  }

  public ConfigProperty<Boolean> panelCommands() {
    return this.panelCommands;
  }

  public ConfigProperty<Boolean> panelStatus() {
    return this.panelStatus;
  }

  public ConfigProperty<Integer> reminderLeadMinutes() {
    return this.reminderLeadMinutes;
  }

  public ConfigProperty<ReminderSound> reminderSound() {
    return this.reminderSound;
  }

  public ConfigProperty<Integer> reminderVolume() {
    return this.reminderVolume;
  }

  public ConfigProperty<Boolean> reminderHint() {
    return this.reminderHint;
  }

  public ConfigProperty<Boolean> highlightReminder() {
    return this.highlightReminder;
  }

  public ConfigProperty<Color> reminderColor() {
    return this.reminderColor;
  }

  public ConfigProperty<Boolean> highlightBid() {
    return this.highlightBid;
  }

  public ConfigProperty<Color> bidColor() {
    return this.bidColor;
  }

  /** Ton der Auktions-Erinnerung. */
  public enum ReminderSound {
    PLING,
    XP,
    LEVELUP,
    VILLAGER,
    OFF
  }

  /** Seite des Info-Panels neben dem Auktionshaus-Fenster. */
  public enum PanelSide {
    RIGHT,
    LEFT
  }

  /** Sprache der Addon-Meldungen: Deutsch, Englisch oder wie das Spiel. */
  public enum Language {
    GERMAN,
    ENGLISH,
    GAME
  }
}
