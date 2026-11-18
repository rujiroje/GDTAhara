# T-03 · Tech Screen · Realtime Help Request Notification

**Role:** Technician
**Priority:** 🔥 Critical
**Target:** Phase 1 (before M7 Mass Rollout · Jan 2027)
**Effort:** M-L (Medium-Large · ~5-7 dev days)
**Source:** Pilot Feedback follow-up (post 8 Sep 2026)
**Related:** O-10 (Op-side · Telegram alert + OTP arrival confirmation)

---

## 📋 Summary · สรุป

ปัจจุบัน · เมื่อ Operator กดปุ่ม "ต้องการความช่วยเหลือ" (call Tech/Leader) · Tech ไม่เห็นการแจ้งเตือนในหน้าจอ MES · ต้องพึ่ง Telegram อย่างเดียว · ทำให้ Tech อาจพลาดถ้าไม่ได้เปิด phone

**Feature นี้:** เพิ่ม realtime in-app notification บน Tech tablet เมื่อมี help request

---

## 🎯 User Story

**As a Technician,**
เมื่อมี Operator กดขอความช่วยเหลือ,
I want to:
- เห็น notification popup ปรากฏบนหน้าจอ MES ทันที (realtime)
- ได้ยินเสียง alert (ปิดไม่ได้ใน work hours)
- เห็นข้อมูล: เครื่องไหน · ใครขอ · ปัญหาอะไร · เวลาที่ขอ
- กด Accept ยืนยันรับเรื่อง · หรือ Snooze/Decline
- หลัง Accept → ระบบเปิด map/direction ไปหาเครื่อง + link ไป O-10 (OTP arrival)

**So that:**
- ไม่พลาด help request · แม้ไม่เปิด phone
- Response time ลดลง · ผลิตต่อเร็วขึ้น
- Log response time · KPI Tech measurable

---

## 📸 Current State

**Screen: Tech Dashboard** (ตามหน้า Setup ใน T-01 · same login)

**ปัจจุบัน:** ไม่มี notification area · ไม่มี help request queue · Tech ไม่รู้ว่ามีคนเรียก unless เปิด Telegram

**Missing components:**
- ❌ Realtime WebSocket/SSE connection
- ❌ Notification bell icon + badge count
- ❌ Alert popup modal
- ❌ Sound alert
- ❌ Help request queue/history view

---

## 🎨 UI/UX Requirements

### 1️⃣ Notification Bell + Badge (Global · always visible)

**Location:** Top-right corner ของ header (ข้าง Login user info)

**UI:**
```
GDTahara                           สวัสดี Tech1  🔔③  (Technician) TH EN [ออกจากระบบ]
                                              ↑
                                     badge = pending count
```

**Spec:**
- Icon: 🔔 (bell) · size 24px
- Badge: red circle top-right · white number
- Colors: 
  - No request: gray bell
  - Pending: red bell + badge
  - Accepted (in progress): orange bell
- Click bell → open Help Request Queue panel

---

### 2️⃣ Help Request Alert Popup (Full-screen modal)

**Trigger:** Realtime WebSocket message arrives

**UI Design:**
```
┌─ 🚨 HELP REQUEST · 3 นาทีที่แล้ว ──────────────────┐
│                                                    │
│  Machine:    RBL103 · TAHARA                       │
│  Requester:  Op1 (Operator · Left)                 │
│  Category:   🔧 Setup problem                      │
│  Urgency:    🔴 HIGH                                │
│                                                    │
│  รายละเอียด:                                        │
│  ┌────────────────────────────────────────────────┐│
│  │ mold ติดตั้งไม่แน่น · ต้องการช่วยขันน๊อต        ││
│  └────────────────────────────────────────────────┘│
│                                                    │
│  📷 Photos (2)                                     │
│  [thumbnail] [thumbnail]                           │
│                                                    │
│  ┌──────────────┐  ┌────────────┐ ┌────────────┐  │
│  │ ✅ รับเรื่อง │  │ 💤 SNOOZE 5m│ │ ❌ ปฏิเสธ  │  │
│  │  (ACCEPT)    │  │             │ │  (DECLINE) │  │
│  └──────────────┘  └────────────┘ └────────────┘  │
└────────────────────────────────────────────────────┘
```

