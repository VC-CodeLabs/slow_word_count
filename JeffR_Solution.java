//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// to test a specific file, pass the filespec on the command-line
// e.g.
// java JeffR_Solution.java someFile.txt [someFile2.txt...]
// -OR-
// javac JeffR_Solution.java
// java JeffR_Solution someFile.txt [someFile2.txt...]
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Count the frequency of words in a file, S-L-O-W-L-Y
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JeffR_Solution {

    public static IWordFrequencyCounter getWordFrequencyCounterImpl() {
        // return new FastCounter();
        // return new FastCounter2();
        return new SlowCounter();
    }

    ////
    // the common workhorse
    //
    // validates filespec & access, times the primary algo defined by the interface, dumps the results
    //
    private static int countWordFrequencyInFile(File testFile) {
        String testFileName;
        try {
            // canonical path will be fully qualified
            testFileName = testFile.getCanonicalPath();

            // basic validation- file exists
            if( !testFile.exists()) {
                System.err.println("testFile " + testFileName + " does not exist.");
                return -2; // DOS file not found
            }

            // basic validation- we have access
            if( !testFile.canRead()) {
                System.err.println("testFile " + testFileName + " cannot be read.");
                return -5; // DOS access denied
            }

            System.out.println();
            System.out.println( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            System.out.println("Processing " + testFileName + "...");

            // time the algo performance
            long startTime = System.nanoTime();

            // the actual algo, utilizing common interface
            IWordFrequencyCounter iwfc = getWordFrequencyCounterImpl();
            iwfc.setup(testFile);
            for(String nextWord; (nextWord = iwfc.getNextWord()) != null; ) {
                iwfc.processWord(nextWord);
            }
            iwfc.finish();
            // we're done processing the file, 
            // we've counted how often each unique word 
            // appears in the file (non-case-sensitive)

            // time the algo performance;
            // note this doesn't include jvm startup time,
            // some initialization and some console i/o esp. dumping the results
            long endTime = System.nanoTime();
            long elapsedTime = endTime - startTime;
            double totalTimeMillis = 1.0 * elapsedTime / 1000000.0;

            // get the list of unique words and sort for easier eyeball valiation
            Map<String,Integer> wordCounts = iwfc.getWordCounts();
            List<String> words = new ArrayList<>(wordCounts.keySet());
            words.sort(new Comparator<String>(){

                @Override
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }

            });

            // dump the unique words and their counts
            System.out.println( "-----------");
            System.out.println( "Word Counts");
            System.out.println( "-----------");
            int totalWordsInFile = 0;
            for( String word : words ) {
                Integer wordFrequency = wordCounts.get(word);
                totalWordsInFile += wordFrequency;
                System.out.println( word + ": " + wordFrequency);
            }
            System.out.println( "___________");
            endTime = System.nanoTime();
            elapsedTime = endTime - startTime;
            totalTimeMillis = 1.0 * elapsedTime / 1000000.0;

            // dump the total unique word count & time to execute
            System.out.println( "...Processed " 
                    + totalWordsInFile + " total words"
                    + " with " + words.size() + " unique words"
                    + " in " /* + totalTimeMillis + "ms." */ + timeElapsed(elapsedTime));
            System.out.println( "===========");

            // return the total # of unique words in the file
            return words.size();

        }
        catch( IOException ex) {
            // some error during processing- dump the deets and return error code indicator
            System.err.println("Failure Processing " + ex.getClass().getName() + " " + ex.getMessage());
            return -(0x1F); // DOS general failure
        }

    }

    private static String timeElapsed(long nanos) {

        String elapsed = "" + nanos + "ns";
        if( nanos < 1000000 ) {
            return elapsed;
        }

        long millis = nanos / 1000000;
        nanos = nanos % 1000000;

        elapsed = "" + nanos + "ns";

        if( millis < 1000 ) {
            return ("" + millis + "ms") + " " + elapsed;
        }

        long secs = millis / 1000;
        millis = millis % 1000;

        elapsed = ("" + millis + "ms") + " " + elapsed;

        if( secs < 60 ) {
            return ("" + secs + "s") + " " + elapsed;
        }

        long mins = secs / 60;
        secs = secs % 60;

        elapsed = ("" + secs + "s") + " " + elapsed;

        return ("" + mins + "m") + " " + elapsed;
    }
    

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // external entry points
    //

    public static int processFile(File testFile) {

        return countWordFrequencyInFile(testFile);

    }

    public static int processFile(String testFileName) {
        File testFile = new File(testFileName);
        return processFile(testFile);
    }

    public static void main(String[] args){

        String sampleTestFile = "JeffR_Sample_OneLine.txt";

        String[] testFiles;

		if( args.length > 0 ) {
            testFiles = args;
        }
        else {
            testFiles = new String[]{ sampleTestFile };
        }

        for( String testFileName : testFiles) {

            int err = processFile(testFileName);

            if( err < 0) {
                // note any additional files won't be processed if one fails
                System.exit(-err);
            }
            

        }
	
	}

}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// define a standard interface so we can try out different algos
//
interface IWordFrequencyCounter {

