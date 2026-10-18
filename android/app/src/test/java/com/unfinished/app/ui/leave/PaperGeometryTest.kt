package com.unfinished.app.ui.leave

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class PaperGeometryTest {
    @Test fun timingKeepsPrintedPageThenFoldsAndDriftsWithoutReplacingMesh() {
        assertEquals(SealFrame(0f, 0f, SealPhase.WITHDRAWING), sealFrame(0f))
        assertEquals(SealPhase.FOLDING, sealFrame(1500f).phase)
        assertEquals(SealPhase.READY, sealFrame(3200f).phase)
        assertEquals(SealPhase.DRIFTING, sealFrame(4000f).phase)
        assertEquals(SealFrame(1f, 1f, SealPhase.COMPLETE), sealFrame(5450f))
        assertEquals(SealFrame(1f, 0f, SealPhase.COMPLETE), sealFrame(4000f, quiet = true))
    }

    @Test fun initialNetIsExactlyTheSamePageAndFinalHullHasAuthoredSharedVertices() {
        val original = paperPose(0f, 360f, 640f)
        assertEquals(PaperPoint(-180f, -320f), original.edge.first())
        assertEquals(PaperPoint(0f, 0f), original.hub)
        val boat = paperPose(1f, 160f, 300f)
        assertEquals(PaperPoint(-68f, -3f), boat.edge.first())
        assertEquals(PaperPoint(3f, 13f), boat.hub)
        assertFalse(boat.printTop)
        assertFalse(boat.printBottom)
    }

    @Test fun affineTrianglesMapActualSourceCornersRatherThanApproximateBounds() {
        val source = listOf(PaperPoint(0f, 0f), PaperPoint(2f, 0f), PaperPoint(0f, 3f))
        val target = listOf(PaperPoint(10f, 20f), PaperPoint(14f, 22f), PaperPoint(13f, 29f))
        val matrix = affineTriangle(source, target)
        assertEquals(2f, matrix[0], .00001f)
        assertEquals(1f, matrix[1], .00001f)
        assertEquals(10f, matrix[2], .00001f)
        assertEquals(1f, matrix[3], .00001f)
        assertEquals(3f, matrix[4], .00001f)
        assertEquals(20f, matrix[5], .00001f)
    }

    @Test fun allAuthoredPhasesRemainFiniteAcrossRealWindowAspectChanges() {
        for ((width, height) in listOf(320f to 340f, 360f to 640f, 600f to 960f)) {
            for (step in 0..1000) {
                val pose = paperPose(step / 1000f, width, height)
                assertEquals(10, pose.edge.size)
                assertTrue((pose.edge + pose.hub).all { it.x.isFinite() && it.y.isFinite() })
                for (face in 0..9) {
                    val matrix = affineTriangle(sourceTriangle(width, height, face), pose.triangle(face))
                    assertTrue(matrix.all { it.isFinite() })
                }
            }
        }
    }

    @Test fun oldPortraitSnapshotStaysCompleteInShortOrLandscapeWindows() {
        for ((width, height) in listOf(360f to 240f, 960f to 360f, 360f to 800f)) {
            val page = printedPageSize(width, height, 360f, 800f)
            assertTrue("Same page may not extend outside the new viewport", page.x <= width && page.y <= height)
            assertEquals(360f / 800f, page.x / page.y, .00001f)
            assertEquals(1f, paperScale(0f, page.x, 120f), 0f)
            val final = paperPose(1f, page.x, page.y)
            val scale = paperScale(1f, page.x, 120f)
            assertEquals(102f, (final.edge.maxOf { it.x } - final.edge.minOf { it.x }) * scale, .001f)
        }
    }
}
