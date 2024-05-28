package ocflzw;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DecompressBlob {

    public static void main(String[] args) throws IOException {
        byte[] contents = Files.readAllBytes(Paths.get("C://Sites//BLOB//blob//B10"));;
        byte[] out = decompress(contents);
        String outCharset = getCharacterEncoding(out);
        String output = new String(out, outCharset);
        System.out.println(output);
    }
    
    public static String getCharacterEncoding(byte[] byte_array)
    {
 
        // Creating an object of InputStream
        InputStream instream
            = new ByteArrayInputStream(byte_array);
 
        // Now, opening new file input stream reader
        InputStreamReader streamreader
            = new InputStreamReader(instream);
        String defaultCharset = streamreader.getEncoding();
 
        // Returning default character encoding
        return defaultCharset;
    }

    private static class LzwItem {
        public int prefix;
        public int suffix;

        public LzwItem(int _prefix, int _suffix) {
            this.prefix = _prefix;
            this.suffix = _suffix;
        }
    }
    
    private static final int MAX_CODES = 8192;
    private static int[] tempDecompressBuffer = new int[MAX_CODES];
    private static LzwItem[] lzwLookupTable = new LzwItem[MAX_CODES];
    private static byte[] finalByteBuffer = null;
    
    private static int codeCount = 257;
    private static int shift = 1;
    private static int currentShift = 1;
    private static int prevCode = 0;
    private static int firstCode = 0;
    private static int middleCode = 0;
    private static int lookupIndex = 0;
    private static int tempBufferIndex = 0;
    private static int currentByteBufferIndex = 0;
    private static int byteArrayIndex = 0;

    public static byte[] decompress(byte[] rawbytes) {
        finalByteBuffer = new byte[rawbytes.length * 4];
        firstCode = rawbytes[byteArrayIndex] & 0xFF;

        while (true) {
            if (currentShift >= 9) {
                currentShift -= 8;
                if (firstCode != 0) {
                    if (byteArrayIndex + 2 >= rawbytes.length) break;
                    middleCode = rawbytes[++byteArrayIndex] & 0xFF;
                    firstCode = (firstCode << (currentShift + 8)) | (middleCode << currentShift);
                    middleCode = rawbytes[++byteArrayIndex] & 0xFF;
                    int tempCode = middleCode >> (8 - currentShift);
                    lookupIndex = firstCode | tempCode;                   
                    skipIt();                   
                    continue;
                } else {
                    if (byteArrayIndex + 1 >= rawbytes.length) break;
                    firstCode = rawbytes[++byteArrayIndex] & 0xFF;
                    middleCode = rawbytes[++byteArrayIndex] & 0xFF;
                }
            } else {
                if (byteArrayIndex + 1 >= rawbytes.length) break;
                middleCode = rawbytes[++byteArrayIndex] & 0xFF;
            }

            lookupIndex = (firstCode << currentShift) | (middleCode >> (8 - currentShift));

            if (lookupIndex == 256) {  // time to move to a new lookup table
                shift = 1;
                currentShift++;
                firstCode = rawbytes[byteArrayIndex] & 0xFF;
                tempDecompressBuffer = new int[MAX_CODES];
                tempBufferIndex = 0;
                lzwLookupTable = new LzwItem[MAX_CODES];
                codeCount = 257;
                continue;
            } else if (lookupIndex == 257) { // EOF marker, better than using the string size
                return finalByteBuffer;
            }
            skipIt();
        }
        return finalByteBuffer;
    }
    
    private static void skipIt() {
        if (prevCode == 0) {
            tempDecompressBuffer[0] = lookupIndex;
        }
        if (lookupIndex < codeCount) {
            saveItemToLookupTable(lookupIndex);
            if (codeCount < MAX_CODES) {
                lzwLookupTable[codeCount++] = new LzwItem(prevCode, tempDecompressBuffer[tempBufferIndex]);
            }
        } else {
            lzwLookupTable[codeCount++] = new LzwItem(prevCode, tempDecompressBuffer[tempBufferIndex]);
            saveItemToLookupTable(lookupIndex);
        }

        firstCode = (middleCode & (0xFF >> currentShift));
        currentShift += shift;

        switch (codeCount) {  // use the lookup table size and not the current byte count
            case 511:
            case 1023:
            case 2047:
            case 4095:
                shift++;
                currentShift++;
                break;
        }
        prevCode = lookupIndex;
    }

    private static void saveItemToLookupTable(int compressedCode) {
        tempBufferIndex = -1;
        while (compressedCode >= 258) {
            tempDecompressBuffer[++tempBufferIndex] = lzwLookupTable[compressedCode].suffix;
            compressedCode = lzwLookupTable[compressedCode].prefix;
        }
        tempDecompressBuffer[++tempBufferIndex] = compressedCode;
        for (int i = tempBufferIndex; i >= 0; i--) {
            if (currentByteBufferIndex < finalByteBuffer.length) {
                finalByteBuffer[currentByteBufferIndex++] = (byte) tempDecompressBuffer[i];
            } else {
                break;
            }
        }
    }
}
