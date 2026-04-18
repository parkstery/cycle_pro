import {onRequest} from "firebase-functions/v2/https";
import {defineSecret} from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";

admin.initializeApp();

/** FCM topic name allowed characters (conservative subset). */
const TOPIC_PATTERN = /^[a-zA-Z0-9_.~-]+$/;

type PushBody = {
  topic?: unknown;
  title?: unknown;
  body?: unknown;
  data?: unknown;
};

const RTW_PUSH_DEV_KEY = defineSecret("RTW_PUSH_DEV_KEY");

function isDevKeyAuthorized(
  req: { get(name: string): string | undefined },
  configuredDevKey: string
): boolean {
  if (!configuredDevKey) return false;
  const providedDevKey = req.get("X-RTW-Dev-Key")?.trim() ?? "";
  return providedDevKey.length > 0 && providedDevKey === configuredDevKey;
}

function normalizeData(raw: unknown): Record<string, string> | undefined {
  if (raw == null || typeof raw !== "object" || Array.isArray(raw)) {
    return undefined;
  }
  const out: Record<string, string> = {};
  for (const [k, v] of Object.entries(raw as Record<string, unknown>)) {
    if (typeof k !== "string" || k.length === 0 || k.length > 256) continue;
    out[k] = typeof v === "string" ? v : JSON.stringify(v);
  }
  return Object.keys(out).length ? out : undefined;
}

export const sendTopicPush = onRequest(
  {
    region: "asia-northeast3",
    cors: false,
    maxInstances: 10,
    invoker: "public",
    secrets: [RTW_PUSH_DEV_KEY],
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ok: false, errorCode: "METHOD_NOT_ALLOWED"});
      return;
    }

    const authHeader = req.get("Authorization") ?? "";
    const bearer = /^Bearer\s+(.+)$/i.exec(authHeader);
    const hasDevKeyAuth = isDevKeyAuthorized(req, RTW_PUSH_DEV_KEY.value().trim());
    if (!bearer?.[1] && !hasDevKeyAuth) {
      res.status(401).json({ok: false, errorCode: "UNAUTHORIZED"});
      return;
    }

    if (bearer?.[1]) {
      try {
        await admin.auth().verifyIdToken(bearer[1]);
      } catch (e) {
        logger.warn("verifyIdToken failed", e);
        if (!hasDevKeyAuth) {
          res.status(401).json({ok: false, errorCode: "INVALID_TOKEN"});
          return;
        }
      }
    }

    const b = req.body as PushBody;
    const topic = typeof b.topic === "string" ? b.topic : "";
    const title = typeof b.title === "string" ? b.title : "";
    const bodyText = typeof b.body === "string" ? b.body : "";
    if (!topic || !title || !bodyText) {
      res.status(400).json({ok: false, errorCode: "INVALID_BODY"});
      return;
    }
    if (!TOPIC_PATTERN.test(topic)) {
      res.status(400).json({ok: false, errorCode: "INVALID_TOPIC"});
      return;
    }

    const data = normalizeData(b.data);

    try {
      await admin.messaging().send({
        topic,
        notification: {title, body: bodyText},
        data,
      });
      res.status(200).json({ok: true});
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : String(e);
      logger.error("messaging.send failed", e);
      res.status(500).json({ok: false, errorCode: "SEND_FAILED", message});
    }
  }
);
