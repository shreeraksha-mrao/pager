package com.pager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pager.entity.AppSetting;
import com.pager.entity.PageEvent;
import com.pager.entity.StudySession;
import com.pager.repository.AppSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

/**
 * Thin wrapper around the Telegram Bot HTTP API for sending and editing
 * messages with inline keyboards. Long-polling (getUpdates) lives in
 * {@link TelegramPollingService}; this class only sends/edits.
 */
@Service
public class TelegramMessagingService {

    private static final Logger log = LoggerFactory.getLogger(TelegramMessagingService.class);
    public static final String CHAT_ID_SETTING_KEY = "telegram_chat_id";

    private final RestClient restClient;
    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;

    @Value("${pager.telegram.bot-token:}")
    private String botToken;

    public TelegramMessagingService(AppSettingRepository appSettingRepository, ObjectMapper objectMapper) {
        this.appSettingRepository = appSettingRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public Optional<String> resolveChatId() {
        return appSettingRepository.findById(CHAT_ID_SETTING_KEY).map(AppSetting::getValue);
    }

    public void registerChatId(String chatId) {
        AppSetting setting = appSettingRepository.findById(CHAT_ID_SETTING_KEY).orElse(new AppSetting());
        setting.setKey(CHAT_ID_SETTING_KEY);
        setting.setValue(chatId);
        appSettingRepository.save(setting);
        log.info("Registered Telegram chat id {}", chatId);
    }

    private boolean isConfigured() {
        return botToken != null && !botToken.isBlank();
    }

    private String apiUrl(String method) {
        return "https://api.telegram.org/bot" + botToken + "/" + method;
    }

    /** Sends the initial page notification with Accept/Decline buttons. Returns the Telegram message id. */
    public String sendPageMessage(PageEvent event) {
        String chatId = event.getTelegramChatId() != null ? event.getTelegramChatId() : resolveChatId().orElse(null);
        if (chatId == null || !isConfigured()) {
            log.warn("Telegram not configured or no chat registered — skipping send for page {}", event.getId());
            return null;
        }
        String text = String.format(
                "\uD83D\uDD14 *Time to work on %s*\nDuration: %d min\nCurrent debt: %d min behind",
                escape(event.getTask().getName()), event.getDurationMinutes(), event.getDebtSnapshotMinutes());

        ArrayNode keyboard = objectMapper.createArrayNode();
        ArrayNode row = keyboard.addArray();
        row.add(button("\u2705 Accept", "accept:" + event.getId()));
        row.add(button("\u274C Decline", "decline:" + event.getId()));

        return sendMessage(chatId, text, keyboard);
    }

    public void showDeclineReasonOptions(PageEvent event, java.util.List<String> presetReasons) {
        String chatId = event.getTelegramChatId();
        if (chatId == null || event.getTelegramMessageId() == null) return;
        ArrayNode keyboard = objectMapper.createArrayNode();
        for (String reason : presetReasons) {
            ArrayNode row = keyboard.addArray();
            row.add(button(reason, "declinereason:" + event.getId() + ":" + reason));
        }
        ArrayNode otherRow = keyboard.addArray();
        otherRow.add(button("\uD83D\uDCDD Other (type reason)", "declineother:" + event.getId()));
        editMessageMarkup(chatId, event.getTelegramMessageId(), keyboard);
    }

    public void editToAccepted(PageEvent event) {
        editFinalState(event.getTelegramChatId(), event.getTelegramMessageId(),
                "\u2705 Accepted — " + event.getTask().getName() + " (" + event.getDurationMinutes() + " min)");
    }

    public void editToDeclined(PageEvent event) {
        String reason = event.getDeclineReason() != null ? " — " + event.getDeclineReason() : "";
        editFinalState(event.getTelegramChatId(), event.getTelegramMessageId(),
                "\u274C Declined — " + event.getTask().getName() + reason);
    }

    public void editToMissed(PageEvent event) {
        editFinalState(event.getTelegramChatId(), event.getTelegramMessageId(),
                "\u231B Missed — " + event.getTask().getName());
    }

    /** Sends the follow-up check-in and returns the Telegram message id (for later editing on response), or null if unsent. */
    public String sendFollowUpCheckin(StudySession session, String chatId) {
        if (chatId == null || !isConfigured()) return null;
        String text = String.format("Did you complete your %d-min %s session?",
                session.getDurationMinutes() != null && session.getDurationMinutes() > 0
                        ? session.getDurationMinutes()
                        : (int) java.time.Duration.between(session.getStartTime(), session.getEndTime()).toMinutes(),
                escape(session.getTask().getName()));
        ArrayNode keyboard = objectMapper.createArrayNode();
        ArrayNode row = keyboard.addArray();
        row.add(button("\u2705 Completed", "sessioncomplete:" + session.getId()));
        row.add(button("\uD83D\uDD01 Extend 30m", "sessionextend:" + session.getId()));
        row.add(button("\u270B Abandoned", "sessionabandon:" + session.getId()));
        return sendMessage(chatId, text, keyboard);
    }

    /** Edits the check-in message to confirm the session was marked completed, and how many minutes it credited. */
    public void editSessionToCompleted(StudySession session) {
        editFinalState(session.getCheckinChatId(), session.getCheckinMessageId(),
                "\u2705 Completed — " + escape(session.getTask().getName()) + " logged "
                        + session.getDurationMinutes() + " min (counts toward today's hours & reduces debt).");
    }

    /** Edits the check-in message to confirm the session was extended — no minutes credited yet. */
    public void editSessionToExtended(StudySession session, int extraMinutes) {
        editFinalState(session.getCheckinChatId(), session.getCheckinMessageId(),
                "\uD83D\uDD01 Extended by " + extraMinutes + " min — " + escape(session.getTask().getName())
                        + " session continues. You'll get another check-in when the new time is up.");
    }

    /** Edits the check-in message to confirm the session was abandoned — 0 minutes credited, debt unchanged. */
    public void editSessionToAbandoned(StudySession session) {
        editFinalState(session.getCheckinChatId(), session.getCheckinMessageId(),
                "\u270B Abandoned — " + escape(session.getTask().getName())
                        + " — 0 min logged, debt unchanged. Log it manually later if you finish independently.");
    }

    public String sendPlainMessage(String chatId, String text) {
        if (chatId == null || !isConfigured()) return null;
        return sendMessage(chatId, text, null);
    }

    private String sendMessage(String chatId, String text, ArrayNode keyboard) {
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "Markdown");
            if (keyboard != null) {
                ObjectNode markup = objectMapper.createObjectNode();
                markup.set("inline_keyboard", keyboard);
                body.put("reply_markup", markup);
            }
            JsonNode response = restClient.post().uri(apiUrl("sendMessage"))
                    .body(body).retrieve().body(JsonNode.class);
            if (response != null && response.path("ok").asBoolean(false)) {
                return response.path("result").path("message_id").asText();
            }
            log.warn("Telegram sendMessage failed: {}", response);
        } catch (Exception e) {
            log.error("Failed to send Telegram message", e);
        }
        return null;
    }

    private void editMessageMarkup(String chatId, String messageId, ArrayNode keyboard) {
        try {
            ObjectNode markup = objectMapper.createObjectNode();
            markup.set("inline_keyboard", keyboard);
            Map<String, Object> body = Map.of(
                    "chat_id", chatId, "message_id", Long.parseLong(messageId), "reply_markup", markup);
            restClient.post().uri(apiUrl("editMessageReplyMarkup")).body(body).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to edit Telegram message markup", e);
        }
    }

    private void editFinalState(String chatId, String messageId, String text) {
        if (chatId == null || messageId == null || !isConfigured()) return;
        try {
            Map<String, Object> body = Map.of(
                    "chat_id", chatId, "message_id", Long.parseLong(messageId), "text", text);
            restClient.post().uri(apiUrl("editMessageText")).body(body).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to edit Telegram message text", e);
        }
    }

    private ObjectNode button(String text, String callbackData) {
        ObjectNode btn = objectMapper.createObjectNode();
        btn.put("text", text);
        btn.put("callback_data", callbackData);
        return btn;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("_", "\\_").replace("*", "\\*");
    }
}
