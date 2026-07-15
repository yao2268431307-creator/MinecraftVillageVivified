package com.livingvillages.regions.client.mixin;

import com.livingvillages.regions.tier.SettlementTier;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Scales each village placement to its settlement tier.
 *
 * <p>Vanilla {@link JigsawStructure#findGenerationPoint} reads the
 * structure's {@code maxDepth} and {@code maxDistanceFromCenter} (the
 * small JSON baseline) and passes them to {@link JigsawPlacement#addPieces}.
 * This Mixin redirects that single call, looks up the settlement tier
 * for the placement's chunk from the world seed, and multiplies the two
 * size arguments by the tier's factors before forwarding the call:</p>
 *
 * <ul>
 *   <li>VILLAGE (1×) &rarr; vanilla size (baseline unchanged)</li>
 *   <li>TOWN (3×) &rarr; ~3&times; depth and footprint</li>
 *   <li>CITY (8×) &rarr; ~8&times; depth and footprint (mega)</li>
 * </ul>
 *
 * <p>Because the scaling happens at the call site (per placement) rather
 * than on the shared {@code JigsawStructure} config instance, different
 * villages of the same biome type can be different sizes. The tier is a
 * pure function of {@code (worldSeed, chunkX, chunkZ)} via
 * {@link SettlementTier#tierFor}, the same function the client display
 * uses, so server sizing and client naming agree without any sync.</p>
 *
 * <p>The {@code verifyRange} codec ceiling is not relaxed here: the JSON
 * baseline is reset to vanilla values that load natively, and the tier
 * scaling is applied at runtime (after the codec-time check), so it is
 * not bounded by it.</p>
 *
 * <p>Registered server-side (in {@code mixins}) so it applies during
 * world-gen structure generation.</p>
 */
@Mixin(JigsawStructure.class)
public abstract class JigsawTierMixin {

    @Redirect(
        method = "findGenerationPoint",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/pools/JigsawPlacement;addPieces(Lnet/minecraft/world/level/levelgen/structure/Structure$GenerationContext;Lnet/minecraft/core/Holder;Ljava/util/Optional;ILnet/minecraft/core/BlockPos;ZLjava/util/Optional;I)Ljava/util/Optional;"
        )
    )
    private Optional<GenerationStub> livingvillages$scaleByTier(
            GenerationContext ctx,
            Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawName,
            int maxDepth,
            BlockPos startPos,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter) {
        SettlementTier tier = SettlementTier.tierFor(ctx.seed(), ctx.chunkPos().x, ctx.chunkPos().z);
        return JigsawPlacement.addPieces(
            ctx, startPool, startJigsawName,
            maxDepth * tier.depthFactor(),
            startPos, useExpansionHack, projectStartToHeightmap,
            maxDistanceFromCenter * tier.distanceFactor());
    }
}