    public void setup(File testFile) throws IOException;    
    public String getNextWord() throws IOException;
    public void processWord(String nextWord);
    public void finish() throws IOException;

    public Map<String,Integer> getWordCounts();
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// well, that's the goal, at least
//
class SlowCounter extends WordFrequencyCounterBase {

    File testFile;
    int offset = 0;

    public void setup(File testFile) throws IOException {
        this.testFile = testFile;
        wordCounts.clear();
        System.out.println();
    }

    public String getNextWord() throws IOException {
        String nextWord = null;
        boolean foundNextWord = false;
        
        FileReader fileReader = new FileReader(testFile);
        for( int pos = 0; pos < offset; pos++) {
            fileReader.read();
        }
        while( !foundNextWord) {
            int nextChar = fileReader.read();
            if( nextChar < 0 ) {
                break;
            }
            // System.out.print("" + nextChar + ",");
            offset++;
            String nextCharAsString = new String( new char[]{ (char)nextChar });
            System.out.print(nextCharAsString);
            Pattern pattern = Pattern.compile("[\s\r\n\t\f]");
            Matcher matcher = pattern.matcher(nextCharAsString);
            if( matcher.matches() || nextChar == 0) {
                foundNextWord = nextWord != null;
            } else if( nextWord == null) {
                nextWord = nextCharAsString;
            }
            else {
                char[] wordSoFar = new char[nextWord.length()+1];
                nextWord.getChars(0, nextWord.length(), wordSoFar, 0);
                wordSoFar[nextWord.length()] = (char)nextChar;
                nextWord = new String();
                for( char c : wordSoFar) {
                    nextWord = nextWord.concat(new String(new char[]{c}));
                }
            }

        }
        while( !(fileReader.read() < 0))
            ;
        fileReader.close();
        return nextWord;
    }

    private int length(String w) {
        int l = 0;
        for( ;; l++) {
            try {
                w.charAt(l);
            }
            catch( IndexOutOfBoundsException ex ) {
                break;
            }
        }
        return l;
    }

    private boolean wordsEqual(String a, String b) {
        boolean equals = true;
        equals = length(a) == b.length();
        for( int i = 0; i < Math.max(a.length(), b.length()); i++) {
            if( i < a.length() && i < b.length()) {
                equals &= a.charAt(i) == b.charAt(i);
            }
            else {
                equals = false;
            }
        }
        return equals;
    }

    private String storedCase(String word) {
        String stored = new String();
        for( int index = 0; index < word.length(); index++ ) {
            String next = new String( new char[]{word.charAt(index)} );
            if( index % 2 != 0) {
                next = next.toLowerCase();
            }
            else {
                next = next.toUpperCase();
            }
            stored = stored.concat(next);
        }
        return stored;
    }

    private String toUpperCase(String word) {
        String uppered = new String();
        char[] buff = new char[word.length()];
        for( int i = 0; i < word.length(); i++) {
            buff[i] = word.charAt(i);
            uppered = uppered.concat(new String(new char[]{buff[i]}).toUpperCase());
        }
        return uppered;

    }

    private String toLowerCase(String word) {
        String lowered = new String();
        char[] buff = new char[word.length()];
        for( int i = 0; i < word.length(); i++) {
            buff[i] = word.charAt(i);
            lowered = lowered.concat(new String(new char[]{buff[i]}).toLowerCase());
        }
        return lowered;

    }

