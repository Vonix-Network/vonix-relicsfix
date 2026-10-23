package network.vonix.relicsfix.mixin;

import network.vonix.relicsfix.LegacyAbilityDataBridge;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Merges the removed Relics 0.11.x method back onto AbilityData.
 *
 * The method is intentionally not annotated @Unique/@Shadow: a plain method declared in a
 * mixin class is merged into the target class, which is exactly what a missing-symbol
 * compatibility fix requires. Relics 0.12.x does not declare it, so there is no collision.
 */
@Mixin(targets = "it.hurts.sskirillss.relics.api.relics.data.AbilityData", remap = false)
public class AbilityDataLegacyApiMixin {

    public boolean isRankModifierUnlocked(String rankModifier) {
        return LegacyAbilityDataBridge.isRankModifierUnlocked(this, rankModifier);
    }
}
