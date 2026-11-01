package org.solidcraft.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch

class DocumentRequestResultsTest {
    @Test
    fun completedResultCanBeTakenOnlyOnce() {
        val requests = DocumentRequestResults()

        assertTrue(requests.register(17L))
        assertTrue(requests.complete(17L, "/private/part.solidcraft"))
        assertEquals("/private/part.solidcraft", requests.take(17L))
        assertNull(requests.take(17L))
    }

    @Test
    fun cancelledRequestCannotPublishALateWorkerResult() {
        val requests = DocumentRequestResults()

        assertTrue(requests.register(23L))
        requests.cancel(23L)

        assertFalse(requests.complete(23L, "/private/part.solidcraft"))
        assertNull(requests.take(23L))
    }

    @Test
    fun cancellationDropsACompletedButUnclaimedResult() {
        val requests = DocumentRequestResults()

        assertTrue(requests.register(31L))
        assertTrue(requests.complete(31L, ""))
        requests.cancel(31L)

        assertNull(requests.take(31L))
    }

    @Test
    fun requestIdCannotBeReusedUntilItsResultIsConsumedOrCancelled() {
        val requests = DocumentRequestResults()

        assertTrue(requests.register(42L))
        assertTrue(requests.complete(42L, ""))
        assertFalse(requests.register(42L))
        assertEquals("", requests.take(42L))
        assertTrue(requests.register(42L))
    }

    @Test
    fun concurrentCancelAndCompletionNeverResurrectAnUnclaimedResult() {
        val requests = DocumentRequestResults()
        repeat(100) { iteration ->
            val id = 100L + iteration
            val start = CountDownLatch(1)
            assertTrue(requests.register(id))
            val completion = Thread {
                start.await()
                requests.complete(id, "/private/model.solidcraft")
            }
            val cancellation = Thread {
                start.await()
                requests.cancel(id)
            }

            completion.start()
            cancellation.start()
            start.countDown()
            completion.join()
            cancellation.join()

            assertNull("cancelled result must not be re-published (iteration $iteration)", requests.take(id))
        }
    }
}
