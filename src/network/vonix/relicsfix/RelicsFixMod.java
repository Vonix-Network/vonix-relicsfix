package network.vonix.relicsfix;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Server-side temporary compatibility shim.
 *
 * Reliquified Ars Nouveau 0.7.1 calls
 *   it.hurts.sskirillss.relics.api.relics.data.AbilityData#isRankModifierUnlocked(String)Z
 * which Relics removed in 0.12.0 (moved verbatim onto AbilityRankModifierData#isUnlocked()).
 * This mod merges that one method back onto AbilityData and delegates it.
 *
 * Remove this jar once Reliquified Ars Nouveau >= 0.8.0 is installed.
 */
@Mod("vonix_relicsfix")
public final class RelicsFixMod {
    public static final Logger LOG = LoggerFactory.getLogger("vonix_relicsfix");

    private static final String TARGET = "it.hurts.sskirillss.relics.api.relics.data.AbilityData";

    public RelicsFixMod(IEventBus modEventBus) {
        modEventBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        try {
            Class<?> c = Class.forName(TARGET, false, getClass().getClassLoader());
            Method legacy = c.getDeclaredMethod("isRankModifierUnlocked", String.class);
            boolean exact = legacy.getReturnType() == boolean.class
                    && java.lang.reflect.Modifier.isPublic(legacy.getModifiers());
            LOG.info("[vonix_relicsfix] SELF-TEST AbilityData.isRankModifierUnlocked(Ljava/lang/String;)Z = {} (public+boolean={})",
                    "PRESENT", exact);
            Method replacement = c.getDeclaredMethod("getRankModifierData", String.class);
            LOG.info("[vonix_relicsfix] SELF-TEST replacement AbilityData.getRankModifierData(String) present, returns {}",
                    replacement.getReturnType().getName());
            if (!exact) {
                LOG.error("[vonix_relicsfix] shim landed with the wrong shape - the NoSuchMethodError is NOT fixed");
            }
        } catch (NoSuchMethodException e) {
            LOG.error("[vonix_relicsfix] SELF-TEST FAILED: legacy method absent on AbilityData - crash will still occur", e);
        } catch (Throwable t) {
            LOG.error("[vonix_relicsfix] SELF-TEST ERROR", t);
        }
    }
}
