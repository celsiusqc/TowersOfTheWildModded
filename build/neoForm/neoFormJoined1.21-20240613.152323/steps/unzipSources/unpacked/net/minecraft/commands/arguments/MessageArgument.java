package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;

public class MessageArgument implements SignedArgument<MessageArgument.Message> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");
    static final Dynamic2CommandExceptionType TOO_LONG = new Dynamic2CommandExceptionType(
        (p_341561_, p_341562_) -> Component.translatableEscape("argument.message.too_long", p_341561_, p_341562_)
    );

    public static MessageArgument message() {
        return new MessageArgument();
    }

    public static Component getMessage(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        MessageArgument.Message messageargument$message = pContext.getArgument(pName, MessageArgument.Message.class);
        return messageargument$message.resolveComponent(pContext.getSource());
    }

    public static void resolveChatMessage(CommandContext<CommandSourceStack> pContext, String pKey, Consumer<PlayerChatMessage> pCallback) throws CommandSyntaxException {
        MessageArgument.Message messageargument$message = pContext.getArgument(pKey, MessageArgument.Message.class);
        CommandSourceStack commandsourcestack = pContext.getSource();
        Component component = messageargument$message.resolveComponent(commandsourcestack);
        CommandSigningContext commandsigningcontext = commandsourcestack.getSigningContext();
        PlayerChatMessage playerchatmessage = commandsigningcontext.getArgument(pKey);
        if (playerchatmessage != null) {
            resolveSignedMessage(pCallback, commandsourcestack, playerchatmessage.withUnsignedContent(component));
        } else {
            resolveDisguisedMessage(pCallback, commandsourcestack, PlayerChatMessage.system(messageargument$message.text).withUnsignedContent(component));
        }
    }

    private static void resolveSignedMessage(Consumer<PlayerChatMessage> pCallback, CommandSourceStack pSource, PlayerChatMessage pMessage) {
        MinecraftServer minecraftserver = pSource.getServer();
        CompletableFuture<FilteredText> completablefuture = filterPlainText(pSource, pMessage);
        Component component = minecraftserver.getChatDecorator().decorate(pSource.getPlayer(), pMessage.decoratedContent());
        pSource.getChatMessageChainer().append(completablefuture, p_300688_ -> {
            PlayerChatMessage playerchatmessage = pMessage.withUnsignedContent(component).filter(p_300688_.mask());
            pCallback.accept(playerchatmessage);
        });
    }

    private static void resolveDisguisedMessage(Consumer<PlayerChatMessage> pCallback, CommandSourceStack pSource, PlayerChatMessage pMessage) {
        ChatDecorator chatdecorator = pSource.getServer().getChatDecorator();
        Component component = chatdecorator.decorate(pSource.getPlayer(), pMessage.decoratedContent());
        pCallback.accept(pMessage.withUnsignedContent(component));
    }

    private static CompletableFuture<FilteredText> filterPlainText(CommandSourceStack pSource, PlayerChatMessage pMessage) {
        ServerPlayer serverplayer = pSource.getPlayer();
        return serverplayer != null && pMessage.hasSignatureFrom(serverplayer.getUUID())
            ? serverplayer.getTextFilter().processStreamMessage(pMessage.signedContent())
            : CompletableFuture.completedFuture(FilteredText.passThrough(pMessage.signedContent()));
    }

    public MessageArgument.Message parse(StringReader pReader) throws CommandSyntaxException {
        return MessageArgument.Message.parseText(pReader, true);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static record Message(String text, MessageArgument.Part[] parts) {
        Component resolveComponent(CommandSourceStack pSource) throws CommandSyntaxException {
            return this.toComponent(pSource, net.neoforged.neoforge.common.CommonHooks.canUseEntitySelectors(pSource));
        }

        /**
         * Converts this message into a text component, replacing any selectors in the text with the actual evaluated selector.
         */
        public Component toComponent(CommandSourceStack pSource, boolean pAllowSelectors) throws CommandSyntaxException {
            if (this.parts.length != 0 && pAllowSelectors) {
                MutableComponent mutablecomponent = Component.literal(this.text.substring(0, this.parts[0].start()));
                int i = this.parts[0].start();

                for (MessageArgument.Part messageargument$part : this.parts) {
                    Component component = messageargument$part.toComponent(pSource);
                    if (i < messageargument$part.start()) {
                        mutablecomponent.append(this.text.substring(i, messageargument$part.start()));
                    }

                    mutablecomponent.append(component);
                    i = messageargument$part.end();
                }

                if (i < this.text.length()) {
                    mutablecomponent.append(this.text.substring(i));
                }

                return mutablecomponent;
            } else {
                return Component.literal(this.text);
            }
        }

        /**
         * Parses a message. The algorithm for this is simply to run through and look for selectors, ignoring any invalid selectors in the text (since players may type e.g. "[@]").
         */
        public static MessageArgument.Message parseText(StringReader pReader, boolean pAllowSelectors) throws CommandSyntaxException {
            if (pReader.getRemainingLength() > 256) {
                throw MessageArgument.TOO_LONG.create(pReader.getRemainingLength(), 256);
            } else {
                String s = pReader.getRemaining();
                if (!pAllowSelectors) {
                    pReader.setCursor(pReader.getTotalLength());
                    return new MessageArgument.Message(s, new MessageArgument.Part[0]);
                } else {
                    List<MessageArgument.Part> list = Lists.newArrayList();
                    int i = pReader.getCursor();

                    while (true) {
                        int j;
                        EntitySelector entityselector;
                        while (true) {
                            if (!pReader.canRead()) {
                                return new MessageArgument.Message(s, list.toArray(new MessageArgument.Part[0]));
                            }

                            if (pReader.peek() == '@') {
                                j = pReader.getCursor();

                                try {
                                    EntitySelectorParser entityselectorparser = new EntitySelectorParser(pReader);
                                    entityselector = entityselectorparser.parse();
                                    break;
                                } catch (CommandSyntaxException commandsyntaxexception) {
                                    if (commandsyntaxexception.getType() != EntitySelectorParser.ERROR_MISSING_SELECTOR_TYPE
                                        && commandsyntaxexception.getType() != EntitySelectorParser.ERROR_UNKNOWN_SELECTOR_TYPE) {
                                        throw commandsyntaxexception;
                                    }

                                    pReader.setCursor(j + 1);
                                }
                            } else {
                                pReader.skip();
                            }
                        }

                        list.add(new MessageArgument.Part(j - i, pReader.getCursor() - i, entityselector));
                    }
                }
            }
        }
    }

    public static record Part(int start, int end, EntitySelector selector) {
        /**
         * Runs the selector and returns the component produced by it. This method does not actually appear to ever return null.
         */
        public Component toComponent(CommandSourceStack pSource) throws CommandSyntaxException {
            return EntitySelector.joinNames(this.selector.findEntities(pSource));
        }
    }
}
