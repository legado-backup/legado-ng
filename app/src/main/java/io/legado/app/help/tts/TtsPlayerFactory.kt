package io.legado.app.help.tts

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.util.Collections
import java.util.WeakHashMap

/**
 * Creates players for synthesized speech.
 *
 * SoundTouch replaces Media3 and Android's Sonic-based time stretching, which can introduce
 * audible artifacts in synthesized speech above 1x.
 */
@SuppressLint("UnsafeOptInUsageError")
object TtsPlayerFactory {

    private val processorChains = Collections.synchronizedMap(
        WeakHashMap<ExoPlayer, TtsAdjustableAudioProcessorChain>()
    )

    fun create(context: Context, allowFormatChanges: Boolean = false): ExoPlayer {
        val processorChain = if (allowFormatChanges) {
            TtsMedia3AudioProcessorChain()
        } else {
            TtsSoundTouchAudioProcessorChain()
        }
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessorChain(processorChain)
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(false)
                    .build()
            }
        }
        return ExoPlayer.Builder(context, renderersFactory).build().also { player ->
            processorChains[player] = processorChain
        }
    }

    fun applyPlaybackAdjustments(
        player: ExoPlayer,
        baseRate: Float,
        voiceParams: TtsVoicePlaybackParams,
    ) {
        val params = TtsPlaybackAdjustmentPolicy.resolve(baseRate, voiceParams)
        val activationChanged = processorChains[player]
            ?.applyPlaybackAdjustments(params)
            ?: false
        val playbackParametersChanged = player.playbackParameters != params.playbackParameters
        player.setPlaybackParameters(params.playbackParameters)
        if (activationChanged && !playbackParametersChanged &&
            player.playbackState != Player.STATE_IDLE
        ) {
            player.seekTo(player.currentPosition)
        }
    }
}
