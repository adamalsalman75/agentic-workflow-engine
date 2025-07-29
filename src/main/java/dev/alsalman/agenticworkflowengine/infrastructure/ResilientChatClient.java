package dev.alsalman.agenticworkflowengine.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;

@Service
public class ResilientChatClient {
    
    private static final Logger log = LoggerFactory.getLogger(ResilientChatClient.class);
    
    private static final int MAX_RETRIES = 3;
    private static final Duration BASE_DELAY = Duration.ofSeconds(1);
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    private final ChatClient chatClient;
    
    public ResilientChatClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    /**
     * Execute a chat completion with automatic retry logic for rate limiting
     */
    public String call(String operationName, String prompt) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.debug("Executing {} (attempt {}/{})", operationName, attempt, MAX_RETRIES);
                
                return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
                    
            } catch (Exception e) {
                lastException = e;
                
                if (isRateLimitError(e)) {
                    if (attempt < MAX_RETRIES) {
                        Duration delay = calculateDelay(attempt);
                        log.warn("Rate limit hit for {}. Retrying in {} ms (attempt {}/{})", 
                                operationName, delay.toMillis(), attempt, MAX_RETRIES);
                        
                        try {
                            Thread.sleep(delay.toMillis());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting for retry", ie);
                        }
                    } else {
                        log.error("Rate limit exceeded for {} after {} attempts", operationName, MAX_RETRIES);
                    }
                } else {
                    // Non-rate-limit error, don't retry
                    log.error("Non-retryable error in {}: {}", operationName, e.getMessage());
                    break;
                }
            }
        }
        
        throw new RuntimeException("Failed to execute " + operationName + " after " + MAX_RETRIES + " attempts", lastException);
    }
    
    private boolean isRateLimitError(Exception e) {
        // Check for HTTP 429 (Too Many Requests)
        if (e instanceof HttpClientErrorException httpError) {
            return httpError.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
        }
        
        // Check for OpenAI specific rate limit messages
        String message = e.getMessage();
        return message != null && (
            message.contains("rate limit") ||
            message.contains("Rate limit") ||
            message.contains("429") ||
            message.contains("too many requests") ||
            message.contains("quota exceeded")
        );
    }
    
    private Duration calculateDelay(int attempt) {
        long delayMs = (long) (BASE_DELAY.toMillis() * Math.pow(BACKOFF_MULTIPLIER, attempt - 1));
        // Add jitter to prevent thundering herd
        long jitter = (long) (delayMs * 0.1 * Math.random());
        return Duration.ofMillis(delayMs + jitter);
    }
}