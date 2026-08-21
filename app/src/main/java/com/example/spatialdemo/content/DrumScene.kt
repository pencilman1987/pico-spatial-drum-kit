package com.example.spatialdemo.content

import com.example.spatialdemo.domain.model.DrumId
import com.example.spatialdemo.domain.model.DrumKit
import com.example.spatialdemo.domain.model.DrumSurface
import com.example.spatialdemo.interaction.HandFrame
import com.example.spatialdemo.interaction.HandSide
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
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sin

class DrumScene private constructor(
    private val content: SpatialViewContent,
    private val drumEntities: Map<DrumId, Entity>,
    private val drumMaterials: Map<DrumId, UnlitMaterial>,
    private val cymbalIds: Set<DrumId>,
    private val stickEntities: Map<HandSide, Entity>,
    private val handTipEntities: Map<HandSide, Entity>,
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

    suspend fun loadProductionModel(): Boolean {
        if (productionModel != null) return true
        val model =
            runCatching {
                Entity.loadSuspend("asset://models/drum_kit_refined.glb").apply {
                    setName("PRODUCTION_DRUM_KIT")
                    components[TransformComponent::class.java]?.apply {
                        position = Vector3(0.11592f, 0.50778f, -0.93882f)
                        eulerAngles = EulerAngles(0f, 0f, 0f)
                        scaleVector = Vector3(1.4f, 1.4f, 1.4f)
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

    fun setCalibrationOverlayVisible(visible: Boolean) {
        calibrationOverlayVisible = visible
        overlayStates.clear()
    }

    fun applySurfaces(surfaces: List<DrumSurface>) {
        surfaces.forEach { surface ->
            drumEntities[surface.id]?.components[TransformComponent::class.java]?.apply {
                position = surface.center
                eulerAngles =
                    if (surface.normal == Vector3.BACK) {
                        EulerAngles(90f, 0f, 0f)
                    } else {
                        EulerAngles(0f, 0f, 0f)
                    }
            }
        }
    }

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

    fun updateHands(frame: HandFrame) {
        val visibleHands = frame.hands.associateBy { it.side }
        HandSide.entries.forEach { side ->
            val hand = visibleHands[side]?.takeIf { it.gripActive }
            val stick = stickEntities[side]
            val tip = handTipEntities[side]
            if (hand == null) {
                stick?.components?.get(TransformComponent::class.java)?.position = HIDDEN_POSITION
                tip?.components?.get(TransformComponent::class.java)?.position = HIDDEN_POSITION
                return@forEach
            }

            val axis = hand.stickTip - hand.stickBase
            val length = axis.length()
            if (length <= 0.001f) return@forEach
            stick?.components?.get(TransformComponent::class.java)?.apply {
                position = (hand.stickBase + hand.stickTip) * 0.5f
                quaternion = rotationFromUp(axis / length)
                scaleVector = Vector3(1f, length, 1f)
            }
            tip?.components?.get(TransformComponent::class.java)?.position = hand.stickTip
        }
    }

    fun destroy() {
        drumEntities.values.forEach { it.destroy(true) }
        stickEntities.values.forEach { it.destroy(true) }
        handTipEntities.values.forEach { it.destroy(true) }
        productionModel?.destroy(true)
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
            val drumEntities =
                DrumKit.surfaces.associate { surface ->
                    val mesh = MeshResource.createCylinder(surface.radius, surface.depth).also(resources::add)
                    val material =
                        UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
                            setBaseColor(Color4.fromSRGBHex("FFFFFF00"))
                        }.also {
                            resources += it
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
                                if (surface.normal == Vector3.BACK) eulerAngles = EulerAngles(90f, 0f, 0f)
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

            val stickMesh = MeshResource.createCylinder(STICK_RADIUS_METERS, 1f).also(resources::add)
            val tipMesh = MeshResource.createSphere(STICK_TIP_RADIUS_METERS).also(resources::add)
            val stickMaterial =
                UnlitMaterial.create(BlendingMode.OPAQUE).apply {
                    setBaseColor(Color4.fromSRGBHex("C58B4AFF"))
                }.also(resources::add)
            val tipMaterial =
                UnlitMaterial.create(BlendingMode.OPAQUE).apply {
                    setBaseColor(Color4.fromSRGBHex("E6C28FFF"))
                }.also(resources::add)
            val sticks =
                HandSide.entries.associateWith { side ->
                    ModelEntity(stickMesh, stickMaterial).apply {
                        setName("${side.name}_DRUM_STICK")
                        components[TransformComponent::class.java]?.position = HIDDEN_POSITION
                        content.addEntity(this)
                    }
                }
            val tips =
                HandSide.entries.associateWith { side ->
                    ModelEntity(tipMesh, tipMaterial).apply {
                        setName("${side.name}_STICK_TIP")
                        components[TransformComponent::class.java]?.position = HIDDEN_POSITION
                        content.addEntity(this)
                    }
                }
            return DrumScene(
                content = content,
                drumEntities = drumEntities,
                drumMaterials = materials,
                cymbalIds = DrumKit.surfaces.filter { it.isCymbal }.mapTo(mutableSetOf()) { it.id },
                stickEntities = sticks,
                handTipEntities = tips,
                resources = resources,
            )
        }

        private fun rotationFromUp(direction: Vector3): Quat {
            val normalized = direction.normalize()
            val dot = Vector3.dot(Vector3.UP, normalized).coerceIn(-1f, 1f)
            if (dot > 0.9999f) return Quat.identity()
            if (dot < -0.9999f) return Quat(Vector3.RIGHT, PI.toFloat())
            val axis = Vector3.cross(Vector3.UP, normalized).normalize()
            return Quat(axis, acos(dot))
        }

        private val HIDDEN_POSITION = Vector3(0f, -10f, 0f)
        private const val STICK_RADIUS_METERS = 0.008f
        private const val STICK_TIP_RADIUS_METERS = 0.014f
        private const val HIT_PULSE_NANOS = 140_000_000L
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
