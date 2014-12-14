package ca.brocku.dotscanvas.app.gameboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

/**
 * DotGrid is essentially the game board. It is essentially a 2D array that holds the dots.
 * <p/>
 * Dots in the grid are indexed by row-by-row. 0-based indexing is used.
 */
public class DotGrid implements Iterable<Dot>, Serializable {
  private final int GRID_LENGTH;
  private final int NUMBER_OF_DOTS;

  private Dot[][] grid;

  public DotGrid(int gridLength) {
    this.GRID_LENGTH = gridLength;
    this.NUMBER_OF_DOTS = GRID_LENGTH * GRID_LENGTH;

    this.grid = new Dot[GRID_LENGTH][GRID_LENGTH];
    initializeGrid();
  }

  /**
   * Gets a dot from the grid at a particular index.
   *
   * @param index the index of the dot to retrieve
   * @return the dot at the specified index
   */
  public Dot dotAt(int index) {
    int row = index / GRID_LENGTH; //the row of the dot
    int col = index % GRID_LENGTH; //the column of the dot

    return grid[row][col];
  }

  /**
   * Calculate the index of a dot in the grid.
   *
   * @param dot the index of this dot will be calculated
   * @return the index of the dot
   */
  public int getIndex(Dot dot) {
    return GRID_LENGTH * dot.getRow() + dot.getCol();
  }

  /**
   * The size of a cluster that is adjacent to the passed, invisible dot is calculated. This dot
   * may be a bridge for disjointed clusters, so one of the adjacent dots are randomly selected.
   *
   * @param dot an invisible dot that may be adjacent to a cluster
   * @return the size of a cluster
   */
  public int clusterSizeFor(Dot dot) {
    int count = 0;
    Stack<Dot> stack = new Stack<Dot>(); //dots to consider
    HashSet<Dot> considered = new HashSet<Dot>();
    Dot currentDot;

    // Push a dot that is adjacent to this dot and is part of a cluster
    ArrayList<Dot> initialAdjDots = getAdjacentVisibleDots(dot);
    if (!initialAdjDots.isEmpty()) {
      Dot randomAdjDot = initialAdjDots.get((int) (Math.random() * initialAdjDots.size()));
      stack.push(randomAdjDot);
    }

    while (!stack.isEmpty()) {
      currentDot = stack.pop();

      if (!considered.contains(currentDot)) {
        considered.add(currentDot);
        count++;

        ArrayList<Dot> adjacentDots = getAdjacentVisibleDots(currentDot);
        for (Dot adjDot : adjacentDots) stack.push(adjDot);
      }
    }

    return count;
  }

  /**
   * Returns the dots that are both adjacent to the specified dot and not invisible. Handles
   * OutOfBounds locations.
   *
   * @param dot adjacent dots will be determined for this dot
   * @return an ArrayList of 0 to 4 adjacent dots
   */
  private ArrayList<Dot> getAdjacentVisibleDots(Dot dot) {
    ArrayList<Dot> adjDotList = new ArrayList<Dot>();
    Dot[] adjDots = new Dot[8];

    adjDots[0] = getNorth(dot);
    adjDots[1] = getNorthEast(dot);
    adjDots[2] = getEast(dot);
    adjDots[3] = getSouthEast(dot);
    adjDots[4] = getSouth(dot);
    adjDots[5] = getSouthWest(dot);
    adjDots[6] = getWest(dot);
    adjDots[7] = getNorthWest(dot);

    for (Dot adjDot : adjDots) {
      if (adjDot != null && adjDot.getState() != DotState.INVISIBLE) adjDotList.add(adjDot);
    }

    return adjDotList;
  }

  private Dot getNorth(Dot dot) {
    int row = dot.getRow();

    if ((row - 1) >= 0 && (row - 1) <= GRID_LENGTH - 1) {
      return grid[row - 1][dot.getCol()];
    } else {
      return null;
    }
  }

  private Dot getNorthEast(Dot dot) {
    int row = dot.getRow();
    int col = dot.getCol();

    if ((row - 1) >= 0 && (row - 1) <= GRID_LENGTH - 1 &&
        (col + 1) >= 0 && (col + 1) <= GRID_LENGTH - 1) {
      return grid[row - 1][col + 1];
    } else {
      return null;
    }
  }

  private Dot getEast(Dot dot) {
    int col = dot.getCol();

    if ((col + 1) >= 0 && (col + 1) <= GRID_LENGTH - 1) {
      return grid[dot.getRow()][col + 1];
    } else {
      return null;
    }
  }

  private Dot getSouthEast(Dot dot) {
    int row = dot.getRow();
    int col = dot.getCol();

    if ((row + 1) >= 0 && (row + 1) <= GRID_LENGTH - 1 &&
        (col + 1) >= 0 && (col + 1) <= GRID_LENGTH - 1) {
      return grid[row + 1][col + 1];
    } else {
      return null;
    }
  }

  private Dot getSouth(Dot dot) {
    int row = dot.getRow();

    if ((row + 1) >= 0 && (row + 1) <= GRID_LENGTH - 1) {
      return grid[row + 1][dot.getCol()];
    } else {
      return null;
    }
  }

  private Dot getSouthWest(Dot dot) {
    int row = dot.getRow();
    int col = dot.getCol();

    if ((row + 1) >= 0 && (row + 1) <= GRID_LENGTH - 1 &&
        (col - 1) >= 0 && (col - 1) <= GRID_LENGTH - 1) {
      return grid[row + 1][col - 1];
    } else {
      return null;
    }
  }

  private Dot getWest(Dot dot) {
    int col = dot.getCol();

    if ((col - 1) >= 0 && (col - 1) <= GRID_LENGTH - 1) {
      return grid[dot.getRow()][col - 1];
    } else {
      return null;
    }
  }

  private Dot getNorthWest(Dot dot) {
    int row = dot.getRow();
    int col = dot.getCol();

    if ((row - 1) >= 0 && (row - 1) <= GRID_LENGTH - 1 &&
        (col - 1) >= 0 && (col - 1) <= GRID_LENGTH - 1) {
      return grid[row - 1][col - 1];
    } else {
      return null;
    }
  }

  private void initializeGrid() {
    int id = 1;
    for (int col = 0; col < GRID_LENGTH; col++) {
      for (int row = 0; row < GRID_LENGTH; row++) {
        grid[row][col] = new Dot(id++, row, col);
      }
    }
  }

  /**
   * Creates and returns a new iterator for this dot grid.
   *
   * @return a new iterator for this dot grid
   */
  @Override
  public Iterator<Dot> iterator() {
    return new DotGridIterator();
  }

  /**
   * Traverses the dots by row.
   */
  private class DotGridIterator implements Iterator<Dot> {
    private int counter; //Number of dots traversed

    private DotGridIterator() {
      counter = 0;
    }

    @Override
    public boolean hasNext() {
      return counter < NUMBER_OF_DOTS;
    }

    @Override
    public Dot next() {
      int row = counter / GRID_LENGTH; //number of whole rows traversed
      int col = counter % GRID_LENGTH; //number of columns traversed in the current row

      counter++;

      return grid[row][col];
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
