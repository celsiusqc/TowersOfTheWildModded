package net.minecraft.core.dispenser;

public abstract class OptionalDispenseItemBehavior extends DefaultDispenseItemBehavior {
    private boolean success = true;

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean pSuccess) {
        this.success = pSuccess;
    }

    @Override
    protected void playSound(BlockSource pBlockSource) {
        pBlockSource.level().levelEvent(this.isSuccess() ? 1000 : 1001, pBlockSource.pos(), 0);
    }
}
