// Ausführen: node --test report-proxy/worker.test.mjs
import assert from "node:assert/strict";
import { test } from "node:test";
import worker, { sanitize } from "./worker.js";

const valid = () => ({
  username: "egal",
  avatar_url: "https://evil.example/a.png",
  content: "@everyone",
  embeds: [{
    title: "Fehlermeldung: Fake nicht erkannt",
    description: "```\nLore\n```",
    url: "https://evil.example",
    fields: [{ name: "Melder", value: "Spieler", inline: true }],
  }],
});

test("übernimmt nur erlaubte Felder", () => {
  const embed = sanitize(valid());
  assert.deepEqual(Object.keys(embed).sort(), ["description", "fields", "timestamp", "title"]);
  assert.deepEqual(embed.fields, [{ name: "Melder", value: "Spieler", inline: true }]);
});

test("lehnt kaputte oder zu große Meldungen ab", () => {
  assert.equal(sanitize(null), null);
  assert.equal(sanitize({ embeds: [] }), null);
  const twoEmbeds = valid();
  twoEmbeds.embeds.push(twoEmbeds.embeds[0]);
  assert.equal(sanitize(twoEmbeds), null);
  const longValue = valid();
  longValue.embeds[0].fields[0].value = "x".repeat(1025);
  assert.equal(sanitize(longValue), null);
  const tooMany = valid();
  tooMany.embeds[0].fields = Array(11).fill({ name: "a", value: "b" });
  assert.equal(sanitize(tooMany), null);
});

test("sendet ohne Pings mit festem Namen und drosselt", async () => {
  let sent;
  globalThis.fetch = async (url, init) => {
    sent = { url, body: JSON.parse(init.body) };
    return new Response(null, { status: 204 });
  };
  let allow = true;
  const env = { WEBHOOK_URL: "https://discord.test/hook", REPORT_LIMIT: { limit: async () => ({ success: allow }) } };
  const request = () => new Request("https://proxy.test/report", { method: "POST", body: JSON.stringify(valid()) });

  assert.equal((await worker.fetch(request(), env)).status, 204);
  assert.equal(sent.url, "https://discord.test/hook");
  assert.equal(sent.body.username, "AH-Fake-Warner");
  assert.deepEqual(sent.body.allowed_mentions, { parse: [] });
  assert.equal(sent.body.content, undefined);
  assert.equal(sent.body.avatar_url, undefined);

  allow = false;
  assert.equal((await worker.fetch(request(), env)).status, 429);
  assert.equal((await worker.fetch(new Request("https://proxy.test/x"), env)).status, 404);
});
