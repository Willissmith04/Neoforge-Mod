package com.parakeetsandcapybaras.entity.ai;

import com.parakeetsandcapybaras.entity.Capybara;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Makes a baby capybara walk up to a nearby adult capybara and climb onto its back, just like
 * baby striders ride on adult striders.
 */
public class BabyRideAdultGoal extends Goal {
    private static final double SEARCH_RANGE = 8.0;

    private final Capybara baby;
    @Nullable
    private Capybara adult;
    private int timeToRecalcPath;
    private int giveUpTimer;

    public BabyRideAdultGoal(Capybara baby) {
        this.baby = baby;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only try every now and then (about once every 5 seconds on average).
        if (!this.baby.canClimbOnAdult() || this.baby.getRandom().nextInt(reducedTickDelay(100)) != 0) {
            return false;
        }

        List<Capybara> nearby = this.baby.level().getEntitiesOfClass(
                Capybara.class, this.baby.getBoundingBox().inflate(SEARCH_RANGE, 4.0, SEARCH_RANGE), this::isFreeAdult);

        Capybara closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Capybara candidate : nearby) {
            double distance = this.baby.distanceToSqr(candidate);
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }

        this.adult = closest;
        return closest != null;
    }

    private boolean isFreeAdult(Capybara capybara) {
        return capybara != this.baby && !capybara.isBaby() && !capybara.isVehicle() && !capybara.isPassenger() && capybara.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.adult != null
                && this.isFreeAdult(this.adult)
                && this.baby.canClimbOnAdult()
                && this.giveUpTimer > 0
                && this.baby.distanceToSqr(this.adult) < 16.0 * 16.0;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.giveUpTimer = this.adjustedTickDelay(200); // give up after 10 seconds
    }

    @Override
    public void stop() {
        this.adult = null;
        this.baby.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.adult == null) {
            return;
        }
        this.giveUpTimer--;
        this.baby.getLookControl().setLookAt(this.adult, 10.0F, (float) this.baby.getMaxHeadXRot());

        if (this.baby.distanceToSqr(this.adult) < 2.0) {
            // Close enough: hop on!
            this.baby.startRiding(this.adult);
            return;
        }

        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            this.baby.getNavigation().moveTo(this.adult, 1.2);
        }
    }
}
