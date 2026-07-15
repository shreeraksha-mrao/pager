package com.pager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pager.entity.BotState;
import com.pager.entity.DeclineReasonType;
import com.pager.repository.BotStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Long-polls Telegram's getUpdates endpoint (no public webhook needed — works
 * from behind the laptop's NAT) and dispatches accept/decline/session
 * callbacks plus free-text decline-reason capture and /start registration.
 */
@Service
public class TelegramPollingService {

    private static final Logger log = LoggerFactory.getLogger(TelegramPollingService.class);
    private static final String OFFSET_KEY = "telegram_update_offset";
    private static final String AWAITING_REASON_PREFIX = "awaiting_decline_reason:";

    private static final List<String> PRESET_DECLINE_REASONS = List.of(
            "Too busy", "Not feeling it", "In a meeting", "Low energy");

    private final RestClient restClient = RestClient.create();
    private final BotStateRepository botStateRepository;
    private final PageEventService pageEventService;
    private final StudySessionService studySessionService;
    private final TelegramMessagingService telegramMessagingService;
    private final ObjectMapper objectMapper;

    @Value("${pager.telegram.bot-token:}")
    private String botToken;

    public TelegramPollingService(BotStateRepository botStateRepository,
                                   PageEventService pageEventService,
                                   StudySessionService studySessionService,
                                   TelegramMessagingService telegramMessagingService,
                                   ObjectMapper objectMapper) {
        this.botStateRepository = botStateRepository;
        this.pageEventService = pageEventService;
        this.studySessionService = studySessionService;
        this.telegramMessagingService = telegramMessagingService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${pager.telegram.polling-interval-ms:2000}")
    public void poll() {
        if (botToken == null || botToken.isBlank()) {
            return;
        }
        try {
            long offset = currentOffset();
            String url = "https://api.telegram.org/bot" + botToken + "/getUpdates?offset=" + offset + "&timeout=0";
            JsonNode response = restClient.get().uri(url).retrieve().body(JsonNode.class);
            if (response == null || !response.path("ok").asBoolean(false)) {
                return;
            }
            for (JsonNode update : response.path("result")) {
                handleUpdate(update);
                long updateId = update.path("update_id").asLong();
                saveOffset(updateId + 1);
            }
        } catch (Exception e) {
            log.error("Telegram polling error", e);
        }
    }

    @Transactional
    void handleUpdate(JsonNode update) {
        if (update.has("callback_query")) {
            handleCallback(update.path("callback_query"));
        } else if (update.has("message")) {
            handleMessage(update.path("message"));
        }
    }

    private void handleMessage(JsonNode message) {
        String chatId = message.path("chat").path("id").asText();
        String text = message.path("text").asText("");

        if ("/start".equalsIgnoreCase(text.trim())) {
            telegramMessagingService.registerChatId(chatId);
            telegramMessagingService.sendPlainMessage(chatId,
                    "\u2705 Registered! You'll receive productivity pages here.");
            return;
        }

        Optional<String> awaitingEventId = getAwaitingReasonEventId(chatId);
        if (awaitingEventId.isPresent()) {
            Long pageEventId = Long.parseLong(awaitingEventId.get());
            pageEventService.decline(pageEventId, text, DeclineReasonType.FREE_TEXT);
            clearAwaitingReason(chatId);
            telegramMessagingService.sendPlainMessage(chatId, "Got it — reason recorded.");
        }
    }

    private void handleCallback(JsonNode callback) {
        String data = callback.path("data").asText("");
        String chatId = callback.path("message").path("chat").path("id").asText();
        String[] parts = data.split(":", 3);
        if (parts.length < 2) return;
        String action = parts[0];

        try {
            switch (action) {
                case "accept" -> pageEventService.accept(Long.parseLong(parts[1]));
                case "decline" -> {
                    var event = pageEventService.getOrThrow(Long.parseLong(parts[1]));
                    telegramMessagingService.showDeclineReasonOptions(event, PRESET_DECLINE_REASONS);
                }
                case "declinereason" -> {
                    Long pageEventId = Long.parseLong(parts[1]);
                    String reason = parts.length > 2 ? parts[2] : "Other";
                    pageEventService.decline(pageEventId, reason, DeclineReasonType.PRESET);
                }
                case "declineother" -> {
                    setAwaitingReason(chatId, parts[1]);
                    telegramMessagingService.sendPlainMessage(chatId, "Please type your decline reason:");
                }
                case "sessioncomplete" -> {
                    Long sid = Long.parseLong(parts[1]);
                    studySessionService.confirmCompleted(sid);
                    telegramMessagingService.editSessionToCompleted(studySessionService.getWithTask(sid));
                }
                case "sessionextend" -> {
                    Long sid = Long.parseLong(parts[1]);
                    studySessionService.extend(sid, 30);
                    telegramMessagingService.editSessionToExtended(studySessionService.getWithTask(sid), 30);
                }
                case "sessionabandon" -> {
                    Long sid = Long.parseLong(parts[1]);
                    studySessionService.abandon(sid);
                    telegramMessagingService.editSessionToAbandoned(studySessionService.getWithTask(sid));
                }
                default -> log.warn("Unknown callback action: {}", action);
            }
        } catch (Exception e) {
            log.error("Failed to handle Telegram callback '{}'", data, e);
        }
    }

    private long currentOffset() {
        return botStateRepository.findById(OFFSET_KEY).map(s -> Long.parseLong(s.getValue())).orElse(0L);
    }

    private void saveOffset(long offset) {
        BotState state = botStateRepository.findById(OFFSET_KEY).orElse(new BotState(OFFSET_KEY, null));
        state.setValue(String.valueOf(offset));
        botStateRepository.save(state);
    }

    private Optional<String> getAwaitingReasonEventId(String chatId) {
        return botStateRepository.findById(AWAITING_REASON_PREFIX + chatId).map(BotState::getValue);
    }

    private void setAwaitingReason(String chatId, String pageEventId) {
        botStateRepository.save(new BotState(AWAITING_REASON_PREFIX + chatId, pageEventId));
    }

    private void clearAwaitingReason(String chatId) {
        botStateRepository.deleteById(AWAITING_REASON_PREFIX + chatId);
    }
}
