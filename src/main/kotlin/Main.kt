import kotlin.math.max
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

    inner class BoolBitVector(var boolArr: BooleanArray = this.toBoolArray()) {
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
        val length: Int
            get() { return boolArr.size }
    }

    private fun toBoolArray(): BooleanArray {
        val result = BooleanArray(length)
        for (i in 0 until length) {
            if (content[i] == '1') result[i] = true
        }
        return result
    }

    fun toBoolBitVector(): BoolBitVector {
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

class RippleCarryAdder(var augend: BitVector = BitVector("0"),
                       var addend: BitVector = BitVector("0"),
                       carryIn: Boolean = false) {
    private var length = max(augend.length, addend.length)
    private var carry = carryIn
    private var sum = BitVector.zeroes(length).toBoolBitVector()
    private var a = false
    private var b = false
    fun add(): BitVector {
        val augend2 = augend.toBoolBitVector().reversed()
        val addend2 = addend.toBoolBitVector().reversed()
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
    fun getSum(): BitVector {
        return sum.toBitVector()
    }
    fun getExtendedSum(): BitVector {
        var result = getSum().padWith(1)
        if (carryOut) result[0] = '1'
        return result
    }
}

class MultiplyAccumulate(var multiplier: BitVector = BitVector("0"),
                         var multiplicand: BitVector = BitVector("0")) {
    private var mr = multiplier.toBoolBitVector()
    private var md = multiplicand.toBoolBitVector()
    private var product = BitVector.zeroes(multiplier.length + multiplicand.length)
    fun multiply(): BitVector {
        for (shamt in 0 until mr.length) {
            if (mr.reversed()[shamt]) { // Add shifted multiplier
                val shiftedMd = shift(multiplicand, shamt).padTo(product.length)
                product = RippleCarryAdder(product, shiftedMd).add()
            }
        }
        return product
    }

    fun shift(bv: BitVector, shamt: Int): BitVector {
        var newContent = bv.content
        for (i in 0 until shamt)
            newContent += "0"
        return BitVector(newContent)
    }
}

fun main(args : Array<String>) {
    var augend = if (args[0].isNotEmpty()) BitVector(args[0]) else BitVector("10001")
    var addend = try { BitVector(args[1]) } catch(e: IndexOutOfBoundsException) { BitVector("0010") }

    // Whichever is smaller will be zero-padded to the length of the larger
    augend.padTo(addend.length)
    addend.padTo(augend.length)

    println("Processing as Unsigned Integers:")

    var rca = RippleCarryAdder(augend, addend)
    println("Addend 1: ${augend.toDecimal()}_10 = ${augend.content}_2")
    println("Addend 2: ${addend.toDecimal()}_10 = ${addend.content}_2")
    var sum = rca.add()
    println("Sum: ${sum.toDecimal()}_10 = ${sum.content}_2")
    if (rca.carryOut) {
        println("Integer Overflow! Extending...")
        sum = rca.getExtendedSum()
        println("Extended Sum: ${sum.toDecimal()}_10 = ${sum.content}_2")
    }

    println("Processing as Signed Two's Complement Integers:")

    val augend2 = SignedBitVector(augend).extend()
    val addend2 = SignedBitVector(addend).extend()

    println("Sign extending...")
    println("Addend 1: ${augend2.toDecimal()}_10 = ${augend2.content}_2")
    println("Addend 2: ${addend2.toDecimal()}_10 = ${addend2.content}_2")

    val sum2 = SignedBitVector(RippleCarryAdder(augend2, addend2).add())
    println("Signed Sum: ${sum2.toDecimal()}_10 = ${sum2.content}_2")
    if (rca.carryOut) {
        println("Integer Overflow! Extending...")
        sum = rca.getExtendedSum()
        println("Extended Signed Sum: ${sum.toDecimal()}_10 = ${sum.content}_2")
    }

    println("Multiplying as Unsigned Integers:")
    val mac = MultiplyAccumulate(augend, addend)
    val product = mac.multiply()
    println("Base 10: ${augend.toDecimal()} * ${addend.toDecimal()} = ${product.toDecimal()}")
    println("Base 2: ${augend.content} * ${addend.content} = ${product.content}")
}
