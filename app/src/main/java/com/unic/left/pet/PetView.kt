package com.unic.left.pet

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceView
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.IBLPrefilterContext
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer

/**
 * A Filament-backed 3D companion surface.
 *
 * Owns a [ModelViewer] for the lifetime of the window attachment and drives it from a
 * [Choreographer] frame callback. One-finger orbit, two-finger pan and pinch-zoom are handled
 * by the viewer; a single tap is forwarded to [onTap] and, when the model has animations,
 * advances to the next one.
 */
class PetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), Choreographer.FrameCallback {

    companion object {
        init {
            // Loads gltfio and the Filament core through the utils layer.
            Utils.init()
        }
    }

    private val choreographer = Choreographer.getInstance()
    private var viewer: ModelViewer? = null
    private var running = false

    /** Called on a single tap, after the model animation has been advanced. */
    var onTap: (() -> Unit)? = null

    private val tapDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            pet()
            return true
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (viewer == null) {
            val v = ModelViewer(this).also { configure(it) }
            viewer = v
            setOnTouchListener { _, event ->
                tapDetector.onTouchEvent(event)
                v.onTouchEvent(event)
                true
            }
        }
        startRendering()
    }

    override fun onDetachedFromWindow() {
        stopRendering()
        viewer?.destroy()
        viewer = null
        super.onDetachedFromWindow()
    }

    private fun startRendering() {
        if (running) return
        running = true
        choreographer.postFrameCallback(this)
    }

    private fun stopRendering() {
        running = false
        choreographer.removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        choreographer.postFrameCallback(this)
        // ModelViewer.render() advances glTF animations when autoPlayAnimations is true.
        viewer?.render(frameTimeNanos)
    }

    private fun configure(v: ModelViewer) {
        // Mobile-friendly quality/performance trade-offs used by the official sample.
        v.view.renderQuality = v.view.renderQuality.apply {
            hdrColorBuffer = View.QualityLevel.MEDIUM
        }
        v.view.dynamicResolutionOptions = v.view.dynamicResolutionOptions.apply {
            enabled = true
            quality = View.QualityLevel.MEDIUM
        }
        v.view.multiSampleAntiAliasingOptions = v.view.multiSampleAntiAliasingOptions.apply {
            enabled = true
        }
        v.view.antiAliasing = View.AntiAliasing.FXAA
        v.view.ambientOcclusionOptions = v.view.ambientOcclusionOptions.apply { enabled = true }
        v.view.bloomOptions = v.view.bloomOptions.apply { enabled = true }

        setupEnvironment(v)

        readAsset("models/girl.glb")?.let { buffer ->
            v.loadModelGlb(buffer)
            v.transformToUnitCube()
        }
    }

    /** Generates image-based lighting and a skybox on-device from the bundled HDR image. */
    private fun setupEnvironment(v: ModelViewer) {
        val engine = v.engine
        val hdr = readAsset("envs/room.hdr") ?: return
        val equirect = HDRLoader.createTexture(engine, hdr) ?: return

        val context = IBLPrefilterContext(engine)
        val equirectToCubemap = IBLPrefilterContext.EquirectangularToCubemap(context)
        val skyboxTexture = equirectToCubemap.run(equirect)
        engine.destroyTexture(equirect)
        if (skyboxTexture == null) {
            equirectToCubemap.destroy()
            context.destroy()
            return
        }

        val specularFilter = IBLPrefilterContext.SpecularFilter(context)
        val reflections = specularFilter.run(skyboxTexture)
        val ibl = IndirectLight.Builder()
            .reflections(reflections)
            .intensity(25_000.0f)
            .build(engine)
        val skybox = Skybox.Builder().environment(skyboxTexture).build(engine)
        specularFilter.destroy()
        equirectToCubemap.destroy()
        context.destroy()

        v.scene.indirectLight = ibl
        v.scene.skybox = skybox
    }

    /** Advances to the next glTF animation (if any) and notifies the host. */
    private fun pet() {
        val v = viewer
        if (v != null) {
            val count = v.animator?.animationCount ?: 0
            if (count > 1) {
                v.activeAnimationIndex = (v.activeAnimationIndex + 1) % count
            }
        }
        onTap?.invoke()
    }

    /** Restores the default camera, animation and model transform. */
    fun resetView() {
        viewer?.resetToDefaultState()
    }

    private fun readAsset(path: String): ByteBuffer? = try {
        context.assets.open(path).use { input ->
            val bytes = ByteArray(input.available())
            var offset = 0
            while (offset < bytes.size) {
                val read = input.read(bytes, offset, bytes.size - offset)
                if (read < 0) break
                offset += read
            }
            ByteBuffer.wrap(bytes)
        }
    } catch (e: Exception) {
        null
    }
}
