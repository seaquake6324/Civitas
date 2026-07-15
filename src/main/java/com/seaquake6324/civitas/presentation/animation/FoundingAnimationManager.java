package com.seaquake6324.civitas.presentation.animation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class FoundingAnimationManager {
    private static final List<Animation> ACTIVE = new ArrayList<>();

    public static void start(BlockPos core, String cityName, int color) {
        ACTIVE.add(new Animation(core.immutable(), cityName, color & 0xFFFFFF));
    }

    public static boolean isActive() { return !ACTIVE.isEmpty(); }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        Iterator<Animation> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next();
            animation.tick(minecraft, level);
            if (animation.age > 120) iterator.remove();
        }
    }

    private static final class Animation {
        private final BlockPos core;
        private final String name;
        private final int color;
        private final DustParticleOptions main;
        private final DustParticleOptions highlight;
        private final DustParticleOptions dark;
        private int age;

        private Animation(BlockPos core, String name, int color) {
            this.core = core;
            this.name = name;
            this.color = color;
            int visible = ensureVisible(color);
            this.main = new DustParticleOptions(0xFF000000 | visible, 1.25F);
            this.highlight = new DustParticleOptions(0xFF000000 | mix(visible, 0xFFFFFF, 0.62F), 1.5F);
            this.dark = new DustParticleOptions(0xFF000000 | mix(visible, 0, 0.42F), 0.9F);
        }

        private void tick(Minecraft minecraft, ClientLevel level) {
            age++;
            ParticleStatus particleStatus = minecraft.options.particles().get();
            double cx = core.getX() + 0.5;
            double cy = core.getY() + 0.65;
            double cz = core.getZ() + 0.5;

            if (age <= 30) drawCharge(level, particleStatus, cx, cy, cz);
            if (age == 1) {
                level.playLocalSound(cx, cy, cz, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.5F, 0.62F, false);
            }
            if (age == 18) {
                level.playLocalSound(cx, cy, cz, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.42F, 0.72F, false);
            }
            if (age == 30) drawLightning(level, particleStatus, cx, cy, cz);
            if (age >= 30 && age <= 38) drawShockwave(level, particleStatus, cx, cz);
            if (age >= 40) drawBoundary(level, particleStatus);

            if (age == 82) {
                MutableComponent title = Component.literal(name).withStyle(style -> style.withColor(color))
                        .append(Component.translatable("civitas.title.established"));
                minecraft.gui.setTimes(5, 25, 10);
                minecraft.gui.setSubtitle(Component.translatable("civitas.title.subtitle"));
                minecraft.gui.setTitle(title);
                level.playLocalSound(cx, cy, cz, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.78F, 0.72F, false);
                level.playLocalSound(cx, cy, cz, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.58F, 1.12F, false);
            }
        }

        private void drawCharge(ClientLevel level, ParticleStatus status, double cx, double cy, double cz) {
            int streams = status == ParticleStatus.ALL ? 12 : status == ParticleStatus.DECREASED ? 7 : 3;
            double contraction = 1.0 - age / 34.0;
            for (int i = 0; i < streams; i++) {
                double angle = age * 0.22 + Math.PI * 2.0 * i / streams;
                double radius = (2.7 + 0.35 * Math.sin(i * 2.1)) * contraction;
                double y = core.getY() + 0.15 + (i % 4) * 0.35 + age * 0.012;
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;
                ParticleOptions particle = (i + age) % 4 == 0 ? highlight : main;
                level.addParticle(particle, x, y, z, (cx - x) * 0.035, (cy - y) * 0.025, (cz - z) * 0.035);
            }
            if (age % 2 == 0) {
                double arm = 0.35 + age / 30.0 * 1.05;
                spawnLine(level, main, new Vec3(cx - arm, core.getY() + 0.08, cz), new Vec3(cx + arm, core.getY() + 0.08, cz), 0.22);
                spawnLine(level, main, new Vec3(cx, core.getY() + 0.08, cz - arm), new Vec3(cx, core.getY() + 0.08, cz + arm), 0.22);
            }
            if (age % 3 == 0) {
                level.addParticle(ParticleTypes.END_ROD, cx, cy + age / 30.0, cz, 0, 0.025, 0);
            }
        }

        private void drawLightning(ClientLevel level, ParticleStatus status, double cx, double cy, double cz) {
            int columns = status == ParticleStatus.MINIMAL ? 1 : status == ParticleStatus.DECREASED ? 3 : 5;
            for (int column = 0; column < columns; column++) {
                double angle = Math.PI * 2.0 * column / Math.max(1, columns);
                double offset = column == 0 ? 0 : 0.11;
                for (double y = 0.5; y <= 24.0; y += 0.45) {
                    double jitter = Math.sin(y * 2.7 + column) * 0.055;
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                            cx + Math.cos(angle) * offset + jitter, core.getY() + y,
                            cz + Math.sin(angle) * offset - jitter, 0, -0.16, 0);
                }
            }
            for (int i = 0; i < (status == ParticleStatus.MINIMAL ? 12 : 36); i++) {
                double angle = Math.PI * 2.0 * i / (status == ParticleStatus.MINIMAL ? 12 : 36);
                level.addParticle(i % 3 == 0 ? ParticleTypes.END_ROD : highlight,
                        cx + Math.cos(angle) * 0.28, core.getY() + 0.45, cz + Math.sin(angle) * 0.28,
                        Math.cos(angle) * 0.22, 0.06, Math.sin(angle) * 0.22);
            }
            level.playLocalSound(cx, cy, cz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.52F, 1.18F, false);
            level.playLocalSound(cx, cy, cz, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 0.82F, 0.8F, false);
        }

        private void drawShockwave(ClientLevel level, ParticleStatus status, double cx, double cz) {
            int samples = status == ParticleStatus.ALL ? 56 : status == ParticleStatus.DECREASED ? 32 : 16;
            double progress = (age - 29) / 9.0;
            double radius = 0.45 + progress * 4.0;
            for (int i = 0; i < samples; i++) {
                double angle = Math.PI * 2.0 * i / samples;
                ParticleOptions particle = i % 5 == 0 ? highlight : main;
                level.addParticle(particle, cx + Math.cos(angle) * radius, core.getY() + 0.09,
                        cz + Math.sin(angle) * radius, Math.cos(angle) * 0.012, 0.005, Math.sin(angle) * 0.012);
            }
        }

        private void drawBoundary(ClientLevel level, ParticleStatus status) {
            ChunkPos center = ChunkPos.containing(core);
            double coreMinX = center.getMinBlockX();
            double coreMinZ = center.getMinBlockZ();
            double groundY = core.getY() + 0.12;
            double spacing = status == ParticleStatus.ALL ? 2.25 : status == ParticleStatus.DECREASED ? 4.0 : 8.0;

            if (age <= 48) {
                double closeProgress = smooth((age - 40) / 8.0);
                spawnPerimeterPartial(level, main, coreMinX, coreMinZ, coreMinX + 16, coreMinZ + 16, groundY, spacing, closeProgress);
                double lead = Math.min(1.0, closeProgress + 0.04);
                spawnPerimeterHead(level, highlight, coreMinX, coreMinZ, coreMinX + 16, coreMinZ + 16, groundY + 0.035, lead, 2.5);
                return;
            }

            double expansion;
            if (age <= 56) expansion = 8.0 * smooth((age - 48) / 8.0);
            else expansion = 8.0 + 8.0 * smooth((age - 56) / 8.0);
            expansion = Math.min(16.0, expansion);
            double minX = coreMinX - expansion;
            double minZ = coreMinZ - expansion;
            double maxX = coreMinX + 16 + expansion;
            double maxZ = coreMinZ + 16 + expansion;

            if (age <= 64) {
                spawnPerimeter(level, main, minX, minZ, maxX, maxZ, groundY, spacing);
                if (status != ParticleStatus.MINIMAL) {
                    double trailExpansion = Math.max(0, expansion - 1.2);
                    spawnPerimeter(level, dark, coreMinX - trailExpansion, coreMinZ - trailExpansion,
                            coreMinX + 16 + trailExpansion, coreMinZ + 16 + trailExpansion, groundY + 0.025, spacing * 1.5);
                }
                cornerBursts(level, minX, minZ, maxX, maxZ, groundY);
                return;
            }

            minX = coreMinX - 16;
            minZ = coreMinZ - 16;
            maxX = coreMinX + 32;
            maxZ = coreMinZ + 32;
            ParticleOptions groundParticle = age > 108 ? dark : main;
            spawnPerimeter(level, groundParticle, minX, minZ, maxX, maxZ, groundY, spacing);
            if (status == ParticleStatus.ALL && age % 3 == 0) {
                spawnPerimeter(level, highlight, minX, minZ, maxX, maxZ, groundY + 0.045, spacing * 2.0);
            }

            if (status == ParticleStatus.MINIMAL) return;
            if (age >= 68) {
                double pillarHeight = age <= 80 ? 12.0 * smooth((age - 68) / 12.0)
                        : age <= 108 ? 12.0 : 12.0 * Math.max(0.0, (120 - age) / 12.0);
                drawPillars(level, minX, minZ, maxX, maxZ, groundY, pillarHeight, status);
                if (age >= 80 && age <= 108) {
                    double topY = groundY + 12.0;
                    if ((age & 1) == 0 || status == ParticleStatus.ALL) {
                        spawnPerimeter(level, main, minX, minZ, maxX, maxZ, topY, spacing);
                    }
                    if (age >= 84 && age <= 104) drawClosingLightFlow(level, minX, minZ, maxX, maxZ, groundY);
                }
            }
        }

        private void drawPillars(ClientLevel level, double minX, double minZ, double maxX, double maxZ,
                                 double groundY, double height, ParticleStatus status) {
            if (height <= 0.05) return;
            double verticalSpacing = status == ParticleStatus.ALL ? 0.85 : 1.45;
            double[][] corners = {{minX, minZ}, {maxX, minZ}, {maxX, maxZ}, {minX, maxZ}};
            for (double[] corner : corners) {
                spawnLine(level, main, new Vec3(corner[0], groundY, corner[1]), new Vec3(corner[0], groundY + height, corner[1]), verticalSpacing);
                level.addParticle(ParticleTypes.END_ROD, corner[0], groundY + height, corner[1], 0, 0.035, 0);
                level.addParticle(highlight, corner[0], groundY + height - 0.15, corner[1], 0, 0.015, 0);
            }
        }

        private void drawClosingLightFlow(ClientLevel level, double minX, double minZ, double maxX, double maxZ, double groundY) {
            double side = maxX - minX;
            double perimeter = side * 4.0;
            double total = perimeter * 2.0 + 24.0;
            double head = smooth((age - 84) / 20.0) * total;
            for (int i = 0; i < 18; i++) {
                double distance = Math.max(0, head - i * 0.7);
                Vec3 point = wirePoint(distance, minX, minZ, maxX, maxZ, groundY, perimeter);
                level.addParticle(i < 5 ? ParticleTypes.END_ROD : highlight, point.x, point.y, point.z, 0, 0.008, 0);
            }
        }

        private static Vec3 wirePoint(double distance, double minX, double minZ, double maxX, double maxZ,
                                      double groundY, double perimeter) {
            double side = maxX - minX;
            if (distance <= perimeter) return perimeterPoint(minX, minZ, maxX, maxZ, groundY, distance / perimeter);
            distance -= perimeter;
            if (distance <= 12.0) return new Vec3(minX, groundY + distance, minZ);
            distance -= 12.0;
            if (distance <= perimeter) return perimeterPoint(minX, minZ, maxX, maxZ, groundY + 12.0, distance / perimeter);
            distance -= perimeter;
            return new Vec3(maxX, groundY + Math.max(0, 12.0 - distance), maxZ);
        }

        private void cornerBursts(ClientLevel level, double minX, double minZ, double maxX, double maxZ, double y) {
            level.addParticle(highlight, minX, y, minZ, 0, 0.02, 0);
            level.addParticle(highlight, maxX, y, minZ, 0, 0.02, 0);
            level.addParticle(highlight, maxX, y, maxZ, 0, 0.02, 0);
            level.addParticle(highlight, minX, y, maxZ, 0, 0.02, 0);
        }

        private static void spawnPerimeter(ClientLevel level, ParticleOptions particle, double minX, double minZ,
                                           double maxX, double maxZ, double y, double spacing) {
            double perimeter = 2.0 * ((maxX - minX) + (maxZ - minZ));
            int samples = Math.max(4, (int)Math.ceil(perimeter / spacing));
            for (int i = 0; i < samples; i++) {
                Vec3 point = perimeterPoint(minX, minZ, maxX, maxZ, y, i / (double)samples);
                level.addParticle(particle, point.x, point.y, point.z, 0, 0.002, 0);
            }
        }

        private static void spawnPerimeterPartial(ClientLevel level, ParticleOptions particle, double minX, double minZ,
                                                  double maxX, double maxZ, double y, double spacing, double progress) {
            double perimeter = 2.0 * ((maxX - minX) + (maxZ - minZ));
            int samples = Math.max(1, (int)Math.ceil(perimeter * progress / spacing));
            for (int i = 0; i <= samples; i++) {
                double t = Math.min(progress, i * spacing / perimeter);
                Vec3 point = perimeterPoint(minX, minZ, maxX, maxZ, y, t);
                level.addParticle(particle, point.x, point.y, point.z, 0, 0.002, 0);
            }
        }

        private static void spawnPerimeterHead(ClientLevel level, ParticleOptions particle, double minX, double minZ,
                                               double maxX, double maxZ, double y, double progress, double trailLength) {
            double perimeter = 2.0 * ((maxX - minX) + (maxZ - minZ));
            int samples = 8;
            for (int i = 0; i < samples; i++) {
                double t = Math.max(0, progress - i * trailLength / samples / perimeter);
                Vec3 point = perimeterPoint(minX, minZ, maxX, maxZ, y, t);
                level.addParticle(particle, point.x, point.y, point.z, 0, 0.006, 0);
            }
        }

        private static Vec3 perimeterPoint(double minX, double minZ, double maxX, double maxZ, double y, double t) {
            double width = maxX - minX;
            double depth = maxZ - minZ;
            double perimeter = 2.0 * (width + depth);
            double distance = Math.max(0, Math.min(1, t)) * perimeter;
            if (distance <= width) return new Vec3(minX + distance, y, minZ);
            distance -= width;
            if (distance <= depth) return new Vec3(maxX, y, minZ + distance);
            distance -= depth;
            if (distance <= width) return new Vec3(maxX - distance, y, maxZ);
            distance -= width;
            return new Vec3(minX, y, maxZ - Math.min(depth, distance));
        }

        private static void spawnLine(ClientLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double spacing) {
            double distance = from.distanceTo(to);
            int samples = Math.max(1, (int)Math.ceil(distance / spacing));
            for (int i = 0; i <= samples; i++) {
                Vec3 point = from.lerp(to, i / (double)samples);
                level.addParticle(particle, point.x, point.y, point.z, 0, 0.003, 0);
            }
        }

        private static double smooth(double value) {
            double t = Math.max(0.0, Math.min(1.0, value));
            return t * t * (3.0 - 2.0 * t);
        }

        private static int ensureVisible(int rgb) {
            int r = rgb >> 16 & 255;
            int g = rgb >> 8 & 255;
            int b = rgb & 255;
            int max = Math.max(r, Math.max(g, b));
            if (max < 96) {
                float scale = 96.0F / Math.max(1, max);
                r = Math.min(255, Math.round(r * scale));
                g = Math.min(255, Math.round(g * scale));
                b = Math.min(255, Math.round(b * scale));
            }
            return r << 16 | g << 8 | b;
        }

        private static int mix(int rgb, int target, float amount) {
            int r = Math.round((rgb >> 16 & 255) * (1 - amount) + (target >> 16 & 255) * amount);
            int g = Math.round((rgb >> 8 & 255) * (1 - amount) + (target >> 8 & 255) * amount);
            int b = Math.round((rgb & 255) * (1 - amount) + (target & 255) * amount);
            return r << 16 | g << 8 | b;
        }
    }

    private FoundingAnimationManager() {}
}
