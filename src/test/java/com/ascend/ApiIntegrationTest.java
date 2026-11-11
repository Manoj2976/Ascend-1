package com.ascend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private JsonNode send(org.springframework.test.web.servlet.RequestBuilder req, int expectedStatus) throws Exception {
        String body = mvc.perform(req).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return body.isBlank() ? mapper.nullNode() : mapper.readTree(body);
    }

    private String register(String email) throws Exception {
        String json = "{\"email\":\"" + email + "\",\"password\":\"password123\",\"name\":\"Tester\"}";
        return send(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json), 201).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void requiresLogin() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        register("login@example.com");
        send(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"login@example.com\",\"password\":\"wrong-password\"}"), 401);
        send(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"LOGIN@example.com\",\"password\":\"password123\"}"), 200);
    }

    @Test
    void tickingALinkedTaskMovesTheMonthlyTarget() throws Exception {
        String token = register("flow@example.com");

        JsonNode goal = send(post("/api/goals").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"MONTHLY\",\"title\":\"Solve 60 DSA problems\",\"unit\":\"problems\",\"target\":60,\"progress\":0,\"month\":\"2026-11\"}"), 201);
        String goalId = goal.get("id").asText();

        JsonNode task = send(post("/api/tasks").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Solve 2 mediums\",\"date\":\"2026-11-10\",\"time\":\"21:00\",\"category\":\"DSA\",\"repeat\":\"none\",\"goalId\":\"" + goalId + "\"}"), 201);
        String taskId = task.get("id").asText();
        assertEquals("21:00", task.get("time").asText());
        assertEquals(goalId, task.get("goalId").asText());

        JsonNode ticked = send(post("/api/tasks/" + taskId + "/toggle").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2026-11-10\"}"), 200);
        assertEquals(1, ticked.get("doneDates").size());
        assertEquals(1, send(get("/api/goals").header("Authorization", bearer(token)), 200).get(0).get("progress").asInt());

        send(post("/api/tasks/" + taskId + "/toggle").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2026-11-10\"}"), 200);
        assertEquals(0, send(get("/api/goals").header("Authorization", bearer(token)), 200).get(0).get("progress").asInt());

        // a day the task does not occur on is rejected
        send(post("/api/tasks/" + taskId + "/toggle").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2026-11-11\"}"), 400);
    }

    @Test
    void dailyTargetsKeepALogPerDay() throws Exception {
        String token = register("daily@example.com");
        String id = send(post("/api/goals").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"DAILY\",\"title\":\"DSA problems\",\"unit\":\"problems\",\"target\":3}"), 201).get("id").asText();

        String today = "{\"date\":\"2026-11-10\",\"delta\":1}";
        send(post("/api/goals/" + id + "/log").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(today), 200);
        JsonNode g = send(post("/api/goals/" + id + "/log").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(today), 200);
        assertEquals(2, g.get("logs").get("2026-11-10").asInt());

        JsonNode back = send(post("/api/goals/" + id + "/log").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-11-10\",\"delta\":-5}"), 200);
        assertFalse(back.get("logs").has("2026-11-10")); // never goes below zero
    }

    @Test
    void usersCannotSeeOrTouchEachOthersData() throws Exception {
        String alice = register("alice@example.com");
        String bob = register("bob@example.com");

        String id = send(post("/api/tasks").header("Authorization", bearer(alice)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Private\",\"date\":\"2026-11-10\"}"), 201).get("id").asText();

        assertEquals(0, send(get("/api/tasks").header("Authorization", bearer(bob)), 200).size());
        send(delete("/api/tasks/" + id).header("Authorization", bearer(bob)), 404);
        send(post("/api/tasks/" + id + "/toggle").header("Authorization", bearer(bob)), 404);
        assertEquals(1, send(get("/api/tasks").header("Authorization", bearer(alice)), 200).size());
    }

    @Test
    void validationErrorsComeBackAsJson() throws Exception {
        String token = register("invalid@example.com");
        JsonNode err = send(post("/api/tasks").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\",\"date\":\"2026-11-10\"}"), 400);
        assertTrue(err.get("fields").has("title"));
        send(post("/api/tasks").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"date\":\"2026-11-10\",\"time\":\"25:99\"}"), 400);
        send(get("/api/does-not-exist").header("Authorization", bearer(token)), 404);
    }

    @Test
    void browserDataCanBeImportedWithItsOwnIds() throws Exception {
        String token = register("import@example.com");
        // Shape and ids exactly like the React app's localStorage ("g1", "t1" are not UUIDs)
        String state = """
                {"tasks":[{"id":"t1","title":"Solve 2 mediums","date":"2026-11-10","time":"10:00","category":"DSA","repeat":"daily","goalId":"g1","notes":"","doneDates":["2026-11-10"]}],
                 "goals":[{"id":"f1","type":"FINAL","title":"Get placed","unit":"","target":100,"progress":0,"month":"","deadline":"2027-06-30","parentId":"","logs":{}},
                          {"id":"g1","type":"MONTHLY","title":"60 DSA","unit":"problems","target":60,"progress":18,"month":"2026-11","deadline":"","parentId":"f1","logs":{}},
                          {"id":"d1","type":"DAILY","title":"DSA","unit":"problems","target":3,"progress":0,"month":"","deadline":"","parentId":"","logs":{"2026-11-10":2}}],
                 "journal":{"2026-11-09":{"mood":4,"text":"Good day","learned":"Two pointers","plan":"More DP"}},
                 "applications":[{"id":"a1","company":"Acme","role":"SDE","status":"Applied","date":"2026-11-05","link":"","notes":""}],
                 "focus":{"2026-11-09":75},
                 "settings":{"name":"Manoj","theme":"dark","windowMin":30,"intervalMin":5,"sound":true,"notifications":true,"streakThreshold":80}}
                """;
        send(put("/api/state").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(state), 200);

        JsonNode goals = send(get("/api/goals").header("Authorization", bearer(token)), 200);
        assertEquals(3, goals.size());
        String monthlyId = null, finalId = null, parentOfMonthly = null;
        for (JsonNode g : goals) {
            if (g.get("type").asText().equals("MONTHLY")) { monthlyId = g.get("id").asText(); parentOfMonthly = g.get("parentId").asText(); }
            if (g.get("type").asText().equals("FINAL")) finalId = g.get("id").asText();
        }
        assertEquals(finalId, parentOfMonthly); // links were re-pointed at the new ids
        UUID.fromString(monthlyId);

        JsonNode tasks = send(get("/api/tasks").header("Authorization", bearer(token)), 200);
        assertEquals(monthlyId, tasks.get(0).get("goalId").asText());
        assertEquals("daily", tasks.get(0).get("repeat").asText());
        assertEquals(1, tasks.get(0).get("doneDates").size());

        assertEquals(75, send(get("/api/focus").header("Authorization", bearer(token)), 200).get("2026-11-09").asInt());
        assertEquals("Good day", send(get("/api/journal/2026-11-09").header("Authorization", bearer(token)), 200).get("text").asText());
        assertEquals("dark", send(get("/api/settings").header("Authorization", bearer(token)), 200).get("theme").asText());

        // exporting and importing again is a stable round trip
        JsonNode exported = send(get("/api/state").header("Authorization", bearer(token)), 200);
        send(put("/api/state").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(exported.toString()), 200);
        assertEquals(3, send(get("/api/goals").header("Authorization", bearer(token)), 200).size());
        assertEquals(1, send(get("/api/tasks").header("Authorization", bearer(token)), 200).size());
    }

    @Test
    void syncKeepsIdsStoresTimetableAndDietAndRefusesStaleWrites() throws Exception {
        String token = register("sync@example.com");
        String other = register("sync-other@example.com");
        String taskId = UUID.randomUUID().toString();
        String state = """
                {"tasks":[{"id":"%s","title":"Bath","date":"2026-11-10","time":"21:00","category":"Personal","repeat":"none","goalId":"","notes":"","doneDates":[]}],
                 "goals":[],"journal":{},"applications":[],"focus":{},
                 "timetable":[{"id":"e1","day":1,"start":"09:00","end":"10:00","title":"OS","kind":"Class","place":"B204","notes":""}],
                 "diet":{"goals":{"kcal":2100,"protein":70,"water":8},"logs":{"2026-11-10":{"items":[{"id":"f1","meal":"Lunch","name":"Rice","kcal":300}],"water":2}},"plan":{}},
                 "revision":0}
                """.formatted(taskId);
        JsonNode saved = send(put("/api/state").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(state), 200);
        assertEquals(1, saved.get("revision").asInt());
        assertEquals(taskId, saved.get("tasks").get(0).get("id").asText()); // client UUID kept
        assertEquals("OS", saved.get("timetable").get(0).get("title").asText());
        assertEquals(300, saved.get("diet").get("logs").get("2026-11-10").get("items").get(0).get("kcal").asInt());

        // saving again with the current revision works and bumps it
        ((com.fasterxml.jackson.databind.node.ObjectNode) saved).put("revision", 1);
        JsonNode again = send(put("/api/state").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(saved.toString()), 200);
        assertEquals(2, again.get("revision").asInt());
        assertEquals(taskId, again.get("tasks").get(0).get("id").asText());

        // a device that still thinks revision is 1 is refused
        send(put("/api/state").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(saved.toString()), 409);

        // another account cannot take over this task id
        String steal = "{\"tasks\":[{\"id\":\"" + taskId + "\",\"title\":\"Mine now\",\"date\":\"2026-11-10\"}]}";
        send(put("/api/state").header("Authorization", bearer(other)).contentType(MediaType.APPLICATION_JSON).content(steal), 200);
        JsonNode mine = send(get("/api/tasks").header("Authorization", bearer(token)), 200);
        assertEquals("Bath", mine.get(0).get("title").asText());
        assertEquals(taskId, mine.get(0).get("id").asText());
        assertNotEquals(taskId, send(get("/api/tasks").header("Authorization", bearer(other)), 200).get(0).get("id").asText());
    }

    @Test
    void changesMadeOutsideTheAppSyncBumpTheRevision() throws Exception {
        String token = register("bump@example.com");
        JsonNode created = send(post("/api/tasks").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Bath\",\"date\":\"2026-11-10\"}"), 201);
        long before = send(get("/api/state").header("Authorization", bearer(token)), 200).get("revision").asLong();
        send(post("/api/tasks/" + created.get("id").asText() + "/toggle").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2026-11-10\"}"), 200);
        long after = send(get("/api/state").header("Authorization", bearer(token)), 200).get("revision").asLong();
        assertTrue(after > before);
    }
}
