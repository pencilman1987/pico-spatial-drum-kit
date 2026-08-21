package com.example.spatialdemo.content

import com.example.spatialdemo.domain.model.DrumId
import com.example.spatialdemo.domain.model.DrumKit
import com.example.spatialdemo.domain.model.DrumKitModelPlacement
import com.example.spatialdemo.domain.model.DrumSurface
import com.example.spatialdemo.interaction.HandFrame
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.GroundShadowComponent
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.ecs.simulation.CollisionResponseMode
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sin

class DrumScene private constructor(
    private val content: SpatialViewContent,
    private val drumEntities: Map<DrumId, Entity>,
    private val drumMaterials: Map<DrumId, UnlitMaterial>,
    private val cymbalIds: Set<DrumId>,
    private val trackingSpaceRoot: Entity,
    private val resources: List<AutoCloseable>,
) {
    private data class HitPulse(
        val startedAtNanos: Long,
        val endsAtNanos: Long,
        val intensity: Float,
    )

    private val pulses = mutableMapOf<DrumId, HitPulse>()
    private val overlayStates = mutableMapOf<DrumId, Int>()
    private var calibrationOverlayVisible = false
    private var productionModel: Entity? = null
    private var fallbackVisible = false
    private var kitOffset = Vector3.ZERO

    suspend fun loadProductionModel(): Boolean {
        if (productionModel != null) return true
        val model =
            runCatching {
                Entity.loadSuspend("asset://models/drum_kit_refined.glb").apply {
                    setName("PRODUCTION_DRUM_KIT")
                    components[TransformComponent::class.java]?.apply {
                        position = DrumKitModelPlacement.modelPosition + kitOffset
                        eulerAngles = EulerAngles(0f, DrumKitModelPlacement.yawDegrees, 0f)
                        scaleVector =
                            Vector3(
                                DrumKitModelPlacement.scale,
                                DrumKitModelPlacement.scale,
                                DrumKitModelPlacement.scale,
                            )
                    }
                    components.set(GroundShadowComponent(true, true))
                    content.addEntity(this)
                }
            }.getOrNull()
        productionModel = model
        fallbackVisible = model == null
        if (fallbackVisible) showFallbackKit()
        overlayStates.clear()
        return model != null
    }

    fun applyKitOffset(offset: Vector3) {
        kitOffset = offset
        productionModel?.components?.get(TransformComponent::class.java)?.position =
            DrumKitModelPlacement.modelPosition + offset
    }

    fun setCalibrationOverlayVisible(visible: Boolean) {
        calibrationOverlayVisible = visible
        overlayStates.clear()
    }

    fun applySurfaces(surfaces: List<DrumSurface>) {
        surfaces.forEach { surface ->
            drumEntities[surface.id]?.components[TransformComponent::class.java]?.apply {
                position = surface.center
                eulerAngles = surfaceOverlayRotation(surface.normal)
            }
        }
    }

    /** Converts Tracking Pack world-space points into this SpatialView's local meter space. */
    fun trackingFrameToScene(frame: HandFrame): HandFrame =
        frame.copy(
            hands =
                frame.hands.map { hand ->
                    hand.copy(
                        stickBase = trackingSpaceRoot.convertPositionFrom(hand.stickBase, null),
                        stickTip = trackingSpaceRoot.convertPositionFrom(hand.stickTip, null),
                    )
                },
        )

    fun registerHit(drumId: DrumId, intensity: Float, nowNanos: Long) {
        pulses[drumId] =
            HitPulse(
                startedAtNanos = nowNanos,
                endsAtNanos = nowNanos + HIT_PULSE_NANOS,
                intensity = intensity.coerceIn(0.12f, 1f),
            )
    }

    fun update(nowNanos: Long) {
        drumEntities.forEach { (id, entity) ->
            val pulse = pulses[id]?.takeIf { it.endsAtNanos > nowNanos }
            if (pulse == null) pulses.remove(id)
            val amount =
                pulse?.let {
                    val progress =
                        ((nowNanos - it.startedAtNanos).toFloat() / HIT_PULSE_NANOS)
                            .coerceIn(0f, 1f)
                    sin(progress * PI.toFloat()) * it.intensity
                } ?: 0f
            entity.components[TransformComponent::class.java]?.scaleVector =
                Vector3(1f + amount * 0.08f, 0.16f + amount * 0.08f, 1f + amount * 0.08f)

            val overlayState = if (pulse != null) 2 else if (calibrationOverlayVisible) 1 else 0
            if (overlayStates[id] != overlayState) {
                overlayStates[id] = overlayState
                drumMaterials[id]?.setBaseColor(overlayColor(id, overlayState))
            }
        }
    }

    fun destroy() {
        drumEntities.values.forEach { it.destroy(true) }
        productionModel?.destroy(true)
        trackingSpaceRoot.destroy(true)
        resources.asReversed().forEach { runCatching { it.close() } }
    }

    private fun showFallbackKit() {
        DrumKit.surfaces.forEach { surface ->
            drumMaterials[surface.id]?.setBaseColor(surface.color)
        }
    }

    companion object {
        fun create(content: SpatialViewContent): DrumScene {
            val resources = mutableListOf<AutoCloseable>()
            val materials = mutableMapOf<DrumId, UnlitMaterial>()
            val trackingSpaceRoot =
                Entity().apply {
                    setName("TRACKING_SPACE_ROOT")
                    content.addEntity(this)
                }
            val drumEntities =
                DrumKit.surfaces.associate { surface ->
                    val mesh = MeshResource.createCylinder(surface.radius, surface.depth).also(resources::add)
                    val material =
                        globalDrumMaterial(surface.id).also {
                            it.setBaseColor(Color4.fromSRGBHex("FFFFFF00"))
                            materials[surface.id] = it
                        }
                    val collisionShape =
                        ShapeResource.createBox(
                            Vector3(surface.radius * 2f, surface.depth, surface.radius * 2f),
                        ).also(resources::add)
                    val physicsMaterial = PhysicsMaterialResource().also(resources::add)
                    val entity =
                        ModelEntity(mesh, material).apply {
                            setName(surface.id.name)
                            components[TransformComponent::class.java]?.apply {
                                position = surface.center
                                eulerAngles = surfaceOverlayRotation(surface.normal)
                            }
                            components.set(
                                CollisionComponent(
                                    collisionShape = listOf(collisionShape),
                                    physicsMaterial = physicsMaterial,
                                    collisionResponseMode = CollisionResponseMode.TRIGGER_LITE,
                                ),
                            )
                            content.addEntity(this)
                        }
                    surface.id to entity
                }

            return DrumScene(
                content = content,
                drumEntities = drumEntities,
                drumMaterials = materials,
                cymbalIds = DrumKit.surfaces.filter { it.isCymbal }.mapTo(mutableSetOf()) { it.id },
                trackingSpaceRoot = trackingSpaceRoot,
                resources = resources,
            )
        }

        private const val HIT_PULSE_NANOS = 140_000_000L

        // The SDK applies material mutations asynchronously. Keeping these eight mutable
        // materials global prevents Stage teardown from closing one while a queued color
        // update is still being executed; they intentionally live for the app process.
        private val globalDrumMaterials = mutableMapOf<DrumId, UnlitMaterial>()

        private fun globalDrumMaterial(id: DrumId): UnlitMaterial =
            globalDrumMaterials.getOrPut(id) {
                UnlitMaterial.create(BlendingMode.TRANSPARENT).apply { toGlobal() }
            }

        /** Rotates a cylinder's local +Y axis onto the measured drum-surface normal. */
        private fun surfaceOverlayRotation(normal: Vector3): EulerAngles {
            val unit = normal.normalize()
            val pitch = atan2(unit.z, unit.y) * 180f / PI.toFloat()
            val roll = -asin(unit.x.coerceIn(-1f, 1f)) * 180f / PI.toFloat()
            return EulerAngles(pitch, 0f, roll)
        }
    }

    private fun overlayColor(id: DrumId, state: Int): Color4 =
        when (state) {
            2 -> Color4.fromSRGBHex(if (id in cymbalIds) "FFD45ED9" else "55B9FFD9")
            1 -> Color4.fromSRGBHex(if (id in cymbalIds) "E7B84B22" else "D8DEE722")
            else ->
                if (fallbackVisible) {
                    DrumKit.surfaces.first { it.id == id }.color
                } else {
                    Color4.fromSRGBHex("FFFFFF00")
                }
        }
}
