package net.minecraft.commands;

@FunctionalInterface
public interface CommandResultCallback {
    CommandResultCallback EMPTY = new CommandResultCallback() {
        @Override
        public void onResult(boolean p_309581_, int p_309698_) {
        }

        @Override
        public String toString() {
            return "<empty>";
        }
    };

    void onResult(boolean pSuccess, int pResult);

    default void onSuccess(int pResult) {
        this.onResult(true, pResult);
    }

    default void onFailure() {
        this.onResult(false, 0);
    }

    static CommandResultCallback chain(CommandResultCallback pFirst, CommandResultCallback pSecond) {
        if (pFirst == EMPTY) {
            return pSecond;
        } else {
            return pSecond == EMPTY ? pFirst : (p_309648_, p_309546_) -> {
                pFirst.onResult(p_309648_, p_309546_);
                pSecond.onResult(p_309648_, p_309546_);
            };
        }
    }
}
