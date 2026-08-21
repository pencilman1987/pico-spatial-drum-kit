package com.example.spatialdemo.content

import com.example.spatialdemo.domain.model.DrumId
import com.example.spatialdemo.domain.model.DrumKit
import com.example.spatialdemo.domain.model.DrumSurface
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
import kotlin.math.sin

class DrumScene private constructor(
    private val content: SpatialViewContent,
    private val drumEntities: Map<DrumId, Entity>,
    private val drumMaterials: Map<DrumId, UnlitMaterial>,
    private val cymbalIds: Set<DrumId>,
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
                        position = PRODUCTION_MODEL_POSITION + kitOffset
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

    fun applyKitOffset(offset: Vector3) {
        kitOffset = offset
        productionModel?.components?.get(TransformComponent::class.java)?.position =
            PRODUCTION_MODEL_POSITION + offset
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

    fun destroy() {
        drumEntities.values.forEach { it.destroy(true) }
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

            return DrumScene(
                content = content,
                drumEntities = drumEntities,
                drumMaterials = materials,
                cymbalIds = DrumKit.surfaces.filter { it.isCymbal }.mapTo(mutableSetOf()) { it.id },
                resources = resources,
            )
        }

        private val PRODUCTION_MODEL_POSITION = Vector3(0.11592f, 0.50778f, -0.93882f)
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
