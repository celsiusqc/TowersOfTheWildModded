package net.minecraft.commands;

import net.minecraft.network.chat.Component;

public class FunctionInstantiationException extends Exception {
    private final Component messageComponent;

    public FunctionInstantiationException(Component pMessageComponent) {
        super(pMessageComponent.getString());
        this.messageComponent = pMessageComponent;
    }

    public Component messageComponent() {
        return this.messageComponent;
    }
}
