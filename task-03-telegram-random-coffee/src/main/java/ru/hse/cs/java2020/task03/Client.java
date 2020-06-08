package ru.hse.cs.java2020.task03;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;

public final class Client {
    private static final int MAGIC_NUBMER_4 = 4;
    private static final int MAGIC_NUBMER_100 = 100;
    private static final int CODE_10_TIMEOUT = 10;
    private static final int CODE_200_SUCCESS = 200;
    private static final int CODE_201_SUCCESS = 201;
    private static final int CODE_401_AUTHORIZE = 401;
    private static final int CODE_403_RIGHTS = 403;
    private static final int CODE_404_FOUND = 404;
    private static Client instance = null;
    private final HttpClient client;

    private Client() {
        client = HttpClient.newHttpClient();
    }

    public static Client getTrackerClient() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    ArrayList<Queue> getAllQueues(String oauthToken, String orgID) throws
            java.io.IOException, java.lang.InterruptedException, AuthorizationException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tracker.yandex.net/v2/queues?"))
                .timeout(Duration.ofSeconds(CODE_10_TIMEOUT))
                .headers("Authorization", "OAuth " + oauthToken, "X-Org-Id", orgID).GET().build();
        var answ = client.send(request, HttpResponse.BodyHandlers.ofString());
        var body = answ.body();
        if (answ.statusCode() / MAGIC_NUBMER_100 == MAGIC_NUBMER_4) {
            throw new AuthorizationException("");
        }
        ArrayList<Queue> result = new ArrayList<>();
        var queues = new JSONArray(body);
        for (int i = 0; i < queues.length(); i++) {
            result.add(new Queue(queues.getJSONObject(i).getString("key"), queues.getJSONObject(i).getInt("id")));
        }
        return result;
    }

    Optional<String> createTask(String oauthToken, String orgID, String name, String description, Optional<String> user, String queueID) {
        JSONObject requestJSON = new JSONObject();
        requestJSON.put("summary", name);
        requestJSON.put("description", description);
        requestJSON.put("queue", new JSONObject().put("id", queueID));
        if (user.isPresent()) {
            requestJSON.put("assignee", user.get());
        }
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.tracker.yandex.net/v2/issues?"))
                .timeout(Duration.ofSeconds(CODE_10_TIMEOUT))
                .headers("Authorization", "OAuth " + oauthToken, "X-Org-Id", orgID)
                .POST(HttpRequest.BodyPublishers.ofString(requestJSON.toString())).build();
        try {
            var answ = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (answ.statusCode() == CODE_201_SUCCESS) {
                JSONObject obj = new JSONObject(answ.body());
                return Optional.of(obj.getString("key"));
            } else {
                return Optional.empty();
            }
        } catch (IOException | InterruptedException e) {
            return Optional.empty();
        }
    }

    ArrayList<String> getTasksByUser(String oauthToken, String orgID, String user) throws
            TrackerException {
        JSONObject request = new JSONObject();
            request.put("filter", new JSONObject().put("assignee", user)); HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tracker.yandex.net/v2/issues/_search?order=+updatedAt"))
                .timeout(Duration.ofSeconds(CODE_10_TIMEOUT))
                .headers("Authorization", "OAuth " + oauthToken, "X-Org-Id", orgID)
                .POST(HttpRequest.BodyPublishers.ofString(request.toString())).build();
        try {
            var answ = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (answ.statusCode() != CODE_200_SUCCESS) {
                System.err.println(answ.body());
                throw new TrackerException(answ.body());
            }
            JSONArray responseJSON = new JSONArray(answ.body());
            var result = new ArrayList<String>();
            for (int i = 0; i < responseJSON.length(); i++) {
                result.add(responseJSON.getJSONObject(i).getString("key"));
            }
            return result;
        } catch (IOException | InterruptedException exc) {
            System.err.println(exc.getMessage());
            throw new TrackerException(exc.getMessage());
        }
    }

    Task getTask(String oauthToken, String orgID, String task) throws
            java.io.IOException, java.lang.InterruptedException, AuthorizationException, TrackerException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.tracker.yandex.net/v2/issues/" + task))
                .timeout(Duration.ofSeconds(CODE_10_TIMEOUT)).headers("Authorization", "OAuth " + oauthToken, "X-Org-Id", orgID).GET().build();
        var answ = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (answ.statusCode() == CODE_401_AUTHORIZE || answ.statusCode() == CODE_403_RIGHTS) {
            throw new AuthorizationException(answ.body());
        }
        if (answ.statusCode() == CODE_404_FOUND) {
            throw new TrackerException(answ.body());
        }
        var body = answ.body();
        var res = new Task();
        JSONObject obj = new JSONObject(body);
        res.setName(obj.getString("key"));
        res.setDescription(obj.getString("summary"));
        if (obj.has("assignee")) {
            res.setAssignedTo(obj.getJSONObject("assignee").getString("display"));
        } else {
            res.setAssignedTo(null);
        }
        res.setAuthor(obj.getJSONObject("createdBy").getString("display"));
        if (obj.has("followers")) {
            var followers = obj.getJSONArray("followers");
            for (int i = 0; i < followers.length(); i++) {
                res.addFollower(followers.getJSONObject(i).getString("display"));
            }
        }
        HttpRequest commentRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tracker.yandex.net/v2/issues/" + task + "/comments"))
                .timeout(Duration.ofSeconds(CODE_10_TIMEOUT))
                .headers("Authorization", "OAuth " + oauthToken, "X-Org-Id", orgID).GET().build();
        answ = client.send(commentRequest, HttpResponse.BodyHandlers.ofString());
        body = answ.body();
        var comments = new JSONArray(body);
        for (int i = 0; i < comments.length(); i++) {
            res.addComment(new Comment(comments.getJSONObject(i).getJSONObject("createdBy")
                    .getString("display"), comments.getJSONObject(i).getString("text")));
        }
        return res;
    }
}
