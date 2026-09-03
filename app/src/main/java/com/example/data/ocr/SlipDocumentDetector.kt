package com.example.data.ocr

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Detects a flat slip / document inside a photo (the largest convex 4-corner region,
 * e.g. a bright slip on a darker background) and returns a perspective-flattened copy.
 * Feeding the flattened image to QR decoding + OCR makes both dramatically more reliable
 * than running them on the raw, skewed camera frame.
 */
object SlipDocumentDetector {

    private const val TAG = "SlipFlattener"

    /** A quadrilateral covering at least this fraction of the frame counts as the slip. */
    private const val MIN_AREA_FRACTION = 0.12

    /** Working image width cap; detection runs on a downscaled copy for speed. */
    private const val MAX_WORK_WIDTH = 1000

    private var loaded: Boolean? = null

    private fun ensureLoaded(): Boolean {
        if (loaded == null) {
            loaded = runCatching { OpenCVLoader.initLocal() }.getOrDefault(false)
            Log.d(TAG, "OpenCV initLocal = $loaded")
        }
        return loaded == true
    }

    /** Ordered corners of the detected slip in the ORIGINAL bitmap's coordinate space. */
    class Quad(val tl: Point, val tr: Point, val br: Point, val bl: Point) {
        val points: List<Point> get() = listOf(tl, tr, br, bl)
    }

    /**
     * Detects the slip's four corners in [bitmap]. Returns null when OpenCV is unavailable,
     * no good 4-corner region is found, or the region is too small to be a slip.
     */
    fun detect(bitmap: Bitmap): Quad? {
        if (!ensureLoaded()) return null

        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val scale = minOf(1.0, MAX_WORK_WIDTH.toDouble() / src.cols().toDouble())
        val work = Mat()
        if (scale < 1.0) {
            Imgproc.resize(src, work, Size(), scale, scale, Imgproc.INTER_AREA)
        } else {
            src.copyTo(work)
        }

        val gray = Mat()
        Imgproc.cvtColor(work, gray, Imgproc.COLOR_RGBA2GRAY)
        val quad = try {
            detectQuadInGray(gray, minArea = work.cols() * work.rows() * MIN_AREA_FRACTION)
        } finally {
            work.release()
            src.release()
            gray.release()
        }

        // Map the downscaled corner coordinates back to the original image space
        return quad?.let { q ->
            if (scale < 1.0) {
                Quad(
                    tl = Point(q.tl.x / scale, q.tl.y / scale),
                    tr = Point(q.tr.x / scale, q.tr.y / scale),
                    br = Point(q.br.x / scale, q.br.y / scale),
                    bl = Point(q.bl.x / scale, q.bl.y / scale)
                )
            } else q
        }
    }

    /**
     * Detects the slip directly on a CameraX Y-plane (grayscale) frame — used for the live
     * preview hint without allocating a full RGB bitmap every frame. Samples every [step]th
     * pixel; returned corners are in the ORIGINAL (un-sampled) frame coordinates.
     */
    fun detectYPlane(width: Int, height: Int, rowStride: Int, yBuffer: java.nio.ByteBuffer, step: Int = 3): Quad? {
        if (!ensureLoaded()) return null
        if (width <= 0 || height <= 0) return null
        val bw = width / step
        val bh = height / step
        if (bw < 40 || bh < 40) return null
        val gray = Mat(bh, bw, org.opencv.core.CvType.CV_8UC1)
        val row = ByteArray(bw)
        for (yy in 0 until bh) {
            val base = yy * step * rowStride
            for (xx in 0 until bw) row[xx] = yBuffer.get(base + xx * step)
            gray.put(yy, 0, row)
        }
        val quad = try {
            detectQuadInGray(gray, minArea = bw * bh * MIN_AREA_FRACTION)
        } finally {
            gray.release()
        }
        return quad?.let { q ->
            Quad(
                tl = Point(q.tl.x * step, q.tl.y * step),
                tr = Point(q.tr.x * step, q.tr.y * step),
                br = Point(q.br.x * step, q.br.y * step),
                bl = Point(q.bl.x * step, q.bl.y * step)
            )
        }
    }

