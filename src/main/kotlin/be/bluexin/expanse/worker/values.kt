package be.bluexin.expanse.worker

interface Value

data class Position(val x: Double, val y: Double, val z: Double) : Value {
    val xi: Int get() = x.toInt()
    val yi: Int get() = y.toInt()
    val zi: Int get() = z.toInt()
}

data class Health(val currentHp: Float, val maxHp: Float) : Value {
    val hpPercent: Float get() = currentHp / maxHp
}
