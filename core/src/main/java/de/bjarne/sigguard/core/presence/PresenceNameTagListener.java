package de.bjarne.sigguard.core.presence;

import de.bjarne.sigguard.core.VersionCompat;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.render.PlayerNameTagRenderEvent;

/**
 * Hängt bei anderen Addon-Nutzern ein Zeichen an den Namen, in der Tabliste und über dem Kopf.
 */
public class PresenceNameTagListener {

  private final PresenceService presence;

  public PresenceNameTagListener(PresenceService presence) {
    this.presence = presence;
  }

  @Subscribe
  public void onNameTag(PlayerNameTagRenderEvent event) {
    if (event.playerInfo() == null || event.nameTag() == null) {
      return;
    }
    if (this.presence.isAddonUser(event.playerInfo().profile().getUniqueId())) {
      event.setNameTag(event.nameTag().copy()
          .append(Component.text(" " + VersionCompat.USER_MARK, NamedTextColor.GREEN)));
    }
  }
}