    /**
     * Perspective-warp the slip region out of [bitmap] into a flat, upright bitmap.
     * Returns null when no slip region is detectable (caller should use the original photo).
     */
    fun flatten(bitmap: Bitmap): Bitmap? {
        val quad = detect(bitmap) ?: return null

        val w1 = kotlin.math.hypot(quad.tl.x - quad.tr.x, quad.tl.y - quad.tr.y)
        val w2 = kotlin.math.hypot(quad.bl.x - quad.br.x, quad.bl.y - quad.br.y)
        val h1 = kotlin.math.hypot(quad.tl.x - quad.bl.x, quad.tl.y - quad.bl.y)
        val h2 = kotlin.math.hypot(quad.tr.x - quad.br.x, quad.tr.y - quad.br.y)
        val outW = maxOf(w1, w2).toInt().coerceAtLeast(1)
        val outH = maxOf(h1, h2).toInt().coerceAtLeast(1)

        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)

        val srcPts = MatOfPoint2f(
            quad.tl, quad.tr, quad.br, quad.bl
        )
        val dstPts = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(outW - 1.0, 0.0),
            Point(outW - 1.0, outH - 1.0),
            Point(0.0, outH - 1.0)
        )
        val transform = Imgproc.getPerspectiveTransform(srcPts, dstPts)
        val warped = Mat()
        try {
            Imgproc.warpPerspective(srcMat, warped, transform, Size(outW.toDouble(), outH.toDouble()), Imgproc.INTER_CUBIC)
            val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(warped, out)
            return out
        } catch (e: Exception) {
            Log.w(TAG, "flatten failed: ${e.message}")
            return null
        } finally {
            srcPts.release(); dstPts.release(); transform.release(); warped.release(); srcMat.release()
        }
    }

    /**
     * Runs the edge/contour pipeline on a grayscale [gray] Mat and returns the best 4-corner
     * polygon. Tries progressively harder to find the slip so detection does not depend on a
     * single lucky Canny threshold:
     *  1. standard edges,  2. softer edges (dim / blurry slips),  3. contrast-normalized.
     * Each pass accepts an exactly-4-corner polygon from the raw outline OR its convex hull,
     * and falls back to the rotated minimum-area rectangle of the largest slip-like contour.
     */
    private fun detectQuadInGray(gray: Mat, minArea: Double): Quad? {
        quadWithCanny(gray, minArea, 50.0, 150.0)?.let { return it }
        quadWithCanny(gray, minArea, 20.0, 70.0)?.let { return it }
        val norm = Mat()
        try {
            Imgproc.equalizeHist(gray, norm)
            quadWithCanny(norm, minArea, 20.0, 70.0)?.let { return it }
        } finally {
            norm.release()
        }
        Log.d(TAG, "no quad found (minArea=${minArea.toInt()})")
        return null
    }

    private fun quadWithCanny(gray: Mat, minArea: Double, cannyLow: Double, cannyHigh: Double): Quad? {
        val blur = Mat()
        Imgproc.GaussianBlur(gray, blur, Size(5.0, 5.0), 0.0)
        val edges = Mat()
        Imgproc.Canny(blur, edges, cannyLow, cannyHigh)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        val dilated = Mat()
        Imgproc.dilate(edges, dilated, kernel)
        val hierarchy = Mat()
        val contours = ArrayList<MatOfPoint>()
        try {
            // RETR_EXTERNAL: only outer boundaries — the slip's edge, not interior text
            Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        } catch (e: Exception) {
            Log.w(TAG, "findContours failed: ${e.message}")
            return null
        } finally {
            blur.release(); edges.release(); kernel.release(); dilated.release(); hierarchy.release()
        }
        try {
            val sorted = contours.sortedByDescending { Imgproc.contourArea(it) }
            for (contour in sorted) {
                val area = Imgproc.contourArea(contour)
                if (area < minArea) break // any smaller contour can't be the slip
                val pts2f = MatOfPoint2f(*contour.toArray())
                try {
                    val peri = Imgproc.arcLength(pts2f, true)
                    // 1) sweep the approximation epsilon until the outline has 4 corners
                    quadFromApprox(pts2f, peri)?.let { return it }
                    // 2) jagged outline: simplify the convex hull instead
                    hullQuad(contour)?.let { return it }
                } finally {
                    pts2f.release()
                }
            }
            // 3) last resort: tight rotated rectangle around the largest slip-like contour
            rectFallback(sorted, minArea, gray.cols(), gray.rows())?.let { return it }
            Log.d(TAG, "contours=${sorted.size} best=none canny=$cannyLow/$cannyHigh minArea=${minArea.toInt()}")
        } finally {
            contours.forEach { it.release() }
        }
        return null
    }

    /** approxPolyDP epsilon sweep looking for an exactly-4-corner polygon of [pts2f]. */
    private fun quadFromApprox(pts2f: MatOfPoint2f, peri: Double): Quad? {
        for (eps in listOf(0.02, 0.03, 0.045, 0.06, 0.09)) {
            val approx = MatOfPoint2f()
            var quad: Quad? = null
            try {
                Imgproc.approxPolyDP(pts2f, approx, eps * peri, true)
                if (approx.total() == 4L) quad = orderPoints(approx.toArray())
            } finally {
                approx.release()
            }
            if (quad != null) return quad
        }
        return null
    }

    /** Convex-hull fallback for outlines that never simplify to exactly 4 raw corners. */
    private fun hullQuad(contour: MatOfPoint): Quad? {
        val hullIdx = org.opencv.core.MatOfInt()
        try {
            Imgproc.convexHull(contour, hullIdx)
            val all = contour.toArray()
            val hullPts = hullIdx.toArray().map { all[it] }.toTypedArray()
            if (hullPts.size < 4) return null
            if (hullPts.size == 4) return orderPoints(hullPts)
            val hull2f = MatOfPoint2f(*hullPts)
            try {
                return quadFromApprox(hull2f, Imgproc.arcLength(hull2f, true))
            } finally {
                hull2f.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "convexHull failed: ${e.message}")
            return null
        } finally {
            hullIdx.release()
        }
    }

    /**
     * Tightest rotated rectangle around the largest contour that is big enough and roughly
     * rectangular (contour fills most of its bounding box) — catches slips whose border is
     * too soft to close into a clean 4-corner outline. Skips a region covering ~the whole
     * frame (that is the frame edge, not the slip).
     */
    private fun rectFallback(sorted: List<MatOfPoint>, minArea: Double, frameW: Int, frameH: Int): Quad? {
        val frameArea = frameW.toDouble() * frameH
        for (contour in sorted) {
            val area = Imgproc.contourArea(contour)
            if (area < minArea) break
            val pts2f = MatOfPoint2f(*contour.toArray())
            try {
                val rect = Imgproc.minAreaRect(pts2f)
                val rectArea = rect.size.width * rect.size.height
                if (rectArea < minArea || rectArea > frameArea * 0.97) continue
                if (area < rectArea * 0.5) continue // too sparse to be a filled slip
                val corners = Array(4) { Point() }
                rect.points(corners)
                val quad = orderPoints(corners)
                if (quad != null) {
                    Log.d(TAG, "rotated-rect fallback ${rect.size.width.toInt()}x${rect.size.height.toInt()}")
                    return quad
                }
            } catch (e: Exception) {
                Log.w(TAG, "minAreaRect failed: ${e.message}")
            } finally {
                pts2f.release()
            }
        }
        return null
    }

    /** Sorts 4 points into TL, TR, BR, BL order. */
    private fun orderPoints(pts: Array<Point>): Quad? {
        if (pts.size != 4) return null
        val sum = pts.map { it.x + it.y }
        val diff = pts.map { it.x - it.y }
        val tl = pts[sum.indices.minBy { sum[it] }]
        val br = pts[sum.indices.maxBy { sum[it] }]
        val tr = pts[diff.indices.maxBy { diff[it] }]
        val bl = pts[diff.indices.minBy { diff[it] }]
        return Quad(tl, tr, br, bl)
    }
}
