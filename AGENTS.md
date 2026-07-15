# Project Agent Notes

1. Avoid player-facing messages unless they are necessary. Choose the feedback style by context instead of forcing one mode everywhere: for feedback caused by operations inside a block/container UI, prefer a polished in-screen transient notification; for direct world interaction, prefer a HUD/actionbar-style transient message near the lower center; for local pointer/canvas validation, prefer a subtle cursor-adjacent or cell-local visual hint that disappears when the cursor leaves. Do not send chat messages for routine feedback.
2. At the end of each implementation task, check whether the client starts without startup errors.
3. Never use the chat feed for Civitas information, success notices, routine status, or failure feedback. Design failure feedback explicitly: use an in-screen transient notice for GUI/container operations, an Action Bar notice for direct world interaction, and a subtle cursor-adjacent or cell-local hint for pointer/canvas validation. Server-wide announcements must be configurable and disabled by default; if enabled, render them as a non-chat HUD/system overlay.

## Five-layer architecture

Keep dependencies directed from the outer layers toward the domain, and do not put game rules in rendering, event, persistence, or compatibility adapters.

1. Presentation (`presentation`): GUI screens, HUD/Action Bar feedback, map overlays, particles, boundary rendering, sounds, and founding animation.
2. Application (`application`): use cases such as founding a city, expanding territory, joining a city, removing a city, and their orchestration/validation results.
3. Domain (`domain`): city aggregates, membership, territory, names/colors, and civilization/ceremony rules. This layer must not depend on Minecraft client classes.
4. Infrastructure (`infrastructure`): `SavedData`, network payloads, configuration, commands, registries, and event listeners that adapt Minecraft/NeoForge to the application layer.
5. Compatibility (`compatibility`): optional map-mod integrations, NPC-mod integrations, and permission-provider adapters. Compatibility code must remain optional and must not be required for the core mod to load.

## Gameplay decision gates

Before implementing or materially changing population, territory expansion, abandonment, decay, defense, or warfare, require a short gameplay contract that states:

- what the player does;
- how the system communicates the result;
- what counts as success;
- the obvious abuse cases;
- what is explicitly out of scope.

Do not invent missing gameplay rules merely to complete an implementation. If a missing decision would materially change the mechanic, stop and request a gameplay decision first.

## Evidence and balance

Treat numeric formulas, thresholds, weights, cooldowns, spawn limits, multipliers, and default configuration values as testable first-pass hypotheses rather than settled design. Keep balance rules centralized in the domain or configuration instead of scattering hidden constants through event, mixin, and rendering code. Reproducible playtest evidence takes precedence over an earlier design document.

When changing spawning, classification, civilization, activity, or other world-scale systems, define representative test scenarios and observable acceptance criteria. Record enough diagnostic evidence to explain a balance change instead of relying only on subjective impressions.

## Explainability and diagnostics

Every invisible derived gameplay value must remain explainable. Administrator/developer diagnostics must be able to expose the relevant raw inputs, component scores, target value, current value, modifiers, classification reasons, and final gameplay effect.

Player-facing interfaces may show only tiers or concise summaries, but diagnostics must retain the complete calculation path. Diagnostics must consume domain/application results rather than reimplementing game rules in presentation code. Extend the existing opt-in debug interface when practical instead of creating unrelated diagnostic surfaces or sending debug output through chat.

## Persistence and migration

Every persistent Civitas root format must store an explicit schema version. Add forward migration code and regression tests whenever a persisted structure changes, including city data, surface and underground civilization data, activity summaries, and spawn-budget indexes.

Never use deleting the test world as the migration strategy. Malformed records must fail safely with useful diagnostics and must not prevent otherwise valid Civitas data from loading.

## Proportional anti-abuse and bounded work

Before adding anti-abuse tracking, compare its implementation, persistence, memory, and runtime cost with the actual harm of the exploit. Prefer coarse limits, cooldowns, local validation, diminishing returns, or making ordinary play more effective. Do not introduce long-lived event histories or complex cross-system tracking without evidence that simpler measures are insufficient.

World scans and classifiers must have explicit work bounds, bounded caches, local invalidation, and observable performance metrics. Measure their cost in representative worlds before increasing their radius, frequency, or retained history.

## Core gameplay direction

The core loop is: players establish a city, create civility through real construction, maintain it through continued activity, and expand civilized space in a dangerous world. New mandatory systems must strengthen this loop.