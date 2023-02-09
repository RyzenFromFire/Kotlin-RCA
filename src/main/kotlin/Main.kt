import kotlin.math.pow

/*
 * Author: Maxwell Phillips
 * Assignment: ECCS 2431 Kotlin Programming Assignment #4
 * Purpose: Signed Ripple Carry Adder
 * Argument Format: Two Binary Bit Strings composed of 0s and 1s
 */
open class BitVector(var content: String = "", length: Int = content.length) {
    var length: Int = length
        protected set(newLength) {
            if (newLength >= 0) {
                field = newLength
            }
        }

    fun truncate(newLength: Int) {
        if (newLength in 0 until length) {
            // truncate content to (newLength) least significant bits
            content = content.takeLast(newLength)
            length = newLength
        }
    }

    fun padTo(newLength: Int): BitVector {
        if (newLength > length) {
            // pad content with zeroes
            var newContent = ""
            for (i in 0 until newLength - length)
                newContent += '0'
            newContent += content
            content = newContent
            length = newLength
            return BitVector(newContent, newLength)
        }
        return this
    }

    fun padWith(bits: Int): BitVector {
        return padTo(length + bits)
    }

    companion object {
        fun zeroes(bits: Int): BitVector {
            return BitVector(CharArray(bits) { '0' }.joinToString(""))
        }
    }

    fun clear() { truncate(0) }

    operator fun get(index: Int): Char {
        return content[index]
    }

    operator fun set(index: Int, value: Char) {
        val arr = content.toCharArray()
        arr[index] = value;
        content = arr.joinToString("")
    }

    inner class BoolBitVector(var boolArr: BooleanArray = toBoolArray()) {
        override fun toString(): String {
            var str = "[ "
            for (b in boolArr) str += "$b "
            str += "]"
            return str
        }
        fun reversed(): BoolBitVector {
            return BoolBitVector(boolArr.reversed().toBooleanArray())
        }
        fun toBitVector(): BitVector {
            var result = ""
            for (i in 0 until boolArr.size) {
                result += if (boolArr[i]) '1' else '0'
            }
            return BitVector(result)
        }
        operator fun get(index: Int): Boolean {
            return boolArr[index]
        }
        operator fun set(index: Int, value: Boolean) {
            boolArr[index] = value;
        }
    }

    private fun toBoolArray(): BooleanArray {
        val result = BooleanArray(length)
        for (i in 0 until length) {
            if (content[i] == '1') result[i] = true
        }
        return result
    }

    fun getBoolBitVector(): BoolBitVector {
        return BoolBitVector()
    }

    open fun toDecimal(): Int {
        var sum: Int = 0
        for (i in 0 until length) {
            if (content.reversed()[i] == '1') {
                sum += (2.0).pow(i.toDouble()).toInt()
            }
        }
        return sum
    }
}

class SignedBitVector(content: String = "",
                      length: Int = content.length)
                      : BitVector(content, length) {
    constructor(bv: BitVector): this(bv.content, bv.length)
    fun extend(bits: Int = 1): SignedBitVector {
        padWith(bits)
        val newContent = super.content.toCharArray()
        for (i in bits - 1 downTo 0)
            newContent[i] = newContent[i + 1]
        super.content = newContent.joinToString("")
        return this
    }

    override fun toDecimal(): Int {
        var sum: Int = 0
        for (i in 0 until length) {
            if (content.reversed()[i] == '1') {
                if (i == length - 1) {
                    sum -= (2.0).pow(i.toDouble()).toInt()
                } else {
                    sum += (2.0).pow(i.toDouble()).toInt()
                }
            }
        }
        return sum
    }
}

class RippleCarryAdder(var length: Int, carryIn: Boolean = false) {
    private var carry = carryIn
    var sum = BitVector.zeroes(length).getBoolBitVector()
    private var a = false
    private var b = false
    fun add(augend: BitVector, addend: BitVector): BitVector {
        val augend2 = augend.getBoolBitVector().reversed()
        val addend2 = addend.getBoolBitVector().reversed()
        for (i in 0 until length) {
            a = augend2[i]
            b = addend2[i]
            sum[i] = ( a.xor(b) ).xor(carry)
            carry = (a && b) || (a.xor(b) && carry)
        }
        return sum.reversed().toBitVector()
    }
    val carryOut: Boolean
        get() { return carry }
}

fun main(args : Array<String>) {
    val augend = if (args[0].isNotEmpty()) BitVector(args[0]) else BitVector("10001")
    val addend = try { BitVector(args[1]) } catch(e: IndexOutOfBoundsException) { BitVector("0001") }

    // Whichever is smaller will be zero-padded to the length of the larger
    augend.padTo(addend.length)
    addend.padTo(augend.length)

    println("Addend 1: ${augend.toDecimal()}_10 = ${augend.content}_2")
    println("Addend 2: ${addend.toDecimal()}_10 = ${addend.content}_2")

    // both lengths are the same now, so it doesn't matter which one is used
    val rca = RippleCarryAdder(augend.length)
    val sum = rca.add(augend, addend)
    println("Sum: ${sum.toDecimal()}_10 = ${sum.content}_2")
}