    List<WordCounter> wordCounterList = null;
    public void processWord(String word) {
        if( wordCounterList == null ) {
            wordCounterList = new ArrayList<>();
            wordCounterList.add(new WordCounter(storedCase(word),1));
        }
        else {
            int activeIndex = -1, foundIndex = -1;
            WordCounter newItem = null;
            for( WordCounter item : wordCounterList ) {
                activeIndex++;
                if( false
                        || wordsEqual(toUpperCase(item.word),toLowerCase(word))
                        || wordsEqual(toLowerCase(item.word),toUpperCase(word))
                        || wordsEqual(item.word,toUpperCase(word))
                        // || wordsEqual(toUpperCase(item.word),word)
                        || wordsEqual(item.word,toLowerCase(word))
                        // || wordsEqual(toLowerCase(item.word),word)
                        || wordsEqual(item.word,word)
                        || wordsEqual(storedCase(item.word),storedCase(word))
                        || wordsEqual(toLowerCase(item.word),toLowerCase(word))
                        || item.word.toLowerCase().equals(word.toLowerCase())
                        ) {
                    newItem = new WordCounter(storedCase(word),item.counter + 1);
                    foundIndex = activeIndex;
                        
                }
            }

            if( newItem == null) {
                // new word we haven't seen before
                newItem = new WordCounter(storedCase(word), 1);
            }

            // create an new updated list
            List<WordCounter> newWordCounterList = new ArrayList<>();
            for( int index = 0; index < wordCounterList.size(); index++ ) {
                if( index == foundIndex) {
                    newWordCounterList.add(new WordCounter(storedCase(newItem.word), newItem.counter));
                }
                else {
                    newWordCounterList.add(new WordCounter(storedCase(wordCounterList.get(index).word), wordCounterList.get(index).counter));
                }
            }

            // new word we hadn't seen before, add to new updated list
            if( foundIndex < 0) {
                newWordCounterList.add(new WordCounter(storedCase(newItem.word), newItem.counter));
            }

            // clear out the old list
            for( int index = wordCounterList.size(); index-- > 0; ) {
                wordCounterList.remove(index);
            }
            
            wordCounterList = new ArrayList<>();

            // move new items back to master list
            for( int index = 0; index < newWordCounterList.size(); index++) {
                wordCounterList.add(new WordCounter(storedCase(newWordCounterList.get(index).word), newWordCounterList.get(index).counter));           
            }
        }

        // redo the full map every time we process a word
        List<String> keys = new ArrayList<>(wordCounts.keySet());
        for( String key : keys ) {
            wordCounts.remove(toLowerCase(key));
        }
        wordCounts.clear();
        for( int index = 0; index < wordCounterList.size(); index++ ) {
            WordCounter item = wordCounterList.get(index);
            String key = toLowerCase(item.word);
            wordCounts.put(key,item.counter);
        }

    }

    public void finish() throws IOException {
        for( int index = wordCounterList.size(); index-- > 0; ) {
            assert( wordCounts.containsKey(toLowerCase(wordCounterList.get(index).word)));
            wordCounterList.remove(index);
        }
        System.out.println();
    
    }

    static class WordCounter {
        public String word;
        public Integer counter;

        public WordCounter(String word, Integer counter) {
            this.word = word;
            this.counter = counter;
        }
    }

}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// similar to original FastCounter (below), 
// but much simpler file i/o
//
class FastCounter2 extends WordFrequencyCounterBase {

    Map<String,Integer> wordCounts = new HashMap<>();

    List<String> lines;

    public void setup(File testFile) throws IOException {
        lines = Files.readAllLines(Path.of(testFile.getAbsolutePath()));
    }

    int lineNum = 0;
    Pattern pattern = Pattern.compile("[^\s\r\n\t\f]+");
    Matcher matcher;

    public String getNextWord() throws IOException {
        String nextWord = null;
        boolean foundNextWord = false;
        while( !foundNextWord) {
            if( matcher == null ) {
                if( lineNum < lines.size())
                    matcher = pattern.matcher(lines.get(lineNum));
                else
                    return null;
            }

            if( matcher.find()) {                 
                nextWord = matcher.group();
                foundNextWord = true;
            }
            else {
                matcher = null;
                lineNum++;
            }
        }
             
        return nextWord;

    }


    public void finish() throws IOException {
        // nothing to clean up in this impl
    }

}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// original (fast, presumably) impl;
// utilizes standard lib
// 
class FastCounter extends WordFrequencyCounterBase {

    static final boolean VERBOSE = false;


    static final int FILE_BUFF_SIZE = 64*1024;
    BufferedReader fileReader;
    char buff[] = new char[FILE_BUFF_SIZE+1];
    int charsInBuff = 0;
    int offset = 0;
    boolean eof = false;

