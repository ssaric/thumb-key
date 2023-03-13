package com.dessalines.thumbkey.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dessalines.thumbkey.IMEService
import com.dessalines.thumbkey.utils.FontSizeVariant
import com.dessalines.thumbkey.utils.KeyAction
import com.dessalines.thumbkey.utils.KeyC
import com.dessalines.thumbkey.utils.KeyDisplay
import com.dessalines.thumbkey.utils.KeyItemC
import com.dessalines.thumbkey.utils.KeyboardMode
import com.dessalines.thumbkey.utils.SwipeDirection
import com.dessalines.thumbkey.utils.TAG
import com.dessalines.thumbkey.utils.buildTapActions
import com.dessalines.thumbkey.utils.colorVariantToColor
import com.dessalines.thumbkey.utils.doneKeyAction
import com.dessalines.thumbkey.utils.fontSizeVariantToFontSize
import com.dessalines.thumbkey.utils.performKeyAction
import com.dessalines.thumbkey.utils.swipeDirection
import kotlin.math.roundToInt

@Composable
fun KeyboardKey(
    key: KeyItemC,
    mode: KeyboardMode,
    lastAction: MutableState<KeyAction?>,
    onToggleShiftMode: (enable: Boolean) -> Unit,
    onToggleNumericMode: (enable: Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val ime = ctx as IMEService
    val scope = rememberCoroutineScope()

    val pressed = remember { mutableStateOf(false) }
    val releasedKey = remember { mutableStateOf<String?>(null) }

    var tapCount by remember { mutableStateOf(0) }

    // TODO does it need to build this every time?
    val tapActions = buildTapActions(key)

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val backgroundColor = if (!pressed.value) {
        colorVariantToColor(colorVariant = key.backgroundColor)
    } else {
        MaterialTheme.colorScheme.inversePrimary
    }

    val keySize = 80.dp
    val keyPadding = 2.dp

    val animationSpeed = 100

    val keyboardKeyModifier =
        Modifier
            .height(keySize)
            .width(keySize * key.widthMultiplier)
            .padding(.5.dp)
            .background(color = backgroundColor)
            .pointerInput(key1 = key) {
                detectTapGestures(
                    onPress = {
                        pressed.value = true
                    },
                    onTap = {
                        // Set the last key info, and the tap count
                        lastAction.value?.let { lastAction ->
                            if (lastAction == key.center.action) {
                                tapCount += 1
                            } else {
                                tapCount = 0
                            }
                        }
                        lastAction.value = key.center.action

                        // Set the correct action
                        val action = tapActions[tapCount % tapActions.size]

                        performKeyAction(
                            action = action,
                            ime = ime,
                            mode = mode,
                            onToggleShiftMode = onToggleShiftMode,
                            onToggleNumericMode = onToggleNumericMode
                        )
                        doneKeyAction(scope, action, pressed, releasedKey)
                    }
                )
            }
            // The key1 is necessary, otherwise new swipes wont work
            .pointerInput(key1 = key) {
                detectDragGestures(
                    onDragStart = {
                        pressed.value = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val (x, y) = dragAmount
                        offsetX += x
                        offsetY += y
                    },
                    onDragEnd = {
                        val leeway = keySize.value
                        val swipeDirection = swipeDirection(offsetX, offsetY, leeway)

                        Log.d(
                            TAG,
                            "x: ${offsetX.roundToInt()}\ny: ${
                            offsetY.roundToInt()
                            }\n${swipeDirection?.name}"
                        )

                        val swipeKey = key.swipes?.get(swipeDirection)
                        val action = swipeKey?.action ?: run { key.center.action }
                        performKeyAction(
                            action = action,
                            ime = ime,
                            onToggleShiftMode = onToggleShiftMode,
                            onToggleNumericMode = onToggleNumericMode,
                            mode = mode
                        )

                        // Reset the drags
                        offsetX = 0f
                        offsetY = 0f

                        doneKeyAction(scope, action, pressed, releasedKey)
                    }
                )
            }

    // a 3x3 grid
    // Use box so they can overlap
    Box(
        modifier = keyboardKeyModifier
    ) {
        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier.fillMaxSize().padding(horizontal = keyPadding)
        ) {
            key.swipes?.get(SwipeDirection.TOP_LEFT)?.let {
                KeyText(it, keySize)
            }
        }
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize().padding(horizontal = keyPadding)
        ) {
            key.swipes?.get(SwipeDirection.TOP)?.let {
                KeyText(it, keySize)
            }
        }
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier.fillMaxSize().padding(horizontal = keyPadding)
        ) {
            key.swipes?.get(SwipeDirection.TOP_RIGHT)?.let {
                KeyText(it, keySize)
            }
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxSize().padding(horizontal = keyPadding)
        ) {
            key.swipes?.get(SwipeDirection.LEFT)?.let {
                KeyText(it, keySize)
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = keyPadding)
        ) {
            KeyText(key.center, keySize)
        }

        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier.fillMaxSize().padding(horizontal = keyPadding)
        ) {
            key.swipes?.get(SwipeDirection.RIGHT)?.let {
                KeyText(it, keySize)
            }
        }
        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier.fillMaxSize().padding(horizontal = keyPadding)
        ) {
            key.swipes?.get(SwipeDirection.BOTTOM_LEFT)?.let {
                KeyText(it, keySize)
            }
        }
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize().padding(horizontal = keyPadding)
        ) {
            key.swipes?.get(SwipeDirection.BOTTOM)?.let {
                KeyText(it, keySize)
            }
        }
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.fillMaxSize().padding(horizontal = keyPadding)
        ) {
            key.swipes?.get(SwipeDirection.BOTTOM_RIGHT)?.let {
                KeyText(it, keySize)
            }
        }
        // The popup overlay
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.tertiaryContainer),
            visible = releasedKey.value != null,
//            enter = scaleIn(tween(100)),
            enter = slideInVertically(tween(animationSpeed)),
            exit = ExitTransition.None
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                val fontSize = fontSizeVariantToFontSize(
                    fontSizeVariant = FontSizeVariant.LARGE,
                    keySize = keySize
                )
                releasedKey.value?.let { text ->
                    Text(
                        text = text,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun KeyText(key: KeyC, keySize: Dp) {
    val color = colorVariantToColor(colorVariant = key.color)
    val fontSize = fontSizeVariantToFontSize(fontSizeVariant = key.size, keySize = keySize)

    when (val display = key.display) {
        is KeyDisplay.IconDisplay -> {
            Icon(
                imageVector = display.icon,
                contentDescription = "TODO",
                tint = color
            )
        }
        is KeyDisplay.TextDisplay -> {
            Text(
                text = display.text,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = color
            )
        }
        null -> {}
    }
}