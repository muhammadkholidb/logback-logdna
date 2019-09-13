package com.gitlab.muhammadkholidb.logbacklogdna;

import static java.net.URLEncoder.encode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map.Entry;

import com.goebl.david.Response;
import com.goebl.david.Webb;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.status.ErrorStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author muhammad
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper=false)
public class LogDNAAppender extends ConsoleAppender<ILoggingEvent> {

    private String ingestionKey;
    private String appName;
    private boolean includeStacktrace = true;
    private boolean sendMDC = true;

    private static final String LOGDNA_INGEST_URL = "https://logs.logdna.com/logs/ingest";

    private Webb webb;

    @Override
    public void start() {
        boolean error = false;
        if (this.encoder == null) {
            addStatus(new ErrorStatus("No encoder set for the appender named \"" + name + "\".", this));
            error = true;
        } else if (ingestionKey == null || ingestionKey.isEmpty()) {
            addStatus(new ErrorStatus("No ingestionKey set in the configuration. Find the value from LogDNA settings.", this));
            error = true;
        } else if (appName == null || appName.isEmpty()) {
            addStatus(new ErrorStatus("No appName set in the configuration.", this));
            error = true;
        }
        // Only error free appenders will be activated
        if (!error) {
            this.webb = Webb.create();
            this.webb.setBaseUri(LOGDNA_INGEST_URL);
            this.webb.setDefaultHeader(Webb.HDR_USER_AGENT, "com.gitlab.muhammadkholidb.logbacklogdna.LogDNAAppender");
            this.webb.setDefaultHeader(Webb.HDR_CONTENT_TYPE, "application/json; charset=UTF-8");
            this.webb.setDefaultHeader("apikey", ingestionKey);
            setOutputStream(null);
            super.start();
        }
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "localhost";
        }
    }

    @Override
    protected void append(ILoggingEvent ev) {
        if (ev.getLoggerName().equals(this.getClass().getName())) {
            return;
        }
        byte[] bytes = this.encoder.encode(ev);
        if (bytes == null) {
            return;
        }
        try {
            JSONObject meta = new JSONObject();
            meta.put("logger", ev.getLoggerName());
            if (this.sendMDC && !ev.getMDCPropertyMap().isEmpty()) {
                for (Entry<String, String> entry : ev.getMDCPropertyMap().entrySet()) {
                    meta.put(entry.getKey(), entry.getValue());
                }
            }

            JSONObject line = new JSONObject();
            line.put("meta", meta);
            line.put("timestamp", ev.getTimeStamp());
            line.put("level", ev.getLevel().toString());
            line.put("app", this.appName);
            line.put("line", new String(bytes));

            JSONArray lines = new JSONArray();
            lines.put(line);

            JSONObject payload = new JSONObject();
            payload.put("lines", lines);

            Response<JSONObject> response = this.webb.post("?hostname=" + encode(getHostname()) + "&now=" + encode(String.valueOf(System.currentTimeMillis())))
                    .body(payload)
                    .retry(3, true)
                    .asJsonObject();

            if (!response.isSuccess()) {
                log.error("Failed to post data to LogDNA: {} ({})", response.getStatusLine(), response.getStatusCode());
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

}
