package com.github.swent.swisstravel.algorithm.orderlocations

/**
 * Asymmetric open Traveling Salesman Problem (TSP) solver.
 *
 * Asymmetric because travel time from A -> B may differ from B -> A. Open because doesn't return to
 * the starting point. TSP because needs to find a route that visits all cities exactly once and
 * minimize the travel time.
 *
 * Unfortunately, the TSP problem as a NP-hard complexity. Therefore we will use a heuristic
 * (Greedy + Local search) to solve it and find a good enough order.
 */
class OpenTsp {

  /**
   * Computes an approximate optimal route for the open asymmetric Traveling Salesman Problem
   *
   * This implementation is designed for travel itineraries where:
   * - The trip has a fixed start and a fixed end point (open path, not a loop).
   * - Travel times between locations may differ depending on direction (asymmetric matrix).
   * - The number of stops is small enough that a fast heuristic is preferable to an exact
   *   exponential algorithm.
   *
   * @param dist a two-dimensional array where `dist[i][j]` is the travel cost (distance or
   *   duration) from location `i` to location `j`. The matrix can be asymmetric; diagonal entries
   *   should be zero.
   * @param start the index of the fixed starting location in the `dist` matrix.
   * @param end the index of the fixed ending location in the `dist` matrix.
   * @return an ordered list of node indices representing the recommended travel sequence, beginning
   *   at `start` and ending at `end`.
   */
  fun openTsp(dist: Array<DoubleArray>, start: Int, end: Int): List<Int> {
    val n = dist.size
    val visited = BooleanArray(n)
    visited[start] = true
    visited[end] = true

    val route = mutableListOf(start)
    var current = start

    // Greedy step: always go to the nearest unvisited node (excluding end)
    while (route.size < n - 1) {
      val next = (0 until n).filter { !visited[it] }.minByOrNull { dist[current][it] } ?: break

      route.add(next)
      visited[next] = true
      current = next
    }

    // Add the fixed end node
    route.add(end)

    // Optional refinement: 2-opt improvement
    return twoOpt(route, dist)
  }

  /**
   * Performs a 2-opt local search on a given route to reduce total travel distance.
   *
   * The 2-opt algorithm systematically checks all pairs of edges (i, j) and tests whether reversing
   * the segment between them yields a shorter path. It repeats until no further improvement is
   * possible.
   *
   * @param route the initial route as a list of node indices.
   * @param dist the same asymmetric distance matrix used in [openTsp].
   * @return an improved route with possibly shorter total cost.
   */
  fun twoOpt(route: List<Int>, dist: Array<DoubleArray>): List<Int> {
    var best = route.toMutableList()
    var improved = true

    fun totalDistance(path: List<Int>): Double {
      var sum = 0.0
      for (i in 0 until path.size - 1) sum += dist[path[i]][path[i + 1]]
      return sum
    }

    while (improved) {
      improved = false
      val bestDistance = totalDistance(best)

      for (i in 1 until best.size - 2) {
        for (j in i + 1 until best.size - 1) {
          val newRoute = best.toMutableList()
          newRoute.subList(i, j + 1).reverse()
          val newDistance = totalDistance(newRoute)

          if (newDistance < bestDistance - 1e-6) {
            best = newRoute
            improved = true
          }
        }
      }
    }
    return best
  }
}