    Pattern pattern = Pattern.compile("[^\s\r\n\t\f]+");
    Matcher matcher;

    //
    // the getNextWord impl theoretically provides fast & efficient file i/o,
    // but with a little tweaking (small buffer size, no BufferedReader, &c)
    // might be just the opposite
    //
    public String getNextWord() throws IOException {

        String nextWord = null;
        boolean foundNextWord = false;
        while( !foundNextWord ) {

            // get some content to process if we don't already have some from the last pass
            if( charsInBuff == 0 && !eof) {
                int charsToRead = FILE_BUFF_SIZE - offset;
                int actuallyRead = fileReader.read(buff, offset, charsToRead);
                if( actuallyRead > 0 ) {
                    charsInBuff = actuallyRead;
                    buff[offset+actuallyRead] = '\0';

                    if( actuallyRead == charsToRead ) {
                        // *maybe* more content in the file
                    }
                    else {
                        // if we got less than the size requested, we've reached the end of file content
                        eof = true;
                    }
                }
                else {
                    // if read() return value is <= 0, there is no more file content available
                    charsInBuff = 0;
                    eof = true;
                }
            }

            // if we have no more content to process, return result(s) from prior pass (if any)
            if( charsInBuff == 0) {
                return nextWord;
            }

            // turn what we got from the file into a String for easier handling
            String inBuff = new String(buff,offset,charsInBuff);

            if( VERBOSE ) {
                System.out.println( "Processing `" + inBuff + "`");
            }

            // if we don't have a pending matcher, create one
            if( matcher == null ) {
                matcher = pattern.matcher(inBuff);
            }

            // find the next word, if any
            if( matcher.find()) {
                // found a word in the file
                if( nextWord == null ) {
                    // we don't have a dangling word, take the word we found as the whole word
                    nextWord = matcher.group();
                }
                else {
                    // we had a dangler

                    if( matcher.start() > 0) {
                        // ...but now we've determined we had whitespace between it and the new word (fragment?)
                        // reset the matcher & take the dangler as the next word, 
                        // we'll pick the new word up on the next pass
                        matcher = null;
                        return nextWord;
                    }
                    // now intervening whitespace between this word and the last word, combine into a single word
                    nextWord = nextWord.concat(matcher.group());
                }

                // the end of this match is where we start looking for the next word
                int endsAt = matcher.end();
                if( endsAt > 0 ) {
                    offset += endsAt;
                    charsInBuff -= endsAt;
                    if( charsInBuff > 0 ) {
                        // more chars in the buffer, so we found the end of the word
                        foundNextWord = true;
                    }
                    else {
                        // bumped up against the end of the buffer, we might have split a word across a read
                    }
                    
                }
                else {
                    offset++;
                    charsInBuff--;
                }

                if( offset >= FILE_BUFF_SIZE) {
                    // nothing more in the buffer, reset to read more on next pass
                    offset = 0;
                    charsInBuff = 0;
                }
            }
            else {
                // nothing found, reset to read more on next pass
                offset = 0;
                charsInBuff = 0;
                if( nextWord != null ) {
                    // if we has a dangler we'll bail out
                    foundNextWord = true;
                }
            }

            // we exhausted this matcher, reset it
            matcher = null;

            // no more input available, bail out
            if( eof ) {
                break;
            }
            
        }

        return nextWord;
    }

    public void setup(File testFile) throws IOException {

        fileReader = new BufferedReader(new FileReader(testFile));
        
        buff[0] = '\0';
        charsInBuff = 0;
        offset = 0;
        
        eof = false;

        wordCounts.clear();
    }

    public void finish() throws IOException {
        if( fileReader != null ) {
            fileReader.close();
        }
    }
}

abstract class WordFrequencyCounterBase implements IWordFrequencyCounter {

    Map<String,Integer> wordCounts = new HashMap<>();

    public Map<String, Integer> getWordCounts() {
        return wordCounts;
    }
    
    
    public void processWord(String nextWord) {
        String key = nextWord.toLowerCase();
        Integer countSoFar = wordCounts.get(key);
        boolean firstTime = countSoFar == null || countSoFar.intValue() == 0;
        if( !firstTime ) {
            countSoFar++;
        }
        else {
            countSoFar = Integer.valueOf(1);
        }
        wordCounts.put(key, countSoFar);

    }

}