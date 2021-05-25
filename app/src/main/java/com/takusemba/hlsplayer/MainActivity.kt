package com.takusemba.hlsplayer

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
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

        //timerObj.schedule(timerTaskObj, 0, 1000 * 60 * 15)
        timerObj.schedule(timerTaskObj, 0, 1000 * 60 * 15)
    }

    private fun play(urlString : String) {
        player = ExoPlayerFactory.newSimpleInstance(
                DefaultRenderersFactory(this), DefaultTrackSelector(), DefaultLoadControl())
        val playerView = findViewById<SimpleExoPlayerView>(R.id.player_view)
        //val uri = Uri.parse(BuildConfig.STREAMING_URL)

        val uri = Uri.parse(urlString)

        playerView.player = player

        val dataSourceFactory = DefaultDataSourceFactory(this, "user-agent")
        val mediaSource = HlsMediaSource(uri, dataSourceFactory, handler, null)

        player?.prepare(mediaSource)
        player?.playWhenReady = true
    }

    private fun stop() {
        player?.playWhenReady = false
        player?.release()
    }
}
