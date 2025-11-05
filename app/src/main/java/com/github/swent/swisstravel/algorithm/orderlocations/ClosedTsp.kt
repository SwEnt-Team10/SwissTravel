package com.github.swent.swisstravel.algorithm.orderlocations

/**
 * Heuristic solver for the asymmetric closed Traveling Salesman Problem (TSP).
 *
 * This implementation finds an approximate solution for a closed tour, where the start and end
 * points are the same. It is designed for asymmetric distances, where the cost from A to B may not
 * be the same as from B to A.
 *
 * The algorithm uses a greedy Nearest Neighbor heuristic for initialization, followed by a 2-opt
 * refinement process to improve the initial path.
 *
 * The time complexity is dominated by the 2-opt refinement, making it O(n^3) in the worst case,
 * where n is the number of nodes.
 */
class ClosedTsp {

  /**
   * Computes an approximate optimal closed route (a tour).
   *
   * @param dist Asymmetric distance matrix where dist[i][j] is the travel cost from node i to j.
   * @param start Index of the starting (and ending) node for the tour.
   * @return A list of node indices representing a loop that starts and ends at `start`.
   */
  fun closedTsp(dist: Array<DoubleArray>, start: Int): List<Int> {
    val n = dist.size
    require(n == 0 || dist.all { it.size == n }) { "The distance matrix must be square." }
    if (n <= 1) return listOf(start)

    require(start in 0 until n) { "Invalid start index: $start for size $n" }

    // 1. Initialize path with a greedy heuristic.
    val initialPath = greedyNearestNeighbor(dist, start)

    // 2. Refine the path using the 2-opt algorithm.
    return runTwoOpt(initialPath, dist)
  }

  /** Constructs an initial path using the Nearest Neighbor greedy heuristic. */
  private fun greedyNearestNeighbor(dist: Array<DoubleArray>, start: Int): MutableList<Int> {
    val n = dist.size
    val path = mutableListOf(start)
    val visited = BooleanArray(n) { false }
    visited[start] = true
    var current = start

    repeat(n - 1) {
      val next =
          (0 until n).filter { !visited[it] }.minByOrNull { dist[current][it] }
              ?: error("No unvisited node found, which should not happen in a connected graph.")

      path.add(next)
      visited[next] = true
      current = next
    }

    // Close the tour by returning to the start.
    path.add(start)
    return path
  }

  /**
   * Improves a given path by repeatedly applying a 2-opt like heuristic, adapted for asymmetric
   * graphs.
   */
  private fun runTwoOpt(path: MutableList<Int>, dist: Array<DoubleArray>): List<Int> {
    var improved: Boolean
    do {
      improved = false
      for (i in 1 until path.size - 2) {
        for (j in i + 1 until path.size - 1) {
          // Cost of the current segment: (i-1 -> i) + ... + (j -> j+1)
          val currentCost =
              dist[path[i - 1]][path[i]] +
                  (i until j).sumOf { dist[path[it]][path[it + 1]] } +
                  dist[path[j]][path[j + 1]]

          // Cost of the proposed new segment with the sub-path reversed.
          // New path is: (i-1 -> j) -> (j-1) -> ... -> (i) -> (j+1)
          val reversedSubPathCost = (j downTo i + 1).sumOf { dist[path[it]][path[it - 1]] }
          val newCost =
              dist[path[i - 1]][path[j]] + reversedSubPathCost + dist[path[i]][path[j + 1]]

          // If the new cost is better, perform the reversal.
          if (newCost < currentCost - 1e-6) {
            path.subList(i, j + 1).reverse()
            improved = true
          }
        }
      }
    } while (improved)
    return path
  }
}
