package com.guptrix.app.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class DrawingPoint(val x: Float, val y: Float)

data class DrawingStroke(
    val points: List<DrawingPoint>,
    val colorHex: String,
    val strokeWidth: Float
)

object DrawingSerializer {
    fun serialize(strokes: List<DrawingStroke>): String {
        val root = JSONArray()
        for (stroke in strokes) {
            val strokeObj = JSONObject()
            strokeObj.put("color", stroke.colorHex)
            strokeObj.put("width", stroke.strokeWidth.toDouble())
            val pts = JSONArray()
            for (p in stroke.points) {
                val ptObj = JSONObject()
                ptObj.put("x", p.x.toDouble())
                ptObj.put("y", p.y.toDouble())
                pts.put(ptObj)
            }
            strokeObj.put("points", pts)
            root.put(strokeObj)
        }
        return root.toString()
    }

    fun deserialize(json: String?): List<DrawingStroke> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<DrawingStroke>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val color = obj.optString("color", "#FFFFFF")
                val width = obj.optDouble("width", 8.0).toFloat()
                val ptsArray = obj.optJSONArray("points") ?: JSONArray()
                val points = mutableListOf<DrawingPoint>()
                for (j in 0 until ptsArray.length()) {
                    val ptObj = ptsArray.getJSONObject(j)
                    points.add(DrawingPoint(ptObj.getDouble("x").toFloat(), ptObj.getDouble("y").toFloat()))
                }
                list.add(DrawingStroke(points, color, width))
            }
        } catch (_: Exception) {
            // In case of parsing error, return empty
        }
        return list
    }
}

@Composable
fun CanvasDrawingView(
    initialCanvasData: String?,
    onCanvasChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strokes = remember { mutableStateListOf<DrawingStroke>() }
    var currentPoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }
    var selectedColorHex by remember { mutableStateOf("#3B82F6") }
    var selectedWidth by remember { mutableStateOf(8f) }

    LaunchedEffect(initialCanvasData) {
        if (strokes.isEmpty() && !initialCanvasData.isNullOrBlank()) {
            val loaded = DrawingSerializer.deserialize(initialCanvasData)
            strokes.clear()
            strokes.addAll(loaded)
        }
    }

    val paletteColors = listOf(
        "#3B82F6", // Blue
        "#10B981", // Emerald
        "#EF4444", // Red
        "#F59E0B", // Amber
        "#8B5CF6", // Purple
        "#FFFFFF", // White
        "#111827"  // Dark
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Drawing Surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .pointerInput(selectedColorHex, selectedWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints = listOf(DrawingPoint(offset.x, offset.y))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val newPoint = DrawingPoint(change.position.x, change.position.y)
                            currentPoints = currentPoints + newPoint
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                strokes.add(DrawingStroke(currentPoints, selectedColorHex, selectedWidth))
                                currentPoints = emptyList()
                                onCanvasChanged(DrawingSerializer.serialize(strokes))
                            }
                        },
                        onDragCancel = {
                            currentPoints = emptyList()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw finished strokes
                for (stroke in strokes) {
                    if (stroke.points.size > 1) {
                        val path = Path()
                        path.moveTo(stroke.points[0].x, stroke.points[0].y)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                        val strokeColor = try {
                            Color(android.graphics.Color.parseColor(stroke.colorHex))
                        } catch (_: Exception) {
                            Color.White
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    } else if (stroke.points.size == 1) {
                        val pt = stroke.points[0]
                        val strokeColor = try {
                            Color(android.graphics.Color.parseColor(stroke.colorHex))
                        } catch (_: Exception) {
                            Color.White
                        }
                        drawCircle(
                            color = strokeColor,
                            radius = stroke.strokeWidth / 2f,
                            center = Offset(pt.x, pt.y)
                        )
                    }
                }

                // Draw active stroke
                if (currentPoints.size > 1) {
                    val path = Path()
                    path.moveTo(currentPoints[0].x, currentPoints[0].y)
                    for (i in 1 until currentPoints.size) {
                        path.lineTo(currentPoints[i].x, currentPoints[i].y)
                    }
                    val strokeColor = try {
                        Color(android.graphics.Color.parseColor(selectedColorHex))
                    } catch (_: Exception) {
                        Color.White
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = selectedWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Toolbar for canvas controls
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (hex in paletteColors) {
                        val color = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) {
                            Color.Gray
                        }
                        val isSelected = selectedColorHex == hex

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                // Width and Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stroke width toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedWidth > 10f) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                selectedWidth = when (selectedWidth) {
                                    4f -> 8f
                                    8f -> 14f
                                    else -> 4f
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(when (selectedWidth) {
                                    4f -> 6.dp
                                    8f -> 10.dp
                                    else -> 14.dp
                                })
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface)
                        )
                    }

                    // Undo
                    IconButton(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                strokes.removeAt(strokes.lastIndex)
                                onCanvasChanged(DrawingSerializer.serialize(strokes))
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Clear
                    IconButton(
                        onClick = {
                            strokes.clear()
                            onCanvasChanged("")
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Clear canvas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
