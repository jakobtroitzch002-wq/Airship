package com.airship;

public final class AirshipLift {
    public static final int BLOCKS_PER_BALLOON = AirshipConfig.get().blocksPerBalloon;

    private AirshipLift() {
    }

    /** Lift the ship needs: every ship block has to be carried (see {@link ModBlocks#liftOf}). */
    public static int requiredLift(int blockCount) {
        return Math.max(1, blockCount);
    }

    public static boolean hasEnoughLift(int lift, int requiredLift) {
        return lift >= requiredLift;
    }
}
