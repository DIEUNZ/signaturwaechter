package de.bjarne.sigguard.core.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.bjarne.sigguard.core.BuildConfig;
import de.bjarne.sigguard.core.Messages;
import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.detect.ItemFingerprint;
import de.bjarne.sigguard.core.reminder.HoverTracker.Hovered;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.TextColor;
import net.labymod.api.client.component.serializer.legacy.LegacyComponentSerializer;
import net.labymod.api.nbt.tags.NBTTagCompound;

/**
 * "Fehler melden": merkt sich das Item bei Tastendruck, zeigt eine Auswahl im Chat und sendet
 * die Meldung nach der Auswahl asynchron an einen Discord-Webhook. Gesendet wird nur auf
 * ausdrücklichen Klick; es gibt eine Wartezeit und jedes Item ist nur einmal meldbar.
 */
public class ReportService {

  private static final long COOLDOWN_MILLIS = 30_000;
  private static final long EXPIRE_MILLIS = 60_000;
  private static final String WEBHOOK_PREFIX = "https://discord.com/api/webhooks/";
  private static final String WEBHOOK_PREFIX_OLD = "https://discordapp.com/api/webhooks/";
  private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10)).build();

  private final SigGuardAddon addon;
  private final Set<String> reported = new HashSet<>();
  private Snapshot pending;
  private long lastSent;

  public ReportService(SigGuardAddon addon) {
    this.addon = addon;
  }

  /** Nur Discord-Webhook-URLs gelten; leer (kein Secret beim Build) heißt deaktiviert. */
  public static boolean available() {
    return BuildConfig.WEBHOOK_URL.startsWith(WEBHOOK_PREFIX)
        || BuildConfig.WEBHOOK_URL.startsWith(WEBHOOK_PREFIX_OLD);
  }

  /** Taste gedrückt: Item merken und die Auswahl im Chat anzeigen. */
  public synchronized void start(Hovered hovered) {
    if (!available()) {
      Messages.send("report.disabledBuild");
      return;
    }

    Snapshot snapshot = snapshot(hovered);
    if (!this.allowed(snapshot.hash())) {
      return;
    }
    this.pending = snapshot;

    String command = "/aht melden " + snapshot.id() + ' ';
    Component line = Component.text("\n ")
        .append(Component.text(Messages.text("prefix") + " " + Messages.text("report.prompt",
            hovered.lines().get(0)) + "\n ", NamedTextColor.GOLD))
        .append(button("report.btnReal", command + "echt", NamedTextColor.GREEN))
        .append(Component.text("  "))
        .append(button("report.btnMissed", command + "fake", NamedTextColor.RED))
        .append(Component.text("  "))
        .append(button("report.btnCancel", command + "abbrechen", NamedTextColor.GRAY))
        .append(Component.text("\n "));
    Laby.labyAPI().minecraft().chatExecutor().displayClientMessage(line);
    // Chat öffnen (schließt dabei das Auktionshaus), damit man die Buttons direkt anklicken kann
    Laby.labyAPI().minecraft().executeOnRenderThread(() -> Laby.labyAPI().minecraft().openChat(command));
  }

  /** Klick auf einen Eintrag der Auswahl ({@code /aht melden <id> echt|fake|abbrechen}). */
  public synchronized void choose(String id, String choice) {
    Snapshot snapshot = this.pending;
    if (snapshot == null || !snapshot.id().equals(id)
        || System.currentTimeMillis() - snapshot.createdAt() > EXPIRE_MILLIS) {
      this.pending = null;
      Messages.send("report.expired");
      return;
    }
    this.pending = null;

    if (choice.equals("abbrechen")) {
      Messages.send("report.cancelled");
    } else if (choice.equals("echt") || choice.equals("fake")) {
      if (this.allowed(snapshot.hash())) {
        this.send(snapshot, choice);
      }
    } else {
      Messages.send("report.expired");
    }
  }

  // Wartezeit und Doppelmeldungen; bei Sperre kommt ein Chat-Hinweis
  private boolean allowed(String hash) {
    if (this.reported.contains(hash)) {
      Messages.send("report.duplicate");
      return false;
    }
    long wait = COOLDOWN_MILLIS - (System.currentTimeMillis() - this.lastSent);
    if (wait > 0) {
      Messages.send("report.cooldown", (wait + 999) / 1000);
      return false;
    }
    return true;
  }

  private void send(Snapshot snapshot, String kind) {
    this.lastSent = System.currentTimeMillis();
    this.reported.add(snapshot.hash());

    String addonVersion = this.addon.addonInfo().getVersion();
    String minecraftVersion = Laby.labyAPI().minecraft().getVersion();
    JsonObject machine = snapshot.machine();
    machine.addProperty("kind", kind.equals("echt") ? "falsePositive" : "missed");
    machine.addProperty("addon", addonVersion);
    machine.addProperty("mc", minecraftVersion);
    JsonObject body = ReportPayload.build(
        kind.equals("echt") ? "Echtes Item als Fake markiert" : "Fake nicht erkannt", machine,
        snapshot.itemName(), snapshot.lore(), snapshot.nbt(), snapshot.verdict(), addonVersion,
        minecraftVersion, Laby.labyAPI().getName(), Instant.now());
    HttpRequest request = HttpRequest.newBuilder(URI.create(BuildConfig.WEBHOOK_URL))
        .timeout(Duration.ofSeconds(15))
        .header("Content-Type", "application/json")
        .POST(BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
        .build();

    // Asynchron: der Render-/Tick-Thread wartet nie auf das Netzwerk
    HTTP.sendAsync(request, BodyHandlers.discarding()).whenComplete((response, error) -> {
      boolean ok = error == null && response.statusCode() / 100 == 2;
      if (!ok) {
        synchronized (this) {
          this.lastSent = 0;
          this.reported.remove(snapshot.hash());
        }
      }
      String reason = error != null ? error.getClass().getSimpleName()
          : "HTTP " + response.statusCode();
      Laby.labyAPI().minecraft().executeOnRenderThread(() ->
          Messages.send(ok ? "report.sent" : "report.failed", reason));
    });
  }

  private static Component button(String key, String command, TextColor color) {
    return Component.text("[" + Messages.text(key) + "]", color)
        .clickEvent(ClickEvent.runCommand(command));
  }

  private static Snapshot snapshot(Hovered hovered) {
    LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    List<String> lore = new ArrayList<>();
    for (Component line : hovered.rawLines()) {
      lore.add(serializer.serialize(line));
    }
    String itemName = lore.isEmpty() ? "" : lore.get(0);
    String verdict = hovered.verdict().isEmpty() ? "keine Warnung"
        : String.join(" | ", hovered.verdict());
    return new Snapshot(UUID.randomUUID().toString().substring(0, 8), hash(lore),
        System.currentTimeMillis(), itemName, lore, nbt(hovered), verdict, machine(hovered));
  }

  // Kompakte, maschinenlesbare Zusammenfassung: Fingerabdruck (wie in der Sammeldatei),
  // Urteil als Kürzel und die Lage von Kennung und Signatur
  private static JsonObject machine(Hovered hovered) {
    String material = hovered.itemStack().getIdentifier().getNamespace() + ':'
        + hovered.itemStack().getIdentifier().getPath();
    ItemFingerprint fingerprint = ItemFingerprint.of(hovered.lines(), material, hovered.signer(),
        hovered.signature());
    JsonObject json = new JsonObject();
    json.addProperty("v", 1);
    json.addProperty("name", fingerprint.name());
    json.addProperty("material", material);
    json.addProperty("signer", fingerprint.signer());
    json.addProperty("signature", fingerprint.verdict());
    json.addProperty("codeAboveSignature", ItemFingerprint.codeAboveSignature(hovered.lines()));
    JsonArray description = new JsonArray();
    fingerprint.description().stream().limit(12).forEach(description::add);
    json.add("description", description);
    JsonArray codes = new JsonArray();
    hovered.codes().forEach(codes::add);
    json.add("codes", codes);
    return json;
  }

  private static String nbt(Hovered hovered) {
    try {
      if (hovered.itemStack().hasNBTTag()) {
        NBTTagCompound tag = hovered.itemStack().getNBTTag();
        return String.valueOf(tag);
      }
      return "keine NBT-Daten";
    } catch (RuntimeException exception) {
      return "nicht lesbar auf dieser Minecraft-Version";
    }
  }

  private static String hash(List<String> lore) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(String.join("\n", lore).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private record Snapshot(String id, String hash, long createdAt, String itemName,
                          List<String> lore, String nbt, String verdict, JsonObject machine) {
  }
}
