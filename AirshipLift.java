package com.airship;

public final class AirshipLift {
    public static final int BLOCKS_PER_BALLOON = AirshipConfig.get().blocksPerBalloon;

    private AirshipLift() {
    }

    /** Lift the ship needs, in half balloons: one Balloon (2) per {@code BLOCKS_PER_BALLOON} ship blocks. */
    public static int requiredLift(int blockCount) {
        int balloons = Math.max(1, (blockCount + BLOCKS_PER_BALLOON - 1) / BLOCKS_PER_BALLOON);
        return balloons * ModBlocks.BALLOON_LIFT;
    }

    public static boolean hasEnoughLift(int lift, int requiredLift) {
        return lift >= requiredLift;
    }
}
