package signpost;

import java.util.Collections;
import java.util.Random;

import signpost.Model.Sq;
import static signpost.Place.PlaceList;
import static signpost.Place.pl;
import static signpost.Utils.*;

/** A creator of random Signpost puzzles.
 *  @author
 */
class PuzzleGenerator implements PuzzleSource {

    /** A new PuzzleGenerator whose random-number source is seeded
     *  with SEED. */
    PuzzleGenerator(long seed) {
        _random = new Random(seed);
    }

    @Override
    public Model getPuzzle(int width, int height, boolean allowFreeEnds) {
        Model model =
            new Model(makePuzzleSolution(width, height, allowFreeEnds));
        // FIXME: Remove the "//" on the following two lines.
        makeSolutionUnique(model);
        model.autoconnect();
        return model;
    }

    /** Return an array representing a WIDTH x HEIGHT Signpost puzzle.
     *  The first array index indicates x-coordinates (column numbers) on
     *  the board, and the second index represents y-coordinates (row numbers).
     *  Its values will be the sequence numbers (1 to WIDTH x HEIGHT)
     *  appearing in a sequence queen moves on the resulting board.
     *  Unless ALLOWFREEENDS, the first and last sequence numbers will
     *  appear in the upper-left and lower-right corners, respectively. */
    private int[][] makePuzzleSolution(int width, int height,
                                       boolean allowFreeEnds) {
        _vals = new int[width][height];
        _successorCells = Place.successorCells(width, height);
        int last = width * height;
        int x0, y0, x1, y1;
        if (allowFreeEnds) {
            int r0 = _random.nextInt(last),
                r1 = (r0 + 1 + _random.nextInt(last - 1)) % last;
            x0 = r0 / height; y0 = r0 % height;
            x1 = r1 / height; y1 = r1 % height;
        } else {
            x0 = 0; y0 = height - 1;
            x1 = width - 1; y1 = 0;
        }
        _vals[x0][y0] = 1;
        _vals[x1][y1] = last;
        // FIXME: Remove the following return statement and uncomment the
        //        next three lines.
        /**return new int[][] {
            { 14, 9, 8, 1 },
            { 15, 10, 7, 2 },
            { 13, 11, 6, 3 },
            { 16, 12, 5, 4 }
        };*/
        boolean ok = findSolutionPathFrom(x0, y0);
        assert ok;
        return _vals;
    }

    /** Try to find a random path of queen moves through VALS from (X0, Y0)
     *  to the cell with number LAST.  Assumes that
     *    + The dimensions of VALS conforms to those of MODEL;
     *    + There are cells (separated by queen moves) numbered from 1 up to
     *      and including the number in (X0, Y0);
     *    + There is a cell numbered LAST;
     *    + All other cells in VALS contain 0.
     *  Does not change the contents of any non-zero cell in VALS.
     *  Returns true and leaves the path that is found in VALS.  Otherwise
     *  returns false and leaves VALS unchanged. Does not change MODEL. */
    private boolean findSolutionPathFrom(int x0, int y0) {
        int w = _vals.length, h = _vals[0].length;
        int v;
        int start = _vals[x0][y0] + 1;
        PlaceList moves = _successorCells[x0][y0][0];
        Collections.shuffle(moves, _random);
        for (Place p : moves) {
            v = _vals[p.x][p.y];
            if (v == 0) {
                _vals[p.x][p.y] = start;
                if (findSolutionPathFrom(p.x, p.y)) {
                    return true;
                }
                _vals[p.x][p.y] = 0;
            } else if (v == start && start == w * h) {
                return true;
            }
        }
        return false;
    }

    /** Extend unambiguous paths in MODEL (add all connections where there is
     *  a single possible successor or predecessor). Return true iff any change
     *  was made. */
    static boolean extendSimple(Model model) {
        boolean found;
        found = false;
        while (makeForwardConnections(model)
               || makeBackwardConnections(model)) {
            found = true;
        }
        return found;
    }

    /** Make all unique forward connections in MODEL (those in which there is
     *  a single possible successor).  Return true iff changes were made. */
    static boolean makeForwardConnections(Model model) {
        int w = model.width(), h = model.height();
        boolean result;
        result = false;
        for (Sq sq : model) {
            if (sq.successor() == null && sq.direction() != 0) {
                Sq found = findUniqueSuccessor(model, sq);
                if (found != null) {
                    sq.connect(found);
                    result = true;
                }
            }
        }
        return result;
    }