**Spec:**
- Modal: fullscreen or 80% viewport · center
- Auto-play sound alert (repeats until user interacts · max 30 sec)
- Actions:
  - **Accept (✅):** confirm รับเรื่อง · start ETA timer · trigger O-10 OTP flow
  - **Snooze (💤):** ซ่อน 5 min · re-notify · max 2 snoozes then auto-escalate to Leader
  - **Decline (❌):** ต้องระบุเหตุผล · auto-forward ให้ Tech อื่น หรือ Leader
- Sound: `/assets/sounds/help-alert.mp3` · loop 30s · volume 80%
- Vibrate: 3 pulses (mobile)

**Modal Behavior:**
- ต้อง interact ก่อนถึงจะปิดได้ · ป้องกันพลาด
- ถ้าไม่ตอบใน 60 วินาที → escalate อัตโนมัติ (ส่งให้ Leader/Tech อื่น)
- Log event: `notification_shown_at`, `response_at`, `action`

---

### 3️⃣ Help Request Queue Panel

**Trigger:** Click bell icon → slide-in panel from right

**UI:**
```
┌─ 🔔 Help Requests ──────────────────────────────────┐
│  Filter: [All ▾]  [Machine ▾]  [Priority ▾]        │
│                                                     │
│  🔴 URGENT · RBL103 · Op1 · 3 นาที · [Accept][View]│
│  🟠 MED    · RBL102 · Op3 · 8 นาที · [Accept][View]│
│  🟢 LOW    · RBL201 · Op5 · 15 นาที · [Accept][View]│
│  ──────────────────────────                        │
│  ประวัติวันนี้: 12 requests · avg response 4m 20s   │
└─────────────────────────────────────────────────────┘
```

**Features:**
- Realtime update · new request appear top
- Filter by machine · priority · category
- Show response time stats (today · this week)
- Export history CSV

---

### 4️⃣ Post-Accept Flow

หลัง Tech กด **Accept:**
1. ปิด popup · ปรับ bell color เป็นสีส้ม (in-progress)
2. เปิด **Action Card** ขวาบน:
   ```
   ┌─ กำลังไปหา RBL103 ──────┐
   │  Op1 · Setup problem     │
   │  ETA: กด "ถึงแล้ว" ที่   │
   │       tablet OP          │
   │  ⏱️ 00:03:42 (elapsed)   │
   │  [🔗 ดู Details] [❌ Cancel]│
   └──────────────────────────┘
   ```
3. Trigger event → O-10 flow · Op tablet แสดง OTP verify screen
4. Tech ไปถึงหน้างาน · Op ให้ Tech กรอก OTP บน Op tablet · ยืนยัน arrival time
5. เมื่อ OTP verified → คำนวณ response time (accept → arrival) · save log

---

## 🗄️ Data Model Changes

### Table: `help_request` (NEW)

```sql
CREATE TABLE help_request (
  request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  machine_id VARCHAR(20) NOT NULL,
  requester_id VARCHAR(50) NOT NULL,      -- Op username
  requester_role VARCHAR(20) NOT NULL,    -- OPERATOR / LEADER
  position VARCHAR(10) NULL,              -- LEFT / RIGHT (Op position on machine)
  category VARCHAR(30) NOT NULL,          -- SETUP / QUALITY / MECHANICAL / SAFETY / OTHER
  urgency VARCHAR(10) NOT NULL,           -- LOW / MED / HIGH / URGENT
  message VARCHAR(1000) NULL,
  photo_ids JSON NULL,                    -- attachment_photo IDs (reuse T-01 table)
  
  -- Status tracking
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING / ACCEPTED / IN_PROGRESS / RESOLVED / DECLINED / ESCALATED
  assigned_to VARCHAR(50) NULL,           -- Tech username after accept
  
  -- Timing
  requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  notified_at TIMESTAMP NULL,             -- when Tech screen showed alert
  accepted_at TIMESTAMP NULL,
  arrived_at TIMESTAMP NULL,              -- confirmed via O-10 OTP
  resolved_at TIMESTAMP NULL,
  decline_reason VARCHAR(500) NULL,
  
  -- OTP verification (linked with O-10)
  arrival_otp VARCHAR(6) NULL,            -- 6-digit code
  arrival_otp_expiry TIMESTAMP NULL,
  arrival_verified BOOLEAN DEFAULT FALSE,
  
  CONSTRAINT chk_hr_status CHECK (status IN ('PENDING','ACCEPTED','IN_PROGRESS','RESOLVED','DECLINED','ESCALATED')),
  CONSTRAINT chk_hr_urgency CHECK (urgency IN ('LOW','MED','HIGH','URGENT')),
  CONSTRAINT chk_hr_category CHECK (category IN ('SETUP','QUALITY','MECHANICAL','SAFETY','OTHER')),
  INDEX idx_hr_status (status),
  INDEX idx_hr_machine (machine_id, status),
  INDEX idx_hr_assigned (assigned_to, status),
  INDEX idx_hr_requested (requested_at)
);
```

