package ru.akydevv.akycheatcheck.service;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.Plugin;
import ru.akydevv.akycheatcheck.config.SettingsProvider;
import ru.akydevv.akycheatcheck.model.CheckSession;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class WebDashboardService {
    private final Plugin plugin;
    private final SettingsProvider settingsProvider;
    private final CheckSessionService checkSessionService;
    private HttpServer server;
    private ExecutorService executor;

    public WebDashboardService(Plugin plugin, SettingsProvider settingsProvider, CheckSessionService checkSessionService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
        this.checkSessionService = Objects.requireNonNull(checkSessionService, "checkSessionService");
    }

    public void start() {
        stop();
        if (!settingsProvider.getSettings().isWebDashboardEnabled()) {
            return;
        }
        try {
            InetSocketAddress address = new InetSocketAddress(
                    settingsProvider.getSettings().getWebDashboardHost(),
                    settingsProvider.getSettings().getWebDashboardPort()
            );
            executor = Executors.newFixedThreadPool(2, task -> {
                Thread thread = new Thread(task, "AkyCheatCheck-WebDashboard");
                thread.setDaemon(true);
                return thread;
            });
            server = HttpServer.create(address, 0);
            server.setExecutor(executor);
            server.createContext("/", this::handleIndex);
            server.createContext("/api/overview", this::handleOverview);
            server.createContext("/api/logs", this::handleLogs);
            server.start();
            plugin.getLogger().info("Web-dashboard запущен: http://" + settingsProvider.getSettings().getWebDashboardHost()
                    + ":" + settingsProvider.getSettings().getWebDashboardPort() + "/?token=" + settingsProvider.getSettings().getWebDashboardToken());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Не удалось запустить web-dashboard", exception);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 401, "text/plain; charset=utf-8", "Unauthorized");
            return;
        }
        send(exchange, 200, "text/html; charset=utf-8", renderDashboard());
    }

    private void handleOverview(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}");
            return;
        }
        send(exchange, 200, "application/json; charset=utf-8", renderOverviewJson());
    }

    private void handleLogs(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}");
            return;
        }
        List<String> logs = readLastLogLines(120);
        StringBuilder json = new StringBuilder("{\"logs\":[");
        for (int index = 0; index < logs.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(escapeJson(logs.get(index))).append('"');
        }
        json.append("]}");
        send(exchange, 200, "application/json; charset=utf-8", json.toString());
    }

    private boolean isAuthorized(HttpExchange exchange) {
        String configuredToken = settingsProvider.getSettings().getWebDashboardToken();
        if (configuredToken.isBlank()) {
            return true;
        }
        String headerToken = exchange.getRequestHeaders().getFirst("X-AkyCheck-Token");
        if (configuredToken.equals(headerToken)) {
            return true;
        }
        return configuredToken.equals(queryParameters(exchange).get("token"));
    }

    private Map<String, String> queryParameters(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            result.put(key, value);
        }
        return result;
    }

    private String renderOverviewJson() {
        List<CheckSession> sessions = checkSessionService.getSessions().stream()
                .sorted(Comparator.comparing(CheckSession::getSuspectName))
                .toList();
        StringBuilder json = new StringBuilder();
        json.append("{\"activeChecks\":").append(checkSessionService.getActiveSessionCount()).append(",");
        json.append("\"sessions\":[");
        Instant now = Instant.now();
        for (int index = 0; index < sessions.size(); index++) {
            CheckSession session = sessions.get(index);
            if (index > 0) {
                json.append(',');
            }
            json.append('{')
                    .append("\"suspect\":\"").append(escapeJson(session.getSuspectName())).append("\",")
                    .append("\"moderator\":\"").append(escapeJson(session.getModeratorName())).append("\",")
                    .append("\"cabin\":\"").append(escapeJson(session.getCabin().id())).append("\",")
                    .append("\"remaining\":").append(session.remainingSeconds(now))
                    .append('}');
        }
        json.append("]}");
        return json.toString();
    }

    private List<String> readLastLogLines(int limit) {
        Path file = plugin.getDataFolder().toPath().resolve("logs").resolve("checks.log");
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int fromIndex = Math.max(0, lines.size() - limit);
            return new ArrayList<>(lines.subList(fromIndex, lines.size()));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Не удалось прочитать checks.log для dashboard", exception);
            return List.of();
        }
    }

    private String renderDashboard() {
        String token = escapeHtml(settingsProvider.getSettings().getWebDashboardToken());
        return """
                <!doctype html>
                <html lang=\"ru\">
                <head>
                <meta charset=\"utf-8\">
                <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
                <title>AkyCheatCheck Console</title>
                <style>
                :root{--surface:#f7f5ef;--panel:#fffdf8;--ink:#10100f;--muted:#737069;--hair:#ded9cf;--accent:#c23224;--ok:#1f7a4d;--mono:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;--sans:Inter,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}
                *{box-sizing:border-box}html{font-family:var(--sans);background:var(--surface);color:var(--ink)}body{margin:0;min-height:100vh}.page{max-width:1240px;margin:0 auto;padding:40px 28px 56px}.mast{display:grid;grid-template-columns:1.1fr .9fr;gap:40px;align-items:end;margin-bottom:44px}.eyebrow{font:600 12px/1 var(--mono);letter-spacing:.12em;text-transform:uppercase;color:var(--accent);margin-bottom:18px}.title{font-size:clamp(48px,7vw,96px);line-height:.98;letter-spacing:-.055em;margin:0;max-width:850px}.lede{font-size:18px;line-height:1.55;color:var(--muted);max-width:460px;margin:0 0 6px}.meta{font:500 13px/1.6 var(--mono);color:var(--muted);margin-top:18px}.grid{display:grid;grid-template-columns:280px 1fr;gap:18px}.stat{background:var(--panel);border:1px solid var(--hair);padding:22px 20px;min-height:140px}.stat b{display:block;font:600 68px/.9 var(--mono);letter-spacing:-.08em}.stat span{display:block;color:var(--muted);font:600 12px/1.4 var(--mono);letter-spacing:.08em;text-transform:uppercase;margin-top:18px}.panel{background:var(--panel);border:1px solid var(--hair);min-width:0}.panelHead{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:16px 18px;border-bottom:1px solid var(--hair)}.panelHead h2{font-size:14px;line-height:1;margin:0;letter-spacing:.02em}.button{border:1px solid var(--ink);background:var(--ink);color:var(--panel);height:36px;padding:0 12px;border-radius:6px;font:600 12px var(--mono);cursor:pointer}.button:hover{background:var(--accent);border-color:var(--accent)}.button:focus-visible{outline:2px solid var(--accent);outline-offset:2px}.sessions{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:0}.sessions div{padding:13px 14px;border-bottom:1px solid var(--hair);font:500 13px var(--mono)}.sessions .head{color:var(--muted);font-size:11px;text-transform:uppercase;letter-spacing:.08em}.empty{padding:42px 18px;color:var(--muted);font-size:15px}.logs{height:440px;overflow:auto;padding:0;margin:0;list-style:none}.logs li{border-bottom:1px solid var(--hair);padding:11px 18px;font:500 12px/1.55 var(--mono);white-space:pre-wrap}.logs li[data-type=START]{color:var(--ok)}.logs li[data-type=END]{color:var(--accent)}.two{display:grid;grid-template-columns:1fr;gap:18px;margin-top:18px}@media(max-width:860px){.page{padding:28px 18px}.mast,.grid{grid-template-columns:1fr}.sessions{grid-template-columns:1fr 1fr}.title{font-size:52px}}@media(prefers-reduced-motion:no-preference){.panel,.stat{transition:border-color .12s ease}.panel:hover,.stat:hover{border-color:#bbb3a6}}
                </style>
                </head>
                <body>
                <main class=\"page\">
                  <section class=\"mast\">
                    <div><div class=\"eyebrow\">AkyCheatCheck / panel</div><h1 class=\"title\">Кто на проверке. Что было в логах.</h1></div>
                    <div><p class=\"lede\">Панель показывает активные проверки и последние записи из журнала. Этого хватает, чтобы быстро понять, кто кого проверяет и чем закончились прошлые сессии.</p><div class=\"meta\">token: <span id=\"token\">%TOKEN%</span></div></div>
                  </section>
                  <section class=\"grid\">
                    <aside class=\"stat\"><b id=\"activeChecks\">0</b><span>сейчас на проверке</span></aside>
                    <div class=\"panel\"><div class=\"panelHead\"><h2>Текущие проверки</h2><button class=\"button\" id=\"refresh\">обновить</button></div><div id=\"sessions\"></div></div>
                  </section>
                  <section class=\"two\"><div class=\"panel\"><div class=\"panelHead\"><h2>Последние записи</h2><span class=\"meta\" id=\"updated\"></span></div><ul class=\"logs\" id=\"logs\"></ul></div></section>
                </main>
                <script>
                const token = new URLSearchParams(location.search).get('token') || document.getElementById('token').textContent;
                async function load(){
                  const [overview, logs] = await Promise.all([fetch('/api/overview?token='+encodeURIComponent(token)).then(r=>r.json()), fetch('/api/logs?token='+encodeURIComponent(token)).then(r=>r.json())]);
                  document.getElementById('activeChecks').textContent = overview.activeChecks ?? 0;
                  const sessions = document.getElementById('sessions');
                  if(!overview.sessions || overview.sessions.length===0){ sessions.innerHTML='<div class=\"empty\">Проверок сейчас нет. Когда модератор вызовет игрока, сессия появится здесь.</div>'; }
                  else { sessions.innerHTML='<div class=\"sessions\"><div class=\"head\">игрок</div><div class=\"head\">модератор</div><div class=\"head\">кабинка</div><div class=\"head\">осталось</div>'+overview.sessions.map(s=>`<div>${esc(s.suspect)}</div><div>${esc(s.moderator)}</div><div>${esc(s.cabin)}</div><div>${time(s.remaining)}</div>`).join('')+'</div>'; }
                  document.getElementById('logs').innerHTML = (logs.logs||[]).slice().reverse().map(line=>`<li data-type=\"${line.includes(' START ')?'START':line.includes(' END ')?'END':''}\">${esc(line)}</li>`).join('') || '<li>В checks.log пока нет записей.</li>';
                  document.getElementById('updated').textContent = new Date().toLocaleTimeString('ru-RU');
                }
                function time(v){v=Math.max(0,Number(v)||0);return String(Math.floor(v/60)).padStart(2,'0')+':'+String(v%60).padStart(2,'0')}
                function esc(v){return String(v).replace(/[&<>\"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;'}[c]))}
                document.getElementById('refresh').addEventListener('click', load); load(); setInterval(load, 5000);
                </script>
                </body>
                </html>
                """.replace("%TOKEN%", token);
    }

    private void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
