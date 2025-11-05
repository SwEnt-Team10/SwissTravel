package com.github.swent.swisstravel.algorithm.orderlocations

/**
 * Heuristic solver for the asymmetric closed Traveling Salesman Problem (TSP).
 *
 * Closed = start and end are the same (forms a loop). Asymmetric = distance(i, j) may differ from
 * distance(j, i). Uses greedy initialization + 2-opt refinement.
 */
class ClosedTsp {

  /**
   * Computes an approximate optimal closed route.
   *
   * @param dist Asymmetric distance matrix where dist[i][j] is the travel cost from i → j.
   * @param start Index of the starting (and ending) node.
   * @return A list of node indices representing a loop starting and ending at `start`.
   */
  fun closedTsp(dist: Array<DoubleArray>, start: Int): List<Int> {
    val n = dist.size
    require(start in 0 until n) { "Invalid start index" }

    val visited = BooleanArray(n) { false }
    val path = mutableListOf(start)
    visited[start] = true
    var current = start

    // Nearest Neighbor heuristic
    repeat(n - 1) {
      var next = -1
      var bestDist = Double.POSITIVE_INFINITY
      for (i in 0 until n) {
        if (!visited[i] && dist[current][i] < bestDist) {
          bestDist = dist[current][i]
          next = i
        }
      }
      require(next >= 0) { "No unvisited node found, something went wrong" }
      path.add(next)
      visited[next] = true
      current = next
    }

    // Close the tour
    path.add(start)

    // 2-opt optimization
    fun routeDistance(route: List<Int>): Double {
      var total = 0.0
      for (i in 0 until route.size - 1) {
        total += dist[route[i]][route[i + 1]]
      }
      return total
    }

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
