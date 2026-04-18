# CyclePro Firebase Functions

## Purpose
- Provide a backend endpoint for topic push delivery.
- Keep FCM Admin credentials on the server side only.

## Endpoint
- Function: `sendTopicPush`
- Region: `asia-northeast3`
- Method: `POST`
- URL form: `https://asia-northeast3-<project-id>.cloudfunctions.net/sendTopicPush`

## Request
- Headers
  - `Content-Type: application/json`
  - One of:
    - `Authorization: Bearer <firebase-id-token>` (production path)
    - `X-RTW-Dev-Key: <dev-key>` (temporary test path)
- Body
```json
{
  "topic": "daily-20h",
  "title": "RTW Pro",
  "body": "Reminder",
  "data": {
    "type": "daily-20h"
  }
}
```

## Response
- Success: `{"ok": true}`
- Fail examples:
  - `{"ok": false, "errorCode": "UNAUTHORIZED"}`
  - `{"ok": false, "errorCode": "INVALID_TOKEN"}`
  - `{"ok": false, "errorCode": "INVALID_BODY"}`

## Setup
1. Install dependencies
   - `cd functions`
   - `npm install`
2. Set Firebase project
   - Update `.firebaserc` default project id
3. Set secret for dev-key testing
   - `firebase functions:secrets:set RTW_PUSH_DEV_KEY`
4. Build and deploy
   - `npm run build`
   - `firebase deploy --only functions`

## Curl test with dev key
```bash
curl -X POST "https://<your-function-url>" \
  -H "Content-Type: application/json" \
  -H "X-RTW-Dev-Key: <your-dev-key>" \
  -d "{\"topic\":\"test\",\"title\":\"Test\",\"body\":\"Push check\"}"
```
