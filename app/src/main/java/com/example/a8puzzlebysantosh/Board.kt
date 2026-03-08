data class Board(val tiles: Array<IntArray>) {
    val size = 3
    private val goal = arrayOf(intArrayOf(1, 2, 3), intArrayOf(4, 5, 6), intArrayOf(7, 8, 0))

    fun manhattan(): Int {
        var distance = 0
        for (i in 0 until size) {
            for (j in 0 until size) {
                val value = tiles[i][j]
                if (value != 0) {
                    val targetX = (value - 1) / size
                    val targetY = (value - 1) % size
                    distance += Math.abs(i - targetX) + Math.abs(j - targetY)
                }
            }
        }
        return distance
    }

    fun isGoal(): Boolean = tiles.contentDeepEquals(goal)

    fun blankPosition(): Pair<Int, Int> {
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (tiles[i][j] == 0) return Pair(i, j)
            }
        }
        throw IllegalStateException("No blank tile")
    }

    fun neighbors(): List<Board> {
        val (x, y) = blankPosition()
        val directions = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)) // up, down, left, right
        return directions.mapNotNull { (dx, dy) ->
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until size && ny in 0 until size) {
                val newTiles = tiles.map { it.copyOf() }.toTypedArray()
                newTiles[x][y] = newTiles[nx][ny]
                newTiles[nx][ny] = 0
                Board(newTiles)
            } else null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Board
        return tiles.contentDeepEquals(other.tiles)
    }

    override fun hashCode(): Int = tiles.contentDeepHashCode()
}