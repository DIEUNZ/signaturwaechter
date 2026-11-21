// Cloudflare Worker für "Fehler melden": Nur dieser Worker kennt die Discord-Webhook-URL
// (Secret WEBHOOK_URL). Das Addon schickt seine Meldung hierher; der Worker begrenzt pro IP,
// übernimmt nur erlaubte Embed-Felder und sendet ohne Pings an Discord weiter.

const MAX_BODY = 16_384;
const MAX_FIELDS = 10;

export default {
  async fetch(request, env) {
    if (request.method !== "POST" || new URL(request.url).pathname !== "/report") {
      return new Response("Not found", { status: 404 });
    }
    const ip = request.headers.get("CF-Connecting-IP") ?? "unknown";
    const { success } = await env.REPORT_LIMIT.limit({ key: ip });
    if (!success) {
      return new Response("Too many reports", { status: 429 });
    }

    const text = await request.text();
    if (text.length > MAX_BODY) {
      return new Response("Too large", { status: 413 });
    }
    let body;
    try {
      body = JSON.parse(text);
    } catch {
      return new Response("Bad request", { status: 400 });
    }
    const embed = sanitize(body);
    if (!embed) {
      return new Response("Bad request", { status: 400 });
    }

    const response = await fetch(env.WEBHOOK_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: "AH-Fake-Warner",
        allowed_mentions: { parse: [] },
        embeds: [embed],
      }),
    });
    return new Response(null, { status: response.ok ? 204 : 502 });
  },
};

/** Baut aus der Meldung ein Embed nur aus Titel, Beschreibung und Textfeldern; sonst null. */
export function sanitize(body) {
  const source = Array.isArray(body?.embeds) && body.embeds.length === 1 ? body.embeds[0] : null;
  if (!isText(source?.title, 256) || !isText(source?.description, 4096)) {
    return null;
  }
  const fields = Array.isArray(source.fields) ? source.fields : [];
  if (fields.length > MAX_FIELDS
      || !fields.every((f) => isText(f?.name, 256) && isText(f?.value, 1024))) {
    return null;
  }
  return {
    title: source.title,
    description: source.description,
    timestamp: new Date().toISOString(),
    fields: fields.map((f) => ({ name: f.name, value: f.value, inline: f.inline === true })),
  };
}

function isText(value, max) {
  return typeof value === "string" && value.length > 0 && value.length <= max;
}
