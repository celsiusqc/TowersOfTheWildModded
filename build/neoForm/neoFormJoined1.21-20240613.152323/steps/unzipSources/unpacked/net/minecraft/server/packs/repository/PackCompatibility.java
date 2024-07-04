package net.minecraft.server.packs.repository;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.InclusiveRange;

public enum PackCompatibility {
    TOO_OLD("old"),
    TOO_NEW("new"),
    COMPATIBLE("compatible");

    private final Component description;
    private final Component confirmation;

    private PackCompatibility(String pType) {
        this.description = Component.translatable("pack.incompatible." + pType).withStyle(ChatFormatting.GRAY);
        this.confirmation = Component.translatable("pack.incompatible.confirm." + pType);
    }

    public boolean isCompatible() {
        return this == COMPATIBLE;
    }

    public static PackCompatibility forVersion(InclusiveRange<Integer> pRange, int pVersion) {
        if (pRange.maxInclusive() < pVersion) {
            return TOO_OLD;
        } else {
            return pVersion < pRange.minInclusive() ? TOO_NEW : COMPATIBLE;
        }
    }

    public Component getDescription() {
        return this.description;
    }

    public Component getConfirmation() {
        return this.confirmation;
    }
}
