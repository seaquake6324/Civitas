package com.seaquake6324.civitas.domain.security;

/** Minecraft-free equipment evidence sampled from the visible citizen entity. */
public record GuardEquipment(boolean basicWeapon, int armorPieces, boolean shield) {
    public GuardEquipment {
        armorPieces = Math.max(0, Math.min(4, armorPieces));
    }
}
