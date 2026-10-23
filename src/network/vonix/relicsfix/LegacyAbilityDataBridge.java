package network.vonix.relicsfix;

import java.lang.reflect.Method;

/**
 * Reflective bridge to the Relics 0.12.x replacement API. Deliberately free of any symbolic
 * reference to Relics classes, so a future Relics reshuffle degrades the shim to "false"
 * instead of throwing a linkage error inside a damage event.
 *
 * Relics 0.11.x AbilityData.isRankModifierUnlocked(String) was moved unchanged to
 * AbilityRankModifierData.isUnlocked() in 0.12.0 (verified in bytecode: identical
 * getTemplate() -> invertFrom(rankModifiers) -> rank >= Collections.max(ranks) logic).
 */
public final class LegacyAbilityDataBridge {
    private static volatile Method getRankModifierData;
    private static volatile Method isUnlocked;
    private static volatile boolean unavailable;

    private LegacyAbilityDataBridge() {}

    public static boolean isRankModifierUnlocked(Object abilityData, String rankModifier) {
        if (abilityData == null || rankModifier == null || unavailable) {
            return false;
        }
        try {
            Method getter = getRankModifierData;
            if (getter == null) {
                getter = abilityData.getClass().getMethod("getRankModifierData", String.class);
                getRankModifierData = getter;
            }
            Object rankModifierData = getter.invoke(abilityData, rankModifier);
            if (rankModifierData == null) {
                return false;
            }
            Method unlocked = isUnlocked;
            if (unlocked == null) {
                unlocked = rankModifierData.getClass().getMethod("isUnlocked");
                isUnlocked = unlocked;
            }
            return Boolean.TRUE.equals(unlocked.invoke(rankModifierData));
        } catch (NoSuchMethodException | IllegalAccessException | LinkageError fatal) {
            // The replacement API is genuinely gone: stop trying, keep the game alive.
            unavailable = true;
            RelicsFixMod.LOG.error("[vonix_relicsfix] Relics replacement API unavailable, shim now always false", fatal);
            return false;
        } catch (Throwable transientFailure) {
            // Per-call failure (e.g. uninitialised data): answer false, but keep the shim live.
            RelicsFixMod.LOG.debug("[vonix_relicsfix] legacy rank-modifier query failed for '{}'", rankModifier, transientFailure);
            return false;
        }
    }
}
