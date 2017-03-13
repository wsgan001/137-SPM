package onethreeseven.spm.data;


import onethreeseven.collections.IntArray;
import onethreeseven.spm.model.CoveredSequentialPattern;
import onethreeseven.spm.model.SequentialPattern;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * For parsing output from SPMF sequential patterns.
 * Can also parseSequences SPMF sequence files.
 * @see <a href="http://www.philippe-fournier-viger.com/spmf/index.php?link=developers.php">SPMF</a>
 * for details on the file format.
 * @author Luke Bermingham
 */
public class SPMFParser {

    private interface PatternProcessor{
        void parseLine(String line);
    }

    private static final Logger logger = Logger.getLogger(SPMFParser.class.getSimpleName());
    private final String delimiter;

    public SPMFParser(){
        this(" ");
    }

    public SPMFParser(String delimiter){
        this.delimiter = delimiter;
    }

    public int[][] parseSequences(File file){
        try {
            return parseSequences(new BufferedReader(new FileReader(file)));
        } catch (FileNotFoundException e) {
            logger.severe("Could not find spmf output file to parseSequences: " + e.getMessage());
        }
        return new int[][]{};
    }

    public int[][] parseSequences(BufferedReader br){
        final ArrayList<int[]> sequences = new ArrayList<>();

        parseImpl(br, line -> {
            //parseSequences
            int[] seq = parseSequence(line, SPMFParser.this.delimiter);
            sequences.add(seq);
        });

        int[][] out = new int[sequences.size()][];
        out = sequences.toArray(out);
        return out;
    }

    public List<SequentialPattern> parsePatterns(File file){
        ArrayList<SequentialPattern> patterns = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            parseImpl(br, line -> {
                SequentialPattern pattern =
                        parsePattern(line, SPMFParser.this.delimiter);
                patterns.add(pattern);
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return patterns;
    }

    private void parseImpl(BufferedReader br, PatternProcessor processor){

        String line;
        boolean keepReading = true;
        while(keepReading){
            try {
                line = br.readLine();
                if(line != null && !line.isEmpty()){
                    processor.parseLine(line);
                }
                keepReading = line != null;

            } catch (IOException e) {
                logger.severe("Could not read spmf file: " + e.getMessage());
            }
        }
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static int[] parseSequence(String line, String delimiter){

        line = line.trim();
        String[] lineParts = line.split(delimiter);

        IntArray arr = new IntArray(lineParts.length, false);

        for (String part : lineParts) {
            part = part.trim();
            if(part.equals("-1") || part.equals("-2") || part.isEmpty()){continue;}
            if(part.startsWith("#")){break;}
            arr.add(Integer.parseInt(part));
        }

        return arr.getArray();
    }

    static SequentialPattern parsePattern(String line, String delimiter){
        line = line.trim();
        String[] lineParts = line.split(delimiter);

        IntArray arr = new IntArray(lineParts.length, false);
        int sup = 0;
        int cover = -1;

        for (String part : lineParts) {
            part = part.trim();
            if(part.equals("-1") || part.equals("-2") || part.isEmpty()){continue;}
            //capture support
            if(part.startsWith("#SUP:")){
                String[] supSplit = part.split(":");
                sup = Integer.parseInt(supSplit[1]);
            }
            else if(part.startsWith("#COVER:")){
                String[] coverSplit = part.split(":");
                cover = Integer.parseInt(coverSplit[1]);
            }
            else{
                arr.add(Integer.parseInt(part));
            }
        }

        if(cover == -1){
            return new SequentialPattern(arr.getArray(), sup);
        }else{
            return new CoveredSequentialPattern(arr.getArray(), sup, cover);
        }
    }

}
