package com.github.swent.swisstravel.algorithm.orderlocations

/**
 * Heuristic solver in O(n²) for the asymmetric closed Traveling Salesman Problem (TSP).
 *
 * Closed = start and end are the same (forms a loop). Asymmetric = distance(i, j) may differ from
 * distance(j, i). Uses greedy initialization + 2-opt refinement.
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

  /** Improves a given path by repeatedly applying the 2-opt heuristic. */
  private fun runTwoOpt(path: MutableList<Int>, dist: Array<DoubleArray>): List<Int> {
    var improved: Boolean
    do {
      improved = false
      for (i in 1 until path.size - 2) {
        for (j in i + 1 until path.size - 1) {
          val delta =
              (dist[path[i - 1]][path[j]] + dist[path[i]][path[j + 1]]) -
                  (dist[path[i - 1]][path[i]] + dist[path[j]][path[j + 1]])

          if (delta < -1e-6) {
            path.subList(i, j + 1).reverse()
            improved = true
          }
        }
      }
    } while (improved)
    return path
  }
}
