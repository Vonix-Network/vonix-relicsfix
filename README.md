# vonix_relicsfix (temporary server-side shim)

Fixes: `java.lang.NoSuchMethodError: AbilityData.isRankModifierUnlocked(String)`
thrown from `reliquified_ars_nouveau 0.7.1` against `relics 0.12.0`.

## What it does
Merges the single method Relics removed in 0.12.0 back onto
`it.hurts.sskirillss.relics.api.relics.data.AbilityData`, delegating to the 0.12.x
replacement `getRankModifierData(id).isUnlocked()` (bytecode-identical logic, verified
with javap against relics 0.11.16 and 0.12.0/0.12.9).

No gameplay values changed. Server-side only; safe to run while clients keep 0.7.1.

## Install
Drop `vonix_relicsfix-1.0.1.jar` into the server `mods/` folder and restart.

Boot gate - the log must show:
    [vonix_relicsfix] SELF-TEST AbilityData.isRankModifierUnlocked(Ljava/lang/String;)Z = PRESENT (public+boolean=true)
If it says MISSING, the shim did not apply and the crash will still occur.

## Remove when
Reliquified Ars Nouveau >= 0.8.0 is installed (upstream fixed it), or Relics is rolled back to 0.11.x
(the merge collides with the real method and will fail class load).

## Verified
- Local NeoForge 21.1.228 / MC 1.21.1 testbed, relics 0.12.0 + RAN 0.7.1 + ars_nouveau 5.11.3.
- Control run (mixin disabled): SELF-TEST ... = MISSING  <-- reproduces the defect.
- Fixed run: SELF-TEST ... = PRESENT (public+boolean=true), clean boot, clean stop, no crash-reports,
  zero NoSuchMethodError.
- NOT verified: a live damage-event call (needs a connected player wearing an affected relic).
  Confirm on the real server after the first player damage event.

sha256 vonix_relicsfix-1.0.1.jar:
ba907b603ebeff6c94890ec04207aa95524117a756ec4f682cef69c52d79d338