### Table: `help_request_event` (Audit log)

```sql
CREATE TABLE help_request_event (
  event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  event_type VARCHAR(30) NOT NULL,        -- CREATED / NOTIFIED / ACCEPTED / SNOOZED / DECLINED / ARRIVED / RESOLVED / ESCALATED
  actor_id VARCHAR(50) NOT NULL,
  actor_role VARCHAR(20) NOT NULL,
  note VARCHAR(500) NULL,
  event_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_hre_request FOREIGN KEY (request_id) REFERENCES help_request(request_id) ON DELETE CASCADE,
  INDEX idx_hre_request (request_id, event_at)
);
```

---

## 🔌 API Changes

### WebSocket Endpoints (NEW)

**WS `/ws/tech/{techUserId}/notifications`**
- Tech connects on login · receives realtime help_request events
- Messages: `{ type: 'HELP_REQUEST_NEW', request: {...} }` · `HELP_REQUEST_UPDATE` · `HELP_REQUEST_CANCELLED`

**Alternative:** Server-Sent Events (SSE) if WebSocket overhead too much

### REST Endpoints (NEW)

**POST `/api/help-request`** (called from Op tablet · O-10)
```json
{
  "machineId": "RBL103",
  "requesterId": "op1",
  "position": "LEFT",
  "category": "SETUP",
  "urgency": "HIGH",
  "message": "mold ติดตั้งไม่แน่น",
  "photoIds": [123, 124]
}
```
Returns: `{ requestId, otpCode: "483091", otpExpiry: "..." }` · triggers WS broadcast to Tech

**POST `/api/help-request/{id}/accept`**
```json
{ "techId": "tech1" }
```
Returns: `{ status: 'ACCEPTED', acceptedAt: "..." }`

**POST `/api/help-request/{id}/snooze`**
```json
{ "snoozeMinutes": 5 }
```

**POST `/api/help-request/{id}/decline`**
```json
{ "techId": "tech1", "reason": "อยู่งานอื่น RBL102" }
```

**POST `/api/help-request/{id}/verify-arrival`** (called from Op tablet)
```json
{ "otpCode": "483091", "verifierId": "op1" }
```
Returns: `{ arrivalVerified: true, responseTimeSec: 214 }`

**POST `/api/help-request/{id}/resolve`**
```json
{ "resolution": "ขันน๊อตให้แน่น · ตรวจสอบ 30 นาทีถัดไป" }
```

**GET `/api/help-request/queue?techId=X&status=PENDING,ACCEPTED`**
- Return Tech's queue for panel display

**GET `/api/help-request/stats?techId=X&period=today`**
- Return: total, avg response time, avg resolution time

---

## 🖥️ Frontend Changes (React)

### Component tree:
```
src/components/HelpRequest/
  ├── NotificationBell.tsx        (NEW · header widget · badge)
  ├── HelpRequestPopup.tsx        (NEW · fullscreen alert modal)
  ├── HelpRequestQueue.tsx        (NEW · sidebar panel)
  ├── ActiveRequestCard.tsx       (NEW · in-progress card)
  └── ResolveModal.tsx            (NEW · resolution note + photo)

src/services/
  ├── websocket.ts                (NEW · WS client · auto-reconnect)
  ├── notification.ts             (NEW · sound + vibrate helpers)
  └── helpRequestApi.ts           (NEW · REST client)

src/hooks/
  └── useHelpRequests.ts          (NEW · realtime state hook)
```

