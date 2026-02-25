package com.example.tabaudit

import android.content.Context
import android.media.MediaPlayer

object SoundManager {

    // We pass the specific Resource ID (e.g., R.raw.melody_login)
    fun play(context: Context, soundResId: Int) {
        try {
            val mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer.setOnCompletionListener { mp -> mp.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}