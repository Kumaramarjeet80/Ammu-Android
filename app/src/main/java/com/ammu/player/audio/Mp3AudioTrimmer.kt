package com.ammu.player.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object Mp3AudioTrimmer {

    private const val BUFFER_SIZE = 256 * 1024

    suspend fun trimAudio(
        context: Context,
        sourceFilePath: String,
        outputFileName: String,
        startSec: Float,
        endSec: Float,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val sourceFile = File(sourceFilePath)
        if (!sourceFile.exists()) {
            throw IllegalArgumentException("Source file does not exist: $sourceFilePath")
        }

        val clipsDir = File(context.filesDir, "Ammu_Clips").apply { if (!exists()) mkdirs() }
        val outputFile = File(clipsDir, outputFileName)

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(sourceFile.absolutePath)
            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || trackFormat == null) {
                throw IllegalStateException("No audio track found in $sourceFilePath")
            }

            extractor.selectTrack(audioTrackIndex)

            val startUs = (startSec * 1_000_000).toLong()
            val endUs = (endSec * 1_000_000).toLong()

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            // Detect output format: MP4 for AAC/M4A, or general muxing
            val muxerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            muxer = MediaMuxer(outputFile.absolutePath, muxerFormat)
            val muxerTrackIndex = muxer.addTrack(trackFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            val totalDurationUs = (endUs - startUs).coerceAtLeast(1)

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)

                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }

                bufferInfo.presentationTimeUs = extractor.sampleTime

                if (bufferInfo.presentationTimeUs > endUs) {
                    break
                }

                if (bufferInfo.presentationTimeUs >= startUs) {
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

                    val currentProgress = ((bufferInfo.presentationTimeUs - startUs).toFloat() / totalDurationUs)
                        .coerceIn(0f, 1f)
                    onProgress(currentProgress)
                }

                extractor.advance()
            }

            Log.i("Mp3AudioTrimmer", "Audio trimmed successfully: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            Log.e("Mp3AudioTrimmer", "Trimming failed via MediaMuxer: ${e.message}", e)
            // Fallback: byte slice if uncompressed/raw or direct copy with trim metadata
            throw e
        } finally {
            try {
                extractor.release()
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.w("Mp3AudioTrimmer", "Clean up warning: ${e.message}")
            }
        }
    }
}
