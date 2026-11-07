package de.bjarne.sigguard.core.listener;

import de.bjarne.sigguard.core.Messages;
import de.bjarne.sigguard.core.SigGuardAddon;
import de.bjarne.sigguard.core.SigGuardConfiguration;
import de.bjarne.sigguard.core.SigGuardConfiguration.PanelSide;
import de.bjarne.sigguard.core.reminder.ReminderManager.Reminder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.TextColor;
import net.labymod.api.client.component.serializer.plain.PlainTextComponentSerializer;
import net.labymod.api.client.gui.screen.ScreenContext;
import net.labymod.api.client.gui.screen.container.ContainerScreenView;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.state.ScreenCanvas;
import net.labymod.api.client.gui.window.Window;
import net.labymod.api.client.render.font.ComponentRenderer;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.gui.screen.ContainerScreenRenderEvent;
import net.labymod.api.event.client.gui.screen.ContainerScreenRenderEvent.Layer;
import net.labymod.api.util.math.vector.FloatVector3;

/**
 * Info-Panel neben dem Auktionshaus-Fenster: Tasten, Farblegende, Befehle und Status.
 * Gezeichnet in der Ebene SLOTS, also vor den Item-Tooltips, die dadurch über dem Panel liegen.
 */
public class InfoPanelListener {

  private static final float OUTER_GAP = 4F;
  private static final float PADDING = 4F;
  private static final float COLUMN_GAP = 8F;
  private static final float SWATCH = 8F;
  private static final float SWATCH_GAP = 4F;
  private static final float MAX_WIDTH = 230F;
  private static final float SECTION_GAP = 3F;
  private static final long REFRESH_MILLIS = 1_000;
  private static final int BACKGROUND = 0xC0_10_10_10;
  private static final int BORDER = 0xFF_40_40_40;
  private static final int GREEN = 0xFF_55_FF_55;

  private final SigGuardAddon addon;
  private Layout layout;
  private long builtAt;

