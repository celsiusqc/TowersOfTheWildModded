package net.minecraft.world.level.border;

public interface BorderChangeListener {
    void onBorderSizeSet(WorldBorder pBorder, double pSize);

    void onBorderSizeLerping(WorldBorder pBorder, double pOldSize, double pNewSize, long pTime);

    void onBorderCenterSet(WorldBorder pBorder, double pX, double pZ);

    void onBorderSetWarningTime(WorldBorder pBorder, int pWarningTime);

    void onBorderSetWarningBlocks(WorldBorder pBorder, int pWarningBlocks);

    void onBorderSetDamagePerBlock(WorldBorder pBorder, double pDamagePerBlock);

    void onBorderSetDamageSafeZOne(WorldBorder pBorder, double pDamageSafeZone);

    public static class DelegateBorderChangeListener implements BorderChangeListener {
        private final WorldBorder worldBorder;

        public DelegateBorderChangeListener(WorldBorder pWorldBorder) {
            this.worldBorder = pWorldBorder;
        }

        @Override
        public void onBorderSizeSet(WorldBorder pBorder, double pNewSize) {
            this.worldBorder.setSize(pNewSize);
        }

        @Override
        public void onBorderSizeLerping(WorldBorder pBorder, double pOldSize, double pNewSize, long pTime) {
            this.worldBorder.lerpSizeBetween(pOldSize, pNewSize, pTime);
        }

        @Override
        public void onBorderCenterSet(WorldBorder pBorder, double pX, double pZ) {
            this.worldBorder.setCenter(pX, pZ);
        }

        @Override
        public void onBorderSetWarningTime(WorldBorder pBorder, int pNewTime) {
            this.worldBorder.setWarningTime(pNewTime);
        }

        @Override
        public void onBorderSetWarningBlocks(WorldBorder pBorder, int pNewDistance) {
            this.worldBorder.setWarningBlocks(pNewDistance);
        }

        @Override
        public void onBorderSetDamagePerBlock(WorldBorder pBorder, double pNewAmount) {
            this.worldBorder.setDamagePerBlock(pNewAmount);
        }

        @Override
        public void onBorderSetDamageSafeZOne(WorldBorder pBorder, double pNewSize) {
            this.worldBorder.setDamageSafeZone(pNewSize);
        }
    }
}
