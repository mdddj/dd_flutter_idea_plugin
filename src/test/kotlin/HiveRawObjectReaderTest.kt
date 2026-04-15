import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vm.hive.HiveRawField
import vm.hive.HiveRawObject
import vm.hive.HiveRawObjectReader

class HiveRawObjectReaderTest {

    @Test
    fun `decodes unknown custom object into indexed fields`() {
        val payload = byteArrayOf(
            2,
            0,
            4,
            5, 0, 0, 0,
            'h'.code.toByte(), 'e'.code.toByte(), 'l'.code.toByte(), 'l'.code.toByte(), 'o'.code.toByte(),
            1,
            1,
            0, 0, 0, 0, 0, 0, 36, 64,
        )
        val buffer = byteArrayOf(
            42,
            0,
            payload.size.toByte(), 0, 0, 0,
            *payload,
        )

        val result = HiveRawObjectReader(emptyMap(), buffer).read()

        assertTrue(result is HiveRawObject)
        val rawObject = result as HiveRawObject
        assertEquals("Type#42", rawObject.name)
        assertEquals(
            listOf(
                HiveRawField("#0", "hello"),
                HiveRawField("#1", 10L),
            ),
            rawObject.fields,
        )
    }
}