  public InfoPanelListener(SigGuardAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onContainer(ContainerScreenRenderEvent event) {
    SigGuardConfiguration config = this.addon.configuration();
    if (event.layer() != Layer.SLOTS || !config.enabled().get() || !config.panelEnabled().get()
        || !isAuctionHouse(event.view())) {
      return;
    }

    // Texte und Status nur einmal pro Sekunde neu aufbauen
    long now = System.currentTimeMillis();
    if (this.layout == null || now - this.builtAt >= REFRESH_MILLIS) {
      this.layout = this.build(config);
      this.builtAt = now;
    }
    Layout panel = this.layout;
    if (panel.rows.isEmpty()) {
      return;
    }

    ContainerScreenView view = event.view();
    Window window = Laby.labyAPI().minecraft().minecraftWindow();
    float x = place(view, panel.width, config.panelSide().get(), window.getScaledWidth());
    if (x < 0 || view.top() + panel.height > window.getScaledHeight()) {
      return;
    }
    this.draw(event.screenContext(), x, view.top(), panel);
  }

  private static boolean isAuctionHouse(ContainerScreenView view) {
    String title = PlainTextComponentSerializer.plainText().serialize(view.title());
    // contains statt startsWith: GG stellt dem Titel unsichtbare Zeichen voran
    return title.toLowerCase(Locale.ROOT).contains("auktionshaus");
  }

  /** Bevorzugte Seite, sonst die andere; -1, wenn das Panel auf keiner Seite Platz hat. */
  private static float place(ContainerScreenView view, float width, PanelSide preferred,
      float screenWidth) {
    float right = view.left() + view.width() + OUTER_GAP;
    float left = view.left() - OUTER_GAP - width;
    boolean rightFits = right + width <= screenWidth;
    boolean leftFits = left >= 0;
    if (preferred == PanelSide.RIGHT) {
      return rightFits ? right : leftFits ? left : -1;
    }
    return leftFits ? left : rightFits ? right : -1;
  }

  private Layout build(SigGuardConfiguration config) {
    List<Row> rows = new ArrayList<>();
    if (config.panelKeys().get()) {
      rows.add(Row.header(text("panel.keys", NamedTextColor.GOLD)));
      rows.add(Row.pair(text("panel.keyReminder", NamedTextColor.GRAY),
          keyName(config.reminderKey().get())));
      rows.add(Row.pair(text("panel.keyReport", NamedTextColor.GRAY),
          keyName(config.reportKey().get())));
    }
    if (config.panelLegend().get()) {
      rows.add(Row.header(text("panel.legend", NamedTextColor.GOLD)));
      rows.add(Row.swatch(Swatch.FRAME, SlotFrameListener.RED, 0, text("panel.legendFake")));
      rows.add(Row.swatch(Swatch.FRAME, SlotFrameListener.ORANGE, 0, text("panel.legendEgg")));
      rows.add(Row.swatch(Swatch.FILL, GREEN, 0, text("panel.legendReal")));
      int timer = SlotHighlightListener.argb(config.reminderColor().get());
      int bid = SlotHighlightListener.argb(config.bidColor().get());
      rows.add(Row.swatch(Swatch.FILL, timer, 0, text("panel.legendTimer")));
      rows.add(Row.swatch(Swatch.FILL, bid, 0, text("panel.legendBid")));
      rows.add(Row.swatch(Swatch.STRIPES, bid, timer, text("panel.legendBoth")));
    }
    if (config.panelCommands().get()) {
      rows.add(Row.header(text("panel.commands", NamedTextColor.GOLD)));
      rows.add(Row.pair(Component.text("/aht list", NamedTextColor.GRAY),
          Messages.text("panel.cmdList")));
      rows.add(Row.pair(Component.text("/aht clear", NamedTextColor.GRAY),
          Messages.text("panel.cmdClear")));
      rows.add(Row.pair(Component.text("/aht sammeln", NamedTextColor.GRAY),
          Messages.text("panel.cmdCollect")));
    }
    if (config.panelStatus().get()) {
      rows.add(Row.header(text("panel.status", NamedTextColor.GOLD)));
      rows.add(Row.pair(text("panel.statusReminders", NamedTextColor.GRAY), this.reminderStatus()));
      rows.add(Row.pair(text("panel.statusCollect", NamedTextColor.GRAY),
          Messages.text(this.addon.collection().enabled() ? "panel.on" : "panel.off")));
      rows.add(Row.pair(text("panel.statusVersion", NamedTextColor.GRAY),
          this.addon.addonInfo().getVersion()));
    }
    return layout(rows);
  }

  // "2 (nächste in 3:10 Min.)" bzw. "keine"
  private String reminderStatus() {
    List<Reminder> reminders = this.addon.reminders().reminders();
    if (reminders.isEmpty()) {
      return Messages.text("panel.none");
    }
    long next = Long.MAX_VALUE;
    for (Reminder reminder : reminders) {
      next = Math.min(next, reminder.remindAt());
    }
    long seconds = Math.max(0, (next - System.currentTimeMillis()) / 1000);
    String remaining = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    return reminders.size() + " " + Messages.text("panel.next", remaining);
  }

  private static String keyName(Key key) {
    return key == null || key == Key.NONE ? Messages.text("panel.unbound") : key.getName();
  }

  private static Component text(String key) {
    return text(key, NamedTextColor.WHITE);
  }

  private static Component text(String key, TextColor color) {
    return Component.text(Messages.text(key), color);
  }

  // Spaltenbreiten bestimmen, lange Texte umbrechen, Gesamtgröße berechnen
  private static Layout layout(List<Row> rows) {
    ComponentRenderer renderer = Laby.references().renderPipeline().componentRenderer();
    float leftWidth = 0;
    for (Row row : rows) {
      if (!row.header) {
        if (row.swatch == null) {
          leftWidth = Math.max(leftWidth, renderer.width(row.left));
        }
      }
    }
    float lineHeight = renderer.height() + 2F;

    float width = 0;
    float height = PADDING;
    List<Placed> placed = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      Row row = rows.get(i);
      List<Component> lines = new ArrayList<>();
      if (row.header) {
        width = Math.max(width, renderer.width(row.left));
        height += i == 0 ? 0 : SECTION_GAP;
        lines.add(row.left);
      } else {
        // Farbfelder stehen direkt vor ihrem Text, Tabellenzeilen nutzen die zweite Spalte
        float indent = row.swatch != null ? SWATCH + SWATCH_GAP : leftWidth + COLUMN_GAP;
        lines.addAll(renderer.split(row.right, Math.max(60F, MAX_WIDTH - indent)));
        for (Component line : lines) {
          width = Math.max(width, indent + renderer.width(line));
        }
      }
      placed.add(new Placed(row, lines, height));
      height += Math.max(1, lines.size()) * lineHeight;
    }
    return new Layout(placed, leftWidth, width + 2 * PADDING, height + PADDING - 2F, lineHeight);
  }

