package com.seaquake6324.civitas.domain.security;

import java.util.LinkedHashMap;

/** Central first-pass willingness hypothesis; callers retain every component for diagnostics. */
public final class GuardAssignmentRules {
    public GuardWillingness evaluate(Inputs input, Weights weights) {
        LinkedHashMap<String, Double> parts = new LinkedHashMap<>();
        add(parts, "settlement", input.settlementWillingness(), weights.settlement());
        add(parts, "safety_need", input.safetyNeed(), weights.safety());
        add(parts, "weapon", input.equipment().basicWeapon() ? 100 : 0, weights.weapon());
        add(parts, "armor", input.equipment().armorPieces() * 25, weights.armor());
        add(parts, "shield", input.equipment().shield() ? 100 : 0, weights.shield());
        add(parts, "shift", input.shift() == GuardShift.DAY ? 100 : 50, weights.shift());
        add(parts, "duty_reliability", input.dutyReliability(), weights.dutyReliability());
        return new GuardWillingness(parts.values().stream().mapToDouble(Double::doubleValue).sum(), parts);
    }

    private static void add(LinkedHashMap<String, Double> parts, String name, double raw, double weight) {
        parts.put(name, clamp(raw) * weight);
    }
    private static double clamp(double value) { return Math.max(0, Math.min(100, value)); }

    public record Inputs(double settlementWillingness, double safetyNeed, GuardEquipment equipment,
                         GuardShift shift, double dutyReliability) {
        public Inputs { if (equipment == null || shift == null) throw new IllegalArgumentException("missing guard input"); }
    }
    public record Weights(double settlement, double safety, double weapon, double armor, double shield,
                          double shift, double dutyReliability) {
        public Weights {
            double total=settlement+safety+weapon+armor+shield+shift+dutyReliability;
            if (!Double.isFinite(total) || Math.abs(total-1.0)>0.000001 || settlement<0 || safety<0 || weapon<0 || armor<0 || shield<0 || shift<0 || dutyReliability<0)
                throw new IllegalArgumentException("guard willingness weights must sum to one");
        }
    }
}
