package com.example.a8puzzlebysantosh

import Board
import Solver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var puzzleGrid: GridLayout
    private lateinit var heuristicText: TextView
    private lateinit var movesText: TextView
    private lateinit var efficiencyText: TextView
    private var currentBoard: Board = createGoalBoard()
    private val buttons: MutableList<Button> = mutableListOf()
    private var takenMoves = 0
    private var optimalMoves = 0
    private val moveHistory: MutableList<Board> = mutableListOf()
    private var startTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tileBackground: GradientDrawable
    private lateinit var blankBackground: GradientDrawable
    private var tileSizePx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val displayMetrics = resources.displayMetrics
        tileSizePx = (displayMetrics.widthPixels * 0.28f).toInt() // Responsive size

        puzzleGrid = findViewById(R.id.puzzleGrid)
        heuristicText = findViewById(R.id.heuristicText)
        movesText = findViewById(R.id.movesText)
        efficiencyText = findViewById(R.id.efficiencyText)

        setupTileBackgrounds()
        setupButtons()
        setupControlButtons()

        shufflePuzzle(20)  // Initial medium shuffle
    }

    private fun setupTileBackgrounds() {
        tileBackground = GradientDrawable().apply {
            setColor(0xFF4CAF50.toInt()) // Vibrant green
            cornerRadius = 25f
            setStroke(5, 0xFF2E7D32.toInt())
        }
        blankBackground = GradientDrawable().apply {
            setColor(0xFFF5F5F5.toInt()) // Light gray
            cornerRadius = 25f
            setStroke(5, 0xFFCCCCCC.toInt())
        }
    }

    private fun createGoalBoard(): Board {
        return Board(arrayOf(intArrayOf(1, 2, 3), intArrayOf(4, 5, 6), intArrayOf(7, 8, 0)))
    }

    private fun setupButtons() {
        buttons.clear()
        puzzleGrid.removeAllViews()
        for (i in 0..8) {
            val button = Button(this).apply {
                val lp = GridLayout.LayoutParams(
                    GridLayout.spec(i / 3),
                    GridLayout.spec(i % 3)
                ).apply {
                    width = tileSizePx
                    height = tileSizePx
                    setMargins(12, 12, 12, 12)
                }
                layoutParams = lp
                typeface = Typeface.DEFAULT_BOLD
                setOnClickListener { onTileClick(i / 3, i % 3) }
            }
            buttons.add(button)
            puzzleGrid.addView(button)
        }
        updateUI()
    }

    private fun setupControlButtons() {
        findViewById<Button>(R.id.hintButton).setOnClickListener { showHint() }
        findViewById<Button>(R.id.solveButton).setOnClickListener { aiSolve() }
        findViewById<Button>(R.id.undoButton).setOnClickListener { undoLastTwoMoves() }
        findViewById<Button>(R.id.shuffleEasy).setOnClickListener { shufflePuzzle(Random.nextInt(10, 21)) }
        findViewById<Button>(R.id.shuffleMedium).setOnClickListener { shufflePuzzle(Random.nextInt(20, 41)) }
        findViewById<Button>(R.id.shuffleHard).setOnClickListener { shufflePuzzle(Random.nextInt(40, 61)) }
    }

    private fun shufflePuzzle(shuffleCount: Int) {
        currentBoard = createGoalBoard()
        repeat(shuffleCount) {
            val neighbors = currentBoard.neighbors()
            if (neighbors.isNotEmpty()) {
                currentBoard = neighbors[Random.nextInt(neighbors.size)]
            }
        }
        // Ensure it's solvable and compute optimal
        val solver = Solver(currentBoard)
        optimalMoves = solver.minMoves
        if (optimalMoves == 0) {
            shufflePuzzle(shuffleCount) // Rare, reshuffle
            return
        }
        takenMoves = 0
        moveHistory.clear()
        startTime = System.currentTimeMillis()
        efficiencyText.text = "Optimal: $optimalMoves moves"
        updateUI()
    }

    private fun onTileClick(row: Int, col: Int) {
        val (blankRow, blankCol) = currentBoard.blankPosition()
        if (abs(row - blankRow) + abs(col - blankCol) == 1) {
            moveHistory.add(currentBoard)
            if (moveHistory.size > 2) moveHistory.removeAt(0)
            val newTiles = currentBoard.tiles.map { it.copyOf() }.toTypedArray()
            newTiles[blankRow][blankCol] = newTiles[row][col]
            newTiles[row][col] = 0
            currentBoard = Board(newTiles)
            takenMoves++
            updateUI()
            if (currentBoard.isGoal()) {
                onSolved()
            }
        }
    }

    private fun showHint() {
        val solver = Solver(currentBoard)
        val path = solver.solution()
        if (path.size > 1) {
            val nextBoard = path[1]
            val (nextBlankRow, nextBlankCol) = nextBoard.blankPosition()
            val hintButton = buttons[nextBlankRow * 3 + nextBlankCol]
            // Flash green
            val originalBg = hintButton.background
            hintButton.setBackgroundColor(0xFF00FF00.toInt())
            handler.postDelayed({
                hintButton.background = originalBg
            }, 800)
        }
    }

    private fun aiSolve() {
        moveHistory.clear() // Clear user history for AI solve
        val solver = Solver(currentBoard)
        optimalMoves = solver.minMoves
        val path = solver.solution()
        animateSolution(path, 0)
    }

    private fun animateSolution(path: List<Board>, index: Int) {
        if (index < path.size) {
            currentBoard = path[index]
            updateUI()
            handler.postDelayed({ animateSolution(path, index + 1) }, 300) // Faster animation
        } else {
            takenMoves = optimalMoves
            onSolved()
        }
    }

    private fun undoLastTwoMoves() {
        var undone = 0
        while (moveHistory.isNotEmpty() && undone < 2) {
            currentBoard = moveHistory.removeLast()
            takenMoves--
            undone++
        }
        updateUI()
    }

    private fun onSolved() {
        val timeSec = (System.currentTimeMillis() - startTime) / 1000f
        val eff = if (takenMoves > 0 && optimalMoves > 0) {
            (optimalMoves.toFloat() / takenMoves * 100).toInt()
        } else 100
        efficiencyText.text = "Solved! Eff: ${eff}% | Moves: $takenMoves / $optimalMoves | Time: ${String.format("%.1f", timeSec)}s"

        AlertDialog.Builder(this)
            .setTitle("🎉 Puzzle Solved! 🎉")
            .setMessage("Moves taken: $takenMoves\nOptimal: $optimalMoves\nEfficiency: ${eff}%\nTime: ${String.format("%.1f", timeSec)}s")
            .setPositiveButton("New Easy") { _, _ -> shufflePuzzle(Random.nextInt(10, 21)) }
            .setNegativeButton("New Medium") { _, _ -> shufflePuzzle(Random.nextInt(20, 41)) }
            .setNeutralButton("New Hard") { _, _ -> shufflePuzzle(Random.nextInt(40, 61)) }
            .show()
    }

    private fun updateUI() {
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                val idx = i * 3 + j
                val btn = buttons[idx]
                val value = currentBoard.tiles[i][j]
                if (value == 0) {
                    btn.text = ""
                    btn.background = blankBackground
                    btn.isEnabled = false
                } else {
                    btn.text = value.toString()
                    btn.textSize = 28f
                    btn.setTextColor(0xFF000000.toInt())
                    btn.gravity = Gravity.CENTER
                    btn.background = tileBackground
                    btn.isEnabled = true
                }
            }
        }
        heuristicText.text = "Heuristic: ${currentBoard.manhattan()}"
        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
        movesText.text = "Moves: $takenMoves  Time: ${String.format("%.1f", elapsedSec)}s"
    }
}