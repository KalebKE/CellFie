package org.caexplorer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Camera state for 3D voxel viewing.
 * Uses spherical coordinates (pitch/yaw) for orbit around the grid center.
 */
@Stable
class Camera3DState {
    var pitch by mutableFloatStateOf(30f)
    var yaw by mutableFloatStateOf(45f)
    var zoom by mutableFloatStateOf(1f)
}

/**
 * Composable that renders a 3D voxel grid using software projection.
 *
 * Renders non-empty cells as isometric cubes with 3 visible faces,
 * sorted back-to-front for correct occlusion. Supports orbit camera
 * (drag to rotate), scroll to zoom, and transparency for partial states.
 */
@Composable
fun VoxelCanvas(
    cellColors: IntArray,
    cellStates: IntArray,
    gridWidth: Int,
    gridHeight: Int,
    gridDepth: Int,
    numStates: Int,
    voxelOpacity: Float = 0.8f,
    depthFadeEnabled: Boolean = false,
    depthFadeReversed: Boolean = false,
    layerMin: Int = 0,
    layerMax: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    val camera = remember { Camera3DState() }

    val effectiveLayerMax = layerMax.coerceAtMost(gridDepth - 1)

    // Precompute visible voxels when data or layer range changes
    val voxelData = remember(cellStates, gridWidth, gridHeight, gridDepth, layerMin, effectiveLayerMax) {
        if (cellStates.isEmpty() || gridWidth <= 0 || gridHeight <= 0 || gridDepth <= 0) {
            emptyList()
        } else {
            buildVisibleVoxels(cellStates, cellColors, gridWidth, gridHeight, gridDepth, layerMin, effectiveLayerMax)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, Color(0x20000000))
            .pointerInput(Unit) {
                // Drag to rotate camera
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    camera.yaw += dragAmount.x * 0.5f
                    camera.pitch = (camera.pitch - dragAmount.y * 0.5f).coerceIn(-89f, 89f)
                }
            }
            .pointerInput(Unit) {
                // Scroll to zoom
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            camera.zoom = (camera.zoom * (1f - scrollY * 0.1f)).coerceIn(0.2f, 5f)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
    ) {
        drawVoxelScene(
            voxelData = voxelData,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            gridDepth = gridDepth,
            camera = camera,
            numStates = numStates,
            voxelOpacity = voxelOpacity,
            depthFadeEnabled = depthFadeEnabled,
            depthFadeReversed = depthFadeReversed
        )
    }
}

private data class VoxelInfo(
    val x: Int, val y: Int, val z: Int,
    val color: Int, val state: Int
)

private fun buildVisibleVoxels(
    states: IntArray, colors: IntArray,
    w: Int, h: Int, d: Int,
    layerMin: Int = 0, layerMax: Int = d - 1
): List<VoxelInfo> {
    val result = ArrayList<VoxelInfo>(states.size / 4)
    val sliceSize = w * h
    val minL = layerMin.coerceIn(0, d - 1)
    val maxL = layerMax.coerceIn(0, d - 1)
    for (layer in minL..maxL) {
        for (row in 0 until h) {
            for (col in 0 until w) {
                val idx = layer * sliceSize + row * w + col
                val state = states.getOrElse(idx) { 0 }
                if (state > 0) {
                    result.add(VoxelInfo(col, row, layer, colors.getOrElse(idx) { -1 }, state))
                }
            }
        }
    }
    return result
}