### Key libraries:
- `socket.io-client` or `reconnecting-websocket` — WebSocket
- Existing UI kit (Material-UI)
- Native Web Audio API (for alert sound)
- Native Vibration API (mobile)

### Global integration:
- App-level provider wraps children · listens to WS
- Notification bell shows on all Tech pages (Setup · Downtime · Dashboard)
- Popup can trigger from any page (not just Dashboard)

---

## ✅ Acceptance Criteria

### Notification Bell
- [ ] Bell icon visible in header ทุกหน้าของ Tech role
- [ ] Badge count = pending help requests · realtime update
- [ ] Color state: gray (no) · red (pending) · orange (in-progress)
- [ ] Click → open queue panel

### Realtime Alert Popup
- [ ] WebSocket connect on Tech login · auto-reconnect on disconnect
- [ ] New help request → popup within 3 seconds of Op button press
- [ ] Popup shows: machine · requester · position · category · urgency · message · photos
- [ ] Sound plays (help-alert.mp3 · loop 30s max)
- [ ] Vibrate on mobile (3 pulses)
- [ ] Actions: Accept · Snooze · Decline (with reason)
- [ ] Cannot dismiss without action · max 60s then auto-escalate

### Queue Panel
- [ ] Slide-in from right on bell click
- [ ] Show all pending + accepted requests
- [ ] Filter by machine · priority · category
- [ ] Stats: today count + avg response time

### Post-Accept Flow
- [ ] After Accept → Action Card appears top-right (persistent across pages)
- [ ] ETA timer running · elapsed time visible
- [ ] Trigger O-10 OTP screen on Op tablet
- [ ] OTP verify → arrival_at recorded · response time calculated
- [ ] Resolve modal (note + photo) · mark RESOLVED

### Data & Logging
- [ ] `help_request` table with all timing fields
- [ ] `help_request_event` audit trail (create, notify, accept, snooze, decline, arrive, resolve)
- [ ] Photos linked via `attachment_photo` (ref_type='HELP_REQUEST') · reuse T-01 pattern

### Performance
- [ ] WebSocket latency < 2 sec end-to-end
- [ ] Popup render within 500ms after WS receive
- [ ] Queue panel loads under 1 sec

### Testing
- [ ] Unit test: WS reconnect logic
- [ ] Unit test: sound/vibrate helpers
- [ ] Integration test: Op create → Tech receive alert → Accept flow
- [ ] E2E test: full flow Op → Tech alert → Accept → OTP verify → Resolve
- [ ] Test บน Samsung Tab Active 5 Pro · ใส่ถุงมือ · popup buttons ติดง่าย
- [ ] Test sound audible over machine noise (test with actual factory noise)
- [ ] Test auto-escalate after 60s no response

---

## 🚀 Migration Plan

**V23:** New Flyway migration
```sql
-- V23__help_request_system.sql

CREATE TABLE help_request (
  request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  machine_id VARCHAR(20) NOT NULL,
  requester_id VARCHAR(50) NOT NULL,
  requester_role VARCHAR(20) NOT NULL,
  position VARCHAR(10) NULL,
  category VARCHAR(30) NOT NULL,
  urgency VARCHAR(10) NOT NULL,
  message VARCHAR(1000) NULL,
  photo_ids JSON NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  assigned_to VARCHAR(50) NULL,
  requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  notified_at TIMESTAMP NULL,
  accepted_at TIMESTAMP NULL,
  arrived_at TIMESTAMP NULL,
  resolved_at TIMESTAMP NULL,
  decline_reason VARCHAR(500) NULL,
  arrival_otp VARCHAR(6) NULL,
  arrival_otp_expiry TIMESTAMP NULL,
  arrival_verified BOOLEAN DEFAULT FALSE,
  CONSTRAINT chk_hr_status CHECK (status IN ('PENDING','ACCEPTED','IN_PROGRESS','RESOLVED','DECLINED','ESCALATED')),
  CONSTRAINT chk_hr_urgency CHECK (urgency IN ('LOW','MED','HIGH','URGENT')),
  CONSTRAINT chk_hr_category CHECK (category IN ('SETUP','QUALITY','MECHANICAL','SAFETY','OTHER'))
);
CREATE INDEX idx_hr_status ON help_request(status);
CREATE INDEX idx_hr_machine ON help_request(machine_id, status);
CREATE INDEX idx_hr_assigned ON help_request(assigned_to, status);
CREATE INDEX idx_hr_requested ON help_request(requested_at);

CREATE TABLE help_request_event (
  event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  event_type VARCHAR(30) NOT NULL,
  actor_id VARCHAR(50) NOT NULL,
  actor_role VARCHAR(20) NOT NULL,
  note VARCHAR(500) NULL,
  event_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_hre_request FOREIGN KEY (request_id) REFERENCES help_request(request_id) ON DELETE CASCADE
);
CREATE INDEX idx_hre_request ON help_request_event(request_id, event_at);

-- Add HELP_REQUEST to attachment_photo allowed refType (from T-01 migration)
-- ALTER TABLE attachment_photo DROP CONSTRAINT chk_ref_type;
-- ALTER TABLE attachment_photo ADD CONSTRAINT chk_ref_type 
--   CHECK (ref_type IN ('SETUP_TIME_LOG', 'MACHINE_DOWNTIME', 'HELP_REQUEST'));
```

