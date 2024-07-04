package net.minecraft.client.resources;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record PlayerSkin(
    ResourceLocation texture,
    @Nullable String textureUrl,
    @Nullable ResourceLocation capeTexture,
    @Nullable ResourceLocation elytraTexture,
    PlayerSkin.Model model,
    boolean secure
) {
    @OnlyIn(Dist.CLIENT)
    public static enum Model {
        SLIM("slim"),
        WIDE("default");

        private final String id;

        private Model(String pId) {
            this.id = pId;
        }

        public static PlayerSkin.Model byName(@Nullable String pName) {
            if (pName == null) {
                return WIDE;
            } else {
                byte b0 = -1;
                switch (pName.hashCode()) {
                    case 3533117:
                        if (pName.equals("slim")) {
                            b0 = 0;
                        }
                    default:
                        return switch (b0) {
                            case 0 -> SLIM;
                            default -> WIDE;
                        };
                }
            }
        }

        public String id() {
            return this.id;
        }
    }
}
