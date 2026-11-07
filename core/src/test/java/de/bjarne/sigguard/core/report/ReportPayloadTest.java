package de.bjarne.sigguard.core.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportPayloadTest {

  @Test
  void shortTextIsKept() {
    assertEquals("abc", ReportPayload.cut("abc", 10));
  }

  @Test
  void longTextIsCutAndMarked() {
    String cut = ReportPayload.cut("x".repeat(5000), ReportPayload.DESCRIPTION_LIMIT);
    assertEquals(ReportPayload.DESCRIPTION_LIMIT, cut.length());
    assertTrue(cut.endsWith("[gekürzt]"));
  }

  @Test
  void cutCodeBlockStaysClosed() {
    String cut = ReportPayload.cut("```\n" + "x".repeat(2000) + "\n```", ReportPayload.FIELD_LIMIT);
    assertTrue(cut.length() <= ReportPayload.FIELD_LIMIT);
    assertTrue(cut.endsWith("```"));
  }

  @Test
  void embedContainsAllInformationAndRespectsLimits() {
    JsonObject machine = new JsonObject();
    machine.addProperty("kind", "missed");
    JsonObject body = ReportPayload.build("Fake nicht erkannt", machine, "&4Name",
        List.of("&4Name", "Zeile"), "x".repeat(3000), "keine Warnung", "1.1.5", "26.3", "Spieler",
        Instant.EPOCH);
    JsonObject embed = body.getAsJsonArray("embeds").get(0).getAsJsonObject();
    assertTrue(embed.get("description").getAsString().startsWith("```json\n{\"kind\":\"missed\"}"));
    assertTrue(embed.get("description").getAsString().length() <= ReportPayload.DESCRIPTION_LIMIT);
    assertEquals("Fehlermeldung: Fake nicht erkannt", embed.get("title").getAsString());
    assertEquals("1970-01-01T00:00:00Z", embed.get("timestamp").getAsString());
    embed.getAsJsonArray("fields").forEach(field ->
        assertTrue(field.getAsJsonObject().get("value").getAsString().length()
            <= ReportPayload.FIELD_LIMIT));
    assertEquals(6, embed.getAsJsonArray("fields").size());
  }
}
