package net.minecraft.network.chat;

import com.mojang.logging.LogUtils;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.util.SignatureValidator;
import org.slf4j.Logger;

@FunctionalInterface
public interface SignedMessageValidator {
    Logger LOGGER = LogUtils.getLogger();
    SignedMessageValidator ACCEPT_UNSIGNED = PlayerChatMessage::removeSignature;
    SignedMessageValidator REJECT_ALL = p_314910_ -> {
        LOGGER.error("Received chat message from {}, but they have no chat session initialized and secure chat is enforced", p_314910_.sender());
        return null;
    };

    @Nullable
    PlayerChatMessage updateAndValidate(PlayerChatMessage pMessage);

    public static class KeyBased implements SignedMessageValidator {
        private final SignatureValidator validator;
        private final BooleanSupplier expired;
        @Nullable
        private PlayerChatMessage lastMessage;
        private boolean isChainValid = true;

        public KeyBased(SignatureValidator pValidator, BooleanSupplier pExpired) {
            this.validator = pValidator;
            this.expired = pExpired;
        }

        private boolean validateChain(PlayerChatMessage pMessage) {
            if (pMessage.equals(this.lastMessage)) {
                return true;
            } else if (this.lastMessage != null && !pMessage.link().isDescendantOf(this.lastMessage.link())) {
                LOGGER.error(
                    "Received out-of-order chat message from {}: expected index > {} for session {}, but was {} for session {}",
                    pMessage.sender(),
                    this.lastMessage.link().index(),
                    this.lastMessage.link().sessionId(),
                    pMessage.link().index(),
                    pMessage.link().sessionId()
                );
                return false;
            } else {
                return true;
            }
        }

        private boolean validate(PlayerChatMessage pMessage) {
            if (this.expired.getAsBoolean()) {
                LOGGER.error("Received message from player with expired profile public key: {}", pMessage);
                return false;
            } else if (!pMessage.verify(this.validator)) {
                LOGGER.error("Received message with invalid signature from {}", pMessage.sender());
                return false;
            } else {
                return this.validateChain(pMessage);
            }
        }

        @Nullable
        @Override
        public PlayerChatMessage updateAndValidate(PlayerChatMessage pMessage) {
            this.isChainValid = this.isChainValid && this.validate(pMessage);
            if (!this.isChainValid) {
                return null;
            } else {
                this.lastMessage = pMessage;
                return pMessage;
            }
        }
    }
}
