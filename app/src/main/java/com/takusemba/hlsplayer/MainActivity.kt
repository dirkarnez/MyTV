package com.takusemba.hlsplayer

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    private val handler = Handler()
    private val timerHandler = Handler()
    private var client = OkHttpClient()
    private var request = Request.Builder()
            .url("https://d1jithvltpp1l1.cloudfront.net/getLiveURL?channelno=332&mode=prod&audioCode=&format=HLS")
            .get()
            .build()

    private var player: SimpleExoPlayer? = null

    /**
     * If you handled the event, return true.
     * If you want to allow the event to be handled by the next receiver, return false.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            stop()
            finish();
            return true;
        }

        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            Toast.makeText(this@MainActivity, String.format("KeyDown %d", keyCode),Toast.LENGTH_SHORT).show()
            return true;
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val timerObj = Timer()
        val timerTaskObj: TimerTask = object : TimerTask() {
            override fun run() {
                timerHandler.post {
                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "onFailure", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onResponse(call: Call?, response: Response?) {
                            var hlsUrl: String? = null
                            response?.body().use {
                                hlsUrl = it?.string()
                            }

                            if (hlsUrl == null) {
                                return
                            }

                            runOnUiThread {
                                try {
                                    stop()
                                    play(JSONObject(hlsUrl)
                                            .getJSONObject("asset")
                                            .getJSONObject("hls")
                                            .getJSONArray("adaptive")
                                            .get(0).toString())
                                } catch (e :Exception) {
                                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                }
            }
        }

        timerObj.schedule(timerTaskObj, 0, 1000 * 10)
        //timerObj.schedule(timerTaskObj, 0, 1000 * 60 * 15)
    }

    private fun play(urlString : String) {
        player = ExoPlayerFactory.newSimpleInstance(
                DefaultRenderersFactory(this), DefaultTrackSelector(), DefaultLoadControl())
        val playerView = findViewById<SimpleExoPlayerView>(R.id.player_view)

        val uri = Uri.parse(urlString)

        playerView.player = player

        val dataSourceFactory = DefaultDataSourceFactory(this, "user-agent")
        val mediaSource = HlsMediaSource(uri, dataSourceFactory, handler, object : AdaptiveMediaSourceEventListener {
            override fun onLoadStarted(dataSpec: DataSpec?, dataType: Int, trackType: Int, trackFormat: Format?, trackSelectionReason: Int, trackSelectionData: Any?, mediaStartTimeMs: Long, mediaEndTimeMs: Long, elapsedRealtimeMs: Long) {
                Toast.makeText(this@MainActivity, "onLoadStarted", Toast.LENGTH_SHORT).show()
            }

            override fun onLoadCompleted(dataSpec: DataSpec?, dataType: Int, trackType: Int, trackFormat: Format?, trackSelectionReason: Int, trackSelectionData: Any?, mediaStartTimeMs: Long, mediaEndTimeMs: Long, elapsedRealtimeMs: Long, loadDurationMs: Long, bytesLoaded: Long) {
                Toast.makeText(this@MainActivity, "onLoadCompleted", Toast.LENGTH_SHORT).show()
            }

            override fun onLoadCanceled(dataSpec: DataSpec?, dataType: Int, trackType: Int, trackFormat: Format?, trackSelectionReason: Int, trackSelectionData: Any?, mediaStartTimeMs: Long, mediaEndTimeMs: Long, elapsedRealtimeMs: Long, loadDurationMs: Long, bytesLoaded: Long) {
                Toast.makeText(this@MainActivity, "onLoadCanceled", Toast.LENGTH_SHORT).show()
            }

            override fun onLoadError(dataSpec: DataSpec?, dataType: Int, trackType: Int, trackFormat: Format?, trackSelectionReason: Int, trackSelectionData: Any?, mediaStartTimeMs: Long, mediaEndTimeMs: Long, elapsedRealtimeMs: Long, loadDurationMs: Long, bytesLoaded: Long, error: IOException?, wasCanceled: Boolean) {
                Toast.makeText(this@MainActivity, "onLoadError", Toast.LENGTH_SHORT).show()
            }

            override fun onUpstreamDiscarded(trackType: Int, mediaStartTimeMs: Long, mediaEndTimeMs: Long) {
                Toast.makeText(this@MainActivity, "onUpstreamDiscarded", Toast.LENGTH_SHORT).show()
            }

            override fun onDownstreamFormatChanged(trackType: Int, trackFormat: Format?, trackSelectionReason: Int, trackSelectionData: Any?, mediaTimeMs: Long) {
                Toast.makeText(this@MainActivity, "onDownstreamFormatChanged", Toast.LENGTH_SHORT).show()
            }
        })

        player?.prepare(mediaSource)
        player?.playWhenReady = true
    }

    private fun stop() {
        player?.playWhenReady = false
        player?.release()
    }
}