  private void draw(ScreenContext context, float x, float y, Layout panel) {
    // Zeichenbefehle gelten relativ zum aktuellen Ursprung; auf Bildschirmkoordinaten umrechnen
    FloatVector3 origin = context.stack().transformVector(0F, 0F, 0F);
    float px = x - origin.getX();
    float py = y - origin.getY();

    ScreenCanvas canvas = context.canvas();
    canvas.submitRelativeRect(px, py, panel.width, panel.height, BACKGROUND);
    canvas.submitRelativeOutlineRect(px, py, panel.width, panel.height, 1F, BORDER, BORDER);

    ComponentRenderer renderer = Laby.references().renderPipeline().componentRenderer();
    for (Placed entry : panel.rows) {
      float rowY = py + entry.top;
      Row row = entry.row;
      if (row.header) {
        text(renderer, context, row.left, px + PADDING, rowY);
        continue;
      }
      if (row.swatch != null) {
        drawSwatch(canvas, row, px + PADDING, rowY);
      } else {
        text(renderer, context, row.left, px + PADDING, rowY);
      }
      float rightX = px + PADDING
          + (row.swatch != null ? SWATCH + SWATCH_GAP : panel.leftWidth + COLUMN_GAP);
      for (int line = 0; line < entry.lines.size(); line++) {
        text(renderer, context, entry.lines.get(line), rightX, rowY + line * panel.lineHeight);
      }
    }
  }

  private static void text(ComponentRenderer renderer, ScreenContext context, Component text,
      float x, float y) {
    renderer.builder().text(text).pos(x, y).allowColors(true).shadow(true).render(context);
  }

  // Farbfeld in denselben Farben und Mustern wie im Auktionshaus
  private static void drawSwatch(ScreenCanvas canvas, Row row, float x, float y) {
    int size = (int) SWATCH;
    switch (row.swatch) {
      case FRAME -> canvas.submitRelativeOutlineRect(x, y, SWATCH, SWATCH,
          SlotFrameListener.FRAME_WIDTH, row.first, row.first);
      case FILL -> canvas.submitRelativeRect(x, y, SWATCH, SWATCH, row.first);
      case STRIPES -> SlotHighlightListener.drawStripes(canvas, x, y, size, row.first, row.second);
    }
  }

  private enum Swatch {
    FILL,
    FRAME,
    STRIPES
  }

  private record Row(boolean header, Component left, Component right, Swatch swatch, int first,
                     int second) {

    static Row header(Component title) {
      return new Row(true, title, null, null, 0, 0);
    }

    static Row pair(Component label, String value) {
      return new Row(false, label, Component.text(value, NamedTextColor.WHITE), null, 0, 0);
    }

    static Row swatch(Swatch swatch, int first, int second, Component meaning) {
      return new Row(false, null, meaning, swatch, first, second);
    }
  }

  private record Placed(Row row, List<Component> lines, float top) {
  }

  private record Layout(List<Placed> rows, float leftWidth, float width, float height,
                        float lineHeight) {
  }
}
