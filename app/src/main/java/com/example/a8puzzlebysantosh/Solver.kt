import java.util.PriorityQueue

class Solver(private val initial: Board) {
    private val solution: MutableList<Board> = mutableListOf()
    val minMoves: Int

    init {
        val pq = PriorityQueue<SearchNode> { a, b -> a.priority - b.priority }
        pq.add(SearchNode(initial, 0, null))

        val visited = mutableSetOf<Board>()
        var found = false

        while (pq.isNotEmpty() && !found) {
            val current = pq.poll()
            if (current.board.isGoal()) {
                reconstructPath(current)
                found = true
            } else if (!visited.contains(current.board)) {
                visited.add(current.board)
                for (neighbor in current.board.neighbors()) {
                    if (!visited.contains(neighbor)) {
                        pq.add(SearchNode(neighbor, current.moves + 1, current))
                    }
                }
            }
        }
        minMoves = solution.size - 1
    }

    private fun reconstructPath(node: SearchNode?) {
        var current: SearchNode? = node
        while (current != null) {
            solution.add(0, current.board)
            current = current.previous
        }
    }

    fun solution(): List<Board> = solution
}

data class SearchNode(
    val board: Board,
    val moves: Int,
    val previous: SearchNode?
) {
    val priority: Int = moves + board.manhattan()
}