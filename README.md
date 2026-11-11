# Ascend Backend

Spring Boot 3 + PostgreSQL API for the Ascend task manager: targets, tasks, journal, placement tracker,
and the **repeating reminders** (first notification at the task's time, then every N minutes until it is ticked
or the window ends).

> **Status:** written to match the React frontend's data model, but it was **not compiled or tested in the
> environment it was written in** (Maven Central was blocked there). Run `mvn test` first. The tests cover
> the reminder rule, recurrence, auth, ownership, and the localStorage import. If something fails, fix that
> before deploying.

## Stack

Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, PostgreSQL, Web Push (VAPID), Telegram Bot API.

## Run locally (STS / terminal)

1. Create a database: `createdb ascend` (or any Postgres you have).
2. Set the variables from `.env.example` (STS: *Run Configurations → Environment*). At minimum `DATABASE_URL`,
   `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.
3. `mvn spring-boot:run`, or run `AscendApplication` from STS. Tables are created automatically on first start.
4. Open `requests.http` in VS Code (REST Client extension) and try Register → Login → create a task.

```bash
mvn test                       # unit + integration tests (in-memory H2, no Postgres needed)
mvn package -DskipTests        # builds target/ascend-backend.jar
```

## How the reminders work

`ReminderScheduler` wakes every 15 seconds (`REMINDER_TICK_MS`). For each user, `ReminderProcessor` looks at
today's tasks **in the user's time zone** (default `Asia/Kolkata`, change it in settings):

- a task is *ringing* from its time until time + `windowMin` (default 30);
- while ringing and not ticked, send immediately, then again every `intervalMin` (default 5);
- ticking the task (from the app **or the Telegram button**) stops it at once;
- the count of reminders sent is stored in `reminder_logs`, so a restart doesn't re-send or lose track.

Each reminder goes to every channel you have set up:

| Channel | Works when the app is closed? | Setup |
| --- | --- | --- |
| Browser push (Web Push) | Yes (iPhone: only after *Add to Home Screen*, iOS 16.4+) | VAPID keys + subscribe from the frontend |
| Telegram | Yes, and it has a **✅ Done** button that ticks the task | Create a bot, set 3 env vars, link from the app |

### VAPID keys (once)

```bash
npx web-push generate-vapid-keys
```
Put them in `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY`. `GET /api/push/config` then returns the public key.

### Telegram (optional, recommended as a backup)

1. In Telegram, talk to **@BotFather** → `/newbot` → copy the token and the bot's username.
2. Set `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`, `TELEGRAM_WEBHOOK_SECRET` (a long random string of letters and digits only, since it becomes part of a URL) and
   `PUBLIC_URL` (your deployed backend URL). On startup the backend registers its webhook with Telegram.
3. In the web app: **Settings → Telegram reminders → Connect**, tap *Open Telegram*, press *Start*. (API: `POST /api/telegram/link-code`.)

   Telegram can only call a **public https** address, so the webhook cannot work on `localhost`. Test linking on your deployed Render backend, or locally through a tunnel such as ngrok (set `PUBLIC_URL` to the tunnel address).
4. `POST /api/notifications/test` sends a test message to every channel.

## Deploy on Render

1. Push this folder to GitHub (its own repo, or a `backend/` folder).
2. Render → New → Web Service → Docker (or New → Blueprint and use `render.yaml`).
3. Set the environment variables from `.env.example`. `CORS_ORIGINS` must be your Vercel URL, and `PUBLIC_URL`
   your Render URL.

**Database:** do *not* use Render's free PostgreSQL. It expires 30 days after creation (14-day grace, then it is deleted)
[per Render's docs](https://render.com/docs/free), which would wipe your journal. Use a free Postgres from another provider
(for example Neon or Supabase; check their current limits) and paste its connection string into `DATABASE_URL`.
Both `jdbc:postgresql://…` and `postgres://user:pass@host/db` forms work.

**Keep it awake:** Render's free web service spins down after 15 minutes without traffic, and the scheduler sleeps with it.
Create a free monitor on UptimeRobot or cron-job.org that requests `GET https://<your-service>/api/health` every 5-10 minutes.
Telegram button taps also wake it.

**After you create your account** set `ALLOW_REGISTRATION=false`, so nobody else can sign up on your public API.

Run **one instance** only; with two, every reminder is sent twice.

## API