private fun DrawScope.drawVoxelScene(
    voxelData: List<VoxelInfo>,
    gridWidth: Int, gridHeight: Int, gridDepth: Int,
    camera: Camera3DState,
    numStates: Int,
    voxelOpacity: Float = 0.8f,
    depthFadeEnabled: Boolean = false,
    depthFadeReversed: Boolean = false
) {
    if (voxelData.isEmpty()) return

    val canvasW = size.width
    val canvasH = size.height

    // Camera rotation matrices
    val pitchRad = camera.pitch * (PI.toFloat() / 180f)
    val yawRad = camera.yaw * (PI.toFloat() / 180f)
    val cosPitch = cos(pitchRad)
    val sinPitch = sin(pitchRad)
    val cosYaw = cos(yawRad)
    val sinYaw = sin(yawRad)

    // Center of the grid
    val cx = gridWidth / 2f
    val cy = gridHeight / 2f
    val cz = gridDepth / 2f

    // Scale to fit the grid in the viewport
    val maxDim = maxOf(gridWidth, gridHeight, gridDepth).toFloat()
    val baseScale = minOf(canvasW, canvasH) / (maxDim * 2f) * camera.zoom

    // Unit vectors for cube face rendering (half-size of one voxel in screen space)
    val halfSize = 0.5f
    // X-axis unit vector after rotation
    val xxScreen = (cosYaw) * baseScale
    val xyScreen = (sinPitch * sinYaw) * baseScale
    // Y-axis unit vector after rotation
    val yxScreen = 0f
    val yyScreen = (-cosPitch) * baseScale
    // Z-axis unit vector after rotation
    val zxScreen = (sinYaw) * baseScale
    val zyScreen = (-sinPitch * cosYaw) * baseScale

    // Project function: world (x,y,z) → screen (sx, sy, depth)
    fun project(wx: Float, wy: Float, wz: Float): Triple<Float, Float, Float> {
        val dx = wx - cx
        val dy = wy - cy
        val dz = wz - cz

        // Rotate around Y axis (yaw), then X axis (pitch)
        val rx = dx * cosYaw + dz * sinYaw
        val ry = dx * sinPitch * sinYaw + dy * (-cosPitch) + dz * (-sinPitch * cosYaw)
        val rz = dx * (-cosPitch * sinYaw) + dy * (-sinPitch) + dz * (cosPitch * cosYaw)

        val sx = canvasW / 2f + rx * baseScale
        val sy = canvasH / 2f + ry * baseScale

        return Triple(sx, sy, rz)
    }

    // Sort voxels back-to-front by depth
    val projected = voxelData.map { v ->
        val (sx, sy, depth) = project(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
        Triple(v, Offset(sx, sy), depth)
    }.sortedBy { it.third } // back-to-front (negative depth = far)

    // Light direction (normalized, in rotated camera space)
    val lightDirX = 0.3f
    val lightDirY = -0.7f
    val lightDirZ = 0.6f
    val lightLen = sqrt(lightDirX * lightDirX + lightDirY * lightDirY + lightDirZ * lightDirZ)
    val lx = lightDirX / lightLen
    val ly = lightDirY / lightLen
    val lz = lightDirZ / lightLen

    // Determine which 3 faces of each cube are visible based on camera direction
    // Camera looks in +Z after rotation, so we see faces pointing toward the camera
    val showTop = sinPitch > 0    // top face (normal = -Y) visible when looking down
    val showFront = cosPitch * cosYaw > 0  // front face (normal = -Z)
    val showRight = cosYaw > 0 // right face (normal = +X) visibility depends on yaw

    // Face normals for lighting
    val topNormal = Triple(0f, -1f, 0f)
    val bottomNormal = Triple(0f, 1f, 0f)
    val frontNormal = Triple(0f, 0f, -1f)
    val backNormal = Triple(0f, 0f, 1f)
    val rightNormal = Triple(1f, 0f, 0f)
    val leftNormal = Triple(-1f, 0f, 0f)

    fun lightFactor(nx: Float, ny: Float, nz: Float): Float {
        val dot = nx * lx + ny * ly + nz * lz
        return (0.3f + 0.7f * maxOf(0f, dot)) // ambient 0.3 + diffuse 0.7
    }

    val topLight = lightFactor(topNormal.first, topNormal.second, topNormal.third)
    val bottomLight = lightFactor(bottomNormal.first, bottomNormal.second, bottomNormal.third)
    val frontLight = lightFactor(frontNormal.first, frontNormal.second, frontNormal.third)
    val backLight = lightFactor(backNormal.first, backNormal.second, backNormal.third)
    val rightLight = lightFactor(rightNormal.first, rightNormal.second, rightNormal.third)
    val leftLight = lightFactor(leftNormal.first, leftNormal.second, leftNormal.third)

    // Voxel size in screen pixels
    val voxelScreenSize = baseScale * 0.95f // slight gap between voxels

    // Draw background grid axes
    drawGridAxes(canvasW, canvasH, baseScale, cx, cy, cz, cosYaw, sinYaw, cosPitch, sinPitch, gridWidth, gridHeight, gridDepth)

    // Draw each voxel
    val path = Path()
    for ((voxel, screenPos, _) in projected) {
        val r = ((voxel.color shr 16) and 0xFF) / 255f
        val g = ((voxel.color shr 8) and 0xFF) / 255f
        val b = (voxel.color and 0xFF) / 255f

        // Base alpha from state fraction
        val stateAlpha = if (numStates > 2) {
            0.4f + 0.6f * (voxel.state.toFloat() / (numStates - 1))
        } else {
            1f
        }

        // Depth fade: compute normalized distance from lattice center (0=center, 1=edge)
        val depthFadeFactor = if (depthFadeEnabled && gridDepth > 1) {
            val centerX = (gridWidth - 1) / 2f
            val centerY = (gridHeight - 1) / 2f
            val centerZ = (gridDepth - 1) / 2f
            val dx = (voxel.x - centerX) / (gridWidth / 2f).coerceAtLeast(1f)
            val dy = (voxel.y - centerY) / (gridHeight / 2f).coerceAtLeast(1f)
            val dz = (voxel.z - centerZ) / (gridDepth / 2f).coerceAtLeast(1f)
            val dist = sqrt(dx * dx + dy * dy + dz * dz).coerceIn(0f, 1.73f) / 1.73f
            if (depthFadeReversed) dist else 1f - dist
        } else {
            1f
        }

        val alpha = (stateAlpha * voxelOpacity * depthFadeFactor).coerceIn(0.02f, 1f)

        val hs = voxelScreenSize * 0.5f

        // Compute the 8 corners of the cube in screen space
        // Corner offsets from center: combinations of ±halfSize along each axis
        val corners = Array(8) { i ->
            val dx = if (i and 1 != 0) halfSize else -halfSize
            val dy = if (i and 2 != 0) halfSize else -halfSize
            val dz = if (i and 4 != 0) halfSize else -halfSize
            Offset(
                screenPos.x + dx * xxScreen + dy * yxScreen + dz * zxScreen,
                screenPos.y + dx * xyScreen + dy * yyScreen + dz * zyScreen
            )
        }
        // Corner indices:
        // 0: ---  1: +--  2: -+-  3: ++-
        // 4: --+  5: +-+  6: -++  7: +++

        // Draw visible faces as filled quads
        fun drawFace(c0: Int, c1: Int, c2: Int, c3: Int, light: Float) {
            val faceColor = Color(
                red = (r * light).coerceIn(0f, 1f),
                green = (g * light).coerceIn(0f, 1f),
                blue = (b * light).coerceIn(0f, 1f),
                alpha = alpha
            )
            path.reset()
            path.moveTo(corners[c0].x, corners[c0].y)
            path.lineTo(corners[c1].x, corners[c1].y)
            path.lineTo(corners[c2].x, corners[c2].y)
            path.lineTo(corners[c3].x, corners[c3].y)
            path.close()
            drawPath(path, faceColor, style = Fill)
        }

        // Top face (Y-) : corners 0,1,5,4 → when looking from above
        if (showTop) {
            drawFace(0, 1, 5, 4, topLight)
        } else {
            drawFace(2, 3, 7, 6, bottomLight)
        }

        // Front face (Z-) : corners 0,1,3,2
        if (showFront) {
            drawFace(0, 1, 3, 2, frontLight)
        } else {
            drawFace(4, 5, 7, 6, backLight)
        }

        // Right face (X+) : corners 1,5,7,3
        if (showRight) {
            drawFace(1, 5, 7, 3, rightLight)
        } else {
            drawFace(0, 4, 6, 2, leftLight)
        }
    }
}

private fun DrawScope.drawGridAxes(
    canvasW: Float, canvasH: Float,
    baseScale: Float,
    cx: Float, cy: Float, cz: Float,
    cosYaw: Float, sinYaw: Float,
    cosPitch: Float, sinPitch: Float,
    gridWidth: Int, gridHeight: Int, gridDepth: Int
) {
    val centerX = canvasW / 2f
    val centerY = canvasH / 2f
    val axisLen = maxOf(gridWidth, gridHeight, gridDepth).toFloat() * 0.6f

    fun projectAxis(dx: Float, dy: Float, dz: Float): Offset {
        val rx = dx * cosYaw + dz * sinYaw
        val ry = dx * sinPitch * sinYaw + dy * (-cosPitch) + dz * (-sinPitch * cosYaw)
        return Offset(centerX + rx * baseScale, centerY + ry * baseScale)
    }

    val origin = Offset(centerX, centerY)
    val xEnd = projectAxis(axisLen, 0f, 0f)
    val yEnd = projectAxis(0f, axisLen, 0f)
    val zEnd = projectAxis(0f, 0f, axisLen)

    val axisAlpha = 0.2f
    drawLine(Color.Red.copy(alpha = axisAlpha), origin, xEnd, strokeWidth = 1.5f)
    drawLine(Color.Green.copy(alpha = axisAlpha), origin, yEnd, strokeWidth = 1.5f)
    drawLine(Color.Blue.copy(alpha = axisAlpha), origin, zEnd, strokeWidth = 1.5f)
}
