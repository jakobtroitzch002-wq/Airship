package com.airship;

public final class AirshipLift {
    public static final int BLOCKS_PER_BALLOON = AirshipConfig.get().blocksPerBalloon;

    private AirshipLift() {
    }

    public static int requiredBalloons(int blockCount) {
        return Math.max(1, (blockCount + BLOCKS_PER_BALLOON - 1) / BLOCKS_PER_BALLOON);
    }

    public static boolean hasEnoughLift(int balloonCount, int requiredBalloons) {
        return balloonCount >= requiredBalloons;
    }
}
