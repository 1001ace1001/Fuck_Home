package cn.fuckhome.xiaowine.utils

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

/** JVM-only regression tests exercising the same adapters as production hooks. */
class ModernHookAdapterTest {
    private class Invocation {
        var calls = 0
        var received: Array<out Any?>? = null
        var originalFailure: RuntimeException? = null
        var originalResult: Any? = "original"
        private val originalArgs = java.util.Collections.unmodifiableList(listOf<Any?>(1))
        private val executable = String::class.java.getDeclaredMethod("substring", Int::class.javaPrimitiveType!!)
        val chain = Proxy.newProxyInstance(
            XposedInterface.Chain::class.java.classLoader,
            arrayOf(XposedInterface.Chain::class.java)
        ) { proxy, method, supplied ->
            when (method.name) {
                "getExecutable" -> executable
                "getThisObject" -> "receiver"
                "getArgs" -> originalArgs
                "getArg" -> originalArgs[supplied!![0] as Int]
                "proceed" -> {
                    calls++
                    @Suppress("UNCHECKED_CAST")
                    val forwarded = if (supplied.isNullOrEmpty()) originalArgs.toTypedArray()
                        else supplied[0] as Array<out Any?>
                    received = forwarded.copyOf()
                    originalFailure?.let { throw it }
                    originalResult
                }
                "toString" -> "FakeChain"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === supplied?.get(0)
                else -> error("Unexpected Chain operation: ${method.name}")
            }
        } as XposedInterface.Chain
    }

    @Test fun beforeNullResultSkipsOriginal() {
        val invocation = Invocation()
        assertNull(executeBefore(invocation.chain) { it.result = null })
        assertEquals(0, invocation.calls)
    }

    @Test fun beforeChangesArgumentsWithoutMutatingChainList() {
        val invocation = Invocation()
        assertEquals("original", executeBefore(invocation.chain) { it.args[0] = 7 })
        assertEquals(1, invocation.calls)
        assertArrayEquals(arrayOf<Any?>(7), invocation.received)
        assertEquals(listOf<Any?>(1), invocation.chain.args)
    }

    @Test fun afterCanReplaceOriginalResult() {
        val invocation = Invocation()
        var observedResult: Any? = null
        assertEquals("changed", executeAfter(invocation.chain) {
            observedResult = it.result
            it.result = "changed"
        })
        assertEquals("original", observedResult)
        assertEquals(1, invocation.calls)
    }

    @Test fun afterCanRecoverOriginalFailure() {
        val invocation = Invocation()
        val failure = IllegalStateException("original failure")
        invocation.originalFailure = failure
        var observedFailure: Throwable? = null
        assertEquals("recovered", executeAfter(invocation.chain) {
            observedFailure = it.throwable
            it.result = "recovered"
        })
        assertSame(failure, observedFailure)
        assertEquals(1, invocation.calls)
    }

    @Test fun unrecoveredOriginalFailurePropagates() {
        val invocation = Invocation()
        val failure = IllegalStateException("original failure")
        invocation.originalFailure = failure
        var observedFailure: Throwable? = null
        try {
            executeAfter(invocation.chain) { observedFailure = it.throwable }
            fail("Original failure must propagate")
        } catch (actual: IllegalStateException) {
            assertSame(failure, actual)
        }
        assertSame(failure, observedFailure)
        assertEquals(1, invocation.calls)
    }

    @Test fun failedBeforeRestoresShortCircuitStateAndArguments() {
        val invocation = Invocation()
        assertEquals("original", executeBefore(invocation.chain) {
            it.args[0] = 99
            it.result = null
            throw IllegalArgumentException("callback failure")
        })
        assertEquals(1, invocation.calls)
        assertArrayEquals(arrayOf<Any?>(1), invocation.received)
    }

    @Test fun failedAfterRestoresOriginalResultWithoutRepeatingCall() {
        val invocation = Invocation()
        assertEquals("original", executeAfter(invocation.chain) {
            it.result = "discard this result"
            throw IllegalArgumentException("callback failure")
        })
        assertEquals(1, invocation.calls)
    }

    @Test fun failedAfterRestoresOriginalFailure() {
        val invocation = Invocation()
        val failure = IllegalStateException("original failure")
        invocation.originalFailure = failure
        try {
            executeAfter(invocation.chain) {
                it.result = "discard this recovery"
                throw IllegalArgumentException("callback failure")
            }
            fail("Original failure must propagate")
        } catch (actual: IllegalStateException) {
            assertSame(failure, actual)
        }
        assertEquals(1, invocation.calls)
    }
}
