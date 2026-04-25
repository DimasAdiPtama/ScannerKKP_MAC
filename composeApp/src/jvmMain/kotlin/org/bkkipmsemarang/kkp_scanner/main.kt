package org.bkkipmsemarang.kkp_scanner

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamDriver
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

fun main() {
    // 1. Initialize OpenCV for Mac ARM64 support
    try {
        println("Initializing OpenPnP OpenCV...")
        OpenCV.loadShared()
        
        // Trigger macOS camera permission request on the main thread
        // This is crucial for macOS security
        val tempCapture = VideoCapture(0)
        if (tempCapture.isOpened) {
            println("Camera permission granted and device opened successfully.")
            tempCapture.release()
        } else {
            println("Warning: Could not open camera on main thread. Permission might be required.")
        }
        
        Webcam.setDriver(OpenPnPWebcamDriver())
        println("Webcam driver successfully set to OpenPnP OpenCV")
    } catch (e: Throwable) {
        println("Failed to initialize OpenCV driver: ${e.message}")
        e.printStackTrace()
    }

    try {
        println("Initializing application...")
        val serviceAccount: InputStream? = object {}.javaClass.getResourceAsStream("/service-account.json")
        if (serviceAccount != null) {
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("Firebase initialized successfully.")
            }
        }
    } catch (e: Exception) {
        println("Initialization error: ${e.message}")
    }

    application {
        Window(onCloseRequest = ::exitApplication, title = "ScannerKKP") {
            App()
        }
    }
}

/**
 * A custom WebcamDriver that uses OpenPnP OpenCV to support Mac ARM64.
 */
class OpenPnPWebcamDriver : WebcamDriver {
    override fun getDevices(): List<WebcamDevice> {
        return listOf(OpenPnPWebcamDevice(0))
    }

    override fun isThreadSafe(): Boolean = true
}

class OpenPnPWebcamDevice(private val index: Int) : WebcamDevice {
    private var capture: VideoCapture? = null
    private val open = AtomicBoolean(false)
    private var resolution = Dimension(640, 480)

    override fun getName(): String = "Camera $index"

    override fun getResolutions(): Array<Dimension> = arrayOf(Dimension(640, 480), Dimension(1280, 720))

    override fun getResolution(): Dimension = resolution

    override fun setResolution(res: Dimension) {
        this.resolution = res
    }

    override fun getImage(): BufferedImage? {
        if (!open.get()) return null
        val mat = Mat()
        try {
            if (capture?.read(mat) == true && !mat.empty()) {
                return matToBufferedImage(mat)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mat.release()
        }
        return null
    }

    override fun open() {
        if (open.compareAndSet(false, true)) {
            println("Opening camera $index via OpenCV...")
            capture = VideoCapture(index)
            capture?.set(Videoio.CAP_PROP_FRAME_WIDTH, resolution.width.toDouble())
            capture?.set(Videoio.CAP_PROP_FRAME_HEIGHT, resolution.height.toDouble())
            if (capture?.isOpened == false) {
                open.set(false)
                println("Failed to open camera $index")
                throw RuntimeException("Could not open camera $index")
            }
            println("Camera $index opened successfully.")
        }
    }

    override fun close() {
        if (open.compareAndSet(true, false)) {
            println("Closing camera $index...")
            capture?.release()
            capture = null
        }
    }

    override fun dispose() {
        close()
    }

    override fun isOpen(): Boolean = open.get()

    private fun matToBufferedImage(mat: Mat): BufferedImage {
        val width = mat.cols()
        val height = mat.rows()
        val channels = mat.channels()
        val sourcePixels = ByteArray(width * height * channels)
        mat.get(0, 0, sourcePixels)
        val image = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
        val targetPixels = (image.raster.dataBuffer as java.awt.image.DataBufferByte).data
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.size)
        return image
    }
}