---

## 🔗 Related Issues

- **O-10** · Op-side · Telegram alert + OTP verify arrival (⭐ dependency · same event flow)
- **T-01** · Photo attachment (reuse `attachment_photo` table · same pattern)
- **O-09** · Realtime hourly monitoring (similar WebSocket infra · can share)
- Future: **Leader escalation dashboard** (see all requests across techs)

---

## 📝 Notes / Open Questions

- [ ] WebSocket vs SSE · which to use? (WS = duplex · SSE = server-push only · simpler)
- [ ] Sound file: หาให้ · 3-second alert loop · ไม่ annoying · industrial-friendly
- [ ] Snooze policy: 5 min OK? · max 2 snoozes then escalate?
- [ ] Auto-escalate to Leader · หรือ next Tech in rotation?
- [ ] Multi-tech scenario · assign to specific Tech หรือ broadcast to all?
- [ ] OTP length: 6 digits OK? · expire time?
- [ ] Response time SLA target · เช่น < 5 min for URGENT · < 15 min for HIGH?
- [ ] Offline handling: Tech disconnect → Queue messages · deliver on reconnect
- [ ] Cross-shift handoff: unresolved requests → notify next shift Tech?

---

## 🎯 Effort Estimate

| Task | Effort |
|---|---|
| Flyway V23 migration + attachment_photo update | 0.5 day |
| Backend · HelpRequest entity + Repo + Service | 1 day |
| Backend · REST endpoints (create · accept · snooze · decline · verify · resolve) | 1.5 days |
| Backend · WebSocket server + broadcast logic | 1 day |
| Backend · Auto-escalate scheduler (60s timeout) | 0.5 day |
| Frontend · WebSocket client + auto-reconnect | 0.5 day |
| Frontend · NotificationBell + Queue panel | 1 day |
| Frontend · HelpRequestPopup (fullscreen alert + sound) | 1 day |
| Frontend · ActiveRequestCard + Resolve modal | 0.75 day |
| Testing + integration | 1.5 days |
| **Total** | **~9.25 days (L)** |

---

## 🎯 Design Decision Log

**Q: WebSocket หรือ Push Notification?**
A: **In-app WebSocket** เพราะ:
- Tech อยู่หน้า MES ตลอด · in-app alert เพียงพอ
- ไม่ต้อง register FCM/APNS (complex)
- Realtime กว่า
- Fallback: O-10 Telegram สำหรับกรณี MES app ปิด

**Q: Sound alert ปิดได้ไหม?**
A: **ปิดไม่ได้ใน work hours** (07:00-19:00) · หลัง 19:00 = configurable
Reason: Safety-critical · ต้องไม่พลาด URGENT

**Q: Multi-tech หลายคน · assign ยังไง?**
A: **Broadcast to all on-shift Techs** · แรกกด Accept = ได้งาน · คนอื่น alert dismiss
Reason: fastest response · fair

**Q: OTP arrival จำเป็นไหม?**
A: **Yes for AUDIT** · ยืนยัน Tech ไปถึงจริง · measure response time accurate · ป้องกัน "รับเรื่องแต่ไม่ไป"
