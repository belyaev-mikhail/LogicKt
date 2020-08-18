package ru.spbstu

sealed class SStream<out T>: Sequence<T>
object SSNil: SStream<Nothing>() {
    object NilIterator: Iterator<Nothing> {
        override fun hasNext(): Boolean = false
        override fun next(): Nothing = throw NoSuchElementException()
    }
    override fun iterator(): Iterator<Nothing> = NilIterator

    override fun toString(): String = "[]"
}

data class SSCons<out T>(val head: T, val lazyTail: Lazy<SStream<T>>? = null): SStream<T>() {
    constructor(head: T, fTail: () -> SStream<T>): this(head, lazy(fTail))

    val tail get() = lazyTail?.value ?: SSNil
    override fun iterator(): Iterator<T> = iterator {
        var current: SStream<T> = this@SSCons
        loop@while(true) {
            when(current) {
                is SSCons -> {
                    yield(current.head)
                    current = current.tail
                }
                is SSNil -> break@loop
            }
        }
    }

    override fun toString(): String = buildString {
        append('[')
        var current: SStream<T> = this@SSCons
        while(current is SSCons) {
            append(current.head)
            val lazyTail = current.lazyTail
            if(lazyTail != null) {
                append(", ")

                if(lazyTail.isInitialized()) {
                    current = lazyTail.value
                } else {
                    append("...")
                    break
                }
            } else break
        }

        append(']')
    }
}

infix fun <T> SStream<T>.mix(that: () -> SStream<T>): SStream<T> = when (this) {
    is SSNil -> that()
    is SSCons -> {
        SSCons(head) { that().mix { tail } }
    }
}

fun <T, U> SStream<T>.map(body: (T) -> U): SStream<U> = when(this) {
    is SSNil -> SSNil
    is SSCons -> SSCons(body(head)) { tail.map(body) }
}

fun <T, U> SStream<T>.mapNotNull(body: (T) -> U?): SStream<U> = when(this) {
    is SSNil -> SSNil
    is SSCons -> run {
        var current: SStream<T> = this
        while(current is SSCons) {
            val bhead = body(current.head)
            if(bhead != null) {
                return@run SSCons(bhead) { tail.mapNotNull(body) }
            }
            current = current.tail
        }
        SSNil
    }
}

fun <T, U> SStream<T>.mixMap(body: (T) -> SStream<U>): SStream<U> = when(this) {
    is SSNil -> SSNil
    is SSCons -> body(head) mix { tail.mixMap(body) }
}

fun <T> Iterator<T>.toSStream(): SStream<T> = when {
    !hasNext() -> SSNil
    else -> SSCons(next()) { toSStream() }
}