Everything except `/api/auth/*`, `/api/health` and the Telegram webhook needs `Authorization: Bearer <token>`.
JSON shapes match what the React app keeps in localStorage (empty string = none, `repeat` is lower-case, dates are `yyyy-MM-dd`).

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register`, `/api/auth/login` | `{email, password, name?}` → `{token, email, name}` |
| GET / POST | `/api/tasks` | list / create `{title, date, time?, category?, repeat?, goalId?, notes?}` |
| PUT / DELETE | `/api/tasks/{id}` | edit / delete |
| POST | `/api/tasks/{id}/toggle` | tick or untick one day: `{date?}` (default today). Moves a linked monthly target by ±1 |
| GET / POST | `/api/goals` | `{type: FINAL\|MONTHLY\|DAILY, title, target, progress?, unit?, month?, deadline?, parentId?}` |
| PUT / DELETE | `/api/goals/{id}` | edit / delete (linked tasks and child targets are kept, just unlinked) |
| POST | `/api/goals/{id}/log` | daily targets: `{date?, delta}` |
| GET | `/api/journal` | all entries keyed by date |
| GET / PUT / DELETE | `/api/journal/{date}` | `{mood?, text?, learned?, plan?}`; only the fields you send are changed |
| GET / POST | `/api/applications`, PUT / DELETE `/api/applications/{id}` | placement tracker; status is one of Wishlist, Applied, Online Test, Interview, Offer, Rejected |
| GET / POST | `/api/focus` | minutes per day / add a finished session `{minutes, date?}` |
| GET / PUT | `/api/settings` | name, theme, windowMin, intervalMin, sound, notifications, streakThreshold, timezone |
| GET / PUT | `/api/state` | the whole account in one object, including `timetable` and `diet` (stored as JSON) and a `revision`. This is what the frontend syncs through |
| GET | `/api/push/config` | `{enabled, publicKey}` |
| POST / DELETE | `/api/push/subscribe` | save / remove a browser's `PushSubscription` (send `JSON.stringify(subscription)`) |
| POST | `/api/notifications/test` | send a test through every channel |
| POST / DELETE | `/api/telegram/link-code`, `/api/telegram/link` | link / unlink Telegram |
| GET | `/api/health` | liveness check for the uptime pinger |

Errors are `{ "error": "…" }` (validation errors add `"fields": { "title": "…" }`).

## Moving your data from the browser to the database

In the web app: **Settings → Export**, then:

```bash
curl -X PUT https://<backend>/api/state \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  --data-binary @ascend-backup-2026-11-10.json
```
Ids from the browser (`"g1"`) are re-mapped to real UUIDs and the links between tasks and targets are kept.

## How the frontend syncs

The React app keeps a working copy in localStorage and syncs the whole account through `/api/state`:

- `GET /api/state` returns everything plus `revision` (0 for a brand-new account).
- `PUT /api/state` replaces everything. Send the `revision` your copy was based on. If it is not the server's current revision the answer is **409**, so an old device can never overwrite newer data. Leave `revision` out to overwrite unconditionally.
- Ids that are UUIDs are kept (so reminder history survives a sync). An id that already belongs to another account is replaced with a new one. Non-UUID ids are re-mapped.
- `timetable` and `diet` are stored as JSON in the `user_data` table (limits: 500 KB and 2 MB).

Because tasks are saved on the server, the backend's reminder scheduler (Web Push and Telegram) works from them.

Other things to do when wiring the rest:

- Turn on push: fetch `/api/push/config`, then

```js
const reg = await navigator.serviceWorker.ready;
const sub = await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(publicKey) });
await api("/api/push/subscribe", { method: "POST", body: JSON.stringify(sub) });
```
  `public/sw.js` in the frontend already handles the `push` event.
- Once push or Telegram is on, disable the frontend's in-browser `ReminderEngine`, otherwise you get each reminder twice.

## Project layout

```
config/    properties, DataSource (accepts postgres:// URLs), security + CORS
security/  JWT issue/verify and the request filter
domain/    JPA entities       repo/  Spring Data repositories
dto/       request/response shapes (match the React app)
service/   business rules (tick = move linked target, ownership checks, import/export)
reminder/  recurrence rules, the reminder rule, the scheduler
notify/    Web Push, Telegram client + webhook, link codes, fan-out
web/       REST controllers and error handling
```

## Known limits

- Tables are managed by `ddl-auto=update`. Move to Flyway migrations once the schema settles.
- No login rate limiting. Keep `ALLOW_REGISTRATION=false` and use a strong password.
- Single-instance scheduler (no distributed lock).
- A task whose reminder window crosses midnight (e.g. 23:50 + 30 min) stops at midnight.