    /** Return the unique square in MODEL to which unconnected square START
     *  can connect, or null if there isn't such a unique square. The unique
     *  square is either (1) the only connectable square in the proper
     *  direction from START, or (2) if START is numbered, a connectable
     *  numbered square in the proper direction from START (with the next
     *  number in sequence). */
    // case for multiple sucs, case for if suc's seqnum is not sequential
    //if multiple sucs, have to check seqnums
    static Sq findUniqueSuccessor(Model model, Sq start) {
        // FIXME: Fill in to satisfy the comment.
        /**for (int coordX = 0; coordX < model.width(); coordX += 1) {
            for (int coordY = 0; coordY < model.height(); coordY += 1) {
                if (start.connectable(model.get(coordX, coordY)) && !start.successors().contains(pl(coordX, coordY))) {
                    start.successors().add(pl(coordX, coordY));
                        }
                    }
                }*/
        PlaceList sucs = start.successors(); PlaceList numOfSucs = new PlaceList();
        if (sucs == null) {
            return null;
        } else if (sucs.size() == 1) {
            return model.get(sucs.get(0));
        } else if (sucs.size() > 1) {
            for (Place placeOfSuc : sucs) {
                if (model.get(placeOfSuc).sequenceNum() != 0 && start.sequenceNum() != 0) {
                    if (model.get(placeOfSuc).sequenceNum() == start.sequenceNum() + 1) {
                        return model.get(placeOfSuc);
                    }
                }
                else {
                    if (start.connectable(model.get(placeOfSuc))) {
                        numOfSucs.add(placeOfSuc);
                    }
                }
            }
        }
        if (numOfSucs.size() == 1) {
            return model.get(numOfSucs.get(0));
        }
        return null;
    }

    /** Make all unique backward connections in MODEL (those in which there is
     *  a single possible predecessor).  Return true iff changes made. */
    static boolean makeBackwardConnections(Model model) {
        int w = model.width(), h = model.height();
        boolean result;
        result = false;
        for (Sq sq : model) {
            if (sq.predecessor() == null && sq.sequenceNum() != 1) {
                Sq found = findUniquePredecessor(model, sq);
                if (found != null) {
                    found.connect(sq);
                    result = true;
                }
            }
        }
        return result;
    }

    /** Return the unique square in MODEL that can connect to unconnected
     *  square END, or null if there isn't such a unique square.
     *  This function does not handle the case in which END and one of its
     *  predecessors is numbered, except when the numbered predecessor is
     *  the only unconnected predecessor.  This is because findUniqueSuccessor
     *  already finds the other cases of numbered, unconnected cells. */
    static Sq findUniquePredecessor(Model model, Sq end) {
        // FIXME: Replace the following to satisfy the comment.
        /**for (int coordX = 0; coordX < model.width(); coordX += 1) {
            for (int coordY = 0; coordY < model.height(); coordY += 1) {
                if (model.get(coordX, coordY).connectable(end)) {
                    end.predecessors().add(pl(coordX, coordY));
                }
            }
        }*/

        /**PlaceList preds = end.predecessors();
        if (preds != null) {
            for (Place i : preds) {
                if (model.get(i).connectable(end)) {
                    return model.get(i);
                }
            }

        }*/
        PlaceList preds = end.predecessors(); PlaceList numOfPreds = new PlaceList();
        if (preds == null) {
            return null;}


        if (preds.size() >= 1) {
            for (Place i : preds) {
                if (model.get(i).connectable(end)) {
                    numOfPreds.add(i);
                }
            }
            }
        if (numOfPreds.size() == 1) {
            return model.get(numOfPreds.get(0));
        }
            //return model.get(preds.get(0));
        //} else if (preds.size() > 1) {
          //  for (Place placeOfSuc : preds) {
            //    if (model.get(placeOfSuc).sequenceNum() != 0 && end.sequenceNum() != 0) {
              //      if (model.get(placeOfSuc).sequenceNum() == end.sequenceNum() - 1) {
                //        return model.get(placeOfSuc);
                  //  }
                //}
            //}
        return  null;
        }



    /** Remove all links in MODEL and unfix numbers (other than the first and
     *  last) that do not affect solvability.  Not all such numbers are
     *  necessarily removed. */
    private void trimFixed(Model model) {
        int w = model.width(), h = model.height();
        boolean changed;
        do {
            changed = false;
            for (Sq sq : model) {
                if (sq.hasFixedNum() && sq.sequenceNum() != 1
                    && sq.direction() != 0) {
                    model.restart();
                    int n = sq.sequenceNum();
                    sq.unfixNum();
                    extendSimple(model);
                    if (model.solved()) {
                        changed = true;
                    } else {
                        sq.setFixedNum(n);
                    }
                }
            }
        } while (changed);
    }

    /** Fix additional numbers in MODEL to make the solution from which
     *  it was formed unique.  Need not result in a minimal set of
     *  fixed numbers. */
    private void makeSolutionUnique(Model model) {
        model.restart();
        AddNum:
        while (true) {
            extendSimple(model);
            if (model.solved()) {
                trimFixed(model);
                model.restart();
                return;
            }
            PlaceList unnumbered = new PlaceList();
            for (Sq sq : model) {
                if (sq.sequenceNum() == 0) {
                    unnumbered.add(sq.pl);
                }
            }
            Collections.shuffle(unnumbered, _random);
            for (Place p : unnumbered) {
                Model model1 = new Model(model);
                model1.get(p).setFixedNum(model.solution()[p.x][p.y]);
                if (extendSimple(model1)) {
                    model.get(p).setFixedNum(model1.get(p).sequenceNum());
                    continue AddNum;
                }
            }
            throw badArgs("no solution found");
        }
    }

    @Override
    public void setSeed(long seed) {
        _random.setSeed(seed);
    }

    /** Solution board currently being filled in by findSolutionPathFrom. */
    private int[][] _vals;
    /** Mapping of positions and directions to lists of queen moves on _vals. */
    private PlaceList[][][] _successorCells;

    /** My PNRG. */
    private Random _random;

}