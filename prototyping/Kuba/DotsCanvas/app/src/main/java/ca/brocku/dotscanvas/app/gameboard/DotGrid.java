package ca.brocku.dotscanvas.app.gameboard;

import java.util.Iterator;

/**
 * @author Jakub Subczynski
 * @date June 04, 2014
 */
public class DotGrid implements Iterable<Dot> {
    private final int GRID_LENGTH;
    private final int NUMBER_OF_DOTS;

    private Dot[][] grid;

    public DotGrid(int gridLength) {
        this.GRID_LENGTH = gridLength;
        this.NUMBER_OF_DOTS = GRID_LENGTH*GRID_LENGTH;

        this.grid = new Dot[GRID_LENGTH][GRID_LENGTH];
        initializeGrid();
    }

    private void initializeGrid() {
        int id = 1;
        for(int col=0; col<GRID_LENGTH; col++) {
            for(int row=0; row<GRID_LENGTH; row++) {
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
            int row = counter/GRID_LENGTH; //number of whole rows traversed
            int col = counter%GRID_LENGTH; //number of columns traversed in the current row

            counter++;

            return grid[row][col];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
