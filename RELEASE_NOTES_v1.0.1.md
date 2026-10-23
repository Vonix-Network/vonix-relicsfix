## v1.0.1 - temporary server-side compatibility shim

Fixes `java.lang.NoSuchMethodError: it.hurts.sskirillss.relics.api.relics.data.AbilityData.isRankModifierUnlocked(java.lang.String)`
thrown from **Reliquified Ars Nouveau 0.7.1** when running against **Relics 0.12.0**.

### Cause
Relics 0.12.0 removed `AbilityData.isRankModifierUnlocked(String)` and moved the identical logic onto
`AbilityRankModifierData.isUnlocked()` (verified by bytecode comparison against Relics 0.11.16 / 0.12.0 / 0.12.9).
Reliquified Ars Nouveau 0.7.1 still calls the removed method from eight classes, so every one of those code
paths is a latent crash - the reported one fired from `CloakOfConcealmentItem$CommonEvents.onLivingDamagePre`
on fall damage.

### Fix
One method is merged back onto `AbilityData` via Mixin, delegating reflectively to the 0.12.x replacement.
No gameplay values are changed, no other mod is patched or republished, and the jar is server-side only -
clients can stay on the pack's shipped versions.

### Verified
- Control run (same classes, mixin disabled): `SELF-TEST ... = MISSING` - defect reproduced.
- Fixed run: `SELF-TEST AbilityData.isRankModifierUnlocked(Ljava/lang/String;)Z = PRESENT (public+boolean=true)`,
  clean boot, clean stop, no crash reports, zero `NoSuchMethodError`.
- Not verified: a live damage-event call (requires a connected player wearing an affected relic).

### Remove when
Reliquified Ars Nouveau >= 0.8.0 is installed (upstream fixed it), or Relics is rolled back to 0.11.x
(the merge would then collide with the real method).

Boot gate: the log must contain
`[vonix_relicsfix] SELF-TEST AbilityData.isRankModifierUnlocked(Ljava/lang/String;)Z = PRESENT`

sha256: `ba907b603ebeff6c94890ec04207aa95524117a756ec4f682cef69c52d79d338`
