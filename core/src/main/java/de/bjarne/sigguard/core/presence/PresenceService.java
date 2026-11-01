package de.bjarne.sigguard.core.presence;

import com.google.gson.JsonObject;
import de.bjarne.sigguard.core.SigGuardAddon;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.labymod.api.Laby;
import net.labymod.api.client.network.server.ServerData;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.lifecycle.GameTickEvent;
import net.labymod.api.event.labymod.labyconnect.session.LabyConnectBroadcastEvent;
import net.labymod.api.event.labymod.labyconnect.session.LabyConnectBroadcastEvent.Action;
import net.labymod.api.labyconnect.LabyConnect;

/**
 * Erkennt andere Addon-Nutzer in der Nähe: Alle paar Sekunden sendet das Addon über LabyConnect
 * die Addon-Version, empfangene Meldungen merken sich den Absender. Es werden keine eigenen
 * Server genutzt, und ohne die Einstellung "Andere Addon-Nutzer markieren" wird nichts gesendet.
 */
public class PresenceService {

  private static final String KEY = "sigguard_presence";
  private static final String SERVER_HOST_PART = "griefergames";
  private static final long SEND_INTERVAL_MILLIS = 20_000;
  private static final long EXPIRE_MILLIS = 70_000;

  private final SigGuardAddon addon;
  private final Map<UUID, Long> seen = new ConcurrentHashMap<>();
  private long lastSent;

  public PresenceService(SigGuardAddon addon) {
    this.addon = addon;
  }

  /** {@code true}, wenn der Spieler vor Kurzem eine Addon-Meldung geschickt hat. */
  public boolean isAddonUser(UUID uuid) {
    if (!this.active()) {
      return false;
    }
    Long last = this.seen.get(uuid);
    if (last == null) {
      return false;
    }
    if (System.currentTimeMillis() - last > EXPIRE_MILLIS) {
      this.seen.remove(uuid);
      return false;
    }
    return true;
  }

  @Subscribe
  public void onTick(GameTickEvent event) {
    if (event.phase() != Phase.POST) {
      return;
    }
    if (!this.active()) {
      this.seen.clear();
      return;
    }

    long now = System.currentTimeMillis();
    if (now - this.lastSent < SEND_INTERVAL_MILLIS || !this.onGrieferGames()) {
      return;
    }
    this.lastSent = now;

    try {
      LabyConnect labyConnect = Laby.labyAPI().labyConnect();
      if (labyConnect == null || !labyConnect.isConnectionEstablished()) {
        return;
      }
      JsonObject payload = new JsonObject();
      payload.addProperty("v", this.addon.addonInfo().getVersion());
      labyConnect.getSession().sendSurroundingBroadcastPayload(KEY, payload);
    } catch (RuntimeException ignored) {
      // LabyConnect nicht erreichbar: das Feature bleibt still, ohne Fehlermeldung im Chat
    }
  }

  @Subscribe
  public void onBroadcast(LabyConnectBroadcastEvent event) {
    if (event.action() != Action.RECEIVE || !KEY.equals(event.getKey()) || !this.active()) {
      return;
    }
    UUID sender = event.getSender();
    if (sender != null && !sender.equals(Laby.labyAPI().getUniqueId())) {
      this.seen.put(sender, System.currentTimeMillis());
    }
  }

  private boolean active() {
    return this.addon.configuration().enabled().get()
        && this.addon.configuration().playerDetection().get();
  }

  private boolean onGrieferGames() {
    ServerData server = Laby.labyAPI().serverController().getCurrentServerData();
    return server != null && server.address() != null
        && server.address().getHost().toLowerCase().contains(SERVER_HOST_PART);
  }
}
