package com.example.rhythmtrainer

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.rhythmtrainer.ui.theme.RhythmTrainerTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContent {
            RhythmTrainerTheme {
                Scaffold( modifier = Modifier.fillMaxSize() ) {
                    RhythmTrainerSynthesizerApp(Modifier)
                }
            }
        }
    }
}

@Composable
fun RhythmTrainerSynthesizerApp(
    modifier: Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        WavetableSelectionPanel(modifier)
        ControlsPanel(modifier)
    }
}

@Composable
fun WavetableSelectionPanel(
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .border(BorderStroke(5.dp, Color.Black)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.wavetable))
            WavetableSelectionButtons(modifier)
        }
    }
}

@Composable
fun WavetableSelectionButtons(modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (wavetable in arrayOf("Sine", "Triangle", "Square", "Saw")) {
            WavetableButton(
                modifier = modifier,
                onClick = {},
                label = wavetable
            )
        }
    }
}

@Composable
fun WavetableButton(modifier: Modifier, onClick: () -> Unit, label: String) {
    Button(modifier = modifier, onClick = onClick) {
        Text(label)
    }
}

@Composable
fun ControlsPanel(
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .border(BorderStroke(5.dp, Color.Black)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .fillMaxWidth(0.7f)
                .border(BorderStroke(5.dp, Color.Black)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PitchControl(modifier)
            PlayControl(modifier)
        }
        Column(
            modifier = modifier
                .fillMaxSize()
                .border(BorderStroke(5.dp, Color.Black)),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VolumeControl(modifier)
        }
    }
}

@Composable
fun PitchControl(modifier: Modifier) {
    var frequency = rememberSaveable { mutableStateOf(300F) }

    PitchControlContent(
        modifier,
        pitchControlLabel = stringResource(R.string.frequency),
        value = frequency.value,
        onValueChange = {
            frequency.value = it
        },
        valueRange = 40F..3000F,
        frequencyValueLabel = stringResource(R.string.frequency_value, frequency.value)
    )
}
@Composable
fun PlayControl(modifier: Modifier) {
    Button(modifier = modifier,
        onClick = {}) {
        Text(stringResource(R.string.play))
    }
}

@Composable
fun PitchControlContent(
    modifier: Modifier,
    pitchControlLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    frequencyValueLabel: String

) {
    Text(pitchControlLabel)
    Slider(modifier = modifier, value = value, onValueChange = onValueChange, valueRange = valueRange)
    Text(frequencyValueLabel)
}

@Composable
fun VolumeControl(modifier: Modifier) {
    val volume = rememberSaveable { mutableStateOf(-10F) }

    VolumeControlContent(
        modifier = modifier,
        value = volume.value,
        onValueChange = {
            volume.value = it
        },
        volumeRange = -60F..0F
    )
}

@Composable
fun VolumeControlContent(
    modifier: Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    volumeRange: ClosedFloatingPointRange<Float>
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val sliderHeight = screenHeight / 8

    Icon(imageVector = Icons.Filled.VolumeUp, contentDescription = null)
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(sliderHeight.dp).rotate(270f),
        valueRange = -60F..0F
    )
    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeMute, contentDescription = null)
}