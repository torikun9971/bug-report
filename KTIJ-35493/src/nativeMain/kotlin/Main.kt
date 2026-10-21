import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import openal.AL_BUFFER
import openal.AL_FORMAT_MONO16
import openal.AL_FORMAT_MONO8
import openal.AL_FORMAT_STEREO16
import openal.AL_FORMAT_STEREO8
import openal.alBufferData
import openal.alGenBuffers
import openal.alGenSources
import openal.alSourcePlay
import openal.alSourcei
import openal.alcCreateContext
import openal.alcMakeContextCurrent
import openal.alcOpenDevice

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val path = readln().toPath()
    val source = FileSystem.SYSTEM.source(path).buffer()

    source.skip(4) // RIFF
    source.skip(4) // size
    source.skip(4) // WAVE

    source.skip(4) // fmt

    // fmt size
    val fmtSize = source.readIntLe()

    // 1 = PCM Wave
    source.skip(2)

    // channels
    val channels = source.readShortLe()

    // samplesPerSec
    val samplesPerSec = source.readIntLe()

    // avgBytesPerSec
    source.skip(4)

    // blockAlign
    source.skip(2)

    // bitsPerSample
    val bitsPerSample = source.readShortLe()

    if (fmtSize > 16) {
        source.skip(fmtSize - 16L)
    }

    source.skip(4) // LIST

    with(source.readIntLe()) {
        source.skip(this.toLong())
    }

    source.skip(4) // data

    val dataSize = source.readIntLe()
    val data = ByteArray(dataSize)

    source.readFully(data)

    source.close()

    memScoped {
        val device = alcOpenDevice(null)
        val context = alcCreateContext(device, null)

        alcMakeContextCurrent(context)

        val bufferId = alloc<UIntVar>()
        val format: Int

        alGenBuffers(1, bufferId.ptr)

        if (channels == 1.toShort()) {
            if (bitsPerSample == 16.toShort()) {
                format = AL_FORMAT_MONO16
            } else {
                format = AL_FORMAT_MONO8
            }
        } else {
            if (bitsPerSample == 16.toShort()) {
                format = AL_FORMAT_STEREO16
            } else {
                format = AL_FORMAT_STEREO8
            }
        }

        data.usePinned { data ->
            alBufferData(bufferId.value, format, data.addressOf(0), dataSize, samplesPerSec)
        }

        val sourceId = alloc<UIntVar>()
        alGenSources(1, sourceId.ptr)

        alSourcei(sourceId.value, AL_BUFFER, bufferId.value.toInt())
        alSourcePlay(sourceId.value)

        @Suppress("ControlFlowWithEmptyBody")
        while (true) {}
    }
}